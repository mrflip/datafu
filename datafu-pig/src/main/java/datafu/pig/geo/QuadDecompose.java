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

import org.apache.pig.data.DataType;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.pig.geo.GeometryUtils;
import datafu.pig.util.SimpleEvalFunc;

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


public class QuadDecompose extends SimpleEvalFunc<String>
{
  
  QuadTree quadTree;
  QuadTreeIterator quadTreeIter;

  private int height;
  public static final String DEFAULT_HEIGHT = "6";

  public static final int   WGS84 = 4326;
  private SpatialReference spatialReference;
    
  
  public QuadDecompose() {
    this(DEFAULT_HEIGHT);
  }

  public QuadDecompose(String ht_str) {
    this.height           = Integer.parseInt(ht_str);
    this.spatialReference = SpatialReference.create(WGS84);
  }

  private void buildQuadTree(Geometry geom){
    quadTree = new QuadTree(new Envelope2D(-180, -90, 180, 90), height);

    Envelope envelope = new Envelope();
    // for (int i=0;i<featureClass.features.length;i++){
    // featureClass.features[i].
      geom.queryEnvelope(envelope);
      quadTree.insert(
        0,
          new Envelope2D(envelope.getXMin(), envelope.getYMin(), envelope.getXMax(), envelope.getYMax()));
      //}

    quadTreeIter = quadTree.getIterator();
  }

  private int queryQuadTree(Point pt, Geometry geom)
  {
    // reset iterator to the quadrant envelope that contains the point passed
    quadTreeIter.resetIterator(pt, 0);

    int elmHandle = quadTreeIter.next();

    while (elmHandle >= 0){
      int featureIndex = quadTree.getElement(elmHandle);

      // we know the point and this feature are in the same quadrant, but we need to make sure the feature
      // actually contains the point
      if (GeometryEngine.contains(geom, pt, spatialReference)){
       return featureIndex;
      }

       elmHandle = quadTreeIter.next();
    }
    return -1; // feature not found
  }

  public String call(String geo_json) throws JSONException {
    try {
      OGCGeometry ogcObj   = OGCGeometry.fromGeoJson(geo_json);
      Geometry    geom     = ogcObj.getEsriGeometry();

      buildQuadTree(geom);
      
      // Create our Point directly from longitude and latitude
      Point bal_11 = new Point(-76.6,39.33);
      Point bos_07 = new Point(-71.1,42.35);

      log.info("geom: "+geom+" bal_11: "+bal_11);

      String   res = "";
      int featureIndex;
      featureIndex = queryQuadTree(bal_11, geom);
      if (featureIndex >= 0) { res = res + "bal_11"; };
      featureIndex = queryQuadTree(bos_07, geom);
      if (featureIndex >= 0) { res = res + geom.getDescription(); };
        
      log.debug(res);
      return   res;
    }
    catch (Exception err) {
      log.error(err.getMessage());
      throw new RuntimeException("Can't parse input: " + err.getMessage() + " /// " + geo_json, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    return new Schema(new Schema.FieldSchema("geo_json", DataType.CHARARRAY));
  }
}
