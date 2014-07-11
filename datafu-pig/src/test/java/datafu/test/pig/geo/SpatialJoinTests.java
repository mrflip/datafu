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

import org.apache.pig.data.Tuple;
import org.apache.pig.data.DataBag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import datafu.pig.geo.GeometryUtils;
import datafu.pig.geo.PigGeometry;
import datafu.pig.geo.Projection;
import datafu.pig.geo.Quadtile;
import datafu.pig.geo.QuadtileUtils;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.QuadTree;
import com.esri.core.geometry.QuadTree.QuadTreeIterator;

import datafu.test.pig.geo.QuadtileTests;

public class SpatialJoinTests extends PigTests
{

  /**
   *
   * Shapes decompose to the set of quadtiles we think they do
   * Shapes pair off only when envelopes intersect
   *
   *
   *
   */

  /** At ZL=7 (128x128) tiles are 10 big. */
  public final static Projection.Linear proj_1280 = new Projection.Linear(1280);

  public final static String[] EXAMPLE_SHAPES = {
    "POLYGON (( 10 10, 10  90,  95  90,  95 10, 10 10 ))",
    "POLYGON (( 40 40, 40 120, 120 120, 120 40, 40 40 ))",
    "POLYGON (( 41 40, 41 120, 121 120, 121 40, 41 40 ))",
    "POLYGON (( 40 40, 78 120, 120 120, 120 40, 40 40 ))",
  };

  public final static String[] EXAMPLE_POINTS = {
    "POINT( 40 40 )", // not in #3
    "POINT( 48 42 )", // in all
    "POINT( 3   3 )", // in none
    "POINT( 42 50 )", // in the bbox of all, but not actually in #4
  };


  public final static String[][] EXAMPLE_LAYOUTS = {
    { // "POLYGON (( 10 10, 10  90,  95  90,  95 10, 10 10 ))",
      // a rectangle larger than two zl-5 tiles, but not overlapping exactly
      "0000003","0000012","0000013"          ,  "0000102","0000103","0000112","0000113"  ,  "0001002","0001003",
      "0000021","000003"                     ,  "000012",           "000013"             ,  "0001020","0001021",
      "0000023"                              ,                                              "0001022","0001023",
      //        -         -         -        +  -         -         -         -          +  -         -
      "0000201","000021"                     ,  "00003"                                  ,  "0001200","0001201",
      "0000203"                              ,                                              "0001202","0001203",
      "0000221","000023"                     ,                                              "0001220","0001221",
      "0000223"                              ,                                              "0001222","0001223",
      //        -         -         -        +  -         -         -         -          +  -         -
      "0002001","0002010","0002011"          ,  "0002100","0002101","0002110","0002111"  ,  "0003000","0003001"
    },
    { // "POLYGON (( 40 40, 40 120, 120 120, 120 40, 40 40 ))",
      // this lines up exactly with four ZL-5 tiles
      "00003"                                ,  "00012"                                  ,
      /**/
      /**/
      /**/
      //        -         -         -        +  -         -         -         -          +  -          -
      "00021"                                ,  "00030"                                  ,
      /**/
      /**/
      /**/
    },
    { // "POLYGON (( 41 40, 41 120, 121 120, 121 40, 41 40 ))",
      // Sliding it one pixel to the right makes lots more subtiles
      "0000300","0000301","000031"           ,  "00012"                                  ,  "0001300",
      "0000302","0000303"                    ,                                              "0001302",
      "0000320","0000321","000033"           ,                                              "0001320",
      "0000322","0000323"                    ,                                              "0001322",
      //        -         -         -        +  -         -         -         -          +  -         -
      "0002100","0002101","000211"           ,  "00030"                                  ,  "0003100",
      "0002102","0002103"                    ,                                              "0003102",
      "0002120","0002121","000213"           ,                                              "0003120",
      "0002122","0002123"                    ,                                              "0003122",
    },
    { // "POLYGON (( 40 40, 78 120, 120 120, 120 40, 40 40 ))",
      // knocking off the corner puts detail along the edge
      "0000300","0000301","000031"           , "00012"                                   ,
      "0000302","0000303"                    ,
      "0000320","0000321","000033"           ,
      /**/      "0000323"                    ,
      //        -         -         -        +  -         -         -         -          +  -         -
      /**/      "0002101","0002110","0002111", "00030"                                   ,
      /**/                "0002112","0002113",
      /**/                "0002130","0002131",
      /**/                          "0002133",
    },
  };

  @Test
  public void zQuadtileDecomposeTest() throws Exception
  {
    DataBag qt_bag;

    for (int idx = 0; idx < EXAMPLE_SHAPES.length; idx++) {
      String test_shape = EXAMPLE_SHAPES[idx];
      //
      qt_bag = Quadtile.decompose(
        OGCGeometry.fromText(test_shape), 4, 7, proj_1280);
      List<Quadtile> qt_list = quadtilesFromResultBag(qt_bag);

      //
      Collections.sort(qt_list, new Quadtile.ZorderComparator());
      for (Quadtile qt: qt_list) {
        // GeometryUtils.dump("%s %3d %3d", qt, qt.zoomedTileIJ(7)[0], qt.zoomedTileIJ(7)[1]);
      }
      // GeometryUtils.dump("");
      assertQuadtileHandlesMatch(qt_list, EXAMPLE_LAYOUTS[idx]);
    }
  }

