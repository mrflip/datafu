IMPORT '/Users/flip/ics/book/big_data_for_chimps/code/common_macros.pig'; %DEFAULT data_dir '/data/rawd'; %DEFAULT out_dir '../output';
REGISTER           '../../datafu-pig/build/libs/datafu-pig-1.2.1.jar';

DEFINE FromGeoJson      datafu.pig.geo.FromGeoJson();
DEFINE ToGeoJson        datafu.pig.geo.ToGeoJson();
DEFINE GeoQuadDecompose datafu.pig.geo.GeoQuadDecompose();
DEFINE ToQuadstr        datafu.pig.geo.QuadtileHandle('quadord', 'quadstr');
DEFINE ToQkZl           datafu.pig.geo.QuadtileHandle('quadord', 'qmorton_zl');
--
raw_feats = LOAD '../data/geo/atlas/world.json' as (geo_json:chararray);
feats     = FOREACH raw_feats GENERATE FromGeoJson(geo_json) AS feat;

decomp    = FOREACH feats {
  quad_geoms = GeoQuadDecompose(feat, 4, 8);
  GENERATE FLATTEN(quad_geoms);
};
decomp  = FOREACH decomp GENERATE ToGeoJson(fragment);
-- decomp    = FOREACH decomp {
--   quadstrs = FOREACH quad_geoms GENERATE FLATTEN(ToQuadstr(quadord));
--   GENERATE quadstrs, quad_geoms;
-- };
--
DESCRIBE decomp;
STORE_TABLE(decomp, 'world-quads-jsnl');
