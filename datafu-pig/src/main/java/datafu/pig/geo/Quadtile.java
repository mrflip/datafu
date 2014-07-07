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
  private final long qk;
  private final int  zl;
  private final int  ti;
  private final int  tj;
  private final Projection proj;
  //
  private Envelope envelope;
  private Geometry fragment;

  /* ***************************************************************************
   *
   * Constructor / Factories
   *
   */

  public Quadtile(long quadkey, int zoomlvl, Projection projection) {
    this.qk = quadkey;
    this.zl = zoomlvl;
    int[] tile_ij = QuadkeyUtils.quadkeyToTileIJ(quadkey);
    this.ti = tile_ij[0];
    this.tj = tile_ij[1];
    this.proj = projection;
  }

  public Quadtile(int tile_i, int tile_j, int zoomlvl, Projection projection) {
    this.ti = tile_i;
    this.tj = tile_j;
    this.zl = zoomlvl;
    this.qk = QuadkeyUtils.tileIJToQuadkey(ti, tj);
    this.proj = projection;
  }

  public Quadtile(String quadstr, Projection proj) {
    this(QuadkeyUtils.quadstrToQuadkey(quadstr), QuadkeyUtils.quadstrToZl(quadstr), proj);
  }

  public String quadstr() { return QuadkeyUtils.quadkeyToQuadstr(qk, zl); }
  public long   quadkey() { return qk; }
  public int    zoomlvl() { return zl; }
  public int    tileI()   { return ti; }
  public int    tileJ()   { return tj; }
  public int[]  tileIJ()  { int[] tile_ij  = { ti, tj } ;     return tile_ij;  }
  public int[]  tileIJZ() { int[] tile_ijz = { ti, tj, zl } ; return tile_ijz; }

  /**
   *
   * Quadkey zoomed to the given level:
   * if finer than the current ZL, zooms in on the top left tile until proper height is reached.
   * if coarser than the current ZL, chops off bits until the proper height is reached.
   */
  public long zoomedQuadkey(int target_zl) {
    if (target_zl > zl) {
      return qk << 2*(target_zl - zl);
    } else {
      return qk >> 2*(zl - target_zl);
    }
  }

  public int[] zoomedTileIJ(int target_zl) {
    return QuadkeyUtils.quadkeyToTileIJ(zoomedQuadkey(target_zl), target_zl);
  }

  /**
   *
   * Smallest quadtile (i.e. most fine-grained zoom level) containing the given object.
   *
   * Be aware that shapes which wrap around the edge of the map by any amount -- Kiribati
   * and American Samoa sure, but also Russia and Alaska -- will end up at zoom level
   * zero. Yikes.
   *
   */
  public static Quadtile quadtileContaining(Geometry geom, int zoomlvl, Projection proj) {
    Envelope env = new Envelope();
    geom.queryEnvelope(env);
    long qk_lfup = QuadkeyUtils.worldToQuadkey(env.getXMin(), env.getYMax(), zoomlvl, proj);
    long qk_rtdn = QuadkeyUtils.worldToQuadkey(env.getXMax(), env.getYMin(), zoomlvl, proj);
    long[] qk_zl = QuadkeyUtils.smallestContaining(qk_lfup, qk_rtdn, zoomlvl);
    Quadtile quadtile = new Quadtile(qk_zl[0], (int)qk_zl[1], proj);
    //
    // GeometryUtils.dump("%d %d %s %s %d %d | %s | %s", qk_lfup, qk_rtdn, QuadkeyUtils.quadkeyToQuadstr(qk_lfup, zoomlvl), QuadkeyUtils.quadkeyToQuadstr(qk_rtdn, zoomlvl), qk_zl[0], qk_zl[1], quadtile, geom);
    return quadtile;
  }

  public static Quadtile quadtileContaining(Geometry geom, Projection projection) {
    return quadtileContaining(geom, QuadkeyUtils.MAX_ZOOM_LEVEL, projection);
  }

  public static Quadtile quadtileContaining(double lng, double lat, int zoomlvl, Projection projection) {
    long quadkey = QuadkeyUtils.worldToQuadkey(lng, lat, zoomlvl, projection);
    return new Quadtile(quadkey, zoomlvl, projection);
  }

  public static Quadtile quadtileContaining(double lng, double lat, Projection projection) {
    return quadtileContaining(lng, lat, QuadkeyUtils.MAX_ZOOM_LEVEL, projection);
  }

  public static Quadtile quadtileContaining(double lf, double dn, double rt, double up, int zoomlvl, Projection projection) {
    long qk_lfup = QuadkeyUtils.worldToQuadkey(lf, up, zoomlvl, projection);
    long qk_rtdn = QuadkeyUtils.worldToQuadkey(rt, dn, zoomlvl, projection);
    long[] qk_zl = QuadkeyUtils.smallestContaining(qk_lfup, qk_rtdn, zoomlvl);
    Quadtile quadtile = new Quadtile(qk_zl[0], (int)qk_zl[1], projection);
    //
    return quadtile;
  }

  public static Quadtile quadtileContaining(double lf, double dn, double rt, double up, Projection projection) {
    return quadtileContaining(lf, dn, rt, up, QuadkeyUtils.MAX_ZOOM_LEVEL, projection);
  }

  public Envelope getEnvelope() {
    if (envelope != null) { return envelope; } // memoize
    double[] coords = w_s_e_n();
    this.envelope = new Envelope(coords[0],
      coords[1]   , // + 1e-6,
      coords[2]   , // - 1e-6,
      coords[3]);
    return envelope;
  }

  public double[] w_s_e_n() {
    return QuadkeyUtils.tileIJToWorldWSEN(ti, tj, zl, proj);
  }

  public String toString() {
    double[] coords = w_s_e_n();
    return String.format("%s %-10s@%2d [%4d %4d] (%6.1f %5.1f %6.1f %5.1f)",
      // this.getClass().getSimpleName(), // TODO: probably should be class, but screen space
      "QT", quadstr(), zl, ti, tj, coords[0], coords[1], coords[2], coords[3]);
  }

  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   *
   * Related Quadtiles
   *
   */

  public Quadtile[] children() {
    Long[]      child_qks   = QuadkeyUtils.quadkeyChildren(qk);
    int         child_zl    = this.zl + 1;
    Quadtile[]  child_tiles = {null, null, null, null};
    for (int ci = 0; ci < child_qks.length; ci++) {
      child_tiles[ci] = new Quadtile(child_qks[ci], child_zl, proj);
    }
    return child_tiles;
  }

  /**
   * Quadtile at the given zoom level containing this quadtile.
   *
   * @param zl_anc    Zoom level of detail for the ancestor. Must not be finer than the tile's zoomlvl
   * @return quadtile at the given zoom level and which contains or equals this one.
   */
  public Quadtile ancestor(int zl_anc) {
    long qk_anc = QuadkeyUtils.quadkeyAncestor(qk, zl, zl_anc);
    return new Quadtile(qk_anc, zl_anc, proj);
  }

  public List<Quadtile> descendantsAt(int target_zl) {
    if (target_zl < zl) { throw new IllegalArgumentException("Cannot iterate descendants at a higher level: my zl "+zl+" req zl "+target_zl ); }
    return decompose(getEnvelope(), target_zl, target_zl);
  };


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
  public static List<Quadtile> quadtilesCovering(Geometry esGeom, int zl_coarse, int zl_fine, Projection projection) {
    Quadtile start_quad = quadtileContaining(esGeom, zl_fine, projection);
    return start_quad.decompose(esGeom, zl_coarse, zl_fine);
  }
  public static List<Quadtile> quadtilesCovering(OGCGeometry geom, int zl_coarse, int zl_fine, Projection projection) {
    return quadtilesCovering(geom.getEsriGeometry(), zl_coarse, zl_fine, projection);
  }

  public List<Quadtile> decompose(Geometry geom, int zl_coarse, int zl_fine) {
    List<Quadtile> quads = new ArrayList<Quadtile>();
    addDescendantsIntersecting_(quads, geom, zl_coarse, zl_fine);
    return quads;
  }

  // make this be a bag of (quadkey, quadtile, clipped geom) objects
  protected void addDescendantsIntersecting_(List<Quadtile> quads, Geometry geom, int zl_coarse, int zl_fine) {
    // intersect the object with our envelope
    Geometry geom_on_tile = GeometryEngine.intersect(geom, getEnvelope(), null);
    //dump("%-20s %2d->%2d %3d %18s %s %s", "Decomposing", zl_coarse, zl_fine, quads.size(), geom, envelope, geom_on_tile);

    if (geom_on_tile.isEmpty()) {
      // intersection is empty: add nothing to the list and return.
      // dump("%-20s %2d->%2d %3d %s", "No intersection", zl_coarse, zl_fine, quads.size(), geom_on_tile);
      return;
      //
    } else if (this.zl  > zl_fine) {
      // zl finer than limit: zoom out to zl, add to list, return
      // dump("%-20s %2d->%2d %3d", "zl finer than limit", zl_coarse, zl_fine, quads.size());
      quads.add(ancestor(zl_fine));
      //
    } else if (this.zl == zl_fine) {
      // zl at finest limit: add self
      // dump("%-20s %2d->%2d %3d", "zl meets finest limit", zl_coarse, zl_fine, quads.size());
      quads.add(this);
      //
    } else if ((this.zl >= zl_coarse) && GeometryEngine.within(getEnvelope(), geom_on_tile, null)) {
      // completely within object: add self, return
      // NOTE: we're trusting that floating-point juju doesn't invalidate
      //   (A within intersection(A, B)) being the same as (A within B)
      // dump("%-20s %2d->%2d %3d %s contains %s", "contained in shape", zl_coarse, zl_fine, quads.size(), geom_on_tile, getEnvelope());
      quads.add(this);
      //
    } else {
      // otherwise, decompose, add those tiles.
      Quadtile[] child_tiles = children();
      child_tiles[0].addDescendantsIntersecting_(quads, geom_on_tile, zl_coarse, zl_fine);
      child_tiles[1].addDescendantsIntersecting_(quads, geom_on_tile, zl_coarse, zl_fine);
      child_tiles[2].addDescendantsIntersecting_(quads, geom_on_tile, zl_coarse, zl_fine);
      child_tiles[3].addDescendantsIntersecting_(quads, geom_on_tile, zl_coarse, zl_fine);
    }
  }

  public int compareTo(Object obj) {
    return Long.compare(qk, ((Quadtile)obj).quadkey());
  }

  public void dump(String fmt, Object... args) {
    fmt = String.format("******\t%30s| %s", this.toString(), fmt);
    System.err.println(String.format(fmt, args));
  }

  // public static class QuadkeyComparator implements Comparator<Quadtile> {
  // }

  /**
   * Sorts quadtiles by IJ (vertically) and then I (horizontally), like you read a book.
   */
  public static class TileIJComparator implements Comparator<Quadtile> {
    public final int comp_zl;

    public TileIJComparator(int comparison_zl) {
      this.comp_zl = comparison_zl;
    }

    public int compare(Quadtile qt_a, Quadtile qt_b){
      int[] tij_a = qt_a.zoomedTileIJ(comp_zl);
      int[] tij_b = qt_b.zoomedTileIJ(comp_zl);

      int i_cmp  = Integer.compare(tij_a[1], tij_b[1]);
      int j_cmp  = Integer.compare(tij_a[0], tij_b[0]);
      int zl_cmp = Integer.compare(qt_a.zoomlvl(), qt_b.zoomlvl());
      return (i_cmp == 0 ? (j_cmp == 0 ? zl_cmp : j_cmp) : i_cmp);
    }
  }


}
