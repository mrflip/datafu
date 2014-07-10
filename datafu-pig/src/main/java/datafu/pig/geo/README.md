## Geospatial Pig -- WORK IN PROGRESS

This is totally a work in progress.

In fact I don't really know which repo to put it into.

* Datafu?
* https://github.com/Esri/spatial-framework-for-hadoop ?
* Pig?

Anyway datafu builds really fast and has a nice test rig. So that's where I'm hacking it out.

### Things that you should know

* Only scattered examples from the below work. Enough to prove it out.

* Shapes are serialized using Well-Known Text. i.e. verbose strings that lose metadata and are expensive to assemble. Does that sound inefficient? It surely is. Testing sure is easier though

* This might only work against my fork of the Esri geometry API: https://github.com/Esri/geometry-api-java/pulls

* I couldn't get the output_schema of GeoScalarFunc to figure out what its template type was. So you can have any scalar function you'd like as long as it returns a double.

### Credits

* https://github.com/Esri/spatial-framework-for-hadoop -- we take a different implementation approach but much of the functionality has been garnered from the Esri collection of Hive UDFs.

### Families of UDFS

Bold == works; TODO == soon; strikethrough == later

#### Supporting Spatial join

* Spatial join on map side:
  - (**works**) decompose shape into quadtiles that cover it. Takes the collection of tiles covering the shape at the specified coarse zoom level of detail, and recursively decomposes them into smaller quadtiles until either the quadtile is completely contained in the shape or the finest zoom level is reached.
  - (**TODO**) make UDF
  - cogroup on quadord at coarsest zoom level (so everything within those largest-area tiles will land on the same reducer)

* (**TODO**) generic spatial join on reduce side:
  - Takes two bags of shapes on the same quadtile;
  - indexes first into a quadtree
  - passes second one through quadtree
  - all candidate pairs are emitted

* (**TODO**) alternative spatial join
  - secondary sort on full quadord, so that all say ZL-6 tiles in set A and the corresponding tiles at ZL-6, ZL-7, ... tiles in set B can be paired off
  - this works when the elements of set A do not overlap with each other

* (**TODO**) Spatial join and filter:
  - either spatial group
  - take the output and immediately apply an operator, eg intersects, before keeping the object around in a bag.

#### Quadtile Machinery

* **QuadtileContaining** (_works_) (geom)
* **QuadDecomp** (_works_)       --  constant zl
* **QuadMultiDecomp** (_works_)  --  in zl range (stop if a quad is fully contained)
* **Quadord,** (_works_) Quadstr
* **TileIJ** (_works_)
* **TileXY** (_works_)
* **TODO** QuadtileDecompose

#### Projections

* **Identity** (_works_)
* **Mercator** (_works_)
* **Equirectangular** (_works_)
* **TODO** ~~ProjectionFactory~~

#### `GeoCombineFunc`s  -- (gA, gB) -> geom

* **Union** (_works, udf_)
* **GeoDifference** (_works, udf_)
* **GeoXor** (_works, udf_)
* **GeoIntersection** (_works, udf_)
* ~~GeoClip~~
* ~~GeoCut~~
* ~~Proximity~~ (getNearestCoordinate, getNearestVertex, getNearestVertices)

#### `GeoProcessFunc`s -- geom -> geom

* **GeoBoundary** (_works, udf_)
* **GeoBuffer** (_works, udf_)
* **GeoCentroid** (_works, udf_)
* **GeoConvexHull** (_works, udf_)
* **GeoEndpoint** (_works, udf_)
* **GeoEnvelope** (_works, udf_)
* ~~GeoDensify~~
* ~~GeoExteriorRing~~
* ~~GeoGeneralize~~
* ~~GeoNthPoint~~
* ~~GeoOffset~~
* ~~GeoSimplify~~
* ~~GeoStartpoint~~
* ~~GeoProject~~
* ~~GeoTransform~~

#### `GeoScalarFunc`s -- geom -> number

* **GeoArea** (_works, udf_)
* **GeoDimensionality** (_works, udf_)
* **GeoNumCoordinates** (_works, udf_)
* **MaxX** (_works, udf_) / MaxY / MaxZ / MaxM
* **MinX** (_works, udf_) / MinY / MinZ / MinM
* ~~GeoCoordX~~ / GeoCoordY / GeoCoordZ / GeoCoordM
* ~~GeoNumGeometries~~
* ~~GeoNumInteriorRing~~
* ~~GeoNumPoints~~
* ~~GeoSimpleLength~~
* ~~GeodesicDistanceOnWGS84~~
* ~~GeodeticLength~~

#### `GeoCompareFunc`s -- (gA, gB) -> boolean

