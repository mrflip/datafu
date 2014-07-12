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

import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;
//
import org.apache.pig.EvalFunc;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.backend.executionengine.ExecException;
import com.google.common.base.CaseFormat;

import datafu.pig.geo.GeometryUtils;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.ogc.OGCGeometry;

public class QuadtileHandle extends EvalFunc<Tuple> {
  public final static Projection.Linear proj_1280 = new Projection.Linear(1280);

  public QtConvertsFrom   converter_from;
  public QtConvertsInto   converter_into;
  public ConverterType    conv_from_type;
  public ConverterType    conv_into_type;
  public String           conv_from_str;
  public String           conv_into_str;
  public String           op_name;

  /**
   * We uppercase the string, so you can supply the name as say 'intersection'
   */
  public enum ConverterType {
    QMORTON_ZL, QUADSTR, QUADORD, TILE_IJ_ZL, GRID_XY, LNG_LAT, WSEN,
  }

  public QuadtileHandle(String from_name, String into_name) {
    super();
    this.conv_from_str  = from_name.toLowerCase();
    this.conv_into_str  = into_name.toLowerCase();
    this.conv_from_type = ConverterType.valueOf(conv_from_str.toUpperCase());
    this.conv_into_type = ConverterType.valueOf(conv_into_str.toUpperCase());
    this.op_name        = conv_into_str;

    switch (conv_from_type){
    case QMORTON_ZL: this.converter_from = new FromQmortonZl(); break;
    case QUADSTR:    this.converter_from = new FromQuadstr();   break;
    case QUADORD:    this.converter_from = new FromQuadord();   break;
    case TILE_IJ_ZL: this.converter_from = new FromTileIJZl();  break;
    case GRID_XY:    this.converter_from = new FromGridXY();    break;
    case LNG_LAT:    this.converter_from = new FromLngLat();    break;
    case WSEN:       this.converter_from = new FromWsen();      break;
    default:
      throw new RuntimeException("Conversion from "+ conv_from_str + " not implemented");
    }
    
    switch (conv_into_type){
    case QMORTON_ZL: this.converter_into = new IntoQmortonZl(); break;
    case QUADSTR:    this.converter_into = new IntoQuadstr();   break;
    case QUADORD:    this.converter_into = new IntoQuadord();   break;
    case TILE_IJ_ZL: this.converter_into = new IntoTileIJZl();  break;
    case GRID_XY:    this.converter_into = new IntoGridXY();    break;
    case LNG_LAT:    this.converter_into = new IntoLngLat();    break;
    case WSEN:       this.converter_into = new IntoWsen();      break;
    default:
      throw new RuntimeException("Conversion from "+ conv_into_str + " not implemented "+ conv_into_type);
    }
  }

  public Tuple exec(Tuple input_tup) {
    try {
      for (Object obj: input_tup) {
        if (obj == null) { return null; }
      }
      Long[]  qk_zl  = converter_from.intoQmortonZl(input_tup, proj_1280);
      Integer zl     = ( qk_zl[1] == null ? null : qk_zl[1].intValue() );
      Tuple   result = converter_into.fromQmortonZl(qk_zl[0], zl, proj_1280);
      // GeometryUtils.dump("Converting from %s into %s: %s -> %d %d -> %s", conv_from_str, conv_into_str, input_tup, qk_zl[0], zl, result);
      return result;
    }
    catch (Exception err) {
      String msg = String.format("Can't convert from %s into %s (%s): %s",
        conv_from_str, conv_into_str, err, input_tup);
      log.error(msg);
      throw new RuntimeException(msg, err);
    }
  }

  public static interface QtConvertsFrom {
    public Long[] intoQmortonZl(Tuple tup, Projection proj) throws ExecException;
  }

  public static class FromQmortonZl implements QtConvertsFrom {
    public Long[] intoQmortonZl(Tuple tup, Projection proj) throws ExecException {
      Integer zoomlvl = (Integer)tup.get(1);
      return new Long[] { (Long)tup.get(0), new Long(zoomlvl) };
    }
  }
  
  public static class FromQuadstr implements QtConvertsFrom {
    public Long[] intoQmortonZl(Tuple tup, Projection proj) throws ExecException {
      String quadstr = (String)tup.get(0);
      return new Long[] { QuadtileUtils.quadstrToQmorton(quadstr), new Long(QuadtileUtils.quadstrToZl(quadstr)) };
    }
  }
  
