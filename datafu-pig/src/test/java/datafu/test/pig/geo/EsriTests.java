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

public class EsriTests extends PigTests
{
  // Approximate latitude and longitude for major cities from maps.google.com
  private static final double[] PT_la = {34.040143,-118.243103};
  private static final double[] PT_tokyo = {35.637209,139.65271};
  private static final double[] PT_ny = {40.716038,-73.99498};
  private static final double[] PT_paris = {48.857713,2.342491};
  private static final double[] PT_sydney = {-33.872696,151.195221};


  private static final String[] EXAMPLE_FEATURES = {
      "{\"type\":\"Point\",\"coordinates\":[10.02,20.01]}",
      "{\"type\":\"LineString\",\"coordinates\":[[10.0,10.0],[20.0,20.0]]}",
      "{\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[0.0,10.0],[10.0,10.0],[0.0,0.0]]]}",
      "{\"type\":\"MultiPoint\",\"coordinates\":[[10.0,40.0],[40.0,30.0],[20.0,20.0],[30.0,10.0]]}",
      "{\"type\":\"LineString\",\"coordinates\":[[2.0,4.0],[10.0,10.0],[20.0,20.0],[7.0,8.0]]}",
      "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[0.0,0.0],[0.0,1.0],[1.0,0.0],[0.0,0.0]]],[[[2.0,2.0],[2.0,3.0],[3.0,2.0],[2.0,2.0]]]]}",
  };

  private static final String[] PARK_PTS = {
    "{\"id\":\"BAL11\",\"type\":\"Point\",\"coordinates\":[\"-76.6\",\"39.33\"]}",
    "{\"id\":\"BOS07\",\"type\":\"Point\",\"coordinates\":[\"-71.1\",\"42.35\"]}",
    "{\"id\":\"CHI11\",\"type\":\"Point\",\"coordinates\":[\"-87.66\",\"41.95\"]}",
    "{\"id\":\"HOB01\",\"type\":\"Point\",\"coordinates\":[\"-74.0268\",\"40.7499\"]}",
    "{\"id\":\"HOU02\",\"type\":\"Point\",\"coordinates\":[\"-95.41\",\"29.68\"]}",
    "{\"id\":\"LOS03\",\"type\":\"Point\",\"coordinates\":[\"-118.24\",\"34.07\"]}",
    "{\"id\":\"SFO03\",\"type\":\"Point\",\"coordinates\":[\"-122.39\",\"37.78\"]}"
  };

  private static final String[] PARK_PTS_OUT = {
    "{\"type\":\"Point\",\"coordinates\":[-76.6,39.33]}",
    "{\"type\":\"Point\",\"coordinates\":[-71.1,42.35]}",
    "{\"type\":\"Point\",\"coordinates\":[-87.66,41.95]}",
    "{\"type\":\"Point\",\"coordinates\":[-74.0268,40.7499]}",
    "{\"type\":\"Point\",\"coordinates\":[-95.41,29.68]}",
    "{\"type\":\"Point\",\"coordinates\":[-118.24,34.07]}",
    "{\"type\":\"Point\",\"coordinates\":[-122.39,37.78]}"
  };

  private static final String[] PARK_CELLS = {
    "{\"id\":\"BAL11_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-84.34830054508906,31.275723653173614],[-80.18203538896985,48.86308725114252],[-66.46250422625525,24.000000000000004],[-80.61567251461987,24],[-84.34830054508906,31.275723653173614]]]}",
    "{\"id\":\"BOS07_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-66,29.544600884319784],[-77.18309557366408,50],[-66,50],[-66,29.544600884319784]]]}",
    "{\"id\":\"CHI11_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-80.08195551117859,50],[-80.18203538896985,48.86308725114252],[-84.34830054508906,31.275723653173614],[-104.49349837941904,43.999870614547476],[-106.03964028776977,50],[-80.08195551117859,50]]]}",
    "{\"id\":\"HOB01_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-80.18203538896985,48.86308725114252],[-80.08195551117859,50],[-77.18309557366408,50],[-66,29.544600884319784],[-66,24],[-66.46250422625525,24.000000000000004],[-80.18203538896985,48.86308725114252]]]}",
    "{\"id\":\"HOU02_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-108.33929040735872,24],[-104.49349837941904,43.999870614547476],[-84.34830054508906,31.275723653173614],[-80.61567251461987,24],[-108.33929040735872,24]]]}",
    "{\"id\":\"LOS03_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-126,29.565768194070078],[-107.73228915662652,50],[-106.03964028776977,50],[-104.49349837941904,43.999870614547476],[-108.33929040735872,24],[-126,24],[-126,29.565768194070078]]]}",
    "{\"id\":\"SFO03_cell\",\"type\":\"Polygon\",\"coordinates\":[[[-107.73228915662652,50],[-126,29.565768194070078],[-126,50],[-107.73228915662652,50]]]}"
  };

