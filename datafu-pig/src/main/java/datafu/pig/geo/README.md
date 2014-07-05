
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
