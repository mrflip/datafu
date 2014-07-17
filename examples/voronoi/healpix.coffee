π   = Math.PI
map = canvasMap().outlinePrecision(0)

class HealPix
  constructor: (@nlobes, @cutpar, @lampar) ->
    @φ0      = @cutpar * π / 180
    @lambert = d3.geo.cylindricalEqualArea.raw(@lampar * π / 180)
    @lam_wid = @lambert(π, 0)[0] - @lambert(-π, 0)[0]
    @col_wid = d3.geo.collignon.raw(π, @φ0)[0] - d3.geo.collignon.raw(-π, @φ0)[0]
    @y0      = @lambert(0, @φ0)[1]
    @y1      = d3.geo.collignon.raw(0, @φ0)[1]
    @dy1     = d3.geo.collignon.raw(0, π / 2)[1] - @y1
    @col_ht  = 4 * @dy1 * @dy1 / @lam_wid

  forward:  (λ, φ) =>
    point = null
    if (Math.abs(φ) > @φ0)
      lobe = Math.min(@nlobes - 1, Math.max(0, Math.floor( (λ + π) * @nlobes / (2*π) )))
      lobe_off = (2*lobe + 1 - @nlobes) / (2*@nlobes)
      λ        = λ - 2 * π * lobe_off
      point    = d3.geo.collignon.raw(λ, Math.abs(φ))
      #
      grid_x = (point[0] / @col_wid)
      grid_y = (point[1] - @y1)/@dy1
      point[0] = @lam_wid * (grid_x + lobe_off)
      point[1] = @y0 + grid_y * @col_ht
      if (φ < 0) then point[1] = -point[1]
    else
      point = @lambert(λ, φ)
    point[0] /= 2
    point

  projection: ()->
    proj           = d3.geo.projection(@forward)
    wrapped_stream = proj.stream
    foo = (pstr)=>
      pstr = wrapped_stream(pstr)
      pstr.sphere = ()=>
        step = 180 / @nlobes
        ε = 1e-1
        λ = ε - 180
        pstr.polygonStart(); pstr.lineStart()
        pstr.point(λ, @cutpar)
        while (λ < 180)
          true;      pstr.point(λ + ε, @cutpar)
          λ += step; pstr.point(λ,     90)
          λ += step; pstr.point(λ - ε, @cutpar)
        pstr.point(180, @cutpar)
        pstr.point(180, -10)
        λ = 180 - ε
        pstr.point(λ, -20)
        while (λ > -180)
          # true;      pstr.point(λ - ε, -10)
          λ -= step; pstr.point(λ,     -20)
          # λ -= step; pstr.point(λ + ε, -10)
        pstr.point(-180, -10)
        pstr.point(-180, @cutpar)
        pstr.lineEnd(); pstr.polygonEnd()
        pstr
      pstr
    proj.stream = foo
    return proj

class HealPixControls extends ControlSet
  update: (ctl)->
    proj = new HealPix(@nlobes, @cutpar, @lampar).projection().rotate([@cenmer,0]).scale(200)
    d3.select("#map").call(map.projection(proj))

controls  = new HealPixControls(['nlobes', 'cutpar', 'lampar', 'cenmer'])
