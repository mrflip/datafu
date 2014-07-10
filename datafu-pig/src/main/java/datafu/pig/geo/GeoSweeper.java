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

public class GeoSweeper {
  private final SweepStack[] stacks;

  public GeoSweeper() {
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
    protected static void sweep(PigGeometry pgeom, Deque<PigGeometry> stack) {
      long   t_quadord = pgeom.quadord();
      while (! stack.isEmpty()) {
        PigGeometry elt = stack.removeLast();
        if (QuadtileUtils.quadordAContainsB(elt.quadord(), t_quadord)) {
          stack.addLast(elt);
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
   * <li>Flush the left stack, by popping elements until you hit one that is a parent or the same as our key (or the stack is empty)</li>
   * <li>Push the new  onto the left stack</li>
   * <li>Flush the right stack, leaving only tile keys that match or contain the current key</li>
   * <li>Iterate over all elements of the right stack, emitting a pair for each.</li>
   * </ul>
   *
   */


}
