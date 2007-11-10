/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocale;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.XError;

import java.util.*;

public class OpPermissionDataSetFactory {

   /**
    * The user error map.
    */
   private final static OpUserErrorMap USER_ERROR_MAP = new OpUserErrorMap();
   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpPermissionDataSetFactory.class);

   // Attention: Role icon index is "appended" to typical icon indexes of user module (reusability)
   public final static int GROUP_ICON_INDEX = 0;
   public final static int USER_ICON_INDEX = 1;
   public final static int ROLE_ICON_INDEX = 2;

   public final static int ACCESS_LEVEL_COLUMN_INDEX = 0;

   // Service parameter constant for transferring permission sets
   public final static String PERMISSION_SET = "PermissionSet";
   public final static String PERMISSION_TREE = "PermissionTree";
   public final static String PERMISSION_TOOL_PANEL = "PermissionToolPanel";

   // I18n resource-maps and names
   public final static String USER_PERMISSIONS_TAB = "user.permissions_tab";
   public final static String USER_OBJECTS = "user.objects";
   public final static String ADMINISTRATORS = "${Administrators}";
   public final static String MANAGERS = "${Managers}";
   public final static String CONTRIBUTORS = "${Contributors}";
   public final static String OBSERVERS = "${Observers}";

   private static final String GET_PERMISSION_COUNT_FOR_OBJECT =
        "select count(permission.ID) from OpPermission permission where permission.Object = (:objectId)";

   public static XComponent defaultPermissionRows(OpProjectSession session, OpBroker broker, XComponent permissionSet) {
      // Reload user and everyone objects in case it was detached
      OpUser user = session.user(broker);
      // TODO: I18n name of group everyone
      // (Note: We could maybe solve this by using a language-resource ${Everyone} in the display name)
      // *** ATTENTION: This would also solve our problem w/the naming of the root project folder and resource pool
      // ==> Use language resources ${RootProjectFolder} and ${RootPool}
      // TODO: Group everyone is not editable (at least not its properties), only membership
      // (Probably hard-code in edit-resource form-provider)
      // TODO: Check for correct schema *AND* required objects on startup
      // (Note: Auto-create required objects on startup rather than when creating schema -- more reliable/failsafe)
      // TODO: Should membership to group everyone be implicit or explicit (most probably explicit)?
      // Default permissions are user: ADMINISTRATOR; Everyone: OBSERVER
      XComponent permissionRow = new XComponent();
      XComponent permissionCell = new XComponent();
      permissionCell.setByteValue(OpPermission.ADMINISTRATOR);
      permissionRow.addChild(permissionCell);
      permissionRow.setStringValue(XValidator.choice(user.locator(), user.getDisplayName(), USER_ICON_INDEX));
      permissionSet.addChild(permissionRow);
      OpGroup everyone = session.everyone(broker);
      permissionRow = new XComponent();
      permissionCell = new XComponent();
      permissionCell.setByteValue(OpPermission.OBSERVER);
      permissionRow.addChild(permissionCell);
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(USER_OBJECTS));
      permissionRow.setStringValue(XValidator.choice(everyone.locator(), localizer.localize(everyone.getDisplayName()),
           GROUP_ICON_INDEX));
      permissionSet.addChild(permissionRow);
      return permissionSet;
   }

   public static void addSystemObjectPermissions(OpProjectSession session, OpBroker broker, OpObject object) {
      // System objects can be viewed by everyone, and administrated by the administrator user
      OpPermission permission = new OpPermission();
      permission.setObject(object);
      permission.setSubject(session.everyone(broker));
      permission.setAccessLevel(OpPermission.OBSERVER);
      broker.makePersistent(permission);
      permission = new OpPermission();
      permission.setObject(object);
      permission.setSubject(session.administrator(broker));
      permission.setAccessLevel(OpPermission.ADMINISTRATOR);
      broker.makePersistent(permission);
   }


   public static void administratePermissionTab(XComponent form, boolean editMode, byte accessLevel) {
      // Only administrator access allows to edit permission tab
      if (!editMode || (accessLevel < OpPermission.ADMINISTRATOR)) {
         form.findComponent(PERMISSION_TREE).setEnabled(false);
         form.findComponent(PERMISSION_TOOL_PANEL).setVisible(false);
      }
   }

   public static void retrievePermissionSet(OpProjectSession session, OpBroker broker, Set permissions,
        XComponent permissionSet, byte accessLevelMask, XLocale locale) {

      XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(locale.getID(), USER_PERMISSIONS_TAB);
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(resourceMap);
      XLanguageResourceMap objectResourceMap = XLocaleManager.findResourceMap(locale.getID(), USER_OBJECTS);
      XLocalizer objectLocalizer = new XLocalizer();
      objectLocalizer.setResourceMap(objectResourceMap);

      // Pre-fill permission map w/desired access levels
      TreeMap permissionMap = new TreeMap();
      ArrayList permissionList = null;
      if ((accessLevelMask & OpPermission.ADMINISTRATOR) > 0) {
         permissionMap.put(new Byte(OpPermission.ADMINISTRATOR), new ArrayList());
      }
      if ((accessLevelMask & OpPermission.MANAGER) > 0) {
         permissionMap.put(new Byte(OpPermission.MANAGER), new ArrayList());
      }
      if ((accessLevelMask & OpPermission.CONTRIBUTOR) > 0) {
         permissionMap.put(new Byte(OpPermission.CONTRIBUTOR), new ArrayList());
      }
      if ((accessLevelMask & OpPermission.OBSERVER) > 0) {
         permissionMap.put(new Byte(OpPermission.OBSERVER), new ArrayList());
      }

      OpPermission permission = null;
      boolean addDefaultPermissions = false;
      if (permissions == null) {
         addDefaultPermissions = true;
      }
      else {
         // Iterate access control list for object in question and build permission map based on access levels
         Iterator i = permissions.iterator();
         while (i.hasNext()) {
            permission = (OpPermission) i.next();
            permissionList = (ArrayList) permissionMap.get(new Byte(permission.getAccessLevel()));
            permissionList.add(permission);
         }
      }

      // Fill permission set using permission map (in reverse order of access levels)
      Object[] accessLevels = permissionMap.keySet().toArray();
      Object[] permissionLists = permissionMap.values().toArray();
      XComponent permissionRow = null;
      XComponent permissionCell = null;
      byte accessLevel = 0;
      String roleName = null;
      int j = 0;
      for (int i = accessLevels.length - 1; i >= 0; i--) {
         // *** Add i18n'ed role row w/access level
         accessLevel = ((Byte) accessLevels[i]).byteValue();
         permissionRow = new XComponent(XComponent.DATA_ROW);
         permissionRow.setExpanded(true);
         // Set localized access level/role name
         switch (accessLevel) {
            case OpPermission.ADMINISTRATOR:
               roleName = localizer.localize(ADMINISTRATORS);
               break;
            case OpPermission.MANAGER:
               roleName = localizer.localize(MANAGERS);
               break;
            case OpPermission.CONTRIBUTOR:
               roleName = localizer.localize(CONTRIBUTORS);
               break;
            case OpPermission.OBSERVER:
               roleName = localizer.localize(OBSERVERS);
               break;
         }
         permissionRow.setStringValue(XValidator.choice(roleName, roleName, ROLE_ICON_INDEX));
         permissionCell = new XComponent(XComponent.DATA_CELL);
         permissionCell.setByteValue(accessLevel);
         permissionRow.addChild(permissionCell);
         permissionSet.addChild(permissionRow);
         permissionList = (ArrayList) permissionLists[i];
         OpSubject subject = null;
         int iconIndex = 0;
         if (addDefaultPermissions) {
            //a "flag" indicating whether the data-row is mutable or not
            XComponent imutableFlag = new XComponent(XComponent.DATA_CELL);
            imutableFlag.setBooleanValue(false);

            switch (accessLevel) {
               case OpPermission.ADMINISTRATOR:
                  permissionRow = new XComponent(XComponent.DATA_ROW);
                  permissionRow.setOutlineLevel(1);
                  OpUser user = session.user(broker);
                  permissionRow.setStringValue(XValidator.choice(user.locator(), objectLocalizer.localize(user
                       .getDisplayName()), USER_ICON_INDEX));
                  if (user.getID() == session.administrator(broker).getID()) {
                     imutableFlag.setBooleanValue(true);
                  }
                  permissionRow.addChild(imutableFlag);
                  permissionSet.addChild(permissionRow);
                  break;
               case OpPermission.OBSERVER:
                  permissionRow = new XComponent(XComponent.DATA_ROW);
                  permissionRow.setOutlineLevel(1);
                  OpGroup everyone = session.everyone(broker);
                  permissionRow.setStringValue(XValidator.choice(everyone.locator(), objectLocalizer.localize(everyone
                       .getDisplayName()), GROUP_ICON_INDEX));
                  permissionRow.addChild(imutableFlag);
                  permissionSet.addChild(permissionRow);
                  break;
            }
         }
         else {
            for (j = 0; j < permissionList.size(); j++) {
               //a "flag" indicating whether the data-row is mutable or not
               XComponent imutableFlag = new XComponent(XComponent.DATA_CELL);
               imutableFlag.setBooleanValue(false);

               permission = (OpPermission) permissionList.get(j);
               permissionRow = new XComponent(XComponent.DATA_ROW);
               permissionRow.setOutlineLevel(1);
               subject = permission.getSubject();
               //<FIXME author="Jochen Mersmann" description="the try-block is a grace-case if you imported data from the team-edition, where this might happen. In the end, for the OE/PE we should not even touch this code" />
               try{ 
	               //don't use subject instance of OpGroup because of Hibernate's "proxy problem" when using polymorphic collections
	               if (OpTypeManager.getPrototypeForObject(subject).getName().equals(OpGroup.GROUP)) {
	                  iconIndex = GROUP_ICON_INDEX;
	               }
	               else {
	                  iconIndex = USER_ICON_INDEX;
	               }
	               permissionRow.setStringValue(XValidator.choice(subject.locator(), objectLocalizer.localize(subject.getDisplayName()), iconIndex));
	               // Disable system-managed permissions (not editable by the user)
	               permissionRow.setEnabled(!permission.getSystemManaged());
	               if (subject.getID() == session.administrator(broker).getID()) {
	                  imutableFlag.setBooleanValue(true);
	               }
	               permissionRow.addChild(imutableFlag);
	               permissionSet.addChild(permissionRow);
               } catch (NullPointerException npe){
            	   logger.info("did not have a user in permission for rolename '" +roleName +"'. Most propably imported team-data into single-user version");
               }
            }
         }
      }
   }


   private static boolean checkPermissionsAgainstLevel(OpBroker broker, XComponent permissionSet) {
      XComponent permissionRow = null;
      byte accessLevel = 0;
      String subjectLocator = null;
      OpUser user = null;
      OpGroup group = null;
      boolean permissionsOk = true;
      for (int i = 0; i < permissionSet.getChildCount(); i++) {
         permissionRow = (XComponent) permissionSet.getChild(i);
         if (permissionRow.getOutlineLevel() == 0) {
            // Role row: Set current access level
            accessLevel = ((XComponent) permissionRow.getChild(ACCESS_LEVEL_COLUMN_INDEX)).getByteValue();
         }
         else {
            // Subject row: Get subject,check for already persisted permission and insert new permission otherwise
            subjectLocator = XValidator.choiceID(permissionRow.getStringValue());
            OpLocator locator = OpLocator.parseLocator(subjectLocator);
            if (locator.getPrototype().getInstanceClass() == OpUser.class) {
               user = (OpUser) broker.getObject(subjectLocator);
               Byte highestPermission = OpUser.getHighestPermission(user.getLevel());
               if (accessLevel > highestPermission) {
                  permissionsOk = false;
               }
            }
            else {
               group = (OpGroup) broker.getObject(subjectLocator);
               for (Iterator iterator = group.getUserAssignments().iterator(); iterator.hasNext();) {
                  OpUserAssignment assignment = (OpUserAssignment) iterator.next();
                  user = assignment.getUser();
                  Byte highestPermission = OpUser.getHighestPermission(user.getLevel());
                  if (accessLevel > highestPermission) {
                     permissionsOk = false;
                  }
               }
            }
         }
      }
      return permissionsOk;
   }

   /**
    * Persists the <code>permissionSet</code> for the given <code>object</code>
    *
    * @param broker        a <code>OpBroker</code>
    * @param session       a <code>OpProjectSession</code> representing the server session.
    * @param object        a <code>XProject</code> or a <code>XProjectPorfolio</code> instance
    * @param permissionSet a <code>XComponent.DATA_SET</code> of permissions
    * @return a <code>XError</code> object if an error occured, or <code>null</code> if the operation was successfull.
    */
   public static XError storePermissionSet(OpBroker broker, OpProjectSession session, OpObject object, XComponent permissionSet) {

      if (!OpEnvironmentManager.isMultiUser() && permissionSet.getChildCount() == 0) {
         //set administrator permission on object (if not set already)
         if (!createAdministratorPermissions(object, broker)) {
            return session.newError(USER_ERROR_MAP, OpUserError.ADMIN_PERMISSION_ERROR);
         }
      }

      if (!checkPermissionsAgainstLevel(broker, permissionSet)) {
         return session.newError(USER_ERROR_MAP, OpUserError.PERMISSION_LEVEL_ERROR);
      }

      if ((object.getPermissions() == null) || (object.getPermissions().size() == 0)) {

         // No permissions are currently stored for this object: Insert new permission set
         XComponent permissionRow = null;
         byte accessLevel = 0;
         String subjectLocator = null;
         OpSubject subject = null;
         OpPermission permission = null;
         //permissions stored during current session for this object
         Map persistedPermissions = new HashMap();

         for (int i = 0; i < permissionSet.getChildCount(); i++) {
            permissionRow = (XComponent) permissionSet.getChild(i);
            if (permissionRow.getOutlineLevel() == 0) {
               // Role row: Set current access level
               accessLevel = ((XComponent) permissionRow.getChild(ACCESS_LEVEL_COLUMN_INDEX)).getByteValue();
            }
            else {
               // Subject row: Get subject,check for already persisted permission and insert new permission otherwise
               subjectLocator = XValidator.choiceID(permissionRow.getStringValue());
               if (!persistedPermissions.containsKey(subjectLocator)) { //not persisted before
                  subject = (OpSubject) broker.getObject(subjectLocator);
                  // TODO: Check if subject is OK
                  permission = new OpPermission();
                  permission.setObject(object);
                  permission.setSubject(subject);
                  permission.setAccessLevel(accessLevel);
                  broker.makePersistent(permission);
                  persistedPermissions.put(subjectLocator, permission);
               }
            }
         }
      }
      else {

         // There are permissions stored for this object: Update permission set
         HashMap permissionMap = new HashMap();
         Iterator permissions = object.getPermissions().iterator();
         OpPermission permission = null;
         while (permissions.hasNext()) {
            permission = (OpPermission) permissions.next();
            // Only add not system managed permissions to permission set
            // (Cannot be updated and are allowed in parallel to user-defined permissions)
            if (!permission.getSystemManaged()) {
               permissionMap.put(permission.getSubject().locator(), permission);
            }
         }
         removeSystemManagedPermissionRows(permissionSet);
         // Iterate permission set and try to get permission by subject from permission map
         XComponent permissionRow = null;
         String subjectLocator = null;
         byte accessLevel = 0;
         OpSubject subject = null;
         for (int i = 0; i < permissionSet.getChildCount(); i++) {
            permissionRow = (XComponent) permissionSet.getChild(i);
            if (permissionRow.getOutlineLevel() == 0) {
               // Role row: Set current access level
               accessLevel = ((XComponent) permissionRow.getChild(ACCESS_LEVEL_COLUMN_INDEX)).getByteValue();
            }
            else {
               removePermissionRows(permissionSet, permissionRow, accessLevel);
               // Check if a permission exists for the given subject
               subjectLocator = XValidator.choiceID(permissionRow.getStringValue());
               permission = (OpPermission) permissionMap.get(subjectLocator);
               if (permission != null) {
                  // Permission with this subject exists: Check access level and update if necessary
                  if (permission.getAccessLevel() != accessLevel) {
                     permission.setAccessLevel(accessLevel);
                     broker.updateObject(permission);
                  }
               }
               else {
                  // No permission exists for this subject yet: Insert new permission
                  subject = (OpSubject) broker.getObject(subjectLocator);
                  permission = new OpPermission();
                  permission.setObject(object);
                  permission.setSubject(subject);
                  permission.setAccessLevel(accessLevel);
                  broker.makePersistent(permission);
               }
            }
         }

         // Remove from permissionMap existent permissions from the permissionSet
         for (int i = 0; i < permissionSet.getChildCount(); i++) {
            permissionRow = (XComponent) permissionSet.getChild(i);
            if (permissionRow.getOutlineLevel() != 0) {
               subjectLocator = XValidator.choiceID(permissionRow.getStringValue());
               if (permissionMap.containsKey(subjectLocator)) {
                  permissionMap.remove(subjectLocator);
               }
            }
         }

         // Iterate remaining permissions in permission map and delete them from the database
         Iterator permissionsToDelete = permissionMap.values().iterator();
         while (permissionsToDelete.hasNext()) {
            permission = (OpPermission) permissionsToDelete.next();
            broker.deleteObject(permission);
         }

      }
      return null;
   }

   /**
    * Sets administrator permission on the given object (if not already set).
    *
    * @param object Object to set the permissions on.
    * @param broker Broker object to use for db access.
    * @return true if the permissions were set on the object (or if the permissions were already on the object).
    */
   private static boolean createAdministratorPermissions(OpObject object, OpBroker broker) {      
      if (!hasPermissions(broker, object)) {
         OpQuery query = broker.newQuery(OpUser.ADMINISTRATOR_ID_QUERY);
         List result = broker.list(query);
         if (result.size() != 1) {
            return false;
         }
         Long adminId = (Long) result.get(0);
         OpUser admin = (OpUser) broker.getObject(OpUser.class, adminId.longValue());
         OpPermission permission = new OpPermission();
         permission.setObject(object);
         permission.setSubject(admin);
         permission.setAccessLevel(OpPermission.ADMINISTRATOR);
         broker.makePersistent(permission);
      }
      return true;
   }

   /**
    * Removes from the <code>permissionSet</code> all permission rows which have the same locator as <code>permissionRow</code>
    * and access level lower than <code>permissionAccessLevel</code>.The navigation in the <code>permissionSet</code> is
    * performed downwords from the index of the <code>permissionRow</code>.
    *
    * @param permissionSet         <code>XComponent.DATA_SET</code> of permissions
    * @param permissionRow         <code>XComponent.DATA_ROW</code> permission row
    * @param permissionAccessLevel <code>int</code> the access level of the <code>permissionRow</code>
    */
   private static void removePermissionRows(XComponent permissionSet, XComponent permissionRow, int permissionAccessLevel) {
      int index = permissionRow.getIndex();
      String subjectLocator = XValidator.choiceID(permissionRow.getStringValue());
      int accessLevel = permissionAccessLevel;
      //navigate downwards and remove permission rows with the same locator but access level lower
      for (int i = ++index; i < permissionSet.getChildCount(); i++) {
         XComponent row = (XComponent) permissionSet.getChild(i);
         if (row.getOutlineLevel() == 0) {
            accessLevel = ((XComponent) row.getChild(ACCESS_LEVEL_COLUMN_INDEX)).getByteValue();
         }
         else {
            if (subjectLocator.equals(XValidator.choiceID(row.getStringValue())) && (permissionAccessLevel >= accessLevel)) {
               removePermissionRows(permissionSet, row, accessLevel);
               permissionSet.removeChild(i);
            }
         }
      }


   }

   /**
    * Filters the given <code>permissionSet</code> by removing all system managed permission rows.
    *
    * @param permissionSet <code>XComponent.DATA_SET</code> of permissions
    */
   private static void removeSystemManagedPermissionRows(XComponent permissionSet) {
      XComponent permissionRow;
      for (int i = permissionSet.getChildCount() - 1; i >= 0; i--) {
         permissionRow = (XComponent) permissionSet.getChild(i);
         if (!permissionRow.getEnabled()) { //system managed
            permissionSet.removeChild(i);
         }
      }
   }

   /**
    * Copies permissions from one object to another.
    *
    * @param broker a <code>OpBroker</code> used for persisting data.
    * @param from   an <code>OpObject</code> representing the source object (where to copy the permissions from)
    * @param to     an <code>OpObject</code> representing the destination object (where to copy the permissions to)
    */
   public static void updatePermissions(OpBroker broker, OpObject from, OpObject to) {
      //delete previous permissions of the destination object
      if (to.getPermissions() != null && !to.getPermissions().isEmpty()) {
         for (Iterator it = to.getPermissions().iterator(); it.hasNext();) {
            OpPermission permission = (OpPermission) it.next();
            broker.deleteObject(permission);
         }
      }

      Set fromPermissions = from.getPermissions();
      if (fromPermissions == null || fromPermissions.isEmpty()) {
         return;
      }
      Set newPermissions = new HashSet(fromPermissions.size());
      for (Iterator it = fromPermissions.iterator(); it.hasNext();) {
         OpPermission fromPermission = (OpPermission) it.next();

         OpPermission toPermission = new OpPermission();
         toPermission.setObject(to);
         toPermission.setSubject(fromPermission.getSubject());
         toPermission.setSystemManaged(fromPermission.getSystemManaged());
         toPermission.setAccessLevel(fromPermission.getAccessLevel());
         broker.makePersistent(toPermission);

         newPermissions.add(toPermission);
      }
      to.setPermissions(newPermissions);
   }

   /**
    * Returns <code>true</code> if the <code>OpObject</code> specified as parameter has permissions or <code>false</code> otherwise.
    *
    * @param broker - the <code>OpBroker</code> object needed to perform DB operations.
    * @param object - the <code>OpObject</code> object.
    * @return <code>true</code> if the <code>OpObject</code> specified as parameter has permissions or <code>false</code> otherwise.
    */
   public static boolean hasPermissions(OpBroker broker, OpObject object) {
      if (object.getPermissions() != null) {
         OpQuery query = broker.newQuery(GET_PERMISSION_COUNT_FOR_OBJECT);
         query.setLong("objectId", object.getID());
         Number counter = (Number) broker.iterate(query).next();
         if (counter.intValue() > 0) {
            return true;
         }
      }
      return false;
   }
}
