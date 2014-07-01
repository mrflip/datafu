package datafu.pig.geo;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.QuadTree;
import com.esri.core.geometry.QuadTree.QuadTreeIterator;
import com.esri.core.geometry.SpatialReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class QuadkeyUtils {
  private static final Log LOG = LogFactory.getLog(QuadkeyUtils.class);

  public static final double MIN_LATITUDE    = -85.05112878;
  public static final double MAX_LATITUDE    = 85.05112878;
  public static final double MIN_LONGITUDE   = -180;
  public static final double MAX_LONGITUDE   = 180;

  public static final double EARTH_RADIUS    = 6378137;
  public static final double EARTH_CIRCUM    = EARTH_RADIUS * 2.0 * Math.PI;
  public static final double EARTH_HALF_CIRC = EARTH_CIRCUM / 2.0;
  public static final double FULL_RESOLUTION = EARTH_CIRCUM / 256.0;

  private static final int   MAX_ZOOM_LEVEL  = 23;
  private static final int   TILE_PIXEL_SIZE = 256;

  /****************************************************************************
   *
   * Longitude/Latitude/ZL Methods
   *
   */

  /**
   * Converts a point from longitude/latitude WGS-84 coordinates (in degrees)
   * into tile XY indices at a specified zoom level.
   *
   * @param longitude Longitude of the point, in degrees
   * @param latitude  Latitude of the point, in degrees
   * @param zl        zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return          [tile_x, tile_y]
   */
  public static int[] lnglatToTileXY(double longitude, double latitude, final int zl) {
    assert longitude <= 180 && longitude >= -180 && latitude <= 90 && latitude >= -90;
    longitude = clip(longitude, MIN_LONGITUDE, MAX_LONGITUDE);
    latitude  = clip(latitude,  MIN_LATITUDE,  MAX_LATITUDE);

    final double sinLatitude = Math.sin(latitude * Math.PI / 180);
    final double tx = (longitude + 180) / 360;
    final double ty = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

    final long   mapsize = mapTileSize(zl);
    int[]        tile_xy = {
      (int) clip(tx * mapsize, 0, mapsize - 1),
      (int) clip(ty * mapsize, 0, mapsize - 1)
    };

    System.err.println( String.format("%8d %8d %3d %8d %18.14f %18.14f %18.14f %18.14f\tlnglatToTileXY",
        tile_xy[0], tile_xy[1], zl, mapsize, longitude, latitude, tx*mapsize, ty*mapsize));
    return tile_xy;
  }

  /**
   * Converts a point from tile X, Y indices and zoom level into
   * longitude/latitude WGS-84 coordinates (in degrees) of its top left (NW)
   * corner.
   *
   * @param tx        X index of tile
   * @param ty        Y index of tile
   * @param zl        zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return          [longitude, latitude]
   */
  public static double[] tileXYToLnglat(int tx, int ty, int zl) {
    double mapsize  = mapTileSize(zl);

    double scaled_x =       (clip(tx,     0, mapsize) / mapsize) - 0.5;
    double scaled_y = 0.5 - (clip(ty,     0, mapsize) / mapsize);

    double lng      = 360 * scaled_x;
    double lat      = 90 - 360 * Math.atan(Math.exp(-scaled_y * 2 * Math.PI)) / Math.PI;

    double[] result = {lng, lat};
    return result;
  }

  /**
   * Converts a point from tile X, Y indices and zoom level into an array of the
   * WGS84 coordinates: west, south, east, north. That is:
   *
   *   minimum longitude, minimum latitude, maximum longitude, maximum latitude
   *
   * @param tx        X index of tile
   * @param ty        Y index of tile
   * @param zl        zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return          [west, south, east, north]
   */
  public static double[] tileXYToCoords(int tx, int ty, int zl) {
    long max_idx  = maxTileIdx(zl);
    // if (tx > max_idx || ty > max_idx) { return null; }
    double[] lf_up = tileXYToLnglat(tx,   ty,   zl);
    double[] rt_dn = tileXYToLnglat(tx+1, ty+1, zl);

    // [​[left, bottom], [right, top]​] ie. min_x, min_y, max_x, max_y
    double[] result = { lf_up[0], rt_dn[1], rt_dn[0], lf_up[1] };

    System.err.println( String.format("%8d %8d %3d %8d %18.14f %18.14f %18.14f %18.14f\ttileXYToCoords",
        tx, ty, zl, max_idx+1L, lf_up[0], rt_dn[1], rt_dn[0], lf_up[1] ));
    
    return result;
  }

  /**
   * Converts a point from longitude/latitude WGS-84 coordinates (in degrees)
   * into Quadkey containing that point at the specified zoom level.
   *
   * @param longitude Longitude of the point, in degrees
   * @param latitude  Latitude of the point, in degrees
   * @param zl        Zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return          Quadkey for the tile
   */
  public static long lnglatToQuadkey(double longitude, double latitude, final int zl) {
    int[] tile_xy = lnglatToTileXY(longitude, latitude, zl);
    return tileXYToQuadkey(tile_xy[0], tile_xy[1]);
  }

  /**
   * Converts the quadkey of a tile to the longitude/latitude WGS-84 coordinates
   * (in degrees) of its top left (NW) corner.
   *
   * @param quadkey   quadkey index for the tile
   * @param zl        zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return          [longitude, latitude]
   */
  public static double[] quadkeyToLnglat(long quadkey, int zl) {
    int[] tile_xy = quadkeyToTileXY(quadkey);
    return tileXYToLnglat(tile_xy[0], tile_xy[1], zl);
  }

  /**
   * Converts a point from longitude/latitude WGS-84 coordinates (in degrees)
   * into Quadstr-string coordinates at a specified zoom level.
   *
   * @param longitude Longitude of the point, in degrees
   * @param latitude  Latitude of the point, in degrees
   * @param zl        Zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return String holding base-4 representation of quadkey
   */
  public static String lnglatToQuadstr(double longitude, double latitude, final int zl) {
    int[] tile_xy = lnglatToTileXY(longitude, latitude, zl);
    return tileXYToQuadstr(tile_xy[0], tile_xy[1], zl);
  }

  /****************************************************************************
   *
   * Quadkey Methods
   *
   */

  /*
   * Thanks
   * Sean Eron Anderson (https://graphics.stanford.edu/~seander/bithacks.html#InterleaveTableLookup)
   * and Harold of bitmath (http://bitmath.blogspot.com/2012_11_01_archive.html)
   */
  public static final long[] MORTON_LUT = {
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

  private static int uninterleaveBits(long num) {
    num =  num                & 0x5555555555555555L;
    num = (num | (num >>  1)) & 0x3333333333333333L;
    num = (num | (num >>  2)) & 0x0F0F0F0F0F0F0F0FL;
    num = (num | (num >>  4)) & 0x00FF00FF00FF00FFL;
    num = (num | (num >>  8)) & 0x0000FFFF0000FFFFL;
    num = (num | (num >> 16)) & 0x00000000FFFFFFFFL;
    return (int) num;
  }

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

  public static int[] quadkeyToTileXY(long qk) {
    int[] res = { uninterleaveBits(qk), uninterleaveBits(qk >> 1) };
    return res;
  }
  public static int[] quadkeyToTileXY(long qk, int zl) {
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
    long lf = quadkeyNeighborLeft(qk);
    long rt = quadkeyNeighborRight(qk);

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

  /****************************************************************************
   *
   * Quadstring Concerns
   *
   */

  public static String quadkeyToQuadstr(long qk, int zl) {
    String qk_base_4 = "00000000000000000000000000000000".concat(Long.toString(qk, 4));
    int len = qk_base_4.length();
    return qk_base_4.substring(len - zl, len);
  }

  public static String tileXYToQuadstr(int tx, int ty, int zl) {
    long quadkey = tileXYToQuadkey(tx, ty);
    return quadkeyToQuadstr(quadkey, zl);
  }

  public static long quadstrToQuadkey(String quadstr) {
    if (quadstr.equals("")) { return 0L; }
    return Long.parseLong(quadstr, 4);
  }

  public static int[] quadstrToTileXY(String quadstr) {
    long quadkey = quadstrToQuadkey(quadstr);
    return quadkeyToTileXY(quadkey, quadstr.length());
  }

  /****************************************************************************
   *
   * pixelXY Concerns
   *
   */

  /**
   * Determines map width and height in pixels at a specified zoom level --
   * that is, the number of tiles across and down. For example, at zoom level
   * 3 there are 8 tiles across and 8 down, making 2,048 pixels across and down.
   *
   * @param zl
   *            zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return The map width and height in pixels
   */
  public static int mapPixelSize(final int zl) {
    return TILE_PIXEL_SIZE << zl;
  }

  /**
   * Converts tile XY coordinates into pixel XY coordinates of the upper-left pixel of the
   * specified tile.
   *
   * @param tileX Tile X coordinate
   * @param tileY  Tile X coordinate
   * @return [tile_x, tile_y]
   */
  public static int[] tileXYToPixelXY(final int tileX, final int tileY) {
    int[] pixelXY = {tileX * TILE_PIXEL_SIZE, tileY * TILE_PIXEL_SIZE};
    return pixelXY;
  }

  /**
   * Converts pixel XY coordinates into tile XY coordinates of the tile
   * containing the specified pixel.
   *
   * @param pixelX  Pixel X coordinate
   * @param pixelY  Pixel Y coordinate
   * @return [x_px, y_px]
   */
  public static int[] pixelXYToTileXY(final int pixelX, final int pixelY) {
    int[] tile_xy = {pixelX / TILE_PIXEL_SIZE, pixelY / TILE_PIXEL_SIZE};
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
   * @param zl
   *            zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return The map width and height in tiles
   */
  public static long mapTileSize(final int zl) {
    return 1L << zl;
  }

  /*
   * Index of highest tile in tileX or tileY for given zoom level
   *
   * @param zl
   *            zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return The map width and height in pixels
   */
  public static int maxTileIdx(int zl) {
    return (int)((1L << zl) - 1);
  }

  /*
   * Index of highest tile in tileX or tileY for given zoom level
   *
   * @param zl  zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return    The highest allowable quadkey
   */
  public static long maxQuadkey(int zl) {
    return (0x3FFFFFFFFFFFFFFFL >> (62 - (zl*2)));
  }

  /**
   * Clips a number to the specified minimum and maximum values.
   *
   * @param n
   *            The number to clip
   * @param minValue
   *            Minimum allowable value
   * @param maxValue
   *            Maximum allowable value
   * @return The clipped value.
   */
  private static double clip(final double n, final double minValue, final double maxValue) {
    return Math.min(Math.max(n, minValue), maxValue);
  }

}
