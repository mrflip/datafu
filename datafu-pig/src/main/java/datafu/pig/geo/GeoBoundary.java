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

import datafu.pig.geo.GeoProcessorFunc;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

public class GeoBoundary extends GeoProcessorFunc {
  public static String opName() { return "boundary"; }

  public Geometry processGeom(OGCGeometry geom) {
    //
    OGCGeometry result = geom.boundary();
    //
    // if (boundGeom.geometryType().equals("MultiLineString") && ((OGCMultiLineString)boundGeom).numGeometries() == 1) { boundGeom = ((OGCMultiLineString)boundGeom).geometryN(0); } // match ST_Boundary/SQL-RDBMS
    //
    if (result == null) { return null; }
    return result.getEsriGeometry();
  }
}
