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

import datafu.pig.util.GeoScalarFunc;
import com.esri.core.geometry.ogc.OGCGeometry;

/**
 *
 * Returns 1 if the geometry is simple, 0 if it is not.
 *
 * A simple geometry has no anomalous geometric points, such as self
 * intersection or self tangency. See the "Simple feature access - Part 1"
 * document (OGC 06-103r4) for meaning of "simple" for each geometry
 * type. (Note: that document is not simple.)
 * 
 * The method has O(n log n) complexity when the input geometry is simple.
 * For non-simple geometries, it terminates immediately when the first issue is
 * encountered.
 * 
 */
public class GeoIsSimple extends GeoScalarFunc<Integer> {

  public Integer processGeom(OGCGeometry geom) {
    //
    Integer result = (geom.isSimple() ? 1 : 0);
    //
    return result;
  }
}
