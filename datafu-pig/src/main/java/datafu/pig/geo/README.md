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




### Families of UDFs

### Supporting Spatial join

* Spatial join on map side:
  - (_works_) decompose shape into quadtiles that cover it. Takes the collection of tiles covering the shape at the specified coarse zoom level of detail, and recursively decomposes them into smaller quadtiles until either the quadtile is completely contained in the shape or the finest zoom level is reached.
  - (**TODO**) make UDF
  - cogroup on quadkey at coarsest zoom level (so everything within those largest-area tiles will land on the same reducer)

* (**TODO**) generic spatial join on reduce side: 
  - Takes two bags of shapes on the same quadtile;
  - indexes first into a quadtree
  - passes second one through quadtree
  - all candidate pairs are emitted

* (**TODO**) alternative spatial join
  - secondary sort on full quadkey, so that all say ZL-6 tiles in set A and the corresponding tiles at ZL-6, ZL-7, ... tiles in set B can be paired off
  - this works when the elements of set A do not overlap with each other

* (**TODO**) Spatial join and filter:
  - either spatial group
  - take the output and immediately apply an operator, eg intersects, before keeping the object around in a bag.

*

### Quadtile

* _works_ QuadtileContaining (geom)
* _works_ QuadDecomp       --  constant zl
* _works_ QuadMultiDecomp  --  in zl range (stop if a quad is fully contained)
* _works_ Quadkey, Quadstr
* _works_ TileIJ
* _works_ TileXY

### GeoProcess -- geom -> geom


* GeoDensify	     (geom)
* GeoExteriorRing
* GeoGeneralize      (envelope becomes polygon)
* GeoNthPoint
* GeoOffset	     (geom)
* GeoSimplify	     (geom)
* GeoStartpoint
* _udf_ GeoBoundary	     (geom)
* _udf_ GeoBuffer	     (geom)
* _udf_ GeoCentroid
* _udf_ GeoConvexHull	     (geom)
* _udf_ GeoEndpoint
* _udf_ GeoEnvelope

### GeoProcessBag --  {bagGeoms} -> geom

* GeoConvexHull	    (bag)
* GeoBuffer	    (bag)
* GeoOffset	    (bag)
* GeoBoundary	    (bag)

### GeoCombine  -- (gA, gB) -> geom

* _udf_ Union	
* _udf_ GeoDifference
* _udf_ GeoXor	   
* _udf_ GeoIntersection
* GeoClip
* GeoCut
* Proximity (getNearestCoordinate, getNearestVertex, getNearestVertices)

### GeoCombineBag -- (geom, {bagGeoms}) -> geom

* GeoIntersection   (geom, {bag}))
* GeoDifference	    (geom, {bag})
* GeoUnion	    (geom, {bag})

### GeoCompare -- (gA, gB) -> boolean

* IsGeoContains
* IsGeoCrosses
* IsGeoDisjoint
* IsGeoEquals
* IsGeoOverlaps
* IsGeoTouches
* IsGeoWithin
* IsGeoRelating  (takes a DE-9IM relation)
* With bag: All? / None? / Any? have relation
* With bag, filter for it
* GeoDistance

* GeoProject
* GeoTransform

### GeoIsSomething -- geom -> boolean

* _udf_ IsGeoSimple
* IsGeoClosed
* IsGeoEmpty
* IsGeoRing
* IsGeoEnvIntersects

### GeoScalar -- geom -> number

* GeoCoordX / GeoCoordY / GeoCoordZ / GeoCoordM
* GeoNumGeometries
* GeoNumInteriorRing
* GeoNumPoints
* GeoSimpleLength
* _udf_ GeoArea
* _udf_ GeoDimensionality
* _udf_ GeoNumCoordinates
* _udf_ MaxX / MaxY / MaxZ / MaxM
* _udf_ MinX / MinY / MinZ / MinM
* GeodesicDistanceOnWGS84
* GeodeticLength

### Other

* _udf_ GeoPoint	    (xx,yy)
* `GeoBBox	    (min_x, min_y, max_x, max_y)` -- envelope object from coords
* SetSpatialRefId
* GetSpatialRefID
* GeoRelatingMatrix (a tuple of the DE-9IM matrix)
* GeoFlattenMultigeom -- turns a Multi-whatever into a bag of whatevers

### Conversion

* _udf_ ToGeoJson
* _udf_ ToWellKnownText