  public static class FromQuadord implements QtConvertsFrom {
    public Long[] intoQmortonZl(Tuple tup, Projection proj) throws ExecException {
      Long quadord = (Long)tup.get(0);
      return new Long[] { QuadtileUtils.quadordToQmorton(quadord), new Long(QuadtileUtils.quadordToZl(quadord)) };
    }
  }
  
  public static class FromTileIJZl implements QtConvertsFrom {
    public Long[] intoQmortonZl(Tuple tup, Projection proj) throws ExecException {
      Integer tile_i  = (Integer)tup.get(0);
      Integer tile_j  = (Integer)tup.get(1);
      Integer zoomlvl = (Integer)tup.get(2);
      return new Long[] { new Long(QuadtileUtils.tileIJToQmorton(tile_i, tile_j)), new Long(zoomlvl) };
    }
  }

  public static class FromGridXY implements QtConvertsFrom {
    public Long[] intoQmortonZl(Tuple tup, Projection proj) throws ExecException {
      Double  grid_x  = (Double)tup.get(0);
      Double  grid_y  = (Double)tup.get(1);
      Integer zoomlvl = (Integer)tup.get(2);
      int[]   tile_ij = QuadtileUtils.gridXYToTileIJ(grid_x, grid_y, zoomlvl);
      return new Long[] { new Long(QuadtileUtils.tileIJToQmorton(tile_ij[0], tile_ij[1])), new Long(zoomlvl) };
    }
  }
  
  public static class FromLngLat implements QtConvertsFrom {
    public Long[] intoQmortonZl(Tuple tup, Projection proj) throws ExecException {
      Double  lng  = (Double)tup.get(0);
      Double  lat  = (Double)tup.get(1);
      Integer zoomlvl = (Integer)tup.get(2);
      return new Long[] {
        QuadtileUtils.worldToQmorton(lng, lat, zoomlvl, proj), new Long(zoomlvl) };
    }
  }
  
  public static class FromWsen implements QtConvertsFrom {
    public Long[] intoQmortonZl(Tuple tup, Projection proj) throws ExecException {
      Double  west    = (Double)tup.get(0);
      Double  south   = (Double)tup.get(1);
      Double  east    = (Double)tup.get(2);
      Double  north   = (Double)tup.get(3);
      long[]  qk_zl   = QuadtileUtils.wsenToQmortonZl(west, south, east, north, proj);
      return new Long[] { new Long(qk_zl[0]), new Long(qk_zl[1]) };
    }
  }

  /**
   *
   * Converters Into a format from qm/zl
   *
   */

  public static interface QtConvertsInto {
    public Tuple fromQmortonZl(Long qmorton, Integer zl, Projection proj);
  }
    
  public static class IntoQmortonZl implements QtConvertsInto {
    public Tuple fromQmortonZl(Long qmorton, Integer zl, Projection proj) {
      Tuple result_tup = TupleFactory.getInstance().newTuple();
      result_tup.append(qmorton);
      result_tup.append(zl);
      return result_tup;
    }
  }

  public static class IntoQuadstr implements QtConvertsInto {
    public Tuple fromQmortonZl(Long qmorton, Integer zl, Projection proj) {
      Tuple result_tup = TupleFactory.getInstance().newTuple();
      String quadstr   = QuadtileUtils.qmortonToQuadstr(qmorton, zl);
      result_tup.append(quadstr);
      return result_tup;
    }
  }  
    
  public static class IntoQuadord implements QtConvertsInto {
    public Tuple fromQmortonZl(Long qmorton, Integer zl, Projection proj) {
      Tuple    result_tup = TupleFactory.getInstance().newTuple();
      Long     quadord    = QuadtileUtils.qmortonToQuadord(qmorton, zl);
      result_tup.append(new Long(quadord));
      return result_tup;
    }
  }

