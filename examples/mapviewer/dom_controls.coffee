f = d3.format "7.2f"

class window.DomControl
  input:   null
  output:  null

  constructor: (@name, @ctlset)->
    @input  = d3.select("##{@name}_ctl")
    @output = d3.select("##{@name}_out")
    window.foo = @input
    console.log "DomControl", this
    @input.on("change", @update)

  value:   (val)->
    return if @input.empty()
    if val? then @input.property("value", val) else @input.property("value")

  # # update our display, set our eponymous attribute on ctlset 
  # set_val: ()->
  #   val = this.value()
  #   @output?.property("value", val)

  # update our value, then notify ctlset that we've changed
  update: ()=>
    console.log("")
    console.log("updating", @name, @value(), this)
    @ctlset["#{@name}_updated"]?(@value(), this)


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#
# Manage a set of controls    

# class ControlSet
#   constructor: (@owner, @ctl_names)->
#     this["#{ctl_name}_ctl"] = new DomControl(ctl_name, this) for ctl_name in ctl_names
#     owner = @owner
#     ptr_lnglat = d3.select('#ptr_lnglat')
#     owner.layers_g.on "mousemove", (tile,idx)->
#       lnglat = owner.projection.invert(d3.mouse(this))
#       ptr_lnglat.text([f(lnglat[0]), f(lnglat[1])])

class window.ZoomerControl
  zoom:       d3.behavior.zoom()
  
  constructor: (@owner, @width, @height)->
    @tx_ctl = new DomControl('tx', this)
    @ty_ctl = new DomControl('ty', this)
    @sc_ctl = new DomControl('sc', this)
    #
    @λ_rg  = d3.scale.linear().domain([0,  @width ]).range([ -180,  180  ]);
    @φ_rg  = d3.scale.linear().domain([0,  @height]).range([   90,  -90  ]);
    @tx_rg = d3.scale.linear().domain([-0.15*@width, 1.15*@width ]).range([0.35*@width,  1.65*@width ]).clamp(true);
    @ty_rg = d3.scale.linear().domain([-0.15*@height,1.15*@height]).range([0.35*@height, 1.65*@height]).clamp(true);
    @sc_rg = d3.scale.linear().domain([16, 800    ]).range([ 0.16,    8.0]).clamp(true);
    #
    @tx_off = width/2; @ty_off = height/2;
    #
    @zoom
      .scaleExtent(@sc_rg.domain())
      .on("zoom", @zoomed)
    @owner.layers_g.call(@zoom)
    #
    @reset()
    # 

  translate: ()-> [ @tx_rg(@tx), @ty_rg(@ty) ]
  rotate:    ()-> [ @λ_rg(@tx),  @φ_rg(@ty) ]

  scale:     ()-> @sc_rg(@sc)

  # ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  #
  # Zoom by mouse

  zoomed: ()=>
    console.log("zoomed", this)
    tr = d3.event.translate ; [tx, ty] = [ tr[0]-@tx_off, tr[1]-@ty_off ]
    sc = d3.event.scale
    #    
    @set_translate [tx, ty]  if [tx, ty] != [@tx, @ty]
    @set_scale     sc        if sc       != @sc
    @owner.redraw()

  # Called by zoomer. Do update dom; don't trigger redraw here
  # 

  set_scale: (val)->
    @sc   = +val
    @sc   = @sc_rg.invert(@scale()) # ensure it's in domain of values
    console.log "sc set", val, @sc, this, @sc_ctl
    @sc_ctl.value(Math.round(@sc))

  set_translate: (val)->
    console.log "translate set", val, this
    [@tx, @ty] = val
    tr = @translate()
    [@tx, @ty] = [ @tx_rg.invert(tr[0]), @ty_rg.invert(tr[1]) ]
    #  
    @tx_ctl.value(Math.round(@tx))
    @ty_ctl.value(Math.round(@ty))

  # Called by the dom. Don't update dom; do trigger redraw; don't call this yourself

  scale_updated: (val)->
    @set_scale(val)
    @zoom.scale(@sc)
    @owner.redraw()
        
  tx_updated: (val)=>
    @tx = +val ; @tx = @tx_rg.invert(@translate()[0])
    console.log "tx_updated", @tx, @zoom.translate(), this
    #
    @tx_ctl.value(Math.round(@tx))
    @update_zoom()
    console.log "tx_updated", @tx, @zoom.translate(), this
    @owner.redraw()
  
  ty_updated: (val)=> 
    @ty = +val ; @ty = @ty_rg.invert(@translate()[1])
    console.log "ty_updated", @ty, this
    #
    @ty_ctl.value(Math.round(@ty))
    @update_zoom()
    @owner.redraw()

  update_zoom: ()->
    @zoom.scale     @sc
    @zoom.translate [@tx + @tx_off, @ty + @ty_off]

  reset: ()=>
    @set_translate  [0, 0] # [@width/2, @height/2]
    @set_scale      100
    @update_zoom()
