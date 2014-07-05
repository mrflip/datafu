package datafu.pig.geo;

import com.esri.core.geometry.*;
import com.esri.core.geometry.ogc.*;

public class GeometryUtils {
  public static final int WKID_UNKNOWN = 0;

  public static OGCGeometry payloadToGeom(String payload) {
    try {
      if (payload == null || payload.length() == 0){
        // LogUtils.Log_ArgumentsNull(LOG);
        return null;
      }
      OGCGeometry geom = OGCGeometry.fromText(payload);
      return geom;
    }
    catch (Exception err) {
      String msg = "Error loading payload ("+err.getMessage()+"): "+printablePayload(payload);
      System.err.println(msg);
      throw new RuntimeException(msg, err);
    }
  }

  public static String pigPayload(OGCGeometry geom) {
    try {
      return geom.asText();
    } catch (Exception err) {
      String msg = "Error serializing payload ("+err.getMessage()+"): "+geom;
      fuckYouError(msg, err);
      throw new RuntimeException(msg, err);
    }
  }

  // OGCGeometry.createFromEsriGeometry(env, null))

  public static String pigPayload(Geometry esGeom) {
    try {
      int wkt_flags = getWktExportFlag(esGeom);
      return GeometryEngine.geometryToWkt(esGeom, wkt_flags);
    } catch (Exception err) {
      String msg = "Error serializing payload ("+err.getMessage()+"): "+esGeom;
      fuckYouError(msg, err);
      throw new RuntimeException(msg, err);
    }
  }


  private static int getWktExportFlag(Geometry esGeom){
    Geometry.Type type = esGeom.getType();
    int           dim  = esGeom.getDimension();

    dump("%-10s %3d | %s", type, dim, esGeom);

    switch (type){
    case Polygon:
      // return WktExportFlags.wktExportPolygon; // fails with polygons masquerading as multipolygons
      return WktExportFlags.wktExportDefaults;
    case Point:              return WktExportFlags.wktExportPoint;
    case MultiPoint:         return WktExportFlags.wktExportMultiPoint;
    case Line:               return WktExportFlags.wktExportLineString;
    case Polyline:           return WktExportFlags.wktExportMultiLineString;
    default:
      return WktExportFlags.wktExportDefaults;
    }
  }

  /**
   * A snippet of the payload that can be dumped to the console or a backtrace.
   *
   * (remember, we're all pretending that the payload is a fancy, efficient
   * serialization and not easily-inspectable text.)
   */
  public static String printablePayload(String payload) {
    return snippetize(payload);
  }

  public static String snippetize(String str, int max_len) {
    return (str.length() > max_len ? str.substring(0,max_len-3)+"..." : str);
  }
  public static String snippetize(String str) {
    return snippetize(str, 200);
  }


  /**
   * Some of the geometry-api methods raise errors without strings.
   * This lets us track them down.
   */
  public static void fuckYouError(String msg, Throwable err) {
    GeometryUtils.dump("%s -- %s", msg, err.getMessage());
    for ( StackTraceElement etr: err.getStackTrace() ) {
      System.err.println(etr);
    }
  }


  public static void dump(String fmt, Object... args) {
    System.err.println("*******");
    System.err.println(String.format(fmt, args));
  }
}
