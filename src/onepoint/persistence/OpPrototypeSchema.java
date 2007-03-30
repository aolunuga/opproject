/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.xml.XSchema;

public class OpPrototypeSchema extends XSchema {

   public OpPrototypeSchema() {
      registerNodeHandler(OpPrototypeHandler.PROTOTYPE, new OpPrototypeHandler());
      registerNodeHandler(onepoint.persistence.OpFieldHandler.FIELD, new OpFieldHandler());
      registerNodeHandler(OpRelationshipHandler.RELATIONSHIP, new OpRelationshipHandler());
   }

}