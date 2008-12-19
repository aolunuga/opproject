/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.application;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.util.OpProjectConstants;


/**
 * @author mihai.costin
 */
public class OpOpenApplication extends OpBasicApplication {

   private final static XLog logger = XLogFactory.getLogger(OpOpenApplication.class);
   private final static String APPLICATION_TITLE = "Onepoint Project Open Edition";

   protected OpOpenApplication() {
      super(APPLICATION_TITLE);
   }

   protected final void start(String[] arguments) {
      //the home should've been initialized by the super class
      super.start(arguments);
   }

   protected OpInitializer getInitializer() {
      OpInitializerFactory factory = OpInitializerFactory.getInstance();
      factory.setInitializer(OpInitializer.class);
      return factory.getInitializer();
   }

   public static void main(String[] args) {
      OpOpenApplication application = new OpOpenApplication();
      application.start(args);
   }

   /**
    * @see onepoint.project.application.OpBasicApplication#getProductCode()
    */
   protected String getProductCode() {
      return OpProjectConstants.OPEN_EDITION_CODE;
   }
}