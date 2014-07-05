
### GeoJoin

* spatial join:
  - Takes two bags;
  - indexes first into a quadtree
  - passes second one through quadtree
  - all candidate pairs are emitted

* Spatial join and filter:
  - spatial group
  - taking the output and applying an operator

### GeoProcess -- geom -> geom

* GeoBuffer	     (geom)
* GeoOffset	     (geom)
* GeoConvexHull	     (geom)
* GeoBoundary	     (geom)
* GeoSimplify	     (geom)
* GeoDensify	     (geom)
* GeoGeneralize      (envelope becomes polygon)
* GeoExteriorRing
* GeoEnvelope

* GeoCentroid
* GeoEndpoint
* GeoStartpoint
* GeoNthPoint

* QuadtileContaining (geom)
* QuadDecomp       --  constant zl
* QuadMultiDecomp  --  in zl range (stop if a quad is fully contained)
* Quadkey, Quadstr
* TileIJ
* TileXY

### GeoProcessBag --  {bagGeoms} -> geom

* GeoConvexHull	    (bag)
* GeoBuffer	    (bag)
* GeoOffset	    (bag)
* GeoBoundary	    (bag)

### GeoAction  -- (gA, gB) -> geom

* Union		    (gA,gB)
* GeoDifference	    (gA,gB)
* GeoXor	    (gA,gB)
* GeoIntersection   (gA,gB)

* GeoClip	    (geom, env)
* GeoCut	    (geom, polyline)

### GeoAction -- (geom, {bagGeoms}) -> geom

* GeoIntersection   (geom, {bag}))
* GeoDifference	    (geom, {bag})
* GeoUnion	    (geom, {bag})

### Other

* GeoPoint	    (xx,yy)
* `GeoBBox	    (min_x, min_y, max_x, max_y)` -- envelope object from coords
* SetSpatialRefId
* GetSpatialRefID
* GeoRelatingMatrix (a tuple of the DE-9IM matrix)
* IsGeoRelating  (takes a DE-9IM relation)

* GeoFlattenMultigeom -- turns a Multi-whatever into a bag of whatevers

### GeoCompare -- (gA, gB) -> boolean

* IsGeoContains
* IsGeoCrosses
* IsGeoDisjoint
* IsGeoEquals
* IsGeoOverlaps
* IsGeoTouches
* IsGeoWithin

* With bag, All? / None? / Any?
* With bag, filter for it

### GeoTest -- geom -> boolean

* IsGeoSimple
* IsGeoClosed
* IsGeoEmpty
* IsGeoRing
* IsGeoEnvIntersects

### geom -> number

* GeoDistance
* GeodesicDistanceOnWGS84
* GeoArea
* GeodeticLength
* GeoDimensionality
* GeoSimpleLength
* GeoNumGeometries
* GeoNumInteriorRing
* GeoNumPoints

* GeoCoordX
* GeoCoordY
* GeoCoordZ
* GeoCoordM

* MaxX / MaxY / MaxZ / MaxM
* MinX / MinY / MinZ / MinM


### Conversion

* ToGeoJson
* ToWellKnownText



### Not yet:

* rasterize
* GeoProject
* GeoTransform
* proximity (getNearestCoordinate, getNearestVertex, getNearestVertices)



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
