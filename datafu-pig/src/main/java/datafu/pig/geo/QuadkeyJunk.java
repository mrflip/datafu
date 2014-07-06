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

/*
*
* Excised code to cull tiles from a circle. Turns out there's not enough to cull to be worth it.
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
//
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
