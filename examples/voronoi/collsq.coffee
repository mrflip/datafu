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

class CollSq
  # given a value in λ: -π..π, φ: -π/2..π/2, map to the square [ -1 -1, 1 -1, 1 1, -1 1 ]
  forward:  (λ, φ) =>
    if (λ == π) then λ -= 1e-12;
    λ_4 = ((4 * (λ + π)) % (2*π)) - π
    [gx, gy] = @raw_fwd(λ_4, φ)
    if      (λ ==  π )   then [gx, gy] = @raw_fwd(π, φ); [gx, gy] = [gx, gy]
    else if (λ >=  π/2)  then [gx, gy] = [ gx,  gy]
    else if (λ >=   0 )  then [gx, gy] = [ gy, -gx]
    else if (λ >= -π/2)  then [gx, gy] = [-gx, -gy]
    else                      [gx, gy] = [-gy,  gx]
    [gx, gy]

  # map from the square [ -1 -1, 1 -1, 1 1, -1 1 ] to λ: -π..π, φ: -π/2..π/2
  # with -180 lat going to the northwest-pointing diagonal, -90 going to the northeast-pointing, and so forth.
  invert: (gx, gy) =>
    # rotate into the domain of the raw transfer (triangle [ 0 0, -1 1, 1 1 ])
    if      ((gy >  gx) && (gy >= -gx)) then dλ = -0.75; rot_x =  gx; rot_y =  gy # top center
    else if ((gy <= gx) && (gy >= -gx)) then dλ = -0.25; rot_x = -gy; rot_y =  gx # right
    else if ((gy <= gx) && (gy <  -gx)) then dλ =  0.25; rot_x = -gx; rot_y = -gy # bottom
    else                                     dλ =  0.75; rot_x =  gy; rot_y = -gx # left
    [λ, φ] = @raw_inv(rot_x, rot_y)
    # scale down to -45..45 and then shift by -135, -45, 45 or 135 to cover the appt ranges
    λ = λ/4 + dλ*π;
    [λ, φ]

  # given a value in λ: -π..π, φ: -π/2..π/2, map to the triangle [ 0 0, -1 1, 1 1 ]
  raw_fwd: (λ, φ) =>
    gy = asqrt( (1 - Math.sin(φ))/2 )
    [ -gy * (λ / π), gy ]

  # given a value in x: -1..1 and y: 0..1 return the latitude λ: -π..π  φ: -π/2..π/2
  raw_inv: (gx, gy) =>
    λ   = if gy == 0 then 0 else (-π * gx / gy)
    φ   = asin( 1 - (2 * gy * gy) )
    [λ, φ]

  projection: ()->
    @forward.invert = @invert
    d3.geo.projection(@forward).center([0,90])

class CoolHat
  b_sc:  0.31726

  constructor: (@cutpar) ->
    @cutrad = @cutpar * rads
    @hat    = new CollSq
    @belt   = d3.geo.equirectangular.raw
    @cut_y  = @belt_fwd(0,    @cutrad)[1]
    @left_x  = @hat.forward(-90*rads, @cutrad)[0]
    @right_x = @hat.forward(  0*rads,  @cutrad)[0]
    @dx      = (@right_x - @left_x) * 2
    @dy      = @hat.forward(-45*rads,  @cutrad)[1]
    console.log(
      @belt_fwd(-180*rads, -90*rads), @belt_fwd(-180*rads, 90*rads), @belt_fwd(180*rads, -90*rads), @belt_fwd(180*rads, 90*rads),
      @dx, @dy*4)

  forward: (λ, φ) =>
    if (φ > @cutrad) # && (λ < -0.5 * π/2)
      [gx, gy] = @hat.forward(λ, φ)
      [mgx, mgy] = [gx,gy]
      mgx = (gx - @left_x) / @dx - 0.5
      mgy = ((gy-@dy) / (-@dy*4)) + @cut_y
      # console.log mgx, mgy, gx, gy, @cut_y
      [mgx, mgy]
    else
      @belt_fwd(λ, φ)

  belt_fwd: (λ, φ)->
    [gx, gy] = @belt(λ, φ)
    [gx / Math.PI, 2 * gy / Math.PI ]

  projection: ()->
    # @forward.invert = @invert
    proj = d3.geo.projection(@forward)
    # @outline_projection(proj)
    proj

class CollSqMap extends map4c.Map
  throttle_timer: null
  cutpar: 22.5

  set_projection: ()->
    size = width
    @projection = new CollSq(@nlobes, @cutpar, @lampar).projection()
      .precision(5)
      .clipExtent([[-size/2,-size/2], [size/2, size/2]])
      .center([0,90])
    @projection = new CoolHat(@cutpar).projection().center([0, 0]).precision(0)
    @projection
      .scale(size/2)
      .translate([0,0])
    @path.projection(@projection)
    @path_objs?.attr("d", @path)
    @redraw("set projection")

  cutpar_updated: (@cutpar, ctl)->
    set_projection()


  # redraw: (force)->
  #   super(force)
  #   if (not force)
  #     window.clearTimeout(@throttle_timer);
  #     @throttle_timer = window.setTimeout( (()=> console.log(this, new Date); @redraw(true)), 200)

window.map       = new CollSqMap(width, height)
map.set_projection()
datasets         = new map4c.Datasets(map)
datasets.fetch()



  # outline_projection: (proj)->
  #   wrapped_stream = proj.stream
  #   foo = (pstr)=>
  #     pstr = wrapped_stream(pstr)
  #     pstr.sphere = ()=>
  #       step = 45
  #       ε = 1e-1
  #       λ = ε - 180
  #       pstr.polygonStart(); pstr.lineStart()
  #       pstr.point(λ, (@cutpar))
  #       pstr.point(λ, (90 - ε))
  #       λ += step; pstr.point(λ, (90 - ε))
  #       λ += step; pstr.point(λ, (90 - ε))
  #       pstr.point(λ, (@cutpar))
  #       while (λ < 180)
  #         λ += step; pstr.point(λ - ε, (@cutpar))
  #       pstr.point(180,  (@cutpar))
  #       pstr.point(180,  -89)
  #       #
  #       λ = 180 - ε
  #       pstr.point(λ,  -89)
  #       while (λ > -180)
  #         λ -= step; pstr.point(λ - ε, -89)
  #       pstr.point(-180, -89)
  #       pstr.point(-180, (@cutpar))
  #       pstr.lineEnd(); pstr.polygonEnd()
  #       pstr
  #     pstr
  #   proj.stream = foo
  #   return proj
