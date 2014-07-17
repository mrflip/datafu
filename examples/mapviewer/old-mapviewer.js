var width = document.getElementById('map').offsetWidth;
var height = width / 2;

var svg, bg_g, places_g, tiles_g, path, graticule;
var projection;
var world_hires, world_lores;
var format = d3.format(".1f");

setup(width, height);

// Start fetching all the data
queue()
  .defer(d3.json,  "/data/geo/atlas/topojson_atlas/world-110m.json")
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
  svg = d3.select("#map").append("svg")
   .attr("height", height) // .attr("width",  width)
    ;
  layers = svg.append("g");
  
  bg_g         = layers.append("g").attr("class",  "bg_g");
  layers.append("path")
    .datum({type: "Sphere"})
    .attr("class", "background")
    .attr("d", path);
  graticule_g  = layers.append("g").attr("class",  "graticule_g" ).attr("id",  "graticule_g");
  // layers.append("path")
  //   .datum({type: "Sphere"})
  //   .attr("class", "outline")
  //   .attr("d", path);

  world_land_g = layers.append("g").attr("class",  "world_land_g").attr("id",  "world_land_g");
  world_bdry_g = layers.append("g").attr("class",  "world_bdry_g").attr("id",  "world_bdry_g");
  places_g     = layers.append("g").attr("class", "places");
  tiles_g      = layers.append("g").attr("class",  "tiles");
  //
  path         = d3.geo.path();
  set_projection("orthographic");
  graticule    = d3.geo.graticule();
  //
  var zoom = d3.behavior.zoom()
    .translate(projection.translate())
    .scale(projection.scale())
    .scaleExtent([height, 8 * height])
    .on("zoom", zoomed)
      // .projection(projection)
    // .on("zoomstart", function() {
    //   world.datum(world110m);
    // })
    // .on("zoomend", function() {
    //   world.datum(world50m).attr("d", path);
    // })
  ;
  
  layers.call(zoom);
  //
  setup_interactions();
  //
 
}

function ready(error,
               _world_topo, _world_lores, us_topo, us_land_topo, us_congress_topo,
               country_names, cities, us_counties, us_stats, ballparks) {
  world_topo = _world_topo;
  world_lores = _world_lores;
  var world_land_f = topojson.feature(world_topo, world_topo.objects.countries);
  var world_bdry_f = topojson.mesh(world_topo, world_topo.objects.countries, function(a, b) { return a !== b; });
  //
  var world_land_f_lores = topojson.feature(world_lores, world_lores.objects.countries);
  var world_bdry_f_lores = topojson.mesh(   world_lores, world_lores.objects.countries, function(a, b) { return a !== b; });
  //
  add_topo_layer("graticule",  graticule_g,  graticule,    "");
  // add_feat_layer("world_land", world_land_g, world_land_f.features, "land");
  add_topo_layer("world_land", world_land_g, world_land_f, "land");
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
  if (proj_name == "orthographic" || proj_name == "") { spherical_interactions("on"); } else { spherical_interactions("off"); }
  if        (proj_name == "orthographic") {
    projection = d3.geo.orthographic().precision(.1).clipAngle(90).rotate([60,0]).center([0,10])
      .scale(300);
  } else if (proj_name == "gilbert") {
    projection = d3.geo.orthographic().precision(.1).clipAngle(90).rotate([-5, -5]).center([0,10]).scale(470);
    var gilbert = d3.geo.gilbert(projection);
  } else if (proj_name == "equirectangular") {
    projection = d3.geo.equirectangular().precision(.1).scale(180).center([-20, 20 ]);
  } else if (proj_name == "gall_peters") {
    projection = d3.geo.cylindricalEqualArea().precision(.1).scale(180).center([-20, 20 ]).parallel(45);
  } else if (proj_name == "eckert4") {
    projection = d3.geo.eckert4().precision(.1)
      .scale(260).center([-20, 20 ]);
  } else { //proj_name == "mercator"
    projection = d3.geo.mercator().scale(180).center([0, 20 ]).rotate([0,0]);
  }
  path.projection(projection);
  layers.selectAll("g path").attr("d", path);
}

function zoomed() {
  projection.translate(d3.event.translate).scale(d3.event.scale);
  layers.selectAll("g path").attr("d", path);
  d3.select("#location").text(formatOrigin(projection.rotate()));
  d3.select("#lng_ctl").text(d3.event.scale);
}

function setup_interactions() {
  d3.selectAll("input.layer_ctl").on("change", function(){
    var name    = this.value;
    var display = this.checked ? "inline" : "none";
    d3.select("#"+name+"_g").attr("display", display);
  });

  d3.select("#projection_ctl").on("change", function(){
    var proj_name = this.value;
    set_projection(proj_name);
  })
}

var λ = d3.scale.linear()
    .domain([0, width])
    .range([-180, 180]);

var φ = d3.scale.linear()
    .domain([0, height])
    .range([90, -90]);

function spherical_interactions(state) {
  if (state == "on") {
    svg.on("mousemove", function() {
      var p = d3.mouse(this);
      projection.rotate([λ(p[0]), φ(p[1])]);
      layers.selectAll("path").attr("d", path);
    });
  } else {
    svg.on("mousemove", function() {});
  }
}

function formatOrigin(o) {
  return format(Math.abs(o[1])) + "°" + (o[1] > 0 ? "S" : "N") + ", " + format(Math.abs(o[0])) + "°" + (o[0] > 0 ? "W" : "E");
}
