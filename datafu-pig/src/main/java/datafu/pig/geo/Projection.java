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

import datafu.pig.geo.GeometryUtils;
import datafu.pig.util.SimpleEvalFunc;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import org.json.JSONException;

public class Projection
{

  public static final double MIN_MERC_LNG    =  -180.0;
  public static final double MAX_MERC_LNG    =   180.0;
  public static final double MIN_MERC_LNGRAD = Math.toRadians(MIN_MERC_LNG);
  public static final double MAX_MERC_LNGRAD = Math.toRadians(MAX_MERC_LNG);
  //
  public static final double MIN_MERC_LAT    =  -85.05112878; // Math.atan(Math.sinh(Math.PI))*180/Math.PI; //
  public static final double MAX_MERC_LAT    =   85.05112878;
  public static final double MIN_MERC_LATRAD = Math.toRadians(MIN_MERC_LAT);
  public static final double MAX_MERC_LATRAD = Math.toRadians(MAX_MERC_LAT);
  //
  public static final double GLOBE_RADIUS    = 6378137.0;
  public static final double GLOBE_CIRCUM    = GLOBE_RADIUS * 2.0 * Math.PI;

}
