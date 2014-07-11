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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import datafu.pig.geo.GeometryUtils;
import com.esri.core.geometry.NumberUtils;

abstract public class Projection
{

  abstract public double[] lngLatToGridXY(double lng, double lat);
  abstract public double[] gridXYToLngLat(double grid_x, double grid_y);


  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   *
   * Projections
   *
   */

  /**
   *
   * Passes through, no changes.
   * (FIXME: should this snap results to be in (0.0, 1.0)?)
   *
   */
  public static class Identity extends Projection {

    public double[] lngLatToGridXY(double lng, double lat) {
      double[] grid_xy = { lng, lat };
      return grid_xy;
    }

    public double[] gridXYToLngLat(double grid_x, double grid_y) {
      double[] lat_lng = { grid_x, grid_y };
      return lat_lng;
    }
  }

  /**
   *
   * Rescales from a scale x scale space to the 1  x 1 space
   * (FIXME: should this snap results to be in (0.0, 1.0)?)
   *
   */
  public static class Linear extends Projection {
    public final double scale;

    public Linear(double sc) {
      this.scale = sc;
    }

    public double[] lngLatToGridXY(double lng, double lat) {
      double[] grid_xy = { lng / scale, lat / scale };
      return grid_xy;
    }

    public double[] gridXYToLngLat(double grid_x, double grid_y) {
      double[] lat_lng = { grid_x * scale, grid_y * scale };
      return lat_lng;
    }
  }

  abstract public static class GlobeProjection extends Projection {
    public final double min_lng;
    public final double max_lng;
    public final double min_lat;
    public final double max_lat;
    public final double globe_radius;

    public GlobeProjection(double min_lng, double min_lat, double max_lng, double max_lat, double globe_radius){
      this.min_lng = min_lng;
      this.min_lat = min_lat;
      this.max_lng = max_lng;
      this.max_lat = max_lat;
      this.globe_radius = globe_radius;
    }

    public double globeCircum() {
      return globe_radius * 2.0 * Math.PI;
    }

    /**
     * Maximum latitude / longitude extent of the area covering a given distance
     * from the point on the globe. The longitude extent is *not* the equivalent
     * of walking east and west from the point, because the meridians can close
     * faster than the shape of the circle.
     *
     * http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates
     * http://gis.stackexchange.com/questions/19221/find-tangent-point-on-circle-furthest-east-or-west
     *
     * Coordinates are returned as (west, south, east, and north) extents,
     * i.e. min lng/lat, max lng/lat. Keep in mind that the north coordinate will
     * have a smaller tile index than the southern.
     *
     * For longitude, the values are _unclipped_: they may extend below -180 or
     * above 180. For example, a circle of 200 km at 179.9 deg longitude might run
     * from longitudes 175 to 184, four degrees off the right side of the map.
     *
     * Areas that extend through the poles necessarily sweep the entire range of
     * longitudes, meaning that the bounding box -- while accurate -- has a
     * significantly larger extent than the circle. See tilesCoveringCircle for
     * one way this can lead to grief.
     *
     * @param lng     Longitude of the point, in WGS-84 degrees
     * @param lat     Latitude of the point, in WGS-84 degrees
     * @param dist    Distance in meters
     * @return        [west, south, east, north] -- i.e. min lng/lat, max lng/lat
     */
    public double[] bboxWSENForCircle(double lng, double lat, double dist) {
      if (dist == 0) {             double[] point = { lng,     lat,   lng,  lat  }; return point; }
      if (dist >= globeCircum()) { double[] world = { -180.0, -90.0, 180.0, 90.0 }; return world; }
      //
      double lat_rad    = Math.toRadians(lat);
      double dist_rad   = dist / this.globe_radius;
      double north_lat  = Math.toDegrees(lat_rad + dist_rad);
      double south_lat  = Math.toDegrees(lat_rad - dist_rad);
      //
      if (north_lat >= 90 || south_lat <= -90) {
        // if either was equal-or-past and both are equal-not-past then neither was past...
        // it only kissed the pole, so sweep the hemisphere not the globe.
        double lng_d = (north_lat <= 90 && south_lat <= -90 ? 90 : 180);
        double[] sweeps_pole = { lng-lng_d, (south_lat < -90 ? -90 : south_lat), lng+lng_d, (north_lat >  90 ?  90 : north_lat) };
        return sweeps_pole;
      }
      //
      double lng_delta = Math.asin( Math.sin(dist_rad) / Math.cos(lat_rad) );
      lng_delta = (Double.isNaN(lng_delta) ? 90.0 : Math.toDegrees(lng_delta));
      double east_lng   = lng + lng_delta;
      double west_lng   = lng - lng_delta;
      //
      double[] coords = { west_lng, south_lat, east_lng, north_lat };
      return coords;
    }


