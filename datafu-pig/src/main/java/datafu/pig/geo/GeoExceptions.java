package datafu.pig.geo;

import com.esri.core.geometry.ogc.OGCGeometry;
import datafu.pig.geo.GeometryUtils;
import org.apache.commons.logging.Log;

public final class GeoExceptions {

  /**
   * Arguments passed to evaluate are null
   * @param logger
   */
  public static class NullArgumentException extends IllegalArgumentException {
    public static String printable() {
      return "One or more arguments are null.";
    }
  }

  public static class MissingArgumentException extends IllegalArgumentException {
    public static String printable() {
      return "Expected one or more arguments.";
    }
  }

  public static class ExpectedPairsException extends IllegalArgumentException {
    public static String printable() {
      return "Expected one or more x,y pairs.";
    }
    public static String printable(int array_argument_index) {
      return GeometryUtils.printableMessage(
        "Expected one or more x,y pairs in array argument %d.",
        array_argument_index);
    }
  }

  // public static void Log_InvalidType(Log logger, OGCType expecting, OGCType actual){
  // 	logger.error(String.format("Invalid geometry type.  Expecting %s but found %s", expecting, actual));
  // }

  public static class MismatchedSpatialReferencesException extends IllegalArgumentException {
    /**
     * Comparing geometries in different spatial references
     *
     * @param wkid_a -- wkid (well-known id) of the first spatial coordinate reference
     * @param wkid_b -- wkid (well-known id) of the other spatial coordinate reference
     */
    public static String printable(int wkid_a, int wkid_b) {
      return GeometryUtils.printableMessage("Mismatched spatial references: (%d != %d)",
        wkid_a, wkid_b);
    }
  }
    
  public static class InvalidTextException extends IllegalArgumentException {
    public static String printable(String text) {
      return GeometryUtils.printableMessage("Ill-formed text: (%s)",
        GeometryUtils.snippetize(text));
    }
  }

  public static class InvalidRangeException extends IllegalArgumentException {
    public static String printable(int actual, int expMin, int expMax){
      return GeometryUtils.printableMessage("Expected value in range [%d, %d]; (actual %d).",
        expMin, expMax, actual);
    }
  }

  public static class Not3DException extends IllegalArgumentException {
    public static String printable(OGCGeometry geom) {
      return "Expected 3D shape, got (%s)";
    }
  }

  public static class NotMeasuredException extends IllegalArgumentException {
    public static String printable(OGCGeometry geom) {
      return "Expected measured (4-D) shape, got (%s)";
    }
  }

  public static class InternalError extends RuntimeException {
    public static String printable(String text) {
      return GeometryUtils.printableMessage("Internal error - %s", text);
    }
  }

  public static class ExceptionThrown extends RuntimeException {
    public static String printable(String method, Exception err) {
      return GeometryUtils.printableMessage("Exception %s thrown by %s", err.getMessage(), method);
    }
  }


  // private static final int MSG_SRID_MISMATCH            = 0;
  // private static final int MSG_ARGUMENTS_NULL           = 1;
  // private static final int MSG_ARGUMENT_LENGTH_XY       = 2;
  // private static final int MSG_MULTI_ARGUMENT_LENGTH_XY = 3;
  // private static final int MSG_INVALID_TYPE             = 4;
  // private static final int MSG_INVALID_TEXT             = 5;
  // private static final int MSG_INVALID_INDEX            = 6;
  // private static final int MSG_INTERNAL_ERROR           = 7;
  // private static final int MSG_ARGUMENT_LENGTH          = 8;
  // private static final int MSG_EXCEPTION_THROWN         = 9;
  // private static final int MSG_NOT_3D                   = 10;
  // private static final int MSG_NOT_MEASURED             = 11;
}
