/**
 * gis.js
 *
 * @author realor
 */

export const translations =
{
  "menu.gis" : "GIS",

  "tool.wfs.label" : "Add WFS layer",
  "tool.wfs.help" : "Load data from Web Feature Service",

  "tool.mapView.label" : "Add map view",
  "tool.mapView.help" : "Load tiles from Map Service",

  "label.wfs.type" : "Select WFS type:",
  "label.wfs.layer_name" : "Layer name:",
  "label.wfs.url" : "WFS URL:",
  "label.wfs.geometry_type" : "Geometry format:",
  "label.wfs.limit_distance" : "Geometry load limit:",
  "label.wfs.limit_distance_help" : "Distance in 'm' or 'km' according to the existing IFC model. If there is no IFC, it does not apply.",
  "label.wfs.srs_name" : "Coordinate system:",
  "label.wfs.srs_name_help" : "Ex. EPSG:3857",
  "label.wfs.extrusion" : "Extrusion",
  "label.wfs.extrusion_depth" : "Extrusion depth (meters):",

  "label.mapView.provider" : "Provider:",
  "label.mapView.map_mode" : "Map mode:",
  "label.mapView.utm_zone" : "UTM zone (0 for Global Mercator):",
  "label.mapView.height_provider" : "Provider for height",
  "label.mapView.provider_key" : "Provider key:",
  "label.mapView.height_provider_key" : "Height provider key:",
  "label.mapView.wms_url" : "WMS URL:",
  "label.mapView.wms_layer" : "WMS Layer:",
  "label.mapView.max_requests_per_second" : "Max requests per second:",

  "option.wfs.geometry_polygon" : "Polygon",
  "option.wfs.geometry_line" : "Line",
  "option.wfs.geojson" : "GeoJSON",
  "option.wfs.gml2" : "GML2",
  "option.wfs.gml3" : "GML3",
  "option.wfs.gml32" : "GML32",

  "option.mapView.openstreetmap" : "OpenStreetMap",
  "option.mapView.googlemaps" : "Google Maps",
  "option.mapView.bingmaps" : "Bing Maps",
  "option.mapView.mapbox" : "MapBox",
  "option.mapView.heremaps" : "HERE Maps",
  "option.mapView.maptiler" : "MapTiler",
  "option.mapView.openmaptiles" : "OpenMapTiles",
  "option.mapView.mapboxheight" : "MapBox height",
  "option.mapView.wms" : "WMS",
  "option.mapView.planar" : "Planar",
  "option.mapView.spherical" : "Spherical",
  "option.mapView.height" : "Height",
  "option.mapView.height_shader" : "Height shader",
  "option.mapView.martini" : "Martini",

  "controller.WFSController" : "Loads geometry from Web Feature Service.",
  "controller.MapViewController" : "Loads tiles from a map server."
};
