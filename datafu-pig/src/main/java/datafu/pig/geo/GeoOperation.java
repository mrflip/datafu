
	// /**
	//  * Constructs the set-theoretic intersection between an array of geometries
	//  * and another geometry.
	//  * 
	//  * @param inputGeometries
	//  *            An array of geometry objects.
	//  * @param geometry
	//  *            The geometry object.
	//  * @return Any array of geometry objects showing the intersection.
	//  */
	// static Geometry[] intersect(Geometry[] inputGeometries, Geometry geometry,
	// 		SpatialReference spatialReference) {
	// 	OperatorIntersection op = (OperatorIntersection) factory
	// 			.getOperator(Operator.Type.Intersection);
	// 	SimpleGeometryCursor inputGeometriesCursor = new SimpleGeometryCursor(
	// 			inputGeometries);
	// 	SimpleGeometryCursor intersectorCursor = new SimpleGeometryCursor(
	// 			geometry);
	// 	GeometryCursor result = op.execute(inputGeometriesCursor,
	// 			intersectorCursor, spatialReference, null);
    // 
	// 	ArrayList<Geometry> resultGeoms = new ArrayList<Geometry>();
	// 	Geometry g;
	// 	while ((g = result.next()) != null) {
	// 		resultGeoms.add(g);
	// 	}
    // 
	// 	Geometry[] resultarr = resultGeoms.toArray(new Geometry[0]);
	// 	return resultarr;
	// }
