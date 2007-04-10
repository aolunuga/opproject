/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project_planning.components.test;

import onepoint.express.application.XExpressApplication;
import onepoint.express.server.XFormLoader;
import onepoint.project.team.modules.project_planning.OpChartComponentHandler;
import onepoint.resource.*;

import java.awt.*;
import java.net.URL;

/**
 * Test class for the advanced project components.
 *
 * @author mihai.costin
 */
public class OpAdvancedProjectComponent_test {
   public static void main(String[] args) {

    XResourceBroker.setResourcePath("onepoint/project/team/modules/project_planning/components/test");

    // TODO: Next two loader should probably take resource path into account
    XLocaleMap locale_map = new XLocaleMapLoader()
        .loadLocaleMap("/locales.olm.xml");
    XLocaleManager.setLocaleMap(locale_map);

    XLanguageKitLoader language_kit_loader = new XLanguageKitLoader();
    XLanguageKit language_kit_en = language_kit_loader
        .loadLanguageKit("/language_kit_en.olk.xml");
    XLocaleManager.registerLanguageKit(language_kit_en);
    XLanguageKit language_kit_de = language_kit_loader
        .loadLanguageKit("/language_kit_de.olk.xml");
    XLocaleManager.registerLanguageKit(language_kit_de);

    OpChartComponentHandler chart_handler = new OpChartComponentHandler();
    XFormLoader.registerComponent(OpChartComponentHandler.PIPELINE_CHART_BOX, chart_handler);  

    // *** If local server start is necessary: Handled by application
    XExpressApplication application = new XExpressApplication("Component Test", 800, 600);

    URL imgURL = Thread.currentThread().getContextClassLoader().getResource("onepoint/express/test/test_icon.png");
    if (imgURL != null) {
       Image icon = Toolkit.getDefaultToolkit().createImage(imgURL);
       MediaTracker tracker = new MediaTracker(application);
       tracker.addImage(icon, 0);
       try {
          tracker.waitForID(0);
       }
       catch(InterruptedException e) {}
       application.setIconImage(icon);
    }

    // *** Maybe XDisplay could also just be a component-type?
    application.start();
    application.getSession().setLocale(XLocaleManager.findLocale("en"));
    application.setVisible(true); // *** OR: "open()"?

    // *** Here we could put a "loading" progress indicator
    application.getDisplay().showForm("/componentsTest.oxf.xml");
   }
}
