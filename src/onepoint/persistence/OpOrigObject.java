/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.custom_attribute.OpCustomAttribute;
import onepoint.project.modules.custom_attribute.OpCustomAttributeManager;
import onepoint.project.modules.custom_attribute.OpCustomAttributeServiceImpl;
import onepoint.project.modules.custom_attribute.OpCustomTextValue;
import onepoint.project.modules.custom_attribute.OpCustomType;
import onepoint.project.modules.custom_attribute.OpCustomValuePage;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpPermission;

public class OpOrigObject {

   private static final XLog logger = XLogFactory.getLogger(OpOrigObject.class);

   public final static String CREATED = "Created";
   public final static String MODIFIED = "Modified";
   public final static String ID = "ID";
   
   public static final int CUSTOM_VALUES_SIZE = 10;

   private static final String NAME_PARAM = "Name";

   private Timestamp created;
   private Timestamp modified;
   private Set<OpPermission> permissions;
   private Set<OpLock> locks;
   private long id = 0;
   private Set dynamicResources = new HashSet();
   private String siteId;
   private OpCustomValuePage customValuePage = null;
   private Set<OpCustomValuePage> customValuePages = null;

   private static OpCustomAttributeServiceImpl customAttributesServiceImpl;   
   
   // for caching only, transient and dynamic
   private Map<String, OpCustomAttribute> customTypeMap;

   public OpOrigObject() {
   }

   private void setId(long id) {
      this.id = id;
   }

   public long getId() {
      return id;
   }

   public String getSiteId() {
      return siteId;
   }

   public void setSiteId(String siteId) {
      this.siteId = siteId;
   }

   public void setCreated(Timestamp created) {
      this.created = created;
   }

   public Timestamp getCreated() {
      return created;
   }

   public void setModified(Timestamp modified) {
      this.modified = modified;
   }

   public Timestamp getModified() {
      return modified;
   }

   public void setPermissions(Set<OpPermission> permissions) {
      this.permissions = permissions;
   }
   
   public Set<OpPermission> getPermissions() {
      return permissions;
   }

   private void setLocks(Set<OpLock> locks) {
      this.locks = locks;
   }

   public Set<OpLock> getLocks() {
      return locks;
   }

   public Set getDynamicResources() {
      return dynamicResources;
   }

