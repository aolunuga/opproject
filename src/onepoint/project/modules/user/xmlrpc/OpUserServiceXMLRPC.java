/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 *
 */
package onepoint.project.modules.user.xmlrpc;

import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpContact;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserServiceImpl;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceException;
import onepoint.service.server.XServiceManager;
import onepoint.service.server.XSession;
import org.apache.xmlrpc.XmlRpcException;

import java.util.HashMap;
import java.util.Map;

/**
 * Xml-Rpc service implementation corresponding to the OpUserService.
 *
 * @author dfreis
 */
public class OpUserServiceXMLRPC {

   /**
    * the underlaying OpUserService
    */
   private OpUserServiceImpl impl;

   /**
    * Default constructor setting up the underlying OpUserService.
    */
   public OpUserServiceXMLRPC() {
      XService xservice = XServiceManager.getService(OpUserServiceImpl.SERVICE_NAME);
      if (xservice == null) {
         throw new IllegalStateException("required service '" + OpUserServiceImpl.SERVICE_NAME + "' not found");
      }
      impl = (OpUserServiceImpl) xservice.getServiceImpl();
      if (impl == null) {
         throw new IllegalStateException("required service impl for 'UserService' not found");
      }
   }

   /**
    * Signs on the given username with the given password.
    *
    * @param username
    * @param password
    * @return true (void methods don't work with xml-rpc)
    * @throws XmlRpcException
    */
   public boolean signOn(String username, String password)
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      try {
         impl.signOn(session, username, password);
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      return true;
   }

   /**
    * Sign off the currently signed on user.
    *
    * @return true (void methods don't work with xml-rpc)
    */
   public boolean signOff()
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      try {
         impl.signOff(session);
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      return true;
   }

   /**
    * Returns all information associated to the currently signed on user.
    *
    * @return a map containing all user information.
    * @throws XmlRpcException
    */
   public Map<String, Object> getSignedOnUserData()
        throws XmlRpcException {
      OpProjectSession session = (OpProjectSession) XSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         return (getUserData(impl.signedOnAs(session, broker)));
      }
      catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Returns all information associated to the currently signed on user.
    *
    * @return a map containing all user information.
    */
   public static Map<String, Object> getUserData(OpUser user) {
      OpContact contact = user.getContact();
      Map<String, Object> ret = new HashMap<String, Object>();
      String value = user.getName();
      if (value == null) {
         value = "";
      }
      ret.put(OpSubject.NAME, value);
      value = user.getDisplayName();
      if (value == null) {
         value = "";
      }
      ret.put(OpSubject.DISPLAY_NAME, value);
      value = user.getDescription();
      if (value == null) {
         value = "";
      }
      ret.put(OpSubject.DESCRIPTION, value);
      Byte level = user.getLevel();
      if (level == null) {
         level = new Byte((byte) 0);
      }
      ret.put("level", new Integer(level));
      value = contact.getFirstName();
      if (value == null) {
         value = "";
      }
      ret.put(OpContact.FIRST_NAME, value);
      value = contact.getLastName();
      if (value == null) {
         value = "";
      }
      ret.put(OpContact.LAST_NAME, value);
      value = contact.getEMail();
      if (value == null) {
         value = "";
      }
      ret.put(OpContact.EMAIL, value);
      value = contact.getFax();
      if (value == null) {
         value = "";
      }
      ret.put(OpContact.FAX, value);
//      ret.put(OpSubject.id, user.getID());
      value = contact.getMobile();
      if (value == null) {
         value = "";
      }
      ret.put(OpContact.MOBILE, value);
      value = contact.getPhone();
      if (value == null) {
         value = "";
      }
      ret.put(OpContact.PHONE, value);

      return ret;
   }
}
