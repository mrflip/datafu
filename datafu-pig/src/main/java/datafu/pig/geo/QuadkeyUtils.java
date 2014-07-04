package datafu.pig.geo;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import com.esri.core.geometry.NumberUtils;

// import org.apache.pig.data.Tuple;
// import org.apache.pig.data.TupleFactory;
// import org.apache.pig.data.BagFactory;
// import org.apache.pig.data.DataBag;
//
// import com.esri.core.geometry.Geometry;
// import com.esri.core.geometry.ogc.OGCGeometry;
//
// import com.esri.core.geometry.Envelope;
// import com.esri.core.geometry.Envelope2D;
// import com.esri.core.geometry.GeometryEngine;
// import com.esri.core.geometry.Point;
// import com.esri.core.geometry.QuadTree;
// import com.esri.core.geometry.QuadTree.QuadTreeIterator;
// import com.esri.core.geometry.SpatialReference;
//
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/****************************************************************************
 *
 * Utilities for converting among different quadtile handle representations and
 * manipulaing quadkeys.
 *
 * Quadtile decomposition (or any of its fancier relatives like k-d trees) are
 * central to doing geospatial analysis using Big Data tools such as
 * Hadoop. They give you an effective way to put spatially nearby objects into
 * common context even when they are indexed at different levels of detail. The
 * examples demonstrating a spatial join show this very well.
 *
 * Tiles can be usefully referenced by any of the following:
 *
 * <ul>
 * <li><em>quadkey</em> -- long containing interleaved x/y bits of tile indices,
 *   aka the Morton number</li>
 *
 * <li><em>quadstr</em> -- String holding the base-4 representation of the
 *   quadkey. You may insert spaces (for readability), and may add trailing
 *   non-digits on the right (a handy trick to force parent tiles to sort ahead
 *   (* or #) or behind (~) their children.)</li>
 *
 * <li><em>tile x,y</em> -- Index of tile: X horizontally (from west to east /
 *   left to right) and Y vertically (from north to south / top to bottom). Note
 *   that this puts the origin at the top left corner.</li>
 *
 * <li><em>pixel x, y</em> -- index of pixel using the 256x256 tile scheme
 *   expected by most industry tileservers. As with tile indices, origin is at
 *   top left.</em>
 *</ul>
 *
 * All references to tiles must come with a zoom level of detail ('zl') (apart
 * from quadstrs, whose zoom level is implied by the count of its digits). The
 * zoom level is permitted to range down to 31, implying a 62-bit quadkey and
 * four million trillion tiles. (Tileservers generally extend only to zl 23).
 * Results are undefined for zoom level 32.
 *
 * The mercator maptile x/y/zl scheme is used by all popular online tile servers
 * -- open streetmap, google maps, bing maps, stamen, leaflet, others -- to
 * serve map imagery. It relies on a mercator projection that makes serious
 * geographers cry, but as anyone who internets should recognize is
 * exceptionally useful and ubiquitous. This provides methods for converting
 * from geographic coordinates to maptile handles.
 *
 * You don't have to use the Mercator projection for generating quadkey handles,
 * and apart from those with "Mercator" in the name these methods apply to any
 * quadtree scheme. The default concrete Quadtree class (QuadTreeImpl) in this
 * library simply partitions longitude and latitude uniformly.
 *
 * For more on Quadtiles and Quadkeys, see:
 *
 * <ul>
 * <li><a href="http://msdn.microsoft.com/en-us/library/bb259689.aspx">Bing Maps</a></li>
 * <li><a href="http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/">Maptiler Projection Widget</a></li>
 * <li><a href="http://wiki.openstreetmap.org/wiki/Tilenames#Implementations">Open Street Map Wiki</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Morton_number_(number_theory)">Wikipedia: Morton Number</a></li>
 * <li><a href="https://graphics.stanford.edu/~seander/bithacks.html#InterleaveTableLookup">Interleaving Morton Numbers"</a>
 * <li><a href="http://www.radicalcartography.net/?projectionref">Display and Thematic Map Projection Reference</a></li>
 * </ul>
*/
public final class QuadkeyUtils {
  private static final Log LOG = LogFactory.getLog(QuadkeyUtils.class);

