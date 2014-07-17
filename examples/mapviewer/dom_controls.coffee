class DomControl
  input:   null
  output:  null

  constructor: (@name, @owner)->
    @input  = d3.select("##{@name}_ctl")
    @output = d3.select("##{@name}_out")
    @input.on("change", @update)
    @update_val()

  value:   ()->
    @input.property("value")

  # update our display, set our eponymous attribute on owner 
  update_val: ()->
    val = this.value()
    @output?.property("value", val)
    @owner[@name] = val

  # update our value, then notify owner that we've changed
  update: ()=>
    @update_val()
    @owner.update(this)

class ControlSet
  constructor: (@ctl_names)->
    this["#{ctl_name}_ctl"] = new DomControl(ctl_name, this) for ctl_name in ctl_names
    @update()

  update: (ctl)->
    true

window.DomControl = DomControl
window.ControlSet = ControlSet
