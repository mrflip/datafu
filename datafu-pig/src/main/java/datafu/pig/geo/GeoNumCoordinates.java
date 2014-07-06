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
 * Number of spatial coordinates used to represent geometries: 2 for x/y, 3 for
 * x/y/z, and so forth. See also the GeoDimensionality UDF, which gives
 * dimensionality (0 for point, 1 for line, etc).
 *
 * @see GeoDimensionality
 *
 */
public class GeoNumCoordinates extends GeoScalarFunc<Integer> {

  public Integer processGeom(OGCGeometry geom) {
    //
    Integer result = geom.coordinateDimension();
    //
    return result;
  }
}