  public static final double MIN_MERC_LNG    =  -180.0;
  public static final double MAX_MERC_LNG    =   180.0;
  public static final double MIN_MERC_LNGRAD = Math.toRadians(MIN_MERC_LNG);
  public static final double MAX_MERC_LNGRAD = Math.toRadians(MAX_MERC_LNG);
  //
  public static final double MIN_MERC_LAT    =  -85.05112878; // Math.atan(Math.sinh(Math.PI))*180/Math.PI; //
  public static final double MAX_MERC_LAT    =   85.05112878;
  public static final double MIN_MERC_LATRAD = Math.toRadians(MIN_MERC_LAT);
  public static final double MAX_MERC_LATRAD = Math.toRadians(MAX_MERC_LAT);
  //
  public static final double GLOBE_RADIUS    = 6378137.0;
  public static final double GLOBE_CIRCUM    = GLOBE_RADIUS * 2.0 * Math.PI;

  public static final int   MAX_ZOOM_LEVEL   = 31;
  public static final int   TILE_PIXEL_SIZE  = 256;

  // The arctan/log/tan/sinh business gives slight loss of precision. We could
  // live with that on the whole, but it can push the boundary of a tile onto
  // the one above it so lnglatToTileXY(tileXYToLnglat(foo)) != foo. Adding
  // this 1-part-per-billion fudge stabilized things; with this, no edge will
  // ever dance across tiles. Each of the following equivalents to the code
  // here or in tileXYToLnglat work, and none performed better.
  //
  // double lat_rad = Math.toRadians(lat);           // OSM version
  // double ty2     = mapsize * (1 - Math.log( Math.tan(lat_rad)  + (1/Math.cos(lat_rad)) )/Math.PI) / 2.0;
  // double sin_lat = Math.sin(lat * Math.PI / 180); // Bing version
  // double ty3     = mapsize * (0.5 - Math.log((1 + sin_lat) / (1 - sin_lat)) / (4 * Math.PI));
  // double lat2    = 180/Math.PI*Math.atan(Math.sinh(Math.PI * (1 - 2.0*ty/mapsize)));
  //
  public static final double EDGE_FUDGE      = 1e-10;

  /****************************************************************************
   *
   * Longitude/Latitude/ZL Methods
   *
   */

  /**
   * Tile XY indices of the tile containing that point at given zoom level using
   * the popular tileserver Mercator projection.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        { tile_x, tile_y }
   */
  public static int[] mercatorToTileXY(double lng, double lat, final int zl) {
    int      mapsize = mapTileSize(zl);
    //
    double[] proj_xy = mercatorToProjXY(lng, lat, zl);
    int[]    tile_xy = {
      (int) NumberUtils.snap(Math.floor(proj_xy[0]), 0, mapsize-1),
      (int) NumberUtils.snap(Math.floor(proj_xy[1]), 0, mapsize-1)
    };
    return tile_xy;
  }

  /**
   * Longitude/latitude WGS-84 coordinates (in degrees) of the top left (NW)
   * corner of the given tile in the popular tileserver Mercator projection.
   *
   * We allow this to be called with tile index = mapsize, i.e. the index of a
   * hypothetical tile hanging off the right or bottom edge of the map, so that
   * you can call this function on tx+1 or ty+1 to get the right/bottom edge of
   * a tile.
   *
   * @param tx      X index of tile
   * @param ty      Y index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        { longitude, latitude }
   */
  public static double[] tileXYToMercator(int tx, int ty, int zl) {
    return projXYToMercator(tx, ty, zl);
  }

  /**
   * XY coordinates of a point at given zoom level using the popular tileserver
   * Mercator projection. This is the fractional equivalent of the TileXY index.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        { tile_x, tile_y }
   */
  public static double[] mercatorToProjXY(double lng, double lat, final int zl) {
    assert lng <= 180 && lng >= -180 && lat <= 90 && lat >= -90;
    lng = NumberUtils.snap(lng,  MIN_MERC_LNG,  MAX_MERC_LNG);
    lat = NumberUtils.snap(lat,  MIN_MERC_LAT,  MAX_MERC_LAT);
    //
    int      mapsize = mapTileSize(zl);
    double   tx      = mapsize   * (lng + 180.0) / 360.0;
    double   ty      = mapsize/2 * (1 - Math.log(Math.tan( (90 + lat)*Math.PI/360.0 ))/Math.PI);
    //
    ty               = ty + EDGE_FUDGE; // See note above EDGE_FUDGE
    double[] tile_xy = { tx, ty };
    return tile_xy;
  }
  // System.err.println(String.format("%8d %8d %4d %20.15f %20.15f mercatorToTileXY",
  //     tile_xy[0], tile_xy[1], zl, lng, lat));

