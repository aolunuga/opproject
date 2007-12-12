/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.project.modules.custom_attribute.OpCustomAttribute;
import onepoint.project.modules.custom_attribute.OpCustomTypeManager;
import onepoint.project.modules.custom_attribute.OpCustomValuePage;
import onepoint.project.modules.user.OpLock;

import java.sql.Timestamp;
import java.util.*;

public class OpObject {

   public final static String CREATED = "Created";
   public final static String MODIFIED = "Modified";
   public final static String ID = "ID";
   
   public static final int CUSTOM_VALUES_SIZE = 10;

   private Timestamp created;
   private Timestamp modified;
   private Set permissions;
   private Set<OpLock> locks;
   private long id = 0;
   private Set dynamicResources = new HashSet();
   private String siteId;
   private OpCustomValuePage customValuePage = null;
   private Set<OpCustomValuePage> customValuePages = null;

   // for caching only, transient and dynamic
   private Map<String, OpCustomAttribute> customTypeMap;

   // The following field is intentionally transient and dynamic
   private byte effectiveAccessLevel;

   public OpObject() {
   }

   private void setID(long id) {
      this.id = id;
   }

   public long getID() {
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

   public void setPermissions(Set permissions) {
      this.permissions = permissions;
   }

   public Set getPermissions() {
      return permissions;
   }

   public void setLocks(Set<OpLock> locks) {
      this.locks = locks;
   }

   public Set<OpLock> getLocks() {
      return locks;
   }

   public void setEffectiveAccessLevel(byte effectiveAccessLevel) {
      this.effectiveAccessLevel = effectiveAccessLevel;
   }

   public byte getEffectiveAccessLevel() {
      return effectiveAccessLevel;
   }

   public String locator() {
      return OpLocator.locatorString(this);
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
   public Boolean getBoolean(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      checkType(type, Boolean.class);
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
      checkType(type, Date.class);
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
      checkType(type, Double.class);
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
      checkType(type, Long.class);
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
      checkType(type, String.class);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getText(pos%CUSTOM_VALUES_SIZE);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setBoolean(java.lang.String, java.lang.Boolean)
    */
   public void setBoolean(String name, Boolean value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      checkType(type, Boolean.class);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setBoolean(pos%CUSTOM_VALUES_SIZE, value);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDate(java.lang.String, java.util.Date)
    */
   public void setDate(String name, Date value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      checkType(type, Date.class);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setDate(pos%CUSTOM_VALUES_SIZE, value);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDecimal(java.lang.String, java.lang.Double)
    */
   public void setDecimal(String name, Double value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      checkType(type, Double.class);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setDecimal(pos%CUSTOM_VALUES_SIZE, value);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setNumber(java.lang.String, java.lang.Long)
    */
   public void setNumber(String name, Long value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      checkType(type, Long.class);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setNumber(pos%CUSTOM_VALUES_SIZE, value);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setText(java.lang.String, java.lang.String)
    */
   public void setText(String name, String value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      checkType(type, String.class);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setText(pos%CUSTOM_VALUES_SIZE, value);
   }

   public Set<OpCustomValuePage> getCustomValuePages() {
      return customValuePages;
   }

   private OpCustomValuePage createCustomValuePage(int pageNr) {
      if (customValuePages == null) {
         customValuePages = new TreeSet<OpCustomValuePage>();
      }
      OpCustomValuePage page = new OpCustomValuePage();
      page.setObject(this);
      page.setSequence(pageNr);
      if (pageNr == 0) {
         setCustomValuePage(page);
      } 
      else {
         customValuePages.add(page);
      }
      return page;
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
      String customTypeName = null;
      if (this instanceof OpSubTypable) {
         subType = ((OpSubTypable) this).getType();
      }
      if (this instanceof OpCustomSubTypable) {
         customTypeName = ((OpCustomSubTypable) this).getCustomTypeName();
      }
      if (customTypeMap == null) {
         customTypeMap = OpCustomTypeManager.getInstance().getCustomAttributesMap(this.getClass(), subType, customTypeName);
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
               if (customTypeName != null) {
                  customTypeName = null;
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
            
            OpCustomAttribute value = OpCustomTypeManager.getInstance().getCustomAttribute(type, subType, customTypeName, name);
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
   private void checkType(OpCustomAttribute attribute, Class type) {
      if (attribute.getTypeAsClass() != type) {
         throw new IllegalArgumentException("type mismatch, attribute '"+attribute.getName()+"' is of type '"+
               attribute.getTypeAsClass().getName()+"' and not of type '"+type.getName()+"' as expected");
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
             (((OpObject) object).id == id) && 
             (((OpObject) object).id != 0)
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

}