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

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

import datafu.pig.geo.QuadkeyUtils;
import datafu.pig.geo.GeometryUtils;
import datafu.pig.geo.Quadtile;

public class EsriTests extends PigTests
{

  /*
   * TODO: add some features to EXAMPLE_FEATURES that are non-simple, and some that are empty
   *
   */


  @Test
  public void quadtileDecomposeTest() throws Exception
  {

    String[] park_cells = {
      "POLYGON ((-84.3 31.2, -80.6 24, -66.4 24, -80.1 48.8, -84.3 31.2))",
      "POLYGON ((-66 29.5, -66 50, -77.1 50, -66 29.5))",
      "POLYGON ((-80 50, -106 50, -104.4 43.9, -84.3 31.2, -80.1 48.8, -80 50))",
      "POLYGON ((-80.1 48.8, -66.4 24, -66 24, -66 29.5, -77.1 50, -80 50, -80.1 48.8))",
      "POLYGON ((-108.3 24, -80.6 24, -84.3 31.2, -104.4 43.9, -108.3 24))",
      "POLYGON ((-126 29.5, -126 24, -108.3 24, -104.4 43.9, -106 50, -107.7 50, -126 29.5))",
      "POLYGON ((-107.7 50, -126 50, -126 29.5, -107.7 50))"
    };
    String[] cell_envelopes = {
      "POLYGON ((-84.3 24, -66.4 24, -66.4 48.8, -84.3 48.8, -84.3 24))",
      "POLYGON ((-77.1 29.5, -66 29.5, -66 50, -77.1 50, -77.1 29.5))",
      "POLYGON ((-106 31.2, -80 31.2, -80 50, -106 50, -106 31.2))",
      "POLYGON ((-80.1 24, -66 24, -66 50, -80.1 50, -80.1 24))",
      "POLYGON ((-108.3 24, -80.6 24, -80.6 43.9, -108.3 43.9, -108.3 24))",
      "POLYGON ((-126 24, -104.4 24, -104.4 50, -126 50, -126 24))",
      "POLYGON ((-126 29.5, -107.7 29.5, -107.7 50, -126 50, -126 29.5))"};
    String test_shape =
      "POLYGON ((-85 20, -70 20, -70 30, -85 30, -85 20))";

    List<Quadtile> qt_list = Quadtile.quadtilesCovering( OGCGeometry.fromText(test_shape), 4, 7);
    for (Quadtile qt: qt_list) {
      GeometryUtils.dump("got qt: %s", qt);
    }
  }

  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   *
   * (geom) -> double UDFs
   *
   */

  /**
  DEFINE Geo%2$-11s       datafu.pig.geo.Geo%2$s(%3$s);
  data_in     = LOAD 'input' as (feature:chararray);
  %1$s_out = FOREACH data_in GENERATE Geo%2$s(feature);
  STORE %1$s_out INTO 'output';
   */
  @Multiline
  private String geoScalarTest;

  public void geoScalarTestHelper(String pr_name, String pr_klass, String ctor_options, String[] expected_output) throws Exception
  {
    String script = String.format(geoScalarTest, pr_name, pr_klass, ctor_options);
    System.err.println(script);
    PigTest test = createPigTestFromString(script);
    this.writeLinesToFile("input", EXAMPLE_FEATURES);
    test.runScript();
    assertOutput(test, pr_name+"_out", expected_output);
  }

  @Test
  public void geoAreaTest() throws Exception
  {
    String[] expected_output = {
      "(0.0)",
      "(0.0)",
      "(50.0)",
      "(0.0)",
      "(0.0)",
      "(1.0)"
    };
    geoScalarTestHelper("area", "Area", "", expected_output);
  }

  @Test
  public void geoNumPointsTest() throws Exception
  {
    String[] expected_output = {
      "(1)",
      "(2)",
      "(4)",
      "(4)",
      "(4)",
      "(8)",
    };
    geoScalarTestHelper("num_points", "NumPoints", "", expected_output);
  }

