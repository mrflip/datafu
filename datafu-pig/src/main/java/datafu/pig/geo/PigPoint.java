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
import com.esri.core.geometry.ogc.OGCPoint;


import datafu.pig.geo.Projection;
import datafu.pig.geo.PigGeometry;

public class PigPoint extends PigGeometry {
  public PigPoint(Point pt, Projection projection) {
    this.proj    = projection;
    double lng   = pt.getX(), lat = pt.getY();
    this.geom    = new OGCPoint(new Point(lng, lat), null);
    this.env     = new Envelope2D(lng, lat, lng, lat);
    //
    int  zl      = QuadtileUtils.MAX_ZOOM_LEVEL;
    long qk      = QuadtileUtils.worldToQmorton(lng, lat, zl, proj);
    this.quadord = QuadtileUtils.qmortonZlToQuadord(qk, zl);
  }
}
