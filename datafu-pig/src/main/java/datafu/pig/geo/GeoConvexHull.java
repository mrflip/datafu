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

import datafu.pig.util.GeoProcessFunc;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.OperatorConvexHull;

/**
 * Calculates the convex hull geometry.
 *
 * Point      -- Returns the same point.
 * Envelope   -- returns the same envelope.
 * MultiPoint -- If the point count is one, returns the same multipoint. If the point count is two, returns a polyline of the points. Otherwise, computes and returns the convex hull polygon.
 * Segment    -- Returns a polyline consisting of the segment.
 * Polyline   -- If consists of only one segment, returns the same polyline. Otherwise, computes and returns the convex hull polygon.
 * Polygon    -- If more than one path or if the path isn't already convex, computes and returns the convex hull polygon. Otherwise, returns the same polygon.
 *
 */
public class GeoConvexHull extends GeoProcessFunc
{
  OperatorConvexHull operator;

  public GeoConvexHull() {
    this.operator = (OperatorConvexHull)OperatorFactoryLocal.getInstance()
      .getOperator(Operator.Type.ConvexHull);
  }
  
  public Geometry processGeom(OGCGeometry geom) {
    Geometry result = operator.execute(geom.getEsriGeometry(), null);
    return result;
  }
}