  @Test
  public void geoDimensionalityTest() throws Exception
  {
    String[] expected_output = {
      "(0)",
      "(1)",
      "(2)",
      "(0)",
      "(1)",
      "(2)",
    };
    geoScalarTestHelper("dimensionality", "Dimensionality", "", expected_output);
  }

  @Test
  public void geoNumCoordinatesTest() throws Exception
  {
    String[] expected_output = {
      "(2)",
      "(2)",
      "(2)",
      "(2)",
      "(2)",
      "(2)",
    };
    geoScalarTestHelper("num_coordinates", "NumCoordinates", "", expected_output);
  }

  @Test
  public void geoIsSimpleTest() throws Exception
  {
    String[] expected_output = {
      "(1)",
      "(1)",
      "(1)",
      "(1)",
      "(1)",
      "(1)",
    };
    geoScalarTestHelper("is_simple", "IsSimple", "", expected_output);
  }

  /**
  DEFINE FromWKT       datafu.pig.geo.FromWellKnownText();
  DEFINE GeoMinX       datafu.pig.geo.GeoMinCoord('X');
  DEFINE GeoMinY       datafu.pig.geo.GeoMinCoord('Y');
  DEFINE GeoMinZ       datafu.pig.geo.GeoMinCoord('Z');
  DEFINE GeoMinM       datafu.pig.geo.GeoMinCoord('M');
  data_in     = LOAD 'input' as (feature:chararray);
  min_coords_out = FOREACH data_in {
    -- feature = FromWKT(raw_feature);
    GENERATE
      GeoMinX(feature), GeoMinY(feature), GeoMinZ(feature), GeoMinM(feature);
  }
  STORE min_coords_out INTO 'output';
   */
  @Multiline
  private String geoMinCoordTest;

  @Test
  public void geoMinCoordTest() throws Exception
  {
    PigTest test = createPigTestFromString(geoMinCoordTest);
    this.writeLinesToFile("input", EXAMPLE_FEATURES);
    test.runScript();
    String[] expected_output = {
      "(10.02,10.02,10.02,10.02)",
      "(10.0,10.0,10.0,10.0)",
      "(0.0,0.0,0.0,0.0)",
      "(10.0,10.0,10.0,10.0)",
      "(2.0,2.0,2.0,2.0)",
      "(0.0,0.0,0.0,0.0)",
    };
    assertOutput(test, "min_coords_out", expected_output);
  }

  /**
  DEFINE FromWKT       datafu.pig.geo.FromWellKnownText();
  DEFINE GeoMaxX       datafu.pig.geo.GeoMaxCoord('X');
  DEFINE GeoMaxY       datafu.pig.geo.GeoMaxCoord('Y');
  DEFINE GeoMaxZ       datafu.pig.geo.GeoMaxCoord('Z');
  DEFINE GeoMaxM       datafu.pig.geo.GeoMaxCoord('M');
  data_in     = LOAD 'input' as (feature:chararray);
  max_coords_out = FOREACH data_in {
    -- feature = FromWKT(raw_feature);
    GENERATE
      GeoMaxX(feature), GeoMaxY(feature), GeoMaxZ(feature), GeoMaxM(feature);
  }
  STORE max_coords_out INTO 'output';
   */
  @Multiline
  private String geoMaxCoordTest;

  @Test
  public void geoMaxCoordTest() throws Exception
  {
    PigTest test = createPigTestFromString(geoMaxCoordTest);
    this.writeLinesToFile("input", EXAMPLE_FEATURES);
    test.runScript();
    String[] expected_output = {
      "(10.02,10.02,10.02,10.02)",
      "(20.0,20.0,20.0,20.0)",
      "(10.0,10.0,10.0,10.0)",
      "(40.0,40.0,40.0,40.0)",
      "(20.0,20.0,20.0,20.0)",
      "(3.0,3.0,3.0,3.0)",
    };
    assertOutput(test, "max_coords_out", expected_output);
  }

