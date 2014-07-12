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
    //
    for (int idx = 0; idx < EXAMPLE_SHAPES.length; idx++) {
      OGCGeometry test_shape = OGCGeometry.fromText(EXAMPLE_SHAPES[idx]);
      //
      qt_bag = Quadtile.decompose(test_shape, 4, 7, proj_1280);
      List<Quadtile> qt_list = quadtilesFromResultBag(qt_bag); // Collections.sort(qt_list, new Quadtile.ZorderComparator()); for (Quadtile qt: qt_list) { GeometryUtils.dump("%s %3d %3d", qt, qt.zoomedTileIJ(7)[0], qt.zoomedTileIJ(7)[1]); } ; GeometryUtils.dump("");
      //
      assertQuadtileHandlesMatch(qt_list, EXAMPLE_LAYOUTS[idx]);
    }
  }

  /**
     DEFINE GeoQuadDecompose datafu.pig.geo.GeoQuadDecompose();
     DEFINE ToQuadstr        datafu.pig.geo.QuadtileHandle('quadord', 'quadstr');
     DEFINE ToQkZl           datafu.pig.geo.QuadtileHandle('quadord', 'qmorton_zl');
     --
     feats     = LOAD 'input_shapes' as (feat:chararray);
     --
     decomp    = FOREACH feats {
       quad_geoms = GeoQuadDecompose(feat, 4, 7);
       GENERATE quad_geoms;
     };
     decomp    = FOREACH decomp {
       -- qg_o     = ORDER quad_geoms BY quadord ASC;
       quadstrs = FOREACH quad_geoms GENERATE FLATTEN(ToQuadstr(quadord));
       GENERATE quadstrs;
     };
     --
     DESCRIBE decomp;
     STORE decomp INTO 'output';
  */
  @Multiline
  private String quadDecompTest;

  @Test
  public void quadDecompTest() throws Exception
  {
    PigTest test = createPigTestFromString(quadDecompTest);
    this.writeLinesToFile("input_shapes", EXAMPLE_SHAPES);
    test.runScript();
    assertOutput(test, "decomp",
      "({(0000003),(0000012),(0000013),(0000021),(0000023),(000003),(0000102),(0000103),(0000112),(0000113),(000012),(000013),(0000201),(0000203),(000021),(0000221),(0000223),(000023),(00003),(0001002),(0001003),(0001020),(0001021),(0001022),(0001023),(0001200),(0001201),(0001202),(0001203),(0001220),(0001221),(0001222),(0001223),(0002001),(0002010),(0002011),(0002100),(0002101),(0002110),(0002111),(0003000),(0003001)})",
      "({(00003),(00012),(00021),(00030)})",
      "({(0000300),(0000301),(0000302),(0000303),(000031),(0000320),(0000321),(0000322),(0000323),(000033),(00012),(0001300),(0001302),(0001320),(0001322),(0002100),(0002101),(0002102),(0002103),(000211),(0002120),(0002121),(0002122),(0002123),(000213),(00030),(0003100),(0003102),(0003120),(0003122)})",
      "({(0000300),(0000301),(0000302),(0000303),(000031),(0000320),(0000321),(0000323),(000033),(00012),(0002101),(0002110),(0002111),(0002112),(0002113),(0002130),(0002131),(0002133),(00030)})");
  }

  /**
     DEFINE ToQkZl        datafu.pig.geo.QuadtileHandle('quadstr',     'qmorton_zl');
     DEFINE ToQuadstr     datafu.pig.geo.QuadtileHandle('quadstr',     'quadstr');
     DEFINE ToQuadord     datafu.pig.geo.QuadtileHandle('quadstr',     'quadord');
     DEFINE ToTileIJZl    datafu.pig.geo.QuadtileHandle('quadstr',     'tile_ij_zl');
     DEFINE ToGridXY      datafu.pig.geo.QuadtileHandle('quadstr',     'grid_xy');
     DEFINE ToLngLat      datafu.pig.geo.QuadtileHandle('quadstr',     'lng_lat');
     DEFINE ToWsen        datafu.pig.geo.QuadtileHandle('quadstr',     'wsen');
     --
     DEFINE FromQkZl      datafu.pig.geo.QuadtileHandle('qmorton_zl',  'quadstr');
     DEFINE FromQuadstr   datafu.pig.geo.QuadtileHandle('quadstr',     'quadstr');
     DEFINE FromQuadord   datafu.pig.geo.QuadtileHandle('quadord',     'quadstr');
     DEFINE FromTileIJZl  datafu.pig.geo.QuadtileHandle('tile_ij_zl',  'quadstr');
     DEFINE FromGridXY    datafu.pig.geo.QuadtileHandle('grid_xy',     'quadstr');
     DEFINE FromLngLat    datafu.pig.geo.QuadtileHandle('lng_lat',     'quadstr');
     DEFINE FromWsen      datafu.pig.geo.QuadtileHandle('wsen',        'quadstr');
     --
     handles = LOAD 'input' as (
       quadstr:chararray, qmorton:long, zl:int,
       quadord:long,
       tile_i:int, tile_j:int, grid_x:double, grid_y:double,
       west:double, south:double, east:double, north:double
       );
     --
     conv_into = FOREACH handles {
       quadstr = (quadstr IS NULL ? '' : quadstr);
       GENERATE ToQuadstr(quadstr), ToQkZl(quadstr), ToQuadord(quadstr),
         ToTileIJZl(quadstr),  ToGridXY(quadstr),  ToLngLat(quadstr),
         ToWsen(quadstr);
     };
     DESCRIBE conv_into;
     --
     conv_from = FOREACH handles {
       quadstr = (quadstr IS NULL ? '' : quadstr);
       GENERATE
         FromQuadstr(quadstr)                AS from_qs,
         FromQkZl(qmorton, zl)               AS from_qm,
         FromQuadord(quadord)                AS from_qo,
         FromTileIJZl(tile_i, tile_j, zl)    AS from_tile_ij,
         FromGridXY(grid_x, grid_y, zl)      AS from_grid_xy,
         FromLngLat(west, south, zl)         AS from_lat_lng,
         FromWsen(west, south, east, north)  AS from_wsen,
         quadstr
         ;
     };
     --
     STORE conv_from INTO 'output';
  */
  @Multiline
  private String quadHandleTest;

  @Test
  public void quadHandleTest() throws Exception
  {
    PigTest test = createPigTestFromString(quadHandleTest);
    this.writeLinesToFile("input",
      // qs    qm zl        quadord          ti tj   grid_x grid_y     west  south    east  north
      ",        0, 0,                    0,   0, 0,  0.0,   0.0,        0.0,   0.0, 1280.0,1280.0".replaceAll(", *","\t"),
      "0,       0, 1,                    1,   0, 0,  0.0,   0.0,        0.0,   0.0,  639.9, 639.9".replaceAll(", *","\t"),
      "3,       3, 1,  3458764513820540929,   1, 1,  0.5,   0.5,      640.0, 640.0, 1280.0,1280.0".replaceAll(", *","\t"),
      "0000,    0, 4,                    4,   0, 0,  0.0,   0.0,        0.0,   0.0,   79.9,  79.9".replaceAll(", *","\t"),
      "3333,  255, 4,  4593671619917905924,  15,15,  0.9375,0.9375,  1200.0,1200.0, 1280.0,1280.0".replaceAll(", *","\t"),
      "1111,   85, 4,  1531223873305968644,  15, 0,  0.9375,0.0,     1200.0,   0.0, 1280.0,  79.9".replaceAll(", *","\t"),
      "0123012301230123012301230123, 7629627604015899,28, 488296166657017564, 89478485, 53687091,0.3333333320915699,0.19999999925494194, 426.6666650772095, 255.99999904632568, 426.66666984558105, 256.0000".replaceAll(", *","\t"),
      "3333333333333333333333333333,72057594037927935,28,4611686018427387868,268435455,268435455,0.9999999962747097,0.9999999962747097, 1279.9999952316284,1279.9999952316284, 1280.0,             1280.0".replaceAll(", *","\t"));
    //
    test.runScript();
    assertOutput(test, "conv_into",
      "((    ),(  0,0),(                  0),( 0, 0,0),(0.0,   0.0),   (0.0,      0.0),(   0.0,   0.0,1280.0,1280.0))".replaceAll(" +",""),
      "((   0),(  0,1),(                  1),( 0, 0,1),(0.0,   0.0),   (0.0,      0.0),(   0.0,   0.0, 640.0, 640.0))".replaceAll(" +",""),
      "((   3),(  3,1),(3458764513820540929),( 1, 1,1),(0.5,   0.5),   (640.0,  640.0),( 640.0, 640.0,1280.0,1280.0))".replaceAll(" +",""),
      "((0000),(  0,4),(                  4),( 0, 0,4),(0.0,   0.0),   (0.0,      0.0),(   0.0,   0.0,  80.0,  80.0))".replaceAll(" +",""),
      "((3333),(255,4),(4593671619917905924),(15,15,4),(0.9375,0.9375),(1200.0,1200.0),(1200.0,1200.0,1280.0,1280.0))".replaceAll(" +",""),
      "((1111),( 85,4),(1531223873305968644),(15, 0,4),(0.9375,0.0),   (1200.0,   0.0),(1200.0,   0.0,1280.0,  80.0))".replaceAll(" +",""),
      "((0123012301230123012301230123),( 7629627604015899,28),( 488296166657017564),( 89478485, 53687091,28),(0.3333333320915699,0.19999999925494194),( 426.6666650772095, 255.99999904632568),( 426.6666650772095, 255.99999904632568,  426.66666984558105, 256.00000381469727))".replaceAll(" +",""),
      "((3333333333333333333333333333),(72057594037927935,28),(4611686018427387868),(268435455,268435455,28),(0.9999999962747097,0.9999999962747097), (1279.9999952316284,1279.9999952316284), (1279.9999952316284,1279.9999952316284, 1280.0,              1280.0))".replaceAll(" +",""));
    //
    assertOutput(test, "conv_from",
      "((),(),(),(),(),(),(),)",
      "((0),(0),(0),(0),(0),(0),(0),0)",
      "((3),(3),(3),(3),(3),(3),(3),3)",
      "((0000),(0000),(0000),(0000),(0000),(0000),(0000),0000)",
      "((3333),(3333),(3333),(3333),(3333),(3333),(3333),3333)",
      "((1111),(1111),(1111),(1111),(1111),(1111),(1111),1111)",
      "((0123012301230123012301230123),(0123012301230123012301230123),(0123012301230123012301230123),(0123012301230123012301230123),(0123012301230123012301230123),(0123012301230123012301230123),(0123012301230123012301230123),0123012301230123012301230123)",
      "((3333333333333333333333333333),(3333333333333333333333333333),(3333333333333333333333333333),(3333333333333333333333333333),(3333333333333333333333333333),(3333333333333333333333333333),(3333333333333333333333333333),3333333333333333333333333333)");
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


  /**
  DEFINE GeoQuadtreeJoin datafu.pig.geo.GeoQuadtreeJoin();
  feats_a   = LOAD 'input_shapes' as (feat:chararray);
  feats_b   = LOAD 'input_points' as (feat:chararray);
  all_feats = COGROUP feats_a ALL, feats_b ALL;
  --
  joined = FOREACH all_feats {
    GENERATE
      FLATTEN( GeoQuadtreeJoin(feats_a, feats_b) );
  };
  STORE joined INTO 'output';
   */
  @Multiline
  private String geoQuadtreeJoinTest;

  @Test
  public void geoQuadtreeJoinTest() throws Exception
  {
    PigTest test = createPigTestFromString(geoQuadtreeJoinTest);
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
