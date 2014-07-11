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
import java.util.Comparator;

import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.WktExportFlags;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;

import datafu.pig.geo.Projection;

public class Quadtile implements Comparable {
  protected final long   qk;
  protected final int    zl;
  protected final long   quadord;
  // protected Long         id_hi;
  // protected Long         id_lo;
  //
  protected Projection   proj;
  //
  // protected final int      srid;
  // protected final int      geom_type;
  //
  // memoized values
  // protected SpatialReference   spatial_reference;
  // protected Point              centroid;

  public Quadtile(long qmorton, int zoomlvl, Projection projection) {
    this.qk      = qmorton;
    this.zl      = zoomlvl;
    this.quadord = QuadtileUtils.qmortonZlToQuadord(qk, zl);
    this.proj    = projection;
  }

  public Quadtile(long _quadord, Projection projection) {
    this.quadord = _quadord;
    this.zl      = QuadtileUtils.quadordToZl(quadord);
    this.qk      = QuadtileUtils.quadordToQmorton(quadord);
    this.proj    = projection;
  }

  public Quadtile(String quadstr, Projection proj) {
    this(QuadtileUtils.quadstrToQmorton(quadstr), QuadtileUtils.quadstrToZl(quadstr), proj);
  }

  /**
   *
   * Smallest quadtile (i.e. most fine-grained zoom level) containing the given object.
   *
   * Be aware that shapes which wrap around the edge of the map by any amount -- Kiribati
   * and American Samoa sure, but also Russia and Alaska -- will end up at zoom level
   * zero. Yikes.
   */
  public static Quadtile quadtileContaining(OGCGeometry geometry, Projection projection) {
    Envelope2D env = new Envelope2D();
    geometry.getEsriGeometry().queryEnvelope2D(env);
    long[] qk_zl = QuadtileUtils.wsenToQmortonZl(env.xmin, env.ymin, env.xmax, env.ymax, projection);
    //
    return new Quadtile(qk_zl[0], (int)qk_zl[1], projection);
  }

  public static Quadtile quadtileContaining(Geometry es_geom, Projection projection) {
    return quadtileContaining(OGCGeometry.createFromEsriGeometry(es_geom, null), projection);
  }

  public static Quadtile quadtileContaining(Envelope2D envelope, Projection projection) {
    long[] qk_zl = QuadtileUtils.wsenToQmortonZl(
      envelope.xmin, envelope.ymin, envelope.xmax, envelope.ymax, projection);
    //
    return new Quadtile(qk_zl[0], (int)qk_zl[1], projection);
  }

  public static Quadtile quadtileContaining(Point pt, Projection projection) {
    return quadtileContaining(pt.getX(), pt.getY(), QuadtileUtils.MAX_ZOOM_LEVEL, projection);
  }

  public static Quadtile quadtileContaining(double lng, double lat, int zl, Projection projection) {
    long qk      = QuadtileUtils.worldToQmorton(lng, lat, zl, projection);
    //
    return new Quadtile(qk, zl, projection);
  }
  

  /* ***************************************************************************
   *
   * Accessors
   *
   */

  /**
   * @returns qmorton -- bit-interleaved long holding the i/j coordinates, such
   *   that it gives a recursive z-ordering to the space
   */
  public long   qmorton() { return qk; }

  /**
   * @returns zoomlvl -- level of detail from 0 (whole grid) to 30 (finest
   * scale).
   */
  public int    zoomlvl() { return zl; }

  /**
   *
   * Returns a key that naturally sorts in z-order of each tile's top left
   * corner, with all descendants of a tile sorting after it, even when the
   * tiles are at different zoom levels.
   *
   * <ul>
   * <li>The zl-3 quadtile # 103 has quadord value 103_0000_0000_0..._0003.</li>
   * <li>Its zl-8 descendant 10310312 has quadord= 103_1031_2000_0..._0020.</li>
   * <li>Her direct NW child 103103120 has quadord 103_1031_2000_0..._0021; the
   *     same position bits, but following its parent due to higher zoom level.</li>
   * <li>The descendant 1031_0312_0123_0123_0123_0123, at zl-28 (highest allowed),
   *     gets a quadord 103_1031_2012_3012_3012_3012_3130.</li>
   * </ul>
   *
   * In this scheme bits 64, 63, and 6 will always be zero. This will allow us
   * later to pull the UTF-8 trick and use a bit to indicate special handling.
   */
  public long quadord() {
    return this.quadord;
  }

  public Projection projection() {
    return this.proj;
  }

  /**
   * @returns bounding box of the shape in world (unprojected) coordinates
   */
  public Envelope envelope() {
    double[] wsen = wsen();
    return new Envelope(wsen[0], wsen[1], wsen[2], wsen[3]);
  }

  public double[] wsen() {
    return QuadtileUtils.qmortonToWorldWSEN(qmorton(), zoomlvl(), proj);
  }

