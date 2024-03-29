/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import onepoint.express.servlet.XExpressServlet;
import onepoint.express.util.XConstants;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocalizer;
import onepoint.service.XMessage;
import onepoint.service.XSizeInputStream;
import onepoint.service.server.XSession;
import onepoint.util.XBase64;
import onepoint.util.XCookieManager;
import onepoint.util.XEncodingHelper;
import onepoint.util.XEnvironmentManager;
import onepoint.util.XIOHelper;

public class OpOpenServlet extends XExpressServlet {

   /**
    * Path separator
    */
   protected static final String PATH_SEPARATOR = "/";

   /**
    * Application constants.
    */
   private final static String WEBPAGEICON = "opp_windows.ico";
   private final static String DEFAULT_CONTEXT_PATH = "opproject";

   /**
    * Applet parameters map keys
    */
   private final static Integer ID_INDEX = 0;
   private final static Integer NAME_INDEX = 1;
   private final static Integer CODEBASE_INDEX = 2;
   private final static Integer CODE_INDEX = 3;
   private final static Integer ARCHIVE_INDEX = 4;
   private final static Integer OTHER_PARAMETERS_INDEX = 5;


   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpOpenServlet.class);

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
   private static final String INSUFICIENT_VIEW_PERMISSIONS = "${InsuficientViewPermissions}";
   private static final String INVALID_SESSION = "${InvalidSession}";
   private static final String INVALID_FILE_URL = "${InvalidFileURL}";
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
   @Override
   public void onInit()
        throws ServletException {
      super.onInit();

      //initialize the path to the projec home
      initProjectHome();

      // override eXpress session class
      getServer().setSessionClass(OpProjectSession.class);

      // setup context path
      initContextPath();

      appletCodebase = servletContextPath + PATH_SEPARATOR + getInitParameter("applet_codebase") + PATH_SEPARATOR;
      appletArchive = getInitParameter("applet_archive");
      imagesPath = servletContextPath + PATH_SEPARATOR + getInitParameter("webimages_path") + PATH_SEPARATOR;
      htmlTitle = getInitParameter("html_title");

      //perform the initialization
      OpInitializer initializer = getProductIntializer();

      //initialize the security feature
      OpConfiguration config = initializer.getConfiguration();
      String secure = config != null ? config.getSecureService() : null;
      secureService = (secure != null) ? secure : Boolean.FALSE.toString();
   }

