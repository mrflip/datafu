//
// Assemble the geographic projection
//

var land_feat, boundary_feat, places,
    projection,
    voronoi,
    path,
    svg,
    bg_g,
    tiles_g,
    places_g;


d3.select(window).on("resize", throttle);

var zoom      = d3.behavior.zoom()
    .scaleExtent([1, 14])
    .on("zoom", move)
     ;

var p = 80
    ;

var width = document.getElementById('map').offsetWidth;
var height = width / 2;


var path      = d3.geo.path().projection(projection);
var graticule = d3.geo.graticule();
var tooltip   = d3.select("#map").append("div").attr("class", "tooltip hidden");
var infobox   = d3.select("#placeinfo");

function qkey(tile){ return tile.key.join(", "); }

setup(width,height);

// Start fetching all the data
queue()
// geojson
  .defer(d3.tsv,  "../data/geo/atlas/topojson_atlas/world-110m.json")
  .defer(d3.tsv,  "../data/geo/atlas/topojson_atlas/us.json")
  .defer(d3.tsv,  "../data/geo/atlas/topojson_atlas/us-land.json")
  .defer(d3.tsv,  "../data/geo/atlas/topojson_atlas/us-congress-113.json")
// data
  .defer(d3.tsv,  "../data/geo/atlas/topojson_atlas/world-country-names.tsv")
  .defer(d3.tsv,  "../data/geo/census/world_cities_grid.tsv")
  .defer(d3.json, "../data/geo/atlas/topojson_atlas/us-county-names.tsv")
  .defer(d3.tsv,  "../data/geo/atlas/topojson_atlas/us-state-names.tsv")
  .defer(d3.tsv,  "./ballparks.tsv")
  .await(ready);

function ready(error,
               atlas_topo, us_topo, us_land_topo, us_congress_topo,
               country_names, cities, us_counties, us_stats, ballparks) {
  land_feat     = topojson.feature(atlas_topo, atlas_topo.objects.land);
  boundary_feat = topojson.mesh(atlas_topo,    atlas_topo.objects.countries, function(a, b) { return a !== b; });
  places        = cities.sort(function(pta,ptb) {
    return (+ptb.lat > +pta.lat ? 1 : -1 );
  })
  places.forEach(function(pts) {
    pts.lng = pts[0] = +pts.lng;
    pts.lat = pts[1] = +pts.lat;
  });
  dump_cells(places);
                 
  draw(land_feat, boundary_feat, places);
}

// ===========================================================================
//
// Basic Structure
//
function setup(width,height){
  projection = d3.geo.mercator()
    .translate([(width/2), (height/2)])
    .scale(0.27 * width  / Math.PI)
    ;

  // projection = d3.geo.equirectangular()
  //     .scale(250)
  //     .translate([width / 2, height / 2])
  //     .precision(.1);

  path = d3.geo.path().projection(projection);

  // Voronoi calculator
  voronoi = d3.geom.voronoi()
        .x(function(pts) { return pts.x; })
        .y(function(pts) { return pts.y; })
        .clipExtent([[0.45*height, 0], [1.55*height, height]]);
  
  // Attach the map element
  svg = d3.select("#map").append("svg")
      .attr("width", width)
      .attr("height", height)
      .call(zoom)
//      .on("click", click)
      .append("g");

  bg_g = svg.append("g")
      .attr("class",  "bg_g");

  places_g = svg.append("g")
      .attr("class", "places");

  tiles_g = svg.append("g")
      .attr("class",  "tiles");

}

function redraw() {
  width = document.getElementById('map').offsetWidth;
  height = width / 2;
  d3.select('svg').remove();
  setup(width,height);
  draw(land_feat, boundary_feat, places);
}

function move() {
  var tr = d3.event.translate;
  var sc = d3.event.scale;
  var ht = height/4;

  // put bounds on the translation
  tr[0] = Math.min(
    (width/height)  * (sc - 1),
    Math.max( width * (1 - sc), tr[0] )
  );
  tr[1] = Math.min(
    ht * (sc - 1) + ht * sc,
    Math.max(height  * (1 - sc) - ht * sc, tr[1])
  );

  // console.log(z, projection.scale(), sc, tr);

  zoom.translate(tr);

  bg_g.attr(    "transform", "translate(" + tr + ")scale(" + sc + ")");
  places_g.attr("transform", "translate(" + tr + ")scale(" + sc + ")");
  tiles_g.attr( "transform", "translate(" + tr + ")scale(" + sc + ")");
  places_g.selectAll("circle").attr("r", 5 / sc);

  //adjust the country hover stroke width based on zoom level
  d3.selectAll(".land-borders").style("stroke-width", 1.5 / sc);
  d3.selectAll(".place-cell"  ).style("stroke-width", 2.0 / sc);
  d3.selectAll(".tile"        ).style("stroke-width", 3.0 / sc);
  d3.selectAll(".equator"     ).style("stroke-width", 2.0 / sc);

  draw_tiles(sc);

}

