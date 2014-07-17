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

class CollSqControls extends ControlSet
  update: (ctl)->
    size = height
    proj = new CollSq(@nlobes, @cutpar, @lampar).projection(size)
      .scale(size/2)
      .translate([size / 2, size/2])
      .rotate([@cenmer,0])
      .clipExtent([[0,0], [size, size]])
      # .precision(0)
    @map.set_projection(proj)

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
    [λ, φ] = @raw_inv(rot_x, rot_y)
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
    @forward.invert = @invert
    d3.geo.projection(@forward).center([0,-90])

# class CoolHat
#   constructor: (@nlobes, @cutpar, @lampar) ->
#     @cutrad = @cutpar * rads
#     @hat    = new CollSq
#     @shoes  =
# 
#   forward:

window.map      = new Map(width, height,
  ["/data/geo/atlas/topojson_atlas/world-110m.json"])
window.controls = new CollSqControls(window.map, ['cutpar', 'lampar', 'cenmer'])
window.bob      = new CollSq
