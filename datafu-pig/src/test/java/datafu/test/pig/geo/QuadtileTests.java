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

import java.util.List;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.testng.annotations.Test;

import datafu.test.pig.PigTests;

import datafu.pig.geo.QuadkeyUtils;

public class QuadtileTests extends PigTests
{

  private final static double[] AUSTIN_LNGLAT    = { -97.759003, 30.273884 };
  private final static double[] SANANT_LNGLAT    = { -98.486123, 29.42575  };
  private final static int[]    AUSTIN_TILEXY_3  = {    1,     3,  3 };
  private final static int[]    AUSTIN_TILEXY_8  = {   58,   105,  8 };
  private final static int[]    AUSTIN_TILEXY_11 = {  467,   843, 11 };
  private final static int[]    AUSTIN_TILEXY_16 = {14971, 26980, 16 };
  private final static String   AUSTIN_QUADSTR   = "0231301203311211";
  private final static long     AUSTIN_QUADKEY   = 767966565;

  private final static int[]    ENDWLD_TILEXY_21 = { 2097151, 2097151, 21 };

  private final static double[] AUSQUAD_W_S_E_N  = {
    -97.7618408203125, 30.273300428069934, -97.75634765625, 30.278044377800153 };
  
  @Test
  public void quadkeyConversionTest() throws Exception
  {
    int zl = 5;
    for (int qk = 0; qk < (1 << 2*zl); qk++) {
      int[]    tile_xy = QuadkeyUtils.quadkeyToTileXY(qk);
      String   quadstr = QuadkeyUtils.quadkeyToQuadstr(qk, zl);
      double[] lnglat  = QuadkeyUtils.quadkeyToMercator(qk, zl);
        
      Assert.assertEquals(qk,     QuadkeyUtils.tileXYToQuadkey(tile_xy[0], tile_xy[1]));
      Assert.assertEquals(qk,     QuadkeyUtils.quadstrToQuadkey(quadstr));
      Assert.assertEquals(qk,     QuadkeyUtils.mercatorToQuadkey(lnglat[0], lnglat[1], zl));
      assertTileXYEquals(tile_xy, QuadkeyUtils.mercatorToTileXY(lnglat[0], lnglat[1], zl));
      assertLnglatsWithin(lnglat, QuadkeyUtils.tileXYToMercator(tile_xy[0], tile_xy[1], zl), 1e-9);
    }
  
    Assert.assertEquals(AUSTIN_QUADKEY >> 26, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_3[0],  AUSTIN_TILEXY_3[1]));
    Assert.assertEquals(AUSTIN_QUADKEY >> 16, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_8[0],  AUSTIN_TILEXY_8[1]));
    Assert.assertEquals(AUSTIN_QUADKEY >> 10, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_11[0], AUSTIN_TILEXY_11[1]));
    Assert.assertEquals(AUSTIN_QUADKEY,       QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_16[0], AUSTIN_TILEXY_16[1]));
  
    assertTileXYEquals(AUSTIN_TILEXY_3,       QuadkeyUtils.mercatorToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 3));
    assertTileXYEquals(AUSTIN_TILEXY_8,       QuadkeyUtils.mercatorToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 8));
    assertTileXYEquals(AUSTIN_TILEXY_11,      QuadkeyUtils.mercatorToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 11));
    assertTileXYEquals(AUSTIN_TILEXY_16,      QuadkeyUtils.mercatorToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 16));
    assertTileXYEquals(ENDWLD_TILEXY_21,      QuadkeyUtils.mercatorToTileXY(179.9998285, -85.0511139712, 21));
    assertTileXYEquals(ENDWLD_TILEXY_21,      QuadkeyUtils.mercatorToTileXY(180,         -85.05112878,   21));
  }

  @Test
  public void mercatorTest() throws Exception
  {
    double[] special_lngs = {
      -180, Math.nextUp(-180),
      QuadkeyUtils.MIN_MERC_LNG,
      -60, -30,
      -1e-8, 0, 1e-8,
      30, 60,
      QuadkeyUtils.MAX_MERC_LNG,
      180
    };
    double[] special_lats = {
      // -90,  QuadkeyUtils.MIN_MERC_LAT, -60,
      -30,
      // -1e-8, 0, 1e-8, 30, 60, QuadkeyUtils.MAX_MERC_LAT, 90
    };

    int zl = 5;
    
    for (int lngi = 0; lngi < special_lngs.length; lngi++) {
      for (int latj = 0; latj < special_lats.length; latj++) {
        int[] tile_xy = QuadkeyUtils.mercatorToTileXY(special_lngs[lngi], special_lats[latj], zl);
        System.err.println(String.format("%8d %8d %4d %20.15f %20.15f mercatorTest",
            tile_xy[0], tile_xy[1], zl, special_lngs[lngi], special_lats[latj]));
      }
    }
  }

  
  // System.err.println( String.format("%6d %6d %6d %6d %3d %19.14f %19.14f %8d %8d %8d %8d %s\tmain",
  //     tile_xy[0], 
  //     txy_2[0],
  //     tile_xy[1],
  //     txy_2[1],
  //     zl, lnglat[0], lnglat[1], qk,
  //     QuadkeyUtils.tileXYToQuadkey(tile_xy[0], tile_xy[1]),
  //     QuadkeyUtils.quadstrToQuadkey(quadstr),
  //     QuadkeyUtils.mercatorToQuadkey(lnglat[0], lnglat[1], zl),
  //     quadstr));

  @Test
  public void coordsTest() throws Exception
  {
    double[] exp = {AUSQUAD_W_S_E_N[0], AUSQUAD_W_S_E_N[3]};
    assertLnglatsWithin(exp, QuadkeyUtils.quadkeyToMercator(AUSTIN_QUADKEY, 16), 1e-10);

    double[] coords = QuadkeyUtils.tileXYToCoords(AUSTIN_TILEXY_16[0], AUSTIN_TILEXY_16[1], 16);
    assertClose(coords[0], AUSQUAD_W_S_E_N[0]);
    assertClose(coords[1], AUSQUAD_W_S_E_N[1]);
    assertClose(coords[2], AUSQUAD_W_S_E_N[2]);
    assertClose(coords[3], AUSQUAD_W_S_E_N[3]);
  }

  @Test
  public void quadkeyHelpersTest() throws Exception
  {
    Assert.assertEquals(1L,                    QuadkeyUtils.mapTileSize(0));
    Assert.assertEquals(8L,                    QuadkeyUtils.mapTileSize(3));
    Assert.assertEquals(ENDWLD_TILEXY_21[0]+1, QuadkeyUtils.mapTileSize(21));
    Assert.assertEquals(0x80000000,            QuadkeyUtils.mapTileSize(31));

    Assert.assertEquals(0,                     QuadkeyUtils.maxTileIdx(0));
    Assert.assertEquals(7,                     QuadkeyUtils.maxTileIdx(3));
    Assert.assertEquals(ENDWLD_TILEXY_21[0],   QuadkeyUtils.maxTileIdx(21));
    Assert.assertEquals(0X7FFFFFFF,            QuadkeyUtils.maxTileIdx(31));
    
    Assert.assertEquals(0L,                    QuadkeyUtils.maxQuadkey(0));
    Assert.assertEquals(63,                    QuadkeyUtils.maxQuadkey(3));
    Assert.assertEquals(0x3FFFFFFFFFFFFFFFL,   QuadkeyUtils.maxQuadkey(31));
  }
  
  @Test
  public void quadstrTest() throws Exception
  {
    int[] res_xy;
    
    assertTileXYEquals(AUSTIN_TILEXY_3,           QuadkeyUtils.quadstrToTileXY("023"             ));
    assertTileXYEquals(AUSTIN_TILEXY_8,           QuadkeyUtils.quadstrToTileXY("02313012"        ));
    assertTileXYEquals(AUSTIN_TILEXY_11,          QuadkeyUtils.quadstrToTileXY("02313012033"     ));
    assertTileXYEquals(AUSTIN_TILEXY_16,          QuadkeyUtils.quadstrToTileXY("0231301203311211"));

    // allows '' to be a quadkey (whole map)
    Assert.assertEquals(0,                       QuadkeyUtils.quadstrToQuadkey(""));
    // Works for exemplar
    Assert.assertEquals(AUSTIN_QUADKEY,          QuadkeyUtils.quadstrToQuadkey(AUSTIN_QUADSTR));

    // whole world == ""
    Assert.assertEquals("",                      QuadkeyUtils.tileXYToQuadstr(  0,  0, 0));
    Assert.assertEquals("",                      QuadkeyUtils.quadkeyToQuadstr(     0, 0));
    // pads out to right length as zl
    Assert.assertEquals("0000",                  QuadkeyUtils.tileXYToQuadstr(  0,  0, 4));
    Assert.assertEquals("0000",                  QuadkeyUtils.quadkeyToQuadstr(     0, 4));
    Assert.assertEquals("3333",                  QuadkeyUtils.tileXYToQuadstr( 15, 15, 4));
    Assert.assertEquals("3333",                  QuadkeyUtils.quadkeyToQuadstr(   255, 4));
    Assert.assertEquals("333333333333333333333", QuadkeyUtils.tileXYToQuadstr(ENDWLD_TILEXY_21[0], ENDWLD_TILEXY_21[1], 21));

    // returns the smallest quadkey containing two points
    // Wu::Geo::Geolocation.quadkey_containing_bbox(aus_lng, aus_lat, sat_lng, sat_lat).should == "023130"

    // returns a bounding box given a point and radius
    // returns a centroid given a bounding box
  }

  @Test
  public void containingTest() throws Exception
  {
    String[] strs = { "1111 1111", "1111 1110", "1111 2111", "1211 1111", "2111 1111", "1113 1111" };
    for (int ii = 0; ii < strs.length; ii++) {
      QuadkeyUtils.smallestContaining("1111 1111", strs[ii]);
    } 
    
    // identical tiles give tile itself
    Assert.assertEquals("22113111", QuadkeyUtils.smallestContaining("2211 3111", "2211 3111"));
    Assert.assertEquals("00000000", QuadkeyUtils.smallestContaining("0000 0000", "0000 0000"));
    Assert.assertEquals("33333333", QuadkeyUtils.smallestContaining("3333 3333", "3333 3333"));

    // First difference of any number of bits
    Assert.assertEquals("0000000",  QuadkeyUtils.smallestContaining("0000 0000", "0000 0003"));
    Assert.assertEquals("1230123",  QuadkeyUtils.smallestContaining("1230 1233", "1230 1230"));
    Assert.assertEquals("1230123",  QuadkeyUtils.smallestContaining("1230 1233", "1230 1231"));
    Assert.assertEquals("1230123",  QuadkeyUtils.smallestContaining("1230 1233", "1230 1232"));
    
    // lots of low bits can be the same, highest difference counts
    Assert.assertEquals("",         QuadkeyUtils.smallestContaining("2231 1231", "1231 1231"));
    Assert.assertEquals("22",       QuadkeyUtils.smallestContaining("2201 1111", "2211 1111"));
    Assert.assertEquals("22",       QuadkeyUtils.smallestContaining("2201 1111", "2211 1111"));
    Assert.assertEquals("22",       QuadkeyUtils.smallestContaining("2221 1111", "2211 1111"));
    Assert.assertEquals("22",       QuadkeyUtils.smallestContaining("2231 1111", "2211 1111"));
  }

  /****************************************************************************
   *
   * Geometry stuff that should probably live elsewhere
   *
   */

  @Test
  public void geometryTest() throws Exception
  {

    double vals[] = {
      QuadkeyUtils.lngEast(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  10),
      QuadkeyUtils.lngWest(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  10),
      QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 10),
      QuadkeyUtils.latSouth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 10),

      QuadkeyUtils.lngEast(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  1000),
      QuadkeyUtils.lngWest(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  1000),
      QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000),
      QuadkeyUtils.latSouth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000),

      QuadkeyUtils.lngEast(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  1000000),
      QuadkeyUtils.lngWest(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  1000000),
      QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000000),
      QuadkeyUtils.latSouth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000000),
      
      QuadkeyUtils.lngEast(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.lngWest(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latSouth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], QuadkeyUtils.EARTH_RADIUS),
      
      QuadkeyUtils.latNorth(        0, 0.5 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(        0, 1.0 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(        0, 2.0 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(        0, 4.0 * QuadkeyUtils.EARTH_RADIUS),

      QuadkeyUtils.latNorth(       90, 0.5 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(       90, 1.0 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(       90, 2.0 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(       90, 4.0 * QuadkeyUtils.EARTH_RADIUS),

      QuadkeyUtils.latNorth(       30, 0.5 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(       30, 1.0 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(       30, 2.0 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(       30, 4.0 * QuadkeyUtils.EARTH_RADIUS),

      QuadkeyUtils.latNorth(      6.9, 0.5 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(      6.9, 1.0 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(      6.9, 2.0 * QuadkeyUtils.EARTH_RADIUS),
      QuadkeyUtils.latNorth(      6.9, 4.0 * QuadkeyUtils.EARTH_RADIUS),

      QuadkeyUtils.latNorth(       90, 10),
      QuadkeyUtils.latNorth(       90, 1000),
      QuadkeyUtils.latNorth(       90, 1000000),

      QuadkeyUtils.latNorth(      -90, 10),
      QuadkeyUtils.latNorth(      -90, 1000),
      QuadkeyUtils.latNorth(      -90, 1000000),

      QuadkeyUtils.latNorth(  90,   0, 10000),

      // QuadkeyUtils.lngEast(         0, 0.5 * QuadkeyUtils.EARTH_RADIUS),
      // QuadkeyUtils.lngEast(         0, 1.0 * QuadkeyUtils.EARTH_RADIUS),
      // QuadkeyUtils.lngEast(         0, 2.0 * QuadkeyUtils.EARTH_RADIUS),
      // QuadkeyUtils.lngEast(         0, 4.0 * QuadkeyUtils.EARTH_RADIUS),
      
      // QuadkeyUtils.lngEast(   90,   0, 10000),
      // QuadkeyUtils.lngEast(    0,   0, 10000),
      // QuadkeyUtils.lngEast( -180,   0, 10000),
      // QuadkeyUtils.lngEast( -180,   0, 10000),
      // QuadkeyUtils.lngEast(   0,   90, 10000),
      // QuadkeyUtils.lngEast(   0,  -90, 10000),
      1
    };
    
    for (int ii = 0; ii < vals.length; ii++) {
      System.err.println(String.format("%20.15f", vals[ii]));
    }
    
    // // calculates the point a given distance directly north from a lat/lng
    // assertClose(39.2671, // 39.28797666667
    //   QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000000) );
    // assertClose(-87.3457,
    //   QuadkeyUtils.lngEast( AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000000) );
  }
  

  /****************************************************************************
   *
   * Helpers
   *
   */

  private void assertTileXYEquals(int[] exp_xyz, int[] res_xyz) {
    Assert.assertEquals(exp_xyz[0], res_xyz[0]); 
    Assert.assertEquals(exp_xyz[1], res_xyz[1]); 
    // Assert.assertEquals(exp_xyz[2], res_xyz[2]);
  }

  private void assertTileXYZsEquals(int[] exp_xyz, int[] res_xyz) {
    Assert.assertEquals(exp_xyz[0], res_xyz[0]); 
    Assert.assertEquals(exp_xyz[1], res_xyz[1]); 
    // Assert.assertEquals(exp_xyz[2], res_xyz[2]);
  }

  private void assertQkZlEquals(long[] res_qkzl, long... exp_qkzl) {
    Assert.assertEquals(exp_qkzl[0], res_qkzl[0]); 
    Assert.assertEquals(exp_qkzl[1], res_qkzl[1]); 
  }

  private void assertLnglatsWithin(double[] exp_ll, double[] res_ll, double eps) {
    Assert.assertTrue("Expected "+exp_ll[0]+","+exp_ll[1]+" and result "+res_ll[0]+","+res_ll[1]+
      " should be within "+eps+": "+(exp_ll[0]-res_ll[0])+" - "+(exp_ll[1]-res_ll[1]),
      ( Math.abs(exp_ll[0] - res_ll[0]) < eps) && Math.abs(exp_ll[1] - res_ll[1]) < eps);
  }

  private void assertWithin(double exp_ll, double res_ll, double eps) {
    Assert.assertTrue("Expected "+exp_ll+" and result "+res_ll+" to be within "+eps+": "+(exp_ll-res_ll),
      ( Math.abs(exp_ll - res_ll) < eps));
  }
  private void assertClose(double exp_ll, double res_ll) {
    assertWithin(exp_ll, res_ll, 1e-10);
  }
}
