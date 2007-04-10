/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license;

import onepoint.license.OpLicense;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.user.OpUser;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import onepoint.service.server.XService;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

/**
 * @author : mihai.costin
 */
public class OpLicenseService extends OpProjectService {

   public final static String LICENSE = "license";

   private OpLicense license;

   private static final OpLicenseErrorMap ERROR_MAP = new OpLicenseErrorMap();

   public OpLicense getLicense() {
      return license;
   }

   public void setLicense(OpLicense license) {
      this.license = license;
   }

   public XMessage checkLicense(XSession s, XMessage request) {

      OpProjectSession session = (OpProjectSession) s;
      XMessage reply = new XMessage();

      if (license != null) {

         XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(session.getLocale().getID(), "license.error");
         XLocalizer localizer = new XLocalizer();
         localizer.setResourceMap(resourceMap);

         //number of users
         OpBroker broker = session.newBroker();
         String queryString = "select count(user) from OpUser as user where user.Level=? and user.Name != '"
              + OpUser.ADMINISTRATOR_NAME + "'";

         //managers
         OpQuery query = broker.newQuery(queryString);
         query.setByte(0, OpUser.MANAGER_USER_LEVEL);
         Iterator result = broker.iterate(query);
         if (result.hasNext()) {
            int mngUsers = ((Integer) result.next()).intValue();
            int licenseUsers = Integer.parseInt(license.getProduct().getManagerUsers());
            if (mngUsers > licenseUsers) {
               reply.setError(session.newError(ERROR_MAP, OpLicenseError.MANAGER_ERROR));
               broker.close();
               return reply;
            }
         }

         //standard
         query = broker.newQuery(queryString);
         query.setByte(0, OpUser.STANDARD_USER_LEVEL);
         result = broker.iterate(query);
         if (result.hasNext()) {
            int standardUsers = ((Integer) result.next()).intValue();
            int licenseUsers = Integer.parseInt(license.getProduct().getStandardUsers());
            if (standardUsers > licenseUsers) {
               reply.setError(session.newError(ERROR_MAP, OpLicenseError.STANDARD_ERROR));
               broker.close();
               return reply;
            }
         }

         //expiration date
         if (licenseExpired()) {
            reply.setError(session.newError(ERROR_MAP, OpLicenseError.STANDARD_ERROR));
            broker.close();
            return reply;
         }

         broker.close();
      }
      return reply;
   }

   public boolean licenseExpired() {
      if (license.getValidUntil() == null) {
         return false;
      }
      Date today = new Date(System.currentTimeMillis());
      DateFormat format = OpLicense.DATE_FORMAT;
      Date licenseDate;
      boolean expired = false;
      try {
         licenseDate = format.parse(license.getValidUntil());
         if (licenseDate.before(today)) {
            expired = true;
         }
      }
      catch (ParseException e) {
         //license does not have a valid date => expired
         expired = true;
      }
      return expired;
   }

   /**
    * @see XService#copyFrom(onepoint.service.server.XService)  
    */
   protected void copyFrom(XService otherService) {
      if (!(otherService instanceof OpLicenseService)) {
         throw new IllegalArgumentException("Cannot copy license module from module of class:" + otherService.getClass().getName());
      }
      super.copyFrom(otherService);
      this.license = ((OpLicenseService) otherService).license;
   }
}
