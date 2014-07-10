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

import org.json.JSONException;

public class FromGeoJson extends SimpleEvalFunc<String>
{

  public String call(String geo_json) throws JSONException {
    try {
      OGCGeometry geom = OGCGeometry.fromGeoJson(geo_json);
      if (geom == null){
        // GeoExceptions.Log_ArgumentsNull(LOG);
        return null;
      }
      //
      return GeometryUtils.pigPayload(geom);
    }
    catch (Exception err) {
      String msg = "Can't parse input (" + err.getMessage() + "): " + GeometryUtils.snippetize(geo_json);
      log.error(msg);
      GeometryUtils.fuckYouError(msg, err);
      throw new RuntimeException(msg, err);
    }
  }


  @Override
  public Schema outputSchema(Schema input)
  {
    return new Schema(new Schema.FieldSchema("geo_json", DataType.CHARARRAY));
  }
}
