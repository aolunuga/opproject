/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.xml_rpc.onepoint.project.modules.user;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpContact;
import onepoint.project.modules.user.OpSubject;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserServiceImpl;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceException;
import onepoint.service.server.XServiceManager;

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
         throw new IllegalStateException("required service '"+OpUserServiceImpl.SERVICE_NAME+"' not found");
      }
      impl = (OpUserServiceImpl) xservice.getServiceImpl();
      if (impl == null) {
         throw new IllegalStateException("required service impl for 'UserService' not found");
      }
   }
   
   /**
    * Signs on the given username with the given password.
    * @param username
    * @param password
    * @return true (void methods don't work with xml-rpc)
    * @throws XmlRpcException
    */
   public boolean signOn(String username, String password) throws XmlRpcException {
      OpProjectSession session = OpProjectSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         impl.signOn(session, broker, username, password);
      } catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
      return true;
   }

   /**
    * Sign off the currently signed on user.
    * @return true (void methods don't work with xml-rpc)
    */
   public boolean signOff() throws XmlRpcException {
      OpProjectSession session = OpProjectSession.getSession();
      OpBroker broker = session.newBroker();
      try {
         impl.signOff(session, broker);
      } catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }
      return true;
   }

   /**
    * Returns all information associated to the currently signed on user.
    * @return a map containing all user information.
    * @throws XmlRpcException
    */
   public Map<String, Object> getSignedOnUserData() throws XmlRpcException
   {
      OpProjectSession session = OpProjectSession.getSession();
      OpBroker broker = session.newBroker();
      
      try {
         return(getUserData(impl.signedOnAs(session, broker)));
      } catch (XServiceException exc) {
         throw new XmlRpcException(exc.getError().getCode(), exc.getLocalizedMessage(), exc);
      }
      finally {
         broker.close();
      }      
   }

   /**
    * Returns all information associated to the currently signed on user.
    * @return a map containing all user information.
    */
   public static Map<String, Object> getUserData(OpUser user) {
      OpContact contact = user.getContact();
      Map<String, Object> ret = new HashMap<String, Object>();
      ret.put(OpSubject.NAME, user.getName());
      ret.put(OpSubject.DISPLAY_NAME, user.getDisplayName());
      ret.put(OpSubject.DESCRIPTION, user.getDescription());
      ret.put("level", new Integer(user.getLevel()));
      ret.put(OpContact.FIRST_NAME, contact.getFirstName());
      ret.put(OpContact.LAST_NAME, contact.getLastName());
      ret.put(OpContact.EMAIL, contact.getEMail());
      ret.put(OpContact.FAX, contact.getFax());
//      ret.put(OpSubject.ID, user.getID());
      ret.put(OpContact.MOBILE, contact.getMobile());
      ret.put(OpContact.PHONE, contact.getPhone());

      return ret;
   }
}
