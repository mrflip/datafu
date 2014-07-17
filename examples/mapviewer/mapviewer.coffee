width  = 800 # document.getElementById('map').offsetWidth
height = 800 # width / 2

layers   = null
controls = null
datasets = null

# throttle_timer = null;
# function throttle() {
#   window.clearTimeout(throttleTimer);
#     throttleTimer = window.setTimeout(function() {
#       redraw();
#     }, 200);
# }

class ProjectionFooler extends Map
  proj_name:  "equirectangular"
  rotate:     [-10, 0]
  shift_lng:  -22.5
  scale_factor: 100

  constructor: (width, height)->
    super(width, height)
    @zr = new ZoomerControl(this, width, height)
  
  add_layers: (datasets)->
    super(datasets)
    # new FeatureLayer this, "tiles",   "tile", datasets.tiles.features
    # new FeatureLayer this, "quads",   "quad", datasets.quads.features
    # new FeatureLayer this, "regions", "regn", datasets.regions.features

  redraw: ()->
    if (@proj_name == "orthographic")
      @projection.rotate(@zr.rotate())
    else
      [tx, ty] = @zr.translate()
      @projection.translate([tx, ty])
    @projection.scale(@zr.scale() * @scale_factor)
    console.log( "redraw", @zr.translate(), @projection.translate()[0], @projection.translate()[1],
      @scale_factor, @zr.scale(), @projection.scale())
    @path_objs?.attr("d", @path)

  # g.transition()
  #     .duration(750)
  #     .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")scale(" + k + ")translate(" + -x + "," + -y + ")")
  #     .style("stroke-width", 1.5 / k + "px");

  reset: ()=>
    @zr.reset()
    @set_projection()

  set_projection: ()->
    switch @proj_name
      when "equirectangular"
        @scale_factor = 120
        @projection   = d3.geo.equirectangular().precision(0.1)
      when "gall_peters"
        @scale_factor = 100*Math.sqrt(2)
        @projection   = d3.geo.cylindricalEqualArea().parallel(45)
      when "eckert4"
        @scale_factor = 160
        @projection   = d3.geo.eckert4().precision(.1)
      when "orthographic"
        @scale_factor = 230
        @projection   = d3.geo.orthographic().clipAngle(90).precision(.1)
        @zr.reset()
      else #"mercator"
        @scale_factor = 120
        @projection   = d3.geo.mercator().precision(0)
    @projection.rotate([-@shift_lng, 0])
    @path.projection(@projection)
    @redraw()

class Datasets
  constructor: (@map) -> true
  
  fetch: ->
    console.log "fetching"
    # Start fetching all the data
    queue()
      .defer(d3.json,    "/data/geo/atlas/topojson_atlas/world-110m.json")
      .defer(@geo_jsnl,  "/data/geo/atlas/world.json")
      .defer(@geo_jsnl,  "/output/world-quads-4-6-jsnl/part-m-00000")
      .defer(@geo_jsnl,  "/output/full-quads-4-7-jsnl/part-m-00000")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/world-50m.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us-land.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us-congress-113.json")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/world-country-names.tsv")
      # .defer(d3.tsv,   "/data/geo/census/world_cities_grid.tsv")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/us-county-names.tsv")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/us-state-names.tsv")
      # .defer(d3.tsv,   "/voronoi/ballparks.tsv")
      .await(@ready)

  ready: (error, @world_topo, @regions, @quads, @tiles)=>
    if error then (d3.select("#infobox").text(error.message + "\n" + error.stack); throw error)
    @map.ready(this)    

  geo_jsnl: (url, callback)->
    d3.xhr url, "application/json", (request)->
      try
        text = request.responseText.replace(/\n$/, "")
        text = '{"type":"FeatureCollection","features":[{"type":"Feature","geometry":' +
          text.replace(/\n/gm, '},{"type":"Feature","geometry":') + "}]}"
        json = JSON.parse text
        callback(null, json)
      catch err
        callback(err)


  jsnl: (url, callback)->
    d3.xhr url, "application/json", (request)->
      try
        text = request.responseText.replace(/\n$/, "")
        text = "[" + text.replace(/\n/gm, ",") + "]"
        json = JSON.parse text
        callback(null, json)
      catch err
        callback(err)


# class ProjFiddleControls extends ControlSet
#   choose_projection: ()->
#     map.proj_name = this.value;
#     map.set_projection();
#     
#   update: (ctl)->
#     super(ctl)
#     # size = height
#     # proj = new CollSq(@nlobes, @cutpar, @lampar).projection(size)
#     #   .scale(size/2)
#     #   .translate([size / 2, size/2])
#     #   .rotate([@cenmer,0])
#     #   .clipExtent([[0,0], [size, size]])
#     #   # .precision(0)
#     # @map.set_projection(proj)
# 
#   constructor: (owner, ctl_names)->
#     super(owner, ctl_names)
#     d3.select("#projection_ctl").on("change", @choose_projection)
#     d3.select("#reset_ctl").on("click", @owner.reset)

window.map = map = new ProjectionFooler(width, height)
map.set_projection()
datasets         = new Datasets(map)
datasets.fetch()



#, world_lores, us_topo, us_land_topo, us_congress_topo, country_names, cities, us_counties, us_stats, ballparks)=>
# # lim_lat = 66.51326044311188 # 85.0511287798066 # 02 66.51326044311188; 0202  55.7765730186677; 02002 61.60639637138628; -56 tierra del fuego 0     85.0511287798066