  /**
   * Longitude/latitude WGS-84 coordinates (in degrees) of the given point in
   * the popular tileserver Mercator projection.
   *
   * @param tx      X index of tile
   * @param ty      Y index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        { longitude, latitude }
   */
  public static double[] projXYToMercator(double tx, double ty, int zl) {
    int mapsize  = mapTileSize(zl);
    tx = NumberUtils.snap(tx, 0, mapsize);
    ty = NumberUtils.snap(ty, 0, mapsize);
    //
    double lng     = 360.0 * tx / mapsize - 180.0;
    double lat     = 90.0 - 360.0/Math.PI*Math.atan( Math.exp(Math.PI*(ty*2/mapsize - 1)) );
    //
    double[] result = {lng, lat};
    return result;
  }

  /**
   * WGS84 coordinates for tile's west, south, east, and north extents in the
   * popular tileserver Mercator projection. That is:
   *
   *   minimum longitude, minimum latitude, maximum longitude, maximum latitude
   *
   * @param tx      X index of tile
   * @param ty      Y index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        [west, south, east, north]
   */
  public static double[] tileXYToCoords(int tx, int ty, int zl) {
    int max_idx  = maxTileIdx(zl);
    if (tx > max_idx || ty > max_idx){ return null; }
    double[] lf_up = tileXYToMercator(tx,   ty,   zl);
    double[] rt_dn = tileXYToMercator(tx+1, ty+1, zl);

    // [left, bottom, right, top]​ -- [min_x, min_y, max_x, max_y]
    double[] result = { lf_up[0], rt_dn[1], rt_dn[0], lf_up[1] };
    return result;
  }

  /**
   * Quadkey handle of the tile containing that point at the given zoom level in
   * the popular tileserver Mercator projection.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        Quadkey handle of the tile
   */
  public static long mercatorToQuadkey(double lng, double lat, final int zl) {
    int[] tile_xy = mercatorToTileXY(lng, lat, zl);
    return tileXYToQuadkey(tile_xy[0], tile_xy[1]);
  }

  /**
   * Longitude/latitude WGS-84 coordinates (in degrees) of the top left (NW)
   * corner of the given tile in the popular tileserver Mercator projection.
   *
   * @param quadkey quadkey handle of the tile
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        { longitude, latitude }
   */
  public static double[] quadkeyToMercator(long quadkey, int zl) {
    int[] tile_xy = quadkeyToTileXY(quadkey);
    return tileXYToMercator(tile_xy[0], tile_xy[1], zl);
  }

  /**
   * Quadstr string handle of tile containing that point at the given zoom level
   * in the popular tileserver Mercator projection.
   *
   * @param lng        Longitude of the point, in degrees
   * @param lat        Latitude of the point, in degrees
   * @param zl         Zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return String holding base-4 representation of quadkey
   */
  public static String mercatorToQuadstr(double lng, double lat, final int zl) {
    int[] tile_xy = mercatorToTileXY(lng, lat, zl);
    return tileXYToQuadstr(tile_xy[0], tile_xy[1], zl);
  }

  /**
   * Longitude/latitude WGS-84 coordinates (in degrees) of the top left (NW)
   * corner of the given tile in the popular tileserver Mercator projection.
   *
   * @param quadstr quadstr string handle of the tile
   * @return        { longitude, latitude }
   */
  public static double[] quadkeyToMercator(String quadstr) {
    int[] tile_xyz = quadstrToTileXYZ(quadstr);
    return tileXYToMercator(tile_xyz[0], tile_xyz[1], tile_xyz[2]);
  }

