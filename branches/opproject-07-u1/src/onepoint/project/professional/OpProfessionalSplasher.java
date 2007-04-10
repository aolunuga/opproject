/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.professional;

import onepoint.express.XSplashWindow;

/**
 * @author mihai.costin
 */
public class OpProfessionalSplasher {

   /**
    * The name of the image that will be used as splash screen.
    */
   private static final String SPLASH_IMAGE_NAME = "Splash_Pro.png";
      
   /**
    * This is not a class that should be instantiated.
    */
   private OpProfessionalSplasher() {
   }

   /**
    * Main class for starting the application with a splash screen.
    * @param args a <code>
    */
   public static void main(String[] args) {
      XSplashWindow splash = XSplashWindow.splash(OpProfessionalSplasher.class.getResource(SPLASH_IMAGE_NAME));
      XSplashWindow.invokeMain(OpProfessionalApplication.class.getName(), args);
      splash.disposeSplash();
   }

}
