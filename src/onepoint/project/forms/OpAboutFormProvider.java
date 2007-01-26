/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.service.server.XSession;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Form provider for the about dialog.
 *
 * @author mihai.costin
 */
public class OpAboutFormProvider implements XFormProvider {

   private static final String PRODUCT_FIELD = "ProductName";
   private static final String VERSION_FIELD = "VersionNumber";

   private final static String FILE_NAME = "version.xml";
   private final static String VERSION = "version";
   private final static String CODE = "code";
   private final static String PRODUCT = "product";

   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      InputStream infoInput = getVersionInput();
      if (infoInput != null) {
         Map valueMap = getInfoMap(infoInput);

         form.findComponent(PRODUCT_FIELD).setStringValue((String) valueMap.get(CODE));
         form.findComponent(VERSION_FIELD).setStringValue((String) valueMap.get(VERSION));

      }
   }

   /**
    * Gets the map of values contained in the given XML format infoInput.
    *
    * @param infoInput input stream ver the XML information.
    * @return map of values
    */
   private Map getInfoMap(InputStream infoInput) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      Map valueMap = new HashMap();
      try {
         DocumentBuilder db = dbf.newDocumentBuilder();
         Document doc = db.parse(infoInput);
         NodeList products = doc.getElementsByTagName(PRODUCT);
         if (products.getLength() > 0) {
            Node node = products.item(0);
            NamedNodeMap map = node.getAttributes();
            Node version = map.getNamedItem(VERSION);
            Node name = map.getNamedItem(CODE);
            if (name != null && version != null) {
               String versionValue = version.getNodeValue();
               String nameValue = name.getNodeValue();
               valueMap.put(VERSION, versionValue);
               valueMap.put(CODE, nameValue);
            }
         }
      }
      catch (ParserConfigurationException e) {
         e.printStackTrace();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      catch (SAXException e) {
         e.printStackTrace();
      }
      return valueMap;
   }

   /**
    * Gets the XML input stream with the version info.
    *
    * @return an input stream.
    */
   private InputStream getVersionInput() {
      return OpAboutFormProvider.class.getResourceAsStream(FILE_NAME);
   }
}
