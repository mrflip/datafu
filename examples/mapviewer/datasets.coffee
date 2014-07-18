map4c = (window.map4c ?= {})

class map4c.Datasets
  constructor: (@map) -> true
  
  fetch: ->
    # Start fetching all the data
    queue()
      .defer(d3.json,    "/data/geo/atlas/topojson_atlas/world-110m.json")
      .defer(@geo_jsnl,  "/data/geo/atlas/world.json")
      .defer(@geo_jsnl,  "/output/world-quads-4-8-jsnl/part-m-00000")
      .defer(@geo_jsnl,  "/output/full-quads-4-7-jsnl/part-m-00000")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/world-50m.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us-land.json")
      # .defer(d3.json,  "/data/geo/atlas/topojson_atlas/us-congress-113.json")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/world-country-names.tsv")
      # .defer(d3.tsv,   "/data/geo/census/world_cities_grid.tsv")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/us-county-names.tsv")
      # .defer(d3.tsv,   "/data/geo/atlas/topojson_atlas/us-state-names.tsv")
      # .defer(d3.tsv,   "/voronoi/ballparks.tsv")
      .await(@ready)

  ready: (error, @world_topo, @regions, @quads, @tiles)=>
    if error then (d3.select("#infobox").text(error.message + "\n" + error.stack); throw error)
    @map.ready(this)    

  geo_jsnl: (url, callback)->
    d3.xhr url, "application/json", (request)->
      try
        text = request.responseText.replace(/\n$/, "")
        text = '{"type":"FeatureCollection","features":[{"type":"Feature","geometry":' +
          text.replace(/\n/gm, '},{"type":"Feature","geometry":') + "}]}"
        json = JSON.parse text
        callback(null, json)
      catch err
        callback(err)


  jsnl: (url, callback)->
    d3.xhr url, "application/json", (request)->
      try
        text = request.responseText.replace(/\n$/, "")
        text = "[" + text.replace(/\n/gm, ",") + "]"
        json = JSON.parse text
        callback(null, json)
      catch err
        callback(err)
