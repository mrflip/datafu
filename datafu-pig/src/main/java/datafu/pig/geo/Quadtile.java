package datafu.pig.geo;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.WktExportFlags;
import com.esri.core.geometry.ogc.OGCGeometry;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;

public class Quadtile {
  private final long qk;
  private final int  zl;
  private final int  tx;
  private final int  ty;
  //
  private Envelope envelope;
  private Geometry fragment;

  /* ***************************************************************************
   *
   * Constructor / Factories
   *
   */

  public Quadtile(long quadkey, int zoomlvl) {
    this.qk = quadkey;
    this.zl = zoomlvl;
    int[] tile_xy = QuadkeyUtils.quadkeyToTileXY(quadkey);
    this.tx = tile_xy[0];
    this.ty = tile_xy[1];
  }

  public Quadtile(int tile_x, int tile_y, int zoomlvl) {
    this.tx = tile_x;
    this.ty = tile_y;
    this.zl = zoomlvl;
    this.qk = QuadkeyUtils.tileXYToQuadkey(tx, ty);
  }

  public Quadtile(String quadstr) {
    this(QuadkeyUtils.quadstrToQuadkey(quadstr), QuadkeyUtils.quadstrToZl(quadstr));
  }

  /**
   *
   * Smallest quadtile (i.e. most fine-grained zoom level) containing the given object.
   *
   * Be aware that shapes which wrap around the edge of the map by any amount -- Kiribati
   * and American Samoa sure, but also Russia and Alaska -- will end up at zoom level
   * zero. Yikes.
   *
   */
  public static Quadtile quadtileContaining(Geometry geom, int zoomlvl) {
    Envelope env = new Envelope();
    geom.queryEnvelope(env);
    long qk_lfup = QuadkeyUtils.mercatorToQuadkey(env.getXMin(), env.getYMax(), zoomlvl);
    long qk_rtdn = QuadkeyUtils.mercatorToQuadkey(env.getXMax(), env.getYMin(), zoomlvl);
    long[] qk_zl = QuadkeyUtils.smallestContaining(qk_lfup, qk_rtdn, zoomlvl);
    Quadtile quadtile = new Quadtile(qk_zl[0], (int)qk_zl[1]);

    String geom_str = GeometryEngine.geometryToWkt(env, WktExportFlags.wktExportDefaults);

    GeometryUtils.dump("%d %d %s %s %d %d | %s | %s", qk_lfup, qk_rtdn,
      QuadkeyUtils.quadkeyToQuadstr(qk_lfup, zoomlvl), QuadkeyUtils.quadkeyToQuadstr(qk_rtdn, zoomlvl),
      qk_zl[0], qk_zl[1], quadtile, geom_str);
    return quadtile;
  }

  public static Quadtile quadtileContaining(Geometry geom) {
    return quadtileContaining(geom, QuadkeyUtils.MAX_ZOOM_LEVEL);
  }

  public static Quadtile quadtileContaining(double lng, double lat, int zoomlvl) {
    long quadkey = QuadkeyUtils.mercatorToQuadkey(lng, lat, zoomlvl);
    return new Quadtile(quadkey, zoomlvl);
  }

  public static Quadtile quadtileContaining(double lng, double lat) {
    return quadtileContaining(lng, lat, QuadkeyUtils.MAX_ZOOM_LEVEL);
  }

  public static Quadtile quadtileContaining(double lf, double dn, double rt, double up, int zoomlvl) {
    long qk_lfup = QuadkeyUtils.mercatorToQuadkey(lf, up, zoomlvl);
    long qk_rtdn = QuadkeyUtils.mercatorToQuadkey(rt, dn, zoomlvl);
    long[] qk_zl = QuadkeyUtils.smallestContaining(qk_lfup, qk_rtdn, zoomlvl);
    Quadtile quadtile = new Quadtile(qk_zl[0], (int)qk_zl[1]);
    //
    return quadtile;
  }

  public static Quadtile quadtileContaining(double lf, double dn, double rt, double up) {
    return quadtileContaining(lf, dn, rt, up, QuadkeyUtils.MAX_ZOOM_LEVEL);
  }

  public Envelope getEnvelope() {
    if (envelope != null) { return envelope; } // memoize
    double[] coords = w_s_e_n();
    this.envelope = new Envelope(coords[0], coords[1] + 1e-6, coords[2] - 1e-6, coords[3]);
    // this.envelope = new Envelope(tile_x, tile_y, tile_x+1, tile_y+1);
    return envelope;
  }

  public double[] w_s_e_n() {
    return QuadkeyUtils.tileXYToCoords(tx, ty, zl);
  }

  public String quadstr() { return QuadkeyUtils.quadkeyToQuadstr(qk, zl); }
  public long   quadkey() { return qk; }
  public int    zoomlvl() { return zl; }
  public int    tileX()   { return tx; }
  public int    tileY()   { return ty; }
  public int[]  tileXY()  { int[] tile_xy  = { tx, ty } ;     return tile_xy;  }
  public int[]  tileXYZ() { int[] tile_xyz = { tx, ty, zl } ; return tile_xyz; }

