/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.server.XSession;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Form provider for the about dialog.
 *
 * @author mihai.costin
 * @author horia.chiorean
 */
public class OpAboutFormProvider implements XFormProvider {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpAboutFormProvider.class);

   /**
    * The map of [product_code, description] for the different flavours of the application.
    */
   private static final Map PRODUCT_CODES_DESCRIPTION;

   private static final String UNKNOWN_VERSION_NUMBER = "N/A";

   /**
    * The url where the product versions are retrieved from.
    */
   private static final String PRODUCT_VERSIONS_URL = "http://www.onepoint.at/current-versions.opv.xml";

   /**
    * Form component ids where the information will be shown.
    */
   private static final String PRODUCT_NAME_LABEL = "ProductName";
   private static final String VERSION_LABEL = "Version";
   private static final String CURRENT_VERSION_LABEL = "CurrentVersion";
   private static final String BUILD_LABEL = "Build";

   /**
    * XML elements and attribute names.
    */
   private final static String VERSION = "version";
   private final static String CODE = "code";
   private final static String PRODUCT = "product";
   private final static String BUILD = "build";

   /**
    * Initializer for the map of product codes.
    * <FIXME author="Horia Chiorean" description="Check if the product names should come from an i18n file">
    */
   static {
      PRODUCT_CODES_DESCRIPTION = new HashMap();
      PRODUCT_CODES_DESCRIPTION.put(OpProjectConstants.BASIC_EDITION_CODE, "Onepoint Project Basic Edition");
      PRODUCT_CODES_DESCRIPTION.put(OpProjectConstants.PROFESSIONAL_EDITION_CODE, "Onepoint Project Professional Edition");
      PRODUCT_CODES_DESCRIPTION.put(OpProjectConstants.OPEN_EDITION_CODE, "Onepoint Project Open Edition");
      PRODUCT_CODES_DESCRIPTION.put(OpProjectConstants.TEAM_EDITION_CODE, "Onepoint Project Team Edition");
      PRODUCT_CODES_DESCRIPTION.put(OpProjectConstants.ON_DEMAND_EDITION_CODE, "Onepoint Project On Demand Edition");
   }

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      Map versionsMap = Collections.EMPTY_MAP;
      try {
         URL versionUrl = new URL(PRODUCT_VERSIONS_URL);
         InputStream inputStream = versionUrl.openStream();
         versionsMap = getProductVersionsMap(inputStream);
         inputStream.close();
      }
      catch (MalformedURLException e) {
         logger.error("Cannot open the connection to get the product information because:" + e.getMessage(), e);
      }
      catch (IOException e) {
         logger.error("Cannot open the connection to get the product information because:" + e.getMessage(), e);
      }
      String productCode = OpEnvironmentManager.getProductCode();
      String currentVersion = (String) versionsMap.get(productCode);
      if (currentVersion == null) {
         currentVersion = UNKNOWN_VERSION_NUMBER;
      }
      String productName = (String) PRODUCT_CODES_DESCRIPTION.get(productCode);

      form.findComponent(PRODUCT_NAME_LABEL).setText(productName);
      
      String version = null;
      Date build = null;
      // try reading infos from manifest
      try {
         URL url = OpAboutFormProvider.class.getResource("");
         JarURLConnection jconn = (JarURLConnection) url.openConnection();
         Manifest mf = jconn.getManifest();
         Attributes attr = mf.getAttributes("Implementation");
         if (attr != null) {
            version = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
         }
         attr = mf.getMainAttributes();
         if (attr != null) {
            String buildString = attr.getValue("Build-Date");
            if (buildString != null) {
               SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
               try {
                  build = df.parse(buildString);
               }
               catch (ParseException exc) {
               }
            }
         }
      }
      catch (IOException exc) {
      }
      catch (ClassCastException exc) {
      }
      if (version == null) {
         version = OpProjectConstants.CODE_VERSION_NUMBER;
      }
      if (build == null) {
         build = new Date();
      }
      form.findComponent(VERSION_LABEL).setText(version);
      String dateText = ((OpProjectSession) session).getCalendar().localizedDateToString(
            new java.sql.Date(build.getTime()));
      form.findComponent(BUILD_LABEL).setText(dateText);
      form.findComponent(CURRENT_VERSION_LABEL).setText(currentVersion);
   }

   /**
    * Gets the map of product versions information from the given input.
    *
    * @param versionsInputStream a <code>InputStream</code> used for obtaining the latest product version information.
    * @return map of values a <code>Map</code> of [String, String] representing product code, latest version pairs.
    */
   private Map getProductVersionsMap(InputStream versionsInputStream) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      Map valueMap = new HashMap();
      try {
         DocumentBuilder db = dbf.newDocumentBuilder();
         Document doc = db.parse(versionsInputStream);
         NodeList products = doc.getElementsByTagName(PRODUCT);
         for (int i = 0; i < products.getLength(); i++) {
            Node node = products.item(i);
            NamedNodeMap map = node.getAttributes();
            Node version = map.getNamedItem(VERSION);
            Node name = map.getNamedItem(CODE);
            if (name != null && version != null) {
               String versionValue = version.getNodeValue();
               String nameValue = name.getNodeValue();
               valueMap.put(nameValue, versionValue);
            }
         }
      }
      catch (Exception e) {
         logger.error("Cannot read product information versions from the given input stream because:" + e.getMessage(), e);
      }
      return valueMap;
   }
}