  private static final String[] PARK_CELLS_OUT = {
    "{\"type\":\"Polygon\",\"coordinates\":[[[-84.34830054508906,31.275723653173614],[-80.18203538896985,48.86308725114252],[-66.46250422625525,24.000000000000004],[-80.61567251461987,24.0],[-84.34830054508906,31.275723653173614]]]}",
    "{\"type\":\"Polygon\",\"coordinates\":[[[-66.0,29.544600884319784],[-77.18309557366408,50.0],[-66.0,50.0],[-66.0,29.544600884319784]]]}",
    "{\"type\":\"Polygon\",\"coordinates\":[[[-80.08195551117859,50.0],[-80.18203538896985,48.86308725114252],[-84.34830054508906,31.275723653173614],[-104.49349837941904,43.999870614547476],[-106.03964028776977,50.0],[-80.08195551117859,50.0]]]}",
    "{\"type\":\"Polygon\",\"coordinates\":[[[-80.18203538896985,48.86308725114252],[-80.08195551117859,50.0],[-77.18309557366408,50.0],[-66.0,29.544600884319784],[-66.0,24.0],[-66.46250422625525,24.000000000000004],[-80.18203538896985,48.86308725114252]]]}",
    "{\"type\":\"Polygon\",\"coordinates\":[[[-108.33929040735872,24.0],[-104.49349837941904,43.999870614547476],[-84.34830054508906,31.275723653173614],[-80.61567251461987,24.0],[-108.33929040735872,24.0]]]}",
    "{\"type\":\"Polygon\",\"coordinates\":[[[-126.0,29.565768194070078],[-107.73228915662652,50.0],[-106.03964028776977,50.0],[-104.49349837941904,43.999870614547476],[-108.33929040735872,24.0],[-126.0,24.0],[-126.0,29.565768194070078]]]}",
    "{\"type\":\"Polygon\",\"coordinates\":[[[-107.73228915662652,50.0],[-126.0,29.565768194070078],[-126.0,50.0],[-107.73228915662652,50.0]]]}"
  };

  private static final String[] EXAMPLE_FEATURES_OUT = {
      "({\"type\":\"Point\",\"coordinates\":[10.02,20.01]})",
      "({\"type\":\"LineString\",\"coordinates\":[[10.0,10.0],[20.0,20.0]]})",
      "({\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[0.0,10.0],[10.0,10.0],[0.0,0.0]]]})",
      "({\"type\":\"MultiPoint\",\"coordinates\":[[10.0,40.0],[40.0,30.0],[20.0,20.0],[30.0,10.0]]})",
      "({\"type\":\"LineString\",\"coordinates\":[[2.0,4.0],[10.0,10.0],[20.0,20.0],[7.0,8.0]]})",
      "({\"type\":\"MultiPolygon\",\"coordinates\":[[[[0.0,0.0],[0.0,1.0],[1.0,0.0],[0.0,0.0]]],[[[2.0,2.0],[2.0,3.0],[3.0,2.0],[2.0,2.0]]]]})",
  };

  private static final String[] FEATURES_AS_ESRI_JSON = {
      "({\"x\":10.02,\"y\":20.01})",
      "({\"paths\":[[[10.0,10.0],[20.0,20.0]]]})",
      "({\"rings\":[[[0.0,0.0],[0.0,10.0],[10.0,10.0],[0.0,0.0]]]})",
      "({\"points\":[[10.0,40.0],[40.0,30.0],[20.0,20.0],[30.0,10.0]]})",
      "({\"paths\":[[[2.0,4.0],[10.0,10.0]],[[20.0,20.0],[7.0,8.0]]]})",
      "({\"rings\":[[[0.0,0.0],[0.0,1.0],[1.0,0.0],[0.0,0.0]],[[2.0,2.0],[2.0,3.0],[3.0,2.0],[2.0,2.0]]]})"
  };

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

