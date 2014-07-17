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

package datafu.test.pig.geo;

import static org.testng.Assert.*;
import junit.framework.Assert;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.testng.annotations.Test;

import datafu.test.pig.PigTests;

import datafu.pig.geo.QuadtileUtils;
import datafu.pig.geo.GeometryUtils;
import datafu.pig.geo.PigGeometry;
import datafu.pig.geo.Projection;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;

import datafu.pig.geo.Quadtile;

public final class ProjectionTests extends PigTests
{


  @Test
  public void getProjectionTest() throws Exception
  {
    // String[] proj_names = {
    //   // "identity", "Linear_1280", "equirectanGULAR",
    //   // "mercator", "cool_hat",
    //   "polar_collignon"
    // };
    // for (String proj_name: proj_names) {
    //   Projection proj = Projection.getProjection(proj_name, "");
    // }

    Projection.PolarCollignon proj = new Projection.PolarCollignon("");
    
    for   (double lat =  -90; lat < 90; lat += 8.95) {
      for (double lng = 180; lng >=  -180 ; lng -= 10) {
        double[] grid_xy = proj.lngLatToGridXY(lng, lat);
        double[] ll_inv  = proj.gridXYToLngLat(grid_xy[0], grid_xy[1]);
        //
        double[] raw_gxy = proj.raw_collignon(Math.toRadians(lng), Math.toRadians(lat));
        double[] raw_lli = proj.raw_invert(raw_gxy[0], raw_gxy[1]);
        double   lng_lli = Math.toDegrees(raw_lli[0]), lat_lli = Math.toDegrees(raw_lli[1]);
        //
        double
          lng_err     = ll_inv[0] - lng, lng_err_raw = lng_lli - lng,
          lat_err     = ll_inv[1] - lat, lat_err_raw = lat_lli - lat;
        //
        GeometryUtils.dump("lng %10.5f %10.5f lat %10.5f %10.5f | %10.5f %10.5f | lnr %10.5f %10.5f ltr %10.5f %10.5f | %10.5f %10.5f | %20.15f %20.15f %s",
          lng, ll_inv[0],   lat, ll_inv[1], grid_xy[0], grid_xy[1],
          lng, lng_lli,     lat, lat_lli,   raw_gxy[0], raw_gxy[1],
          lng_err, lat_err, ((Math.abs(lng_err)+Math.abs(lat_err) > 1e-7) ? "!!!!!" : "") );
        //
        // Assert (
      }
    }
  }
  
}
