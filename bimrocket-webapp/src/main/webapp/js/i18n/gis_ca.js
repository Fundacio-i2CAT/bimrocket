/**
 * gis_ca.js
 *
 * @author realor
 */

export const translations =
{
  "menu.gis" : "GIS",

  "tool.wfs.label" : "Afegir capa WFS",
  "tool.wfs.help" : "Carregar dades des de Web Feature Service",

  "tool.mapView.label" : "Afegeix vista de mapa",
  "tool.mapView.help" : "Carregar tessel·les des de Map Service",

  "label.wfs.type" : "Selecciona tipus de WFS:",
  "label.wfs.layer_name" : "Nom de la capa:",
  "label.wfs.url" : "URL WFS:",
  "label.wfs.geometry_type" : "Format de la geometria:",
  "label.wfs.limit_distance" : "Límit de càrrega de geometries:",
  "label.wfs.limit_distance_help" : "Distància en 'm' o 'km' segons el model IFC existent. Si no hi ha IFC, no aplica.",
  "label.wfs.srs_name" : "Sistema de coordenades:",
  "label.wfs.srs_name_help" : "Ex. EPSG:3857",
  "label.wfs.extrusion" : "Extrusió",
  "label.wfs.extrusion_depth" : "Profunditat d'extrusió (metres):",

  "label.mapView.provider" : "Proveïdor:",
  "label.mapView.map_mode" : "Mode del mapa:",
  "label.mapView.utm_zone" : "Zona UTM (0 per Global Mercator):",
  "label.mapView.height_provider" : "Proveïdor per a l'altura",
  "label.mapView.provider_key" : "Clau del proveïdor:",
  "label.mapView.height_provider_key" : "Clau del proveïdor d'altura:",
  "label.mapView.wms_url" : "URL de WMS:",
  "label.mapView.wms_layer" : "Capa de WMS:",
  "label.mapView.max_requests_per_second" : "Màx. sol·licituds per segon:",

  "option.wfs.geometry_polygon" : "Polígon",
  "option.wfs.geometry_line" : "Línia",
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
  "option.mapView.mapboxheight" : "Altura de MapBox",
  "option.mapView.wms" : "WMS",
  "option.mapView.planar" : "Pla",
  "option.mapView.spherical" : "Esfèric",
  "option.mapView.height" : "Altura",
  "option.mapView.height_shader" : "Shader d'altura",
  "option.mapView.martini" : "Martini",

  "controller.WFSController" : "Carrega geometria d'un servei WFS.",
  "controller.MapViewController" : "Carrega tessel·les d'un servidor de mapes."
};