  /**
   * @returns quadstr -- the base-4 representation of the qmorton as a string,
   *   padded so that its length matches the zoom level
   */
  public String quadstr() { return QuadtileUtils.qmortonToQuadstr(qmorton(), zoomlvl()); }

  /**
   * @returns tile_i -- the first (horizontal) coordinate on the grid, reading
   * from 0 on the left to (2^zoomlvl)-1 on the right.
   */
  public int    tileI()   { return tileIJZl()[0]; }
  /**
   * @returns tile_j -- the second (vertical) coordinate on the grid, reading
   * from 0 on the top to (2^zoomlvl)-1 on the bottom
   */
  public int    tileJ()   { return tileIJZl()[1]; }
  /**
   * @returns coordinates as an array: { tile_i, tile_j, zoomlvl }
   */
  public int[]  tileIJZl() { return QuadtileUtils.qmortonToTileIJ(qmorton()); }

  public String toString() {
    double[] coords = wsen();
    return GeometryUtils.printableMessage("%s %-10s@%2d [%4d %4d] (%6.1f %5.1f %6.1f %5.1f) %s",
      this.getClass().getSimpleName(),
      quadstr(), zoomlvl(), tileI(), tileJ(), coords[0], coords[1], coords[2], coords[3],
      envelope());
  }

  /**
   *
   * Sorts tiles by the z-order of their top left corner, placing a tile before
   * any of its descendants.
   *
   * For example, these tiles are in sorted order (shown with base-4 string
   * handles and qmortons):
   *
   * qmortons           3 @4   1 @3   4 @4   3 @3   2 @2   32 @4
   * quadstrs           0003   001    0010   003    02     0200
   * corners at zl-4:   1      4      4      12     32     32
   * quadstrs           0003   001*   0010   003*   02**   0200
   */
  public int compareTo(Object obj) {
    Quadtile other = (Quadtile)obj;
    return Long.compare(this.quadord, other.quadord);
  }

  /**
   *
   * Sorts tiles by z-order, snaking from 0000... to 3333... so that nearby
   * tiles spatially are typically nearby in z-order.
   *
   * If one tile is at a coarser zoom level than the other, it is treated as its
   * top-left descendant at the finer zoom, and precedes any of its descendants.
   *
   * These base-4 string handles are in sorted order:
   *
   *     0001  001   0010  0013  02    0200
   */
  public static class ZorderComparator implements Comparator<Quadtile> {
    /** z-order qmorton, breaking ties zoom level, coarser first */
    public int compare(Quadtile qt_a, Quadtile qt_b) {
      return Long.compare(qt_a.quadord, qt_b.quadord);
    }
  }

  /**
   * Sorts quadtiles by J (vertically) and then I (horizontally), as you read a
   * book, breaking ties by placing coarser zoom level first. This sort order is
   * useful for dumping tiles to screen.
   *
   * Note that if one tile contains the other, it precedes any of its descendants.
   */
  public static class TileIJComparator implements Comparator<Quadtile> {
    /** top-to-bottom, left-to-right, break ties by placing coarse zoom level first  */
    public int compare(Quadtile qt_a, Quadtile qt_b){
      int[] ijzl_a = qt_a.tileIJZl();
      int[] ijzl_b = qt_b.tileIJZl();
      int ti_a = ijzl_a[0], tj_a = ijzl_a[1], zl_a = ijzl_a[2];
      int ti_b = ijzl_b[0], tj_b = ijzl_b[1], zl_b = ijzl_b[2];
      // Put coordinates at same scale -- northwest corner of (1,3)@zl-3 is (4,12)@zl-5
      if (zl_a > zl_b) {
        ti_b <<= (zl_a - zl_b);
        tj_b <<= (zl_a - zl_b);
      } else {
        ti_a <<= (zl_b - zl_a);
        tj_a <<= (zl_b - zl_a);
      }
      if      (tj_a != tj_b) { return Integer.compare(tj_a, tj_b); }
      else if (ti_a != ti_b) { return Integer.compare(ti_a, ti_b); }
      else                   { return Integer.compare(zl_a, zl_b); }
    }
  }



  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   *
   * Decompose Shape
   *
   */

  /**
   *
   * List of all quadtiles at the given zoom level that intersect the object. The object
   * will lie completely within the union of the returned tiles; every returned tile
   * intersects with the object; the returned tiles do not overlap.
   *
   * @param esGeom    A shape to decompose
   * @param zl_coarse The coarsest (i.e. towards 0) tile to return. All tiles will be at or finer than this.
   * @param zl_fine   The finest  (i.e. towards 30) tile to return. All tiles will be at or coarser than this.
   * @return list of quadtiles in arbitrary order.
   */
  public DataBag decompose(OGCGeometry geom, int zl_coarse, int zl_fine) {
    return decompose(geom.getEsriGeometry(), zl_coarse, zl_fine);
  }

  public DataBag decompose(Geometry geom, int zl_coarse, int zl_fine) {
    DataBag result_bag = BagFactory.getInstance().newDefaultBag();
    addQuadsOnTile(result_bag, geom, zl_coarse, zl_fine);
    return result_bag;
  }

