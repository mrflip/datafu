width = document.getElementById('map').offsetWidth
height = width / 2

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

class Layers
  path:       null
  svg:        null
  layers_g:   null
  projection: null
  proj_name:  "mercator"
  scale:      100
  translate:  [width/2, height/2]
  rotate:     [-10, 0]
  λ:          d3.scale.linear().domain([0, width ]).range([-180, 180]);
  φ:          d3.scale.linear().domain([0, height]).range([  90, -90]);

  add_feat_layer: (name, dom, feats, cssclass)->
    return unless feats
    dom.selectAll("path").data(feats)
      .enter().append("path")
      .attr("class", name+" "+cssclass)
      .attr("d", @path)

  ready: (error, world_topo, quads, regions)=> #, world_lores, us_topo, us_land_topo, us_congress_topo, country_names, cities, us_counties, us_stats, ballparks)=>
    if error
      d3.select("#placeinfo").text(error.message + "\n" + error.stack)
      throw error

    world_land_f = topojson.feature world_topo, world_topo.objects.land
    world_bdry_f = topojson.mesh    world_topo, world_topo.objects.countries, ((a, b)-> a isnt b)

    @graticule_g.append("path").datum(d3.geo.graticule())
      .attr("class", "graticule").attr("d", @path)
    @add_feat_layer("background", @background_g, [{type: "Sphere"}], "")
    @add_feat_layer("world_land", @world_land_g, [world_land_f],        "land") # [world_land_f] or world_land_f.features
    @add_feat_layer("world_bdry", @world_bdry_g, [world_bdry_f],        "bdry")
    @add_feat_layer("quads",      @quads_g,      quads?.features,       "tile")
    @add_feat_layer("regions",    @regions_g,    regions?.features,     "regn")
    @path_objs = @layers_g.selectAll("path")

  add_g: (name)->
    this["#{name}_g"] = @layers_g.append("g").attr("id",  "#{name}_g")

  zoomed: ()=>
    @translate = d3.event.translate
    @scale     = d3.event.scale
    # console.log @translate[0], @translate[1], @scale_factor, @scale, @rotate, width, height
    @redraw()

  whirled: ()=>
    @translate = d3.event.translate
    @path_objs.attr("d", @path)

  redraw: ()->
    # console.log( "redraw", @translate[0], @translate[1],
    #   @projection.translate()[0], @projection.translate()[1],
    #   @scale_factor, @scale, @projection.scale(), width, height )
    if (@proj_name == "orthographic")
      @projection.rotate([@λ(@translate[0]), @φ(@translate[1])])
    else
      @projection.translate(@translate).scale(@scale * @scale_factor)
    @path_objs?.attr("d", @path)
    # formatOrigin(projection.rotate()));
    d3.select("#lng_ctl"  ).attr("value", Math.round(@projection.translate()[0]))
    d3.select("#lat_ctl"  ).attr("value", Math.round(@projection.translate()[1]))
    d3.select("#scale_ctl").attr("value", Math.round(@projection.scale()))

  reset: ()=>
    @scale = 100
    @translate = [width/2, height/2]
    @zoom.scale(@scale).translate(@translate)
    @set_projection()

  setup: (width, height)->
    # Attach the map element and layers
    @svg = d3.select("#map").append("svg").attr("height", height)
    @layers_g = @svg.append("g")
    @add_g g_layer for g_layer in [
      'background', 'graticule','world_land','world_bdry','quads','regions']
    #
    @path = d3.geo.path()
    @set_projection()
    #
    @zoom = d3.behavior.zoom()
      .translate(@projection.translate())
      .scale(@projection.scale())
      .scaleExtent([height/4.0, 8 * height])
      .on("zoom", @whirled)
    @layers_g.call(@zoom)
    controls.setup_interactions()

  set_projection: ()->
    console.log @proj_name
    @scale_factor = 1.0
    switch @proj_name
      when "equirectangular"
        @scale_factor = 1.2
        @projection   = d3.geo.equirectangular()
      when "gall_peters"
        @scale_factor = Math.sqrt(2)
        @projection   = d3.geo.cylindricalEqualArea().parallel(45).center([0,7])
      when "eckert4"
        @scale_factor = 1.6
        @projection   = d3.geo.eckert4().precision(.1)
      when "orthographic"
        @scale_factor = 3.0
        @projection   = d3.geo.orthographic().clipAngle(90).precision(.1)
      else #"mercator"
        @scale_factor = 1.2
        @projection   = d3.geo.mercator().precision(0).center([0,15])
    #
    # @projection.translate([width/2, height/2]).scale(150*@scale_factor)
    @path.projection(@projection)
    # @path_objs?.attr("d", @path)
    @redraw()

class LocationControls

  setup_interactions: ()->
    d3.selectAll("input.layer_ctl").on "change", ()->
      name    = this.value
      display = if this.checked then "inline" else "none"
      d3.select("##{name}_g").attr("display", display)
    d3.select("#projection_ctl").on "change", ()->
      layers.proj_name = this.value;
      layers.set_projection();
    d3.select("#reset_ctl").on "click", layers.reset

class Projector
  hi: 7

class Datasets
  fetch: ->
    # Start fetching all the data
    queue()
      .defer(d3.json,    "/data/geo/atlas/topojson_atlas/world-110m.json")
      .defer(@geo_jsnl,  "/geography/output/world-quads-jsnl/part-m-00000")
      .defer(@geo_jsnl,  "/data/geo/atlas/world.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/world-50m.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us-land.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us-congress-113.json")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/world-country-names.tsv")
      # .defer(d3.tsv,   "/data/geo/census/world_cities_grid.tsv")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/us-county-names.tsv")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/us-state-names.tsv")
      # .defer(d3.tsv,   "/voronoi/ballparks.tsv")
      .await(layers.ready)

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

window.layers   = layers   = new Layers
window.controls = controls = new LocationControls
window.datasets = datasets = new Datasets

layers.setup(width, height)
datasets.fetch()
