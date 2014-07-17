π    = Math.PI
window.rads = π / 180
window.degs = 180 / π
window.halfπ = π / 2
window.sqrtπ = Math.sqrt(π)
width =  800 # document.getElementById('map').offsetWidth
height = 800
asqrt  = (x)-> if x > 0 then Math.sqrt(x) else 0
asin   = (x)->
  if       x > 1  then halfπ
  else if  x < -1 then -halfπ
  else                 Math.asin(x)
f      = d3.format "7.2f"

class MapLayer
  display:   "inline"

  constructor: (@owner, @name, @cssclass)->
    @layer_dom = owner.layers_g.append("g").attr("id", "#{name}_g")
    #
    @toggler   = d3.select("##{@name}_togg").on("change", @toggle_layer)
    @toggle_layer()

  draw: ()->
    @layer_dom.selectAll("path").attr("d", @owner.path)

  toggle_layer: ()=>
    return unless @toggler
    @display = if @toggler.property("checked") then "inline" else "none"
    @layer_dom.attr("display", @display)

class ShapeLayer extends MapLayer
  add_shape: (datum, _cssclass="")->
    @layer_dom.append("path")
      .datum(datum)
      .attr("class", @name+" "+_cssclass)

class GraticuleLayer extends ShapeLayer
  constructor: (@owner, @name, @cssclass)->
    super(@owner, @name, @cssclass)
    @add_shape(d3.geo.graticule())
    @draw()

class FeatureLayer extends MapLayer
  constructor: (@owner, @name, @cssclass, @data)->
    super(@owner, @name, @cssclass)
    @add_shape(@data)
    @draw()
    
  add_shape: ()->
    @layer_dom.selectAll("path")
      .data(@data)
      .enter().append("path")
      .attr("class", @name+" "+@cssclass)

class Map
  constructor: (width, height)->
    # Attach the map element and layers
    @svg      = d3.select("#map>svg")
    @layers_g = @svg.append("g")
    @add_g g_layer for g_layer in ['background'] # , 'graticule','world_land','world_bdry']
    #
    @path = d3.geo.path()

  ready: (error, world_topo)=>
    if error
      d3.select("#infobox").text(error.message + "\n" + error.stack)
      throw error
    @graticule  = new GraticuleLayer this, "graticule",  ""
    @world_land = new FeatureLayer   this, "world_land", "land", [topojson.feature(world_topo, world_topo.objects.land)]
    @world_land = new FeatureLayer   this, "world_land", "land", [topojson.mesh(world_topo, world_topo.objects.countries, ((a, b)-> a isnt b))]
    # topojson.feature(world_topo, world_topo.objects.countries).features
    #
    # @add_tropic(lat)   for lat in @coord_ring( -90,    90,   8)
    @add_meridian(lng) for lng in @coord_ring( -180,   180, 9)
    @graticule.draw()
    @path_objs = @layers_g.selectAll("path")

  add_g: (name)->
    this["#{name}_g"] = @layers_g.append("g").attr("id",  "#{name}_g")
  
  coord_ring: (min, max, pts) ->
    tot = max-min
    arr = ( ((tot*ii/pts)) + min for ii in [0..pts] )
  
  add_meridian: (lng) ->
    console.log lng
    coords = ([lng, lat] for lat in @coord_ring(-90, 90, 5))
    @graticule.add_shape({type: "LineString", coordinates: coords}, "tropic")
  
  add_tropic: (lat) ->
    coords = ([lng, lat] for lng in @coord_ring(-180, 180, 8))
    coords = coords.concat([coords[0]])
    @graticule_g.append("path").datum({type: "LineString", coordinates: coords}).attr("class", "tropic").attr("d", @path)
  
  redraw: ()->
    @path_objs?.attr("d", @path)
  
  set_projection: (@projection)->
    @path.projection(@projection)
    @redraw()

class CollSqControls extends ControlSet
  update: (ctl)->
    size = height
    proj = new CollSq(@nlobes, @cutpar, @lampar).projection(size)
      .scale(size/2)
      .translate([size / 2, size/2])
      .rotate([@cenmer,0])
      .clipExtent([[0,0], [size, size]])
      # .precision(0)
    map.set_projection(proj)

class CollSq
  # given a value in λ: -π..π, φ: -π/2..π/2, map to the square [ -1 -1, 1 -1, 1 1, -1 1 ]
  forward:  (λ, φ) =>
    if (λ == π) then λ -= 1e-12; 
    φ = -φ
    λ_4 = ((4 * (λ + π)) % (2*π)) - π
    [gx, gy] = @raw_fwd(λ_4, φ)
    if      (λ ==  π )   then [gx, gy] = @raw_fwd(π, φ)
    else if (λ >=  π/2)  then [gx, gy] = [-gy,  gx]
    else if (λ >=   0 )  then [gx, gy] = [-gx, -gy]
    else if (λ >= -π/2)  then [gx, gy] = [ gy, -gx]
    else                      [gx, gy] = [ gx,  gy]
    [gx, gy]

  # map from the square [ -1 -1, 1 -1, 1 1, -1 1 ] to λ: -π..π, φ: -π/2..π/2
  # with -180 lat going to the northwest-pointing diagonal, -90 going to the northeast-pointing, and so forth.
  invert: (gx, gy) =>
    # rotate into the domain of the raw transfer (triangle [ 0 0, -1 1, 1 1 ])
    if      ((gy >  gx) && (gy >= -gx)) then dλ = -0.75; rot_x =  gx; rot_y =  gy # top center
    else if ((gy <= gx) && (gy >= -gx)) then dλ = -0.25; rot_x = -gy; rot_y =  gx # right
    else if ((gy <= gx) && (gy <  -gx)) then dλ =  0.25; rot_x = -gx; rot_y = -gy # bottom
    else                                     dλ =  0.75; rot_x =  gy; rot_y = -gx # left
    [λ, φ] = raw_invert(rot_x, rot_y)
    # scale down to -45..45 and then shift by -135, -45, 45 or 135 to cover the appt ranges
    λ = λ/4 + dλ*π;
    [λ, φ]

  # given a value in λ: -π..π, φ: -π/2..π/2, map to the triangle [ 0 0, -1 1, 1 1 ]
  raw_fwd: (λ, φ) =>
    gy = asqrt( (1 - Math.sin(φ))/2 )
    [ gy * (λ / π), gy ]

  # given a value in x: -1..1 and y: 0..1 return the latitude λ: -π..π  φ: -π/2..π/2
  raw_inv: (gx, gy) =>
    λ   = if gy == 0 then 0 else (π * gx / gy)
    φ   = asin( 1 - (2 * gy * gy) )
    [λ, φ]

  projection: (size)->
    d3.geo.projection(@forward).center([0,-90])

# class CoolHat
#   constructor: (@nlobes, @cutpar, @lampar) ->
#     @cutrad = @cutpar * rads
#     @hat    = new CollSq
#     @shoes  =
#
#   forward:

window.map      = new Map(width, height)
window.controls = new CollSqControls(['nlobes', 'cutpar', 'lampar', 'cenmer'])
window.bob = new CollSq

queue()
  .defer(d3.json,    "/data/geo/atlas/topojson_atlas/world-110m.json")
  .await(map.ready)