  /**
   * Quadtiles covering the area within a given distance in meters from a point.
   *
   * The tiles will cover a larger extent than the circle itself, as it returns
   * all tiles that cover any part of the bounding box that covers the circle.
   * And for large areas, this may return tiles that do not actually intersect
   * the circle (especially for far-northerly points).
   *
   * However, when using this to partition big data sets, In our experience it's
   * rarely worth filtering out the bits in the corner (consider that a circle
   * occupies 79% of its bounding square), and even less worth fine-graining the
   * tile size (blowing up their count) to get a closer tiling. And since <a
   * href="http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates">
   * distance on a sphere isn't so simple</a> -- it can be the case that all
   * corners of a tile are not within your circle while yet parts of the tile
   * are -- doing that filtering naively isn't a good plan.
   *
   * Areas that extend through the poles necessarily sweep the entire range of
   * longitudes. If they also extend significantly southward, this will blow up
   * their tile coverage. For a sense of this, a 600 km circle at zoom level 6
   * on a Mercator grid requires 16 tiles when centered at London (lat 50.5), 25
   * centered at Reykjavik (lat 62.1), 180 at Alert, Canada (82.5, the most
   * northern occupied place) and 520 tiles at 84.8 deg.
   *
   * So start by using the complete but sometimes overexuberant sets this
   * returns, and see whether you care. If you find that the northern-latitudes
   * or lots-of-small-tiles cases are important, look to the (more expensive)
   * functions that find a minimal tile set using Geometry objects. Also
   * consider indexing far-northerly points using an alternate scheme.
   *
   * @param lng     Longitude of the point, in WGS-84 degrees
   * @param lat     Latitude of the point, in WGS-84 degrees
   * @param dist    Distance in meters
   * @param zl      Zoom level of detail for the tiles
   * @return        List of quadstr string handles for a superset of the covering tiles
   */
  public List<String> tilesCoveringCircle(double lng, double lat, double dist, int zl, Projection proj) {
    List<String> tiles = new ArrayList<String>();
    //
    // Get the max/min latitude to index
    double[] bbox = bboxWSENForCircle(lng, lat, dist);
    double   west = bbox[0], south = bbox[1], east = bbox[2], north = bbox[3];
    //
    // See if we wrapped off the edge of the world
    double   west_2 = 0, east_2 = 0;
    if      (west <= -180 && east >= 180){    east  =  180;      west   = -180; } // whole world
    else if (west < -180){ west_2 = west+360; east_2 = 180;      west   = -180; } // iterate  west+360..180 and -180..east
    else if (east >  180){ west_2 = -180;     east_2 = east-360; east   =  180; } // iterate -180..east-360 and  west..180
    //
    // Scan over tile indexes.
    int[] tij_min = QuadtileUtils.worldToTileIJ(west,  north,  zl, proj); // (lower tj is north)
    int[] tij_max = QuadtileUtils.worldToTileIJ(east,  south,  zl, proj);
    QuadtileUtils.addTilesCoveringIJRect(tij_min[0], tij_min[1], tij_max[0], tij_max[1], zl, tiles);
    //
    // If we wrapped, also contribute the wrapped portion
    if (west_2 != 0 || east_2 != 0) {
      tij_min = QuadtileUtils.worldToTileIJ(west_2, north,  zl, proj);
      tij_max = QuadtileUtils.worldToTileIJ(east_2, south,  zl, proj);
      QuadtileUtils.addTilesCoveringIJRect(tij_min[0], tij_min[1], tij_max[0], tij_max[1], zl, tiles);
    }
    //
    int[] tij_pt  = QuadtileUtils.worldToTileIJ(lng, lat, zl, proj);
    System.err.println(String.format("%10.5f %10.5f %2d %4d | %10.5f %10.5f %10.5f %10.5f %10.5f %10.5f | %5d < %5d > %5d | %5d < %5d > %5d",
        lng, lat, zl, tiles.size(),
        west, south, east, north, west_2, east_2,
        tij_min[0], tij_pt[0], tij_max[0],
        tij_min[1], tij_pt[1], tij_max[1]));
    //
    return tiles;
  }

  }


  public static class Equirectangular extends GlobeProjection {
    public static final double DEFAULT_MIN_LNG      =  -180.0;
    public static final double DEFAULT_MAX_LNG      =   180.0;
    public static final double DEFAULT_MIN_LAT      =  -90.0;
    public static final double DEFAULT_MAX_LAT      =   90.0;
    public static final double DEFAULT_GLOBE_RADIUS = 6378137.0;

    public Equirectangular(){
      super(DEFAULT_MIN_LNG, DEFAULT_MIN_LAT, DEFAULT_MAX_LNG, DEFAULT_MAX_LAT, DEFAULT_GLOBE_RADIUS);
    }

