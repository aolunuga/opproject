package onepoint.project.servlet;

import onepoint.express.servlet.XExpressServlet;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Map;
import java.util.StringTokenizer;

public class OpProjectServlet extends XExpressServlet {

   /**
    *  Application constants.
    */
   public final static String WEBPAGEICON = "opp_windows.ico";
   public final static String WAR_NAME = "opproject";

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpProjectServlet.class, true);

   /**
    * Servlet init parameters.
    */
   private static String appletCodebase;
   private static String appletArchive;
   private static String imagesPath;

   /**
    * String that indicates whether secure comunication should be used (https)
    */
   private static String secureService = null;
   private static final String PARAMETERS_ARGUMENT = "parameters";

   /**
    * @see onepoint.express.servlet.XExpressServlet#onInit()
    */
   public void onInit() {
      super.onInit();
      // Override eXpress session class
      getServer().setSessionClass(OpProjectSession.class);

      // Setup environment
      appletCodebase = getServletConfig().getInitParameter("applet_codebase");
      appletArchive = getServletConfig().getInitParameter("applet_archive");
      imagesPath = getServletConfig().getInitParameter("webimages_path");

      String project_home = getServletConfig().getInitParameter("onepoint_home");
      if (project_home == null) {
         ServletContext context = getServletConfig().getServletContext();
         project_home = context.getRealPath("");
      }
      Map initParams = OpInitializer.init(project_home);
      //initialize the security feature
      String secure = (String) initParams.remove(OpInitializer.SECURE_SERVICE);
      secureService = secure != null ? secure : "false";

      //<FIXME author="Horia Chiorean" description="Hack. Should not remain here.">
      OpProjectService.setRemote(true);
      //<FIXME>

   }


   public void doGet(HttpServletRequest http_request, HttpServletResponse http_response) throws ServletException,
        IOException {


      // Get the session context ('true': create new session if necessary)
      HttpSession http_session = http_request.getSession(true);
      XSession session = getSession(http_session);

      //search for attachments
      String attachment = http_request.getParameter("contentId");
      if (attachment != null && !attachment.trim().equals("")) {
         generateContentPage(attachment, http_response, session);
         return;
      }

      //search for any files which need to be uploaded (e.g reports)
      String filePath = http_request.getParameter("file");
      if (filePath != null && !filePath.trim().equals("")) {
         generateFilePage(filePath, http_response);
         return;
      }
      //by default generate the applet page
      http_response.setContentType("text/html");
      generateAppletPage(http_response.getOutputStream(), http_request);

   }


   /**
    * Generates the default response which contains the application applet.
    * @param sout a <code>ServletOutputStream</code> where the output will be written.
    * @param request a <code>HttpServletRequest</code> representing the current request.
    */
   private void generateAppletPage(ServletOutputStream sout, HttpServletRequest request) {
      PrintStream out = new PrintStream(sout);
      // parse query string
      String start_form = request.getParameter("start_form");
      if (start_form == null || start_form.equals("")) {
         start_form = OpProjectConstants.DEFAULT_START_FORM;
      }
      else {
         try {
            start_form = URLDecoder.decode(start_form, "UTF-8");
         }
         catch (UnsupportedEncodingException e) {
            logger.warn("Could not decode start_form for applet: " + e);
         }
      }

      out.println("<html>");
      out.println("<head>");
      out.println("<title>Onepoint Project</title>");
      //out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
      out.println(getIconString(request));
      out.println("</head>");
      out.println("<body bgcolor=\"#ffffff\" onResize=\"resize()\"onLoad=\"resize()\" topmargin=\"0\" leftmargin=\"0\" marginwidth=\"0\" marginheight=\"0\">");
      generateAppletResizeFunction(out);
      String codebase = urlBase(request).concat(appletCodebase);

      out
           .println("<applet id=\"onepoint\" name=\"Onepoint Project 06 Team Edition (Client Applet)\" width=\"100%\" height=\"100%\" code=\"onepoint.project.applet.OpProjectApplet.class\" codebase=\""
                + codebase + "\" archive=\"" + appletArchive + "\">");
      out.println("<param name=\"host\" value=\"" + request.getServerName() + "\">");
      out.println("<param name=\"port\" value=\"" + request.getServerPort() + "\">");
      out.println("<param name=\"path\" value=\"/"+WAR_NAME +"/service\">");
      out.println("<param name=\"start-form\" value=\"" + start_form + "\">");
      out.println("<param name=\"" + OpProjectConstants.RUN_LEVEL + "\" value=\"" + OpInitializer.getRunLevel() + "\">");
      out.println("<param name=\"secure-service\" value=\"" + secureService + "\">");
      String sessionTimeoutSecs = String.valueOf(request.getSession().getMaxInactiveInterval());
      out.println("<param name=\"session-timeout\" value=\"" + sessionTimeoutSecs + "\">");

      String parameter_names = request.getParameter(PARAMETERS_ARGUMENT);
      if (parameter_names != null) {
         out.println("<param name=\"parameters\" value=\"" + parameter_names + "\">");

         try {
            parameter_names = URLDecoder.decode(parameter_names, "UTF-8");
         }
         catch (UnsupportedEncodingException e) {
            logger.warn("Could not decode start_form for applet: " + e);
         }

         StringTokenizer st = new StringTokenizer(parameter_names, ";");
         while (st.hasMoreTokens()) {
            String parameter = st.nextToken();
            String val = parameter;
            int idx = parameter.indexOf(":");
            if (idx != -1) {
               val = parameter.substring(0, idx);
            }
            String parameter_value = request.getParameter(val);
            out.println("<param name=\"" + val + "\" value=\"" + parameter_value + "\">");
         }
      }

      out.println("</applet>");
      out.println("</body>");
      out.println("</html>");

      out.flush();
      out.close();
   }

   /**
    * Gets the base url for the given request.
    * @param request a <code>HttpServletRequest</code> object.
    * @return a <code>String</code> representing the base url of the request.
    */
   private String urlBase(HttpServletRequest request) {
     return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/";
   }

   /**
    * Return a string that contains a path to an icon that will displayed for the application.
    * @param request a <code>HttpServletRequest</code> object.
    * @return a <code>String</code> representing an html snippet of code.
    */
   private String getIconString(HttpServletRequest request) {
    return "<link rel=\"SHORTCUT ICON\" href=\""+urlBase(request).concat(imagesPath).concat(WEBPAGEICON)+"\" type=\"image/ico\" />";
   }

   /**
    * Generates the server response in the case when a persisted contentId is requested.
    * @param contentId a <code>String</code> representing a content id.
    * @param http_response a <code>HttpServletResponse</code> representing the server response.
    * @param s a <code>XSession</code> object representing the server session.
    */
   public void generateContentPage(String contentId, HttpServletResponse http_response, XSession s) {
      OpProjectSession session = (OpProjectSession) s;

      OpBroker broker = ((OpProjectSession) session).newBroker();
      OpTransaction t = broker.newTransaction();

      byte[] content = null;

      OpContent cnt = (OpContent) broker.getObject(contentId);

      if (cnt != null) {
         content = cnt.getBytes();
         String mimeType = cnt.getMediaType();
         http_response.setContentType(mimeType);
         try {
            OutputStream stream = http_response.getOutputStream();
            stream.write(content);
         }
         catch (IOException e) {
            logger.error("Cannot send contentId", e);
         }
      }
      else {
         http_response.setContentType("text/plain");
         try {
            PrintStream out = new PrintStream(http_response.getOutputStream());
            out.println("Content with id " + contentId + " could not be found");
            out.flush();
            out.close();
         }
         catch (IOException e) {
            logger.error(e);
         }
      }
      t.commit();
      broker.close();
   }

   /**
    * Generates a response from the server when a file is requested.
    *
    * @param filePath a <code>String</code> representing the path to a file in an <code>URL</code> format.
    * @param httpResponse a <code>HttpServletResponse</code> object representing the response.
    */
   public void generateFilePage(String filePath, HttpServletResponse httpResponse) {

      String name = filePath.substring(filePath.lastIndexOf("/"), filePath.length());
      byte[] buffer = new byte[1024];
      FileNameMap map = URLConnection.getFileNameMap();
      String mimeType = map.getContentTypeFor(name);
      httpResponse.setContentType(mimeType);

      try {
         OutputStream stream = httpResponse.getOutputStream();
         InputStream is = new URL(filePath).openStream();
         int length = is.read(buffer);
         while (length != -1) {
            stream.write(buffer);
            length = is.read(buffer);
         }
         is.close();
      }
      catch (IOException e) {
         logger.error("Cannot open url to file" + filePath, e);
      }
   }

   /**
    * Prints to the output stream the java script function for resizing the applet.
    *
    * @param out a <code>PrintStream</code>
    */
   private void generateAppletResizeFunction(PrintStream out) {
      out.println("<script language=\"JavaScript\">\n" +
           "            function resize() {\n" +
           "                if (navigator.appName.indexOf(\"Microsoft\") != -1) {\n" +
           "                    width = document.body.clientWidth; \n" +
           "                    height = document.body.clientHeight - 4;\n" +
           "                } else {\n" +
           "                    width = window.innerWidth; \n" +
           "                    height = window.innerHeight; \n" +
           "                }\n" +
           "                document.getElementById(\"onepoint\").width = width;\n" +
           "                document.getElementById(\"onepoint\").height = height;\n" +
           "                window.scroll(0,0);\n" +
           "            }\n" +
           "            window.onResize = resize;\n" +
           "            window.onLoad = resize;\n" +
           "        </script>");

   }


   protected XMessage processRequest(XMessage request, boolean sessionExpired, HttpServletRequest http_request, XSession session) {
      if (request.getAction().equalsIgnoreCase(OpProjectConstants.GET_RUN_LEVEL_ACTION)) {
         XMessage response = new XMessage();
         response.setArgument(OpProjectConstants.RUN_LEVEL, Byte.toString(OpInitializer.getRunLevel()));
         return response;
      }
      else {
         return super.processRequest(request, sessionExpired, http_request, session);
      }
   }
}
