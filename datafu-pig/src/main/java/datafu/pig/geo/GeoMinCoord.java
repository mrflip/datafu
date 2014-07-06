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

import datafu.pig.util.GeoScalarFunc;
import datafu.pig.geo.LogUtils;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import com.esri.core.geometry.Envelope;

public class GeoMinCoord extends GeoScalarFunc<Double> {
  public MinHandler handler;
  public String dimension_name;
  
  private enum Dimensions {
    X,
    Y,
    Z,
    M
  };

  public GeoMinCoord(String dimension) {
    this.dimension_name = dimension.toUpperCase();
    Dimensions op_dim = Dimensions.valueOf(dimension_name);
    switch (op_dim){
    case X: this.handler = new MinXHandler(); break;
    case Y: this.handler = new MinXHandler(); break;
    case Z: this.handler = new MinXHandler(); break;
    case M: this.handler = new MinXHandler(); break;
    }
  }

  @Override
  protected String opName() {
    return "MinCoord"+dimension_name;
  }

  public Double processGeom(OGCGeometry geom) {
    //
    Double result = handler.handle(geom);
    //
    if (result == null) { return null; }
    return result;
  }

  /*
   * Handlers
   */

  interface MinHandler {
    public Double handle(OGCGeometry geom);
  }
  
  class MinXHandler implements MinHandler{
    public Double handle(OGCGeometry geom) {
      Envelope bbox = new Envelope();
      geom.getEsriGeometry().queryEnvelope(bbox);
      return bbox.getXMin();
    }
  }

  class MinYHandler implements MinHandler {
    public Double handle(OGCGeometry geom) {
      Envelope bbox = new Envelope();
      geom.getEsriGeometry().queryEnvelope(bbox);
      return bbox.getYMin();
    }
  }

  class MinZHandler implements MinHandler {
    public Double handle(OGCGeometry geom) {
      if (! geom.is3D()) {
        LogUtils.Log_Not3D(log);
        return null;
      }
      return geom.MinZ();
    }
  }
  
  class MinMHandler implements MinHandler {
    public Double handle(OGCGeometry geom) {
      if (! geom.isMeasured()) {
        LogUtils.Log_NotMeasured(log);
        return null;
      }
      return geom.MinMeasure();
    }
  }

}
