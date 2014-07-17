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
  proj_name:  "orthographic"
  scale:      100
  translate:  [width/2, height/2]
  rotate:     [-10, 0]
  λ:          d3.scale.linear().domain([0, width ]).range([-180, 180]);
  φ:          d3.scale.linear().domain([0, height]).range([  90, -90]);
  shift_lng:  -22.5

  add_feat_layer: (name, dom, feats, cssclass)->
    return unless feats
    dom.selectAll("path").data(feats)
      .enter().append("path")
      .attr("class", name+" "+cssclass)
      .attr("d", @path)

  coord_ring: (min, max, pts) ->
    tot = max-min
    arr = ( ((tot*ii/pts)) + min for ii in [0..pts] )
    arr.concat([min])

  add_meridian: (lng) ->
    lng = lng + @shift_lng
    coords = ([lng, lat] for lat in @coord_ring(-89, 89, 16))
    @graticule_g.append("path").datum({type: "LineString", coordinates: coords}).attr("class", "tropic").attr("d", @path)

  add_tropic: (lat) ->
    console.log(lat)
    coords = ([lng, lat] for lng in @coord_ring(-180 + @shift_lng, 180 + @shift_lng, 32))
    @graticule_g.append("path").datum({type: "LineString", coordinates: coords}).attr("class", "tropic").attr("d", @path)

  add_g: (name)->
    this["#{name}_g"] = @layers_g.append("g").attr("id",  "#{name}_g")

  ready: (error, world_topo, regions, quads, tiles)=> #, world_lores, us_topo, us_land_topo, us_congress_topo, country_names, cities, us_counties, us_stats, ballparks)=>
    if error
      d3.select("#placeinfo").text(error.message + "\n" + error.stack)
      throw error
    world_land_f = topojson.feature world_topo, world_topo.objects.land
    world_bdry_f = topojson.mesh    world_topo, world_topo.objects.countries, ((a, b)-> a isnt b)
    @graticule_g.append("path").datum(d3.geo.graticule())
      .attr("class", "graticule").attr("d", @path)
    #
    # lim_lat = 66.51326044311188 # 85.0511287798066 # 02 66.51326044311188; 0202  55.7765730186677; 02002 61.60639637138628; -56 tierra del fuego 0     85.0511287798066
    lim_lat =  Math.asin(0.8) * 180 / Math.PI
    @add_tropic(lat)   for lat in @coord_ring( -lim_lat,  lim_lat, 8)
    @add_meridian(lng) for lng in @coord_ring(-180,   180,  16)
    #
    @add_feat_layer("background", @background_g, [{type: "Sphere"}], "")
    @add_feat_layer("world_land", @world_land_g, [world_land_f],        "land") # [world_land_f] or world_land_f.features
    @add_feat_layer("world_bdry", @world_bdry_g, [world_bdry_f],        "bdry")
    @add_feat_layer("quads",      @quads_g,      quads?.features,       "quad")
    @add_feat_layer("regions",    @regions_g,    regions?.features,     "regn")
    @add_feat_layer("tiles",      @tiles_g,      tiles?.features,       "tile")
    @path_objs = @layers_g.selectAll("path")
    controls.setup_interactions()

  zoomed: ()=>
    @translate = d3.event.translate
    @scale     = d3.event.scale
    @redraw()

  redraw: ()->
    if (@proj_name == "orthographic")
      @projection.rotate([@λ(@translate[0]), @φ(@translate[1])]).scale(@scale * @scale_factor)
    else
      @projection.translate(@translate).scale(@scale * @scale_factor)
    @path_objs?.attr("d", @path)
    # console.log( "redraw", @translate[0], @translate[1],
    #   @projection.translate()[0], @projection.translate()[1],
    #   @scale_factor, @scale, @projection.scale(), width, height )
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
      'background', 'tiles', 'graticule','world_land','world_bdry', 'quads', 'regions']
    #
    @path = d3.geo.path()
    @set_projection()
    #
    @zoom = d3.behavior.zoom()
      .translate(@translate).scale(@scale)
      .scaleExtent([height/6.0, 8 * height])
      .on("zoom", @zoomed)
    @layers_g.call(@zoom)

  set_projection: ()->
    @scale_factor = 1.0
    switch @proj_name
      when "equirectangular"
        @scale_factor = 1.2
        @projection   = d3.geo.equirectangular().precision(0.1)
      when "gall_peters"
        @scale_factor = Math.sqrt(2)
        @projection   = d3.geo.cylindricalEqualArea().parallel(45)
      when "eckert4"
        @scale_factor = 1.6
        @projection   = d3.geo.eckert4().precision(.1)
      when "orthographic"
        @scale_factor = 2.5
        @projection   = d3.geo.orthographic().clipAngle(90).precision(.1)
        @translate    = [width/2, height/2]
      else #"mercator"
        @scale_factor = 1.2
        @projection   = d3.geo.mercator().precision(0)
    @projection.rotate([-@shift_lng, 0])
    @path.projection(@projection)
    @redraw()

class LocationControls
  toggle_layer: ()->
    console.log(this)
    name    = this.value
    display = if this.checked then "inline" else "none"
    d3.select("##{name}_g").attr("display", display)

  toggle_projection: ()->
      layers.proj_name = this.value;
      layers.set_projection();

  setup_interactions: ()->
    d3.selectAll("input.layer_ctl").on("change", @toggle_layer).each(@toggle_layer)
    d3.select("#projection_ctl").on("change", @toggle_projection).each(@toggle_projection)
    d3.select("#reset_ctl").on("click", layers.reset)
    ptr_lnglat = d3.select('#ptr_lnglat')
    layers.layers_g.on "mousemove", (tile,idx)->
      lnglat = layers.projection.invert(d3.mouse(this))
      ptr_lnglat.text([Math.round(lnglat[0]*10)/10.0, Math.round(lnglat[1]*10)/10.0])

class Datasets
  fetch: ->
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
