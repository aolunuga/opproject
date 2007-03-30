/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

public class OpPrototypeLoader extends XLoader {

   public final static XSchema PROTOTYPE_SCHEMA = new OpPrototypeSchema();

   public OpPrototypeLoader() {
      super(new XDocumentHandler(PROTOTYPE_SCHEMA));
   }

   public OpPrototype loadPrototype(String filename) {
      return (OpPrototype) (loadObject(filename, null));
   }

}