var width =  960,
    height = 600;
var svg, bg_g, places_g, tiles_g, path, graticule;
var projection;

setup(width, height);

// Start fetching all the data
queue()
  .defer(d3.json,  "/data/geo/atlas/topojson_atlas/world-50m.json")
  .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us.json")
  .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us-land.json")
  .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us-congress-113.json")
  .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/world-country-names.tsv")
  .defer(d3.tsv,   "/data/geo/census/world_cities_grid.tsv")
  .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/us-county-names.tsv")
  .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/us-state-names.tsv")
  .defer(d3.tsv,   "/voronoi/ballparks.tsv")
  .await(ready);

function setup(width, height) {
  // Attach the map element
  svg        = d3.select("#map").append("svg")
    .attr("width",  width)
    .attr("height", height);
  bg_g         = svg.append("g").attr("class",  "bg_g");
  graticule_g  = svg.append("g").attr("class",  "graticule_g" ).attr("id",  "graticule_g");
  world_land_g = svg.append("g").attr("class",  "world_land_g").attr("id",  "world_land_g");
  world_bdry_g = svg.append("g").attr("class",  "world_bdry_g").attr("id",  "world_bdry_g");
  places_g     = svg.append("g").attr("class", "places");
  tiles_g      = svg.append("g").attr("class",  "tiles");
  //
  path         = d3.geo.path();
  set_projection("eckert4");
  graticule    = d3.geo.graticule();
  //
  setup_interactions();
}

function ready(error,
               atlas_topo, us_topo, us_land_topo, us_congress_topo,
               country_names, cities, us_counties, us_stats, ballparks) {
  var world_land_f = topojson.feature(atlas_topo, atlas_topo.objects.countries).features;
  var world_bdry_f = topojson.mesh(atlas_topo, atlas_topo.objects.countries, function(a, b) { return a !== b; });
  //
  add_topo_layer("graticule",  graticule_g,  graticule,    "");
  add_feat_layer("world_land", world_land_g, world_land_f, "land");
  add_topo_layer("world_bdry", world_bdry_g, world_bdry_f, "bdry");
  //

}

function add_feat_layer(name, dom, feats, cssclass){
  return dom.selectAll("path").data(feats)
    .enter().append("path")
    .attr("class", name+" "+cssclass)
    .attr("d", path);
}

function add_topo_layer(name, dom, topo, cssclass){
  return dom.append("path").datum(topo)
    .attr("class", name+" "+cssclass)
    .attr("id",    name)
    .attr("d",     path);
}


function set_projection(proj_name) {
  if        (proj_name == "orthographic") {
    projection = d3.geo.orthographic().precision(.1).clipAngle(90).rotate([60,0]).center([0,10])
      .scale(300);
  } else if (proj_name == "equirectangular") {
    projection = d3.geo.equirectangular().precision(.1)
      .scale(180).center([0, 20 ]);
  } else if (proj_name == "eckert4") {
    projection = d3.geo.eckert4().precision(.1)
      .scale(180).center([0, 20 ]);
  } else { //proj_name == "mercator"
    projection = d3.geo.mercator().center([0, 30 ]).scale(180).rotate([0,0]);
  }
  path.projection(projection);
  svg.selectAll("g path").attr("d", path);
}

function setup_interactions() {
  d3.selectAll("input.layer_sel").on("change", function(){
    var name    = this.value;
    var display = this.checked ? "inline" : "none";
    d3.select("#"+name+"_g").attr("display", display);
  });

  d3.select("#projection_sel").on("change", function(){
    var proj_name = this.value;
    set_projection(proj_name);
  })
}
