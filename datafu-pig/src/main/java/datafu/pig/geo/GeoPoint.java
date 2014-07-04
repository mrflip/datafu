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
import com.esri.core.geometry.Point;
import com.esri.core.geometry.ogc.OGCPoint;
import com.esri.core.geometry.ogc.OGCGeometry;

public class GeoPoint extends SimpleEvalFunc<String>
{
  public String call(Double xx, Double yy) {
    if (xx == null || yy == null) {
      // LogUtils.Log_ArgumentsNull(LOG);
      return null;
    }
    try {
      Point pt = new Point(xx.doubleValue(), yy.doubleValue());
      return GeometryUtils.pigPayload(new OGCPoint(pt, null));
    }
    catch (Exception err) {
      String msg = "Can't parse input ("+xx+","+yy+"): "+err.getMessage();
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    return new Schema(new Schema.FieldSchema("point", DataType.CHARARRAY));
  }
}
