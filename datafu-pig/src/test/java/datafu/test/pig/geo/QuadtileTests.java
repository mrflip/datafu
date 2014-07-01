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

    QuadkeyUtils.lnglatToTileXY(180,         -85.0511139,  21);
    QuadkeyUtils.lnglatToTileXY(180,         -85.05111397,  21);
    QuadkeyUtils.lnglatToTileXY(180,         -85.05111398,  21);
    QuadkeyUtils.lnglatToTileXY(180,         -85.05111399,  21);
    QuadkeyUtils.lnglatToTileXY(179.9998285, -85.05112877980659, 21);

    QuadkeyUtils.lnglatToTileXY(179.9998285, -85.051128781, 21);
    QuadkeyUtils.lnglatToTileXY(179.9998285, -85.05112876, 21);

    double[] coords;
    coords = QuadkeyUtils.tileXYToCoords(2097151, 2097151, 21); QuadkeyUtils.lnglatToTileXY(coords[0], coords[3], 21); QuadkeyUtils.lnglatToTileXY(coords[2], coords[1], 21);
    coords = QuadkeyUtils.tileXYToCoords(2097150, 2097151, 21); QuadkeyUtils.lnglatToTileXY(coords[0], coords[3], 21); QuadkeyUtils.lnglatToTileXY(coords[2], coords[1], 21);
    coords = QuadkeyUtils.tileXYToCoords(2097151, 2097150, 21); QuadkeyUtils.lnglatToTileXY(coords[0], coords[3], 21); QuadkeyUtils.lnglatToTileXY(coords[2], coords[1], 21);
    coords = QuadkeyUtils.tileXYToCoords(2097150, 2097150, 21); QuadkeyUtils.lnglatToTileXY(coords[0], coords[3], 21); QuadkeyUtils.lnglatToTileXY(coords[2], coords[1], 21);
    
    int zl = 4;
    for (int qk = 0; qk < 256; qk++) {
      int[]    tile_xy = QuadkeyUtils.quadkeyToTileXY(qk);
      String   quadstr = QuadkeyUtils.quadkeyToQuadstr(qk, zl);
      double[] lnglat  = QuadkeyUtils.quadkeyToLnglat(qk, zl);

      Assert.assertEquals(qk, QuadkeyUtils.tileXYToQuadkey(tile_xy[0], tile_xy[1]));
      Assert.assertEquals(qk, QuadkeyUtils.quadstrToQuadkey(quadstr));
      // Assert.assertEquals(qk, QuadkeyUtils.lnglatToQuadkey(lnglat[0], lnglat[1], zl));
    }

    Assert.assertEquals(AUSTIN_QUADKEY >> 26, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_3[0],  AUSTIN_TILEXY_3[1]));
    Assert.assertEquals(AUSTIN_QUADKEY >> 16, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_8[0],  AUSTIN_TILEXY_8[1]));
    Assert.assertEquals(AUSTIN_QUADKEY >> 10, QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_11[0], AUSTIN_TILEXY_11[1]));
    Assert.assertEquals(AUSTIN_QUADKEY,       QuadkeyUtils.tileXYToQuadkey(AUSTIN_TILEXY_16[0], AUSTIN_TILEXY_16[1]));

    assertTileXYsEqual(AUSTIN_TILEXY_16,  QuadkeyUtils.lnglatToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 16));
    
    assertTileXYsEqual(AUSTIN_TILEXY_3,   QuadkeyUtils.lnglatToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 3));
    assertTileXYsEqual(AUSTIN_TILEXY_8,   QuadkeyUtils.lnglatToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 8));
    assertTileXYsEqual(AUSTIN_TILEXY_11,  QuadkeyUtils.lnglatToTileXY(AUSTIN_LNGLAT[0], AUSTIN_LNGLAT[1], 11));

    
    assertTileXYsEqual(ENDWLD_TILEXY_21, QuadkeyUtils.lnglatToTileXY(180,         -85.0511139712, 21));
    assertTileXYsEqual(ENDWLD_TILEXY_21, QuadkeyUtils.lnglatToTileXY(179.9998285, -85.05112878, 21));
  }

  @Test
  public void quadkeyHelpersTest() throws Exception
  {

    assertTileXYsEqual(ENDWLD_TILEXY_21, QuadkeyUtils.lnglatToTileXY(180,         -85.0511139712,  21));
    
    Assert.assertEquals(1L,                    QuadkeyUtils.mapTileSize(0));
    Assert.assertEquals(8L,                    QuadkeyUtils.mapTileSize(3));
    Assert.assertEquals(ENDWLD_TILEXY_21[0]+1, QuadkeyUtils.mapTileSize(21));
    Assert.assertEquals(0x80000000L,           QuadkeyUtils.mapTileSize(31));

    Assert.assertEquals(0,                     QuadkeyUtils.maxTileIdx(0));
    Assert.assertEquals(7,                     QuadkeyUtils.maxTileIdx(3));
    Assert.assertEquals(ENDWLD_TILEXY_21[0],   QuadkeyUtils.maxTileIdx(21));
    Assert.assertEquals(0XFFFFFFFF,            QuadkeyUtils.maxTileIdx(32));
    
    Assert.assertEquals(0L,                    QuadkeyUtils.maxQuadkey(0));
    Assert.assertEquals(63,                    QuadkeyUtils.maxQuadkey(3));
    Assert.assertEquals(0x3FFFFFFFFFFFFFFFL,   QuadkeyUtils.maxQuadkey(31));
  }
    //     Wu::Geo::Geolocation.tile_xy_zl_to_packed_qk(aus_tile_x_3.floor,  aus_tile_y_3.floor,  3).should == "023".to_i(4)
    //     Wu::Geo::Geolocation.tile_xy_zl_to_packed_qk(aus_tile_x_8.floor,  aus_tile_y_8.floor,  8).should == "02313012".to_i(4)
    //     Wu::Geo::Geolocation.tile_xy_zl_to_packed_qk(aus_tile_x_11.floor, aus_tile_y_11.floor,11).should == "02313012033".to_i(4)

  
  @Test
  public void fromQuadstrTest() throws Exception
  {
    int[] res_xy;
    
    Assert.assertEquals(AUSTIN_QUADKEY, QuadkeyUtils.quadstrToQuadkey(AUSTIN_QUADSTR));

    res_xy = QuadkeyUtils.quadstrToTileXY(AUSTIN_QUADSTR);
    
    assertTileXYsEqual(AUSTIN_TILEXY_16, QuadkeyUtils.quadstrToTileXY(AUSTIN_QUADSTR));
    assertTileXYsEqual(AUSTIN_TILEXY_11, QuadkeyUtils.quadstrToTileXY(AUSTIN_QUADSTR.substring(0,11)));
    assertTileXYsEqual(AUSTIN_TILEXY_8,  QuadkeyUtils.quadstrToTileXY(AUSTIN_QUADSTR.substring(0,8)));
    assertTileXYsEqual(AUSTIN_TILEXY_3,  QuadkeyUtils.quadstrToTileXY(AUSTIN_QUADSTR.substring(0,3)));

    // allows '' to be a quadkey (whole map)
    Assert.assertEquals(0, QuadkeyUtils.quadstrToQuadkey(""));

    // whole world == ""
    Assert.assertEquals("",     QuadkeyUtils.tileXYToQuadstr(0,   0, 0));
    // pads out to right length as zl
    Assert.assertEquals("0000", QuadkeyUtils.tileXYToQuadstr(0,   0, 4));
    Assert.assertEquals("3333", QuadkeyUtils.tileXYToQuadstr(15, 15, 4));

    Assert.assertEquals("333333333333333333333", QuadkeyUtils.tileXYToQuadstr(ENDWLD_TILEXY_21[0], ENDWLD_TILEXY_21[1], 21));

    double[] exp = {AUSQUAD_W_S_E_N[0], AUSQUAD_W_S_E_N[3]};
    
    assertLnglatsWithin(exp, QuadkeyUtils.quadkeyToLnglat(AUSTIN_QUADKEY, 16), 1e-10);

    double[] coords = QuadkeyUtils.tileXYToCoords(AUSTIN_TILEXY_16[0], AUSTIN_TILEXY_16[1], 16);
    assertWithin(coords[0], AUSQUAD_W_S_E_N[0]);
    assertWithin(coords[1], AUSQUAD_W_S_E_N[1]);
    assertWithin(coords[2], AUSQUAD_W_S_E_N[2]);
    assertWithin(coords[3], AUSQUAD_W_S_E_N[3]);
  
    // it "returns a tile_x, tile_y pair given a longitude, latitude and zoom level" do
    //   Wu::Geo::Geolocation.lng_lat_zl_to_tile_xy(aus_lng, aus_lat,  8).should == [ 58, 105]
    //   Wu::Geo::Geolocation.lng_lat_zl_to_tile_xy(aus_lng, aus_lat, 11).should == [467, 843]
    // end
    // 
    // it "returns a longitude, latitude pair given tile_x, tile_y and zoom level" do
    //   lng, lat = Wu::Geo::Geolocation.tile_xy_zl_to_lng_lat(aus_tile_x_8, aus_tile_y_8, 8)
    //   lng.should be_within(0.0001).of(aus_lng)
    //   lat.should be_within(0.0001).of(aus_lat)
    // end
    // 
    // #
    // # Quadkey coordinates
    // #
    // context 'quadkey coordinates' do
    //   it "returns a quadkey given a tile x-y pair and a zoom level" do
    //     Wu::Geo::Geolocation.tile_xy_zl_to_quadkey(aus_tile_x_3,  aus_tile_y_3,  3).should == "023"
    //     Wu::Geo::Geolocation.tile_xy_zl_to_quadkey(aus_tile_x_8,  aus_tile_y_8,  8).should == "02313012"
    //     Wu::Geo::Geolocation.tile_xy_zl_to_quadkey(aus_tile_x_11, aus_tile_y_11,11).should == "02313012033"
    //   end
    // 
    //   it "returns a quadkey given a longitude, latitude and a zoom level" do
    //     Wu::Geo::Geolocation.lng_lat_zl_to_quadkey(aus_lng, aus_lat,  3).should == "023"
    //     Wu::Geo::Geolocation.lng_lat_zl_to_quadkey(aus_lng, aus_lat,  8).should == "02313012"
    //     Wu::Geo::Geolocation.lng_lat_zl_to_quadkey(aus_lng, aus_lat, 11).should == "02313012033"
    //     Wu::Geo::Geolocation.lng_lat_zl_to_quadkey(aus_lng, aus_lat, 16).should == "0231301203311211"
    //   end
    // 
    //   it "returns a packed quadkey (an integer) given a tile xy and zoom level" do
    //   end
    // end
    // 
    // context '.packed_qk_zl_to_tile_xy' do
    //   let(:packed_qk){ "0231301203311211".to_i(4) }
    //   it "returns a tile xy given a packed quadkey (integer)" do
    //     Wu::Geo::Geolocation.packed_qk_zl_to_tile_xy(packed_qk >> 26,  3).should == [  1,   3,  3]
    //     Wu::Geo::Geolocation.packed_qk_zl_to_tile_xy(packed_qk >> 16,  8).should == [ 58, 105,  8]
    //     Wu::Geo::Geolocation.packed_qk_zl_to_tile_xy(packed_qk >> 10, 11).should == [467, 843, 11]
    //   end
    // 
    //   it "defaults to zl=16 for packed quadkeys" do
    //     Wu::Geo::Geolocation.packed_qk_zl_to_tile_xy(packed_qk    ).should == [14971, 26980, 16]
    //     Wu::Geo::Geolocation.packed_qk_zl_to_tile_xy(packed_qk, 16).should == [14971, 26980, 16]
    //   end
    // end
    // 
    // it "maps tile xyz [0,0,0] to quadkey ''" do
    //   Wu::Geo::Geolocation.tile_xy_zl_to_quadkey(0,0,0).should == ""
    // end
    // 
    // it "throws an error if a bad quadkey is given" do
    //   expect{ Wu::Geo::Geolocation.quadkey_to_tile_xy_zl("bad_key") }.to raise_error(ArgumentError, /Quadkey.*characters/)
    // end
    // 
    //                                                                     // returns a bounding box given a quadkey
    //   left, btm, right, top = Wu::Geo::Geolocation.quadkey_to_bbox(aus_quadkey_3)
    //   left.should  be_within(0.0001).of(-135.0)
    //   right.should be_within(0.0001).of(- 90.0)
    //   btm.should   be_within(0.0001).of(   0.0)
    //   top.should   be_within(0.0001).of(  40.9799)
    // end

        // returns the smallest quadkey containing two points
        // Wu::Geo::Geolocation.quadkey_containing_bbox(aus_lng, aus_lat, sat_lng, sat_lat).should == "023130"

        // returns a bounding box given a point and radius
        // returns a centroid given a bounding box
  }

  /****************************************************************************
   *
   * Helpers
   *
   */

  private void assertTileXYsEqual(int[] exp_xyz, int[] res_xyz) {
    Assert.assertEquals(exp_xyz[0], res_xyz[0]); 
    Assert.assertEquals(exp_xyz[1], res_xyz[1]); 
    // Assert.assertEquals(exp_xyz[2], res_xyz[2]);
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
  private void assertWithin(double exp_ll, double res_ll) {
    assertWithin(exp_ll, res_ll, 1e-10);
  }
}
