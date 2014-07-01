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

public final class QuadtileUtils {

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
    longitude = clip(longitude, MIN_LONGITUDE, MAX_LONGITUDE);
    latitude  = clip(latitude,  MIN_LATITUDE, MAX_LATITUDE);

    final double sinLatitude = Math.sin(latitude * Math.PI / 180);
    final double tx = (longitude + 180) / 360;
    final double ty = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

    final int    mapsize = mapTileSize(zl);
    int[]        tile_xy = {
      (int) clip(tx * mapsize + 0.5, 0, mapsize - 1),
      (int) clip(ty * mapsize + 0.5, 0, mapsize - 1)
    };
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

    double scaled_x =       (clip(tx,     0, mapsize - 1) / mapsize) - 0.5;
    double scaled_y = 0.5 - (clip(ty,     0, mapsize - 1) / mapsize);

    double lng      = 360 * scaled_x;
    double lat      = 90 - 360 * Math.atan(Math.exp(-scaled_y * 2 * Math.PI)) / Math.PI;

    return {lng, lat};
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
    int max_idx  = maxTileIdx(zl);
    if (tx > max_idx || ty > max_idx) { return null; }
    lf_up = tileXYToLnglat(tx,   ty,   zl);
    rt_dn = tileXYToLnglat(tx+1, ty+1, zl);

