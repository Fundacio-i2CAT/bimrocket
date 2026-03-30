/**
 * gis_es.js
 *
 * @author realor
 */

export const translations =
{
  "menu.gis" : "GIS",

  "tool.wfs.label" : "Añadir capa WFS",
  "tool.wfs.help" : "Cargar datos desde Web Feature Service",

  "tool.mapView.label" : "Añadir vista de mapa",
  "tool.mapView.help" : "Cargar teselas desde Map Service",

  "label.wfs.type" : "Seleccionar tipo de WFS:",
  "label.wfs.layer_name" : "Nombre de la capa:",
  "label.wfs.url" : "URL WFS:",
  "label.wfs.geometry_type" : "Formato de la geometría:",
  "label.wfs.limit_distance" : "Límite de carga de geometrías:",
  "label.wfs.limit_distance_help" : "Distancia en 'm' o 'km' según el modelo IFC existente. Si no hay IFC, no aplica.",
  "label.wfs.srs_name" : "Sistema de coordenadas:",
  "label.wfs.srs_name_help" : "Ej. EPSG:3857",
  "label.wfs.extrusion" : "Extrusión",
  "label.wfs.extrusion_depth" : "Profundidad de extrusión (metros):",

  "label.mapView.provider" : "Proveedor:",
  "label.mapView.map_mode" : "Modo del mapa:",
  "label.mapView.utm_zone" : "Zona UTM (0 para Global Mercator):",
  "label.mapView.height_provider" : "Proveedor para la altura",
  "label.mapView.provider_key" : "Clave del proveedor:",
  "label.mapView.height_provider_key" : "Clave del proveedor de altura:",
  "label.mapView.wms_url" : "URL de WMS:",
  "label.mapView.wms_layer" : "Capa de WMS:",
  "label.mapView.max_requests_per_second" : "Máx. solicitudes por segundo:",

  "option.wfs.geojson" : "GeoJSON",
  "option.wfs.gml2" : "GML2",
  "option.wfs.gml3" : "GML3",
  "option.wfs.gml32" : "GML32",
  "option.wfs.geometry_polygon" : "Polígono",
  "option.wfs.geometry_line" : "Línea",

  "option.mapView.openstreetmap" : "OpenStreetMap",
  "option.mapView.googlemaps" : "Google Maps",
  "option.mapView.bingmaps" : "Bing Maps",
  "option.mapView.mapbox" : "MapBox",
  "option.mapView.heremaps" : "HERE Maps",
  "option.mapView.maptiler" : "MapTiler",
  "option.mapView.openmaptiles" : "OpenMapTiles",
  "option.mapView.mapboxheight" : "MapBox Height",
  "option.mapView.wms" : "WMS",
  "option.mapView.planar" : "Plano",
  "option.mapView.spherical" : "Esferico",
  "option.mapView.height" : "Altura",
  "option.mapView.height_shader" : "Shader de altura",
  "option.mapView.martini" : "Martini",

  "controller.WFSController" : "Carga geometría de un servicio WFS.",
  "controller.MapViewController" : "Carga teselas de un servidor de mapas."
};