  public String toString() {
    double[] coords = w_s_e_n();
    return String.format("%s %-10s@%2d [%4d %4d] (%6.1f %5.1f %6.1f %5.1f)",
      // this.getClass().getSimpleName(), // TODO: probably should be class, but screen space
      "QT", quadstr(), zl, tx, ty, coords[0], coords[1], coords[2], coords[3]);
  }


  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   *
   * Related Quadtiles
   *
   */

  public Quadtile[] children() {
    Long[]      child_qks   = QuadkeyUtils.quadkeyChildren(qk);
    int         child_zl    = this.zl + 1;
    Quadtile[]  child_tiles = {null, null, null, null};
    for (int ci = 0; ci < child_qks.length; ci++) {
      child_tiles[ci] = new Quadtile(child_qks[ci], child_zl);
    }
    return child_tiles;
  }

  /**
   * Quadtile at the given zoom level containing this quadtile.
   *
   * @param zl_anc    Zoom level of detail for the ancestor. Must not be finer than the tile's zoomlvl
   * @return quadtile at the given zoom level and which contains or equals this one.
   */
  public Quadtile ancestor(int zl_anc) {
    long qk_anc = QuadkeyUtils.quadkeyAncestor(qk, zl, zl_anc);
    return new Quadtile(qk_anc, zl_anc);
  }


  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   *
   * Decompose Shape
   *
   */

  /**
   *
   * List of all quadtiles at the given zoom level that intersect the object. The object
   * will lie completely within the union of the returned tiles; and every returned tile
   * intersects with the object.
   *
   */
  public static List<Quadtile> quadtilesCovering(Geometry esGeom, int zl_coarse, int zl_fine) {
    Quadtile start_quad = quadtileContaining(esGeom, zl_fine);
    return start_quad.decompose(esGeom, zl_coarse, zl_fine);
  }
  public static List<Quadtile> quadtilesCovering(OGCGeometry geom, int zl_coarse, int zl_fine) {
    return quadtilesCovering(geom.getEsriGeometry(), zl_coarse, zl_fine);
  }

  public List<Quadtile> decompose(Geometry geom, int zl_coarse, int zl_fine) {
    List<Quadtile> quads = new ArrayList<Quadtile>();
    dump("%-20s |  %s", "start", geom);
    addDescendantsIntersecting_(quads, geom, zl_coarse, zl_fine);
    return quads;
  }

  public void dump(String fmt, Object... args) {
    fmt = String.format("******\t%30s| %s", this.toString(), fmt);
    System.err.println(String.format(fmt, args));
  }

  // make this be a bag of (quadkey, quadtile, clipped geom) objects
  protected void addDescendantsIntersecting_(List<Quadtile> quads, Geometry geom, int zl_coarse, int zl_fine) {
    // intersect the object with our envelope
    Geometry geom_on_tile = GeometryEngine.intersect(geom, getEnvelope(), null);

    dump("%-20s %2d->%2d %3d %18s %s %s", "Decomposing", zl_coarse, zl_fine, quads.size(), geom,
      // envelope, geom_on_tile
      envelope, "");

    if (geom_on_tile.isEmpty()) {
      // intersection is empty: add nothing to the list and return.
      dump("%-20s %2d->%2d %3d %s", "No intersection", zl_coarse, zl_fine, quads.size(), geom_on_tile);
      return;
      //
    } else if (this.zl  > zl_fine) {
      // zl finer than limit: zoom out to zl, add to list, return
      // dump("%-20s %2d->%2d %3d", "zl finer than limit", zl_coarse, zl_fine, quads.size());
      quads.add(ancestor(zl_fine));
      //
    } else if (this.zl == zl_fine) {
      // zl at finest limit: add self
      // dump("%-20s %2d->%2d %3d", "zl meets finest limit", zl_coarse, zl_fine, quads.size());
      quads.add(this);
      //
    } else if ((this.zl >= zl_coarse) && GeometryEngine.within(getEnvelope(), geom, null)) {
      // completely within object: add self, return
      dump("%-20s %2d->%2d %3d %s contains %s", "contained in shape", zl_coarse, zl_fine, quads.size(), geom_on_tile, getEnvelope());
      quads.add(this);
      //
    } else {
      // otherwise, decompose, add those tiles.
      dump("%-20s %2d->%2d %3d %s", "recursing", zl_coarse, zl_fine, quads.size(), geom_on_tile);
      Quadtile[] child_tiles = children();
      child_tiles[0].addDescendantsIntersecting_(quads, geom, zl_coarse, zl_fine);
      child_tiles[1].addDescendantsIntersecting_(quads, geom, zl_coarse, zl_fine);
      child_tiles[2].addDescendantsIntersecting_(quads, geom, zl_coarse, zl_fine);
      child_tiles[3].addDescendantsIntersecting_(quads, geom, zl_coarse, zl_fine);
    }
  }

}


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
