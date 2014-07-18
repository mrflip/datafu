map4c = (window.map4c ?= {})

#
# A control on the page, with optional separate display, that notifies changes
# back to the responsible model
# 
class DomControl
  input:   null
  output:  null

  constructor: (@name, @ctlset)->
    @input  = d3.select("##{@name}_ctl")
    @output = d3.select("##{@name}_out")
    @_format = d3.identity
    @input.on("change", @update)

  # with arg: set value, return this; no arg: return current value
  # Changing value here does _not_ call {whatever}_update on owner
  value:   (val)->
    return if @input.empty()
    if val?
      @input.property("value", val)
      this
    else
      @input.property("value")

  formatter: (fn)->
    if fn?
      @_format = fn
      this
    else
      @_format

  # notify ctlset that we've changed
  update: ()=>
    val = @value()
    @output.text(@_format(val)) unless @input.empty() || @output.empty()
    @ctlset["#{@name}_updated"]?(val, this)


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#
# Handle Zooming and Scrolling    
#
# 
class ZoomerControl
  zoom:       d3.behavior.zoom()
  tr_frac:    0.65
  
  constructor: (@owner, @width, @height)->
    @tx_ctl = new DomControl('tx', this)
    @ty_ctl = new DomControl('ty', this)
    @sc_ctl = new DomControl('sc', this)
    @reset_ctl = d3.select("#reset_ctl").on("click", @owner.reset)
    #
    @λ_rg  = d3.scale.linear().domain([-@width,  @width ]).range([ -180,  180  ]);
    @φ_rg  = d3.scale.linear().domain([-@height, @height]).range([   90,  -90  ]);
    @sc_rg = d3.scale.linear().domain([50, 800    ]).range([ 0.5,    8.0]).clamp(true);
    @troffs = @width/2
    #
    @zoom
      .scaleExtent(@sc_rg.domain())
      .on("zoom", @zoomed)
    @owner.svg.call(@zoom)
    #
    @reset()
    # 

  translate: ()-> [ @tx, @ty ]
  rotate:    ()-> [ @λ_rg(@tx),  @φ_rg(@ty) ]

  scale:     ()-> @sc_rg(@sc)

  # ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  #
  # Zoom/Scroll by mouse
  #

  zoomed: ()=>
    tr = d3.event.translate
    sc = d3.event.scale
    tx = @coerce_tr(tr[0]-@troffs, @width)
    ty = @coerce_tr(tr[1]-@troffs, @height)
    #    
    @set_translate [tx, ty]  if [tx, ty] != [@tx, @ty]
    @set_scale     sc        if sc       != @sc
    @owner.redraw()

  # Called by zoomer. Do update dom; don't trigger redraw here

  set_scale: (val)->
    @sc   = +val
    @sc   = @sc_rg.invert(@scale()) # ensure it's in domain of values
    @sc_ctl.value(Math.round(@sc))

  set_translate: (val)->
    @tx = @coerce_tr(val[0], @width);
    @ty = @coerce_tr(val[1], @height); 
    #  
    @tx_ctl.value(Math.round(@tx))
    @ty_ctl.value(Math.round(@ty))

  # ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  #
  # Zoom/Scroll by direct numeric value
  #

  # Called by the dom. Don't update dom; do trigger redraw; don't call this yourself

  sc_updated: (val)->
    @set_scale(val)
    @set_translate([@tx, @ty])
    @update_zoom()
    @owner.redraw()
        
  tx_updated: (val)=>
    @tx = @coerce_tr(+val, @width)
    #
    @tx_ctl.value(Math.round(@tx))
    @update_zoom()
    @owner.redraw()
  
  ty_updated: (val)=> 
    @ty = @coerce_tr(+val, @height)
    #
    @ty_ctl.value(Math.round(@ty))
    @update_zoom()
    @owner.redraw()

  reset: ()=>
    @set_translate  [0,0]
    @set_scale      100
    @update_zoom()

  update_zoom: ()->
    @zoom.scale     @sc
    @zoom.translate [@tx + @troffs, @ty + @troffs]

  coerce_tr: (val, dim)->
    bound = (@tr_frac * dim * Math.sqrt(@scale()))
    if      val < -bound then return -bound
    else if val >  bound then return  bound
    else                      return  val

map4c.DomControl    = DomControl
map4c.ZoomerControl = ZoomerControl