    datafu.pig.geo.FromGeoJson fgj = new datafu.pig.geo.FromGeoJson();

    for (int ii=0; ii < PARK_PTS.length; ii++) {
      String res = fgj.call(PARK_PTS[ii]);
      Assert.assertEquals(res, PARK_PTS_OUT[ii]);
    }

    for (int ii=0; ii < PARK_CELLS.length; ii++) {
      String res = fgj.call(PARK_CELLS[ii]);
      Assert.assertEquals(res, PARK_CELLS_OUT[ii]);
    }

    for (int ii=0; ii < EXAMPLE_FEATURES.length; ii++) {
      String res = fgj.call(EXAMPLE_FEATURES[ii]);
      Assert.assertEquals(res, EXAMPLE_FEATURES[ii]);
    }

    PigTest test = createPigTestFromString(fromGeoJsonTest);

    this.writeLinesToFile("input", EXAMPLE_FEATURES);
    test.runScript();
    assertOutput(test, "data_out", EXAMPLE_FEATURES_OUT);
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

    writeExampleWellKnownText();
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

    writeExampleWellKnownText();
    test.runScript();
    assertOutput(test, "data_out",
      "({\"type\":\"Point\",\"coordinates\":[10.02,20.01]})",
      "({\"type\":\"LineString\",\"coordinates\":[[10.0,10.0],[20.0,20.0]]})",
      "({\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[0.0,10.0],[10.0,10.0],[0.0,0.0]]]})",
      "({\"type\":\"MultiPoint\",\"coordinates\":[[10.0,40.0],[40.0,30.0],[20.0,20.0],[30.0,10.0]]})",
      "({\"type\":\"LineString\",\"coordinates\":[[2.0,4.0],[10.0,10.0],[20.0,20.0],[7.0,8.0]]})",
      "({\"type\":\"MultiPolygon\",\"coordinates\":[[[[0.0,0.0],[0.0,1.0],[1.0,0.0],[0.0,0.0]]],[[[2.0,2.0],[2.0,3.0],[3.0,2.0],[2.0,2.0]]]]})");
  }

  /**
  DEFINE ToEsriJson datafu.pig.geo.ToEsriJson();
  data_in = LOAD 'input' as (val:chararray);
  data_out = FOREACH data_in GENERATE ToEsriJson(val) AS esri_json;
  STORE data_out INTO 'output';
   */
  @Multiline
  private String toEsriJsonTest;

  @Test
  public void toEsriJsonTest() throws Exception
  {
    PigTest test = createPigTestFromString(toEsriJsonTest);

    writeExampleWellKnownText();
    test.runScript();
    assertOutput(test, "data_out",
      "({\"x\":10.02,\"y\":20.01})",
      "({\"paths\":[[[10.0,10.0],[20.0,20.0]]]})",
      "({\"rings\":[[[0.0,0.0],[0.0,10.0],[10.0,10.0],[0.0,0.0]]]})",
      "({\"points\":[[10.0,40.0],[40.0,30.0],[20.0,20.0],[30.0,10.0]]})",
      "({\"paths\":[[[2.0,4.0],[10.0,10.0]],[[20.0,20.0],[7.0,8.0]]]})",
      "({\"rings\":[[[0.0,0.0],[0.0,1.0],[1.0,0.0],[0.0,0.0]],[[2.0,2.0],[2.0,3.0],[3.0,2.0],[2.0,2.0]]]})");
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

  private void writeExampleWellKnownText() throws Exception
  {
    this.writeLinesToFile("input",
      // wkid = UNKNOWN
        "point (10.02 20.01)\t0",
        "linestring (10 10, 20 20)\t0",
        "polygon ((0 0, 0 10, 10 10, 0 0))\t0",
        "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))\t0",
        "multilinestring ((2 4, 10 10), (20 20, 7 8))\t0",
        "multipolygon (((0 0, 0 1, 1 0, 0 0)), ((2 2, 2 3, 3 2, 2 2)))\t0");
  }

}
