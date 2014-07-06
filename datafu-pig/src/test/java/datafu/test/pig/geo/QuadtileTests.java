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
import datafu.pig.geo.GeometryUtils;
import datafu.pig.geo.Quadtile;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;

public class QuadtileTests extends PigTests
{

  private final static double[] AUSTIN_LNGLAT    = { -97.759003,  30.273884 };
  private final static double[] SANANT_LNGLAT    = { -98.486123,  29.42575  };
  private final static double[] REYKJAVIK_LNGLAT = { -21.940556,  64.13     };
  private final static int[]    AUSTIN_TILEXY_3  = {    1,     3,  3 };
  private final static int[]    AUSTIN_TILEXY_8  = {   58,   105,  8 };
  private final static int[]    AUSTIN_TILEXY_11 = {  467,   843, 11 };
  private final static int[]    AUSTIN_TILEXY_16 = {14971, 26980, 16 };
  private final static String   AUSTIN_QUADSTR   = "0231301203311211";
  private final static long     AUSTIN_QUADKEY   = 767966565;

  private final static int[]    ENDWLD_TILEXY_21 = { 2097151, 2097151, 21 };

  private final static double[] AUS_WSEN_16  = {
    -97.7618408203125, 30.273300428069934, -97.75634765625, 30.278044377800153 };

  private final static double[] AUS_WSEN_11  = {
    -97.910156250000,   30.145127184376120, -97.734375001,   30.297017883372035 };

  @Test
  public void quadtileBasicsTest() throws Exception
  {
    Quadtile qt   = Quadtile.quadtileContaining(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 16);
    Assert.assertEquals(AUSTIN_QUADSTR, qt.quadstr());
    Assert.assertEquals(AUSTIN_QUADKEY,      qt.quadkey());
    Assert.assertEquals(AUSTIN_TILEXY_16[0], qt.tileX());
    Assert.assertEquals(AUSTIN_TILEXY_16[1], qt.tileY());
    Assert.assertEquals(AUSTIN_QUADKEY,      qt.quadkey());
    assertTileXYEquals(AUSTIN_TILEXY_16,     qt.tileXY());
    assertTileXYZEquals(AUSTIN_TILEXY_16,    qt.tileXYZ());
  }

  @Test
  public void quadtileConstructorsTest() throws Exception
  {
    Quadtile qt;
    double
      lng    = AUSTIN_LNGLAT[0],
      lat    = AUSTIN_LNGLAT[1],
      quad_w = AUS_WSEN_16[0],
      quad_s = AUS_WSEN_16[1]+1e-9, // nudge the bottom right corner back into the zl-16 tile
      quad_e = AUS_WSEN_16[2]-1e-9,
      quad_n = AUS_WSEN_16[3];
    //
    qt = Quadtile.quadtileContaining(lng, lat, 16);
    Assert.assertEquals("From lng/lat",                                   AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Point(lng, lat), 16);
    Assert.assertEquals("From Point Geometry",                            AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n));
    Assert.assertEquals("From Envelope",                                  AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 16);
    Assert.assertEquals("From shape and zl",                              AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(quad_w, quad_s, quad_e, quad_n, 16);
    Assert.assertEquals("From coords",                                    AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 20);
    Assert.assertEquals("From shape & finer zl -- zl fits the shape",     AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 11);
    Assert.assertEquals("From shape & coarser zl -- gives zl specified",  AUSTIN_QUADSTR.substring(0,11), qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 20);
    Assert.assertEquals("From coords & finer zl -- zl fits the shape",    AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 11);
    Assert.assertEquals("From coords & coarser zl -- gives zl specified", AUSTIN_QUADSTR.substring(0,11), qt.quadstr());
  }

