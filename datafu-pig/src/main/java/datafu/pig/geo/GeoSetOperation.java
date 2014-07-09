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
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorFactoryLocal;

import com.esri.core.geometry.CombineOperator;

/**
 * GeoAction  -- (gA, gB) -> geom
 *
 * Union		    (gA,gB)
 * GeoDifference	    (gA,gB)
 * GeoXor	    (gA,gB)
 * GeoIntersection   (gA,gB)
 *
 * TODO: ...
 *
 * GeoClip	    (geom, env)
 * GeoCut	    (geom, polyline)
 */
public class GeoSetOperation extends SimpleEvalFunc<String>
{
  public CombineOperator  operator;
  public String           op_name;
  public SetOperationType op_type;

  /**
   * We uppercase the string, so you can supply the name as say 'intersection'
   */
  public enum SetOperationType {
    INTERSECTION,
    UNION,
    DIFFERENCE,
    SYMMETRIC_DIFFERENCE,
    XOR
  }

  public GeoSetOperation(String operation_name) {
    super();
    this.op_name = operation_name;
    this.op_type = SetOperationType.valueOf(op_name.toUpperCase());

    switch (op_type){
    case INTERSECTION:
      this.operator = (CombineOperator)OperatorFactoryLocal.getInstance()
        .getOperator(Operator.Type.Intersection);
    default:
    }
  }

  public String call(String payload_1, String payload_2) {
    OGCGeometry geom_1 = GeometryUtils.payloadToGeom(payload_1);
    OGCGeometry geom_2 = GeometryUtils.payloadToGeom(payload_2);
    if (geom_1 == null || geom_2 == null){ return null; }
    //
    try {
      Geometry result = operator.execute(
        geom_1.getEsriGeometry(), geom_2.getEsriGeometry(),
          geom_1.getEsriSpatialReference(), null);
      //
      return GeometryUtils.pigPayload(result);
    }
    catch (Exception err) {
      String msg = String.format("Can't find %s (%s): %s // %s", op_name, err.getMessage(),
        GeometryUtils.printablePayload(payload_1), GeometryUtils.printablePayload(payload_2));
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    return new Schema(new Schema.FieldSchema(op_name, DataType.CHARARRAY));
  }
}
