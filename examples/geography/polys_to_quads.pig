REGISTER           '../../datafu-pig/build/libs/datafu-pig-1.2.1.jar';

DEFINE FromGeoJson  datafu.pig.geo.FromGeoJson();
DEFINE QuadDecompose  datafu.pig.geo.QuadDecompose('6');

park_cells = LOAD 'park_cells.json' AS (geo_json:chararray);

parks = FOREACH park_cells GENERATE QuadDecompose(geo_json);

DUMP parks