  @Test
  public void quadtileDecomposeTest() throws Exception
  {

  }

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
    //
    Assert.assertEquals(AUSTIN_QUADKEY >> 26, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_3[0],  AUSTIN_TILEXY_3[1]));
    Assert.assertEquals(AUSTIN_QUADKEY >> 16, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_8[0],  AUSTIN_TILEXY_8[1]));
    Assert.assertEquals(AUSTIN_QUADKEY >> 10, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_11[0], AUSTIN_TILEXY_11[1]));
    Assert.assertEquals(AUSTIN_QUADKEY,       QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_16[0], AUSTIN_TILEXY_16[1]));
    //
    assertTileXYEquals(AUSTIN_TILEXY_3,       QuadkeyUtils.mercatorToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 3));
    assertTileXYEquals(AUSTIN_TILEXY_8,       QuadkeyUtils.mercatorToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 8));
    assertTileXYEquals(AUSTIN_TILEXY_11,      QuadkeyUtils.mercatorToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 11));
    assertTileXYEquals(AUSTIN_TILEXY_16,      QuadkeyUtils.mercatorToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 16));
    assertTileXYEquals(ENDWLD_TILEXY_21,      QuadkeyUtils.mercatorToTileXY(179.9998285, -85.0511139712, 21));
    assertTileXYEquals(ENDWLD_TILEXY_21,      QuadkeyUtils.mercatorToTileXY(180,         -85.05112878,   21));
  }

  @Test
  public void coordsTest() throws Exception
  {
    double[] exp = {AUS_WSEN_16[0], AUS_WSEN_16[3]};
    assertLnglatsWithin(exp, QuadkeyUtils.quadkeyToMercator(AUSTIN_QUADKEY, 16), 1e-10);

    double[] coords = QuadkeyUtils.tileXYToCoords(AUSTIN_TILEXY_16[0], AUSTIN_TILEXY_16[1], 16);
    assertClose(coords[0], AUS_WSEN_16[0]);
    assertClose(coords[1], AUS_WSEN_16[1]);
    assertClose(coords[2], AUS_WSEN_16[2]);
    assertClose(coords[3], AUS_WSEN_16[3]);
  }

  @Test
  public void quadkeyHelpersTest() throws Exception
  {
    Assert.assertEquals(1L,                    QuadkeyUtils.mapTileSize(0));
    Assert.assertEquals(8L,                    QuadkeyUtils.mapTileSize(3));
    Assert.assertEquals(ENDWLD_TILEXY_21[0]+1, QuadkeyUtils.mapTileSize(21));
    Assert.assertEquals(0x80000000,            QuadkeyUtils.mapTileSize(31));
    //
    Assert.assertEquals(0,                     QuadkeyUtils.maxTileIdx(0));
    Assert.assertEquals(7,                     QuadkeyUtils.maxTileIdx(3));
    Assert.assertEquals(ENDWLD_TILEXY_21[0],   QuadkeyUtils.maxTileIdx(21));
    Assert.assertEquals(0X7FFFFFFF,            QuadkeyUtils.maxTileIdx(31));
    //
    Assert.assertEquals(0L,                    QuadkeyUtils.maxQuadkey(0));
    Assert.assertEquals(63,                    QuadkeyUtils.maxQuadkey(3));
    Assert.assertEquals(0x3FFFFFFFFFFFFFFFL,   QuadkeyUtils.maxQuadkey(31));
  }

  @Test
  public void quadstrTest() throws Exception
  {
    int[] res_xy;
    //
    Assert.assertEquals("allows '' to be a quadkey (whole map)", 0, QuadkeyUtils.quadstrToQuadkey(""));
    //
    Assert.assertEquals("Works for exemplar", AUSTIN_QUADKEY, QuadkeyUtils.quadstrToQuadkey(AUSTIN_QUADSTR));
    //
    //
    Assert.assertEquals("quadstr '' == whole world", "", QuadkeyUtils.tileXYToQuadstr(  0,  0, 0));
    Assert.assertEquals("quadstr '' == whole world", "", QuadkeyUtils.quadkeyToQuadstr(     0, 0));
    //
    Assert.assertEquals("pads out as zl increases", "0000",                  QuadkeyUtils.tileXYToQuadstr(  0,  0, 4));
    Assert.assertEquals("pads out as zl increases", "0000",                  QuadkeyUtils.quadkeyToQuadstr(     0, 4));
    Assert.assertEquals("pads out as zl increases", "3333",                  QuadkeyUtils.tileXYToQuadstr( 15, 15, 4));
    Assert.assertEquals("pads out as zl increases", "3333",                  QuadkeyUtils.quadkeyToQuadstr(   255, 4));
    Assert.assertEquals("pads out as zl increases", "333333333333333333333", QuadkeyUtils.tileXYToQuadstr(ENDWLD_TILEXY_21[0], ENDWLD_TILEXY_21[1], 21));
    //
    assertTileXYEquals(AUSTIN_TILEXY_3,           QuadkeyUtils.quadstrToTileXY("023"             ));
    assertTileXYEquals(AUSTIN_TILEXY_8,           QuadkeyUtils.quadstrToTileXY("02313012"        ));
    assertTileXYEquals(AUSTIN_TILEXY_11,          QuadkeyUtils.quadstrToTileXY("02313012033"     ));
    assertTileXYEquals(AUSTIN_TILEXY_16,          QuadkeyUtils.quadstrToTileXY("0231301203311211"));
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

  // @Test
  // public void mercatorTest() throws Exception
  // {
  //   double[] special_lngs = {
  //     QuadkeyUtils.MIN_MERC_LNG,
  //     QuadkeyUtils.MIN_MERC_LNG+QuadkeyUtils.EDGE_FUDGE,
  //     -168.7500000001,
  //     -168.750000000,
  //     -60, -30, -1e-8, 0, 1e-8, 30, 60,
  //     168.7500000001,
  //     QuadkeyUtils.MAX_MERC_LNG-QuadkeyUtils.EDGE_FUDGE,
  //     QuadkeyUtils.MAX_MERC_LNG
  //   };
  //   double[] special_lats = {
  //     -90,
  //     QuadkeyUtils.MIN_MERC_LAT,
  //     -60, -30, -1e-8, 0, 1e-8, 30, 60,
  //     55.776573018667705,
  //     61.60639637,
  //     61.606396371286266,
  //     QuadkeyUtils.MAX_MERC_LAT,
  //     90
  //   };
  //   //
  //   int zl = 5;
  //   for (int lngi = 0; lngi < special_lngs.length; lngi++) {
  //     for (int latj = 0; latj < special_lats.length; latj++) {
  //       int[] tile_xy    = QuadkeyUtils.mercatorToTileXY(special_lngs[lngi], special_lats[latj], zl);
  //       double[] coords  = QuadkeyUtils.tileXYToCoords(tile_xy[0], tile_xy[1], zl);
  //       //
  //       System.err.println(String.format("%8d %8d %4d %20.15f %20.15f %20.15f %20.15f %20.15f %20.15f mercatorTest",
  //           tile_xy[0], tile_xy[1], zl,
  //           coords[0], special_lngs[lngi], coords[2],
  //           coords[1], special_lats[latj], coords[3]));
  //     }
  // //     QuadkeyUtils.tileXYToQuadkey(tile_xy[0], tile_xy[1]),
  // //     QuadkeyUtils.quadstrToQuadkey(quadstr),
  // //     QuadkeyUtils.mercatorToQuadkey(lnglat[0], lnglat[1], zl),
  //   }
  // }

  @Test
  public void geometryTest() throws Exception
  {

    double[] lats = { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(AUSTIN_LNGLAT[0]+lats[idx],    AUSTIN_LNGLAT[1],    300000, 10);
    // }
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(REYKJAVIK_LNGLAT[0]+lats[idx], REYKJAVIK_LNGLAT[1], 100000, 10);
    // }
    //
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(-REYKJAVIK_LNGLAT[0]-lats[idx], REYKJAVIK_LNGLAT[1], 100000, 10);
    // }
    //
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(-176-lats[idx], REYKJAVIK_LNGLAT[1], 200000, 7);
    // }
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(172.1+2*lats[idx], REYKJAVIK_LNGLAT[1], 200000, 7);
    // }
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(50, 51.5 + 2*lats[idx], 600000, 6);
    // }
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(50, 62.1 + 2*lats[idx], 600000, 6);
    // }
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(50, 82.5 + 2*lats[idx], 600000, 6);
    // }
    // for (int idx = 0; idx < lats.length; idx++) {
    //   QuadkeyUtils.tilesCoveringCircle(50, 84.0 + 2*lats[idx], 600000, 6);
    // }
    //
    // double vals[] = {
    //   QuadkeyUtils.lngEast(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  10),
    //   QuadkeyUtils.lngWest(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  10),
    //   QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 10),
    //   QuadkeyUtils.latSouth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 10),
    //
    //   QuadkeyUtils.lngEast(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  1000),
    //   QuadkeyUtils.lngWest(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  1000),
    //   QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000),
    //   QuadkeyUtils.latSouth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000),
    //
    //   QuadkeyUtils.lngEast(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  1000000),
    //   QuadkeyUtils.lngWest(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  1000000),
    //   QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000000),
    //   QuadkeyUtils.latSouth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000000),
    //
    //   QuadkeyUtils.lngEast(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.lngWest(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1],  QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latSouth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], QuadkeyUtils.EARTH_RADIUS),
    //
    //   QuadkeyUtils.latNorth(        0, 0.5 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(        0, 1.0 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(        0, 2.0 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(        0, 4.0 * QuadkeyUtils.EARTH_RADIUS),
    //
    //   QuadkeyUtils.latNorth(       90, 0.5 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(       90, 1.0 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(       90, 2.0 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(       90, 4.0 * QuadkeyUtils.EARTH_RADIUS),
    //
    //   QuadkeyUtils.latNorth(       30, 0.5 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(       30, 1.0 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(       30, 2.0 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(       30, 4.0 * QuadkeyUtils.EARTH_RADIUS),
    //
    //   QuadkeyUtils.latNorth(      6.9, 0.5 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(      6.9, 1.0 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(      6.9, 2.0 * QuadkeyUtils.EARTH_RADIUS),
    //   QuadkeyUtils.latNorth(      6.9, 4.0 * QuadkeyUtils.EARTH_RADIUS),
    //
    //   QuadkeyUtils.latNorth(       90, 10),
    //   QuadkeyUtils.latNorth(       90, 1000),
    //   QuadkeyUtils.latNorth(       90, 1000000),
    //
    //   QuadkeyUtils.latNorth(      -90, 10),
    //   QuadkeyUtils.latNorth(      -90, 1000),
    //   QuadkeyUtils.latNorth(      -90, 1000000),
    //
    //   QuadkeyUtils.latNorth(  90,   0, 10000),
    //
    //   // QuadkeyUtils.lngEast(         0, 0.5 * QuadkeyUtils.EARTH_RADIUS),
    //   // QuadkeyUtils.lngEast(         0, 1.0 * QuadkeyUtils.EARTH_RADIUS),
    //   // QuadkeyUtils.lngEast(         0, 2.0 * QuadkeyUtils.EARTH_RADIUS),
    //   // QuadkeyUtils.lngEast(         0, 4.0 * QuadkeyUtils.EARTH_RADIUS),
    //
    //   // QuadkeyUtils.lngEast(   90,   0, 10000),
    //   // QuadkeyUtils.lngEast(    0,   0, 10000),
    //   // QuadkeyUtils.lngEast( -180,   0, 10000),
    //   // QuadkeyUtils.lngEast( -180,   0, 10000),
    //   // QuadkeyUtils.lngEast(   0,   90, 10000),
    //   // QuadkeyUtils.lngEast(   0,  -90, 10000),
    //   1
    // };
    //
    // for (int ii = 0; ii < vals.length; ii++) {
    //   System.err.println(String.format("%20.15f", vals[ii]));
    // }
    //
    // // // calculates the point a given distance directly north from a lat/lng
    // // assertClose(39.2671, // 39.28797666667
    // //   QuadkeyUtils.latNorth(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000000) );
    // // assertClose(-87.3457,
    // //   QuadkeyUtils.lngEast( AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 1000000) );
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

  private void assertTileXYZEquals(int[] exp_xyz, int[] res_xyz) {
    Assert.assertEquals(exp_xyz[0], res_xyz[0]);
    Assert.assertEquals(exp_xyz[1], res_xyz[1]);
    Assert.assertEquals(exp_xyz[2], res_xyz[2]);
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
    assertWithin(exp_ll, res_ll, 1e-9);
  }
}
