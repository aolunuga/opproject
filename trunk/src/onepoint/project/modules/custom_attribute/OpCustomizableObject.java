/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import onepoint.error.XLocalizableException;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpCustomSubTypable;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpSubTypable;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpAttachment;
import onepoint.service.server.XServiceManager;
import onepoint.service.server.XSession;

/**
 * Interface to access custom attributes.
 * All OpObjects that can store custom attributes implement this interface.
 *
 * @author dfreis
 */

public class OpCustomizableObject extends OpObject implements OpCustomizable {

   private static final String NAME_PARAM = "Name";
   
   private static final XLog logger = XLogFactory.getLogger(OpObject.class);

   private OpCustomValuePage customValuePage = null;
   private Set<OpCustomValuePage> customValuePages = null;

   private static OpCustomAttributeServiceImpl customAttributesServiceImpl;   

   // for caching only, transient and dynamic
   private Map<String, OpCustomAttribute> customTypeMap;
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getBoolean(java.lang.String)
    */
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getObject(java.lang.String)
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
	      if (type == OpCustomAttribute.CHOICE) {
	          return getChoice(name);
	       }
	      throw new IllegalArgumentException("type mismatch, attribute '"+name+"' is of unknown type '"+type);
	   }
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getBoolean(java.lang.String)
    */
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setObject(java.lang.String, java.lang.Object)
    */
   public void setObject(String name, Object value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      int typeClass = type.getType();
      if (typeClass == OpCustomAttribute.BOOLEAN) {
         setBoolean(name, (Boolean)value);
         return;
      }
      if (typeClass == OpCustomAttribute.DATE) {
         setDate(name, (Date) value);
         return;
      }
      if (typeClass == OpCustomAttribute.DECIMAL) {
         setDecimal(name, (Double) value);
         return;
      }
      if (typeClass == OpCustomAttribute.NUMBER) {
         setNumber(name, (Long) value);
         return;
      }
      if (typeClass == OpCustomAttribute.TEXT) {
         setText(name, (String) value);
         return;
      }
      if (typeClass == OpCustomAttribute.ATTACHMENT) {
         setAttachment(name, (OpAttachment) value);
         return;
      }
      if (typeClass == OpCustomAttribute.MEMO) {
         setMemo(name, (OpCustomTextValue) value);
         return;
      }
      if (typeClass == OpCustomAttribute.CHOICE) {
          setChoice(name, (String) value);
          return;
       }
      throw new IllegalArgumentException("type mismatch, attribute '"+name+"' is of unknown type '"+typeClass);
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
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setBoolean(java.lang.String, java.lang.Boolean)
    */
   public void setBoolean(String name, Boolean value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.BOOLEAN, value);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setBoolean(pos%CUSTOM_VALUES_SIZE, value);
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
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setNumber(java.lang.String, java.lang.Long)
    */
   public void setNumber(String name, Long value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.NUMBER, value);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setNumber(pos%CUSTOM_VALUES_SIZE, value);
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
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDecimal(java.lang.String, java.lang.Double)
    */
   public void setDecimal(String name, Double value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.DECIMAL, value);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setDecimal(pos%CUSTOM_VALUES_SIZE, value);
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
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDate(java.lang.String, java.util.Date)
    */
   public void setDate(String name, Date value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.DATE, value);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setDate(pos%CUSTOM_VALUES_SIZE, value);
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
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getMemo(java.lang.String)
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
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getAttachment(java.lang.String)
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

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getAttachment(java.lang.String)
    */
   public String getChoice(String name) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.CHOICE);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         return null;
      }
      return page.getChoice(pos%CUSTOM_VALUES_SIZE);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setText(java.lang.String, java.lang.String)
    */
   public void setText(String name, String value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.TEXT, value);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setText(pos%CUSTOM_VALUES_SIZE, value);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setText(java.lang.String, java.lang.String)
    */
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setMemo(java.lang.String, onepoint.project.modules.custom_attribute.OpCustomTextValue)
    */
   public void setMemo(String name, OpCustomTextValue value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.MEMO, value);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setMemo(pos%CUSTOM_VALUES_SIZE, value);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setDocument(java.lang.String, onepoint.project.modules.attachments.OpDocument)
    */
   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setAttachment(java.lang.String, onepoint.project.modules.project.OpAttachment)
    */
   public void setAttachment(String name, OpAttachment value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.ATTACHMENT, value);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setAttachment(pos%CUSTOM_VALUES_SIZE, value);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setAttachment(java.lang.String, onepoint.project.modules.project.OpAttachment)
    */
   public void setChoice(String name, String value) throws IllegalArgumentException {
      OpCustomAttribute type = getCustomAttribute(name);
      check(type, OpCustomAttribute.CHOICE, value);
      int pos = type.getPosition();
      OpCustomValuePage page = getCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      if (page == null) {
         page = createCustomValuePage(pos/CUSTOM_VALUES_SIZE);
      }
      page.setChoice(pos%CUSTOM_VALUES_SIZE, value);
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getCustomValuePages()
    */
   public Set<OpCustomValuePage> getCustomValuePages() {
      return customValuePages;
   }

   private OpCustomValuePage createCustomValuePage(int pageNr) {
      if (customValuePages == null) {
         customValuePages = new TreeSet<OpCustomValuePage>();
      }
      OpBroker broker = OpBroker.getBroker();
      if (broker == null) {
         logger.error("Broker is required in order to set custom values");
         return null;
      }
      OpCustomValuePage page = new OpCustomValuePage();
      page.setObject(this);
      page.setSequence(pageNr);
      broker.makePersistent(page);
      if (pageNr == 0) {
         setCustomValuePage(page);
      } 
      else {
         customValuePages.add(page);
      }
      return page;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setCustomValuePages(java.util.Set)
    */
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
    * @param classType
    * @param name
    * @pre
    * @post
    */
   private void check(OpCustomAttribute attribute, int type, Object value) {
      check(attribute, type);
      if (attribute.isMandatory() && value == null) {
         Map<String, Object> params = new HashMap<String, Object>();
         params.put(NAME_PARAM, attribute.getLabel() != null ? attribute.getLabel() : attribute.getName());
         throw new XLocalizableException(OpCustomAttributeErrorMap.ERROR_MAP, OpCustomAttributeError.MANDATORY_ERROR, params);
      }
      if (attribute.isUnique()) {
         OpProjectSession session = (OpProjectSession) XSession.getSession();
         OpBroker broker = session.newBroker();
         try {
            if (customAttributesServiceImpl == null) {
               customAttributesServiceImpl = (OpCustomAttributeServiceImpl) XServiceManager.getServiceImpl(OpCustomAttributeServiceImpl.SERVICE_NAME);
            }
            List<OpObject> list = customAttributesServiceImpl.getObjects(session, broker, attribute, value);
            if (list.isEmpty()) {
               return;
            }
            if ((list.size() != 1) || (list.get(0).getId() != getId())) {
               Map<String, Object> params = new HashMap<String, Object>();
               params.put(NAME_PARAM, attribute.getLabel() != null ? attribute.getLabel() : attribute.getName());
               throw new XLocalizableException(OpCustomAttributeErrorMap.ERROR_MAP, OpCustomAttributeError.NOT_UNIQUE_ERROR, params);               
            }
         }
         finally {
            broker.close();
         }
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

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#getCustomValuePage()
    */
   public OpCustomValuePage getCustomValuePage() {
      return customValuePage;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.custom_attribute.OpCustomizable#setCustomValuePage(onepoint.project.modules.custom_attribute.OpCustomValuePage)
    */
   public void setCustomValuePage(OpCustomValuePage customValuePage) {
      this.customValuePage = customValuePage;
   }

}