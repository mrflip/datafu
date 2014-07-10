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

abstract public class PigGeometry implements Comparable {
  // TODO: make final once we figure out fuck you java
  protected long         quadord;
  protected Geometry     geom;
  protected Envelope2D   env;
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

  /* ***************************************************************************
   *
   * Accessors
   *
   */

  /**
   * @returns qmorton -- bit-interleaved long holding the i/j coordinates, such
   *   that it gives a recursive z-ordering to the space
   */
  public long   qmorton() { return QuadtileUtils.quadordToQmorton(quadord); }

  /**
   * @returns zoomlvl -- level of detail from 0 (whole grid) to 30 (finest
   * scale).
   */
  public int    zoomlvl() { return QuadtileUtils.quadordToZl(quadord); }

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

  /**
   * @returns bounding box of the shape in world (unprojected) coordinates
   */
  public Envelope2D getEnvelope() {
    return this.env;
  }

  /**
   * @returns the shape itself in world (unprojected) coordinates
   */
  public Geometry geometry() {
    return this.geom;
  }

  public Projection projection() {
    return this.proj;
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

  public double[] tile_wsen() {
    int[] tile_ijzl = tileIJZl();
    return QuadtileUtils.qmortonToWorldWSEN(qmorton(), zoomlvl(), proj);
  }

  public String toString() {
    double[] coords = tile_wsen();
    return GeometryUtils.printableMessage("%s %-10s@%2d [%4d %4d] (%6.1f %5.1f %6.1f %5.1f) %s",
      this.getClass().getSimpleName(),
      quadstr(), zoomlvl(), tileI(), tileJ(), coords[0], coords[1], coords[2], coords[3],
      env);
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
    PigGeometry other = (PigGeometry)obj;
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
  public static class ZorderComparator implements Comparator<PigGeometry> {
    /** z-order qmorton, breaking ties zoom level, coarser first */
    public int compare(PigGeometry qt_a, PigGeometry qt_b) {
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
  public static class TileIJComparator implements Comparator<PigGeometry> {
    /** top-to-bottom, left-to-right, break ties by placing coarse zoom level first  */
    public int compare(PigGeometry qt_a, PigGeometry qt_b){
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


  public void dump(String fmt, Object... args) {
    fmt = String.format("******\t%30s| %s", this.toString(), fmt);
    System.err.println(String.format(fmt, args));
  }

  //
  // public Quadtile(long qmorton, int zoomlvl, Projection projection) {
  //   this.qk = qmorton;
  //   this.zl = zoomlvl;
  //   int[] tile_ij =
  //   this.ti = tile_ij[0];
  //   this.tj = tile_ij[1];
  //   this.proj = projection;
  // }
  //
  // public Quadtile(int tile_i, int tile_j, int zoomlvl, Projection projection) {
  //   this.ti = tile_i;
  //   this.tj = tile_j;
  //   this.zl = zoomlvl;
  //   this.qk = QuadtileUtils.tileIJToQmorton(ti, tj);
  //   this.proj = projection;
  // }
  //
  // public Quadtile(String quadstr, Projection proj) {
  //   this(QuadtileUtils.quadstrToQmorton(quadstr), QuadtileUtils.quadstrToZl(quadstr), proj);
  // }

}