  /**
   * Maximum WGS-84 latitude / longitude extent of the circle covering a given
   * distance from the point. The longitude extent is **not** the equivalent of
   * walking east and west from the point, because the meridians can close faster
   * than the shape of the circle.
   *
   * http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates
   * http://gis.stackexchange.com/questions/19221/find-tangent-point-on-circle-furthest-east-or-west
   *
   * Coordinates are returned as (west, south, east, and north) extents,
   * i.e. min lng/lat, max lng/lat. Keep in mind that the north coordinate will
   * have a smaller tile index than the southern.
   *
   * For longitude, the values are _unclipped_: they may extend below -180 or
   * above 180. For example, a circle of 200 km at 179.9 deg longitude might run
   * from longitudes 175 to 184, four degrees off the right side of the map.
   *
   * Areas that extend through the poles necessarily sweep the entire range of
   * longitudes, meaning that the bounding box -- while accurate -- has a
   * significantly larger extent than the circle. See tilesCoveringCircle for
   * one way this can lead to grief.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param dist    Distance in meters
   * @return        [west, south, east, north] -- i.e. min lng/lat, max lng/lat
   */
  public static double[] bboxCoordsForCircle(double lng, double lat, double dist) {
    if (dist == 0) {            double[] point = { lng,     lat,   lng,  lat  }; return point; }
    if (dist >= GLOBE_CIRCUM) { double[] world = { -180.0, -90.0, 180.0, 90.0 }; return world; }
    //
    double lat_rad    = Math.toRadians(lat);
    double dist_rad   = dist / GLOBE_RADIUS;
    double north_lat  = Math.toDegrees(lat_rad + dist_rad);
    double south_lat  = Math.toDegrees(lat_rad - dist_rad);
    //
    if (north_lat >= 90 || south_lat <= -90) {
      // if either was equal-or-past and both are equal-not-past then neither was past...
      // it only kissed the pole, so sweep the hemisphere not the globe.
      double lng_d = (north_lat <= 90 && south_lat <= -90 ? 90 : 180);
      double[] sweeps_pole = { lng-lng_d, (south_lat < -90 ? -90 : south_lat), lng+lng_d, (north_lat >  90 ?  90 : north_lat) };
      return sweeps_pole;
    }
    //
    double lng_delta = Math.asin( Math.sin(dist_rad) / Math.cos(lat_rad) );
    lng_delta = (Double.isNaN(lng_delta) ? 90.0 : Math.toDegrees(lng_delta));
    double east_lng   = lng + lng_delta;
    double west_lng   = lng - lng_delta;
    //
    double[] coords = { west_lng, south_lat, east_lng, north_lat };
    return coords;
  }

  /**
   * Quadtiles covering the area within a given distance in meters from a point.
   *
   * The tiles will cover a larger extent than the circle itself, as it returns
   * all tiles that cover any part of the bounding box that covers the circle.
   * And for large areas, this may return tiles that do not actually intersect
   * the circle (especially for far-northerly points).
   *
   * However, when using this to partition big data sets, In our experience it's
   * rarely worth filtering out the bits in the corner (consider that a circle
   * occupies 79% of its bounding square), and even less worth fine-graining the
   * tile size (blowing up their count) to get a closer tiling. And since <a
   * href="http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates">
   * distance on a sphere isn't so simple</a> -- it can be the case that all
   * corners of a tile are not within your circle while yet parts of the tile
   * are -- doing that filtering naively isn't a good plan.
   *
   * Areas that extend through the poles necessarily sweep the entire range of
   * longitudes. If they also extend significantly southward, this will blow up
   * their tile coverage. For a sense of this, a 600 km circle at zoom level 6
   * on a Mercator grid requires 16 tiles when centered at London (lat 50.5), 25
   * centered at Reykjavik (lat 62.1), 180 at Alert, Canada (82.5, the most
   * northern occupied place) and 520 tiles at 84.8 deg.
   *
   * So start by using the complete but sometimes overexuberant sets this
   * returns, and see whether you care. If you find that the northern-latitudes
   * or lots-of-small-tiles cases are important, look to the (more expensive)
   * functions that find a minimal tile set using Geometry objects. Also
   * consider indexing far-northerly points using an alternate scheme.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param dist    Distance in meters
   * @param zl      Zoom level of detail for the tiles
   * @return        List of quadstr string handles for a superset of the covering tiles
   */
  public static List<String> tilesCoveringCircle(double lng, double lat, double dist, int zl) {
    List<String> tiles = new ArrayList<String>();
    //
    // Get the max/min latitude to index
    double[] bbox = bboxCoordsForCircle(lng, lat, dist);
    double   west = bbox[0], south = bbox[1], east = bbox[2], north = bbox[3];
    //
    // See if we wrapped off the edge of the world
    double   west_2 = 0, east_2 = 0;
    if      (west <= -180 && east >= 180){    east  =  180;      west   = -180; } // whole world
    else if (west < -180){ west_2 = west+360; east_2 = 180;      west   = -180; } // iterate  west+360..180 and -180..east
    else if (east >  180){ west_2 = -180;     east_2 = east-360; east   =  180; } // iterate -180..east-360 and  west..180
    //
    // Scan over tile indexes. For Mercator, lines of lat/lng have constant tile index so only need two corners
    int[] txy_min = mercatorToTileXY(west,  north,  zl); // (lower ty is north)
    int[] txy_max = mercatorToTileXY(east,  south,  zl);
    addTilesCoveringXYRect(txy_min[0], txy_min[1], txy_max[0], txy_max[1], zl, tiles);
    //
    // If we wrapped, also contribute the wrapped portion
    if (west_2 != 0 || east_2 != 0) {
      txy_min = mercatorToTileXY(west_2, north,  zl);
      txy_max = mercatorToTileXY(east_2, south,  zl);
      addTilesCoveringXYRect(txy_min[0], txy_min[1], txy_max[0], txy_max[1], zl, tiles);
    }
    //
    int[] txy_pt  = mercatorToTileXY(lng, lat, zl);
    System.err.println(String.format("%10.5f %10.5f %2d %4d | %10.5f %10.5f %10.5f %10.5f %10.5f %10.5f | %5d < %5d > %5d | %5d < %5d > %5d",
        lng, lat, zl, tiles.size(),
        west, south, east, north, west_2, east_2,
        txy_min[0], txy_pt[0], txy_max[0],
        txy_min[1], txy_pt[1], txy_max[1]));
    //
    return tiles;
  }

