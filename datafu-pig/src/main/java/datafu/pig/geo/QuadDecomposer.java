/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package datafu.pig.geo;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

import org.apache.pig.data.DataType;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.pig.geo.GeometryUtils;
import datafu.pig.util.SimpleEvalFunc;
import com.google.common.base.CaseFormat;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.QuadTree;
import com.esri.core.geometry.QuadTree.QuadTreeIterator;
import com.esri.core.geometry.SpatialReference;

import org.json.JSONException;

import datafu.pig.geo.GeometryUtils;

public class QuadDecomposer {

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
  public static DataBag decompose(PigGeometry shape, int zl_coarse, int zl_fine) {
    return decompose(shape.geometry().getEsriGeometry(), shape.qmorton(), shape.zoomlvl(), zl_coarse, zl_fine, shape.projection());
  }

  public static DataBag decompose(Geometry geom, long qmorton, int zl, int zl_coarse, int zl_fine, Projection proj) {
    DataBag result_bag = BagFactory.getInstance().newDefaultBag();
    addQuadsOnTile(result_bag, geom, qmorton, zl, zl_coarse, zl_fine, proj);
    return result_bag;
  }

  protected static Envelope quadtileEnvelope(long qmorton, int zl, Projection proj) {
    double[] wsen = QuadtileUtils.qmortonToWorldWSEN(qmorton, zl, proj);
    return new Envelope(wsen[0], wsen[1], wsen[2], wsen[3]);
  }

  protected static void addQuad(DataBag result_bag, long quadord, Geometry es_geom) {
    Tuple  result_tup = TupleFactory.getInstance().newTuple();
    String payload = GeometryUtils.pigPayload(es_geom);
    result_tup.append(quadord);
    result_tup.append(payload);
    result_bag.add(result_tup);
  }

  protected static void addQuadsOnTile(DataBag result_bag, Geometry geom, long qmorton, int zl, int zl_coarse, int zl_fine, Projection proj) {
    // Portion of the shape on this tile
    Envelope tile_env = quadtileEnvelope(qmorton, zl, proj);
    Geometry geom_on_tile = GeometryEngine.intersect(geom, tile_env, null);
    //dump("%-20s %2d->%2d %3d %18s %s %s", "Decomposing", zl_coarse, zl_fine, result_bag.size(), geom, envelope, geom_on_tile);
    //
    if (geom_on_tile.isEmpty()) {
      // intersection is empty: add nothing to the list and return.
      // dump("%-20s %2d->%2d %3d %s", "No intersection", zl_coarse, zl_fine, result_bag.size(), geom_on_tile);
      return;
      //
    } else if (zl  >= zl_fine) {
      // zl at finest limit: add tile, stop recursing
      // dump("%-20s %2d->%2d %3d", "zl meets finest limit", zl_coarse, zl_fine, result_bag.size());
      //
      addQuad(result_bag, QuadtileUtils.qmortonZlToQuadord(qmorton, zl), geom);
    } else if ((zl >= zl_coarse) && GeometryEngine.within(tile_env, geom_on_tile, null)) {
      // completely within object: add self, return
      // dump("%-20s %2d->%2d %3d %s contains %s", "contained in shape", zl_coarse, zl_fine, result_bag.size(), geom_on_tile, getEnvelope());
      //
      addQuad(result_bag, QuadtileUtils.qmortonZlToQuadord(qmorton, zl), geom);
    } else {
      // otherwise, decompose, add those tiles.
      long[] child_qms = QuadtileUtils.qmortonChildren(qmorton);
      addQuadsOnTile(result_bag, geom_on_tile, child_qms[0], zl+1, zl_coarse, zl_fine, proj);
      addQuadsOnTile(result_bag, geom_on_tile, child_qms[1], zl+1, zl_coarse, zl_fine, proj);
      addQuadsOnTile(result_bag, geom_on_tile, child_qms[2], zl+1, zl_coarse, zl_fine, proj);
      addQuadsOnTile(result_bag, geom_on_tile, child_qms[3], zl+1, zl_coarse, zl_fine, proj);
    }
  }

  // public PigGeometry[] children() {
  //   Long[]      child_qks   = QuadtileUtils.qmortonChildren(qk);
  //   int         child_zl    = this.zl + 1;
  //   PigGeometry[]  child_tiles = {null, null, null, null};
  //   for (int ci = 0; ci < child_qks.length; ci++) {
  //     child_tiles[ci] = new PigGeometry(child_qks[ci], child_zl, proj);
  //   }
  //   return child_tiles;
  // }
  //
  // /**
  //  * PigGeometry at the given zoom level containing this quadtile.
  //  *
  //  * @param zl_anc    Zoom level of detail for the ancestor. Must not be finer than the tile's zoomlvl
  //  * @return quadtile at the given zoom level and which contains or equals this one.
  //  */
  // public PigGeometry ancestor(int zl_anc) {
  //   long qk_anc = QuadtileUtils.qmortonAncestor(qk, zl, zl_anc);
  //   return new PigGeometry(qk_anc, zl_anc, proj);
  // }
  //
  // public List<PigGeometry> descendantsAt(int target_zl) {
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

}
