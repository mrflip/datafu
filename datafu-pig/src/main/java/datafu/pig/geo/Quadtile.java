// package datafu.pig.geo;
//
// import java.util.Iterator;
// import java.util.ArrayList;
// import java.util.List;
//
// import org.apache.pig.data.Tuple;
// import org.apache.pig.data.TupleFactory;
// import org.apache.pig.data.BagFactory;
// import org.apache.pig.data.DataBag;
//
// import com.esri.core.geometry.Geometry;
// import com.esri.core.geometry.ogc.OGCGeometry;
//
// import com.esri.core.geometry.Envelope;
// import com.esri.core.geometry.Envelope2D;
// import com.esri.core.geometry.GeometryEngine;
// import com.esri.core.geometry.Point;
// import com.esri.core.geometry.QuadTree;
// import com.esri.core.geometry.QuadTree.QuadTreeIterator;
// import com.esri.core.geometry.SpatialReference;
//
// public class Quadtile {
//   private final long quadkey;
//   private final long zl;
//   private int tile_x = null;
//   private int tile_y = null;
//
//   /* ***************************************************************************
//    *
//    * Constructor / Factories
//    *
//    */
//
//   public Quadtile(String quadstr) {
//     qk = QuadtileUtils.quadstrToQuadkey(quadstr);
//     this(qk, quadstr.length());
//   }
//
//   public Quadtile(int tx, int ty, int zl) {
//     this( QuadtileUtils.tileXYToQuadkey(tx, ty, zl) );
//     this.tile_x = tx;
//     this.tile_y = ty;
//   }
//
//   public Quadtile(long qk, int zl) {
//     this.quadkey = qk;
//     this.zl      = zl;
//   }
//
//   public static Quadtile quadtileContaining(Geometry geom) {
//     //   Coordinate nw = new Coordinate(west, north);
//     //   Coordinate ne = new Coordinate(east, north);
//     //   Coordinate sw = new Coordinate(west, south);
//     //   Coordinate se = new Coordinate(east, south);
//     //   Coordinate[] bboxCoordinates = {nw, ne, se, sw, nw};
//     //   LinearRing bboxRing  = geomFactory.createLinearRing(bboxCoordinates);
//     //   Polygon    bbox_poly = geomFactory.createPolygon(bboxRing, null);
//     //   return     bbox_poly;
//     // }
//   }
//
//   public static Quadtile quadtileContaining(double west, double south, double east, double north) {
//
//   }
//
//   public static Quadtile quadtileContaining(double lng, double lat) {
//
//   }
//
//
    // For things that cover the poles, we can either clip the circle, or find
    // the "north" lat (on the other side of the pole) and have the longitude
    // sweep -180 to 180. The latter can blow up Finland -- a large-enough
    // circle to hit the poles will sweep south enough that we end up tiling
    // most of the hemisphere. Clipping (the current behavior) means we lose
    // results that might extend on the other side. But anyone willing to use
    // the Mercator projection has already sacrificed Santa Claus to the
    // purposes of convenience, I'd rather have Hammerfest miss the Yukon than
    // blow up Reykjavik. But remember, this is undefined behavior, so you must
    // not count on it remaining the same.
    //
    // north_lat = NumberUtils.snap(north_lat, MIN_MERC_LAT, MAX_MERC_LAT);
    // south_lat = NumberUtils.snap(south_lat, MIN_MERC_LAT, MAX_MERC_LAT);