  /**
   * iterate over the given indices in both directions, pushing all tiles found into the list
   */
  private static void addTilesCoveringXYRect(int tx_min, int ty_min, int tx_max, int ty_max, int zl, List<String> tiles) {
    for (int ty_q = ty_min; ty_q <= ty_max; ty_q++) {
      for (int tx_q = tx_min; tx_q <= tx_max; tx_q++) {
        String quadstr = tileXYToQuadstr(tx_q, ty_q, zl);
        // System.err.println(String.format("%-12s | %8d %8d %8d | %8d %8d %8d",
        //     quadstr, tx_min, tx_q, tx_max, ty_min, ty_q, ty_max));
        tiles.add(quadstr);
      }
    }
  }

  /****************************************************************************
   *
   * Quadkey Methods
   *
   */

  public static long quadkeyZoomBy(long quadkey, int zldiff) throws IllegalArgumentException {
    if (zldiff < 0) { throw new IllegalArgumentException("Cannot zoom in on a quadkey; we wouldn't know which one to choose"); }
    return quadkey >> zldiff;
  }

  public static long quadkeyZoom(long quadkey, int zl_old, int zl_new) {
    return quadkeyZoomBy(quadkey, zl_new - zl_old);
  }

  /**
   * Quadkey handle (Morton number) of tile with given tile x/y indices.
   */
  public static long tileXYToQuadkey(int tx, int ty) {
    long qk =
      MORTON_LUT[(ty >> 24) & 0xff] << 49 |
      MORTON_LUT[(tx >> 24) & 0xff] << 48 |
      MORTON_LUT[(ty >> 16) & 0xff] << 33 |
      MORTON_LUT[(tx >> 16) & 0xff] << 32 |
      MORTON_LUT[(ty >>  8) & 0xff] << 17 |
      MORTON_LUT[(tx >>  8) & 0xff] << 16 |
      MORTON_LUT[ ty        & 0xff] << 1  |
      MORTON_LUT[ tx        & 0xff];
    return qk;
  }

  /**
   * Tile X/Y indices of a
   */
  public static int[] quadkeyToTileXY(long qk) {
    int[] res = { uninterleaveBits(qk), uninterleaveBits(qk >> 1) };
    return res;
  }
  public static int[] quadkeyToTileXYZ(long qk, int zl) {
    int[] res = { uninterleaveBits(qk), uninterleaveBits(qk >> 1), zl };
    return res;
  }

  /* Quadkey directly up of the given quadkey */
  public static long quadkeyNeighborUp(long qk) {
    long qk_yd = (qk    & 0xAAAAAAAAAAAAAAAAL) - 2;
    return       (qk_yd & 0xAAAAAAAAAAAAAAAAL) | (qk & 0x5555555555555555L);
  }

  /* Quadkey directly left of the given quadkey */
  public static long quadkeyNeighborLeft(long qk) {
    long qk_xd = (qk    & 0x5555555555555555L) - 1;
    return       (qk_xd & 0x5555555555555555L) | (qk & 0xAAAAAAAAAAAAAAAAL); // Don't be afraid, 0xAAAAAA!
  }

  /* Quadkey to the direct right of the given quadkey */
  public static long quadkeyNeighborRight(long qk) {
    long qk_xi = (qk    | 0xAAAAAAAAAAAAAAAAL) + 1;
    return       (qk_xi & 0x5555555555555555L) | (qk & 0xAAAAAAAAAAAAAAAAL);
  }

  /* Quadkey to the direct down of the given quadkey */
  public static long quadkeyNeighborDown(long qk) {
    long qk_yi = (qk    | 0x5555555555555555L) + 2;
    return       (qk_yi & 0xAAAAAAAAAAAAAAAAL) | (qk & 0x5555555555555555L);
  }

