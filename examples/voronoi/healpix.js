
var π = Math.PI,
    map = canvasMap().outlinePrecision(0);

d3.select("#lobes"     ).on("change", update);
d3.select("#cutpar_ctl").on("change", update);
d3.select("#lampar_ctl").on("change", update);
d3.select("#cenmer_ctl").on("change", update);

update();

function update() {
  var n      = +d3.select("#lobes"     ).property("value");
  // var cutpar = +d3.select("#cutpar_ctl").property("value");
  // var lampar = +d3.select("#lampar_ctl").property("value");
  // var cenmer = +d3.select("#cenmer_ctl").property("value");
  var cutpar = 60, lampar = 0, cenmer = 0;
  if (lampar > 0.8 * cutpar) { lampar = 0.8 * cutpar; }
  d3.select("#lobes-output" ).property("value", n);
  d3.select("#cutpar-output").property("value", cutpar);
  d3.select("#lampar-output").property("value", lampar);
  d3.select("#cenmer-output").property("value", cenmer);
  d3.select("#map").call(map.projection(healpix(n, cutpar, lampar).rotate([cenmer,0]).scale(200))); //225
}

function healpix(h, cutpar, lampar) {
  var φ0      = cutpar * π / 180,
      lambert = d3.geo.cylindricalEqualArea.raw(lampar * π / 180),
      lam_wid = lambert(π, 0)[0] - lambert(-π, 0)[0],
      col_wid     = d3.geo.collignon.raw(π, φ0)[0] - d3.geo.collignon.raw(-π, φ0)[0],
      y0      = lambert(0, φ0)[1],
      y1      = d3.geo.collignon.raw(0, φ0)[1],
      dy1     = d3.geo.collignon.raw(0, π / 2)[1] - y1,
      col_ht  = 4 * dy1 * dy1 / lam_wid,
      k	      = 2 * π / h;

  function forward(λ, φ) {
    var point;
    if (Math.abs(φ) > φ0) {
      var lobe = Math.min(h - 1, Math.max(0, Math.floor( (λ + π) / k)));
      var lobe_off = (2*lobe + 1 - h) / (2*h);
      λ        = λ - 2 * π * lobe_off;
      point    = d3.geo.collignon.raw(λ, Math.abs(φ));

      grid_x = (point[0] / col_wid);
      grid_y = (point[1] - y1)/dy1;
      if (lobe != 1) {
        point[0] = lam_wid * (grid_x + lobe_off);
        point[1] = y0 + grid_y * col_ht;
      } else {
        point[0] = lam_wid * (grid_x + lobe_off);
        point[1] = y0 + (grid_y) * col_ht;
      }
      if (φ < 0) point[1] = -point[1];
    } else {
      point = lambert(λ, φ);
    }
    point[0] /= 2;
    return point;
  }

      // λ = λ + π * (h - 1) / h - i * k;
      // point[0] = (point[0] * lam_wid / col_wid) - (lam_wid * (h - 1) / (2 * h)) + (i * lam_wid / h);

  // TODO forward.invert = function(x, y) {};

  var projection = d3.geo.projection(forward),
      wrappedStream = projection.stream;

  var foo =
  function(stream) {
    stream = wrappedStream(stream);
    stream.sphere = function() {
      var step = 180 / h,
          λ,
          ε = 1e-1;
      stream.polygonStart(), stream.lineStart();
      stream.point(λ = ε - 180, cutpar);
      while (λ < 180) {
        stream.point(λ + ε, cutpar);
        stream.point(λ += step, 90);
        stream.point((λ += step) - ε, cutpar);
      }
      stream.point(180, cutpar);
      stream.point(180, -cutpar);
      stream.point(λ = 180 - ε, -90);
      while (λ > -180) {
        stream.point(λ - ε, -cutpar);
        stream.point(λ -= step, -90);
        stream.point((λ -= step) + ε, -cutpar);
      }
      stream.point(-180, -cutpar);
      stream.point(-180, cutpar);
      stream.lineEnd(), stream.polygonEnd();
    };
    return stream;
  };
  projection.stream = foo;
  
  return projection;
}
