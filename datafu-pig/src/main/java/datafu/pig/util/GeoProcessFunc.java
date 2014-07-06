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
package datafu.pig.util;

import java.io.IOException;
import org.apache.pig.data.DataType;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.pig.util.SimpleEvalFunc;
import com.google.common.base.CaseFormat;

import datafu.pig.geo.GeometryUtils;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

/**
 *
 * Process a geometry into a value (double)
 *
 */
public abstract class GeoProcessFunc extends SimpleEvalFunc<String>
{
  abstract public Geometry processGeom(OGCGeometry geom);

  protected String opName() {
    return this.getClass().getSimpleName().replaceFirst("^Geo", "");
  }

  public String call(String payload) {
    OGCGeometry geom = GeometryUtils.payloadToGeom(payload);
    if (geom == null){ return null; }
    //
    try {
      Geometry result = processGeom(geom);
      return GeometryUtils.pigPayload(result);
    }
    catch (Exception err) {
      String msg = String.format("Can't %s (%s): %s", opName(), err.getMessage(),
        GeometryUtils.printablePayload(payload));
      GeometryUtils.fuckYouError(msg, err);
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    String field_name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, opName());
    return new Schema(new Schema.FieldSchema(field_name, DataType.CHARARRAY));
  }
}

// import com.esri.core.geometry.Operator;
// import com.esri.core.geometry.OperatorFactoryLocal;

// import com.esri.core.geometry.Envelope;
// import com.esri.core.geometry.Point;
// import com.esri.core.geometry.MultiPath;
//
// import com.esri.core.geometry.OperatorBoundary;
// import com.esri.core.geometry.OperatorConvexHull;
// // import com.esri.core.geometry.OperatorDensify;
// // import com.esri.core.geometry.OperatorEnvelope;
// // import com.esri.core.geometry.OperatorExteriorRing;
// import com.esri.core.geometry.OperatorGeneralize;
// import com.esri.core.geometry.OperatorOffset;
// import com.esri.core.geometry.OperatorSimplify;
  //     // DENSIFY,
  //   // END_POINT,
  //   // ENVELOPE,
  //   // EXTERIOR_RING,
  //   // GENERALIZE,
  //   // NTH_POINT
  //   // OFFSET,
  //   // SIMPLIFY,
  //   // START_POINT,
