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

public class GeoIntersects extends SimpleEvalFunc<String>
{
  public String call(String payload_1, String payload_2) {
    OGCGeometry geom_1 = GeometryUtils.payloadToGeom(payload_1);
    OGCGeometry geom_2 = GeometryUtils.payloadToGeom(payload_2);
    if (geom_1 == null || geom_2 == null){ return null; }
    //
    try {
      OGCGeometry result = geom_1.intersection(geom_2);
      //
      return GeometryUtils.pigPayload(result);
    }
    catch (Exception err) {
      String msg = "Can't find intersection ("+err.getMessage()+"): "+
        GeometryUtils.printablePayload(payload_1)+" // "+GeometryUtils.printablePayload(payload_2);
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    return new Schema(new Schema.FieldSchema("intersection", DataType.CHARARRAY));
  }
}
