/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.ldap;

import javax.naming.NamingException;
import javax.xml.bind.JAXBException;

import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XServer;

/**
 * Interface for OpLdapService. This interface was required because the user service performing signOn() calls 
 * needs to communicate with the ldap service in order to sign on ldap users. The ldap service implementation however
 * is part of the closed source project and therefor the user service must not know it directly.
 *    
 * @author dfreis
 *
 */
public interface OpLdapService {

   /**
    * Returns true if ldap is enabled, false otherwise.
    * @return true if ldap is enabled, false otherwise.
    */
   public abstract boolean isEnabled();
   
   /**
    * Initializes the Ldap Module.
    * @throws JAXBException in case of a JAXB error.
    * @throws NamingException in case of a naming error.
    */
   public abstract void init() throws JAXBException, NamingException;

   /**
    * Synchronizes all user and group information stored within ldap to the local OPP database.
    * Users and groups may be created, deleted or updated.
    * Users will not only be created if they are found within the names set by <code>setShedowUserNames()</code> or if 
    * <code>setShadowAllUserNames</code> was set to true. Updates are always running from within a different thread.
    * @param server the server to perform the operation.
    */
   public abstract void initialUpdate(XServer server);

   /**
    * Returns the hash algorithm used to encode the given users (username) password.
    * @param session the session to use. 
    * @param broker the broker to use.
    * @param username the username to get the hash encoding algorithm for.
    * @return the users password hash encoding algorithm name. 
    * @throws NamingException in case of a naming error
    */
   public abstract String getHashAlgorithm(OpProjectSession session, OpBroker broker, String username) 
   throws NamingException;

   /**
    * Signs on the {@link String username} with the given {@link String password}.
    * @param session the session to use.
    * @param broker the broker to use.
    * @param username the users name.
    * @param password the users password.
    * @return true if signOn went OK, false otherwise.
    * @throws NamingException in case of a naming error.
    */
   public abstract boolean signOn(OpProjectSession session, OpBroker broker, String username, String password) 
   throws NamingException;

   /**
    * Updates the given users data by shadowing the information from ldap to OPP. 
    * @param session the session to use.
    * @param broker the broker to use.
    * @param user the user to update.
    * @param password the users password hash.
    * @throws NamingException in case of a naming error.
    */
   public abstract void updateUser(OpProjectSession session, OpBroker broker, OpUser user, String password) throws NamingException;

   /**
    * Adds the user identified by {@link String userName} to the OPP database.
    * @param session the session to use.
    * @param broker the broker to use.
    * @param userName the name of the user to add.
    * @param password the users password hash.
    * @return the added user.
    * @throws NamingException in case of a naming error.
    */
   public abstract OpUser addUser(OpProjectSession session, OpBroker broker,
         String userName, String password) throws NamingException;
}