  /**
   * Given a [tile_x, tile_y] pair, returns a 9-element array of [tile_x,
   * tile_y] pairs in the following order, right to left and up to down:
   *
   *     x-1,y-1   x,y-1    x+1,y-1
   *     x-1,y     x,y      x+1,y
   *     x-1,y+1   x,y+1    x+1,y+1
   *
   * If a tile would be off the map, a null value appears in place of the pair.
   * See neighborsList if you only want the neighbors that exist.
   *
   */
  public static Long[] quadkeyNeighborhood(long qk, int zl) {
    long max_qk = maxQuadkey(zl);
    long lf     = quadkeyNeighborLeft(qk);
    long rt     = quadkeyNeighborRight(qk);

    Long[] nbrs = {
      quadkeyNeighborUp(lf),   quadkeyNeighborUp(qk),   quadkeyNeighborUp(rt),
      lf,                      qk,                      rt,
      quadkeyNeighborDown(lf), quadkeyNeighborDown(qk), quadkeyNeighborDown(rt)
    };
    for (int idx = 0; idx < 9; idx++) {
      if (nbrs[idx] < 0 || nbrs[idx] > max_qk) {
        nbrs[idx] = null;
      }
    }
    return nbrs;
  }

  /**
   * Returns a list, in arbitrary order, of the quadkeys for the given tile and
   * all its eight neighbors.
   *
   * Most tiles have 9 neighbors.  However, quadkeys for tiles off the map are
   * not in the list; so a typical tile on the anti-meridian has only 6
   * neighbors, and there's four lonely spots in the artic and antarctic with
   * only four neighbors.
   *
   */
  public static List<Long> quadkeyNeighborhoodList(long qk, int zl) {
    List<Long> result = new ArrayList<Long>(9);

    Long[] nbrs = quadkeyNeighborhood(qk, zl);
    for (int idx = 0; idx < 9; idx++) {
      if (nbrs[idx] != null) {
        result.add(nbrs[idx]);
      }
    }
    return result;
  }

  /**
   * Quadkey of the smallest parent tile containing the given tiles. Both
   * quadkeys must refer to tiles at the same zoom level or the result is
   * meaningless.
   *
   * @param qk_1    Quadkey handle
   * @param qk_2    Quadkey handle
   * @return        quadkey of smallest tile (highest ZL) containing both
   */
  public static long[] smallestContaining(long qk_1, long qk_2) throws RuntimeException {
    for (long zldiff = 0L; zldiff <= MAX_ZOOM_LEVEL;  zldiff++) {
      if (qk_1 == qk_2) {
        long[] qk_zldiff = { qk_1, zldiff };
        return qk_zldiff;
      }
      qk_1 >>= 2;
      qk_2 >>= 2;
    }
    throw new RuntimeException("Quadkeys out of range: "+qk_1+" or "+qk_2+" have more bits than I can shift.");
  }


  /**
   * Quadkey of the smallest parent tile containing the tiles for given quadstr
   * string handles. Throws an error if tiles are not at the same zoom level
   * (result is otherwise meaningless.)
   *
   * @param quadstr_1   Quadstr string handle
   * @param quadstr_2   Quadstr string handle
   * @return            quadstr of smallest tile (highest ZL) containing both
   */
  public static String smallestContaining(String quadstr_1, String quadstr_2) throws RuntimeException {
    int  zl_1 = quadstrToZl(quadstr_1),      zl_2 = quadstrToZl(quadstr_2);
    long qk_1 = quadstrToQuadkey(quadstr_1), qk_2 = quadstrToQuadkey(quadstr_2);
    if (zl_1 != zl_2) { throw new IllegalArgumentException("Tiles must be at same zoom level for the result to make sense"); }
    //
    long[] qk_zldiff = smallestContaining(qk_1, qk_2);
    String res = quadkeyToQuadstr(qk_zldiff[0], zl_1 - (int)qk_zldiff[1]);
    System.err.println(String.format("%8d %8d %3d %3d %-12s %-12s %-12s", qk_1, qk_2, zl_1, qk_zldiff[1], quadstr_1, quadstr_2, res));
    return res;
  }

  /****************************************************************************
   *
   * Quadstring Concerns
   *
   */

  /**
   * Quadstr string handle (base-4 representation of the quadkey) for the given
   * quadkey and zoom level
   */
  public static String quadkeyToQuadstr(long qk, int zl) {
    String qk_base_4 = "00000000000000000000000000000000".concat(Long.toString(qk, 4));
    int len = qk_base_4.length();
    return qk_base_4.substring(len - zl, len);
  }

