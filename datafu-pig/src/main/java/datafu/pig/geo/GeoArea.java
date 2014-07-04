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

import java.io.IOException;
import org.apache.pig.data.DataType;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.pig.util.SimpleEvalFunc;

import datafu.pig.geo.GeometryUtils;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.ogc.OGCGeometry;

public class GeoArea extends SimpleEvalFunc<String>
{
  public Double call(String payload) {
    OGCGeometry geom = GeometryUtils.payloadToGeom(payload);
    if (geom == null){ return null; }
    try {
      //
      Double area = ogcGeometry.getEsriGeometry().calculateArea2D();
      //
      return area;
    }
    catch (Exception err) {
      String msg = "Can't find envelope ("+err.getMessage()+"): "+GeometryUtils.printablePayload(payload);
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    return new Schema(new Schema.FieldSchema("area", DataType.DOUBLE));
  }
}
