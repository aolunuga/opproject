/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.application;

import onepoint.express.XSplashWindow;

/**
 * Class responsible for starting the application with a splash screen.
 *
 * @author horia.chiorean
 */
public final class OpBasicSplasher {

   /**
    * This is not a class that should be instantiated.
    */
   private OpBasicSplasher() {
   }

   /**
    * Main class for starting the application with a splash screen.
    * @param args a <code>
    */
   public static void main(String[] args) {
      XSplashWindow splash = XSplashWindow.splash(OpBasicSplasher.class.getResource("opp_splash_be_061.png"));
      XSplashWindow.invokeMain(OpBasicApplication.class.getName(), args);
      splash.disposeSplash();
   }
}
