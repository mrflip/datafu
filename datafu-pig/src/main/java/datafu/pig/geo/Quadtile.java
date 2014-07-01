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
