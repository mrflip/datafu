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
public abstract class GeoScalarFunc<T> extends SimpleEvalFunc<T>
{
  abstract public T processGeom(OGCGeometry geom);

  protected String opName() {
    return this.getClass().getSimpleName().replaceFirst("^Geo", "");
  }

  public T call(String payload) {
    OGCGeometry geom = GeometryUtils.payloadToGeom(payload);
    if (geom == null){ return null; }
    //
    try {
      return processGeom(geom);
    }
    catch (Exception err) {
      String msg = String.format("Can't %s (%s): %s", opName(), err.getMessage(),
        GeometryUtils.printablePayload(payload));
      GeometryUtils.fuckYouError(msg, err);
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  //
  // TODO: FIXME: This returns DOUBLE for everything
  //
  
  @Override
  public Schema outputSchema(Schema input)
  {
    String field_name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, opName());
    //
    // FIXME: Recovering the type from the template fails horribly -- and in
    // fact only every other test run (?!). This makes it work, but the ones
    // that return an integer will have surprising types for the moment
    byte type = DataType.DOUBLE;
    //
    //
    return new Schema(new Schema.FieldSchema(field_name, type));
  }
}
