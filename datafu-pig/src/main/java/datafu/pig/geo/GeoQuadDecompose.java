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


public class GeoQuadDecompose extends SimpleEvalFunc<DataBag>
{
  public static final String DEFAULT_HEIGHT = "7";
  public static final int   WGS84 = 4326;

  private int               height;
  private SpatialReference  spatialReference;

  public GeoQuadDecompose() {
    this(DEFAULT_HEIGHT);
  }
  public GeoQuadDecompose(String ht_str) {
    this.height           = Integer.parseInt(ht_str);
    this.spatialReference = SpatialReference.create(WGS84);
  }

  /**
   *
   * .
   *
   * In the future, this should accept the bag and only lazily iterate over them
   *
   */
  public static class Geometries {
    protected final List<OGCGeometry> geoms;
    public Geometries() {
      this.geoms = new ArrayList<OGCGeometry>();
    }

    /**
     * This currently scans the bag immediately, but later we may choose to do so lazily.
     */
    public void accumulateGeomPayloads(DataBag geom_payloads) {
      try {
        for (Tuple payload_tup: geom_payloads) {
          String payload = (String)payload_tup.get(0);
          OGCGeometry geom = GeometryUtils.payloadToGeom(payload);
          if (geom == null){ continue; }
          add(geom);
        }
      } catch (ExecException err) { throw new RuntimeException(err); }
    }
    public boolean add(OGCGeometry geom){
      return geoms.add(geom);
    }

    public int size(){
      return geoms.size();
    }

    public String toString() {
      // TODO: toString() on the list = bad when big; string build instead.
      String snippet = geoms.toString();
      if (snippet.length() > 200) { snippet = snippet.substring(0, 197)+"..."; }
      return String.format("%s: %s", this.getClass().getSimpleName(), snippet);
    }

    // Collection:
    //   contains(Object obj) containsAll(Collection<>clxn) equals(Object obj)
    //   isEmpty() iterator() size() toArray() toArray(T[] a)
    //
    // List
    //   get(int idx) indexOf(Object obj) lastIndexOf(Object obj) listIterator(int idx)
  }

  public static class QuadtreeGeometries extends Geometries {
    private QuadTree          quadtree;
    private QuadTreeIterator  quaditer;
    /** The tolerance used for the intersection tests -- see QuadTree.QuadTreeIterator */
    private static final double tolerance = 0.0;

    public QuadtreeGeometries() {
      this(new Envelope2D(0, 0, 1280, 1280), 8);
    }

    public QuadtreeGeometries(Envelope2D qdtr_env, int max_zl) {
      this.quadtree = new QuadTree(qdtr_env, max_zl);
      this.quaditer = quadtree.getIterator();
    }

    public boolean add(OGCGeometry geom){
      super.add(geom);
      int item_idx = this.size()-1;
      int iter_key = quadtree.insert(item_idx, GeometryUtils.getEnvelope2D(geom));
      GeometryUtils.dump("%d %d %d %d %s",
        item_idx, iter_key, quadtree.getQuad(iter_key), quadtree.getElement(iter_key), geom);
      return true;
    }

    /**
     * List of geometries potentially close to the given one.
     *
     * We don't like spooling up a list of results just so you can put them in a
     * bag right away, so don't get addicted to this method. The silliness of it
     * name bespeaks its probable ephemerality.
     */
    public List<OGCGeometry> nearish(Geometry q_geom) {
      List<OGCGeometry> result = new ArrayList<OGCGeometry>();
      // reset iterator to the quadrant envelope that contains the point passed
      quaditer.resetIterator(q_geom, 0);
      //
      int iter_key = quaditer.next();
      while (iter_key >= 0) {
        int item_idx = quadtree.getElement(iter_key);
        OGCGeometry geom = geoms.get(item_idx);
        result.add(geom);
        GeometryUtils.dump("%d %d %d %d %s %s",
          item_idx, iter_key, quadtree.getQuad(iter_key), quadtree.getElement(iter_key), geom, q_geom);
        iter_key = quaditer.next();
      }
      return result;
    }
  }

  public DataBag call(DataBag bag_a, DataBag bag_b) throws JSONException {
    DataBag result_bag = BagFactory.getInstance().newDefaultBag();
    try {
      //
      QuadtreeGeometries joiner = new QuadtreeGeometries();
      joiner.accumulateGeomPayloads(bag_a);
      //

      for (Tuple payload_tup: bag_b) {
        String payload = (String)payload_tup.get(0);
        OGCGeometry geom_b = GeometryUtils.payloadToGeom(payload);
        if (geom_b == null){ continue; }
        //
        List<OGCGeometry> joinables = joiner.nearish(geom_b.getEsriGeometry());
        //
        // 
        for (OGCGeometry joinable: joinables) {
          Tuple result_tup = TupleFactory.getInstance().newTuple();
          result_tup.append(GeometryUtils.pigPayload(joinable));
          result_tup.append(GeometryUtils.pigPayload(geom_b));
          result_bag.add(result_tup);
        }
      }
      return result_bag;
    }
    catch (Exception err) {
      String msg = String.format("Can't %s (%s)", opName(), err.getMessage());
      GeometryUtils.fuckYouError(msg, err);
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    Schema result_tuple_schema = new Schema();
    try {
      String bag_name  = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, opName());
      result_tuple_schema.add(new Schema.FieldSchema("geom_a", DataType.CHARARRAY));
      result_tuple_schema.add(new Schema.FieldSchema("geom_b", DataType.CHARARRAY));
      return new Schema(new Schema.FieldSchema(
          bag_name, result_tuple_schema, DataType.BAG));
    } catch (FrontendException err) { throw new RuntimeException(err); }
  }

  protected String opName() {
    return this.getClass().getSimpleName().replaceFirst("^Geo", "");
  }
}
      //
      // Tuple result_tup = TupleFactory.getInstance().newTuple();
      // result_tup.append(pt);
      // result_bag.add(result_tup);
      // //
      // for (Tuple feat_a_tup : bag_a) {
      //   result_bag.add(feat_a_tup);
      // }