  protected void addQuadsOnTile(DataBag result_bag, Geometry geom, int zl_coarse, int zl_fine) {
    Envelope tile_env = envelope();
    // Portion of the shape on this tile
    Geometry geom_on_tile = GeometryEngine.intersect(geom, tile_env, null);
    //dump("%-20s %2d->%2d %3d %18s %s %s", "Decomposing", zl_coarse, zl_fine, result_bag.size(), geom, envelope, geom_on_tile);
    //
    if (geom_on_tile.isEmpty()) {
      // intersection is empty: add nothing to the list and return.
      // dump("%-20s %2d->%2d %3d %s", "No intersection", zl_coarse, zl_fine, result_bag.size(), geom_on_tile);
      return;
      //
    } else if (zoomlvl()  >= zl_fine) {
      // zl at finest limit: add tile, stop recursing
      // dump("%-20s %2d->%2d %3d", "zl meets finest limit", zl_coarse, zl_fine, result_bag.size());
      //
      addQuad(result_bag, geom_on_tile);
    } else if ((zoomlvl() >= zl_coarse) && GeometryEngine.within(tile_env, geom_on_tile, null)) {
      // completely within object: add self, return
      // dump("%-20s %2d->%2d %3d %s contains %s", "contained in shape", zl_coarse, zl_fine, result_bag.size(), geom_on_tile, getEnvelope());
      //
      addQuad(result_bag, geom_on_tile);
    } else {
      // otherwise, decompose, add those tiles.
      for (Quadtile qt: childQuadtiles()) {
        qt.addQuadsOnTile(result_bag, geom_on_tile, zl_coarse, zl_fine);
      }
    }
  }

  protected void addQuad(DataBag result_bag, Geometry es_geom) {
    Tuple  result_tup = TupleFactory.getInstance().newTuple();
    String payload = GeometryUtils.pigPayload(es_geom);
    result_tup.append(quadord());
    result_tup.append(payload);
    result_bag.add(result_tup);
  }

  public Quadtile[] childQuadtiles() {
    long[]      child_qks   = QuadtileUtils.qmortonChildren(qmorton());
    int         child_zl    = this.zl + 1;
    Quadtile[]  child_tiles = {null, null, null, null};
    for (int ci = 0; ci < child_qks.length; ci++) {
      child_tiles[ci] = new Quadtile(child_qks[ci], child_zl, proj);
    }
    return child_tiles;
  }
  //
  // /**
  //  * Quadtile at the given zoom level containing this quadtile.
  //  *
  //  * @param zl_anc    Zoom level of detail for the ancestor. Must not be finer than the tile's zoomlvl
  //  * @return quadtile at the given zoom level and which contains or equals this one.
  //  */
  // public Quadtile ancestor(int zl_anc) {
  //   long qk_anc = QuadtileUtils.qmortonAncestor(qk, zl, zl_anc);
  //   return new Quadtile(qk_anc, zl_anc, proj);
  // }
  //
  // public List<Quadtile> descendantsAt(int target_zl) {
  //   if (target_zl < zl) { throw new IllegalArgumentException("Cannot iterate descendants at a higher level: my zl "+zl+" req zl "+target_zl ); }
  //   return decompose(getEnvelope(), target_zl, target_zl);
  // };
  //
  // public static Quadtile quadtileContaining(double lng, double lat, int zoomlvl, Projection projection) {
  //   long qmorton = QuadtileUtils.worldToQmorton(lng, lat, zoomlvl, projection);
  //   return new Quadtile(qmorton, zoomlvl, projection);
  // }
  //
  // public static Quadtile quadtileContaining(double lng, double lat, Projection projection) {
  //   return quadtileContaining(lng, lat, QuadtileUtils.MAX_ZOOM_LEVEL, projection);
  // }
  //
  // public static Quadtile quadtileContaining(double lf, double dn, double rt, double up, int zoomlvl, Projection projection) {
  //   long qk_lfup = QuadtileUtils.worldToQmorton(lf, up, zoomlvl, projection);
  //   long qk_rtdn = QuadtileUtils.worldToQmorton(rt, dn, zoomlvl, projection);
  //   long[] qk_zl = QuadtileUtils.smallestContaining(qk_lfup, qk_rtdn, zoomlvl);
  //   Quadtile quadtile = new Quadtile(qk_zl[0], (int)qk_zl[1], projection);
  //   //
  //   return quadtile;
  // }
  //
  // public static Quadtile quadtileContaining(double lf, double dn, double rt, double up, Projection projection) {
  //   return quadtileContaining(lf, dn, rt, up, QuadtileUtils.MAX_ZOOM_LEVEL, projection);
  // }


  public void dump(String fmt, Object... args) {
    fmt = String.format("******\t%30s| %s", this.toString(), fmt);
    System.err.println(String.format(fmt, args));
  }

}