    // [​[left, bottom], [right, top]​] ie. min_x, min_y, max_x, max_y
    { lf_up[0], rt_dn[1], rt_dn[0], lf_up[1] }
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
    return tileXYToQuadkey(tile_xy[0], tile_xy[1], zl);
  }

  /**
   * Converts the quadkey of a tile to the longitude/latitude WGS-84 coordinates
   * (in degrees) of its top left (NW) corner.
   *
   * @param tx        X index of tile
   * @param ty        Y index of tile
   * @param zl        zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return          [longitude, latitude]
   */
  public static double[] quadkeyToLnglat(long quadkey, int zl) {
    int[] tile_xy = quadkeyToTileXY(quadkey, zl);
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
  public static final int[] MORTON_LUT = {
    0x0000,0x0001,0x0004,0x0005,0x0010,0x0011,0x0014,0x0015, 0x0040,0x0041,0x0044,0x0045,0x0050,0x0051,0x0054,0x0055,
    0x0100,0x0101,0x0104,0x0105,0x0110,0x0111,0x0114,0x0115, 0x0140,0x0141,0x0144,0x0145,0x0150,0x0151,0x0154,0x0155,
    0x0400,0x0401,0x0404,0x0405,0x0410,0x0411,0x0414,0x0415, 0x0440,0x0441,0x0444,0x0445,0x0450,0x0451,0x0454,0x0455,
    0x0500,0x0501,0x0504,0x0505,0x0510,0x0511,0x0514,0x0515, 0x0540,0x0541,0x0544,0x0545,0x0550,0x0551,0x0554,0x0555,
    0x1000,0x1001,0x1004,0x1005,0x1010,0x1011,0x1014,0x1015, 0x1040,0x1041,0x1044,0x1045,0x1050,0x1051,0x1054,0x1055,
    0x1100,0x1101,0x1104,0x1105,0x1110,0x1111,0x1114,0x1115, 0x1140,0x1141,0x1144,0x1145,0x1150,0x1151,0x1154,0x1155,
    0x1400,0x1401,0x1404,0x1405,0x1410,0x1411,0x1414,0x1415, 0x1440,0x1441,0x1444,0x1445,0x1450,0x1451,0x1454,0x1455,
    0x1500,0x1501,0x1504,0x1505,0x1510,0x1511,0x1514,0x1515, 0x1540,0x1541,0x1544,0x1545,0x1550,0x1551,0x1554,0x1555,
    0x4000,0x4001,0x4004,0x4005,0x4010,0x4011,0x4014,0x4015, 0x4040,0x4041,0x4044,0x4045,0x4050,0x4051,0x4054,0x4055,
    0x4100,0x4101,0x4104,0x4105,0x4110,0x4111,0x4114,0x4115, 0x4140,0x4141,0x4144,0x4145,0x4150,0x4151,0x4154,0x4155,
    0x4400,0x4401,0x4404,0x4405,0x4410,0x4411,0x4414,0x4415, 0x4440,0x4441,0x4444,0x4445,0x4450,0x4451,0x4454,0x4455,
    0x4500,0x4501,0x4504,0x4505,0x4510,0x4511,0x4514,0x4515, 0x4540,0x4541,0x4544,0x4545,0x4550,0x4551,0x4554,0x4555,
    0x5000,0x5001,0x5004,0x5005,0x5010,0x5011,0x5014,0x5015, 0x5040,0x5041,0x5044,0x5045,0x5050,0x5051,0x5054,0x5055,
    0x5100,0x5101,0x5104,0x5105,0x5110,0x5111,0x5114,0x5115, 0x5140,0x5141,0x5144,0x5145,0x5150,0x5151,0x5154,0x5155,
    0x5400,0x5401,0x5404,0x5405,0x5410,0x5411,0x5414,0x5415, 0x5440,0x5441,0x5444,0x5445,0x5450,0x5451,0x5454,0x5455,
    0x5500,0x5501,0x5504,0x5505,0x5510,0x5511,0x5514,0x5515, 0x5540,0x5541,0x5544,0x5545,0x5550,0x5551,0x5554,0x5555
  };

  private long uninterleaveBits(long num) {
    num =  num                & 0x5555555555555555;
    num = (num | (num >>  1)) & 0x3333333333333333;
    num = (num | (num >>  2)) & 0x0F0F0F0F0F0F0F0F;
    num = (num | (num >>  4)) & 0x00FF00FF00FF00FF;
    num = (num | (num >>  8)) & 0x0000FFFF0000FFFF;
    num = (num | (num >> 16)) & 0x00000000FFFFFFFF;
    return num;
  }

  public long tileXYToQuadkey(int tx, int ty) {
    long qk = 
      MORTON_LUT[(ty >> 24) && 0xff] << 49 |
      MORTON_LUT[(tx >> 24) && 0xff] << 48 |
      MORTON_LUT[(ty >> 16) && 0xff] << 33 |
      MORTON_LUT[(tx >> 16) && 0xff] << 32 |
      MORTON_LUT[(ty >>  8) && 0xff] << 17 |
      MORTON_LUT[(tx >>  8) && 0xff] << 16 |
      MORTON_LUT[ ty        && 0xff] << 1  |
      MORTON_LUT[ tx        && 0xff];
    return qk;
  }

  private int[] quadkeyToTileXY(long qk) {
    int[] res = {
      uninterleaveBits(qk),
      uninterleaveBits(qk >> 1)
    };
    return res;
  }

  /* Quadkey directly up of the given quadkey */
  static long quadkeyNeighborUp(long qk) {
    long qk_yd = (qk    & 0xAAAAAAAAAAAAAAAA) - 2;
    return       (qk_yd & 0xAAAAAAAAAAAAAAAA) | (qk & 0x5555555555555555);
  }

  /* Quadkey directly left of the given quadkey */
  static long quadkeyNeighborLeft(long qk) {
    long qk_xd = (qk    & 0x5555555555555555) - 1;
    return       (qk_xd & 0x5555555555555555) | (qk & 0xAAAAAAAAAAAAAAAA);
  }

  /* Quadkey to the direct right of the given quadkey */
  static long quadkeyNeighborRight(long qk) {
    long qk_xi = (qk    | 0xAAAAAAAAAAAAAAAA) + 1;
    return       (qk_xi & 0x5555555555555555) | (qk & 0xAAAAAAAAAAAAAAAA);
  }

  /* Quadkey to the direct down of the given quadkey */
  static long quadkeyNeighborDown(long qk) {
    long qk_yi = (qk    | 0x5555555555555555) + 2;
    return       (qk_yi & 0xAAAAAAAAAAAAAAAA) | (qk & 0x5555555555555555);
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
  public static Long[] neighborsTuple(long qk) {
    long max_qk = maxQuadkey(zl);
    long lf = quadkeyNeighborLeft(qk);
    long rt = quadkeyNeighborRight(qk);
    
    Long[] nbrs = {
      quadkeyNeighborUp(lf),   quadkeyNeighborUp(qk),   quadkeyNeighborUp(rt),
      lf,                      qk,                      rt,
      quadkeyNeighborDown(lf), quadkeyNeighborDown(qk), quadkeyNeighborDown(rt)
    };
    for (idx = 0; idx < 9; idx++) {
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
  public static List<Long> quadkeyNeighborhoodList(long qk) {
    List<Long> result = new ArrayList<Long>(9);

    Long[] nbrs = quadkeyNeighborhood(qk);
    for (idx = 0; idx < 9; idx++) {
      if (nbrs[idx] != null) {
        result.add(nbrs[idx]);
      }
    }
    return nbrs;
  }

  /****************************************************************************
   *
   * Quadstring Concerns
   *
   */

  static String quadkeyToQuadstr(long qk, int zl) {
    String qk_base_4 = Long.toString(qk, 4);
    int len = qk_base_4.length();
    return qk_base_4.substring(len - zl, len);
  }

  static String tileXYToQuadstr(int tx, int ty, int zl) {
    long quadkey = tileXYToQuadkey(tx, ty);
    return quadkeyToQuadstr(quadkey, zl);
  }

  static long quadstrToQuadkey(String quadstr) {
    return Long.parseLong(quadstr, 4);
  }

  static int[] quadstrToTileXY(String quadstr) {
    long quadkey = quadstrToQuadkey(quadstr);
    return quadkeyToTileXY(quadkey);
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
   * @param tileX
   *            Tile X coordinate
   * @param tileY
   *            Tile X coordinate
   * @param reuse
   *            An optional Point to be recycled, or null to create a new one automatically
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
  public static int mapTileSize(final int zl) {
    return 1 << zl;
  }

  /*
   * Index of highest tile in tileX or tileY for given zoom level
   *
   * @param zl
   *            zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return The map width and height in pixels
   */
  public static int maxTileIdx(int zl) {
    return mapTileSize() - 1;
  }

  /*
   * Index of highest tile in tileX or tileY for given zoom level
   *
   * @param zl  zoom level, from 1 (lowest detail) to 23 (highest detail)
   * @return    The highest allowable quadkey
   */
  public static long maxQuadkey(int zl) {
    return (0xFFFFFFFFFFFFFFFF >> (64 - zl*2))
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
