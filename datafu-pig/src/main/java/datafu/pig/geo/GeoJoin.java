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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Deque;
import java.util.ArrayDeque;

import org.apache.pig.data.DataType;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.pig.geo.GeometryUtils;
import datafu.pig.util.SimpleEvalFunc;
import com.google.common.base.CaseFormat;

import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.pig.geo.GeometryUtils;
import datafu.pig.util.SimpleEvalFunc;
import com.google.common.base.CaseFormat;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import datafu.pig.geo.GeometryUtils;
import datafu.pig.geo.PigGeometry;

public class GeoJoin  extends SimpleEvalFunc<DataBag> {
  private final SweepStack[] stacks;

  public static final Projection.GlobeProjection MERCATOR = new Projection.Mercator();

  public GeoJoin() {
    this.stacks = new SweepStack[] { new SweepStack(), new SweepStack() };
  }

  // /**
  //  * True if *this* tile contains or equals the other.
  //  */
  // public boolean tileContains(PigGeometry other) {
  //   return QuadtileUtils.quadordAContainsB(quadord, other.quadord());
  // }

  public static class SweepStack extends ArrayDeque<PigGeometry> {
    /** remove all irrelevant tiles from the stack */
    protected void flush(PigGeometry pgeom) {
      long   t_quadord = pgeom.quadord();
      while (! isEmpty()) {
        PigGeometry elt = removeLast();
        if (QuadtileUtils.quadordAContainsB(elt.quadord(), t_quadord)) {
          addLast(elt);
          break;
        }
      }
    }
  }

  /**
   *
   * Iterate over the collection.
   *
   * When we read a key from (say) the left stream,
   *
   * <ul>
   * <li>Flush the stacks, by popping elements until you hit one that is a parent or the same as our key (or the stack is empty)</li>
   * <li>Push the new  onto the left stack</li>
   * <li>Iterate over all elements of the right stack, emitting a pair for each.</li>
   * </ul>
   *
   * Input must be sorted on `quadord`.
   *
   * @param result_bag -- databag that will hold the matches
   * @param new_elt    -- new element to match with.
   * @param table_idx  -- the position index of the table the new element comes from
   *
   */
  public void sweepAndMatch(DataBag result_bag, PigGeometry new_elt, int table_idx) {
    // Remove irrelevant items
    for (SweepStack stack: stacks) {
      stack.flush(new_elt);
    }
    // save new item
    stacks[table_idx].addLast(new_elt);
    // pair new item with all others
    if (table_idx == 0) {
      for (PigGeometry other_elt: stacks[1]) {
        Tuple result_tup = TupleFactory.getInstance().newTuple();
        result_tup.append(new_elt);
        result_tup.append(other_elt);
        result_bag.add(result_tup);
      }
    } else {
      for (PigGeometry other_elt: stacks[0]) {
        Tuple result_tup = TupleFactory.getInstance().newTuple();
        result_tup.append(other_elt);
        result_tup.append(new_elt);
        result_bag.add(result_tup);
      }
    }
  }

  /**
   *
   * Input must be sorted on tile quadord then table index
   */
  public DataBag call(DataBag tile_tbl_geoms) {
    DataBag result_bag = BagFactory.getInstance().newDefaultBag();
    try {
      //
      for (Tuple payload_tup: tile_tbl_geoms) {
        Long    quadord   = (Long)payload_tup.get(0);
        Integer table_idx = (Integer)payload_tup.get(1);
        String  payload   = (String)payload_tup.get(2);
        OGCGeometry geom  = GeometryUtils.payloadToGeom(payload);
        if (geom == null){ continue; }
        PigShape shape = new PigShape(geom, MERCATOR);
        //
        sweepAndMatch(result_bag, shape, table_idx);
      }
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
      String bag_name  = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, opName());
      result_tuple_schema.add(new Schema.FieldSchema("geom_a", DataType.CHARARRAY));
      result_tuple_schema.add(new Schema.FieldSchema("geom_b", DataType.CHARARRAY));
      return new Schema(new Schema.FieldSchema(
          bag_name, result_tuple_schema, DataType.BAG));
    } catch (FrontendException err) { throw new RuntimeException(err); }
  }

  protected String opName() {
    return this.getClass().getSimpleName().replaceFirst("^Geo", "");
  }

}