//   /**
//    * Returns an instance of <code>OpInitializer</code> that will be responsible for product initialization.
//    *
//    * @return initializer instance.
//    */

   protected OpInitializer getProductIntializer() {
      OpInitializerFactory factory = OpInitializerFactory.getInstance();
      OpInitializer initializer = factory.getInitializer();
      if (initializer == null) {
         initializer = factory.setInitializer(OpInitializer.class);
         initializer.init(getProductCode());
      }
      return initializer;
   }

   /**
    * Initializes the servlet context path
    */
   private void initContextPath() {
      //first check to see whether an explicit value was given via web.xml
      String contextPathParameter = getServletConfig().getInitParameter("context_path");
      if (contextPathParameter != null) {
         servletContextPath = contextPathParameter;
         logger.info("context path: "+servletContextPath);
         return;
      }

      //if not, attempt to load the value via the real path of the servlet context.
      String contextPath = this.getServletContext().getRealPath("");
      if (contextPath == null) {
         servletContextPath = DEFAULT_CONTEXT_PATH;
         logger.info("context path: "+servletContextPath);
         return;
      }
      int separatorIndex = contextPath.lastIndexOf(File.separator);
      if (separatorIndex != -1) {
         servletContextPath = contextPath.substring(contextPath.lastIndexOf(File.separator) + 1);
      }
      else {
         servletContextPath = contextPath;
      }
      logger.info("context path: "+servletContextPath);
   }

   @Override
   public void doGet(HttpServletRequest http_request, HttpServletResponse http_response)
        throws ServletException,
        IOException {

      // fix OPP-243 (MSIE bug: cannot view pdf files)
      if (!isFileRequest(http_request)) {
         http_response.setHeader("Cache-Control", "max-age=1");
//         http_response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
         http_response.setHeader("Pragma", "no-cache"); // HTTP 1.0
         http_response.setDateHeader("Expires", 0); //prevents caching at the proxy server
      }
      else {
         // don't cache anything for more than 1 sec (posible security issue).
         http_response.setHeader("Cache-Control", "max-age=1");
      }

      // Get the session context ('true': create new session if necessary)
      OpProjectSession session = (OpProjectSession) getSession(http_request);

      //there must be a user on the session for a file request to succed.
      if (isFileRequest(http_request) && session.isEmpty()) {
         generateErrorPage(http_response, INVALID_SESSION, session);
         return;
      }

      //search for content ids
      String contentId = http_request.getParameter(CONTENT_ID_PARAM);
      if (contentId != null && contentId.trim().length() > 0) {
         String contentUrl = readParameter(http_request, FILENAME_PARAM);
         if (contentUrl != null) {
            if (XEncodingHelper.isValueEncoded(contentUrl)) {
               contentUrl = XEncodingHelper.decodeValue(contentUrl);
            }
         }

         generateContentPage(contentId, contentUrl, http_response, session);
         return;
      }

      //search for any files which need to be uploaded (e.g reports)
      String encFile = readParameter(http_request, FILE_PARAM);
      if (encFile != null && encFile.trim().length() > 0) {
         if (XEncodingHelper.isValueEncoded(encFile)) {
            String fileName = XEncodingHelper.decodeValue(encFile);
            String filePath = new File(XEnvironmentManager.TMP_DIR, fileName).getAbsolutePath();
            generateFilePage(filePath, http_response);
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
      String chunk;

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
   protected void generateAppletPage(ServletOutputStream sout, HttpServletRequest request) {
      PrintStream out = new PrintStream(sout);
      out.println("<html>");
      this.generatePageHeader(request, out);
      out.println("<body bgcolor=\"#ffffff\" onResize=\"resize()\"onLoad=\"resize()\" topmargin=\"0\" leftmargin=\"0\" marginwidth=\"0\" marginheight=\"0\">");

      Map<Integer, Object> appletParameters = this.createAppletParameters(request);
      this.generateAppletJS(out, appletParameters);
      out.println("</body>");
      out.println("</html>");

      out.flush();
      out.close();
   }

   /**
    * Generates the header of the applet page.
    *
    * @param request a <code>HttpServletRequest</code> the client HTTP request.
    * @param out     a <code>PrintStream</code> used to write the response onto.
    */
   protected void generatePageHeader(HttpServletRequest request, PrintStream out) {
      out.println("<head>");
      out.println("<title>" + htmlTitle + "</title>");
      //out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
      out.println(getIconString(request));
      out.println("</head>");
   }

   /**
    * Creates a map of applet parameters.
    *
    * @param request a <code>HttpRequest</code> representing the client request.
    * @return a <code>Map</code> of applet parameters.
    */
   private Map<Integer, Object> createAppletParameters(HttpServletRequest request) {
      String contextName = getServletConfig().getServletContext().getServletContextName();
      String codebase = urlBase(request).concat(appletCodebase);
      String sessionTimeoutSecs = String.valueOf(request.getSession().getMaxInactiveInterval());

      Map<Integer, Object> appletParams = new HashMap<Integer, Object>();
      appletParams.put(NAME_INDEX, contextName);
      appletParams.put(ID_INDEX, "onepoint");
      appletParams.put(CODE_INDEX, getAppletClassName());
      appletParams.put(CODEBASE_INDEX, codebase);
      appletParams.put(ARCHIVE_INDEX, appletArchive);

      Map<String, Object> otherAppletParams = new HashMap<String, Object>();
      otherAppletParams.put("host", request.getServerName());
      otherAppletParams.put("port", String.valueOf(request.getServerPort()));
      otherAppletParams.put("path", generatePath(request));
      otherAppletParams.put("secure-service", Boolean.toString(request.isSecure()));
      otherAppletParams.put("session-timeout", sessionTimeoutSecs);

      OpInitializer initializer = OpInitializerFactory.getInstance().getInitializer();
      Map<String, Object> params = initializer.getInitParams();
      otherAppletParams.put(OpProjectConstants.RUN_LEVEL, String.valueOf(initializer.getRunLevel()));
      otherAppletParams.put(OpProjectConstants.START_FORM, params.get(OpProjectConstants.START_FORM));
      otherAppletParams.put(OpProjectConstants.START_FORM_PARAMETERS, params.get(OpProjectConstants.START_FORM_PARAMETERS));
      otherAppletParams.put(OpProjectConstants.AUTO_LOGIN_START_FORM, initializer.getAutoLoginStartForm());

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

   /**
    * Generates a string which will represent the URL to which the client will issue requests
    * when performing any operation.
    *
    * @param request a <code>HttpServletRequest</code>.
    * @return a <code>String</code> representing a request URL.
    */
   protected String generatePath(HttpServletRequest request) {
      return PATH_SEPARATOR + servletContextPath + request.getServletPath();
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
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + PATH_SEPARATOR;
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

      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();

      try {
         XSession.setSession(session);

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

            InputStream content = cnt.getStream();
            String mimeType = cnt.getMediaType();
            http_response.setContentType(mimeType);
            if (contentUrl != null) {
               setContentDisposition(http_response, contentUrl);
            }
            try {
               OutputStream stream = http_response.getOutputStream();
               XIOHelper.copy(content, stream);
            }
            catch (IOException e) {
               logger.error("Cannot send contentId", e);
            }
            finally {
               if (content != null) {
                  try {
                     content.close();
                  }
                  catch (IOException e) {
                     logger.error("Cannot close content stream", e);
                  }
               }
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
         XSession.removeSession();

         t.commit();
         broker.close();
      }
   }

   /**
    * Generates a response from the server when a file is requested.
    *
    * @param filePath     a <code>String</code> representing the full path to a file.
    * @param httpResponse a <code>HttpServletResponse</code> object representing the response.
    */
   public void generateFilePage(String filePath, HttpServletResponse httpResponse) {

      String name = filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.length());
      String mimeType = OpContentManager.getFileMimeType(name);
      httpResponse.setContentType(mimeType);
      setContentDisposition(httpResponse, filePath);

      byte[] buffer = new byte[1024];
      try {
         OutputStream stream = httpResponse.getOutputStream();
         InputStream is = new FileInputStream(filePath);
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
      if (fileName == null || fileName.length() == 0) {
         fileName = "NewFile";
      }
      if (fileName.indexOf(File.separator) != -1) {
         fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
      }
      fileName = fileName.replaceAll("[^-._a-zA-Z0-9]", "_");

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
    * @param out    a <code>PrintStream</code> were the result will be outputed to.
    * @param params a <codde>Map</code> representing the applet parameters.
    */
   private void generateAppletMainFunction(PrintStream out, Map<Integer, Object> params) {
      String name = (String) params.get(NAME_INDEX);
      String id = (String) params.get(ID_INDEX);
      String codebase = (String) params.get(CODEBASE_INDEX);
      String code = (String) params.get(CODE_INDEX);
      String archive = (String) params.get(ARCHIVE_INDEX);
      Map<String, String> otherParams = (Map<String, String>) params.get(OTHER_PARAMETERS_INDEX);

      StringBuffer buffer = new StringBuffer();
      buffer.append("function getAppletHTML() {\n");
      buffer.append(" var nsplugin = \"http://java.sun.com/j2se/1.4.2/\";\n");
      buffer.append("	var strArr = [];\n");
      buffer.append("	var info = navigator.userAgent;\n");
      buffer.append("	var ns = (info.indexOf(\"Mozilla\") >= 0) && (info.indexOf(\"Netscape\") >= 0) && (info.indexOf(\"Gecko\") >= 0);\n");
      buffer.append("	var moz = (info.indexOf(\"Mozilla\") >= 0) && (info.indexOf(\"Gecko\") >= 0);\n");
      buffer.append("  var ie = (info.indexOf(\"MSIE\") > 0);\n");
      buffer.append("	if (ie) {\n");
      buffer.append(" strArr.push(\"<object name=\\\"").append(name).append("\\\" classid=\\\"clsid:8AD9C840-044E-11D1-B3E9-00805F499D93\\\" width=\\\"100%\\\" height=\\\"100%\\\"\");\n");
      buffer.append(" strArr.push(\" id=\\\"").append(id).append("\\\"\");\n");
      buffer.append(" strArr.push(\" name=\\\"").append(id).append("\\\"\");\n");
      buffer.append("	strArr.push(\" codebase=\\\"http://java.sun.com/products/plugin/autodl/jinstall-1_4_2-windows-i586.cab#Version=1,4,2,0\\\">\");\n");
      buffer.append(" 	strArr.push(\"<param name=\\\"codebase\\\" value=\\\"").append(codebase).append("\\\">\");\n");
      buffer.append(" strArr.push(\"<param name=\\\"code\\\" value=\\\"").append(code).append("\\\">\");\n");
      buffer.append(" strArr.push(\"<param name=\\\"archive\\\" value=\\\"").append(archive).append("\\\">\");\n");
      buffer.append(" strArr.push(\"<param name=\\\"type\\\" value=\\\"application/x-java-applet;version=1.4.2\\\">\");\n");
      buffer.append("	strArr.push(\"<param name=\\\"mayscript\\\" value=\\\"true\\\">\");\n");
      buffer.append(" 	strArr.push(\"<param name=\\\"scriptable\\\" value=\\\"true\\\">\");\n");
      for (String paramName : otherParams.keySet()) {
         String paramValue = otherParams.get(paramName);
         buffer.append(" strArr.push(\"<param name=\\\"").append(paramName).append("\\\" value=\\\"").append(paramValue).append("\\\">\");\n");
      }
      buffer.append("strArr.push(\"</object>\");\n");
      buffer.append(" }\n");
      buffer.append("	else {\n");
      buffer.append("	if (moz || ns) {\n");
      buffer.append("  strArr.push(\"<embed type=\\\"application/x-java-applet;version=1.4.2\\\" pluginspage=\\\"http://java.sun.com/j2se/1.4.2\\\" codebase=\\\"");
      buffer.append(codebase).append("\\\" code=\\\"").append(code).append("\\\" archive=\\\"").append(archive);
      buffer.append("\\\" width=\\\"100%\\\" height=\\\"100%\\\" mayscript=\\\"true\\\" name=\\\"").append(name).append("\\\" \");\n");
      buffer.append(" strArr.push(\" id=\\\"").append(id).append("\\\"\");\n");
      buffer.append(" strArr.push(\" name=\\\"").append(id).append("\\\"\");\n");
      for (String paramName : otherParams.keySet()) {
         String paramValue = otherParams.get(paramName);
         buffer.append(" strArr.push(\"").append(paramName).append("=\\\"").append(paramValue).append("\\\"\");\n");
      }
      buffer.append(" strArr.push(\"><noembed><span>Java is not installed on your machine. Please install it.</span></noembed></embed>\");\n");
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
    *
    * @param out    a <code>PrintStream</code> where the response will be written to.
    * @param params a <code>Map</code> representing applet specific parameters.
    */
   private void generateAppletJS(PrintStream out, Map<Integer, Object> params) {
      out.println("<script language=\"JavaScript\">\n");
      out.println(" \n");
      this.generateAppletMainFunction(out, params);
      out.println(" \n");
      this.generateAppletResizeFunction(out);
      out.println(" \n");
      out.println("document.write(getAppletHTML())\n");
      out.println("</script>");
   }

   @Override
   protected XMessage processRequest(XMessage request, boolean sessionExpired, HttpServletRequest http_request, HttpServletResponse http_response, XSession session) {
      if (request.getAction().equalsIgnoreCase(OpProjectConstants.GET_RUN_LEVEL_ACTION)) {
         XMessage response = new XMessage();
         OpInitializer initializer = OpInitializerFactory.getInstance().getInitializer();
         response.setArgument(OpProjectConstants.RUN_LEVEL, Byte.toString(initializer.getRunLevel()));
         return response;
      }

      XMessage response = super.processRequest(request, sessionExpired, http_request, http_response, session);
      addAutoLoginCookie(request, response, http_response);
      return response;
   }

   /**
    * Add an AutoLogin cookie to the HTTP request if conditions are meet.
    *
    * @param request       the <code>XMessage</code> action request
    * @param response      the <code>XMessage</code> action response
    * @param http_response the HTTP request
    */
   private void addAutoLoginCookie(XMessage request, XMessage response, HttpServletResponse http_response) {
      boolean singOnAction = request.getAction().equalsIgnoreCase(OpProjectConstants.SIGNON_ACTION);
      boolean rememberChecked = request.getArgument(OpProjectConstants.REMEMBER_PARAM) != null && ((Boolean) request.getArgument(OpProjectConstants.REMEMBER_PARAM));
      boolean noError = response != null && response.getError() == null;
      // check if conditions are meet: sign-on action, remember param is set and login is succesful
      if (singOnAction && rememberChecked && noError) {
         String name = (String) request.getArgument(OpUserService.LOGIN);
         String password = "";
         if (request.getArgument(OpUserService.PASSWORD) != null) {
            password = (String) request.getArgument(OpUserService.PASSWORD);
         }
         // encode [ user + ' ' + password ] with base64 .
         Cookie cookie = new Cookie(XCookieManager.AUTO_LOGIN, XBase64.encodeString(name + ' ' + password));
         cookie.setVersion(0);
         cookie.setMaxAge(XCookieManager.TTL); // one day in seconds
         http_response.addCookie(cookie);
      }
   }

   /**
    * Generates an error page, as a user response to some action.
    *
    * @param http_response a <code>HttpServletResponse</code> http response.
    * @param errorMessage  a <code>String</code> representing an error message to display. The errorMessage tries to be
    *                      i18ned from the main language res file.
    * @param session       a <code>OpProjectSession</code> representing the application user session.
    * @throws java.io.IOException if opening the output stream fails
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
      if (content.getRefCount() == 0) {
         return true; // just a temporary content
      }

      Set attachments = content.getAttachments();
      for (Object attachmentObj : attachments) {
         OpObjectIfc attachment = (OpObjectIfc) attachmentObj;
         if (session.checkAccessLevel(broker, attachment.getId(), OpPermission.OBSERVER)) {
            return true;
         }
      }

      Set attachmentVersions = content.getAttachmentVersions();
      if (attachmentVersions != null) {
         for (Object attachmentVersionObj : attachmentVersions) {
            OpObjectIfc attachmentVersion = (OpObjectIfc) attachmentVersionObj;
            if (session.checkAccessLevel(broker, attachmentVersion.getId(), OpPermission.OBSERVER)) {
               return true;
            }
         }
      }

      Set documents = content.getDocuments();
      for (Object documentObj : documents) {
         OpObjectIfc document = (OpObjectIfc) documentObj;
         if (session.checkAccessLevel(broker, document.getId(), OpPermission.OBSERVER)) {
            return true;
         }
      }

      Set documentNodes = content.getDocumentNodes();
      for (Object documentObj : documentNodes) {
         OpObjectIfc documentNode = (OpObjectIfc) documentObj;
         if (session.checkAccessLevel(broker, documentNode.getId(), OpPermission.OBSERVER)) {
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

   /**
    * Handle a file upload request
    *
    * @param request a file upload <code>HttpServletRequest</code>
    * @param stream  the <code>InputStream</code> to read the files from
    * @param message the <code>XMessage</code> received from the client
    * @throws java.io.IOException if the file upload handling failed
    */
   @Override
   protected void handleFileUpload(HttpServletRequest request, InputStream stream, XMessage message)
        throws IOException {
      LinkedHashMap<String, Long> sizes = (LinkedHashMap<String, Long>) message.getArgument(XConstants.FILE_SIZE_ARGUMENT);
      Map<String, String> names = (Map<String, String>) message.getArgument(XConstants.FILE_NAME_ARGUMENT);
      Map<String, String> references = (Map<String, String>) message.getArgument(XConstants.FILE_REF_ARGUMENT);
      // check if the uplaoded file should pe inserted in a content or copied to a local file
      boolean streamToFile = false;
      if (message.getArgumentsMap().containsKey(XConstants.STREAM_TO_FILE)) {
         streamToFile = (Boolean) message.getArgument(XConstants.STREAM_TO_FILE);
      }

      if (sizes != null && !sizes.isEmpty()) {
         if (streamToFile) {
            Map<String, File> files = new HashMap<String, File>();

            for (Map.Entry<String, Long> entry : sizes.entrySet()) {
               String id = entry.getKey();
               long size = entry.getValue();
               String name = names != null ? names.get(id) : null;

               InputStream fis = new XSizeInputStream(stream, size, true);
               File newFile = new File(XEnvironmentManager.TMP_DIR, name);
               OutputStream fos = new FileOutputStream(newFile);
               XIOHelper.copy(fis, fos);
               fos.flush();
               fos.close();
               fis.close();

               // use the same new file for each uploaded file that has the same source
               Set<String> refs = getRefIds(id, references);
               for (String refId : refs) {
                  files.put(refId, newFile);
               }
            }

            message.insertObjectsIntoArguments(files);
         }
         else {
            this.checkAttachmentSizes(sizes.values());
            Map<String, String> contents = new HashMap<String, String>();

            // Get the session context ('true': create new session if necessary)
            OpProjectSession session = (OpProjectSession) getSession(request);
            OpBroker broker = session.newBroker();
            try {

               for (Map.Entry<String, Long> entry : sizes.entrySet()) {
                  String id = entry.getKey();
                  long size = entry.getValue();
                  String name = names != null ? names.get(id) : null;
                  String mimeType = OpContentManager.getFileMimeType(name != null ? name : "");

                  OpContent content = OpContentManager.newContent(new XSizeInputStream(stream, size, true), mimeType, 0);

                  OpTransaction t = broker.newTransaction();
                  broker.makePersistent(content);
                  t.commit();

                  // adds the same OpContent locator for each content that refer teh same file.
                  Set<String> refs = getRefIds(id, references);
                  for (String refId : refs) {
                     contents.put(refId, content.locator());
                  }
               }
            }
            finally {
               broker.close();
            }

            message.insertObjectsIntoArguments(contents);
         }
      }
   }

   /**
    * Checks if any attachments has a size larger than the configured application size, and
    * throws an exception if it does.
    *
    * @param attachmentSizes a <code>Collection(Long)</code> the attachment sizes in bytes.
    * @throws IOException if any of the attachment sizes is larget than the configured size.
    */
   private void checkAttachmentSizes(Collection<Long> attachmentSizes)
        throws IOException {
      OpInitializer initializer = OpInitializerFactory.getInstance().getInitializer();
      long maxSizeBytes = initializer.getMaxAttachmentSizeBytes();
      for (Long attachmentSize : attachmentSizes) {
         if (attachmentSize > maxSizeBytes) {
            String message = "Attachments larger than the configured size of " + maxSizeBytes + " are not allowed. Aborting transaction";
            logger.error(message);
            throw new IOException(message);
         }
      }
   }

   /**
    * Retrieves all the generated id of the contents that refer the same file.
    *
    * @param id         the id of the refered file
    * @param references a <code>Map</code> of key = generated id of a content, value = generated id of the refered content
    * @return a <code>Set</code> of generated ids which refere to the same file. (Includes the refered id)
    */
   private Set<String> getRefIds(String id, Map<String, String> references) {
      Set<String> refs = new HashSet<String>();
      refs.add(id);
      for (Map.Entry<String, String> entry : references.entrySet()) {
         if (entry.getValue().equals(id)) {
            refs.add(entry.getKey());
         }
      }
      return refs;
   }
}