  /**
   * Quadstr string handle (base-4 representation of the quadkey) for the given
   * tile x/y indices and zoom level
   */
  public static String tileXYToQuadstr(int tx, int ty, int zl) {
    long quadkey = tileXYToQuadkey(tx, ty);
    return quadkeyToQuadstr(quadkey, zl);
  }

  /**
   * Convert base-4 representation of a quadkey to quadkey value.  Trailing
   * non-digits and spaces are removed. (Padding out with trailing characters is
   * a handy trick to force supertiles to sort ahead (* or #) or behind (~)
   * their children, and spaces help readability.)
   * @param   quadstr
   * @return  quadkey
   */
  public static long quadstrToQuadkey(String quadstr) {
    quadstr = quadstr.replaceAll("( |[^\\d]+$)", "");
    if (quadstr.equals("")) { return 0L; }
    return Long.parseLong(quadstr, 4);
  }

  /**
   * Zoom level of detail implied by the given quadstr string handle
   */
  public static int quadstrToZl(String quadstr) {
    quadstr = quadstr.replaceAll("( |[^\\d]+$)", "");
    return quadstr.length();
  }

  /**
   * X / Y indices for tile given by that quadstr string handle
   *
   * @param   quadstr
   * @return  { tile_x, tile_y }
   */
  public static int[] quadstrToTileXY(String quadstr) {
    return quadkeyToTileXY( quadstrToQuadkey(quadstr) );
  }

  /**
   * X / Y indices and zoom level for tile given by that quadstr string handle
   *
   * @param   quadstr
   * @return  { tile_x, tile_y }
   */
  public static int[] quadstrToTileXYZ(String quadstr) {
    return quadkeyToTileXYZ( quadstrToQuadkey(quadstr), quadstrToZl(quadstr) );
  }

  /****************************************************************************
   *
   * Pixel px_x / px_y Concerns
   *
   */

  /**
   * Determines map width and height in pixels at a specified zoom level --
   * that is, the number of tiles across and down. For example, at zoom level
   * 3 there are 8 tiles across and 8 down, making 2,048 pixels across and down.
   *
   * @param zl
   *            zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return The map width and height in pixels
   */
  public static int mapPixelSize(final int zl) {
    return TILE_PIXEL_SIZE << zl;
  }

  /**
   * Pixel px_x/px_y indices for upper-left pixel of given tile
   *
   * @param tile_x   Tile X index
   * @param tile_y   Tile Y index
   * @return         { px_x, px_y }
   */
  public static int[] tileXYToPixelXY(final int tile_x, final int tileY) {
    int[] px_xy = { tile_x * TILE_PIXEL_SIZE, tileY * TILE_PIXEL_SIZE };
    return px_xy;
  }

  /**
   * Tile XY coordinates of the tile containing the specified pixel.
   *
   * @param px_x    Pixel X coordinate
   * @param px_y    Pixel Y coordinate
   * @return        { tile_x, tile_y }
   */
  public static int[] pixelXYToTileXY(final int px_x, final int px_y) {
    int[] tile_xy = { px_x / TILE_PIXEL_SIZE, px_y / TILE_PIXEL_SIZE };
    return tile_xy;
  }


  /****************************************************************************
   *
   * Helpers
   *
   */

  /**
   * Determines map width and height in tiles at a specified zoom level --
   * that is, the number of tiles across and down. For example, at zoom level
   * 3 there are 8 tiles across and 8 down, 64 total
   *
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        Count of tiles in width or height
   */
  public static int mapTileSize(int zl) {
    return 1 << zl;
  }

  /**
   * Highest tile_x or tile_y index at given zoom level (rightmost / bottomest)
   *
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        The map width and height in pixels
   */
  public static int maxTileIdx(int zl) {
    return mapTileSize(zl) - 1;
  }

  /**
   * Index of highest quadkey value for given zoom level (right bottom tile)
   *
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        Highest quadkey value
   */
  public static long maxQuadkey(int zl) {
    return (0x3FFFFFFFFFFFFFFFL >> (62 - (zl*2)));
  }

