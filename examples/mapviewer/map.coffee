map4c = (window.map4c ?= {})
ff = d3.format "7.2f"

# ~~~~~~~~~~~~~~~~~~~~
# Map Drawer
#
class map4c.Map
  constructor: (@width, @height)->
    @svg      = d3.select("#map>svg")
    @layers_g = @svg.append("g")
    @path     = d3.geo.path()
    @zr       = new map4c.ZoomerControl(this, @width, @height)
    #
    this_map   = this
    @layers_g.on "mousemove", ()->
      return unless this_map.projection.invert?
      ptr_lnglat = d3.select('#ptr_lnglat')
      lnglat     = this_map.projection.invert(d3.mouse(this)) # d3.mouse(this_map.layers_g.node())
      ptr_lnglat.text([ff(lnglat[0]), ff(lnglat[1])])

  ready: (datasets)=>
    @add_layers(datasets)
    @path_objs = @layers_g.selectAll("path")
    @redraw("loaded datasets")

  add_layers: (datasets)->
    #
    new map4c.GraticuleLayer this, "graticule"
    @world_land = new map4c.FeatureLayer(this, "world_land")
    @world_bdry = new map4c.FeatureLayer(this, "world_bdry")
    new map4c.GriddleLayer   this, "griddle"
    new map4c.SphereLayer    this, "background"
    #
    world_topo = datasets.world_topo
    @world_land.add_feature("land", [topojson.feature(world_topo, world_topo.objects.land)])
    @world_bdry.add_feature("bdry", [topojson.mesh(   world_topo, world_topo.objects.countries, ((a, b)-> a isnt b))])

  redraw: (force)->
    [tx, ty] = @zr.translate()
    sc       = @zr.scale()
    @layers_g
      .attr("transform",
        "translate(" + (@width/2) + "," + (@height/2) + ")" +
        "translate(" + tx + "," + ty + ")" +
        "scale(" + sc + ")" +"")
      .style("stroke-width", 1.5 / sc + "px");
    # console.log( "redraw", tx, ty, sc, @projection.translate()[0], @projection.translate()[1], @zr.scale(), @projection.scale())
    if force
      console.log "forcing redraw", force # , arguments.callee.caller.toString()
      @path_objs?.attr("d", @path)
      
  reset: ()=>
    @zr.reset()
    @redraw()

  set_projection: (@projection)->
    @path.projection(@projection)
    @redraw("set projection")

# ~~~~~~~~~~~~~~~~~~~~
# Generic Layer with toggle controls and other sweetness
#
class map4c.MapLayer
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
class map4c.GraticuleLayer extends map4c.MapLayer
  constructor: (@owner, @name)->
    super(@owner, @name)
    @add_shape("graticule", d3.geo.graticule())

# ~~~~~~~~~~~~~~~~~~~~
# Geo Features
#
class map4c.FeatureLayer extends map4c.MapLayer
  constructor: (@owner, @name, @cssclass, @data)->
    super(@owner, @name)
    @add_feature(@cssclass, @data) if @data

class map4c.SphereLayer extends map4c.MapLayer
  constructor: (@owner, @name, cssclass="background")->
    super(@owner, @name)
    @add_feature cssclass, [{type: "Sphere"}]

# ~~~~~~~~~~~~~~~~~~~~
# Meridians and Tropics drawn at specified spacing
#
class map4c.GriddleLayer extends map4c.MapLayer
  constructor: (@owner, @name, @cssclass)->
    super(@owner, @name, @cssclass)
    @add_lines()

  add_lines: () ->
    @add_tropic(lat)   for lat in @coord_ring( -90 + 1e-9, 89 - 1e-9,  16)
    @add_meridian(lng) for lng in @coord_ring( -180,       180,         8)

  coord_ring: (min, max, pts) ->
    tot = max-min
    arr = ( ((tot*ii/pts)) + min for ii in [0..pts] )

  add_meridian: (lng) ->
    coords = ([lng, lat] for lat in @coord_ring(-90, 90, 32))
    @add_shape("meridian", {type: "LineString", coordinates: coords})

  add_tropic: (lat) ->
    coords = ([lng, lat] for lng in @coord_ring(-180, 180, 16))
    coords = coords.concat([coords[0]])
    @add_shape("tropic", {type: "LineString", coordinates: coords})
