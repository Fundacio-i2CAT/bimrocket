/*
 * TubeBuilder.js
 *
 * @author jiponsI2cat
 */
import { ObjectBuilder } from "./ObjectBuilder.js";
import * as THREE from "three";

class TubeBuilder extends ObjectBuilder {
  constructor(radius = 6.0, zscale = 0.001, zoffset = -1) {
    super();
    this.type = "tube";
    this.radius = radius;
    this.zscale = zscale;
    this.zoffset = zoffset;
  }

  performBuild(object) {
    let source = object;
    if (!object.geometry || !object.geometry.type.includes("CordGeometry")) {
      source = object.children.find(c => c.geometry && c.geometry.type.includes("CordGeometry"));
    }

    if (!source || !source.geometry) return;

    try {
      const attr = source.geometry.attributes.position;
      if (!attr) return;

      const points = [];
      for (let i = 0; i < attr.count; i++) {
        const v = new THREE.Vector3().fromBufferAttribute(attr, i);
        // Avoid duplicated points that break the curve
        if (v && (points.length === 0 || v.distanceTo(points[points.length - 1]) > 0.01)) {
          points.push(v);
        }
      }

      if (points.length > 1) {
        const curve = new THREE.CatmullRomCurve3(points);
        // Creamos la geometría del tubo
        const tubeGeo = new THREE.TubeGeometry(curve, points.length * 4, this.radius, 8, false);
        
        // Asignamos la nueva geometría al objeto principal
        object.geometry = tubeGeo;

        if (object.material) {
          object.material.wireframe = false;
          object.material.needsUpdate = true;
        }

        object.edgesVisible = false;

        //plain tube 
        object.scale.set(1, 1, this.zscale);
        //avoid Z-fighting
        object.position.z -= this.zoffset;

        // if source is child (WFS ADD_OBJECT mode), we hidden it to avoid duplication
        if (source !== object) source.visible = false;
        
        object.updateMatrix();
      }
    } catch (e) {
      console.error("TubeBuilder Error:", e);
    }
  }

  clone() {
    return new TubeBuilder(this.radius);
  }
}

export { TubeBuilder };