  /**
   *
   * Lookup table for converting a byte's worth of tile indices to the
   * corresponding quadkey bits. To convert Y indices, shift the result left by
   * one; to convert X indices use as-is. Thanks Sean Eron Anderson
   * (https://graphics.stanford.edu/~seander/bithacks.html#InterleaveTableLookup)
   * and Harold of bitmath (http://bitmath.blogspot.com/2012_11_01_archive.html)
   */
  private static final long[] MORTON_LUT = {
    0x0000L,0x0001L,0x0004L,0x0005L,0x0010L,0x0011L,0x0014L,0x0015L, 0x0040L,0x0041L,0x0044L,0x0045L,0x0050L,0x0051L,0x0054L,0x0055L,
    0x0100L,0x0101L,0x0104L,0x0105L,0x0110L,0x0111L,0x0114L,0x0115L, 0x0140L,0x0141L,0x0144L,0x0145L,0x0150L,0x0151L,0x0154L,0x0155L,
    0x0400L,0x0401L,0x0404L,0x0405L,0x0410L,0x0411L,0x0414L,0x0415L, 0x0440L,0x0441L,0x0444L,0x0445L,0x0450L,0x0451L,0x0454L,0x0455L,
    0x0500L,0x0501L,0x0504L,0x0505L,0x0510L,0x0511L,0x0514L,0x0515L, 0x0540L,0x0541L,0x0544L,0x0545L,0x0550L,0x0551L,0x0554L,0x0555L,
    0x1000L,0x1001L,0x1004L,0x1005L,0x1010L,0x1011L,0x1014L,0x1015L, 0x1040L,0x1041L,0x1044L,0x1045L,0x1050L,0x1051L,0x1054L,0x1055L,
    0x1100L,0x1101L,0x1104L,0x1105L,0x1110L,0x1111L,0x1114L,0x1115L, 0x1140L,0x1141L,0x1144L,0x1145L,0x1150L,0x1151L,0x1154L,0x1155L,
    0x1400L,0x1401L,0x1404L,0x1405L,0x1410L,0x1411L,0x1414L,0x1415L, 0x1440L,0x1441L,0x1444L,0x1445L,0x1450L,0x1451L,0x1454L,0x1455L,
    0x1500L,0x1501L,0x1504L,0x1505L,0x1510L,0x1511L,0x1514L,0x1515L, 0x1540L,0x1541L,0x1544L,0x1545L,0x1550L,0x1551L,0x1554L,0x1555L,
    0x4000L,0x4001L,0x4004L,0x4005L,0x4010L,0x4011L,0x4014L,0x4015L, 0x4040L,0x4041L,0x4044L,0x4045L,0x4050L,0x4051L,0x4054L,0x4055L,
    0x4100L,0x4101L,0x4104L,0x4105L,0x4110L,0x4111L,0x4114L,0x4115L, 0x4140L,0x4141L,0x4144L,0x4145L,0x4150L,0x4151L,0x4154L,0x4155L,
    0x4400L,0x4401L,0x4404L,0x4405L,0x4410L,0x4411L,0x4414L,0x4415L, 0x4440L,0x4441L,0x4444L,0x4445L,0x4450L,0x4451L,0x4454L,0x4455L,
    0x4500L,0x4501L,0x4504L,0x4505L,0x4510L,0x4511L,0x4514L,0x4515L, 0x4540L,0x4541L,0x4544L,0x4545L,0x4550L,0x4551L,0x4554L,0x4555L,
    0x5000L,0x5001L,0x5004L,0x5005L,0x5010L,0x5011L,0x5014L,0x5015L, 0x5040L,0x5041L,0x5044L,0x5045L,0x5050L,0x5051L,0x5054L,0x5055L,
    0x5100L,0x5101L,0x5104L,0x5105L,0x5110L,0x5111L,0x5114L,0x5115L, 0x5140L,0x5141L,0x5144L,0x5145L,0x5150L,0x5151L,0x5154L,0x5155L,
    0x5400L,0x5401L,0x5404L,0x5405L,0x5410L,0x5411L,0x5414L,0x5415L, 0x5440L,0x5441L,0x5444L,0x5445L,0x5450L,0x5451L,0x5454L,0x5455L,
    0x5500L,0x5501L,0x5504L,0x5505L,0x5510L,0x5511L,0x5514L,0x5515L, 0x5540L,0x5541L,0x5544L,0x5545L,0x5550L,0x5551L,0x5554L,0x5555L
  };

  /**
   * X index (i.e. the even (lsb, lsb+2, ...) bits) of the given number.
   * To find the Y index, supply the quadkey shifted right by one bit.
   */
  private static int uninterleaveBits(long num) {
    num =  num                & 0x5555555555555555L;
    num = (num | (num >>  1)) & 0x3333333333333333L;
    num = (num | (num >>  2)) & 0x0F0F0F0F0F0F0F0FL;
    num = (num | (num >>  4)) & 0x00FF00FF00FF00FFL;
    num = (num | (num >>  8)) & 0x0000FFFF0000FFFFL;
    num = (num | (num >> 16)) & 0x00000000FFFFFFFFL;
    return (int) num;
  }

}
