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

// import org.apache.pig.data.Tuple;
// import org.apache.pig.data.TupleFactory;
// import org.apache.pig.data.BagFactory;
// import org.apache.pig.data.DataBag;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Point;

import datafu.pig.geo.Projection;
import datafu.pig.geo.PigGeometry;
import datafu.pig.geo.QuadtileUtils;

public class PigShape extends PigGeometry {

  /**
   *
   * Smallest quadtile (i.e. most fine-grained zoom level) containing the given object.
   *
   * Be aware that shapes which wrap around the edge of the map by any amount -- Kiribati
   * and American Samoa sure, but also Russia and Alaska -- will end up at zoom level
   * zero. Yikes.
   */
  public PigShape(Geometry geometry, Projection projection) {
    this.proj    = projection;
    this.geom    = geometry;
    this.env     = new Envelope2D();
    geom.queryEnvelope2D(env);
    //
    long[] qk_zl = QuadtileUtils.wsenToQmortonZl(env.xmin, env.ymin, env.xmax, env.ymax, proj);
    //
    long qk      = qk_zl[0];
    int  zl      = (int)qk_zl[1];
    this.quadord = QuadtileUtils.qmortonZlToQuadord(qk, zl);
    //
    // GeometryUtils.dump("%d %d %s %s %d %d | %s | %s", qk_lfup, qk_rtdn, QuadtileUtils.qmortonToQuadstr(qk_lfup, zoomlvl), QuadtileUtils.qmortonToQuadstr(qk_rtdn, zoomlvl), qk_zl[0], qk_zl[1], quadtile, geom);
  }
}
