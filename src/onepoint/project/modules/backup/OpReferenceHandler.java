/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpTypeManager;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

/**
 * Class that handles reference nodes.
 */
public class OpReferenceHandler implements XNodeHandler {

   /**
    * 
    */
   public OpReferenceHandler() {
   }
   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpReferenceHandler.class);

   /**
    * @see XNodeHandler#newNode(onepoint.xml.XContext,String,java.util.HashMap)
    */
   public Object newNode(XContext context, String name, HashMap attributes) {
      return new StringBuffer();
   }

   /**
    * @see XNodeHandler#addChildNode(onepoint.xml.XContext,Object,String,Object)
    */
   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   /**
    * @see XNodeHandler#addNodeContent(onepoint.xml.XContext,Object,String)
    */
   public void addNodeContent(XContext context, Object node, String content) {
      ((StringBuffer) node).append(content);
   }

   /**
    * @see XNodeHandler#nodeFinished(onepoint.xml.XContext,String,Object,Object)
    */
   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      // Members are always written in the same order as defined in the backup file header
      OpObjectIfc object = (OpObjectIfc) parent;
      OpRestoreContext restoreContext = (OpRestoreContext) context;
      OpBackupMember backupMember = restoreContext.nextBackupMember();
      String valueString = ((StringBuffer) node).toString().trim();

      if (!valueString.equals(OpBackupManager.NULL)) {
         
         Long objectId = new Long(valueString);
         OpPrototype pt = OpTypeManager.getPrototypeByID(backupMember.typeId);
         OpLocator locator = null;
         OpObjectIfc relationshipEnd = null;
//         if (pt.isInterface()) {
//            for (OpPrototype pti : pt.getSubTypes()) {
//           	   OpLocator loc = new OpLocator(pti, objectId);
//               relationshipEnd = restoreContext.getRelationshipOwner(loc);
//               if (relationshipEnd != null){ //found it
//            	  locator = loc;
//                  break;
//               }
//            }
//         }
//         if (locator == null) {
         locator = new OpLocator(pt, objectId);
         relationshipEnd = restoreContext.getRelationshipOwner(locator);
//         }
         Object value = null;
         if (relationshipEnd == null) {
        	 
        	 logger.warn("Cannot restore relationship from object "+restoreContext.activeLocator+" towards object " +locator);
            if (backupMember.accessor == null) {
               logger.warn("No Accessor for " + object + ":" + locator);
            }
            restoreContext.putRelationDelayed(object, locator, backupMember);
         }
         else {
            if (backupMember.relationship) {
               value = relationshipEnd;
            }
            else {
               //there was a type change
               String workingDirectory = (String) context.getVariable(OpRestoreContext.WORKING_DIRECTORY);
               //<FIXME author="Horia Chiorean" description="For relationships changed to attributes, this might not work correctly">
               value = OpBackupTypeManager.convertParsedValue(backupMember.typeId, String.valueOf(relationshipEnd.getId()), workingDirectory);
               //<FIXME>
            }
         }
         invokeMethod(object, backupMember, value);
//      }
//         finally {
//            broker.close();
//         }
      }
   }
   /**
    * @param object
    * @param backupMember
    * @param value
    * @pre
    * @post
    */
   private void invokeMethod(OpObjectIfc object, OpBackupMember backupMember,
         Object value) {
      Object[] arguments = new Object[]{value};
      try {
         //we should be somewhat graceful. It may happen, that members vanish...
         if (backupMember.accessor != null && !backupMember.wasDeleted) {
            if (backupMember.convertMethod != null) {
               try {
                  Method method = object.getClass().getDeclaredMethod(backupMember.convertMethod, new Class[0]);
                  value = method.invoke(object, new Object[0]);
                  arguments = new Object[]{value};
               }
               catch (SecurityException exc) {
                  logger.error("could not convert values", exc);
               }
               catch (NoSuchMethodException exc) {
                  // try it with arg:
                  try {
                     Method method = object.getClass().getDeclaredMethod(backupMember.convertMethod, backupMember.accessor.getParameterTypes());
                     value = method.invoke(object, arguments);
                     arguments = new Object[]{value};
                  }
                  catch (SecurityException exc2) {
                     logger.error("could not convert values", exc2);
                  }
                  catch (NoSuchMethodException exc2) {
                     logger.error("could not convert values", exc2);
                  }
               }
            }
            backupMember.accessor.invoke(object, arguments);
         }
         else {
          if (!backupMember.wasDeleted) {
             logger.error("skipped execution of accessor for " + backupMember.name + " as it was null.");
          }
         }
      }
      catch (InvocationTargetException e) {
         logger.error("Could not restore property value", e);
      }
      catch (IllegalAccessException e) {
         logger.error("Could not restore property value", e);
      }
      arguments = null;
   }

}