* **TODO** ~~GeoAIntersectsB~~
* ~~GeoAContainsB~~
* ~~GeoACrossesB~~
* ~~GeoADisjointB~~
* ~~GeoAEqualsB~~
* ~~GeoAOverlapsB~~
* ~~GeoATouchesB~~
* ~~GeoAWithinB~~
* ~~AlwaysTrue~~,
* ~~AlwaysFalse~~,
* ~~GeoABRelatingMatrix~~  returns tuple with the DE-9IM matrix
* ~~GeoAllRelating~~/~~GeoAnyRelating~~/~~GeoNoneRelating~~: With bag, All? / Any? / None? have relation
* ~~GeoDistance~~
* ~~GeoIsEnvIntersects~~

#### `GeoIsPropFunc`s -- geom -> boolean

* **GeoIsSimple** (_works, udf_)
* ~~GeoIsClosed~~
* ~~GeoIsEmpty~~
* ~~GeoIsRing~~
* ~~GeoFilter~~ --With bag, filter for it

#### Conversion

* **FromGeoJson** (_works, udf_)
* **FromWellKnownText** (_works, udf_)
* **ToGeoJson** (_works, udf_)
* **ToWellKnownText** (_works, udf_)

#### Other

* **GeoPoint** (_works, udf_) -- get point from coords
* ~~GeoBBox~~ (min_x,~~ min_y, max_x, max_y) -- get envelope object from coords
* ~~SetSpatialRefId~~
* ~~GetSpatialRefID~~
* ~~GeoRelatingMatrix~~ (a tuple of the DE-9IM matrix)
* ~~GeoFlattenMultigeom~~ -- turns a Multi-whatever into a bag of whatevers

#### `GeoCombineBagFunc`s -- (geom, {bagGeoms}) -> geom

* ~~GeoIntersection~~
* ~~GeoDifference~~
* ~~GeoUnion~~

#### `GeoProcessBagFunc`s --  {bag of geoms} -> {bag of processed geoms}

* ~~GeoConvexHullBag~~
* ~~GeoBufferBag~~
* ~~GeoOffsetBag~~
* ~~GeoBoundaryBag~~
* ~~GeoProjectBag~~
* ~~GeoTransformBag~~

### Seeing all this stuff

* **doohickey to render shapes onto a d3 map** (_works_)
* **TODO** make doohickey and code above work nicely together

## Interfaces


