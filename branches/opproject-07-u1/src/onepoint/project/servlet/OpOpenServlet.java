package onepoint.project.servlet;

import onepoint.express.servlet.XExpressServlet;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocalizer;
import onepoint.service.XMessage;
import onepoint.service.server.XSession;
import onepoint.util.XEncodingHelper;
import onepoint.util.XEnvironmentManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class OpOpenServlet extends XExpressServlet {

   /**
    * Application constants.
    */
   private final static String WEBPAGEICON = "opp_windows.ico";
   private final static String DEFAULT_CONTEXT_PATH = "opproject";

   /**
    * Applet parameters map keys
    */
   private final static Integer ID_INDEX = new Integer(0);
   private final static Integer NAME_INDEX = new Integer(1);
   private final static Integer CODEBASE_INDEX = new Integer(2);
   private final static Integer CODE_INDEX = new Integer(3);
   private final static Integer ARCHIVE_INDEX = new Integer(4);
   private final static Integer OTHER_PARAMETERS_INDEX = new Integer(5);


   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpOpenServlet.class, true);

   /**
    * Servlet init parameters.
    */
   private static String appletCodebase;
   private static String appletArchive;
   private static String imagesPath;
   private static String servletContextPath;
   private static String htmlTitle;

   /**
    * String that indicates whether secure comunication should be used (https)
    */
   private static String secureService = null;
   private static final String PARAMETERS_ARGUMENT = "parameters";
   private static final String MAIN_ERROR_MAP_ID = "main.error";
   private static final String CONTENT_ID_PARAM = "contentId";
   private static final String FILENAME_PARAM = "filename";
   private static final String FILE_PARAM = "file";
   private static final String INSUFICIENT_VIEW_PERMISSIONS = "{$InsuficientViewPermissions}";
   private static final String INVALID_SESSION = "{$InvalidSession}";
   private static final String INVALID_FILE_URL = "{$InvalidFileURL}";
   private static final String TEXT_HTML_CONTENT_TYPE = "text/html";

   private String projectHome = null;

   /**
    * Initializes the path considered to be the home of the application.
    */
   protected void initProjectHome() {
      if (projectHome == null) {
         projectHome = getServletConfig().getInitParameter("onepoint_home");
         if (projectHome == null) {
            ServletContext context = getServletConfig().getServletContext();
            projectHome = context.getRealPath("");
         }
         OpEnvironmentManager.setOnePointHome(projectHome);
      }
   }

   /**
    * @see onepoint.express.servlet.XExpressServlet#onInit()
    */
   public void onInit()
        throws ServletException {
      super.onInit();

      //initialize the path to the projec home
      initProjectHome();

      // override eXpress session class
      getServer().setSessionClass(OpProjectSession.class);

      // setup context path
      initContextPath();

      appletCodebase = servletContextPath + "/" + getServletConfig().getInitParameter("applet_codebase") + "/";
      appletArchive = getServletConfig().getInitParameter("applet_archive");
      imagesPath = servletContextPath + "/" + getServletConfig().getInitParameter("webimages_path") + "/";
      htmlTitle = getServletConfig().getInitParameter("html_title");

      //perform the initialization
      OpInitializer.init(this.getProductCode());

      //initialize the security feature
      String secure = OpInitializer.getConfiguration() != null ? OpInitializer.getConfiguration().getSecureService()
           : null;
      secureService = (secure != null) ? secure : Boolean.FALSE.toString();
   }

   /**
    * Initializes the servlet context path
    */
   private void initContextPath() {
      //first check to see whether an explicit value was given via web.xml
      String contextPathParameter = getServletConfig().getInitParameter("context_path");
      if (contextPathParameter != null && contextPathParameter.trim().length() > 0) {
         servletContextPath = contextPathParameter;
         return;
      }
      //if not, attempt to load the value via the real path of the servlet context.
      String contextPath = this.getServletContext().getRealPath("");
      if (contextPath == null) {
         servletContextPath = DEFAULT_CONTEXT_PATH;
         return;
      }
      String separator = XEnvironmentManager.FILE_SEPARATOR;
      int separatorIndex = contextPath.lastIndexOf(separator);
      if (separatorIndex != -1) {
         servletContextPath = contextPath.substring(contextPath.lastIndexOf(separator) + separator.length());
      }
      else {
         servletContextPath = contextPath;
      }
   }

   public void doGet(HttpServletRequest http_request, HttpServletResponse http_response)
        throws ServletException,
        IOException {
      //don't cache anything for more than 1 sec (posible security issue).
      http_response.setHeader("Cache-Control", "max-age=1");

      // Get the session context ('true': create new session if necessary)
      HttpSession http_session = http_request.getSession(true);
      OpProjectSession session = (OpProjectSession) getSession(http_session);

      //there must be a user on the session for a file request to succed.
      if (isFileRequest(http_request) && session.isEmpty()) {
         generateErrorPage(http_response, INVALID_SESSION, session);
         return;
      }

      //search for content ids
      String contentId = http_request.getParameter(CONTENT_ID_PARAM);
      if (contentId != null && !contentId.trim().equals("")) {
         String contentUrl = readParameter(http_request, FILENAME_PARAM);

         if (contentUrl != null) {
            if (XEncodingHelper.isValueEncoded(contentUrl)) {
               contentUrl = XEncodingHelper.decodeValue(contentUrl);
            }
            else {
               generateErrorPage(http_response, INVALID_FILE_URL, session);
               return;
            }
         }

         generateContentPage(contentId, contentUrl, http_response, session);
         return;
      }

      //search for any files which need to be uploaded (e.g reports)
      String filePath = readParameter(http_request, FILE_PARAM);
      if (filePath != null && !filePath.trim().equals("")) {
         if (XEncodingHelper.isValueEncoded(filePath)) {
            generateFilePage(XEncodingHelper.decodeValue(filePath), http_response);
            return;
         }
         else {
            generateErrorPage(http_response, INVALID_FILE_URL, session);
            return;
         }
      }

      //by default generate the applet page
      http_response.setContentType(TEXT_HTML_CONTENT_TYPE);
      generateAppletPage(http_response.getOutputStream(), http_request);
   }

   /**
    * Checks if the given request is a file request.
    *
    * @param request a <code>HttpServletRequest</code>.
    * @return <code>true</coed> if the request is a request for a file.
    */
   private boolean isFileRequest(HttpServletRequest request) {
      return readParameter(request, FILE_PARAM) != null || request.getParameter(CONTENT_ID_PARAM) != null;
   }

   /**
    * Reads those paramters from requests whcih might have values longer than 255 chars. Values of those
    * paramters were splitted in chuncks.
    *
    * @param request         a <code>HttpServletRequest</code>.
    * @param parameterPrefix parameter prefix to look for.
    * @return value for that parameter.
    */
   private String readParameter(HttpServletRequest request, String parameterPrefix) {
      StringBuffer buff = new StringBuffer();
      String chunk = null;

      int counter = 0;
      while ((chunk = request.getParameter(parameterPrefix + counter)) != null) {
         buff.append(chunk);
         counter++;
      }

      return buff.length() > 0 ? buff.toString() : null;
   }

   /**
    * Generates the default response which contains the application applet.
    *
    * @param sout    a <code>ServletOutputStream</code> where the output will be written.
    * @param request a <code>HttpServletRequest</code> representing the current request.
    */
   private void generateAppletPage(ServletOutputStream sout, HttpServletRequest request) {
      PrintStream out = new PrintStream(sout);
      out.println("<html>");
      out.println("<head>");
      out.println("<title>" + htmlTitle + "</title>");
      //out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
      out.println(getIconString(request));
      out.println("</head>");
      out.println("<body bgcolor=\"#ffffff\" onResize=\"resize()\"onLoad=\"resize()\" topmargin=\"0\" leftmargin=\"0\" marginwidth=\"0\" marginheight=\"0\">");

      Map appletParameters = createAppletParameters(request);
      generateAppletJS(out, appletParameters);
      out.println("</applet>");
      out.println("</body>");
      out.println("</html>");

      out.flush();
      out.close();
   }

   /**
    * Creates a map of applet parameters.
    * @param request  a <code>HttpRequest</code> representing the client request.
    * @return a <code>Map</code> of applet parameters.
    */
   private Map createAppletParameters(HttpServletRequest request) {
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
      String contextName = getServletConfig().getServletContext().getServletContextName();
      String codebase = urlBase(request).concat(appletCodebase);
      String sessionTimeoutSecs = String.valueOf(request.getSession().getMaxInactiveInterval());

      Map appletParams = new HashMap();
      appletParams.put(NAME_INDEX, contextName);
      appletParams.put(ID_INDEX, "onepoint");
      appletParams.put(CODE_INDEX,  getAppletClassName());
      appletParams.put(CODEBASE_INDEX,  codebase);
      appletParams.put(ARCHIVE_INDEX, appletArchive);

      Map otherAppletParams = new HashMap();
      otherAppletParams.put("host", request.getServerName());
      otherAppletParams.put("port", String.valueOf(request.getServerPort()));
      otherAppletParams.put("path",  "/" + servletContextPath + "/service");
      otherAppletParams.put("start-form",  start_form);
      otherAppletParams.put("secure-service",  secureService);
      otherAppletParams.put("session-timeout",  sessionTimeoutSecs);
      otherAppletParams.put(OpProjectConstants.DB_CONNECTION_CODE,  String.valueOf(OpInitializer.getConnectionTestCode()));
      otherAppletParams.put(OpProjectConstants.RUN_LEVEL,  String.valueOf(OpInitializer.getRunLevel()));

      String parameterNames = request.getParameter(PARAMETERS_ARGUMENT);
      if (parameterNames != null) {
         otherAppletParams.put("parameters", parameterNames);
         try {
            parameterNames = URLDecoder.decode(parameterNames, "UTF-8");
         }
         catch (UnsupportedEncodingException e) {
            logger.warn("Could not decode start_form for applet: " + e);
         }

         StringTokenizer st = new StringTokenizer(parameterNames, ";");
         while (st.hasMoreTokens()) {
            String parameter = st.nextToken();
            String val = parameter;
            int idx = parameter.indexOf(":");
            if (idx != -1) {
               val = parameter.substring(0, idx);
            }
            otherAppletParams.put(val, request.getParameter(val));
         }
      }
      appletParams.put(OTHER_PARAMETERS_INDEX, otherAppletParams);
      return appletParams;
   }

   protected String getAppletClassName() {
      return "onepoint.project.applet.OpOpenApplet.class";
   }

   /**
    * Gets the base url for the given request.
    *
    * @param request a <code>HttpServletRequest</code> object.
    * @return a <code>String</code> representing the base url of the request.
    */
   private String urlBase(HttpServletRequest request) {
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/";
   }

   /**
    * Return a string that contains a path to an icon that will displayed for the application.
    *
    * @param request a <code>HttpServletRequest</code> object.
    * @return a <code>String</code> representing an html snippet of code.
    */
   private String getIconString(HttpServletRequest request) {
      return "<link rel=\"SHORTCUT ICON\" href=\"" + urlBase(request).concat(imagesPath).concat(WEBPAGEICON) + "\" type=\"image/ico\" />";
   }

   /**
    * Generates the server response in the case when a persisted contentId is requested.
    *
    * @param contentId     a <code>String</code> representing a content id.
    * @param contentUrl    a <code>String</code> representing the url of the content.
    * @param http_response a <code>HttpServletResponse</code> representing the server response.
    * @param session       a <code>OpProjectSession</code> object representing the server session.
    */
   private void generateContentPage(String contentId, String contentUrl, HttpServletResponse http_response, OpProjectSession session) {

      OpBroker broker = ((OpProjectSession) session).newBroker();
      OpTransaction t = broker.newTransaction();

      try {
         OpContent cnt = (OpContent) broker.getObject(contentId);
         if (cnt != null) {
            //check access level
            if (!hasContentPermissions(session, broker, cnt)) {
               try {
                  PrintStream ps = new PrintStream(http_response.getOutputStream());
                  generateErrorPage(ps, INSUFICIENT_VIEW_PERMISSIONS, session);
                  ps.flush();
                  ps.close();
               }
               catch (IOException e) {
                  logger.error("Cannot generate error response", e);
               }
               return;
            }

            byte[] content = null;

            content = cnt.getBytes();
            String mimeType = cnt.getMediaType();
            http_response.setContentType(mimeType);
            if (contentUrl != null) {
               setContentDisposition(http_response, contentUrl);
            }
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
      }
      finally {
         t.commit();
         broker.close();
      }
   }

   /**
    * Generates a response from the server when a file is requested.
    *
    * @param filePath     a <code>String</code> representing the path to a file in an <code>URL</code> format.
    * @param httpResponse a <code>HttpServletResponse</code> object representing the response.
    */
   public void generateFilePage(String filePath, HttpServletResponse httpResponse) {

      String name = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
      String mimeType = OpContentManager.getFileMimeType(name);
      httpResponse.setContentType(mimeType);
      setContentDisposition(httpResponse, filePath);

      byte[] buffer = new byte[1024];
      try {
         OutputStream stream = httpResponse.getOutputStream();
         InputStream is = new URL(filePath).openStream();
         int length = is.read(buffer);
         while (length != -1) {
            stream.write(buffer, 0, length);
            length = is.read(buffer);
         }
         is.close();
      }
      catch (IOException e) {
         logger.error("Cannot open url to file" + filePath, e);
      }
   }

   /**
    * Sets the content disposition for the http response.
    *
    * @param httpResponse a <code>HttpServletResponse</code> object.
    * @param fileName     a <code>String</code> representing a name of a file.
    */
   private void setContentDisposition(HttpServletResponse httpResponse, String fileName) {
      if (fileName != null && fileName.indexOf("/") != -1) {
         fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
      }

      httpResponse.setHeader("Content-Disposition", "inline;filename=" + fileName);
   }


   /**
    * Prints to the output stream the java script function for resizing the applet.
    *
    * @param out a <code>PrintStream</code>
    */
   private void generateAppletResizeFunction(PrintStream out) {
      out.println("function resize() {\n" +
           "                if (navigator.appName.indexOf(\"Microsoft\") != -1) {\n" +
           "                    width = document.body.clientWidth; \n" +
           "                    height = document.body.clientHeight - 4;\n" +
           "                } " +
           "               else {\n" +
           "                    width = window.innerWidth; \n" +
           "                    height = window.innerHeight; \n" +
           "                }\n" +
           "                document.getElementById(\"onepoint\").width = width;\n" +
           "                document.getElementById(\"onepoint\").height = height;\n" +
           "                window.scroll(0,0);\n" +
           "            }\n" +
           "            window.onResize = resize;\n" +
           "            window.onLoad = resize;\n");
   }

   /**
    * Generates a JS function that will be used to render the applet.
    *
    * @param out a <code>PrintStream</code> were the result will be outputed to.
    * @param params a <codde>Map</code> representing the applet parameters.
    */
   private void generateAppletMainFunction(PrintStream out, Map params) {
      String name = (String) params.get(NAME_INDEX);
      String id = (String) params.get(ID_INDEX);
      String codebase = (String) params.get(CODEBASE_INDEX);
      String code = (String) params.get(CODE_INDEX);
      String archive = (String) params.get(ARCHIVE_INDEX);
      Map otherParams = (Map) params.get(OTHER_PARAMETERS_INDEX);

      StringBuffer buffer = new StringBuffer();
      buffer.append("function getAppletHTML() {\n");
      buffer.append(" var nsplugin = \"http://java.sun.com/j2se/1.4.2/\";\n");
      buffer.append("	var strArr = [];\n");
      buffer.append("	var info = navigator.userAgent;\n");
      buffer.append("	var ns = (info.indexOf(\"Mozilla\") >= 0) && (info.indexOf(\"Netscape\") >= 0) && (info.indexOf(\"Gecko\") >= 0);\n");
      buffer.append("	var moz = (info.indexOf(\"Mozilla\") >= 0) && (info.indexOf(\"Gecko\") >= 0);\n");
      buffer.append("  var ie = (info.indexOf(\"MSIE\") > 0);\n");
      buffer.append("	if (ie) {\n");
      buffer.append(" strArr.push(\"<object name=\\\"" + name + "\\\" classid=\\\"clsid:8AD9C840-044E-11D1-B3E9-00805F499D93\\\" width=\\\"100%\\\" height=\\\"100%\\\"\");\n");
      buffer.append(" strArr.push(\" id=\\\"" + id + "\\\"\");\n");
      buffer.append("	strArr.push(\" codebase=\\\"http://java.sun.com/products/plugin/autodl/jinstall-1_4_2-windows-i586.cab#Version=1,4,2,0\\\">\");\n");
      buffer.append(" 	strArr.push(\"<param name=\\\"codebase\\\" value=\\\"" + codebase + "\\\">\");\n");
      buffer.append(" strArr.push(\"<param name=\\\"code\\\" value=\\\"" + code + "\\\">\");\n");
      buffer.append(" strArr.push(\"<param name=\\\"archive\\\" value=\\\"" + archive + "\\\">\");\n");
      buffer.append(" strArr.push(\"<param name=\\\"type\\\" value=\\\"application/x-java-applet;version=1.4.2\\\">\");\n");
      buffer.append("	strArr.push(\"<param name=\\\"mayscript\\\" value=\\\"true\\\">\");\n");
      buffer.append(" 	strArr.push(\"<param name=\\\"scriptable\\\" value=\\\"true\\\">\");\n");
      for (Iterator it = otherParams.keySet().iterator(); it.hasNext();) {
         String paramName = (String) it.next();
         String paramValue = otherParams.get(paramName).toString();
         buffer.append(" strArr.push(\"<param name=\\\"" + paramName + "\\\" value=\\\"" + paramValue + "\\\">\");\n");
      }
      buffer.append("strArr.push(\"</object>\");\n");
      buffer.append(" }\n");
      buffer.append("	else {\n");
      buffer.append("	if (moz || ns) {\n");
      buffer.append("  strArr.push(\"<embed type=\\\"application/x-java-applet;version=1.4.2\\\" pluginspage=\\\"http://java.sun.com/j2se/1.4.2\\\" codebase=\\\"" + codebase + "\\\" code=\\\"" + code + "\\\" archive=\\\"" + archive + "\\\"" +
           " width=\\\"100%\\\" height=\\\"100%\\\" mayscript=\\\"true\\\" name=\\\"" + name + "\\\" \");\n");
      buffer.append(" strArr.push(\" id=\\\"" + id + "\\\"\");\n");
      for (Iterator it = otherParams.keySet().iterator(); it.hasNext();) {
         String paramName = (String) it.next();
         String paramValue = otherParams.get(paramName).toString();
         buffer.append(" strArr.push(\"" + paramName + "=\\\"" + paramValue + "\\\"\");\n");
      }
      buffer.append(" strArr.push(\"<noembed><span>Java is not installed on your machine. Please install it.</span></noembed></embed>\");\n");
      buffer.append(" } \n");
      buffer.append(" 	else {\n");
      buffer.append("	strArr.push(\"You have the wrong browser.\");\n");
      buffer.append(" } \n");
      buffer.append(" } \n");
      buffer.append("	return strArr.join(\" \");");
      buffer.append("}\n");

      out.println(buffer.toString());
   }

   /**
    * Writes a response representing the JavaScript code that will generate the applet.
    * @param out a <code>PrintStream</code> where the response will be written to.
    * @param params a <code>Map</code> representing applet specific parameters.
    */
   private void generateAppletJS(PrintStream out, Map params) {
      out.println("<script language=\"JavaScript\">\n");
      out.println(" \n");
      this.generateAppletMainFunction(out, params);
      out.println(" \n");
      this.generateAppletResizeFunction(out);
      out.println(" \n");
      out.println("document.write(getAppletHTML())\n");
      out.println("</script>");
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

   /**
    * Generates an error page, as a user response to some action.
    *
    * @param http_response a <code>HttpServletResponse</code> http response.
    * @param errorMessage  a <code>String</code> representing an error message to display. The errorMessage tries to be
    *                      i18ned from the main language res file.
    * @param session       a <code>OpProjectSession</code> representing the application user session.
    * @throws  IOException if the response cannot be written.
    */
   private void generateErrorPage(HttpServletResponse http_response, String errorMessage, OpProjectSession session)
        throws IOException {
      http_response.setContentType(TEXT_HTML_CONTENT_TYPE);
      PrintStream ps = new PrintStream(http_response.getOutputStream());
      generateErrorPage(ps, errorMessage, session);
      ps.flush();
      ps.close();
   }

   /**
    * Generates an error page, as a user response to some action.
    *
    * @param out          a <code>PrintStream</code> representing the output stream on which the server response is written.
    * @param errorMessage a <code>String</code> representing an error message to display. The errorMessage tries to be
    *                     i18ned from the main language res file.
    * @param session      a <code>OpProjectSession</code> representing the application user session.
    */
   private void generateErrorPage(PrintStream out, String errorMessage, OpProjectSession session) {
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(MAIN_ERROR_MAP_ID));
      String i18nErrorMessage = localizer.localize(errorMessage);
      out.println("<html>");
      out.println("<head><title> " + htmlTitle + " Error </title></head>");
      out.print("<body><h1><font color=\"red\">");
      out.print(i18nErrorMessage);
      out.println("</font></h1></body>");
      out.print("</html>");
   }

   /**
    * Checks if the current logged in user has at least view-permissions of the given content.
    *
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param content a <code>OpContent</code> object representing the content the user is trying to view.
    * @param broker  a <code>OpBroker</code> used for performing business operations.
    * @return <code>true</code> if the user has rights to view the content, false otherwise.
    *         <FIXME author="Horia Chiorean" description="Is is safe to assume that it's enough to have permissions on at least 1 of the entities referencing the content ?">
    */
   private boolean hasContentPermissions(OpProjectSession session, OpBroker broker, OpContent content) {
      Set attachments = content.getAttachments();
      for (Iterator it = attachments.iterator(); it.hasNext();) {
         OpObject attachment = (OpObject) it.next();
         if (session.checkAccessLevel(broker, attachment.getID(), OpPermission.OBSERVER)) {
            return true;
         }
      }

      Set attachmentVersions = content.getAttachmentVersions();
      if (attachmentVersions != null) {
         for (Iterator it = attachmentVersions.iterator(); it.hasNext();) {
            OpObject attachmentVersion = (OpObject) it.next();
            if (session.checkAccessLevel(broker, attachmentVersion.getID(), OpPermission.OBSERVER)) {
               return true;
            }
         }
      }

      Set documents = content.getDocuments();
      for (Iterator it = documents.iterator(); it.hasNext();) {
         OpObject document = (OpObject) it.next();
         if (session.checkAccessLevel(broker, document.getID(), OpPermission.OBSERVER)) {
            return true;
         }
      }
      return false;
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
