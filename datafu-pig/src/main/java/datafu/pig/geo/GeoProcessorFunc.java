/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datafu.pig.geo;

import java.io.IOException;
import org.apache.pig.data.DataType;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.pig.util.SimpleEvalFunc;

import datafu.pig.geo.GeometryUtils;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

/**
 *
 * Process a geometry into a new geometry
 */
abstract public class GeoProcessorFunc extends SimpleEvalFunc<String>
{
  public static String    op_name;

  abstract public Geometry process_geom(OGCGeometry geom);

  public String call(String payload) {
    OGCGeometry geom = GeometryUtils.payloadToGeom(payload);
    if (geom == null){ return null; }
    //
    try {
      Geometry result = process_geom(geom);
      //
      return GeometryUtils.pigPayload(result);
    }
    catch (Exception err) {
      String msg = String.format("Can't %s (%s): %s", op_name, err.getMessage(),
        GeometryUtils.printablePayload(payload));
      GeometryUtils.fuckYouError(msg, err);
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    return new Schema(new Schema.FieldSchema(op_name, DataType.CHARARRAY));
  }

// import com.esri.core.geometry.Operator;
// import com.esri.core.geometry.OperatorFactoryLocal;
  
// import com.esri.core.geometry.Envelope;
// import com.esri.core.geometry.Point;
// import com.esri.core.geometry.MultiPath;
// 
// import com.esri.core.geometry.OperatorBoundary;
// import com.esri.core.geometry.OperatorBuffer;
// import com.esri.core.geometry.OperatorConvexHull;
// // import com.esri.core.geometry.OperatorDensify;
// // import com.esri.core.geometry.OperatorEnvelope;
// // import com.esri.core.geometry.OperatorExteriorRing;
// import com.esri.core.geometry.OperatorGeneralize;
// import com.esri.core.geometry.OperatorOffset;
// import com.esri.core.geometry.OperatorSimplify;

  // class BoundaryHandler implements ProcessorHandler {
  //   public BoundaryHandler(String options) {
  //     if (! options.equals("")){ throw new IllegalArgumentException("boundary takes no options, got: "+options); }
  //   }
  //   public Geometry execute(OGCGeometry geom) {
  //       //
  //       OGCGeometry result = geom.boundary();
  //       //
  //       // if (boundGeom.geometryType().equals("MultiLineString") && ((OGCMultiLineString)boundGeom).numGeometries() == 1) { boundGeom = ((OGCMultiLineString)boundGeom).geometryN(0); } // match ST_Boundary/SQL-RDBMS
  //       //
  //       if (result == null) { return null; }
  //       return result.getEsriGeometry();
  //   }
  // }
  // 
  // class BufferHandler implements ProcessorHandler {
  //   OperatorBuffer operator;
  //   double         bufferDistance;
  //   public BufferHandler(String options) {
  //     this.bufferDistance = Double.parseDouble(options);
  //     this.operator = (OperatorBuffer)OperatorFactoryLocal.getInstance()
  //       .getOperator(Operator.Type.Buffer);
  //   }
  //   public Geometry execute(OGCGeometry geom) {
  //     Geometry result = operator.execute(geom.getEsriGeometry(),
  //       geom.getEsriSpatialReference(), bufferDistance, null);
  //     return result;
  //   }
  // }
  // 
  // /**
  //  * Calculates the convex hull geometry.
  //  *
  //  * Point      -- Returns the same point.
  //  * Envelope   -- returns the same envelope.
  //  * MultiPoint -- If the point count is one, returns the same multipoint. If the point count is two, returns a polyline of the points. Otherwise, computes and returns the convex hull polygon.
  //  * Segment    -- Returns a polyline consisting of the segment.
  //  * Polyline   -- If consists of only one segment, returns the same polyline. Otherwise, computes and returns the convex hull polygon.
  //  * Polygon    -- If more than one path or if the path isn't already convex, computes and returns the convex hull polygon. Otherwise, returns the same polygon.
  //  */
  // class ConvexHullHandler implements ProcessorHandler {
  //   OperatorConvexHull operator;
  //   public ConvexHullHandler(String options) {
  //     if (! options.equals("")){ throw new IllegalArgumentException("convex_hull takes no options, got: "+options); }
  //     this.operator = (OperatorConvexHull)OperatorFactoryLocal.getInstance()
  //       .getOperator(Operator.Type.ConvexHull);
  //   }
  //   public Geometry execute(OGCGeometry geom) {
  //     Geometry result = operator.execute(geom.getEsriGeometry(), null);
  //     return result;
  //   }
  // }
  // 
  // class EndPointHandler implements ProcessorHandler {
  //   public EndPointHandler(String options) {
  //     if (! options.equals("")){ throw new IllegalArgumentException("end_point takes no options, got: "+options); }
  //   }
  //   public Geometry execute(OGCGeometry geom) {
  //     MultiPath lines = (MultiPath)(geom.getEsriGeometry());
  //     Geometry result = (Geometry)lines.getPoint(lines.getPointCount()-1);
  //     return result;
  //   }
  // }
  // 
  //     // DENSIFY,
  //   // END_POINT,
  //   // ENVELOPE,
  //   // EXTERIOR_RING,
  //   // GENERALIZE,
  //   // NTH_POINT
  //   // OFFSET,
  //   // SIMPLIFY,
  //   // START_POINT,
  

}
