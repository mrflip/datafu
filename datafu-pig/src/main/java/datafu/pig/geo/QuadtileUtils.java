/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datafu.pig.geo;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import com.esri.core.geometry.NumberUtils;

import datafu.pig.geo.Projection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/****************************************************************************
 *
 * Utilities for converting among different quadtile handle representations and
 * efficiently manipulating their keys.
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
 * <li><em>qmorton</em> -- long containing interleaved x/y bits of tile indices,
 *   aka the Morton number</li>
 *
 * <li><em>quadstr</em> -- String holding the base-4 representation of the
 *   Morton number. You may insert spaces (for readability), and may add trailing
 *   non-digits on the right (a handy trick to force parent tiles to sort ahead
 *   (* or #) or behind (~) their children.)</li>
 *
 * <li><em>tile i,j</em> -- Index of tile: <code>i</code> horizontally (from west to east /
 *   left to right) and <code>j</code> vertically (from north to south / top to bottom). Note
 *   that this puts the origin at the top left corner.</li>
 *
 *</ul>
 *
 * All references to tiles must come with a zoom level of detail ('zl') (apart
 * from quadstrs, whose zoom level is implied by the count of its digits). The
 * zoom level is permitted to range down to 28, implying a 56-bit morton and
 * 72,000 trillion tiles. (Tileservers generally extend only to zl 23, so this
 * is plenty). Results are undefined for zoom levels greater than 28 -- we do
 * no checking.
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
public final class QuadtileUtils {
  private static final Log LOG = LogFactory.getLog(QuadtileUtils.class);

  /**
   * 28 levels of detail means 72,000 trillion tiles, 1-inch earth resolution.
   * It also lets us pack the left-shifted qmorton and zoom level into a long.
   */
  public static final int   MAX_ZOOM_LEVEL   = 28;

  /****************************************************************************
   *
   * GridXY Methods
   *
   */

  /**
   *
   * Tile I/J coordinates containing given grid coordinates.
   *
   * Tiles are clipped to the map size, so that 1.0 and 1.1 land on the eastmost
   * or southmost tile, and 0 or -0.1 land on the westmost or northmost.
   *
   * @param grid_x  X (horizontal) grid coordinate, from 0.0 to 1.0 left-to-right
   * @param grid_y  Y (vertical) grid coordinate, from 0.0 to 1.0 top-to-bottom
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        { tile_i, tile_j }
   */
  public static int[] gridXYToTileIJ(double grid_x, double grid_y, int zl) {
    int      mapsize  = mapTileSize(zl);
    double   tile_x   = mapsize * grid_x;
    double   tile_y   = mapsize * grid_y;
    //
    return new int[] {
      (int) NumberUtils.snap(Math.floor(tile_x), 0, mapsize-1),
      (int) NumberUtils.snap(Math.floor(tile_y), 0, mapsize-1), zl };
  }

  /**
   *
   * Grid X/Y coordinates for the tile.
   *
   * Rather than clipping to (0, mapsize-1), you may call wtih tile index ==
   * mapsize (the index of a hypothetical tile hanging off the right or bottom
   * edge of the map) allowing you to get the right/bottom edge of tile i,j
   * by calling this method on tile i+1,j+1.
   *
   * @param tile_i      I index of tile
   * @param tile_j      J index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        { grid_x, grid_y }
   */
  public static double[] tileIJToGridXY(int tile_i, int tile_j, int zl) {
    int mapsize  = mapTileSize(zl);
    tile_i = NumberUtils.snap(tile_i, 0, mapsize);
    tile_j = NumberUtils.snap(tile_j, 0, mapsize);
    //
    return new double[] { 1.0*tile_i / mapsize, 1.0*tile_j / mapsize };
  }

  /****************************************************************************
   *
   * Qmorton Methods
   *
   */

  /**
   * Morton handle (z-order index) of tile with given tile i/j indices.
   */
  public static long tileIJToQmorton(int ti, int tj) {
    // convert it 8 bits a piece: x evens, y odds.
    long qm =
      QMORTON_LUT[ ti        & 0xff]       | QMORTON_LUT[ tj        & 0xff] << 1  |
      QMORTON_LUT[(ti >>  8) & 0xff] << 16 | QMORTON_LUT[(tj >>  8) & 0xff] << 17 |
      QMORTON_LUT[(ti >> 16) & 0xff] << 32 | QMORTON_LUT[(tj >> 16) & 0xff] << 33 |
      QMORTON_LUT[(ti >> 24) & 0xff] << 48 | QMORTON_LUT[(tj >> 24) & 0xff] << 49 ;
    return qm;
  }

  /**
   * Tile I/J indices of a tile given by its qmorton
   *
   * @param  qmorton handle of a tile
   * @return { tile_i, tile_j }
   */
  public static int[] qmortonToTileIJ(long qm) {
    return new int[] { uninterleaveBits(qm), uninterleaveBits(qm >> 1) };
  }
  /**
   * Tile I/J indices and zoom level of a tile given by its qmorton and zoom level.
   * (This is provided as a convenience; the zoom level passes through).
   *
   * @param  qmorton handle of a tile
   * @return { tile_i, tile_j, zoom level }
   */
  public static int[] qmortonToTileIJ(long qm, int zl) {
    return new int[] { uninterleaveBits(qm), uninterleaveBits(qm >> 1), zl };
  }

  /**
   * Zoom qmorton out (coarser zl) by the given number of levels. A negative difference
   * will zoom in by choosing the top left child tile each time.
   *
   * Using tile with base-4 string handle 012311, for example:
   *
   *     zoom by  0  //  012311
   *     zoom by  2  //  0123
   *     zoom by -3  //  01231100
   *
   * @param qmorton
   * @param zldiff  Number of zoom levels to increase. Must not be negative
   * @return qmorton of the specified parent, or the same qmorton for zldiff=0
   */
  public static long qmortonZoomBy(long qmorton, int zldiff) {
    if (zldiff >= 0) {
      return qmorton >> 2*zldiff;
    } else {
      return qmorton << (-2*zldiff);
    }
  }

  // See http://bitmath.blogspot.com/2012/11/tesseral-arithmetic.html
  public static long I_BITS = 0x5555555555555555L;
  public static long J_BITS = 0xAAAAAAAAAAAAAAAAL;

  /** Qmorton to the direct south-east (down-right) of the given qmorton */
  public static Long qmortonNeighborSE(long qm, int zl) {
    // See http://bitmath.blogspot.com/2012/11/tesseral-arithmetic.html
    Long i_part = (((qm | J_BITS) + 1) & I_BITS) % (1 << zl);
    Long j_part = (((qm | I_BITS) + 2) & J_BITS);
    return i_part | j_part;
  }

  /** Qmortons of each chiild tile at one finer zoom level */
  public static long[] qmortonChildren(long qm) {
    long cqm = qm << 2;
    return new long[] { cqm|0, cqm|1, cqm|2, cqm|3 };
  }

  /**
   * Qmorton of the ancestor containting that tile at the given zoom level
   *
   * @param qmorton
   * @param zl
   * @param zl_anc    Zoom level of detail for the ancestor. Must not be finer than the given qmorton's zl
   * @return qmorton of the specified tile
   */
  public static long qmortonAncestor(long qmorton, int zl, int zl_anc) {
    return qmortonZoomBy(qmorton, zl_anc - zl);
  }

  /**
   * Qmorton of the smallest parent tile containing the given tiles. Both
   * qmortons must refer to tiles at the same zoom level or the result is
   * meaningless.
   *
   * @param qm_1    Morton handle
   * @param qm_2    Morton handle
   * @return        morton of smallest tile (highest zoom level) containing both
   */
  public static long[] ancestorOf(long qm_1, long qm_2, int zl) throws RuntimeException {
    for (; zl >= 0;  zl--) {
      if (qm_1 == qm_2) {
        long[] qm_zl = { qm_1, zl };
        return qm_zl;
      }
      qm_1 >>= 2;
      qm_2 >>= 2;
    }
    throw new RuntimeException("Quadtile Morton indexes out of range: "+qm_1+" or "+qm_2+" have more bits than I can shift.");
  }

  /**
   * Morton of the smallest parent tile containing the tiles for given quadstr
   * string handles. Throws an error if tiles are not at the same zoom level
   * (result is otherwise meaningless.)
   *
   * @param quadstr_1   Quadstr string handle
   * @param quadstr_2   Quadstr string handle
   * @return            quadstr of smallest tile (highest zoom level) containing both
   */
  public static String ancestorOf(String quadstr_1, String quadstr_2) throws RuntimeException {
    int  zl_1 = quadstrToZl(quadstr_1),      zl_2 = quadstrToZl(quadstr_2);
    long qm_1 = quadstrToQmorton(quadstr_1), qm_2 = quadstrToQmorton(quadstr_2);
    if (zl_1 != zl_2) { throw new IllegalArgumentException("Tiles must be at same zoom level for the result to make sense"); }
    //
    long[] qm_zl = ancestorOf(qm_1, qm_2, zl_1);
    return qmortonToQuadstr(qm_zl[0], (int)qm_zl[1]);
  }

  /**
   * iterate over the given indices in both directions, pushing all tiles found into the list
   */
  public static void addTilesCoveringIJRect(List<String> tiles, int ti_min, int tj_min, int ti_max, int tj_max, int zl) {
    for (int tj_q = tj_min; tj_q <= tj_max; tj_q++) {
      for (int ti_q = ti_min; ti_q <= ti_max; ti_q++) {
        String quadstr = tileIJToQuadstr(ti_q, tj_q, zl);
        tiles.add(quadstr);
      }
    }
  }

  /****************************************************************************
   *
   * Quadord Methods
   *
   */

  public static long QUADORD_ZL_MASK = 0x000000000000001fL; // bits 5..1
  public static long QUADORD_QM_MASK = 0x3fffffffffffffc0L; // bits 62..7 ; bits 64, 63 and 6 always 0

  /**
   *
   * Returns a key that naturally sorts in Z-order even across zoom levels -- a
   * parent (containing) tile always sorts in front of all its descendants, but
   * after its predecessor and all its predecessor's descendants. It does so by
   * left-aligning the qmorton into bits 62..7, and the zoom level into bits
   * 5..1, leaving bits 64, 63 and 6 blank.
   *
   * * The zl-3 quadtile # 123 has quadordKey() 123_0000_0000_0..._0003.
   * * Its zl-8 descendant 12310312 has quadord 123_1031_2000_0..._0020.
   * * Her direct nw child 123103120 sorts using 123_1031_2000_0..._0021 -- same
   *   position bits, but following its parent due to higher zoom level.
   * * Descendant 1231_0312_0123_0123_0123_0123, at zl-28 (highest allowed),
   *   has quadord key 123_1031_2012_3012_3012_3012_3130.
   *
   * In this scheme bits 64, 63, and 6 will always be zero. The MSB (sign bit)
   * is reserved to pull the UTF-8 trick: set it to indicate special handling.
   */
  public static long qmortonToQuadord(long qm, int zl) {
    int shift = 62 - 2*zl;
    return (qm << shift) | zl;
  }

  /**
   * Qmorton of given quadord key
   * @see qmortonToQuadord
   */
  public static long quadordToQmorton(long quadord) {
    long shift = 62L - 2*(quadord & QUADORD_ZL_MASK);
    return quadord >> shift;
  }

  /**
   * Zoom level of given quadord key
   * @see qmortonToQuadord
   */
  public static int quadordToZl(long quadord) {
    return (int)(quadord & QUADORD_ZL_MASK);
  }

  /**
   * True if first quadord key refers to a tile that is the same or contains the
   * other. Quadord 123_0000...003 (tile 27 @zl-3) contains 123_0000...004 and
   * 123_3000...004 (tiles 108 and 111 @zl-4) because they have the same prefix
   * '123' at the coarser zoom level of 3. It does not contain 120_0000...002
   * because that has a finer zoom level, and it does not contain 121_0000...003
   * because that has a different prefix (meaning they do not overlap).
   */
  public static boolean quadordAContainsB(long qo_a, long qo_b) {
    if (qo_a == qo_b) { return true; }
    int zl_a = (int)(qo_a & QUADORD_ZL_MASK), zl_b = (int)(qo_b & QUADORD_ZL_MASK);
    // a can't contain if b is at coarser zoom level
    if (zl_b < zl_a){ return false; }
    long qm_b_prefix = qo_b & (QUADORD_QM_MASK << (56 - 2*zl_a));
    //  GeometryUtils.dump("%2d %2d %s %s %s %s", zl_a, zl_b, zeroPadStr(Long.toString(qm_b_prefix, 4), 31), zeroPadStr(Long.toString(qo_a, 4), 31), zeroPadStr(Long.toString(qo_b, 4),31),  zeroPadStr(Long.toString(QUADORD_QM_MASK & (QUADORD_QM_MASK << (56 - 2*zl_a)), 4), 31));
    return (qm_b_prefix == (qo_a & QUADORD_QM_MASK));
  }

  /****************************************************************************
   *
   * Quadstrings (Base-4 Morton keys)
   *
   */

  protected static String zeroPadStr(String str, int wantlen) {
    String prefixed = "00000000000000000000000000000000".concat(str);
    int len = prefixed.length();
    return prefixed.substring(len - wantlen, len);
  }
    

  /**
   * Quadstr string handle (base-4 representation of the qmorton) for the given
   * qmorton and zoom level
   */
  public static String qmortonToQuadstr(long qm, int zl) {
    // String qm_base_4 = "00000000000000000000000000000000".concat(Long.toString(qm, 4));
    // int len = qm_base_4.length();
    // return qm_base_4.substring(len - zl, len);
    return zeroPadStr( Long.toString(qm, 4), zl );
  }


  // /**
  //  * Quadstr string handle (base-4 representation of the qmorton) for the given
  //  * qmorton and zoom level
  //  */
  // public static String qmortonToPaddedQuadstr(long qm, int zl, int target_zl) {
  //   String suffix = "";
  //   if (target_zl > zl) {
  //   //   qm <<= 2*(target_zl - zl);
  //     suffix = "********************************".substring(0, target_zl - zl);
  //   }
  //   // String qm_base_4 = Long.toString(qm, 4).concat(suffix);
  //   // return qm_base_4; // .substring(0, target_zl);
  //   return qmortonToQuadstr(qm, zl).concat(suffix).substring(0, target_zl);
  // }

  /**
   * Quadstr string handle (base-4 representation of the qmorton) for the given
   * tile i/j indices and zoom level
   */
  public static String tileIJToQuadstr(int tile_i, int tile_j, int zl) {
    long qmorton = tileIJToQmorton(tile_i, tile_j);
    return qmortonToQuadstr(qmorton, zl);
  }

  /**
   * Convert base-4 representation of a qmorton to qmorton value.  Trailing
   * non-digits and spaces are removed. (Padding out with trailing characters is
   * a handy trick to force supertiles to sort ahead (* or #) or behind (~)
   * their children, and spaces help readability.)
   * @param   quadstr
   * @return  qmorton
   */
  public static long quadstrToQmorton(String quadstr) {
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
   * I / J indices and zoom level for tile given by that quadstr string handle
   *
   * @param   quadstr
   * @return  { tile_i, tile_j, zoomlvl }
   */
  public static int[] quadstrToTileIJ(String quadstr) {
    return qmortonToTileIJ( quadstrToQmorton(quadstr), quadstrToZl(quadstr) );
  }

  /****************************************************************************
   *
   * To/From World Coordinate Methods
   *
   */

  /**
   * Tile IJ indices of the tile containing that point at given zoom level using
   * the popular tileserver Mercator projection.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        { tile_i, tile_j }
   */
  public static int[] worldToTileIJ(double lng, double lat, int zl, Projection proj) {
    double[] grid_xy = proj.lngLatToGridXY(lng, lat);
    return gridXYToTileIJ(grid_xy[0], grid_xy[1], zl);
  }

  /**
   *
   * This method nudges the southeast corner towards the northeast corner by a
   * small amount (1e-9, and only if the envelope is larger than that). Since
   * coordinates on the boundary of a tile are interpreted as belonging to the
   * north or west edge of a tile, this ensures that calling qmortonToWorldWSEN
   * and wsenToQmortonZl will round-trip.
   *
   */
  public static long[] wsenToQmortonZl(double west, double south, double east, double north, Projection proj) {
    long qm_nw = worldToQmorton(west, north, MAX_ZOOM_LEVEL, proj);
    // Cheat the southeast corner onto the tile
    if (east  - west  > 1e-9) { east  -= 1e-9; }
    if (north - south > 1e-9) { south += 1e-9; }
    long qm_se = worldToQmorton(east, south, MAX_ZOOM_LEVEL, proj);
    return ancestorOf(qm_nw, qm_se, MAX_ZOOM_LEVEL);
  }

  /**
   * Longitude/latitude WGS-84 coordinates (in degrees) of the top left (NW)
   * corner of the given tile in the popular tileserver Mercator projection.
   *
   * We allow this to be called with tile index = mapsize, i.e. the index of a
   * hypothetical tile hanging off the right or bottom edge of the map, so that
   * you can call this function on tile_i+1 or tile_j+1 to get the right/bottom
   * edge of a tile.
   *
   * @param tile_i  I index of tile
   * @param tile_j  J index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        { longitude, latitude }
   */
  public static double[] tileIJToWorld(int tile_i, int tile_j, int zl, Projection proj) {
    double[] grid_xy = tileIJToGridXY(tile_i, tile_j, zl);
    return proj.gridXYToLngLat(grid_xy[0], grid_xy[1]);
  }

  /**
   * WGS84 coordinates for tile's west, south, east, and north extents in the
   * popular tileserver Mercator projection. That is:
   *
   *   minimum longitude, minimum latitude, maximum longitude, maximum latitude
   *
   * @param tile_i  I index of tile
   * @param tile_j  J index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        [west, south, east, north]
   */
  public static double[] tileIJToWorldWSEN(int tile_i, int tile_j, int zl, Projection proj) {
    double[] min_xy = tileIJToGridXY(tile_i,   tile_j,   zl);
    double[] max_xy = tileIJToGridXY(tile_i+1, tile_j+1, zl);
    //
    // // [min_x, min_y, max_x, max_y] -> [west south east north]​
    return proj.gridXYXYToWSEN(min_xy[0], min_xy[1], max_xy[0], max_xy[1]);
  }

  /**
   * Qmorton handle of the tile containing that point at the given zoom level in
   * the popular tileserver Mercator projection.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param zl      zoom level, from 1 (lowest detail) to 30 (highest detail)
   * @param proj    Projection to convert between world and grid coordinates
   * @return        Qmorton handle of the tile
   */
  public static long worldToQmorton(double lng, double lat, final int zl, Projection proj) {
    int[] tile_ij = worldToTileIJ(lng, lat, zl, proj);
    return tileIJToQmorton(tile_ij[0], tile_ij[1]);
  }

  /**
   * World coordinates of the tile's top left corner
   *
   * @param qmorton qmorton handle of the tile
   * @param zl      zoom level, from 1 (lowest detail) to 30 (highest detail)
   * @param proj    Projection to convert between world and grid coordinates
   * @return        { longitude, latitude }
   */
  public static double[] qmortonToWorld(long qmorton, int zl, Projection proj) {
    int[] tile_ij = qmortonToTileIJ(qmorton);
    return tileIJToWorld(tile_ij[0], tile_ij[1], zl, proj);
  }

  /**
   * WGS84 coordinates for tile's west, south, east, and north extents in the
   * popular tileserver Mercator projection. That is:
   *
   *   minimum longitude, minimum latitude, maximum longitude, maximum latitude
   *
   * @param qmorton Morton key of tile
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        [west, south, east, north]
   */
  public static double[] qmortonToWorldWSEN(long qmorton, int zl, Projection proj) {
    int[] tile_ij = qmortonToTileIJ(qmorton);
    return tileIJToWorldWSEN(tile_ij[0], tile_ij[1], zl, proj);
  }

  /**
   * Coordinates of the center of the tile -- useful for finding the child envelopes in one go.
   */
  public static double[] qmortonToTileCenter(long qmorton, int zl, Projection proj) {
    // pop down one zoom level
    qmorton <<= 2; zl += 1;
    // pop over to the right and down, get coordinates
    int[] tile_ij = qmortonToTileIJ(qmorton);
    return tileIJToWorld(tile_ij[0] + 1, tile_ij[1] + 1, zl, proj);
  }

  /**
   * Quadstr string handle of tile at the given zoom level containing that point
   *
   * @param lng     Longitude of the point, in degrees
   * @param lat     Latitude of the point, in degrees
   * @param zl      Zoom level, from 1 (lowest detail) to 30 (highest detail)
   * @param proj    Projection to convert between world and grid coordinates
   * @return String holding base-4 representation of qmorton
   */
  public static String worldToQuadstr(double lng, double lat, final int zl, Projection proj)
  {
    int[] tile_ij = worldToTileIJ(lng, lat, zl, proj);
    return tileIJToQuadstr(tile_ij[0], tile_ij[1], zl);
  }

  /**
   * World coordinates of the tile's top left corner
   *
   * @param quadstr quadstr string handle of the tile
   * @param proj    Projection to convert between world and grid coordinates
   * @return        { longitude, latitude }
   */
  public static double[] quadstrToWorld(String quadstr, Projection proj) {
    int[] tile_ij_zl = quadstrToTileIJ(quadstr);
    return tileIJToWorld(tile_ij_zl[0], tile_ij_zl[1], tile_ij_zl[2], proj);
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
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        Count of tiles in width or height
   */
  public static int mapTileSize(int zl) {
    return 1 << zl;
  }

  /**
   * Highest tile_i or tile_j index at given zoom level (rightmost / bottomest)
   *
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        The map width and height in tiles
   */
  public static int maxTileIdx(int zl) {
    return mapTileSize(zl) - 1;
  }

  /**
   * Index of highest qmorton value for given zoom level (right bottom tile)
   *
   * @param zl      zoom level, from 1 (lowest detail) to 28 (highest detail)
   * @return        Highest qmorton value
   */
  public static long maxQmorton(int zl) {
    return (0x3FFFFFFFFFFFFFFFL >>> (62 - (zl*2)));
  }

  /**
   *
   * Lookup table for converting a byte's worth of tile indices to the
   * corresponding qmorton bits. To convert J indices, shift the result left by
   * one; to convert I indices use as-is. Thanks Sean Eron Anderson
   * (https://graphics.stanford.edu/~seander/bithacks.html#InterleaveTableLookup)
   * and Harold of bitmath (http://bitmath.blogspot.com/2012_11_01_archive.html)
   */
  private static final long[] QMORTON_LUT = {
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
   * I index (i.e. the even (lsb, lsb+2, ...) bits) of the given number.
   * To find the J index, supply the qmorton shifted right by one bit.
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