//       // #
//       // # Bounding box coordinates
//       // #
//       //
//       // # Convert a quadkey into a bounding box using adjacent tile
//       // def quadkey_to_bbox(quadkey)
//       //   tile_x, tile_y, zl = quadkey_to_tile_xy_zl(quadkey)
//       //   # bottom right of me is top left of my southeast neighbor
//       //   left,  top = tile_xy_zl_to_lng_lat(tile_x,     tile_y,     zl)
//       //   right, btm = tile_xy_zl_to_lng_lat(tile_x + 1, tile_y + 1, zl)
//       //   [left, btm, right, top]
//       // end
//       //
//       // # Retuns the smallest quadkey containing both of corners of the given bounding box
//       // def quadkey_containing_bbox(left, btm, right, top)
//       //   qk_tl = lng_lat_zl_to_quadkey(left,  top, 23)
//       //   qk_2  = lng_lat_zl_to_quadkey(right, btm, 23)
//       //   # the containing qk is the longest one that both agree on
//       //   containing_key = ""
//       //   qk_tl.chars.zip(qk_2.chars).each do |char_tl, char_2|
//       //     break if char_tl != char_2
//       //     containing_key << char_tl
//       //   end
//       //   containing_key
//       // end
//       //
//       // # Returns a bounding box containing the circle created by the lat/lng and radius
//       // def lng_lat_rad_to_bbox(longitude, latitude, radius)
//       //   left, _    = point_east( longitude, latitude, -radius)
//       //   _,     btm = point_north(longitude, latitude, -radius)
//       //   right, _   = point_east( longitude, latitude,  radius)
//       //   _,     top = point_north(longitude, latitude,  radius)
//       //   [left, btm, right, top]
//       // end
//
//       // # Convert latitude in degrees to integer tile x,y coordinates at given
//       // # zoom level.  Assigns points outside the tile coverage to "0000..."
//       // # (north) and "33333..." (south) rather than raising an error.
//       // def point_to_quadkey_withpoles(longitude, latitude)
//       //   if    (MAX_LATITUDE ..  90.0).include?(latitude) then return ("0"*POINT_ZL)
//       //   elsif (-90.0 .. MIN_LATITUDE).include?(latitude) then return ("3"*POINT_ZL) end
//       //   lng_lat_zl_to_quadkey(longitude, latitude, POINT_ZL)
//       // end
//
//
//   /* ***************************************************************************
//    *
//    * Geometry Methods
//    *
//    */
//
//   public Geometry geomExtent() {
//   }
//
//   public List<Quadtile> neighborhoodList() {
//   }
//
//   public Quadtile[] neighborhood_9() {
//   }
//
//   public Quadtile[] descendants(int child_zl) throws RuntimeException {
//     int zl_diff = child_zl - zl;
//     if (zl_diff < 0) { throw new RuntimeException("Asked for children at higher zoom level than tile: tile is "+zl+"; requested "+child_zl); }
//
//     Quadtile[] result = [];
//     long qk_base = quadkey << zl_diff;
//     for (offset = 0; offset < (1 << zl_diff); offset++) {
//       result[i] = new Quadtile(qk_base | offset);
//     }
//     return result;
//   }
//
//   public Quadtile[] children() {
//     return descendants(zl+1);
//   }
//
//   /**
//      The desired behavior is to return an empty string if the geometry is
//      too large to be inside even the lowest resolution quadstr.
//   */
//   public static String quadtileCovering(Geometry g, int zl) {
//     Point centroid = g.getCentroid();
//     for (int i = zl; i > 0; i--) {
//       String  quadstr    = geoPointToQuadstr(centroid.getX(), centroid.getY(), i);
//       Polygon quadstrBox = quadstrToBox(quadstr);
//       if (quadstrBox.contains(g)) return quadstr;
//     }
//     return "";
//   }
//
//   public static List<String> childrenContaining(Geometry geom, String parent) {
//     List<String> children = childrenFor(parent);
//     List<String> returnChildren = new ArrayList<String>();
//     for (String child : children) {
//       Polygon quadstrBox = quadstrToBox(child);
//       if (quadstrBox.intersects(g)) {
//         returnChildren.add(child);
//       }
//     }
//     return returnChildren;
//   }
//
//
//   /**
//      Recursively search through quadstr for overlapping with the passed in geometry.
//   */
//   public static boolean checkQuadstr(String quadstr, DataBag returnKeys, Geometry g, int maxDepth) {
//     // Compute bounding box for the tile
//     Polygon keyBox = quadstrToBox(quadstr);
//     if (returnKeys.size() > MAX_TILES) return false;
//
//     if (keyBox.intersects(g)) {
//       if (quadstr.length() >= maxDepth ) {
//         Tuple quadstrTuple = tupleFactory.newTuple(quadstr);
//         returnKeys.add(quadstrTuple);
//         return true;
//       }
//       List<String> children = childrenFor(quadstr);
//
//       Geometry cut = g.intersection(keyBox);
//       cut = (cut.getGeometryType().equals(GEOM_COLLEC) ? cut.getEnvelope() : cut );
//
//       for (String child : children) {
//         checkQuadstr(child, returnKeys, cut, maxDepth);
//       }
//     }
//     return true;
//   }
//
//   /* ***************************************************************************
//    *
//    * Handles and coordinates
//    *
//    */
//
//   public int getTileX() {
//     if (tile_x == null) {
//       int[] tile_xy = QuadkeyUtils.quadkeyToTileXY(quadkey);
//       this.tile_x = tile_xy[0];
//       this.tile_y = tile_xy[0];
//     }
//     return tile_x;
//   }
//
//   /**
//    * Returns the corner WGS84 coordinates of the tile:
//    *
//    * [west, south, east, north​] (i.e. min_x, min_y, max_x, max_y)
//    *
//    * @return west, south, east, north coordinates
//    */
//   public double[] cornerCoords() {
//   }
//
//
//
//   /**
//      Get all tiles overlapping the given geometry at the specified zoom level.
//   */
//   public static DataBag allTilesFor(Geometry g, int maxDepth) {
//     String       container   = containingQuadtile(g, maxDepth);
//     List<String> keysToCheck = childrenFor(container);
//     DataBag      returnKeys  = bagFactory.newDefaultBag();
//
//     for (String key : keysToCheck) {
//       boolean fullySearched = checkQuadstr(key, returnKeys, g, maxDepth);
//       // If there are ever too many tiles, stop everything and return empty bag
//       if (!fullySearched) {
//         System.out.println("Too many tiles! ["+returnKeys.size()+"]");
//         returnKeys.clear();
//         return returnKeys;
//       }
//     }
//     return returnKeys;
//   }


  // /**
  //  * Latitude (WGS-84 degrees) directly north by the given distance from that point
  //  *
  //  * Note that the result is <em>capped at the poles</em> -- walking north from
  //  * 88 deg latitude by the distance equivalent of 10 degrees does not carry
  //  * through to 82 degrees, but stops at the pole.
  //  */
  // public static double latNorth(double lat, double distance) {
  //   // This would give you the 'wrapped' version. There's probably something smarter than taking arcsin(sin(x)).
  //   // double north_lat = Math.toDegrees( Math.asin(Math.sin(lat_rad + dist_rad)) );
  //   // System.err.println(String.format("%10s %10.5f %12.0f %10.5f %10.5f", "", lat, distance, north_lat, Math.toDegrees(Math.asin(Math.sin(lat_rad + dist_rad)))));
  //   //
  //   double dist_rad   = distance / GLOBE_RADIUS;
  //   double lat_rad    = Math.toRadians(lat);
  //   return NumberUtils.snap(Math.toDegrees(lat_rad + dist_rad), -90.0, 90.0);
  // }
  //
  // /**
  //  * Latitude (WGS-84 degrees) directly south by the given distance from that point
  //  */
  // public static double latSouth(            double lat, double distance) {
  //   return latNorth(lat, -distance);
  // }
  // // longitude not needed for calculation, but provided for symmetry
  // public static double latNorth(double lng, double lat, double distance) {
  //   return latNorth(lat, distance);
  // }
  // public static double latSouth(double lng, double lat, double distance) {
  //   return latNorth(lat, -distance);
  // }
  //
  // /**
  //  * Longitude (WGS-84 degrees) directly east by the given distance from that point
  //  */
  // public static double lngEast(double lng, double lat, double distance) {
  //   //
  //   double lng_rad = Math.toRadians(lng) + (distance/radiusOfLatitude(lat));
  //   lng_rad = ((lng_rad + Math.PI) % NumberUtils.PI_X2) + Math.PI;
  //   return Math.toDegrees(lng_rad);
  // }
  //
  // /**
  //  * Radius for the Parallel of Latitude at the given latitude in degrees.  This
  //  * is the radius of the circle of constant latitude in a spherical-earth
  //  * model.
  //  */
  // public static double radiusOfLatitude(double lat) {
  //   return GLOBE_RADIUS * Math.cos(Math.toRadians(lat));
  // }


  // // Size of one tile == S = GLOBE_CIRCUM * cos(ty) / 2^zl