  @Test
  public void sortingQuadtileTest() throws Exception
  {
    List<Quadtile> qt_list, comp_1_list, comp_2_list;

    for (int idx = 0; idx < EXAMPLE_SHAPES.length; idx++) {
      String test_shape = EXAMPLE_SHAPES[idx];
      //
      DataBag qt_bag = Quadtile.decompose(
        OGCGeometry.fromText(test_shape), 4, 7, proj_1280);
      qt_list = quadtilesFromResultBag(qt_bag);
      comp_1_list = new ArrayList<Quadtile>(qt_list);
      comp_2_list = new ArrayList<Quadtile>(qt_list);
      //
      Collections.sort(qt_list);
      Collections.sort(comp_1_list, new Quadtile.ZorderComparator());
      //
      Assert.assertEquals(qt_list, comp_1_list);
    }
  }


  @Test
  public void aQuadtileDecomposeTest() throws Exception
  {
    String test_shape = "POLYGON ((-85 20, -70 20, -70 30, -85 30, -85 20))";
    //
    DataBag qt_bag  = Quadtile.decompose( OGCGeometry.fromText(test_shape), 4, 8, new Projection.Mercator());
    List<Quadtile> qt_list = quadtilesFromResultBag(qt_bag);
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


  //  /**
  //    DEFINE GeoJoin datafu.pig.geo.GeoJoin();
  //    DEFINE GeoQuadDecompose datafu.pig.geo.GeoQuadDecompose();
  //    feats_a   = LOAD 'input_shapes' as (feat:chararray);
  //    feats_b   = LOAD 'input_points' as (feat:chararray);
  //    all_feats = COGROUP feats_a ALL, feats_b ALL;
  //    --
  //    joined = FOREACH all_feats {
  //      GENERATE FLATTEN( GeoQuadDecompose(feats_a, feats_b) );
  //
  //    feats_a   = LOAD 'input_shapes' as (feat:chararray);
  //    feats_b   = LOAD 'input_points' as (feat:chararray);
  //    all_feats = COGROUP feats_a ALL, feats_b ALL;
  //    --
  //    joined = FOREACH all_feats {
  //      GENERATE FLATTEN( QuadDecomposer(feats_a, feats_b) );
  //    };
  //    STORE joined INTO 'output';
  //   */
  //  @Multiline
  //  private String quadDecompTest;
  //
  //  @Test
  //  public void geoJoinTest() throws Exception
  //  {
  //    PigTest test = createPigTestFromString(quadDecompTest);
  //    this.writeLinesToFile("input_shapes", EXAMPLE_SHAPES);
  //    this.writeLinesToFile("input_points", EXAMPLE_POINTS);
  //    test.runScript();
  //    assertOutput(test, "joined",
  //      "(POLYGON ((-84.3 24, -66.4 24, -66.4 48.8, -84.3 48.8, -84.3 24)))");
  //  }


  /**
    DEFINE GeoQuadDecompose datafu.pig.geo.GeoQuadDecompose();
    feats_a   = LOAD 'input_shapes' as (feat:chararray);
    feats_b   = LOAD 'input_points' as (feat:chararray);
    all_feats = COGROUP feats_a ALL, feats_b ALL;
    --
    joined = FOREACH all_feats {
      GENERATE
        FLATTEN( GeoQuadDecompose(feats_a, feats_b) );
    };
    STORE joined INTO 'output';
   */
  @Multiline
  private String quadDecompTest;

  @Test
  public void quadDecompTest() throws Exception
  {
    PigTest test = createPigTestFromString(quadDecompTest);
    this.writeLinesToFile("input_shapes", EXAMPLE_SHAPES);
    this.writeLinesToFile("input_points", EXAMPLE_POINTS);
    test.runScript();
    assertOutput(test, "joined",
      // POINT( 40 40 ) is not in #3
      "(POLYGON ((10 10, 95 10, 95 90, 10 90, 10 10)),POINT (40 40))",
      "(POLYGON ((40 40, 120 40, 120 120, 40 120, 40 40)),POINT (40 40))",
      "(POLYGON ((40 40, 120 40, 120 120, 78 120, 40 40)),POINT (40 40))",
      // POINT( 48 42 ) is in all
      "(POLYGON ((10 10, 95 10, 95 90, 10 90, 10 10)),POINT (48 42))",
      "(POLYGON ((40 40, 120 40, 120 120, 40 120, 40 40)),POINT (48 42))",
      "(POLYGON ((41 40, 121 40, 121 120, 41 120, 41 40)),POINT (48 42))",
      "(POLYGON ((40 40, 120 40, 120 120, 78 120, 40 40)),POINT (48 42))",
      // POINT( 3   3 ) is in none
      // POINT( 42 50 ) is in the bbox of all, but not actually in #4
      "(POLYGON ((10 10, 95 10, 95 90, 10 90, 10 10)),POINT (42 50))",
      "(POLYGON ((40 40, 120 40, 120 120, 40 120, 40 40)),POINT (42 50))",
      "(POLYGON ((41 40, 121 40, 121 120, 41 120, 41 40)),POINT (42 50))",
      "(POLYGON ((40 40, 120 40, 120 120, 78 120, 40 40)),POINT (42 50))");
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
    Assert.assertEquals(new ArrayList(), extra_quadstrs);
    Assert.assertEquals(new ArrayList(), missing_quadstrs);
  }

  public List<Quadtile> quadtilesFromResultBag(DataBag qt_bag) {
    List<Quadtile> qt_list = new ArrayList<Quadtile>();
    for (Tuple qo_pl_tup: qt_bag) {
      try {
        Long quadord = (Long)qo_pl_tup.get(0);
        Quadtile qt = new Quadtile(quadord, proj_1280);
        qt_list.add(qt);
      } catch (Exception err) { throw new RuntimeException(err); }
    }
    return qt_list;
  }

}
