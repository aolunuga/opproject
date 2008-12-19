/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.util.HashMap;

import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

/**
 * Class that handles objects tags.
 */
public class OpObjectHandler implements XNodeHandler {

   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext, String, java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      String idString = (String) attributes.get(OpBackupManager.ID);
      Long id = new Long(idString);
      String system = (String) attributes.get(OpBackupManager.SYSTEM);
      return ((OpRestoreContext) context).activateObject(id, system);
   }

   /**
    * @see XNodeHandler#addChildNode(onepoint.xml.XContext, Object, String, Object)
    */
   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   /**
    * @see XNodeHandler#addNodeContent(onepoint.xml.XContext, Object, String)
    */
   public void addNodeContent(XContext context, Object node, String content) {
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext, String, Object, Object) 
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      // Persist object and add new ID to ID-map
      ((OpRestoreContext) context).persistActiveObject();
   }

}
