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

import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.testng.annotations.Test;

import datafu.test.pig.PigTests;

import datafu.pig.geo.QuadkeyUtils;
import datafu.pig.geo.GeometryUtils;
import datafu.pig.geo.Quadtile;
import datafu.pig.geo.Projection;

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
  private final static int[]    AUSTIN_TILEIJ_3  = {    1,     3,  3 };
  private final static int[]    AUSTIN_TILEIJ_8  = {   58,   105,  8 };
  private final static int[]    AUSTIN_TILEIJ_11 = {  467,   843, 11 };
  private final static int[]    AUSTIN_TILEIJ_16 = {14971, 26980, 16 };
  private final static String   AUSTIN_QUADSTR   = "0231301203311211";
  private final static long     AUSTIN_QUADKEY   = 767966565;

  private final static int[]    ENDWLD_TILEIJ_21 = { 2097151, 2097151, 21 };

  private final static double[] AUS_WSEN_16  = {
    -97.7618408203125, 30.273300428069934, -97.75634765625, 30.278044377800153 };

  private final static double[] AUS_WSEN_11  = {
    -97.910156250000,   30.145127184376120, -97.734375001,   30.297017883372035 };

  @Test
  public void quadtileBasicsTest() throws Exception
  {
    Quadtile qt   = Quadtile.quadtileContaining(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 16, new Projection.Mercator());
    Assert.assertEquals(AUSTIN_QUADSTR, qt.quadstr());
    Assert.assertEquals(AUSTIN_QUADKEY,      qt.quadkey());
    Assert.assertEquals(AUSTIN_TILEIJ_16[0], qt.tileI());
    Assert.assertEquals(AUSTIN_TILEIJ_16[1], qt.tileJ());
    Assert.assertEquals(AUSTIN_QUADKEY,      qt.quadkey());
    assertTileIJEquals(AUSTIN_TILEIJ_16,     qt.tileIJ());
    assertTileIJZLEquals(AUSTIN_TILEIJ_16,     qt.tileIJ());
  }

  @Test
  public void quadtileConstructorsTest() throws Exception
  {
    Quadtile qt;
    Projection proj = new Projection.Mercator();
    double
      lng    = AUSTIN_LNGLAT[0],
      lat    = AUSTIN_LNGLAT[1],
      quad_w = AUS_WSEN_16[0],
      quad_s = AUS_WSEN_16[1]+1e-9, // nudge the bottom right corner back into the zl-16 tile
      quad_e = AUS_WSEN_16[2]-1e-9,
      quad_n = AUS_WSEN_16[3];
    //
    qt = Quadtile.quadtileContaining(lng, lat, 16, proj);
    Assert.assertEquals("From lng/lat",                                   AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Point(lng, lat), 16, proj);
    Assert.assertEquals("From Point Geometry",                            AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), proj);
    Assert.assertEquals("From Envelope",                                  AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 16, proj);
    Assert.assertEquals("From shape and zl",                              AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(quad_w, quad_s, quad_e, quad_n, 16, proj);
    Assert.assertEquals("From coords",                                    AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 20, proj);
    Assert.assertEquals("From shape & finer zl -- zl fits the shape",     AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 11, proj);
    Assert.assertEquals("From shape & coarser zl -- gives zl specified",  AUSTIN_QUADSTR.substring(0,11), qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 20, proj);
    Assert.assertEquals("From coords & finer zl -- zl fits the shape",    AUSTIN_QUADSTR, qt.quadstr());
    qt = Quadtile.quadtileContaining(new Envelope(quad_w, quad_s, quad_e, quad_n), 11, proj);
    Assert.assertEquals("From coords & coarser zl -- gives zl specified", AUSTIN_QUADSTR.substring(0,11), qt.quadstr());
  }

  @Test
  public void quadkeyConversionTest() throws Exception
  {
    Projection proj = new Projection.Mercator();
    int zl = 5;
    for (int qk = 0; qk < (1 << 2*zl); qk++) {
      int[]    tile_ij = QuadkeyUtils.quadkeyToTileIJ(qk);
      String   quadstr = QuadkeyUtils.quadkeyToQuadstr(qk, zl);
      double[] lnglat  = QuadkeyUtils.quadkeyToWorld(qk, zl, proj);

      Assert.assertEquals(qk,     QuadkeyUtils.tileIJToQuadkey(tile_ij[0], tile_ij[1]));
      Assert.assertEquals(qk,     QuadkeyUtils.quadstrToQuadkey(quadstr));
      Assert.assertEquals(qk,     QuadkeyUtils.worldToQuadkey(lnglat[0], lnglat[1], zl, proj));
      assertTileIJEquals(tile_ij, QuadkeyUtils.worldToTileIJ(lnglat[0], lnglat[1], zl, proj));
      assertLnglatsWithin(lnglat, QuadkeyUtils.tileIJToWorld(tile_ij[0], tile_ij[1], zl, proj), 1e-9);

      // GeometryUtils.dump("%8d %-10s %-10s %-10s %-10s",
      //   qk,
      //   QuadkeyUtils.quadkeyToQuadstr(qk, zl),
      //   QuadkeyUtils.quadkeyToPaddedQuadstr(qk, zl, 3),
      //   QuadkeyUtils.quadkeyToPaddedQuadstr(qk, zl, 5),
      //   QuadkeyUtils.quadkeyToPaddedQuadstr(qk, zl, 7));

    }
    //
    Assert.assertEquals(AUSTIN_QUADKEY >> 26, QuadkeyUtils.tileIJToQuadkey(AUSTIN_TILEIJ_3[0],  AUSTIN_TILEIJ_3[1]));
    Assert.assertEquals(AUSTIN_QUADKEY >> 16, QuadkeyUtils.tileIJToQuadkey(AUSTIN_TILEIJ_8[0],  AUSTIN_TILEIJ_8[1]));
    Assert.assertEquals(AUSTIN_QUADKEY >> 10, QuadkeyUtils.tileIJToQuadkey(AUSTIN_TILEIJ_11[0], AUSTIN_TILEIJ_11[1]));
    Assert.assertEquals(AUSTIN_QUADKEY,       QuadkeyUtils.tileIJToQuadkey(AUSTIN_TILEIJ_16[0], AUSTIN_TILEIJ_16[1]));
    //
    assertTileIJEquals(AUSTIN_TILEIJ_3,       QuadkeyUtils.worldToTileIJ(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 3, proj));
    assertTileIJEquals(AUSTIN_TILEIJ_8,       QuadkeyUtils.worldToTileIJ(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 8, proj));
    assertTileIJEquals(AUSTIN_TILEIJ_11,      QuadkeyUtils.worldToTileIJ(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 11, proj));
    assertTileIJEquals(AUSTIN_TILEIJ_16,      QuadkeyUtils.worldToTileIJ(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 16, proj));
    assertTileIJEquals(ENDWLD_TILEIJ_21,      QuadkeyUtils.worldToTileIJ(179.9998285, -85.0511139712, 21, proj));
    assertTileIJEquals(ENDWLD_TILEIJ_21,      QuadkeyUtils.worldToTileIJ(180,         -85.05112878,   21, proj));
  }

  @Test
  public void coordsTest() throws Exception
  {
    Projection proj = new Projection.Mercator();
    double[] exp = {AUS_WSEN_16[0], AUS_WSEN_16[3]};
    assertLnglatsWithin(exp, QuadkeyUtils.quadkeyToWorld(AUSTIN_QUADKEY, 16, proj), 1e-10);

    double[] coords = QuadkeyUtils.tileIJToWorldWSEN(AUSTIN_TILEIJ_16[0], AUSTIN_TILEIJ_16[1], 16, proj);
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
    Assert.assertEquals(ENDWLD_TILEIJ_21[0]+1, QuadkeyUtils.mapTileSize(21));
    Assert.assertEquals(0x80000000,            QuadkeyUtils.mapTileSize(31));
    //
    Assert.assertEquals(0,                     QuadkeyUtils.maxTileIdx(0));
    Assert.assertEquals(7,                     QuadkeyUtils.maxTileIdx(3));
    Assert.assertEquals(ENDWLD_TILEIJ_21[0],   QuadkeyUtils.maxTileIdx(21));
    Assert.assertEquals(0X7FFFFFFF,            QuadkeyUtils.maxTileIdx(31));
    //
    Assert.assertEquals(0L,                    QuadkeyUtils.maxQuadkey(0));
    Assert.assertEquals(63,                    QuadkeyUtils.maxQuadkey(3));
    Assert.assertEquals(0x3FFFFFFFFFFFFFFFL,   QuadkeyUtils.maxQuadkey(31));
  }

  @Test
  public void quadstrTest() throws Exception
  {
    int[] res_ij;
    //
    Assert.assertEquals("allows '' to be a quadkey (whole map)", 0, QuadkeyUtils.quadstrToQuadkey(""));
    //
    Assert.assertEquals("Works for exemplar", AUSTIN_QUADKEY, QuadkeyUtils.quadstrToQuadkey(AUSTIN_QUADSTR));
    //
    //
    Assert.assertEquals("quadstr '' == whole world", "", QuadkeyUtils.tileIJToQuadstr(  0,  0, 0));
    Assert.assertEquals("quadstr '' == whole world", "", QuadkeyUtils.quadkeyToQuadstr(     0, 0));
    //
    Assert.assertEquals("pads out as zl increases", "0000",                  QuadkeyUtils.tileIJToQuadstr(  0,  0, 4));
    Assert.assertEquals("pads out as zl increases", "0000",                  QuadkeyUtils.quadkeyToQuadstr(     0, 4));
    Assert.assertEquals("pads out as zl increases", "3333",                  QuadkeyUtils.tileIJToQuadstr( 15, 15, 4));
    Assert.assertEquals("pads out as zl increases", "3333",                  QuadkeyUtils.quadkeyToQuadstr(   255, 4));
    Assert.assertEquals("pads out as zl increases", "333333333333333333333", QuadkeyUtils.tileIJToQuadstr(ENDWLD_TILEIJ_21[0], ENDWLD_TILEIJ_21[1], 21));
    //
    assertTileIJEquals(AUSTIN_TILEIJ_3,           QuadkeyUtils.quadstrToTileIJ("023"             ));
    assertTileIJEquals(AUSTIN_TILEIJ_8,           QuadkeyUtils.quadstrToTileIJ("02313012"        ));
    assertTileIJEquals(AUSTIN_TILEIJ_11,          QuadkeyUtils.quadstrToTileIJ("02313012033"     ));
    assertTileIJEquals(AUSTIN_TILEIJ_16,          QuadkeyUtils.quadstrToTileIJ("0231301203311211"));
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
  public void mercatorTest() throws Exception
  {
    Projection.GlobeProjection proj = new Projection.Mercator();
    double[] special_lngs = {
      proj.min_lng,
      proj.min_lng+QuadkeyUtils.EDGE_FUDGE,
      -168.7500000001,
      -168.750000000,
      -60, -30, -1e-8, 0, 1e-8, 30, 60,
      168.7500000001,
      proj.max_lng-QuadkeyUtils.EDGE_FUDGE,
      proj.max_lng
    };
    double[] special_lats = {
      -90,
      proj.min_lat,
      -60, -30, -1e-8, 0, 1e-8, 30, 60,
      55.776573018667705,
      61.60639637,
      61.606396371286266,
      proj.max_lat,
      90
    };
    //
    int zl = 5;
    for (int lngi = 0; lngi < special_lngs.length; lngi++) {
      for (int latj = 0; latj < special_lats.length; latj++) {
        int[]    tile_ij = QuadkeyUtils.worldToTileIJ(special_lngs[lngi], special_lats[latj], zl, proj);
        double[] coords  = QuadkeyUtils.tileIJToWorldWSEN(tile_ij[0], tile_ij[1], zl, proj);
        //
        GeometryUtils.dump("%8d %8d %4d %20.15f %20.15f %20.15f %20.15f %20.15f %20.15f mercatorTest",
          tile_ij[0], tile_ij[1], zl,
          coords[0], special_lngs[lngi], coords[2],
          coords[1], special_lats[latj], coords[3]);
      }
    }
  }

  @Test
  public void aQuadtileDecomposeTest() throws Exception
  {
    String test_shape = "POLYGON ((-85 20, -70 20, -70 30, -85 30, -85 20))";
    //
    List<Quadtile> qt_list  = Quadtile.quadtilesCovering( OGCGeometry.fromText(test_shape), 4, 8, new Projection.Mercator());
    //
    assertQuadtileHandlesMatch(qt_list,
      // two ZL-6
      "032023",   "032032",
      // some ZL-7
      "0320212",  "0320213",  "0320302",  "0320303",  "0320312",  "0320330",  "0320332",
      // and ZL-8 all around the edges
      "03202013", "03202031", "03202033", "03202102", "03202103", "03202112", "03202113",
      "03202211", "03202213", "03202231", "03202233", "03203002", "03203003", "03203012",
      "03203013", "03203102", "03203103", "03203112", "03203130", "03203132", "03203310",
      "03203312", "03203330", "03203332", "03220011", "03220013", "03220100", "03220101",
      "03220102", "03220103", "03220110", "03220111", "03220112", "03220113", "03221000",
      "03221001", "03221002", "03221003", "03221010", "03221011", "03221012", "03221013",
      "03221100", "03221101", "03221102", "03221103", "03221110", "03221112");
 }

  /****************************************************************************
   *
   * Helpers
   *
   */

  private void assertTileIJEquals(int[] exp_ij, int[] res_ij) {
    Assert.assertEquals(exp_ij[0], res_ij[0]);
    Assert.assertEquals(exp_ij[1], res_ij[1]);
  }

  private void assertTileIJZLEquals(int[] exp_ij_zl, int[] res_ij_zl) {
    Assert.assertEquals(exp_ij_zl[0], res_ij_zl[0]);
    Assert.assertEquals(exp_ij_zl[1], res_ij_zl[1]);
    Assert.assertEquals(exp_ij_zl[2], res_ij_zl[2]);
  }

  private void assertQkZlEquals(long[] res_qk_zl, long... exp_qk_zl) {
    Assert.assertEquals(exp_qk_zl[0], res_qk_zl[0]);
    Assert.assertEquals(exp_qk_zl[1], res_qk_zl[1]);
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

  /**
   *
   * Asserts that the list of expected quadstr handles matches the list of
   * handles for the given Quadtiles.
   *
   * Assert.assertEquals dumps both lists in full if there is any difference,
   * which is a hassle in all sorts of ways. This instead reports any elements
   * of one missing in the other, which is what you want to know.
   *
   */
  public static void assertQuadtileHandlesMatch(List<Quadtile> qt_list, String... expected_quadstrs) {
    List<String> missing_quadstrs = new ArrayList(Arrays.asList(expected_quadstrs));
    List<String> extra_quadstrs   = new ArrayList();
    for (Quadtile qt: qt_list) {
      if (! missing_quadstrs.remove(qt.quadstr())) { extra_quadstrs.add(qt.quadstr()); };
    }
    // Assert.assertEquals(new ArrayList(), extra_quadstrs);
    // Assert.assertEquals(new ArrayList(), missing_quadstrs);
    GeometryUtils.dump("%s - %s", extra_quadstrs, missing_quadstrs);
  }

}