function dump_cells(places) {
  var cell_ring, cell_geom, pt_geom;
  var cells_dump = "", pts_dump = "";

  var voronoi_geo = d3.geom.voronoi()
      .x(function(pts) { return pts.lng; })
      .y(function(pts) { return pts.lat; })
      .clipExtent([[-180, -85.1], [180, 85.1]])
  ;
  
  voronoi_geo(places).forEach(function(cell) {
    
    var pt = cell.point;
    // var props  = {
    //   park_id: pt.park_id, park_name: pt.park_name,
    //   beg_date: pt.beg_date, end_date: pt.end_date,
    //   is_active: pt.is_active, n_games: pt.n_games,
    //   lng: pt.lng, lat: pt.lat,
    //   city: pt.city, state_id: pt.state_id, country_id: pt.country_id
    // };

    // Close the polygon
    cell_ring = cell.slice();
    cell_ring.push(cell[0]);

    cell_geom = {id: pt.city + "_cell", type: "Polygon", coordinates: [cell_ring]};
    cells_dump += JSON.stringify(cell_geom) + "\n";
    pt_geom   = {id: pt.city,           type: "Point",   coordinates: [pt.lng, pt.lat] };
    pts_dump   += JSON.stringify(pt_geom)  + "\n";

  });

  infobox.text("cells:\n"  + cells_dump + "\n\n" + "points:\n" + pts_dump);
}

function draw(land_feat, boundary_feat, places) {

  //
  // Calculate voronoi cells of places
  //

  places.forEach(function(pts) {
    var position = projection(pts);
    pts.x = position[0];
    pts.y = position[1];
  });

  voronoi(places)
      .forEach(function(cell) { cell.point.cell = cell; });
  
  //
  // Draw the Atlas Land background and the borders

  // Viewport device
  bg_g.append("path")
    .datum({type: "Sphere"})
    .attr("class", "outline")
    .attr("d", path);

  // // Grid lines
  // bg_g.append("path")
  //   .datum(graticule)
  //   .attr("class", "graticule")
  //   .attr("d", path);

  bg_g.append("path")
      .datum(land_feat)
      .attr("class", "land")
      .attr("d", path)
  ;
  bg_g.append("path")
      .datum(boundary_feat)
      .attr("class", "land-borders")
      .attr("d", path)
  ;

  bg_g.append("path")
   .datum({type: "LineString", coordinates: [[-180, 0], [-90, 0], [0, 0], [90, 0], [180, 0]]})
   .attr("class", "equator")
   .attr("d", path);

  var place = places_g
    .selectAll("g")
       .data(places)
    .enter().append("g")
      .attr("class", "place");


  var circles = place.append("circle")
      .attr("transform", function(pt) { return "translate(" + pt.x + "," + pt.y + ")"; })
      // .attr("r",         function(pt) { return 3 + Math.log(pt.n_games); })
      .attr("r", 5)
      ;

  place.append("path")
      .attr("class", "place-cell")
    .attr("d", function(pts) { if (! pts.cell) { console.log(pts); }; return (pts.cell && pts.cell.length) ? "M" + pts.cell.join("L") + "Z" : null; })
      .on("click.cell", click_handler)
      .on("mouseover.cell", function(pt,idx){ d3.select(this).style("fill", "#ecc");  })
      .on("mouseout.cell",  function(pt,idx){ d3.select(this).style("fill", "");  })
    .append("title").text(function(pt){ return pt.city + " " + pt.country_id; })
  ;
}

function draw_tiles(sc) {
  var z = (Math.log(sc) / Math.LN2 | 0) + 4,
      tiles = d3.quadTiles(projection, z);
  var tile = tiles_g.selectAll(".tile")
        .data(tiles, qkey);

  tile.enter().append("path")
      .attr("class", "tile outline")
      .on("click.cell", click_handler)
      .attr("d", path)
    .append("title").text(qkey)
  ;
  tile.exit().remove();

  // //offsets for tooltips
  // var offsetL = 40; // document.getElementById('map').offsetLeft+20;
  // var offsetT = 10; // document.getElementById('map').offsetTop+10;

  // .on("mousemove", function(tile,idx) {
  //   var mouse = d3.mouse(svg.node()).map( function(dd) { return parseInt(dd); } );
  //   var me = d3.select(this);
  //   console.log(mouse, this, me, this.offsetLeft);
  //   tooltip.classed("hidden", false)
  //     .attr("style", "left:"+(mouse[0]+offsetL)+"px;top:"+(mouse[1]+offsetT)+"px")
  //     .html(qkey(tile));
  // })
  // .on("mouseout",  function(d,i) {
  //   tooltip.classed("hidden", true);
  // })

  // var text = tiles_g.selectAll("text").data(tiles, qkey);
  // text.enter().append("text")
  //     .attr("text-anchor", "middle")
  //     .text(qkey);
  // text.exit().remove();
  // text.attr("transform", function(pt) {
  //   pt_c = projection(pt.centroid)
  //   // return "translate(" + (pt_c[0]+tr[0]) + "," + (pt_c[1] + tr[1]) + ")";
  //   return "translate(" + pt_c + ")";
  // });
  //  text.style("font-size", (10/sc)+"px");
}

function click_handler(pt,idx) {
  var latlon = projection.invert(d3.mouse(this));
  infobox.text(
    "click: " + latlon + "\n\nobject:\n" +
      JSON.stringify(pt)
  );
  console.log(idx, latlon, pt);
}

var throttleTimer;
function throttle() {
  window.clearTimeout(throttleTimer);
    throttleTimer = window.setTimeout(function() {
      redraw();
    }, 200);
}