```
    GeometryCursor    execute(GeometryCursor    inputGeometries,   GeometryCursor            intersector, SpatialReference sr, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeometries,   GeometryCursor            rightGeometry, SpatialReference sr, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeometries,   GeometryCursor            subtractor, SpatialReference sr, progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             Envelope2D                envelope, SpatialReference spatialRef, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        GeometryCursor            intersector, SpatialReference sr, progtr, int dimensionMask);

    GeometryCursor    execute(GeometryCursor    geoms,             SpatialReference          sr, boolean bForceSimplify, progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             SpatialReference          sr, boolean bForceSimplify, progtr);

    Geometry          execute(Geometry          inputGeometry,     Geometry                  intersector, SpatialReference sr, progtr);
    Geometry          execute(Geometry          inputGeometry,     Geometry                  subtractor, SpatialReference sr, progtr);
    Geometry          execute(Geometry          inputGeom1,        Geometry                  geom2, SpatialReference sr, progtr);
    Geometry          execute(Geometry          leftGeometry,      Geometry                  rightGeometry, SpatialReference sr, progtr);
    Geometry          execute(Geometry          inputGeometry,     Envelope2D                envelope, SpatialReference spatialRef, progtr);

    double            execute(Geometry          inputGeometry,     SpatialReference          sr, int geodeticCurveType, progtr);
    double            execute(Geometry          inputGeometry,     SpatialReference          sr, int geodeticCurveType, progtr);
    double[]          execute(GeometryCursor    geoms,             SpatialReference          sr, int geodeticCurveType, progtr);
    double[]          execute(GeometryCursor    geoms,             SpatialReference          sr, int geodeticCurveType, progtr);


    Geometry          execute(Geometry          inputGeometry,     SpatialReference          sr, boolean bForceSimplify, progtr);
    Geometry          execute(Geometry          inputGeometry,     SpatialReference          sr, boolean bForceSimplify, progtr);
    Geometry          execute(Geometry          inputGeometry,     progtr);
    Geometry          execute(Geometry          inputGeometry,     progtr);

    Geometry          execute(Geometry          inputGeometry,     SpatialReference          sr, double distance, JoinType joins, double bevelRatio, double flattenError,
    Geometry          execute(Geometry          inputGeometry,     SpatialReference          sr, double distance, progtr);
    Geometry          execute(Geometry          geometry,          ProjectionTransformation  projection, progtr);

    Geometry          execute(Geometry          inputGeometry,     double                    maxDeviation, boolean bRemoveDegenerateParts, progtr);
    Geometry          execute(Geometry          inputGeometry,     double                    maxLength, progtr);

    ByteBuffer        execute(int               exportFlags,       Geometry                  geometry);
    ByteBuffer        execute(int               exportFlags,       Geometry                  geometry, progtr);
    ByteBufferCursor  execute(int               exportFlags,       GeometryCursor            geometryCursor);

    Geometry          execute(int               importFlags,       Geometry.Type             type, ByteBuffer shapeBuffer);
    Geometry          execute(int               importFlags,       Geometry.Type             type, ByteBuffer wkbBuffer, progtr);
    Geometry          execute(int               import_flags,      Geometry.Type             type, String wkt_string, progtr);

    GeometryCursor    execute(GeometryCursor    geoms,             progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             boolean                   b_merge, progtr);
    GeometryCursor    execute(GeometryCursor    geoms,             double                    maxDeviation, boolean bRemoveDegenerateParts, progtr);

    GeometryCursor    execute(GeometryCursor    inputGeoms,        SpatialReference          sr, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        SpatialReference          sr, double distance, JoinType joins, double bevelRatio, double flattenError,
    GeometryCursor    execute(GeometryCursor    inputGeoms,        SpatialReference          sr, double[] distances, boolean bUnion, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        double                    maxLength, progtr);
    GeometryCursor    execute(GeometryCursor    inputGeoms,        ProjectionTransformation  projection, progtr);

    GeometryCursor    execute(boolean           bConsiderTouch,    Geometry                  cuttee, Polyline cutter, SpatialReference spatialReference, progtr);
    GeometryCursor    execute(int               importFlags,       Geometry.Type             type, ByteBufferCursor shapeBuffers);
    MapGeometry       execute(int               import_flags,      Geometry.Type             type, String geoJsonString, progtr
    MapGeometryCursor execute(Geometry.Type     type,              JsonParserCursor          jsonParserCursor);
    String            execute(Geometry          geometry);
    String            execute(SpatialReference  spatialReference,  Geometry                  geometry);
    String            execute(SpatialReference  spatialReference,  Geometry                  geometry);
    String            execute(SpatialReference  spatialReference,  Geometry                  geometry, Map<String, Object> exportProperties);
    String            execute(int               exportFlags,       Geometry                  geometry, progtr);
    String            execute(int               exportFlags,       SpatialReference          spatialReference, Geometry geometry);
    boolean           execute(Geometry          inputGeom1,        Geometry                  inputGeom2, SpatialReference sr, progtr);
    boolean           execute(Geometry          inputGeom1,        Geometry                  inputGeom2, SpatialReference sr, String de_9im_string, progtr);
    double            execute(Geometry          geom1,             Geometry                  inputGeom2, progtr);
    int               execute(int               exportFlags,       Geometry                  geometry, ByteBuffer shapeBuffer);
    int               execute(int               exportFlags,       Geometry                  geometry, ByteBuffer wkbBuffer, progtr);

    MapGeometry       execute(Geometry.Type     type,              JSONObject                jsonObject)
    MapGeometry       execute(Geometry.Type     type,              JsonParser                jsonParser);
    MapGeometry       execute(Geometry.Type     type,              String                    string)
    JsonCursor        execute(SpatialReference  spatialReference,  GeometryCursor            geometryCursor);
    JsonCursor        execute(SpatialReference  spatialReference,  GeometryCursor            geometryCursor);
```
