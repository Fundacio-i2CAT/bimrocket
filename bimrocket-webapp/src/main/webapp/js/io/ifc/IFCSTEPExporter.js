/**
 * IFCSTEPExporter.js
 *
 * @author realor
 */

import { IFCExporter } from "./IFCExporter.js";
import { STEPWriter } from "../STEP.js";
import { IFC, Constant } from "./IFC.js";

class IFCSTEPExporter extends IFCExporter
{
  constructor()
  {
    super();
  }

  exportFile(ifcFile)
  {
    const writer = new class IFCSTEPWriter extends STEPWriter
    {
      constructor()
      {
        super();
      }

      createEntityTag(entity)
      {
        const tag = super.createEntityTag(entity);
        entity._id = tag;
        return tag;
      }
    };

    writer.schemaName = IFC.DEFAULT_SCHEMA_NAME;
    writer.constantClass = Constant;

    let entry;

    // IfcRoots first (and dependent entities)
    for (entry of ifcFile.entitiesByGlobalId)
    {
      let entity = entry[1];
      writer.addEntities(entity);
    }

    // Other entities
    for (entry of ifcFile.entitiesByClass)
    {
      let entity = entry[1];
      if (!entity.GlobalId)
      {
        writer.addEntities(entity);
      }
    }
    const output = writer.write();

    return output;
  }
}

export { IFCSTEPExporter };