/*
 *
 * Discarded code to cull tiles from a circle. Turns out there's not enough to discard to be worth it.
 *
 */



  // /**
  //  *
  //  * @param lat1     The y coordinate of the first point, in radians
  //  * @param lng1     The x coordinate of the first point, in radians
  //  * @param lat2     The y coordinate of the second point, in radians
  //  * @param lng2     The x coordinate of the second point, in radians
  //  * @return The distance between the two points, as determined by the Haversine formula, in radians.
  //  *
  //  * From Spatial4j -- http://grepcode.com/file_/repo1.maven.org/maven2/com.spatial4j/spatial4j/0.2/com/spatial4j/core/distance/DistanceUtils.java/?v=source
  //  */
  // public static double haversineDistRad(double lng1, double lat1, double lng2, double lat2) {
  //   if (lat1 == lat2 && lng1 == lng2) { return 0.0; }
  //   //
  //   double hsinX = Math.sin((lng1 - lng2) * 0.5);
  //   double hsinY = Math.sin((lat1 - lat2) * 0.5);
  //   double h = hsinY * hsinY +
  //           (Math.cos(lat1) * Math.cos(lat2) * hsinX * hsinX);
  //   return 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
  // }
    //
    // Iterate across the tiles; add ones which poke into our circle
    //
    // For latitude:
    //   above => tile south, below => tile north, same => point's latitude
    // For longitude:
    //   left => tile east;   right => tile west; same row, use point's latitude
    //
    // double test_lat_rad, test_lng_rad;
      // if        (ty_q < txy_pt[1]) { test_lat_rad = NumberUtils.PI_D2 - 2*Math.atan( Math.exp( (ty_q+1)*NumberUtils.PI_X2/mapsize - Math.PI) ); }
      // else if   (ty_q > txy_pt[1]) { test_lat_rad = NumberUtils.PI_D2 - 2*Math.atan( Math.exp( (ty_q  )*NumberUtils.PI_X2/mapsize - Math.PI) ); }
      // else                         { test_lat_rad = lat_rad; }
      //   if      (tx_q < txy_pt[0]) { test_lng_rad = NumberUtils.PI_X2 * (tx_q+1) / mapsize - Math.PI; }
      //   else if (tx_q > txy_pt[0]) { test_lng_rad = NumberUtils.PI_X2 * (tx_q  ) / mapsize - Math.PI; }
      //   else                       { test_lng_rad = lng_rad; }

        // if (haversineDistRad(test_lng_rad, test_lat_rad, lng_rad, lat_rad) <= dist_rad) {
  // System.err.println(String.format("%s\t%-12s %8d %8d %8d %8d | %10.5f %10.5f | %10.5f %10.5f %10.5f %10.5f",
  //     ""+added, quadstr, tx_q, ty_q, txy_pt[0], txy_pt[1], lng_rad, lat_rad, lng, lat));
  // double tile_width = Math.toRadians(lng) + (distance/radiusOfLatitude(lat));
  //
  // // Get the tile's minimum width in meters -- top of the box north of the
  // // equator, bottom of the box south of the equator.
  //
  // int    mapsize    = mapTileSize(zl);
  //
  // double par_radius = GLOBE_RADIUS * Math.cos(t_lat_rad);
  //
  // // Get the distance to the nearest edge of the tile
  //
  // double edge_rad_dist = t_lat_rad - lat_rad;
  //
  // Find the remaining distance to
  //
