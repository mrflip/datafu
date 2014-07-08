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


public class QuadDecompose extends SimpleEvalFunc<DataBag>
{
  public static final String DEFAULT_HEIGHT = "7";
  public static final int   WGS84 = 4326;

  private int               height;
  private SpatialReference  spatialReference;
  
  public QuadDecompose() {
    this(DEFAULT_HEIGHT);
  }

  public QuadDecompose(String ht_str) {
    this.height           = Integer.parseInt(ht_str);
    this.spatialReference = SpatialReference.create(WGS84);
  }

  private List<OGCGeometry> geomsFromBag(DataBag raw_geoms, QuadTree quadtree) {
    List<OGCGeometry> geoms = new ArrayList<OGCGeometry>();
    //
    int    qtr_idx = 0;
    String payload = null;
    //
    for (Tuple rawg_tup: raw_geoms) {
      try {
        payload = (String)rawg_tup.get(0);
        OGCGeometry geom = GeometryUtils.payloadToGeom(payload);
        if (geom == null){ return null; }
        //
        quadtree.insert(qtr_idx, GeometryUtils.getEnvelope2D(geom));
        geoms.add(geom);
        qtr_idx = qtr_idx+1;
      }
      catch (Exception err) {
        String msg = String.format("Loading failed in %s (%s): %s", opName(), err.getMessage(),
          GeometryUtils.printablePayload(payload));
        GeometryUtils.fuckYouError(msg, err);
        log.error(msg);
        throw new RuntimeException(msg, err);
      }
    }
    return geoms;
  }

  public DataBag call(DataBag bag_a, String pt_payload) throws JSONException {
    QuadTree          quadtree = new QuadTree(new Envelope2D(0, 0, 1280, 1280), height);
    QuadTreeIterator  qtr_iter = quadtree.getIterator();
    List<OGCGeometry> geoms_a;

    try {
      geoms_a        = geomsFromBag(bag_a, quadtree);
      Geometry pt    = GeometryUtils.payloadToGeom(pt_payload).getEsriGeometry();
      if (pt == null){ return null; }
      GeometryUtils.dump("%s", geoms_a);
      
      DataBag result_bag = BagFactory.getInstance().newDefaultBag();

      // reset iterator to the quadrant envelope that contains the point passed
      qtr_iter.resetIterator(pt, 0);
  
      int qtr_handle = qtr_iter.next();
      while (qtr_handle >= 0){
        int qtr_idx = quadtree.getElement(qtr_handle);
  
        Tuple result_tup = TupleFactory.getInstance().newTuple();
        result_tup.append(String.format("%s / %s / %d / %d",
            pt, geoms_a.get(qtr_idx),
            qtr_handle, qtr_idx));
        // result_tup.append(geoms_a.get(qtr_idx));
        
        result_bag.add(result_tup);
      
        // // we know the point and this feature are in the same quadrant, but we need to make sure the feature
        // // actually contains the point
        // if (GeometryEngine.contains(geom, pt, spatialReference)){
        //  return featureIndex;
        // }
        qtr_handle = qtr_iter.next();
      }
      
      return result_bag;
    }
    catch (Exception err) {
      String msg = String.format("Can't %s (%s): %s", opName(), err.getMessage(),
        // GeometryUtils.printablePayload(payload),
        pt_payload);
      GeometryUtils.fuckYouError(msg, err);
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    try {
      String field_name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, opName());
      return new Schema(new Schema.FieldSchema(field_name, input.getField(0).schema, DataType.BAG));
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
