/*
 * WMSDialog.js
 *
 * @author nexus
 */

import { Dialog } from "./Dialog.js";
import { Controls } from "./Controls.js";
import { MapViewController } from "../controllers/MapViewController.js";
import * as THREE from "three";

class WMSDialog extends Dialog
{
  constructor(application)
  {
    super("tool.wms.label");
    this.application = application;
    this.setSize(420, 380);
    this.setI18N(application.i18n);
    this.setClassName("wms_container");

    this.createUI();
  }

  createUI()
  {
    const bodyElem = this.bodyElem;
    bodyElem.innerHTML = "";
    bodyElem.style.padding = "10px";

    this.providerElem = Controls.addSelectField(bodyElem, "wmsProvider",
      "label.wms.provider",
      [
        ["OpenStreetMapsProvider", "option.wms.openstreetmap"],
        ["GoogleMapsProvider", "option.wms.googlemaps"],
        ["BingMapsProvider", "option.wms.bingmaps"],
        ["MapBoxProvider", "option.wms.mapbox"],
        ["HereMapsProvider", "option.wms.heremaps"],
        ["MapTilerProvider", "option.wms.maptiler"],
        ["OpenMapTilesProvider", "option.wms.openmaptiles"],
        ["WMSProvider", "option.wms.wms"]
      ],
      "OpenStreetMapsProvider");

    this.providerElem.style.display = "flex";
    this.providerElem.style.flexDirection = "column";
    this.providerElem.style.width = "100%";
    this.providerElem.style.padding = "6px";
    this.providerElem.style.marginBottom = "6px";

    this.providerKeyElem = Controls.addTextField(bodyElem, "wmsProviderKey",
      "label.wms.provider_key", "");
    this.providerKeyElem.spellcheck = false;
    this.providerKeyElem.style.padding = "6px";
    this.providerKeyElem.parentNode.style.marginBottom = "6px";
    this.providerKeyElem.parentNode.style.display = "none";

    this.mapModeElem = Controls.addSelectField(bodyElem, "wmsMapMode",
      "label.wms.map_mode",
      [
        ["PLANAR", "option.wms.planar"],
        ["SPHERICAL", "option.wms.spherical"],
        ["HEIGHT", "option.wms.height"],
        ["HEIGHT_SHADER", "option.wms.height_shader"],
        ["MARTINI", "option.wms.martini"]
      ],
      "PLANAR");
    this.mapModeElem.style.display = "flex";
    this.mapModeElem.style.flexDirection = "column";
    this.mapModeElem.style.width = "100%";
    this.mapModeElem.style.padding = "6px";
    this.mapModeElem.style.marginBottom = "6px";

    this.utmZoneElem = Controls.addTextField(bodyElem, "wmsUtmZone",
      "label.wms.utm_zone", "0");
    this.utmZoneElem.spellcheck = false;
    this.utmZoneElem.style.padding = "6px";
    this.utmZoneElem.style.marginBottom = "6px";

    this.heightProviderKeyElem = Controls.addTextField(bodyElem,
      "wmsHeightProviderKey", "label.wms.height_provider_key", "pk.eyJ1IjoiYXZhbGxzIiwiYSI6ImNtaDkzMm40NDBhYWMyanIxbnVraGFqY2oifQ.iFeS28_97GcOTB5tUutR-Q");
    this.heightProviderKeyElem.spellcheck = false;
    this.heightProviderKeyElem.style.padding = "6px";
    this.heightProviderKeyElem.parentNode.style.marginBottom = "6px";
    this.heightProviderKeyElem.parentNode.style.display = "none";

    this.providerElem.addEventListener("change", () =>
      this.updateProviderKeyVisibility());
    this.mapModeElem.addEventListener("change", () =>
      this.updateProviderKeyVisibility());
    this.updateProviderKeyVisibility();

    this.acceptButton = this.addButton("accept", "button.accept",
      () => this.onAccept());
    this.cancelButton = this.addButton("cancel", "button.cancel",
      () => this.hide());
  }

  parseUtmZone(value)
  {
    if (!value) return { utmZoneNumber: 0, utmZoneLetter: "" };

    const trimmed = value.trim();
    if (trimmed === "" || trimmed === "0")
    {
      return { utmZoneNumber: 0, utmZoneLetter: "" };
    }

    const match = trimmed.match(/(\d+)([a-zA-Z]?)/);
    if (!match) return { utmZoneNumber: 0, utmZoneLetter: "" };

    const parsedNumber = Number.parseInt(match[1], 10);
    if (parsedNumber < 1 || parsedNumber > 60)
    {
      return { utmZoneNumber: 0, utmZoneLetter: "" };
    }

    const letter = (match[2] || "").toUpperCase();
    const utmZoneLetter =
      letter === "" || /^[A-Z]$/.test(letter) ? (letter || "N") : "N";

    return { utmZoneNumber: parsedNumber, utmZoneLetter };
  }

