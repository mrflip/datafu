package datafu.pig.geo;

import com.esri.core.geometry.*;
import com.esri.core.geometry.ogc.*;

public class GeometryUtils {
  public static final int WKID_UNKNOWN = 0;

  /**
   *
   * Extracts a fully-baked OGCGeometry object from its opaque pig payload. This
   * method will take care of receiving a null or empty payload, logging and/or
   * re-throwing error conditions, and all other decisions. If you get a null
   * record back, you should silently skip it and carry on; any exceptions
   * raised should belong to this method.
   *
   * Standard stanza:
   *
   *    OGCGeometry geom = GeometryUtils.payloadToGeom(payload);
   *    if (geom == null){ return null; } // or continue, or whatever
   *
   * Unless you _really_ need to have different behavior than all the other
   * spatial methods, don't do any handling beyond skipping a null response.
   *
   * Also you will notice that the payload is not so opaque as we claimed. It's
   * a text string holding the geometry description in WKT (well-known text)
   * format. Besides being bulky and inefficient to de/serialize, it washes out
   * desirable metadata. But! it's really friendly to work with, and means we
   * can concentrate on getting the whole works functional.
   *
   */
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
      // TODO: log and return null. During development, re-raise.
      // return null;
      throw new RuntimeException(msg, err);
    }
  }

  public static String pigPayload(OGCGeometry geom) {
    if (geom == null) { return null; }
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
    if (esGeom == null) { return null; }
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

  public static Envelope2D getEnvelope2D(OGCGeometry geom) {
    return getEnvelope2D(geom.getEsriGeometry());
  }

  public static Envelope2D getEnvelope2D(Geometry geom) {
    Envelope2D env = new Envelope2D();
    geom.queryEnvelope2D(env);
    return env;
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
    System.err.println(String.format("******\t"+fmt, args));
  }
}
