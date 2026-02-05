/*
 * WFSDialog.js
 *
 * @author nexus
 */

import { Dialog } from "./Dialog.js";
import { Controls } from "./Controls.js";
import { WFSController } from "../controllers/WFSController.js";
import { Solid } from "../core/Solid.js";
import { Profile } from "../core/Profile.js";
import { Extruder } from "../builders/Extruder.js";
import { RectangleBuilder } from "../builders/RectangleBuilder.js";
import { Formula } from "../formula/Formula.js";
import * as THREE from "three";

class WFSDialog extends Dialog
{
  constructor(application)
  {
    super("tool.wfs.label");
    this.application = application;
    this.setSize(420, 460);
    this.setI18N(application.i18n);
    this.setClassName("wfs_container");

    this.createUI();
  }

  createUI()
  {
    const bodyElem = this.bodyElem;
    bodyElem.innerHTML = "";
    bodyElem.style.padding = "10px";

    const groupElem = document.createElement("div");
    
    this.wfsTypeElem = Controls.addSelectField(groupElem, "wfsType",
      "label.wfs.type",
      [["geojson", "option.wfs.geojson"],
       ["gml2", "option.wfs.gml2"],
       ["gml3", "option.wfs.gml3"],
       ["gml32", "option.wfs.gml32"]],
      "geojson");
    this.wfsTypeElem.style.display = "flex";
    this.wfsTypeElem.style.flexDirection = "column";
    this.wfsTypeElem.style.width = "100%";
    this.wfsTypeElem.style.padding = "6px";
    this.wfsTypeElem.style.marginBottom = "6px";

    bodyElem.appendChild(groupElem);

    this.geometryTypeElem = Controls.addSelectField(bodyElem, "wfsGeometryType",
      "label.wfs.geometry_type",
      [["polygon", "option.wfs.geometry_polygon"],
       ["line", "option.wfs.geometry_line"]],
      "polygon");
    this.geometryTypeElem.style.display = "flex";
    this.geometryTypeElem.style.flexDirection = "column";
    this.geometryTypeElem.style.width = "100%";
    this.geometryTypeElem.style.padding = "6px";
    this.geometryTypeElem.style.marginBottom = "6px";

    this.urlElem = Controls.addTextField(bodyElem, "wfsUrl",
      "label.wfs.url",
      "https://geoserver.nexusgeografics.com/geoserver/fires/ows");
    this.urlElem.spellcheck = false;
    this.urlElem.style.padding = "6px";
    this.urlElem.style.marginBottom = "6px";
    this.markRequired(this.urlElem);

    this.layerNameElem = Controls.addTextField(bodyElem, "wfsLayerName",
      "label.wfs.layer_name", "");
    this.layerNameElem.spellcheck = false;
    this.layerNameElem.style.padding = "6px";
    this.layerNameElem.style.marginBottom = "6px";
    this.markRequired(this.layerNameElem);

    this.limitDistanceElem = Controls.addTextField(bodyElem,
      "wfsLimitDistance", "label.wfs.limit_distance", null);
    this.limitDistanceElem.spellcheck = false;
    this.limitDistanceElem.style.padding = "6px";
    this.limitDistanceElem.style.marginBottom = "6px";

    const limitDistanceNote = Controls.addText(bodyElem,
      "label.wfs.limit_distance_help");
    limitDistanceNote.style.display = "block";
    limitDistanceNote.style.fontSize = "11px";
    limitDistanceNote.style.color = "#9caded";
    limitDistanceNote.style.marginBottom = "10px";

    this.srsNameElem = Controls.addTextField(bodyElem, "wfsSrsName",
      "label.wfs.srs_name", "");
    this.srsNameElem.spellcheck = false;
    this.srsNameElem.style.padding = "6px";
    this.srsNameElem.style.marginBottom = "6px";

    this.extrusionElem = Controls.addCheckBoxField(bodyElem, "wfsExtrusion",
      "label.wfs.extrusion", false, "report_name");

    this.acceptButton = this.addButton("accept", "button.accept",
      () => this.onAccept());
    this.cancelButton = this.addButton("cancel", "button.cancel",
      () => this.hide());

    this.updateAcceptButtonState();
    this.urlElem.addEventListener("input", () =>
      this.updateAcceptButtonState());
    this.layerNameElem.addEventListener("input", () =>
      this.updateAcceptButtonState());
  }

  markRequired(inputElem)
  {
    const groupElem = inputElem.parentNode;
    if (!groupElem) return;

    const labelElem = groupElem.firstChild;
    if (!labelElem) return;

    labelElem.classList.add("required");
  }

  updateAcceptButtonState()
  {
    const urlValue = this.urlElem.value.trim();
    const layerValue = this.layerNameElem.value.trim();
    this.acceptButton.disabled =
      urlValue.length === 0 || layerValue.length === 0;
  }

  onAccept()
  {
    const application = this.application;
    const layerName = this.layerNameElem.value;
    const wfsType = this.wfsTypeElem.value;
    const srsName = this.srsNameElem.value;
    const extrusionEnabled = this.extrusionElem.checked;
    const url = this.urlElem.value;
    const geometryType = this.geometryTypeElem.value;
    const limitDistanceValue = Number.parseFloat(this.limitDistanceElem.value);

    const layerGroup = new THREE.Group();
    layerGroup.name = layerName || "WFS Layer";
    application.addObject(layerGroup, application.baseObject);

    const controllerName = "wfs_controller_" + Date.now();
    const controller = application.createController(
      WFSController,
      layerGroup,
      controllerName
    );

    controller.layer = layerName;
    controller.url = url;
    controller.username = "";
    controller.password = "";
    switch (wfsType)
    {
      case "gml2":
        controller.format = "GML2";
        break;
      case "gml3":
        controller.format = "GML3";
        break;
      case "gml32":
        controller.format = "GML32";
        break;
      default:
        controller.format = "GeoJSON";
    }
    controller.srsName = srsName;
    controller.representationMode = WFSController.ADD_OBJECT_REPR_MODE;

    if (Number.isFinite(limitDistanceValue) && limitDistanceValue > 0)
    {
      const sites = application.findObjects(
        $ => $("IFC", "ifcClassName") === "IfcSite");
      if (sites.length > 0)
      {
        const site = sites[0];
        site.updateMatrixWorld(true);
        const center = new THREE.Vector3(0, 0, 0);
        center.applyMatrix4(site.matrixWorld);
        controller.bbox = 
        [
          center.x - limitDistanceValue,
          center.y - limitDistanceValue,
          center.x + limitDistanceValue,
          center.y + limitDistanceValue
        ].join(",");
      }
      else
      {
        console.warn("WFS: limit distance ignored (IfcSite not found).");
      }
    }

    const representation = new Solid();
    representation.name = WFSController.REPRESENTATION_NAME;
    representation.builder = new Extruder();
    representation.builder.depth = extrusionEnabled ? 1 : 0;
    Formula.create(
      representation,
      "material",
      "new THREE.MeshPhongMaterial({ color: 0x808080 })",
      false
    );

    if (geometryType === "line")
    {
      const profile = new Profile();
      profile.builder = new RectangleBuilder();
      profile.builder.width = 4;
      profile.builder.height = 1;
      profile.rotation.x = 0;
      application.addObject(profile, representation);
    }

    application.addObject(representation, layerGroup);

    controller.start();

    this.hide();
  }
}

export { WFSDialog };