   public void setDynamicResources(Set dynamicResources) {
      this.dynamicResources = dynamicResources;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getBoolean(java.lang.String)
    */
   public Object getObject(String name) throws IllegalArgumentException {
      OpCustomAttribute att = getCustomAttribute(name);
      int type = att.getType();
      if (type == OpCustomAttribute.BOOLEAN) {
         return getBoolean(name);
      }
      if (type == OpCustomAttribute.DATE) {
         return getDate(name);
      }
      if (type == OpCustomAttribute.DECIMAL) {
         return getDecimal(name);
      }
      if (type == OpCustomAttribute.NUMBER) {
         return getNumber(name);
      }
      if (type == OpCustomAttribute.TEXT) {
         return getText(name);
      }
      if (type == OpCustomAttribute.MEMO) {
         return getMemo(name);
      }
      if (type == OpCustomAttribute.ATTACHMENT) {
         return getAttachment(name);
      }
      throw new IllegalArgumentException("type mismatch, attribute '"+name+"' is of unknown type '"+type);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getBoolean(java.lang.String)
    */
   public Boolean getBoolean(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.BOOLEAN);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getBoolean(pos%CUSTOM_VALUES_SIZE);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getDate(java.lang.String)
    */
   public Date getDate(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.DATE);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getDate(pos%CUSTOM_VALUES_SIZE);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getDecimal(java.lang.String)
    */
   public Double getDecimal(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.DECIMAL);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getDecimal(pos%CUSTOM_VALUES_SIZE);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getNumber(java.lang.String)
    */
   public Long getNumber(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.NUMBER);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getNumber(pos%CUSTOM_VALUES_SIZE);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getText(java.lang.String)
    */
   public String getText(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.TEXT);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getText(pos%CUSTOM_VALUES_SIZE);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getText(java.lang.String)
    */
   public OpCustomTextValue getMemo(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.MEMO);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getMemo(pos%CUSTOM_VALUES_SIZE);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getDocument(java.lang.String)
    */
   public OpAttachment getAttachment(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.ATTACHMENT);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getAttachment(pos%CUSTOM_VALUES_SIZE);
   }

   public Set<OpCustomValuePage> getCustomValuePages() {
      return customValuePages;
   }

   public void setCustomValuePages(Set<OpCustomValuePage> pages) {
      customValuePages = pages;
   }

   /**
    * @param name
    * @return
    * @pre
    * @post
    */
   private OpCustomAttribute getCustomAttribute(String name) {
      Byte subType = null;
      OpCustomType customType = null;
      if (this instanceof OpSubTypable) {
         subType = ((OpSubTypable) this).getType();
      }
      if (this instanceof OpCustomSubTypable) {
         customType = ((OpCustomSubTypable) this).getCustomType();
      }
      if (customTypeMap == null) {
         customTypeMap = OpCustomAttributeManager.getInstance().
            getCustomAttributesMap(this.getClass(), subType, customType == null ? null : customType.getCustomTypeName());
      }
      OpCustomAttribute ret = null;
      if (customTypeMap != null) {
         ret = customTypeMap.get(name);
      }
      if (ret == null) {
         // try parent classes
         Class type = this.getClass();
         while (true) {
            if (subType != null) {
               if (customType != null) {
                  customType = null;
               }
               else {
                  subType = null;
               }
            }
            else{
               if (type == OpObject.class) {
                  break;
               }
               type = type.getSuperclass();
            }
            
            OpCustomAttribute value = OpCustomAttributeManager.getInstance().
               getCustomAttribute(type, subType, customType == null ? null : customType.getCustomTypeName(), name);
            if (value != null) {
               return value;
            }
         }
         throw new IllegalArgumentException("no custom attribute found for name '"+name+"' within '"+this.getClass().getName()+"'");
      }
      return ret;
   }

   /**
    * @param classType
    * @param name
    * @pre
    * @post
    */
   private void check(OpCustomAttribute attribute, int type) {
	      if (attribute.getType() != type) {
	         throw new IllegalArgumentException("type mismatch, attribute '"+attribute.getName()+"' is of type '"+
	               OpCustomAttribute.DB_TYPES_NAMES[attribute.getType()]+"' and not of type '"+OpCustomAttribute.DB_TYPES_NAMES[type]+"' as expected");
	      }
	   }

   /**
    * @param page
    * @return
    * @pre
    * @post
    */
   private OpCustomValuePage getCustomValuePage(int page) {
      if (page > 0) {
         throw new IndexOutOfBoundsException("type position must be < "+CUSTOM_VALUES_SIZE);
      }
      if (page == 0) {
         return customValuePage;
      }
      // assuming pages to be ordered according there sequence!
      if (customValuePages == null) {
         return null;
      }
      Iterator<OpCustomValuePage> iter = customValuePages.iterator();
      if (iter.hasNext())
         return iter.next();
      return null;
   }

   @Override
   public boolean equals(Object object) {
      if (object == null) {
         return false;
      }
      return (object.getClass() == getClass()) && 
             (((OpOrigObject) object).id == id) && 
             (((OpOrigObject) object).id != 0)
             && (id != 0);
   }

   @Override
   public int hashCode() {
      if (id != 0) {
         return (int) (id ^ (id >>> 32));
      }
      else {
         return System.identityHashCode(this);
      }
   }

   public OpCustomValuePage getCustomValuePage() {
      return customValuePage;
   }

   public void setCustomValuePage(OpCustomValuePage customValuePage) {
      this.customValuePage = customValuePage;
   }

   /**
    * @return
    * @pre
    * @post
    */
   public boolean exists() {
      return getId() != 0;
   }

}