    /**
     * Projected grid coordinates for the given longitude / latitude pair.
     * Note the order: x-then-y.
     *
     * @param lng     Longitude of the point, in degrees
     * @param lat     Latitude of the point, in degrees
     * @return        { grid_x, grid_y }
     */
    public double[] lngLatToGridXY(double lng, double lat) {
      assert lng <= 180 && lng >= -180 && lat <= 90 && lat >= -90;
      //
      double   grid_x      = (lng + 180.0) / 360.0;
      double   grid_y      = (lat + 90.0)  / 180.0;
      //
      double[] grid_xy = { grid_x, grid_y };
      return   grid_xy;
    }

    /**
     * Longitude/latitude coordinates, in degrees, of the given point
     *
     * @param grid_x  Projected X value
     * @param grid_y  Projected Y value
     * @return        { longitude, latitude }
     */
    public double[] gridXYToLngLat(double grid_x, double grid_y) {
      double lng     = 360.0 * grid_x - 180.0;
      double lat     = 180.0 * grid_y -  90.0;
      //
      double[] result = {lng, lat};
      return result;
    }
  }


  /*
   *
   * The mercator maptile i/j/zl scheme is used by all popular online tile servers
   * -- open streetmap, google maps, bing maps, stamen, leaflet, others -- to
   * serve map imagery. It relies on a mercator projection that makes serious
   * geographers cry, but as anyone who internets should recognize is
   * exceptionally useful and ubiquitous. This provides methods for converting
   * from geographic coordinates to maptile handles.
   *
   * You don't have to use the Mercator projection for generating quadtile handles,
   * and apart from those with "Mercator" in the name these methods apply to any
   * quadtree scheme. The default concrete Quadtree class (QuadTreeImpl) in this
   * library simply partitions longitude and latitude uniformly.
   */
  public static class Mercator extends GlobeProjection {
    public static final double DEFAULT_MIN_LNG      =  -180.0;
    public static final double DEFAULT_MAX_LNG      =   180.0;
    public static final double DEFAULT_MIN_LAT      =  -85.05112878; // Math.atan(Math.sinh(Math.PI))*180/Math.PI; //
    public static final double DEFAULT_MAX_LAT      =   85.05112878;
    public static final double DEFAULT_GLOBE_RADIUS = 6378137.0;


  // The arctan/log/tan/sinh business gives slight loss of precision. We could
  // live with that on the whole, but it can push the boundary of a tile onto
  // the one above it so lnglatToTileIJ(tileIJToLnglat(foo)) != foo. Adding
  // this 1-part-per-billion fudge stabilized things; with this, no edge will
  // ever dance across tiles. Each of the following equivalents to the code
  // here or in tileIJToLnglat work, and none performed better.
  //
  // double lat_rad = Math.toRadians(lat);           // OSM version
  // double tj2     = mapsize * (1 - Math.log( Math.tan(lat_rad)  + (1/Math.cos(lat_rad)) )/Math.PI) / 2.0;
  // double sin_lat = Math.sin(lat * Math.PI / 180); // Bing version
  // double tj3     = mapsize * (0.5 - Math.log((1 + sin_lat) / (1 - sin_lat)) / (4 * Math.PI));
  // double lat2    = 180/Math.PI*Math.atan(Math.sinh(Math.PI * (1 - 2.0*tj/mapsize)));
  //

    public Mercator(){
      super(DEFAULT_MIN_LNG, DEFAULT_MIN_LAT, DEFAULT_MAX_LNG, DEFAULT_MAX_LAT, DEFAULT_GLOBE_RADIUS);
    }

    /**
     * Projected grid coordinates for the given longitude / latitude pair.
     * Note the order: x-then-y.
     *
     * @param lng     Longitude of the point, in degrees
     * @param lat     Latitude of the point, in degrees
     * @return        { grid_x, grid_y }
     */
    public double[] lngLatToGridXY(double lng, double lat) {
      assert lng <= 180 && lng >= -180 && lat <= 90 && lat >= -90;
      lng = NumberUtils.snap(lng,  min_lng,  max_lng);
      lat = NumberUtils.snap(lat,  min_lat,  max_lat);
      //
      double   grid_x      = (lng + 180.0) / 360.0;
      double   grid_y      = (1 - Math.log(Math.tan( (90 + lat)*Math.PI/360.0 ))/Math.PI) / 2;
      //
      double[] grid_xy = { grid_x, grid_y };
      return   grid_xy;
    }

    /**
     * Longitude/latitude WGS-84 coordinates, in degrees, of the given point in
     * the popular tileserver Mercator projection.
     *
     * @param grid_x  Projected X value
     * @param grid_y  Projected Y value
     * @return        { longitude, latitude }
     */
    public double[] gridXYToLngLat(double grid_x, double grid_y) {
      double lng     = 360.0 * grid_x - 180.0;
      double lat     = 90.0 - 360.0/Math.PI*Math.atan( Math.exp(Math.PI*(2*grid_y - 1)) );
      //
      double[] result = {lng, lat};
      return result;
    }

  }
}