  buildProviderSetup(provider, providerKey)
  {
    if (provider === "WMSProvider")
    {
      return {
        baseUrl: "https://geoserveis.icgc.cat/servei/catalunya/orto-territorial/wms",
        layers: "ortofoto_gris_vigent",
        format: "image/png",
        transparent: true
      };
    }

    if (!providerKey)
    {
      return {};
    }

    if (
        provider === "MapBoxProvider" ||
        provider === "MapBoxHeightProvider"
      )
    {
      return { apiToken: providerKey };
    }
    else if (provider === "GoogleMapsProvider")
    {
      return { apiKey: providerKey };
    }
    else if (provider === "HereMapsProvider")
    {
      return { appId: providerKey, appCode: providerKey };
    }
    else if (
        provider === "MapTilerProvider" ||
        provider === "BingMapsProvider" ||
        provider === "OpenMapTilesProvider"
      )
    {
      return { key: providerKey };
    }

    return {};
  }

  applyHeightProviderSetup(layerGroup, controllerName, providerKey)
  {
    const heightSetupName = controllerName + "_MapBoxHeightProvider_height";
    layerGroup.userData[heightSetupName] = {};
    if (providerKey)
    {
      layerGroup.userData[heightSetupName].apiToken = providerKey;
    }
  }

  initLayerGroup(provider)
  {
    const layerGroup = new THREE.Group();
    layerGroup.name = "WMS Layer - " + provider;
    layerGroup.userData = {};

    if (!layerGroup.controllers)
    {
      layerGroup.controllers = {};
    }

    return layerGroup;
  }

  configureController(controller, mapMode, utmZoneNumber, utmZoneLetter, useHeightProvider)
  {
    controller.mapMode = mapMode;
    controller.utmZoneNumber = utmZoneNumber;
    controller.utmZoneLetter = utmZoneLetter;

    if (useHeightProvider)
    {
      controller.heightProvider = "MapBoxHeightProvider";
    }
  }

  updateProviderKeyVisibility()
  {
    const provider = this.providerElem.value;
    const requiresKey = [
      "GoogleMapsProvider",
      "MapBoxProvider",
      "HereMapsProvider",
      "MapTilerProvider",
      "OpenMapTilesProvider",
      "MapBoxHeightProvider",
      "BingMapsProvider"
    ].includes(provider);

    this.providerKeyElem.parentNode.style.display =
      requiresKey ? "block" : "none";
    const heightModes = ["HEIGHT", "HEIGHT_SHADER", "MARTINI"];
    const heightModeEnabled = heightModes.includes(this.mapModeElem.value);
    this.heightProviderKeyElem.parentNode.style.display =
      heightModeEnabled ? "block" : "none";
  }

  onAccept()
  {
    const application = this.application;
    const provider = this.providerElem.value;
    const mapMode = this.mapModeElem.value;
    const utmZoneValue = this.utmZoneElem.value;
    const useHeightProvider =
      ["HEIGHT", "HEIGHT_SHADER", "MARTINI"].includes(mapMode);
    const providerKey = this.providerKeyElem.value;
    const heightProviderKey = this.heightProviderKeyElem.value;

    const { utmZoneNumber, utmZoneLetter } = this.parseUtmZone(utmZoneValue);
    const layerGroup = this.initLayerGroup(provider);
    application.addObject(layerGroup, application.baseObject);

    const controllerName = "wms_controller_" + Date.now();
    const setupName = controllerName + "_" + provider;
    layerGroup.userData[setupName] = this.buildProviderSetup(
      provider,
      providerKey
    );

    if (useHeightProvider)
    {
      this.applyHeightProviderSetup(layerGroup, controllerName, heightProviderKey);
    }

    const controller = new MapViewController(layerGroup, controllerName);

    controller.provider = provider;
    this.configureController(
      controller,
      mapMode,
      utmZoneNumber,
      utmZoneLetter,
      useHeightProvider
    );

    layerGroup.controllers[controllerName] = controller;

    controller.init(application);
    controller.start();

    console.log("WMS Layer added and initialized:", provider);

    this.hide();
  }

}

export { WMSDialog };
