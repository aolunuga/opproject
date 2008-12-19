/*
 * Copyright(c) Onepoint Software GmbH 2008. All Rights Reserved.
 */

package onepoint.project.test;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import net.sf.cglib.proxy.Enhancer;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.log.server.XLog4JLog;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpSource;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.custom_attribute.OpCustomAttribute;
import onepoint.project.modules.custom_attribute.OpCustomType;
import onepoint.project.modules.external_applications.OpExternalApplication;
import onepoint.project.modules.external_applications.OpExternalApplicationParameter;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.user.OpContact;
import onepoint.project.modules.user.OpGroup;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocaleMap;
import onepoint.resource.XLocaleMapLoader;
import onepoint.resource.XResourceBroker;

/**
 * Base class for test initializers.
 *
 * @author mihai.costin
 */
public abstract class OpTestInitializer {
   /**
    * Class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpTestInitializer.class);

   /**
    * Initializes the tests.
    *
    * @param testProperties
    */
   public void initialize(Properties testProperties) {
      XResourceBroker.setResourcePath(OpTestDataFactory.RESOURCE_PATH);
      XLocaleMap locale_map = new XLocaleMapLoader().loadLocaleMap(OpTestDataFactory.LOCALES_OLM_XML);
      XLocaleManager.setLocaleMap(locale_map);
   }

   /**
    * Gets the source name that will be used for the tests.
    *
    * @return test source
    */
   public String getSourceName() {
      return OpSource.DEFAULT_SOURCE_NAME;
   }


   /**
    * This method checks if the objects from remaining list can be left into database or  not.
    *
    * @param session session to use
    */
   final public void checkObjects(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("select op from OpObject as op");
         List<OpPermissionable> remaining = broker.list(query, OpPermissionable.class);

         validateRemaningObjects(broker, remaining);
         if (logger.isLoggable(XLog4JLog.ERROR)) {
            for (OpPermissionable current : remaining) {
               logger.error("Object not deleted: " + current + ", id: " + current.getId() + ", type: " + current.getClass().getName());
            }
         }
         if (remaining.size() != 0) {
            throw new RuntimeException("Database still contains invalid objects. " + remaining);
         }
      }
      finally {
         broker.close();
      }
   }

   /**
    * This method checks if the objects from remaining list can be left into database or  not.
    *
    * @param broker    broker to use.
    * @param remaining list of objects to check.
    */
   protected void validateRemaningObjects(OpBroker broker, List<OpPermissionable> remaining) {
      Set<Long> systemObjIds = OpBackupManager.querySystemObjectIdMap(broker).keySet();
      Iterator<OpPermissionable> iter = remaining.iterator();
      while (iter.hasNext()) {
         OpPermissionable obj = iter.next();
         if (Enhancer.isEnhanced(obj.getClass())) {
            obj = (OpPermissionable) broker.getObject(obj.locator());
         }
         if (systemObjIds.contains(obj.getId())) {
            iter.remove();
         }
         else if (obj instanceof OpUser) {
            OpUser user = (OpUser) obj;
            if (OpUser.ADMINISTRATOR_NAME.equals(user.getName())) {
               iter.remove();
            }
         }
         else if (obj instanceof OpContact) {
            OpContact contact = (OpContact) obj;
            if (OpUser.ADMINISTRATOR_NAME.equals(contact.getUser().getName())) {
               iter.remove();
            }
         }
         else if (obj instanceof OpGroup) {
            OpGroup group = (OpGroup) obj;
            if (OpGroup.EVERYONE_NAME.equals(group.getName())) {
               iter.remove();
            }
         }
         else if (obj instanceof OpResourcePool) {
            OpResourcePool pool = (OpResourcePool) obj;
            if (OpResourcePool.ROOT_RESOURCE_POOL_NAME.equals(pool.getName())) {
               iter.remove();
            }
         }
         else if (obj instanceof OpProjectNode) {
            OpProjectNode node = (OpProjectNode) obj;
            if (OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME.equals(node.getName())) {
               iter.remove();
            }
         }
         else if (obj instanceof OpCustomType) {
            iter.remove();
         }
         else if (obj instanceof OpCustomAttribute) {
            iter.remove();
         }
         else if (obj instanceof OpExternalApplication) {
            iter.remove();
         }
         else if (obj instanceof OpExternalApplicationParameter) {
            iter.remove();
         }
         else if (obj instanceof OpPermission) {
            OpPermission permission = (OpPermission) obj;
            OpPermissionable object = permission.getObject();
            OpSubject subject = permission.getSubject();
            if (Enhancer.isEnhanced(object.getClass())) {
               object = (OpPermissionable) broker.getObject(object.locator());
            }
            if (Enhancer.isEnhanced(subject.getClass())) {
               subject = (OpSubject) broker.getObject(subject.locator());
            }
            if ((object instanceof OpResourcePool) &&
                 (OpResourcePool.ROOT_RESOURCE_POOL_NAME.equals(((OpResourcePool) object).getName()))) {
               if ((subject instanceof OpGroup) &&
                    (OpGroup.EVERYONE_NAME.equals(((OpGroup) subject).getName()))) {
                  iter.remove();
               }
               else if ((subject instanceof OpUser) &&
                    (OpUser.ADMINISTRATOR_NAME.equals(((OpUser) subject).getName()))) {
                  iter.remove();
               }
            }
            else if ((object instanceof OpProjectNode) &&
                 (OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME.equals(((OpProjectNode) object).getName()))) {
               if ((subject instanceof OpUser) &&
                    (OpUser.ADMINISTRATOR_NAME.equals(((OpUser) subject).getName()))) {
                  iter.remove();
               }
               if ((subject instanceof OpGroup) &&
                    (OpGroup.EVERYONE_NAME.equals(((OpGroup) subject).getName()))) {
                  iter.remove();
               }
            }
         }
      }
   }
}
