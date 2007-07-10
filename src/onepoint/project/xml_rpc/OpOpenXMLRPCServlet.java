/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 *
 */
package onepoint.project.xml_rpc;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.OpService;
import onepoint.project.OpInitializerFactory;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

/**
 * Servlet used for Xml-Rpc Funcionality.
 *
 * @author dfreis
 *
 */
public class OpOpenXMLRPCServlet extends XmlRpcServlet {

   /**
    * the serial version uid
    */
   private static final long serialVersionUID = 1L;

   /**
    * the logger
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpOpenXMLRPCServlet.class);

   /**
    * the key of the OpProjectSession session variable.
    */
   private static final String OP_PROJECT_SESSION = "opProjectSession";


   /**
    * the suffix appended to all handlers.
    */
   private static final String XMLRPC_SUFFIX = "XMLRPC";

   /**
    * Mapping holding all Xml-Rpc service implementations together with an unique key.
    */
   private static PropertyHandlerMapping handler_mapping = null;

   /**
    * Default constructor.
    */
   public OpOpenXMLRPCServlet() {
      super();
   }

   /**
    * Initializes the path considered to be the home of the application.
    */
   protected void initProjectHome() {
      String projectHome = getServletConfig().getInitParameter("onepoint_home");
      if (projectHome == null) {
         ServletContext context = getServletConfig().getServletContext();
         projectHome = context.getRealPath("");
      }
      OpEnvironmentManager.setOnePointHome(projectHome);
   }

   /* 
    * Initializes all handlers by looking them up according the service names.
    */
   private void initHandlers() {
      handler_mapping = new PropertyHandlerMapping();
      // create our Request Processor Factory Factory and pass our pobject instance.
      Iterator serviceIter = XServiceManager.iterator();
      Class xmlRpcServiceClass;
      OpService service;
      if (!serviceIter.hasNext()) {
         logger.error("no service found!");
      }

      while (serviceIter.hasNext()) {
         service = (OpService) (((XService) serviceIter.next()).getServiceImpl());
         xmlRpcServiceClass = getXMLRPCServiceClass(service);
         if (xmlRpcServiceClass != null) {
            try {
               logger.info("Registered service: "+xmlRpcServiceClass.getName());
               handler_mapping.addHandler(service.getName(), xmlRpcServiceClass);
            }
            catch (XmlRpcException exc) {
               logger.error("Could not add XML-RPC handler for service: "+service.getName());
               logger.debug(exc);
            }
            catch (NoClassDefFoundError exc) {
               System.err.println("EXC: "+exc);   
               exc.printStackTrace();
            }
         }
         else {
            logger.error("No XML-RPC handler for service '"+(service == null ? "<null>" : service.getName())+"' found!");            
         }
      }
   }

   /* Initializes this servlet by setting up the any OpProject required dettings.
    * @see javax.servlet.GenericServlet#init()
    */
   @Override
   public void init() throws ServletException {
      logger.debug("Call init() method");

      super.init();

      //initialize the path to the project home
      initProjectHome();

      //perform the initialization
      // FIXME how to init right edition? - should't that be read from license      
      OpInitializerFactory factory = OpInitializerFactory.getInstance();
      factory.setInitializer(OpInitializer.class);

      OpInitializer initializer = factory.getInitializer();
      initializer.init(this.getProductCode());

      // initialize the handlers
      initHandlers();
   }

   /* (non-Javadoc)
    * @see org.apache.xmlrpc.webserver.XmlRpcServlet#newXmlRpcHandlerMapping()
    */
   @Override
   protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {
      if (handler_mapping == null) {
         throw new IllegalStateException("handler not jet initialized");
      }
      return handler_mapping;
   }

   /* Sets the OpProjectSession as thread local and calls its super method.
    * @see org.apache.xmlrpc.webserver.XmlRpcServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
    */
   @Override
   public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
      try {
         OpProjectSession session = (OpProjectSession) request.getSession(true).getAttribute(OP_PROJECT_SESSION);
         if (session == null) {
            session = new OpProjectSession();
            request.getSession().setAttribute(OP_PROJECT_SESSION, session);
         }
         OpProjectSession.setSession(session);
         super.doPost(request, response);
         OpProjectSession.removeSession();
      } catch (Throwable t) {
         logger.error("XML-RPC: get Exception during doPost()", t);
      }
   }


//   /* (non-Javadoc)
//    * @see org.apache.xmlrpc.webserver.XmlRpcServlet#getXmlRpcServletServer()
//    */
//   @Override
//   public XmlRpcServletServer getXmlRpcServletServer() {
//      // TODO Auto-generated method stub
//      XmlRpcServletServer server = super.getXmlRpcServletServer();
//      
//   }

   protected PropertyHandlerMapping newPropertyHandlerMapping(URL url) throws IOException, XmlRpcException {
      PropertyHandlerMapping mapping = new PropertyHandlerMapping();
      RequestProcessorFactoryFactory factory = new RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory() {
         protected Object getRequestProcessor(Class pClass, XmlRpcRequest pRequest) throws XmlRpcException {
            Object proc = super.getRequestProcessor(pClass, pRequest);
            logger.debug("PROC: "+proc.getClass().getName());
            return proc;
         }
      };
      mapping.setRequestProcessorFactoryFactory(factory);
      mapping.load(Thread.currentThread().getContextClassLoader(), url);
      return mapping;
   }

   /**
    * Returns a XMLRPCService that uses the given service to perform its work.
    * An XMLRPCService can therefor be seen as an XML-RPC adapter for the given service.
    * @param service the service to get the XML-RPC adapter for.
    * @return the XML-RPC specific adapter or null if no such adapter was found.
    */
   private Class getXMLRPCServiceClass(OpService service) {
      if (service == null) { // paranoia
         return null;
      }
      try {
         String className = getClass().getPackage().getName()+"."+service.getClass().getName();
         // FIXME(dfreis Apr 17, 2007 1:48:18 PM) temporary code until
         // ServiceImpls become Service, prevents changing all XML-RPC adapter classes.
         // {
         if (className.endsWith("Impl"))
            className = className.substring(0, className.length()-4);
         // }
         className += XMLRPC_SUFFIX;
         return Class.forName(className);
      }
      catch (Exception exc)
      {
         logger.debug("No XMLRPC class found for Service '"+service.getClass().getName()+"'");
         //exc.printStackTrace();
         return null;
      }
   }
   /**
    * Gets a product code string for this servlet.
    *
    * @return a <code>String</code> representing a product code.
    */
   protected String getProductCode() {
      return OpProjectConstants.OPEN_EDITION_CODE;
   }
}
