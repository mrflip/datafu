window.map4c ?= {}

# ~~~~~~~~~~~~~~~~~~~~
# Map Drawer
# 
class window.Map
  constructor: (width, height)->
    @svg      = d3.select("#map>svg")
    @layers_g = @svg.append("g")
    @path = d3.geo.path()

  ready: (datasets)=>
    @add_layers(datasets)
    @path_objs = @layers_g.selectAll("path")
    @redraw()

  add_layers: (datasets)->
    console.log datasets
    #    
    new SphereLayer    this, "background"
    # new GraticuleLayer this, "graticule"
    # new GriddleLayer   this, "griddle"
    # @world_land = new FeatureLayer(this, "world_land")
    # @world_bdry = new FeatureLayer(this, "world_bdry")
    # #
    # world_topo = datasets.world_topo
    # @world_land.add_feature("land", [topojson.feature(world_topo, world_topo.objects.land)])
    # @world_bdry.add_feature("bdry", [topojson.mesh(   world_topo, world_topo.objects.countries, ((a, b)-> a isnt b))])
    
  redraw: ()->
    @path_objs?.attr("d", @path)
  
  set_projection: (@projection)->
    @path.projection(@projection)
    @redraw()

# ~~~~~~~~~~~~~~~~~~~~
# Generic Layer with toggle controls and other sweetness
# 
class window.MapLayer
  display:   "inline"

  constructor: (@owner, @name)->
    @layer_dom = owner.layers_g.append("g").attr("id", "#{name}_g")
    #
    @toggler   = d3.select("##{@name}_togg").on("change", @toggle_layer)
    @toggle_layer()

  draw: ()->
    @layer_dom.selectAll("path").attr("d", @owner.path)

  toggle_layer: ()=>
    return if @toggler.empty()
    @display = if @toggler.property("checked") then "inline" else "none"
    @layer_dom.attr("display", @display)
      
  add_shape: (cssclass, datum)->
    @layer_dom.append("path")
      .datum(datum)
      .attr("class", @name+" "+cssclass)

  add_feature: (cssclass, data)->
    feat_dom = @layer_dom.selectAll("path")
      .data(data)
      .enter().append("path")
      .attr("class", @name+" "+cssclass)
    @feat_dom
    

# ~~~~~~~~~~~~~~~~~~~~
# Grid lines
# 
class window.GraticuleLayer extends MapLayer
  constructor: (@owner, @name)->
    super(@owner, @name)
    @add_shape("graticule", d3.geo.graticule())

# ~~~~~~~~~~~~~~~~~~~~
# Geo Features
# 
class window.FeatureLayer extends MapLayer
  constructor: (@owner, @name, @cssclass, @data)->
    super(@owner, @name)
    @add_feature(@cssclass, @data) if @data

class window.SphereLayer extends MapLayer
  constructor: (@owner, @name, cssclass="background")->
    super(@owner, @name)
    @add_feature cssclass, [{type: "Sphere"}]
    
# ~~~~~~~~~~~~~~~~~~~~
# Meridians and Tropics drawn at specified spacing
# 
class GriddleLayer extends MapLayer
  constructor: (@owner, @name, @cssclass)->
    super(@owner, @name, @cssclass)
    @add_lines()

  add_lines: () ->
    @add_tropic(lat)   for lat in @coord_ring( -90 + 1e-9, 89 - 1e-9,   8)
    @add_meridian(lng) for lng in @coord_ring( -180,       180,         9)
  
  coord_ring: (min, max, pts) ->
    tot = max-min
    arr = ( ((tot*ii/pts)) + min for ii in [0..pts] )
  
  add_meridian: (lng) ->
    coords = ([lng, lat] for lat in @coord_ring(-90, 90, 5))
    @add_shape("meridian", {type: "LineString", coordinates: coords})
  
  add_tropic: (lat) ->
    coords = ([lng, lat] for lng in @coord_ring(-180, 180, 8))
    coords = coords.concat([coords[0]])
    @add_shape("tropic", {type: "LineString", coordinates: coords})