  /**
  DEFINE FromGeoJson datafu.pig.geo.FromGeoJson();
  DEFINE GeoIntersection datafu.pig.geo.GeoIntersection();
  data_in = LOAD 'input' as (geo_json:chararray);
  data_out = FOREACH data_in {
    feature = FromGeoJson(geo_json);
    test_pt     = 'POINT (-100 27)';
    test_line   = 'LINESTRING (-100 20, -140 29)';
    test_poly   = 'POLYGON ((-100 20, -90 30, -70 40, -70 20, -100 20))';
    test_pts     = 'MULTIPOINT ((-100 27),(-126 29.5),(-120 29.5))';
    GENERATE
      GeoIntersection(feature, test_pt)   AS feat_and_pt,
      GeoIntersection(feature, test_line) AS feat_and_line,
      GeoIntersection(feature, test_poly) AS feat_and_poly,
      GeoIntersection(feature, test_poly) AS feat_and_pts;
  };
  STORE data_out INTO 'output';
   */
  @Multiline
  private String geoIntersectionTest;

  @Test
  public void geoIntersectionTest() throws Exception
  {
    PigTest test = createPigTestFromString(geoIntersectionTest);
    this.writeLinesToFile("input", PARK_CELLS);
    test.runScript();
    assertOutput(test, "data_out",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,POLYGON ((-80.6 24, -70 24, -70 30.516788321167876, -74.10489731437599 37.947551342812005, -83.85290322580644 33.07354838709678, -84.3 31.2, -80.6 24)),POLYGON ((-80.6 24, -70 24, -70 30.516788321167876, -74.10489731437599 37.947551342812005, -83.85290322580644 33.07354838709678, -84.3 31.2, -80.6 24)))",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,POLYGON ((-70 36.88738738738739, -70 40, -71.32629558541267 39.33685220729367, -70 36.88738738738739)),POLYGON ((-70 36.88738738738739, -70 40, -71.32629558541267 39.33685220729367, -70 36.88738738738739)))",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,POLYGON ((-84.3 31.2, -83.85290322580644 33.07354838709677, -85.75780219780219 32.1210989010989, -84.3 31.2)),POLYGON ((-84.3 31.2, -83.85290322580644 33.07354838709677, -85.75780219780219 32.1210989010989, -84.3 31.2)))",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,POLYGON ((-70 30.516788321167876, -70 36.88738738738739, -71.32629558541267 39.33685220729367, -74.10489731437599 37.947551342812005, -70 30.516788321167876)),POLYGON ((-70 30.516788321167876, -70 36.88738738738739, -71.32629558541267 39.33685220729367, -74.10489731437599 37.947551342812005, -70 30.516788321167876)))",
      "(POINT (-100 27),MULTIPOLYGON EMPTY,POLYGON ((-96.00000000000001 24, -80.6 24, -84.3 31.2, -85.75780219780219 32.121098901098904, -90 30, -96.00000000000001 24)),POLYGON ((-96.00000000000001 24, -80.6 24, -84.3 31.2, -85.75780219780219 32.121098901098904, -90 30, -96.00000000000001 24)))",
      "(MULTIPOLYGON EMPTY,LINESTRING (-117.77777777777777 24, -126 25.849999999999998),MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY)",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY)");
  }

  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   *
   * Processor: (geom) -> geom
   */

  /**
  DEFINE Geo%2$-11s       datafu.pig.geo.Geo%2$s(%3$s);
  data_in     = LOAD 'input' as (feature:chararray);
  %1$s_out = FOREACH data_in GENERATE SUBSTRING(Geo%2$s(feature), 0, 50);
  STORE %1$s_out INTO 'output';
   */
  @Multiline
  private String geoProcessTest;

  public void geoProcessTestHelper(String pr_name, String pr_klass, String ctor_options, String[] expected_output) throws Exception
  {
    String script = String.format(geoProcessTest, pr_name, pr_klass, ctor_options);
    System.err.println(script);
    PigTest test = createPigTestFromString(script);
    this.writeLinesToFile("input", EXAMPLE_FEATURES);
    test.runScript();
    assertOutput(test, pr_name+"_out", expected_output);
  }

  @Test
  public void geoBoundaryTest() throws Exception
  {
    String[] expected_output = {
      "(MULTIPOINT EMPTY)",
      "(MULTIPOINT ((10 10), (20 20)))",
      "(MULTILINESTRING ((0 0, 10 10, 0 10, 0 0)))",
      "(MULTIPOINT EMPTY)",
      "(MULTIPOINT ((2 4), (10 10), (20 20), (7 8)))",
      "(MULTILINESTRING ((0 0, 1 0, 0 1, 0 0), (2 2, 3 2, )",
    };
    geoProcessTestHelper("boundary", "Boundary", "", expected_output);
  }

  @Test
  public void geoBufferTest() throws Exception
  {
    String[] expected_output = {
      "(MULTIPOLYGON (((110.02 20.01, 109.80589232386029 2)",
      "(MULTIPOLYGON (((10 -90, 16.54031292301429 -89.7858)",
      "(MULTIPOLYGON (((-100 0, -99.78589232386028 -6.5403)",
      "(MULTIPOLYGON (((30 -90, 36.54031292301429 -89.7858)",
      "(MULTIPOLYGON (((2 -96, 8.540312923014293 -95.78589)",
      "(MULTIPOLYGON (((0 -100, 1 -100, 7.540312923014293 )"
    };
    geoProcessTestHelper("buffer", "Buffer", "'100'", expected_output);
  }

  @Test
  public void geoCentroidTest() throws Exception
  {
    String[] expected_output = {
      "(POINT (10.02 20.01))",
      "(POINT (15 15))",
      "(POINT (5 5))",
      "(POINT (25 25))",
      "(POINT (11 12))",
      "(POINT (1.5 1.5))",
    };
    geoProcessTestHelper("centroid", "Centroid", "", expected_output);
  }

  @Test
  public void geoConvexHullTest() throws Exception
  {
    String[] expected_output = {
      "(POINT (10.02 20.01))",
      "(MULTILINESTRING ((10 10, 20 20)))",
      "(MULTIPOLYGON (((0 0, 10 10, 0 10, 0 0))))",
      "(MULTIPOLYGON (((10 40, 20 20, 30 10, 40 30, 10 40))",
      "(MULTIPOLYGON (((2 4, 10 10, 20 20, 2 4))))",
      "(MULTIPOLYGON (((0 0, 1 0, 3 2, 2 3, 0 1, 0 0))))"
    };
    geoProcessTestHelper("convex_hull", "ConvexHull", "", expected_output);
  }

  @Test
  public void geoEndPointTest() throws Exception
  {
    String[] expected_output = {
      "()",
      "(POINT (20 20))",
      "(POINT (10 10))",
      "()",
      "(POINT (7 8))",
      "(POINT (3 2))"
    };
    geoProcessTestHelper("end_point", "EndPoint", "", expected_output);
  }


  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   *
   * SetOperations: (geom, geom) -> geom
   */


  /**
  DEFINE FromGeoJson datafu.pig.geo.FromGeoJson();
  DEFINE GeoSetOperation datafu.pig.geo.GeoSetOperation('intersection');
  data_in = LOAD 'input' as (geo_json:chararray);
  data_out = FOREACH data_in {
    feature = FromGeoJson(geo_json);
    test_pt     = 'POINT (-100 27)';
    test_line   = 'LINESTRING (-100 20, -140 29)';
    test_poly   = 'POLYGON ((-100 20, -90 30, -70 40, -70 20, -100 20))';
    test_pts     = 'MULTIPOINT ((-100 27),(-126 29.5),(-120 29.5))';
    GENERATE
      GeoIntersection(feature, test_pt)   AS feat_and_pt,
      GeoIntersection(feature, test_line) AS feat_and_line,
      GeoIntersection(feature, test_poly) AS feat_and_poly,
      GeoIntersection(feature, test_poly) AS feat_and_pts;
  };
  STORE data_out INTO 'output';
   */
  @Multiline
  private String geoSetOperationTest;

  @Test
  public void geoSetOperationTest() throws Exception
  {
    PigTest test = createPigTestFromString(geoSetOperationTest);
    this.writeLinesToFile("input", PARK_CELLS);
    test.runScript();
    assertOutput(test, "data_out",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,POLYGON ((-80.6 24, -70 24, -70 30.516788321167876, -74.10489731437599 37.947551342812005, -83.85290322580644 33.07354838709678, -84.3 31.2, -80.6 24)),POLYGON ((-80.6 24, -70 24, -70 30.516788321167876, -74.10489731437599 37.947551342812005, -83.85290322580644 33.07354838709678, -84.3 31.2, -80.6 24)))",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,POLYGON ((-70 36.88738738738739, -70 40, -71.32629558541267 39.33685220729367, -70 36.88738738738739)),POLYGON ((-70 36.88738738738739, -70 40, -71.32629558541267 39.33685220729367, -70 36.88738738738739)))",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,POLYGON ((-84.3 31.2, -83.85290322580644 33.07354838709677, -85.75780219780219 32.1210989010989, -84.3 31.2)),POLYGON ((-84.3 31.2, -83.85290322580644 33.07354838709677, -85.75780219780219 32.1210989010989, -84.3 31.2)))",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,POLYGON ((-70 30.516788321167876, -70 36.88738738738739, -71.32629558541267 39.33685220729367, -74.10489731437599 37.947551342812005, -70 30.516788321167876)),POLYGON ((-70 30.516788321167876, -70 36.88738738738739, -71.32629558541267 39.33685220729367, -74.10489731437599 37.947551342812005, -70 30.516788321167876)))",
      "(POINT (-100 27),MULTIPOLYGON EMPTY,POLYGON ((-96.00000000000001 24, -80.6 24, -84.3 31.2, -85.75780219780219 32.121098901098904, -90 30, -96.00000000000001 24)),POLYGON ((-96.00000000000001 24, -80.6 24, -84.3 31.2, -85.75780219780219 32.121098901098904, -90 30, -96.00000000000001 24)))",
      "(MULTIPOLYGON EMPTY,LINESTRING (-117.77777777777777 24, -126 25.849999999999998),MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY)",
      "(MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY,MULTIPOLYGON EMPTY)");
  }

  /**
  DEFINE GeoPoint datafu.pig.geo.GeoPoint();
  data_in = LOAD 'input' as (lng:double, lat:double);
  data_out = FOREACH data_in GENERATE GeoPoint(lng, lat) AS feature;
  STORE data_out INTO 'output';
   */
  @Multiline
  private String geomPointTest;

  @Test
  public void geomPointTest() throws Exception
  {
    PigTest test = createPigTestFromString(geomPointTest);
    //
    this.writeLinesToFile("input", PARK_PTS);
    test.runScript();
    assertOutput(test, "data_out", PARK_PTS_OUT);
  }

  /**
  DEFINE FromGeoJson datafu.pig.geo.FromGeoJson();
  DEFINE GeoEnvelope datafu.pig.geo.GeoEnvelope();
  data_in = LOAD 'input' as (geo_json:chararray);
  data_out = FOREACH data_in {
    feature = FromGeoJson(geo_json);
    GENERATE GeoEnvelope(feature);
  };
  STORE data_out INTO 'output';
   */
  @Multiline
  private String geoEnvelopeTest;

  @Test
  public void geoEnvelopeTest() throws Exception
  {
    PigTest test = createPigTestFromString(geoEnvelopeTest);
    this.writeLinesToFile("input", PARK_CELLS);
    test.runScript();
    assertOutput(test, "data_out",
      "(POLYGON ((-84.3 24, -66.4 24, -66.4 48.8, -84.3 48.8, -84.3 24)))",
      "(POLYGON ((-77.1 29.5, -66 29.5, -66 50, -77.1 50, -77.1 29.5)))",
      "(POLYGON ((-106 31.2, -80 31.2, -80 50, -106 50, -106 31.2)))",
      "(POLYGON ((-80.1 24, -66 24, -66 50, -80.1 50, -80.1 24)))",
      "(POLYGON ((-108.3 24, -80.6 24, -80.6 43.9, -108.3 43.9, -108.3 24)))",
      "(POLYGON ((-126 24, -104.4 24, -104.4 50, -126 50, -126 24)))",
      "(POLYGON ((-126 29.5, -107.7 29.5, -107.7 50, -126 50, -126 29.5)))");
  }

  //
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  //


  /**
  DEFINE FromGeoJson datafu.pig.geo.FromGeoJson();

  data_in = LOAD 'input' as (geo_json:chararray);
  data_out = FOREACH data_in GENERATE FromGeoJson(geo_json) AS feature;
  STORE data_out INTO 'output';
   */
  @Multiline
  private String fromGeoJsonTest;

  @Test
  public void fromGeoJsonTest() throws Exception
  {
    PigTest test = createPigTestFromString(fromGeoJsonTest);

    this.writeLinesToFile("input", FEATURES_AS_GEOJSON);
    test.runScript();
    assertOutput(test, "data_out", EXAMPLE_FEATURES_OUT);

    this.writeLinesToFile("input", PARK_CELLS);
    test.runScript();
    assertOutput(test, "data_out", PARK_CELLS_OUT);

    datafu.pig.geo.FromGeoJson fgj = new datafu.pig.geo.FromGeoJson();
    //
    for (int ii=0; ii < PARK_PTS_GEOJSON.length; ii++) {
      String res = fgj.call(PARK_PTS_GEOJSON[ii]);
      Assert.assertEquals(PARK_PTS_OUT[ii], "("+res+")");
    }
    for (int ii=0; ii < PARK_CELLS.length; ii++) {
      String res = fgj.call(PARK_CELLS[ii]);
      Assert.assertEquals(PARK_CELLS_OUT[ii], "("+res+")");
    }
    for (int ii=0; ii < FEATURES_AS_GEOJSON.length; ii++) {
      String res = fgj.call(FEATURES_AS_GEOJSON[ii]);
      Assert.assertEquals(EXAMPLE_FEATURES_OUT[ii], "("+res+")");
    }
  }

  /**
  DEFINE FromWellKnownText datafu.pig.geo.FromWellKnownText();

  data_in = LOAD 'input' as (val:chararray, wkid:int);
  data_out = FOREACH data_in GENERATE FromWellKnownText(val, wkid) AS feature;
  STORE data_out INTO 'output';
   */
  @Multiline
  private String fromWellKnownTextTest;

  @Test
  public void fromWellKnownTextTest() throws Exception
  {
    PigTest test = createPigTestFromString(fromWellKnownTextTest);
    //
    this.writeLinesToFile("input", FEATURES_AS_WKT);
    test.runScript();
    assertOutput(test, "data_out", EXAMPLE_FEATURES_OUT);
  }

  /**
  DEFINE ToGeoJson datafu.pig.geo.ToGeoJson();
  data_in = LOAD 'input' as (val:chararray);
  data_out = FOREACH data_in GENERATE ToGeoJson(val) AS geo_json;
  STORE data_out INTO 'output';
   */
  @Multiline
  private String toGeoJsonTest;

  @Test
  public void toGeoJsonTest() throws Exception
  {
    PigTest test = createPigTestFromString(toGeoJsonTest);
    //
    this.writeLinesToFile("input", EXAMPLE_FEATURES);
    test.runScript();
    assertOutput(test, "data_out",
      "({\"type\":\"Point\",\"coordinates\":[10.02,20.01]})",
      "({\"type\":\"LineString\",\"coordinates\":[[10.0,10.0],[20.0,20.0]]})",
      "({\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[0.0,10.0],[10.0,10.0],[0.0,0.0]]]})",
      "({\"type\":\"MultiPoint\",\"coordinates\":[[10.0,40.0],[40.0,30.0],[20.0,20.0],[30.0,10.0]]})",
      "({\"type\":\"MultiLineString\",\"coordinates\":[[[2.0,4.0],[10.0,10.0]],[[20.0,20.0],[7.0,8.0]]]})",
      "({\"type\":\"MultiPolygon\",\"coordinates\":[[[[0.0,0.0],[0.0,1.0],[1.0,0.0],[0.0,0.0]]],[[[2.0,2.0],[2.0,3.0],[3.0,2.0],[2.0,2.0]]]]})");
  }


  // Run last just to make sure things are sane.
  @Test
  public void zLastTest() throws Exception
  {
    String[] expected_output = {
      "(0.0)",
      "(0.0)",
      "(50.0)",
      "(0.0)",
      "(0.0)",
      "(1.0)"
    };
    geoScalarTestHelper("area", "Area", "", expected_output);
  }

  /*****************************************************************************
   *
   * Helpers
   *
   */

  private void assertWithin(double expected, Tuple actual, double maxDiff) throws Exception
  {
    Double actualVal = (Double)actual.get(0);
    assertTrue(Math.abs(expected-actualVal) < maxDiff);
  }

  private String coords(double[] coords1, double[] coords2)
  {
    assertTrue(coords1.length == 2);
    assertTrue(coords2.length == 2);
    return String.format("%f\t%f\t%f\t%f", coords1[0], coords1[1], coords2[0], coords2[1]);
  }

  /*****************************************************************************
   *
   * Examples
   *
   */

  // Approximate latitude and longitude for major cities from maps.google.com
  private static final double[] PT_la = {34.040143,-118.243103};
  private static final double[] PT_tokyo = {35.637209,139.65271};
  private static final double[] PT_ny = {40.716038,-73.99498};
  private static final double[] PT_paris = {48.857713,2.342491};
  private static final double[] PT_sydney = {-33.872696,151.195221};


  private static final String[] EXAMPLE_FEATURES = {
    "POINT (10.02 20.01)",
    "LINESTRING (10 10, 20 20)",
    "POLYGON ((0 0, 10 10, 0 10, 0 0))",
    "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))",
    "MULTILINESTRING ((2 4, 10 10), (20 20, 7 8))",
    "MULTIPOLYGON     (((0 0, 1 0, 0 1, 0 0)), ((2 2, 3 2, 2 3, 2 2)))",
    // "POINT            EMPTY",
    // "LINESTRING       EMPTY",
    // "POLYGON          EMPTY",
    // "MULTIPOINT       EMPTY",
    // "MULTILINESTRING  EMPTY",
    // "MULTIPOLYGON EMPTY",
  };


  private static final String[] FEATURES_AS_GEOJSON = {
    "{\"type\":\"Point\",\"coordinates\":[10.02,20.01]}",
    "{\"type\":\"LineString\",\"coordinates\":[[10.0,10.0],[20.0,20.0]]}",
    "{\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[0.0,10.0],[10.0,10.0],[0.0,0.0]]]}",
    "{\"type\":\"MultiPoint\",\"coordinates\":[[10.0,40.0],[40.0,30.0],[20.0,20.0],[30.0,10.0]]}",
    "{\"type\":\"MultiLineString\",\"coordinates\":[[[2.0,4.0],[10.0,10.0]],[[20.0,20.0],[7.0,8.0]]]}",
    "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[0.0,0.0],[0.0,1.0],[1.0,0.0],[0.0,0.0]]],[[[2.0,2.0],[2.0,3.0],[3.0,2.0],[2.0,2.0]]]]}",
  };

  private static final String[] FEATURES_AS_WKT = {
    "point (10.02 20.01)\t0",
    "linestring (10 10, 20 20)\t0",
    "polygon ((0 0, 0 10, 10 10, 0 0))\t0",
    "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))\t0",
    "multilinestring ((2 4, 10 10), (20 20, 7 8))\t0",
    "multipolygon (((0 0, 0 1, 1 0, 0 0)), ((2 2, 2 3, 3 2, 2 2)))\t0"
  };

  private static final String[] EXAMPLE_FEATURES_OUT = {
    "(POINT (10.02 20.01))",
    "(LINESTRING (10 10, 20 20))",
    "(POLYGON ((0 0, 10 10, 0 10, 0 0)))",
    "(MULTIPOINT ((10 40), (40 30), (20 20), (30 10)))",
    "(MULTILINESTRING ((2 4, 10 10), (20 20, 7 8)))",
    "(MULTIPOLYGON (((0 0, 1 0, 0 1, 0 0)), ((2 2, 3 2, 2 3, 2 2))))",
  };

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  //
  // Baseball parks
  //

  private static final String[] PARK_PTS_GEOJSON = {
    "{\"id\":\"BAL11\",\"type\":\"Point\",\"coordinates\":[\"-76.6\",\"39.3\"]}",
    "{\"id\":\"BOS07\",\"type\":\"Point\",\"coordinates\":[\"-71.1\",\"42.3\"]}",
    "{\"id\":\"CHI11\",\"type\":\"Point\",\"coordinates\":[\"-87.6\",\"41.9\"]}",
    "{\"id\":\"HOB01\",\"type\":\"Point\",\"coordinates\":[\"-74.0\",\"40.7\"]}",
    "{\"id\":\"HOU02\",\"type\":\"Point\",\"coordinates\":[\"-95.4\",\"29.6\"]}",
    "{\"id\":\"LOS03\",\"type\":\"Point\",\"coordinates\":[\"-118.2\",\"34.0\"]}",
    "{\"id\":\"SFO03\",\"type\":\"Point\",\"coordinates\":[\"-122.3\",\"37.7\"]}"
  };

  private static final String[] PARK_PTS = {
    "-76.6\t39.3",
    "-71.1\t42.3",
    "-87.6\t41.9",
    "-74.0\t40.7",
    "-95.4\t29.6",
    "-118.2\t34.0",
    "-122.3\t37.7"
  };

  private static final String[] PARK_PTS_OUT = {
    "(POINT (-76.6 39.3))",
    "(POINT (-71.1 42.3))",
    "(POINT (-87.6 41.9))",
    "(POINT (-74 40.7))",
    "(POINT (-95.4 29.6))",
    "(POINT (-118.2 34))",
    "(POINT (-122.3 37.7))"
  };

  private static final String[] PARK_CELLS = {
    "{\"id\":\"BAL11_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-84.3,31.2],[-80.1,48.8],[-66.4,24.0],[-80.6,24],[-84.3,31.2]]]}",
    "{\"id\":\"BOS07_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-66,29.5],[-77.1,50],[-66,50],[-66,29.5]]]}",
    "{\"id\":\"CHI11_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-80.0,50],[-80.1,48.8],[-84.3,31.2],[-104.4,43.9],[-106.0,50],[-80.0,50]]]}",
    "{\"id\":\"HOB01_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-80.1,48.8],[-80.0,50],[-77.1,50],[-66,29.5],[-66,24],[-66.4,24.0],[-80.1,48.8]]]}",
    "{\"id\":\"HOU02_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-108.3,24],[-104.4,43.9],[-84.3,31.2],[-80.6,24],[-108.3,24]]]}",
    "{\"id\":\"LOS03_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-126,29.5],[-107.7,50],[-106.0,50],[-104.4,43.9],[-108.3,24],[-126,24],[-126,29.5]]]}",
    "{\"id\":\"SFO03_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-107.7,50],[-126,29.5],[-126,50],[-107.7,50]]]}"
  };

  private static final String[] PARK_CELLS_OUT = {
    "(POLYGON ((-84.3 31.2, -80.6 24, -66.4 24, -80.1 48.8, -84.3 31.2)))",
    "(POLYGON ((-66 29.5, -66 50, -77.1 50, -66 29.5)))",
    "(POLYGON ((-80 50, -106 50, -104.4 43.9, -84.3 31.2, -80.1 48.8, -80 50)))",
    "(POLYGON ((-80.1 48.8, -66.4 24, -66 24, -66 29.5, -77.1 50, -80 50, -80.1 48.8)))",
    "(POLYGON ((-108.3 24, -80.6 24, -84.3 31.2, -104.4 43.9, -108.3 24)))",
    "(POLYGON ((-126 29.5, -126 24, -108.3 24, -104.4 43.9, -106 50, -107.7 50, -126 29.5)))",
    "(POLYGON ((-107.7 50, -126 50, -126 29.5, -107.7 50)))"
  };

}