  public static class IntoTileIJZl implements QtConvertsInto {
    public Tuple fromQmortonZl(Long qmorton, Integer zl, Projection proj) {
      Tuple    result_tup = TupleFactory.getInstance().newTuple();
      int[]    tile_ij    = QuadtileUtils.qmortonToTileIJ(qmorton, zl);
      result_tup.append(new Integer(tile_ij[0]));
      result_tup.append(new Integer(tile_ij[1]));
      result_tup.append(new Integer(zl));
      return result_tup;
    }
  }

    
  public static class IntoGridXY implements QtConvertsInto {
    public Tuple fromQmortonZl(Long qmorton, Integer zl, Projection proj) {
      Tuple    result_tup = TupleFactory.getInstance().newTuple();
      int[]    tile_ij    = QuadtileUtils.qmortonToTileIJ(qmorton, zl);
      double[] grid_xy    = QuadtileUtils.tileIJToGridXY(tile_ij[0], tile_ij[1], zl);
      result_tup.append(new Double(grid_xy[0]));
      result_tup.append(new Double(grid_xy[1]));
      return result_tup;
    }
  }

    
  public static class IntoLngLat implements QtConvertsInto {
    public Tuple fromQmortonZl(Long qmorton, Integer zl, Projection proj) {
      Tuple    result_tup = TupleFactory.getInstance().newTuple();
      double[] lng_lat    = QuadtileUtils.qmortonToWorld(qmorton, zl, proj);
      result_tup.append(new Double(lng_lat[0]));
      result_tup.append(new Double(lng_lat[1]));
      return result_tup;
    }
  }

  public static class IntoWsen implements QtConvertsInto {
    public Tuple fromQmortonZl(Long qmorton, Integer zl, Projection proj) {
      Tuple    result_tup = TupleFactory.getInstance().newTuple();
      double[] wsen       = QuadtileUtils.qmortonToWorldWSEN(qmorton, zl, proj);
      result_tup.append(new Double(wsen[0]));
      result_tup.append(new Double(wsen[1]));
      result_tup.append(new Double(wsen[2]));
      result_tup.append(new Double(wsen[3]));
      return result_tup;
    }
  }
  
  /**
   *
   * Output Schema Logic
   *
   */
  
  @Override
  public Schema outputSchema(Schema input)
  {
    Schema tuple_schema = new Schema();
    String tuple_name   = opName();
    
    try {
      switch (conv_into_type){
      case QMORTON_ZL:
        tuple_schema.add(new Schema.FieldSchema("qmorton", DataType.LONG));
        tuple_schema.add(new Schema.FieldSchema("zoomlvl", DataType.INTEGER));
        break;
      case QUADORD:
        tuple_schema.add(new Schema.FieldSchema("quadord", DataType.LONG));
        break;
      case TILE_IJ_ZL:
        tuple_schema.add(new Schema.FieldSchema("tile_i",  DataType.INTEGER));
        tuple_schema.add(new Schema.FieldSchema("tile_j",  DataType.INTEGER));
        tuple_schema.add(new Schema.FieldSchema("zoomlvl", DataType.INTEGER));
        break;
      case GRID_XY:
        tuple_schema.add(new Schema.FieldSchema("grid_xy", DataType.DOUBLE));
        tuple_schema.add(new Schema.FieldSchema("grid_xy", DataType.DOUBLE));
        break;
      case LNG_LAT:
        tuple_schema.add(new Schema.FieldSchema("lng", DataType.DOUBLE));
        tuple_schema.add(new Schema.FieldSchema("lat", DataType.DOUBLE));
        break;
      case WSEN:
        tuple_schema.add(new Schema.FieldSchema("west",  DataType.DOUBLE));
        tuple_schema.add(new Schema.FieldSchema("south", DataType.DOUBLE));
        tuple_schema.add(new Schema.FieldSchema("east",  DataType.DOUBLE));
        tuple_schema.add(new Schema.FieldSchema("north", DataType.DOUBLE));
        break;
      case QUADSTR:
        tuple_schema.add(new Schema.FieldSchema("quadstr",  DataType.CHARARRAY));
        break;
      default:
        throw new RuntimeException("Conversion from "+ conv_from_str + " not implemented");
      }
      //
      return new Schema(new Schema.FieldSchema(tuple_name, tuple_schema, DataType.TUPLE));
      //
    } catch (FrontendException err) { throw new RuntimeException(err); }
  }
  protected String opName() { return this.op_name; }
  
}
