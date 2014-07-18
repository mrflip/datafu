width  = 800 # document.getElementById('map').offsetWidth
height = 800 # width / 2

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

class ProjectionFooler extends map4c.Map
  proj_name:  "orthographic"

  constructor: (width, height)->
    super(width, height)
    this_map = this
    choose_projection = ()-> this_map.proj_name = this.value; this_map.set_projection();
    d3.select("#projection_ctl").on("change", choose_projection) # .call(choose_projection)
    @set_projection()

  add_layers: (datasets)->
    super(datasets)
    new map4c.FeatureLayer this, "tiles",   "tile", datasets.tiles.features
    new map4c.FeatureLayer this, "quads",   "quad", datasets.quads.features
    new map4c.FeatureLayer this, "regions", "regn", datasets.regions.features

  reset: ()=>
    @zr.reset()
    @set_projection()

  redraw: (force)->
    if (@proj_name == "orthographic")
      return @redraw_rotating()
    else
      super(force)

  redraw_rotating: ()->
    sc       = @zr.scale()
    rot      = @zr.rotate()
    console.log sc, rot, @zr, @projection.rotate()
    @projection.rotate(rot)
    @path_objs?.attr("d", @path)
    @layers_g
      .attr("transform", "translate(" + (@width/2) + "," + (@height/2) + ")" +"scale(" + sc + ")")
      .style("stroke-width", 1.5 / sc + "px");

  set_projection: ()->
    sf = 126.82
    switch @proj_name
      when "equirectangular"
        @projection   = d3.geo.equirectangular().precision(0.1).scale(sf)
      when "gall_peters"
        @projection   = d3.geo.cylindricalEqualArea().parallel(45).scale(sf * Math.sqrt(2))
      when "eckert4"
        @projection   = d3.geo.eckert4().precision(.1).scale(150)
      when "orthographic"
        @projection   = d3.geo.orthographic().clipAngle(90).precision(.1).scale(300)
        @zr.reset()
      else #"mercator"
        @projection   = d3.geo.mercator().precision(0).scale(sf)
    @projection.translate([0,0])
    @path.projection(@projection)
    @redraw("chose projection")

  cenmer_updated: (val, ctl)->
    @cenmer = val
    @projection.rotate([-@cenmer, 0])
    @redraw()

window.map = map = new ProjectionFooler(width, height)
datasets         = new map4c.Datasets(map)

cenmer_ctl = new map4c.DomControl('cenmer', map);
cenmer_ctl.formatter((val)-> "Meridian = #{val}").update()

datasets.fetch()

#, world_lores, us_topo, us_land_topo, us_congress_topo, country_names, cities, us_counties, us_stats, ballparks)=>
# # lim_lat = 66.51326044311188 # 85.0511287798066 # 02 66.51326044311188; 0202  55.7765730186677; 02002 61.60639637138628; -56 tierra del fuego 0     85.0511287798066
