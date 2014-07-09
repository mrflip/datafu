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
 * <li><em>tile i,j</em> -- Index of tile: <code>i</code> horizontally (from west to east /
 *   left to right) and <code>j</code> vertically (from north to south / top to bottom). Note
 *   that this puts the origin at the top left corner.</li>
 *
 *</ul>
 *
 * All references to tiles must come with a zoom level of detail ('zl') (apart
 * from quadstrs, whose zoom level is implied by the count of its digits). The
 * zoom level is permitted to range down to 31, implying a 62-bit quadkey and
 * four million trillion tiles. (Tileservers generally extend only to zl 23).
 * Results are undefined for zoom level 32.
 *
 * The mercator maptile i/j/zl scheme is used by all popular online tile servers
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

  /**
   * 28 levels of detail means 72,000 trillion tiles, 1-inch earth resolution.
   * It also lets us pack the left-shifted quadkey and zoom level into a long.
   */
  public static final int   MAX_ZOOM_LEVEL   = 28;

  // The arctan/log/tan/sinh business gives slight loss of precision. We could
  // live with that on the whole, but it can push the boundary of a tile onto
  // the one above it so lnglatToTileIJ(tileIJToLnglat(foo)) != foo. Adding
  // this 1-part-per-billion fudge stabilized things; with this, no edge will
  // ever dance across tiles. Each of the following equivalents to the code
  // here or in tileIJToLnglat work, and none performed better.
  //
  // double lat_rad = Math.toRadians(lat);           // OSM version
  // double tj2     = mapsize * (1 - Math.log( Math.tan(lat_rad)  + (1/Math.cos(lat_rad)) )/Math.PI) / 2.0;
  // double sin_lat = Math.sin(lat * Math.PI / 180); // Bing version
  // double tj3     = mapsize * (0.5 - Math.log((1 + sin_lat) / (1 - sin_lat)) / (4 * Math.PI));
  // double lat2    = 180/Math.PI*Math.atan(Math.sinh(Math.PI * (1 - 2.0*tj/mapsize)));
  //
  public static final double EDGE_FUDGE      = 1e-10;

  /****************************************************************************
   *
   * GridXY Methods
   *
   */

  public static int[] gridXYToTileIJ(double gx, double gy, int zl) {
    int      mapsize  = mapTileSize(zl);
    double   tile_x   = mapsize * gx;
    double   tile_y   = mapsize * gy;
    //
    return new int[] {
      (int) NumberUtils.snap(Math.floor(tile_x), 0, mapsize-1),
      (int) NumberUtils.snap(Math.floor(tile_y), 0, mapsize-1) };
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
   * @param ti      I index of tile
   * @param tj      J index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        { longitude, latitude }
   */
  public static double[] tileIJToGridXY(int ti, int tj, int zl) {
    int mapsize  = mapTileSize(zl);
    ti = NumberUtils.snap(ti, 0, mapsize);
    tj = NumberUtils.snap(tj, 0, mapsize);
    //
    return new double[] { 1.0*ti / mapsize, 1.0*tj / mapsize };
  }

  /****************************************************************************
   *
   * Quadkey Methods
   *
   */

  /**
   * Quadkey handle (Morton number) of tile with given tile i/j indices.
   */
  public static long tileIJToQuadkey(int ti, int tj) {
    // convert it 8 bits a piece: x evens, y odds. ">>>" means logical bit shift.
    long qk =
      MORTON_LUT[ ti         & 0xff]       | MORTON_LUT[ tj         & 0xff] << 1  |
      MORTON_LUT[(ti >>>  8) & 0xff] << 16 | MORTON_LUT[(tj >>>  8) & 0xff] << 17 |
      MORTON_LUT[(ti >>> 16) & 0xff] << 32 | MORTON_LUT[(tj >>> 16) & 0xff] << 33 |
      MORTON_LUT[(ti >>> 24) & 0xff] << 48 | MORTON_LUT[(tj >>> 24) & 0xff] << 49 ;
    return qk;
  }

  /**
   * Tile I/J indices of a tile given by its quadkey
   *
   * @param  quadkey handle of a tile
   * @return { tile_i, tile_j }
   */
  public static int[] quadkeyToTileIJ(long qk) {
    return new int[] { uninterleaveBits(qk), uninterleaveBits(qk >>> 1) };
  }
  /**
   * Tile I/J indices and zoom level of a tile given by its quadkey and zoom level.
   * (This is provided as a convenience; the zoom level passes through).
   *
   * @param  quadkey handle of a tile
   * @return { tile_i, tile_j, zoom level }
   */
  public static int[] quadkeyToTileIJ(long qk, int zl) {
    return new int[] { uninterleaveBits(qk), uninterleaveBits(qk >>> 1), zl };
  }

  /**
   * Zoom quadkey out (coarser zl) by the given number of levels. A negative difference
   * will zoom in by choosing the top left child tile each time.
   *
   * Using tile with base-4 string handle 012311, for example:
   *
   *     zoom by  0  //  012311
   *     zoom by  2  //  0123
   *     zoom by -3  //  01231100
   *
   * @param quadkey
   * @param zldiff  Number of zoom levels to increase. Must not be negative
   * @return quadkey of the specified parent, or the same quadkey for zldiff=0
   */
  public static long quadkeyZoomBy(long quadkey, int zldiff) {
    if (zldiff >= 0) { 
      return quadkey >>> 2*zldiff;
    } else {
      return quadkey << (-2*zldiff);
    }
  }

  /**
   *
   * Returns a key that naturally sorts in Z-order even across zoom levels -- a
   * parent (containing) tile always sorts in front of all its descendants, but
   * after its predecessor and all its predecessor's descendants. It does so by
   * left-aligning the quadkey into bits 62..7, and the zoom level into bits
   * 5..1, leaving bits 64, 63 and 6 blank.
   *
   * * The zl-3 quadtile # 103 has quadordKey() 103_0000_0000_0..._0003.
   * * Its zl-8 descendant 10310312 has quadord 103_1031_2000_0..._0020.
   * * Her direct nw child 103103120 sorts using 103_1031_2000_0..._0021 -- same
   *   position bits, but following its parent due to higher zoom level.   
   * * Descendant 1031_0312_0123_0123_0123_0123, at zl-28 (highest allowed),
   *   has quadord key 103_1031_2012_3012_3012_3012_3130.
   * 
   * In this scheme bits 64, 63, and 6 will always be zero. The MSB (sign bit)
   * is reserved to pull the UTF-8 trick: set it to indicate special handling.
   */
  public static long quadordQuadkey(long qk, int zl) {
    int shift = 2*(31 - zl);
    return (qk << shift | zl);
  }
  
  /**
   * Turns a quadord key into a quadkey
   * @see quadordQuadkey
   */
  public static long[] quadkeyZlFromQuadord(long quadord) {
    long zl = quadord & 0x1fL;
    return new long[] { quadord >>> zl, zl };
  }

  public static long QUADORD_ZL_MASK = 0x000000000000001FL;
  public static long QUADORD_QK_MASK = 0x3fffffffffffffc0L; // "03333333333333333333333333333000"
  public static boolean quadordAContainsB(long sk_a, long sk_b) {
    int zl_a = (int)(sk_a & QUADORD_ZL_MASK), zl_b = (int)(sk_b & QUADORD_ZL_MASK);
    // a can't contain if b is at coarser zoom level
    if (zl_b < zl_a){ return false; }
    long qk_b_prefix = sk_b & (QUADORD_QK_MASK << (2*(zl_b - zl_a)));
    return (qk_b_prefix == (sk_a & QUADORD_QK_MASK));
  }

  /**
   * Quadkey of the ancestor containting that tile at the given zoom level
   *
   * @param quadkey
   * @param zl
   * @param zl_anc    Zoom level of detail for the ancestor. Must not be finer than the given quadkey's zl
   * @return quadkey of the specified tile
   */
  public static long quadkeyAncestor(long quadkey, int zl, int zl_anc) {
    return quadkeyZoomBy(quadkey, zl_anc - zl);
  }

  /** Quadkeys of each chiild tile at one finer zoom level */
  public static Long[] quadkeyChildren(long qk) {
    Long cqk = qk << 2;
    return new Long[] { cqk|0, cqk|1, cqk|2, cqk|3 };
  }

  /** Quadkey directly left of the given quadkey */
  public static long quadkeyNeighborLeft(long qk) {
    long qk_id = (qk    & 0x5555555555555555L) - 1;
    return       (qk_id & 0x5555555555555555L) | (qk & 0xAAAAAAAAAAAAAAAAL);
  }

  /** Quadkey directly up of the given quadkey */
  public static long quadkeyNeighborUp(long qk) {
    long qk_jd = (qk    & 0xAAAAAAAAAAAAAAAAL) - 2;
    return       (qk_jd & 0xAAAAAAAAAAAAAAAAL) | (qk & 0x5555555555555555L);
  }

  /** Quadkey to the direct right of the given quadkey */
  public static long quadkeyNeighborRight(long qk) {
    long qk_iu = (qk    | 0xAAAAAAAAAAAAAAAAL) + 1;
    return       (qk_iu & 0x5555555555555555L) | (qk & 0xAAAAAAAAAAAAAAAAL);
  }

  /** Quadkey to the direct down of the given quadkey */
  public static long quadkeyNeighborDown(long qk) {
    long qk_ju = (qk    | 0x5555555555555555L) + 2;
    return       (qk_ju & 0xAAAAAAAAAAAAAAAAL) | (qk & 0x5555555555555555L);
  }


  /**
   * Given a [tile_i, tile_j] pair, returns a 9-element array of [tile_i,
   * tile_j] pairs in the following order, right to left and up to down:
   *
   *     i-1,j-1   i,j-1    i+1,j-1
   *     i-1,j     i,j      i+1,j
   *     i-1,j+1   i,j+1    i+1,j+1
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
   * not in the list; so a tjpical tile on the anti-meridian has only 6
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
   * @return        quadkey of smallest tile (highest ZL) containing both,
   */
  public static long[] smallestContaining(long qk_1, long qk_2, int zl) throws RuntimeException {
    for (; zl >= 0;  zl--) {
      if (qk_1 == qk_2) {
        long[] qk_zl = { qk_1, zl };
        return qk_zl;
      }
      qk_1 >>>= 2;
      qk_2 >>>= 2;
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
    long[] qk_zl = smallestContaining(qk_1, qk_2, zl_1);
    String res = quadkeyToQuadstr(qk_zl[0], (int)qk_zl[1]);
    return res;
  }

  /**
   * iterate over the given indices in both directions, pushing all tiles found into the list
   */
  public static void addTilesCoveringIJRect(int ti_min, int tj_min, int ti_max, int tj_max, int zl, List<String> tiles) {
    for (int tj_q = tj_min; tj_q <= tj_max; tj_q++) {
      for (int ti_q = ti_min; ti_q <= ti_max; ti_q++) {
        String quadstr = tileIJToQuadstr(ti_q, tj_q, zl);
        tiles.add(quadstr);
      }
    }
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


  // /**
  //  * Quadstr string handle (base-4 representation of the quadkey) for the given
  //  * quadkey and zoom level
  //  */
  // public static String quadkeyToPaddedQuadstr(long qk, int zl, int target_zl) {
  //   String suffix = "";
  //   if (target_zl > zl) {
  //   //   qk <<= 2*(target_zl - zl);
  //     suffix = "********************************".substring(0, target_zl - zl);
  //   }
  //   // String qk_base_4 = Long.toString(qk, 4).concat(suffix);
  //   // return qk_base_4; // .substring(0, target_zl);
  //   return quadkeyToQuadstr(qk, zl).concat(suffix).substring(0, target_zl);
  // }

  /**
   * Quadstr string handle (base-4 representation of the quadkey) for the given
   * tile i/j indices and zoom level
   */
  public static String tileIJToQuadstr(int ti, int tj, int zl) {
    long quadkey = tileIJToQuadkey(ti, tj);
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
   * I / J indices and zoom level for tile given by that quadstr string handle
   *
   * @param   quadstr
   * @return  { tile_i, tile_j, zoomlvl }
   */
  public static int[] quadstrToTileIJ(String quadstr) {
    return quadkeyToTileIJ( quadstrToQuadkey(quadstr), quadstrToZl(quadstr) );
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
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        { tile_i, tile_j }
   */
  public static int[] worldToTileIJ(double lng, double lat, int zl, Projection proj) {
    double[] grid_xy = proj.lngLatToGridXY(lng, lat);
    grid_xy[1] += (EDGE_FUDGE/(1<<zl));  // See note above EDGE_FUDGE
    return gridXYToTileIJ(grid_xy[0], grid_xy[1], zl);
  }

  /**
   * Longitude/latitude WGS-84 coordinates (in degrees) of the top left (NW)
   * corner of the given tile in the popular tileserver Mercator projection.
   *
   * We allow this to be called with tile index = mapsize, i.e. the index of a
   * hypothetical tile hanging off the right or bottom edge of the map, so that
   * you can call this function on ti+1 or tj+1 to get the right/bottom edge of
   * a tile.
   *
   * @param ti      I index of tile
   * @param tj      J index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        { longitude, latitude }
   */
  public static double[] tileIJToWorld(int ti, int tj, int zl, Projection proj) {
    double[] grid_xy = tileIJToGridXY(ti, tj, zl);
    return proj.gridXYToLngLat(grid_xy[0], grid_xy[1]);
  }


  /**
   * Quadkey handle of the tile containing that point at the given zoom level in
   * the popular tileserver Mercator projection.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param zl      zoom level, from 1 (lowest detail) to 30 (highest detail)
   * @param proj    Projection to convert between world and grid coordinates
   * @return        Quadkey handle of the tile
   */
  public static long worldToQuadkey(double lng, double lat, final int zl, Projection proj) {
    int[] tile_ij = worldToTileIJ(lng, lat, zl, proj);
    return tileIJToQuadkey(tile_ij[0], tile_ij[1]);
  }

  public static long worldToQuadkey(double lng, double lat, Projection proj) {
    return worldToQuadkey(lng, lat, MAX_ZOOM_LEVEL, proj);
  }

  /**
   * World coordinates of the tile's top left corner
   *
   * @param quadkey quadkey handle of the tile
   * @param zl      zoom level, from 1 (lowest detail) to 30 (highest detail)
   * @param proj    Projection to convert between world and grid coordinates
   * @return        { longitude, latitude }
   */
  public static double[] quadkeyToWorld(long quadkey, int zl, Projection proj) {
    int[] tile_ij = quadkeyToTileIJ(quadkey);
    return tileIJToWorld(tile_ij[0], tile_ij[1], zl, proj);
  }

  /**
   * Quadstr string handle of tile at the given zoom level containing that point
   *
   * @param lng     Longitude of the point, in degrees
   * @param lat     Latitude of the point, in degrees
   * @param zl      Zoom level, from 1 (lowest detail) to 30 (highest detail)
   * @param proj    Projection to convert between world and grid coordinates
   * @return String holding base-4 representation of quadkey
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

  /**
   * WGS84 coordinates for tile's west, south, east, and north extents in the
   * popular tileserver Mercator projection. That is:
   *
   *   minimum longitude, minimum latitude, maximum longitude, maximum latitude
   *
   * @param ti      I index of tile
   * @param tj      J index of tile
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        [west, south, east, north]
   */
  public static double[] tileIJToWorldWSEN(int ti, int tj, int zl, Projection proj) {
    int max_idx  = maxTileIdx(zl);
    if (ti > max_idx || tj > max_idx){ return null; }
    double[] lf_up = tileIJToWorld(ti,   tj,   zl, proj);
    double[] rt_dn = tileIJToWorld(ti+1, tj+1, zl, proj);

    // [left, bottom, right, top]​ -- [min_x, min_y, max_x, max_y]
    return new double[] { lf_up[0],
                          rt_dn[1] , // +EDGE_FUDGE,
                          rt_dn[0] , // -EDGE_FUDGE,
                          lf_up[1] };
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
   * Highest tile_i or tile_j index at given zoom level (rightmost / bottomest)
   *
   * @param zl      zoom level, from 1 (lowest detail) to 31 (highest detail)
   * @return        The map width and height in tiles
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
    return (0x3FFFFFFFFFFFFFFFL >>> (62 - (zl*2)));
  }

  /**
   *
   * Lookup table for converting a byte's worth of tile indices to the
   * corresponding quadkey bits. To convert J indices, shift the result left by
   * one; to convert I indices use as-is. Thanks Sean Eron Anderson
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
   * I index (i.e. the even (lsb, lsb+2, ...) bits) of the given number.
   * To find the J index, supply the quadkey shifted right by one bit.
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
