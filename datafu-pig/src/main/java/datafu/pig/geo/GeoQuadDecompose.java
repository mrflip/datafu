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

import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.pig.util.SimpleEvalFunc;
import com.google.common.base.CaseFormat;

import com.esri.core.geometry.ogc.OGCGeometry;

import datafu.pig.geo.GeometryUtils;
import datafu.pig.geo.Projection;

public class GeoQuadDecompose extends SimpleEvalFunc<DataBag>
{
  public final static Projection.Linear proj_1280 = new Projection.Linear(1280);

  public DataBag call(String payload, Integer coarse_zl, Integer fine_zl) {
    try {
      OGCGeometry geom   = GeometryUtils.payloadToGeom(payload);
      DataBag result_bag = Quadtile.decompose(geom, coarse_zl, fine_zl, proj_1280);
      return result_bag;
    }
    catch (Exception err) {
      String msg = String.format("Can't %s (%s)", opName(), err.getMessage());
      GeometryUtils.fuckYouError(msg, err);
      
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    Schema result_tuple_schema = new Schema();
    try {
      String bag_name  = "quad_geoms";
      result_tuple_schema.add(new Schema.FieldSchema("partkey",   DataType.LONG));
      result_tuple_schema.add(new Schema.FieldSchema("quadord",   DataType.LONG));
      result_tuple_schema.add(new Schema.FieldSchema("fragment",  DataType.CHARARRAY));
      result_tuple_schema.add(new Schema.FieldSchema("table_idx", DataType.INTEGER));
      //
      return new Schema(new Schema.FieldSchema(
          bag_name, result_tuple_schema, DataType.BAG));
    } catch (FrontendException err) { throw new RuntimeException(err); }
  }

  protected String opName() {
    return this.getClass().getSimpleName().replaceFirst("^Geo", "");
  }
}
