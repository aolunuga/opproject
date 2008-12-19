package onepoint.project.modules.external_applications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpLanguageKitPath;
import onepoint.project.module.OpModule;
import onepoint.project.module.OpServiceFile;
import onepoint.project.modules.external_applications.generated.Applications;
import onepoint.project.modules.external_applications.generated.description.Application;
import onepoint.resource.XLanguageKit;
import onepoint.resource.XLocaleManager;
import onepoint.service.server.XServiceManager;

public class OpExternalApplicationsModule extends OpModule {

   public static String MODULE_NAME = null;
   
   private static final XLog logger = XLogFactory
         .getLogger(OpExternalApplicationsModule.class);

   private Map<String, OpExternalApplicationDescription> applicationRegistry = null;

   private static final String JAXB_CLASS_PACKAGE = "onepoint.project.modules.external_applications.generated";
   private static final String JAXB_DESC_CLASS_PACKAGE = "onepoint.project.modules.external_applications.generated.description";

   private static final String EXT_APPS = "onepoint/project/modules/external_applications/external_applications.aps.xml";

   protected String getExternalAppsFile() {
      return EXT_APPS;
   }
   
   public void start(OpProjectSession session) {
      // TODO Auto-generated method stub
      super.start(session);
      MODULE_NAME = this.getName(); // ;-)
      
      try {
         JAXBContext jc = JAXBContext.newInstance(JAXB_CLASS_PACKAGE);
         Unmarshaller unmarshaller = jc.createUnmarshaller();

         InputStream is = Thread.currentThread().getContextClassLoader()
               .getResourceAsStream(getExternalAppsFile());
         BufferedReader reader = new BufferedReader(new InputStreamReader(is));

         String response = new String();
         String line;
         while ((line = reader.readLine()) != null) {
            response += new String(line.getBytes(), "UTF-8");
            response += '\n';
         }
         is.close();
         Reader ris = new StringReader(response);
         Object res = null;
         res = unmarshaller.unmarshal(ris);
         ris.close();
         Applications app = (Applications) res;

         Iterator<Applications.Application> i = app.getApplication().iterator();
         while (i.hasNext()) {
            Applications.Application a = i.next();
            startApplication(session, a.getDescription());
         }

      } catch (JAXBException e) {
         e.printStackTrace();
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

   }

   private void startApplication(OpProjectSession session, String descrFile) {
      try {
         JAXBContext jc = JAXBContext.newInstance(JAXB_DESC_CLASS_PACKAGE);
         Unmarshaller unmarshaller = jc.createUnmarshaller();

         InputStream is = Thread.currentThread().getContextClassLoader()
               .getResourceAsStream(descrFile);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is));

         String response = new String();
         String line;
         while ((line = reader.readLine()) != null) {
            response += new String(line.getBytes(), "UTF-8");
            response += '\n';
         }
         is.close();
         Reader ris = new StringReader(response);
         Object res = null;
         res = unmarshaller.unmarshal(ris);
         ris.close();
         Application app = (Application) res;

         Class c = Class.forName(app.getMainClass().getName());

         OpExternalApplicationInstance extApp = (OpExternalApplicationInstance) c
               .newInstance();

         OpExternalApplicationDescription desc = new OpExternalApplicationDescription(app, extApp);
         registerApplication(desc);
                 
         extApp.start(session, desc);
         
         // load language kits:
         if (app.getLanguageKitPath() != null) {
            OpLanguageKitPath i18nPath = new OpLanguageKitPath();
            i18nPath.setPath(app.getLanguageKitPath().getPath());
            List<XLanguageKit> appKits = i18nPath.loadLanguageKits();
            Iterator<XLanguageKit> kit = appKits.iterator();
            while (kit.hasNext()) {
               XLocaleManager.registerLanguageKit(kit.next());
            }
         }
         // and services:
         if (app.getServiceFile() != null) {
            OpServiceFile serviceFile = new OpServiceFile();
            serviceFile.setFileName(app.getServiceFile().getFileName());
            XServiceManager.registerService(serviceFile.loadService());
         }
      } catch (JAXBException e) {
         logger.error("JAXB: " + e.getMessage());
      } catch (UnsupportedEncodingException e) {
         logger.error("Huh?: " + e.getMessage());
      } catch (IOException e) {
         logger.error("Cannot read: " + e.getMessage());
      } catch (ClassNotFoundException e) {
         logger.error("Class not found: " + e.getMessage());
      } catch (InstantiationException e) {
         logger.error("Cannot instantiate: " + e.getMessage());
      } catch (IllegalAccessException e) {
         logger.error("Illegal Access: " + e.getMessage());
      } catch (SecurityException e) {
         logger.error("Security: " + e.getMessage());
      } catch (IllegalArgumentException e) {
         logger.error("Illegal argument: " + e.getMessage());
      }

   }

   public void check(OpProjectSession session) {
      super.check(session);
      
      Iterator<OpExternalApplicationDescription> apps = getApplications();
      while (apps.hasNext()) {
         OpExternalApplicationDescription desc = apps.next();
         desc.getInstance().check(session);
      }
      
   }
   
   public boolean registerApplication(OpExternalApplicationDescription description) {
      if (applicationRegistry == null) {
         applicationRegistry = new HashMap<String, OpExternalApplicationDescription>();
      }
      return applicationRegistry.put(description.getApplicationType(), description) == null;
   }
   
   public Iterator<OpExternalApplicationDescription> getApplications() {
      if (applicationRegistry == null) {
         applicationRegistry = new HashMap<String, OpExternalApplicationDescription>();
      }
      return applicationRegistry.values().iterator();
   }
   
   public OpExternalApplicationDescription getApplicationDescription(String type) {
      if (applicationRegistry == null) {
         return null;
      }
      return applicationRegistry.get(type);
   }

}