* A spatial partitioning and ordering mechanism should be first class and pig should be built around it.
  - that _doesn't_ mean you're tied to using the simple quadtile scheme. Anything that traces an order through space that we can use to partition nearby objects works.
  - (partition prefix, zl (height), secondary sort, 

### Geometry Type

* each has
  - a geometry object: OGCGeometry? Geometry? If I understand right, we want OGCGeometry as that would in principle allow alternate backends.
  - ?an envelope, or a lazy-eval'ed envelope?
  - a quadord key

* means to serialize
  - quadord, envelope (for sorting)
  - magic word wtih bits that say it's a geometry, the version number, and the subtype (point..multiline, tile, envelope)
  - the geometry object, efficiently serialized. Unless WKB is widly inefficient either to pack/unpack or in space consumed, it seems like a reasonable choice. Look at how Hive UDFs did it. 

* sub types:
  - generic `geometry`; `point`, `multipoint`, `polygon`, `multipolygon`, `line`, `multiline`
  - envelope and tile: could be tuple (xmin, ymin, xmax, ymax), (long, zl), or could both be maps. But we want some sugar to make them easy to schematize. Maybe point also gets special treatment.
  - feature -- tuple of `(geometry, ...properties...)`
  - geometry collection -- bag{g:(geometry)}; feature collection is bag{f:(geometry, ...properties...)}
  - I think the common case is that the SpatialReference is uniform across a type
    - so it should be specified in the schema and _not_ accommodated in the type
    - if that's not OK, you must ride the wkid along in its own field.
    

* `apache.pig.data.DataType`: `extractTypeFromClass`;
  - `compare` types (sort between string and map?)

* sorting:
  - by quadord (sort by northwest corner, parents preceding descendants
  - 


## (ignore)

Here's the signatures of all the esri geometry-api operators


```
combine A with B

    Geometry          execute(Geometry          inputGeometry,     Geometry                  intersector, SpatialReference sr, progtr);
    Geometry          execute(Geometry          inputGeometry,     Geometry                  subtractor, SpatialReference sr, progtr);
    Geometry          execute(Geometry          inputGeom1,        Geometry                  geom2, SpatialReference sr, progtr);
    Geometry          execute(Geometry          leftGeometry,      Geometry                  rightGeometry, SpatialReference sr, progtr);
    Geometry          execute(Geometry          inputGeometry,     Envelope2D                envelope, SpatialReference spatialRef, progtr);
    GeometryCursor    execute(boolean           bConsiderTouch,
                              Geometry          cuttee,            Polyline                  cutter, SpatialReference spatialReference, progtr);

combine A with many:

    GeometryCursor    execute(GeometryCursor    inputGeometries,   GeometryCursor            intersector, SpatialReference sr, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeometries,   GeometryCursor            rightGeometry, SpatialReference sr, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeometries,   GeometryCursor            subtractor, SpatialReference sr, progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             Envelope2D                envelope, SpatialReference spatialRef, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        GeometryCursor            intersector, SpatialReference sr, progtr, int dimensionMask);

Process shape:

    Geometry          execute(Geometry          inputGeometry,     SpatialReference          sr, boolean bForceSimplify, progtr);
    Geometry          execute(Geometry          inputGeometry,     SpatialReference          sr, boolean bForceSimplify, progtr);
    Geometry          execute(Geometry          inputGeometry,     progtr);
    Geometry          execute(Geometry          inputGeometry,     progtr);
    Geometry          execute(Geometry          inputGeometry,     SpatialReference          sr, double distance, JoinType joins, double bevelRatio, double flattenError,
    Geometry          execute(Geometry          inputGeometry,     SpatialReference          sr, double distance, progtr);
    Geometry          execute(Geometry          inputGeometry,     ProjectionTransformation  projection, progtr);
    Geometry          execute(Geometry          inputGeometry,     double                    maxDeviation, boolean bRemoveDegenerateParts, progtr);
    Geometry          execute(Geometry          inputGeometry,     double                    maxLength, progtr);

Process many:

    GeometryCursor    execute(GeometryCursor    geoms,             SpatialReference          sr, boolean bForceSimplify, progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             SpatialReference          sr, boolean bForceSimplify, progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             boolean                   b_merge, progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             double                    maxDeviation, boolean bRemoveDegenerateParts, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        SpatialReference          sr, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        SpatialReference          sr, double distance, JoinType joins, double bevelRatio, double flattenError,
    GeometryCursor    execute(GeometryCursor    inputGeoms,        SpatialReference          sr, double[] distances, boolean bUnion, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        double                    maxLength, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        ProjectionTransformation  projection, progtr);

Scalar from shape

    double            execute(Geometry          inputGeometry,     SpatialReference          sr, int geodeticCurveType, progtr);
    double            execute(Geometry          inputGeometry,     SpatialReference          sr, int geodeticCurveType, progtr);

Scalar from A and B

    double            execute(Geometry          geom1,             Geometry                  inputGeom2, progtr);
    boolean           execute(Geometry          inputGeom1,        Geometry                  inputGeom2, SpatialReference sr, progtr);
    boolean           execute(Geometry          inputGeom1,        Geometry                  inputGeom2, SpatialReference sr, String de_9im_string, progtr);

Scalar from many shapes:

    double[]          execute(GeometryCursor    geoms,             SpatialReference          sr, int geodeticCurveType, progtr);
    double[]          execute(GeometryCursor    geoms,             SpatialReference          sr, int geodeticCurveType, progtr);

import / export

    MapGeometry       execute(int               import_flags,      Geometry.Type             type, String geoJsonString, progtr
    GeometryCursor    execute(int               importFlags,       Geometry.Type             type, ByteBufferCursor shapeBuffers);
    Geometry          execute(int               importFlags,       Geometry.Type             type, ByteBuffer shapeBuffer);
    Geometry          execute(int               importFlags,       Geometry.Type             type, ByteBuffer wkbBuffer, progtr);
    Geometry          execute(int               import_flags,      Geometry.Type             type, String wkt_string, progtr);
    MapGeometryCursor execute(Geometry.Type     type,              JsonParserCursor          jsonParserCursor);

    int               execute(int               exportFlags,       Geometry                  geometry, ByteBuffer shapeBuffer);
    int               execute(int               exportFlags,       Geometry                  geometry, ByteBuffer wkbBuffer, progtr);
    ByteBuffer        execute(int               exportFlags,       Geometry                  geometry);
    ByteBuffer        execute(int               exportFlags,       Geometry                  geometry, progtr);
    ByteBufferCursor  execute(int               exportFlags,       GeometryCursor            geometryCursor);
    String            execute(int               exportFlags,       Geometry                  geometry, progtr);
    String            execute(int               exportFlags,       SpatialReference          spatialReference, Geometry geometry);
    String            execute(Geometry          geometry);
    String            execute(SpatialReference  spatialReference,  Geometry                  geometry);
    String            execute(SpatialReference  spatialReference,  Geometry                  geometry);
    String            execute(SpatialReference  spatialReference,  Geometry                  geometry, Map<String, Object> exportProperties);

    MapGeometry       execute(Geometry.Type     type,              JSONObject                jsonObject)
    MapGeometry       execute(Geometry.Type     type,              JsonParser                jsonParser);
    MapGeometry       execute(Geometry.Type     type,              String                    string)
    JsonCursor        execute(SpatialReference  spatialReference,  GeometryCursor            geometryCursor);
    JsonCursor        execute(SpatialReference  spatialReference,  GeometryCursor            geometryCursor);
```
