/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import onepoint.express.server.XExpressSession;
import onepoint.express.server.XResourceInterceptor;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpLanguageKitFile;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLanguageKit;
import onepoint.resource.XLocaleManager;
import onepoint.util.XEnvironmentManager;

public class OpReportManager implements XResourceInterceptor {

   /**
    * The Logger for this class...
    */
   private static final XLog logger = XLogFactory.getLogger(OpReportManager.class);

   /**
    * Determines the path to the filesystem, where we expect the report-jars to
    * reside.
    */
   private final static String REPORT_JAR_PATH = OpEnvironmentManager.getOnePointHome() + File.separator + "reportjars";

   /**
    * Is the base part of the path where the Jasperfiles to be included in the
    * filesystem reside...
    */
   private final static String REPORT_JAR_WORKINGPATH_BASE = XEnvironmentManager.TMP_DIR + "opp_work";

   /**
    * Is the base part of the path where the forms and scripts to be included
    * in the filesystem reside...
    */
   private final static String REPORT_JAR_EXPRESSPATH_BASE = XEnvironmentManager.TMP_DIR + "opp_work";

   /**
    * the text for the readme to be generated in the REPORT_JAR_PATH dir, if we
    * have to generate it
    */
   private final static String REPORT_JAR_PATH_READMESTRING = "In this directory the JAR-files for the reports do reside.\nIt will be automatically generated if not present\nWhile using, a Subdirectory 'work' will be created which is the scratch-area";

   /**
    * How much time between two weak-dir-scans. Currently set to 1 hour...
    */
   private final static long TIME_BETWEEN_DIRSCANS = 3600000;

   /**
    * Now some static texts for internal use, to avoid typos
    */
   private final static String JASPERREPORTSCACHENAME = "jasperReports.obj";
   private final static String MANIFESTFILE_SECTION = "Report";
   private final static String MANIFESTFILE_MANDATORY_MAINFILE = "mainfile";
   private final static String MANIFESTFILE_MANDATORY_JESNAME = "jesname";
   private final static String MANDATORY_JES_EXTENSION = "jes";
   private final static String MANIFESTFILE_MANDATORY_i18FILENAME = "i18nfilename";
   private final static String MANDATORY_i18FILENAME_EXTENSION = ".olk.xml";
   private final static String MANIFESTFILE_MANDATORY_DEFAULTLANGUAGE = "en";
   private final static String REPORTJAR_ENTRY_JAPSERDIRNAME = "jasperDirName";
   private final static String REPORTJAR_ENTRY_CACHEENTRIES = "cacheEntries";
   private final static String REPORTJAR_ENTRY_JASPERENTRIES = "jasperEntries";
   private final static String REPORTJAR_ENTRY_MAINJASPERFILEPATH = "mainJasperFilePath";
   private final static String REPORTJAR_ENTRY_LASTMODIFIED = "lastModified";
   private final static String META_INF_SUBDIR_NAME = "META-INF";
   private final static String JASPER_SUBDIR_NAME = "jasper";
   private final static String REPORTFILE_SOURCE_EXTENSION = ".jrxml";
   private final static String REPORTFILE_COMPILED_EXTENSION = ".jasper";
   private final static String COMPILED_JAVA_EXTENSION = ".class";

   /**
    * This one holds the entries for all of our reports.
    */
   private HashMap jasperReports;

   /**
    * This one holds the file-paths for opp-stuff for all of our reports.
    */
   private HashMap expressFilePathCache;

   /**
    * This is the session, this OpReportManager works for
    */
   private String userSpecificString = null;

   /**
    * This is the session's, localeID to get localized strings...
    */
   private String localeId = null;

   /**
    * This is the dir, where we look for new Reports for this user...
    */
   private File reportsDir = null;

   /**
    * This is the time, when the last Dir-scan took place...
    */
   private long lastDirScan = 0;

   /**
    * Flags that indicated when the cache should be used.
    */
   private boolean dirScanNeeded = false;
   private boolean cachedirty = false;

   /**
    * Flag indicating whether any of the reports have been changed.
    */
   private URL[] reportWorkingDirs = null;

   /**
    * Thread-local variable used to detect whether the class loader has been changed or not.
    */
   private ThreadLocal classLoaderMonitor = new ThreadLocal() {
      protected Object initialValue() {
         return Boolean.FALSE;
      }
   };

   /**
    * This is the constructor
    */
   private OpReportManager(OpProjectSession session) {
      session.addResourceInterceptor(this);
      this.jasperReports = new HashMap();
      this.expressFilePathCache = new HashMap();
      // we can do some initialization-stuff here...
      reportsDir = new File(REPORT_JAR_PATH);
      if (!reportsDir.exists()) {
         reportsDir.mkdirs();
         File readme = new File(reportsDir, "readme.txt");
         try {
            readme.createNewFile();
            FileWriter fw = new FileWriter(readme);
            fw.write(REPORT_JAR_PATH_READMESTRING);
            fw.flush();
            fw.close();
         }
         catch (Exception e) {
            logger.warn("Cannot write reports readme file", e);
         }
      }

      this.localeId = session.getLocale().getID();
      this.userSpecificString = (new Long(session.getUserID())).toString().concat(File.separator);

      String tempFilename = REPORT_JAR_EXPRESSPATH_BASE;
      File tempFile = new File(tempFilename);
      if (!tempFile.exists()) {
         tempFile.mkdirs();
      }

      tempFilename = REPORT_JAR_WORKINGPATH_BASE;
      tempFile = new File(tempFilename);
      if (!tempFile.exists()) {
         tempFile.mkdirs();
      }

      tempFilename = REPORT_JAR_EXPRESSPATH_BASE.concat(File.separator).concat(userSpecificString);
      tempFile = new File(tempFilename);
      if (!tempFile.exists()) {
         tempFile.mkdirs();
      }

      tempFilename = REPORT_JAR_WORKINGPATH_BASE.concat(File.separator).concat(userSpecificString);
      tempFile = new File(tempFilename);
      if (!tempFile.exists()) {
         tempFile.mkdirs();
      }
      // now restore state, from last use...
      // with this one we also have all our important caches back, as the lastDirScan-member is set to 0, we will do a
      // weakDirScan on the first touch anyway...
      // for explanation of the weakDirScan, see comment of
      // "weakDirScanNeeded".
      restoreState();
   }

   /**
    * Returns an <code>OpReportManager</code> instance for the given session. Since at the moment there should be just
    * 1 report manager, removes previous report managers from previous sessions.
    *
    * @param s the <code>OpProjectSession</code> to work on.
    * @return OpReportManager for this session.
    */
   public synchronized static OpReportManager getReportManager(OpProjectSession s) {
      String sessionSpecificString = String.valueOf(s.getUserID()).concat(File.separator);
      OpReportManager instance = null;

      Set resourceInterceptors = s.getResourceInterceptors();
      Iterator resourceInterceptorsIter = resourceInterceptors.iterator();
      while (resourceInterceptorsIter.hasNext()) {
         XResourceInterceptor interceptor = (XResourceInterceptor) resourceInterceptorsIter.next();
         if (interceptor instanceof OpReportManager) {
            if (((OpReportManager) interceptor).userSpecificString.equalsIgnoreCase(sessionSpecificString)) {
               instance = (OpReportManager) interceptor;
            }
            else {
               resourceInterceptorsIter.remove();
            }
         }
      }

      if (instance == null) {
         instance = new OpReportManager(s);
      }

      return instance;
   }

   /**
    * Determines, when it is the next time to run the weak Directory-Scan.
    * During a weak directory scan, all files from the jars are extracted to
    * the appropriate places, but only if the extracted lastModification Date
    * of the jared file differs from the original one, the compilations of an
    * JRXML takes place.
    *
    * @return true if the weak dir-scan should take place, otherwise false
    */
   private boolean weakDirScanNeeded() {
      return new Date().getTime() - this.lastDirScan > TIME_BETWEEN_DIRSCANS;
   }

   /**
    * @see XResourceInterceptor#getResource(String,onepoint.express.server.XExpressSession)
    */
   public byte[] getResource(String path, XExpressSession s) {
      //any resources going through this interceptor need an additional class loader
      changeClassLoader();

      if (expressFilePathCache.get(path) != null) {
         try {
            FileInputStream fis = new FileInputStream((File) expressFilePathCache.get(path));
            byte[] bytes = readBytesFromFile(fis);
            fis.close();
            return s.getResourceBroker().applyFilters(path, bytes);
         }
         catch (FileNotFoundException fnfe) {
            logger.error("Could not retrieve the resource for " + path, fnfe);
            clearCache();
            return null;
         }
         catch (IOException ioe) {
            logger.error("IOException while retrieving the resource for " + path, ioe);
            clearCache();
            return null;
         }
         catch (Exception e) {
            logger.error("Exception while retrieving the resource for " + path, e);
            clearCache();
            return null;
         }
      }
      else {
         //try loading the resource direcly via the class-loader (will only work is the resources is found in the report)
         ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
         InputStream is = currentLoader.getResourceAsStream(path);
         if (is != null) {
            try {
               byte[] bytes = readBytesFromFile(is);
               is.close();
               return s.getResourceBroker().applyFilters(path, bytes);
            }
            catch (IOException e) {
               logger.error("Cannot load report resource ", e);
            }
         }
      }
      return null;
   }

   /**
    * Changes the current class loader for this report.
    */
   private void changeClassLoader() {
      Boolean hasClassLoaderChanged = (Boolean) classLoaderMonitor.get();
      boolean isClassLoaderTypeOk = (Thread.currentThread().getContextClassLoader() instanceof OpReportCustomClassLoader);
      if (!hasClassLoaderChanged.booleanValue() || !isClassLoaderTypeOk) {
         ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
         URL[] reportDirsUrls = getReportsWorkingDirsUrls();
         URLClassLoader newClassLoader = new OpReportCustomClassLoader(reportDirsUrls, currentClassLoader);
         try {
            Thread.currentThread().setContextClassLoader(newClassLoader);
         }
         catch (SecurityException e) {
            logger.error("Cannot set class loader !", e);
         }
         classLoaderMonitor.set(Boolean.TRUE);
      }
   }

   /**
    * Loads the given custom data source for the curent report.
    *
    * @param name
    * @return
    */
   public OpReportDataSource getDataSourceClass(String name) {
      changeClassLoader();
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      try {
         Class dsClass = loader.loadClass(name);
         return (OpReportDataSource) dsClass.newInstance();
      }
      catch (ClassNotFoundException e) {
         logger.error("Cannot load the data source class", e);
      }
      catch (IllegalAccessException e) {
         logger.error("Cannot load the data source class", e);
      }
      catch (InstantiationException e) {
         logger.error("Cannot load the data source class", e);
      }
      return null;
   }

   /**
    * Reads the bytes of a file from the given input stream.
    *
    * @param fileInputStream a <code>FileInputStream</code> that points to a file.
    * @return a <code>byte[]</code> representhing the bytes of the file.
    * @throws IOException if the file cannot be read.
    */
   private byte[] readBytesFromFile(InputStream fileInputStream)
        throws IOException {
      ByteArrayOutputStream byte_output = new ByteArrayOutputStream();
      byte[] file_buffer = new byte[4096];
      int bytes_read = fileInputStream.read(file_buffer);
      while (bytes_read != -1) {
         byte_output.write(file_buffer, 0, bytes_read);
         bytes_read = fileInputStream.read(file_buffer);
      }
      byte_output.flush();
      byte[] byteOutputArray = byte_output.toByteArray();
      byte_output.close();
      return byteOutputArray;
   }

   /**
    * This method clears the cache. Thus with the next click, the whole stuff
    * will be recompiled...
    */
   private void clearCache() {
      this.jasperReports = new HashMap();
      this.expressFilePathCache = new HashMap();
      // and to get no problems with next restart...
      String tempFilename = REPORT_JAR_WORKINGPATH_BASE.concat(File.separator).concat(userSpecificString);
      File cacheFile = new File(tempFilename.concat(File.separator).concat(JASPERREPORTSCACHENAME));
      cacheFile.delete();
   }

   /**
    * Constructs the session-specific path which can be included in the classloader to get the correct Jes-files and Form-files for the given
    * reports.
    *
    * @param reportName the Jar-Name, for which the path is to be created.
    * @return The correct path, which can be included into the classpath.
    */
   public String getReportJarExpressFilesPath(String reportName) {
      String reportPathName = reportName.replace('.', '_');
      return REPORT_JAR_EXPRESSPATH_BASE.concat(File.separator).concat(userSpecificString).concat(reportPathName);
   }

   /**
    * Constructs the session-specific path which can be included in the classloader to get the correct Jasper-files and other resources for the
    * given reports.
    *
    * @param reportName the Jar-Name, for which the path is to be created.
    * @return The correct path, which can be included into the classpath.
    */
   public String getReportJarJasperFilesPath(String reportName) {
      String reportPathName = reportName.replace('.', '_');
      return REPORT_JAR_WORKINGPATH_BASE.concat(File.separator).concat(userSpecificString).concat(reportPathName).concat(File.separator).concat(JASPER_SUBDIR_NAME);
   }

   /**
    * Gives the localized Name of the requested JasperReport. If no current localization is available, the english name is given back...
    *
    * @param reportName reportName name of the report (this should be the name of the jar-file)
    * @param localeId   the two-letter code, which represents the laguage to retrieve the Name for...
    * @return localized Name of the requested JasperReport. If no current localization is available, the english name is given back...
    */
   public String getLocalizedJasperFileName(String reportName, String localeId) {
      String localizedName = (String) getAttributeForReport(reportName, localeId, jasperReports);
      
      if (localizedName == null) {
    	  int delim = localeId.indexOf("_");
    	  if (delim >= 0) {
    		  localizedName = (String) getAttributeForReport(reportName, localeId.substring(0, delim), jasperReports);
    	  }
      }
      // maybe we do not have the right locale, but English is mandatory...
      if (localizedName == null) {
         localizedName = (String) getAttributeForReport(reportName, MANIFESTFILE_MANDATORY_DEFAULTLANGUAGE, jasperReports);
      }
      return localizedName;
   }

   public String getEnFileName(String reportName) {
      return (String) getAttributeForReport(reportName, MANIFESTFILE_MANDATORY_DEFAULTLANGUAGE, jasperReports);
   }

   /**
    * Gives the localized Name of the requested JasperReport. The locale is derived from the current session
    *
    * @param reportName reportName name of the report (this should be the name of the jar-file)
    * @return localized Name of the requested JasperReport. If no current localization is available, the english name is given back...
    */
   public String getLocalizedJasperFileName(String reportName) {
      return getLocalizedJasperFileName(reportName, localeId);
   }

   /**
    * Gives the AbsolutePath to the Jasper-file which is the "Main-report" for this Jasper-Report (remember: there can be Subreports as well)...
    *
    * @param reportName reportName name of the report (this should be the name of the jar-file)
    * @return AbsolutePath to the Jasper-file which is the "Main-report" for this Jasper-Report
    */
   public String getJasperFileName(String reportName) {
      return (String) getAttributeForReport(reportName, REPORTJAR_ENTRY_MAINJASPERFILEPATH, jasperReports);
   }

   /**
    * Gives the AbsolutePath to the Directory, where all the resources (jasperfile, images, resource-bundles)necessary for the given report
    * reside.
    *
    * @param reportName name of the report (this should be the name of the jar-file)
    * @return AbsolutePath to the Directory, where all the resources (jasperfile, images, resource-bundles)necessary for the given
    *         report reside.
    */
   public String getJasperDirName(String reportName) {
      return (String) getAttributeForReport(reportName, REPORTJAR_ENTRY_JAPSERDIRNAME, jasperReports);
   }

   /**
    * Gives the name of the jesfile, this report is handled by
    *
    * @param reportName name of the report (this should be the name of the jar-file)
    * @return the name of the jesfile, this report is handled by
    */
   public String getJesName(String reportName) {
      return (String) getAttributeForReport(reportName, MANIFESTFILE_MANDATORY_JESNAME, jasperReports);
   }

   /**
    * Gives the name of the i18nfile, this report is handled by
    *
    * @param reportName name of the report (this should be the name of the jar-file)
    * @return the name of the i18n file, as it appears in the manifest of the jar file.
    */
   public String getI18nFileName(String reportName) {
      return (String) getAttributeForReport(reportName, MANIFESTFILE_MANDATORY_i18FILENAME, jasperReports);
   }

   /**
    * This method delivers the Attributestring asked for out of the JasperReports-Hashmap.
    *
    * @param reportName         The JasperReport to query.
    * @param attributeName      The Attribute to query.
    * @param jasperReportsLocal The HashMap of reportParams for this session. Needed, as this is a static method.
    * @return the queried Attrib for the JasperReport in question. Else null...
    */
   private Object getAttributeForReport(String reportName, String attributeName, HashMap jasperReportsLocal) {
      Object retVal = null;
      HashMap reportParam = (HashMap) jasperReportsLocal.get(reportName);
      if (reportParam != null) {
         retVal = reportParam.get(attributeName);
      }
      return retVal;
   }

   /**
    * This method checks, if the current state of our already parsed Reportdescriptions is still current and gives the corrected ones out.
    *
    * @return Hashmap of the reportDefinitons.
    */
   public HashMap getJasperDescriptions() {
      cachedirty = false;
      dirScanNeeded = weakDirScanNeeded();

      File[] f = reportsDir.listFiles(new ReportFilter());
      HashMap temp = (HashMap) jasperReports.clone();
      for (int i = 0; i < f.length; i++) {
// fixme: didi         temp.remove(f[i].getAbsolutePath());
         temp.remove(f[i].getName());
      }
      if (temp.size() > 0) {
         Iterator t = temp.keySet().iterator();
         while (t.hasNext()) {
            String name = (String) t.next();
            removeReportEntries(name);
         }
      }
      if (cachedirty) {
         persistState();
      }
      if (dirScanNeeded) {
         lastDirScan = new Date().getTime();
         dirScanNeeded = false;
      }
      return jasperReports;
   }

   private synchronized void persistState() {
      boolean persistOk = true;
      String tempFilename = REPORT_JAR_WORKINGPATH_BASE.concat(File.separator).concat(userSpecificString);
      File tempFile = new File(tempFilename);
      FileOutputStream fos = null;
      ObjectOutputStream oos = null;
      if (!tempFile.exists()) {
         tempFile.mkdir();
      }
      try {
         fos = new FileOutputStream(tempFilename.concat(File.separator).concat(JASPERREPORTSCACHENAME));
         oos = new ObjectOutputStream(fos);
         oos.writeObject(jasperReports);
         oos.flush();
         fos.flush();
      }
      catch (IOException ioe) {
         logger.error("IOException while trying to open FileOutputStream in persistState()", ioe);
         persistOk = false;
      }
      finally {
         if (oos != null) {
            try {
               oos.close();
            }
            catch (Exception ee) {
               logger.warn(ee);
            }
         }
         if (fos != null) {
            try {
               fos.close();
            }
            catch (Exception ee) {
               logger.warn(ee);
            }
         }
      }
      cachedirty = persistOk;
   }

   private synchronized void restoreState() {
      boolean restoreOk = true;
      String tempFilename = REPORT_JAR_WORKINGPATH_BASE.concat(File.separator).concat(userSpecificString);
      FileInputStream fis = null;
      ObjectInputStream ois = null;
      try {
         fis = new FileInputStream(tempFilename.concat(File.separator).concat(JASPERREPORTSCACHENAME));
         ois = new ObjectInputStream(fis);
         jasperReports = (HashMap) ois.readObject();
      }
      catch (FileNotFoundException fnfe) {
         logger.debug("could not find jasperReports.obj (most probably someone deleted that)");
         restoreOk = false;
      }
      catch (Exception e) {
         logger.error("Exception while trying to restore state of 'jasperReports'", e);
         // that is more serious. Thus we try do get rid of the cache-object.
         if (ois != null) {
            try {
               ois.close();
            }
            catch (Exception ee) {
               logger.warn(e);
            }
         }
         if (fis != null) {
            try {
               fis.close();
            }
            catch (Exception ee) {
               logger.warn(e);
            }
         }
         File cacheFile = new File(tempFilename.concat(File.separator).concat(JASPERREPORTSCACHENAME));
         cacheFile.delete();
         restoreOk = false;
      }
      finally {
         if (ois != null) {
            try {
               ois.close();
            }
            catch (Exception ee) {
               logger.warn(ee);
            }
         }
         if (fis != null) {
            try {
               fis.close();
            }
            catch (Exception ee) {
               logger.warn(ee);
            }
         }
      }
      Iterator iter = jasperReports.keySet().iterator();
      while (iter.hasNext()) {
         HashMap reportMap = ((HashMap) jasperReports.get(iter.next()));
         expressFilePathCache.putAll((HashMap) reportMap.get(REPORTJAR_ENTRY_CACHEENTRIES));
      }
      // if cache-restore does not fully function, we prefer to build up everything from scratch...
      if (!restoreOk) {
         clearCache();
      }
   }

   /**
    * Deletes the cache-entries, which are associated with the given Report.
    *
    * @param reportName report, for which cache-entries are to be deleted.
    */
   private void removeReportEntries(String reportName) {
      HashMap currReport = (HashMap) jasperReports.get(reportName);
      if (currReport != null) {
         Iterator cacheEntries = ((HashMap) currReport.get(REPORTJAR_ENTRY_CACHEENTRIES)).keySet().iterator();
         while (cacheEntries.hasNext()) {
            expressFilePathCache.remove((String) cacheEntries.next());
         }
      }
      // now delete the old stuff...
      File oldFile = new File(getReportJarExpressFilesPath(reportName));
      if (!deleteDirAndContent(oldFile)) {
         logger.warn("could not delete express-dir of an outdated report:" + oldFile.getAbsolutePath());
      }
      oldFile = new File(getReportJarJasperFilesPath(reportName));
      if (!deleteDirAndContent(oldFile)) {
         logger.warn("could not delete working-dir of an outdated report:" + oldFile.getAbsolutePath());
      }
      jasperReports.remove(reportName);
   }

   /**
    * If for some reasons, the caching-entries seem corrupt for this
    * reportName, the outside-world can tell us and this entry is deleted.
    *
    * @param reportName the report which should be deleted from the cache.
    */
   public void removeReportFromCache(String reportName) {
      removeReportEntries(reportName);
      persistState();
   }

   /**
    * If for some reasons, this entry needs attention. First all cacheentries
    * are cleared, and then an update of these stuff is tried.
    *
    * @param reportName the report which should be deleted from the cache.
    */
   public void updateReportCacheEntry(String reportName) {
      removeReportEntries(reportName);
      checkReportJar(new File(reportsDir.getAbsolutePath().concat(
           File.separator).concat(reportName)), false);
      persistState();
   }

   /**
    * This method checks, if the given file in <code>jarFilePath</code> is an appropriate jar which contains a Report-definition as we expect it.
    * For that the Manifest is examined. By the way a HashMap is maintained which speeds up stuff...
    * ATTENTION: for Jar-Handling we need stuff to be handled in files. So it is no opportunity to switch to steams or something...
    *
    * @param jarFilePath destincts, where the jar in question is located.
    * @return true if the given file is a valid report-file, else false
    */
   private boolean checkReportJar(File jarFilePath, boolean weakCheck) {
      //make sure the class loader gets changed after a report what checked.
      this.classLoaderMonitor.set(Boolean.FALSE);

      // first paranoia-stuff
      if (jarFilePath == null) {
         logger.error("Got null-path for 'checkReportsJar'");
         return false;
      }
      // Now we can start...
      JarFile reportjar = null;
      try {
         reportjar = new JarFile(jarFilePath);
      }
      catch (IOException ioe) {
         // nothing to do about it...
         logger.error("Got unusable Path in 'checkReportsJar' for '" + jarFilePath.getAbsolutePath() + "' : " + ioe.getMessage());
         return false;
      }
      String name = jarFilePath.getName();
      Long lastModified = jarFilePath.lastModified();
      boolean needCreation = jasperReports.get(name) == null;
      if (!needCreation) {
         // even if we had that report already, it might be outdated and thus needs refinement...
         Long persistedModified = (Long) ((HashMap) jasperReports.get(name)).get(REPORTJAR_ENTRY_LASTMODIFIED);
         boolean needUpdate = !persistedModified.equals(lastModified);
         if (needUpdate) {
            // delete old cache-entries...
            removeReportEntries(name);
            needCreation = true;
         }
      }

      //if the hashmap contains the entry, we already got by and stuff should be ok...
      if (needCreation) {
         return updateReportJar(name, jarFilePath, reportjar, lastModified, false);
      }
      else if (weakCheck) {
         return updateReportJar(name, jarFilePath, reportjar, lastModified, weakCheck);
      }
      return true; // coming here means, everything worked ok ;-)
   }

   /**
    * Checks if the given description is valid, for the jar indicated by the report.
    *
    * @param reportDescription a <code>Map</code> of <code>String,String</code> representing the report description.
    * @param jarFilePath       a <code>String</code> representing the path to a jar file.
    * @return <code>true</code> if the report description is valid, <code>false</code> otherwise.
    */
   private boolean isReportDescriptionValid(Map reportDescription, String jarFilePath) {
      // to make sure, we did not get an incompatible jar, we check if mandatory attributes were found in the manifest...
      String mainFile = (String) reportDescription.get(MANIFESTFILE_MANDATORY_MAINFILE);
      if (mainFile == null) {
         logger.error("Got a Jar to handle in 'checkReportsJar' without the required '" + MANIFESTFILE_MANDATORY_MAINFILE + "' attribute in the manifest! ("
              + jarFilePath + ")");
         return false;
      }
      if (reportDescription.get(MANIFESTFILE_MANDATORY_JESNAME) == null) {
         logger.error("Got a Jar to handle in 'checkReportsJar' without the required '" + MANIFESTFILE_MANDATORY_JESNAME + "' attribute in the manifest! ("
              + jarFilePath + ")");
         return false;
      }
      if (reportDescription.get(MANIFESTFILE_MANDATORY_DEFAULTLANGUAGE) == null) {
         logger.error("Got a Jar to handle in 'checkReportsJar' without the required '" + MANIFESTFILE_MANDATORY_DEFAULTLANGUAGE + "' attribute in the manifest! ("
              + jarFilePath + ")");
         return false;
      }
      return true;
   }

   /**
    * Checks if a given file name string matches the expected patterns.
    *
    * @param fileName  a <code>String</code> representing the name of a file.
    * @param prefix    a <code>String</code> representing the expected prefix.
    * @param extension a <code>String</code> representing the expected extension.
    * @return <code>true</code> if the filename contains the prefix and has the extension.
    */
   private static boolean matchesFilePattern(String fileName, String prefix, String extension) {
      return fileName.endsWith(extension) && fileName.startsWith(prefix);
   }

   private boolean updateReportJar(String name, File jarFilePath, JarFile reportjar, Long lastModified, boolean weakCheck) {
      //if a report jar is updated, make sure the working dirs are calculated again.
      this.reportWorkingDirs = null;

      try {
         Manifest mf = reportjar.getManifest();
         HashMap reportDescription = new HashMap();
         reportDescription.put(REPORTJAR_ENTRY_LASTMODIFIED, lastModified);

         //get the attributes from the manifest and create the report description
         Attributes reportAttributes = mf.getAttributes(MANIFESTFILE_SECTION);
         if (reportAttributes == null) {
            logger.error("Got a Jar to handle in 'checkReportsJar' without a '" + MANIFESTFILE_SECTION + "' section in the manifest! ("
                 + jarFilePath + ")");
            return false;
         }
         Iterator reportAttributesIterator = reportAttributes.keySet().iterator();
         while (reportAttributesIterator.hasNext()) {
            String repKey = ((Name) reportAttributesIterator.next()).toString();
            reportDescription.put(repKey, reportAttributes.getValue(repKey));
         }

         //check report description
         if (!isReportDescriptionValid(reportDescription, jarFilePath.toString())) {
            return false;
         }

         //the parent directory which contains all the files (extracted from the jar)
         File jarsWorkDir = new File(getReportJarExpressFilesPath(name));
         if (!jarsWorkDir.exists()) {
            jarsWorkDir.mkdir();
         }
         String baseDir = jarsWorkDir.getAbsolutePath();

         //the subdirectory of the parent directory where the japer related files reside
         File jasperWorkDir = new File(getReportJarJasperFilesPath(name));
         if (!jasperWorkDir.exists()) {
            jasperWorkDir.mkdir();
         }

         reportDescription.put(REPORTJAR_ENTRY_JAPSERDIRNAME, jasperWorkDir.getAbsolutePath());
         reportDescription.put(REPORTJAR_ENTRY_CACHEENTRIES, new HashMap());
         reportDescription.put(REPORTJAR_ENTRY_JASPERENTRIES, new HashMap());

         // now write out anything we need...
         Enumeration jarEntries = reportjar.entries();
         // we have to make sure, that the jesfile we think to have, is really there.
         boolean foundJesFile = false;
         boolean foundLanguageFile = false;
         while (jarEntries.hasMoreElements()) {
            JarEntry currEntry = (JarEntry) jarEntries.nextElement();
            String entryName = currEntry.getName();
            if (entryName.startsWith(META_INF_SUBDIR_NAME)) {
               continue;
            }

            //all files other than .class files and jasper reports files are cached
            boolean putToCache = true;
            if (entryName.startsWith(JASPER_SUBDIR_NAME) || entryName.endsWith(COMPILED_JAVA_EXTENSION)) {
               putToCache = false;
            }

            String targetFilename = baseDir.concat(File.separator).concat(entryName);
            File target = new File(targetFilename);

            //check that the mandatory files are there
            String jesFilePrefix = (String) reportDescription.get(MANIFESTFILE_MANDATORY_JESNAME);
            foundJesFile = foundJesFile || matchesFilePattern(target.getName(), jesFilePrefix, MANDATORY_JES_EXTENSION);
            String languageFilePrefix = (String) reportDescription.get(MANIFESTFILE_MANDATORY_i18FILENAME);
            foundLanguageFile = foundLanguageFile || matchesFilePattern(target.getName(), languageFilePrefix, MANDATORY_i18FILENAME_EXTENSION);

            // now only stuff, we need...
            InputStream is = null;
            FileOutputStream fos = null;
            long jarEntryLastModified = 0;
            jarEntryLastModified = currEntry.getTime();
            try {
               if (jarEntryLastModified != target.lastModified()) {
                  is = reportjar.getInputStream(currEntry);
                  if (targetFilename.endsWith("/")) {
                     target.mkdir();
                  }
                  else {
                     target.createNewFile();
                     fos = new FileOutputStream(target);
                     byte[] file_buffer = new byte[4096];
                     int bytes_read = is.read(file_buffer);
                     while (bytes_read != -1) {
                        fos.write(file_buffer, 0, bytes_read);
                        bytes_read = is.read(file_buffer);
                     }
                     if (putToCache) {
                        String key = "/".concat(entryName);
                        ((HashMap) reportDescription.get(REPORTJAR_ENTRY_CACHEENTRIES)).put(key, target);
                        expressFilePathCache.put(key, target);
                     }
                  }
               }
            }
            catch (Exception e) {
               logger.error("Cannot update report " + name, e);
            }
            finally {
               if (is != null) {
                  is.close();
               }
               if (fos != null) {
                  fos.flush();
                  fos.close();
                  // we tweak the change-date, to enable some caching...
                  target.setLastModified(jarEntryLastModified);
               }
            }

            if (targetFilename.endsWith(REPORTFILE_SOURCE_EXTENSION)) {
               // now do the trick to enable weak scanning...
               ((HashMap) reportDescription.get(REPORTJAR_ENTRY_JASPERENTRIES)).put(target.getAbsolutePath(), target.lastModified());
            }
         }

         //everything is broken, if the jesfile was not in the jar...
         if (!foundJesFile) {
            logger.error("Did not find the given '" + MANIFESTFILE_MANDATORY_JESNAME + "' entry in the jarfile '" + jarFilePath + "'");
            return false;
         }
         else if (!foundLanguageFile) {
            logger.error("Did not find the given '" + MANIFESTFILE_MANDATORY_i18FILENAME + "' entry in the jarfile '" + jarFilePath + "'");
            return false;
         }

         //compile jasper files
         boolean compileResult = compileJasperFiles(jasperWorkDir, reportDescription, weakCheck);
         if (!compileResult) {
            logger.error("There was an error when compiling the jasper files");
            return false;
         }

         //register the language kits
         this.registerLanguageKits(jarsWorkDir);

         // as everything went ok, we now can add stuff to the cache.
// fixme: didi         jasperReports.put(jarFilePath.getAbsolutePath(), reportDescription);
         jasperReports.put(name, reportDescription);
      }
      catch (IOException ioe) {
         logger.error("Error checking report " + name, ioe);
         return false;
      }
      catch (Exception e) {
         logger.error("Something serious happened in 'checkReportJar'", e);
      }
      finally {
         // we should tidy up. E.g. we still have a filehandle on wrong jars, which we want to get rid of...
         try {
            reportjar.close();
         }
         catch (Exception e) {
            logger.error(e);
         }
      }
      cachedirty = true;
      return true;
   }

   /**
    * Compiles all the jasper files from the given directory, and updates a report description.
    *
    * @param jarsWorkDir       a <code>File</code> representing a directory where the jasper files are located.
    * @param reportDescription a <code>HashMap</code> representing the report decription.
    * @param weakCheck         a <code>boolean</code> indicating whether to look at the timestamps or not.
    * @return <code>true</code> if the compilation was successfull.
    */
   private boolean compileJasperFiles(File jarsWorkDir, HashMap reportDescription, boolean weakCheck) {
      // now we are quite sure, to have all JRXML-files in the root of our
      // current working dir. Lets go and compile them...
      File[] jrxmlFiles = jarsWorkDir.listFiles(new JRXMLFilter());
      for (int i = 0; i < jrxmlFiles.length; i++) {
         String jrxmlFileName = jrxmlFiles[i].getName();
         String jrxmlFilePath = jrxmlFiles[i].getAbsolutePath();
         String jasperFileName = jrxmlFilePath.replaceAll(REPORTFILE_SOURCE_EXTENSION, REPORTFILE_COMPILED_EXTENSION);
         Long lastCacheModified = (Long) ((HashMap) reportDescription.get(REPORTJAR_ENTRY_JASPERENTRIES)).get(jrxmlFilePath);
         File jasperFile = new File(jasperFileName);
         // we only want to compile, if the compile-date is different from the last change date of the Source...
         if (!weakCheck || lastCacheModified.longValue() != jasperFile.lastModified()) {
            if (!OpJasperReportBuilder.compileReport(jrxmlFilePath, jasperFileName)) {
               return false;
            }
            // to make caching function, we have to tweak the change-date...
            jasperFile.setLastModified(lastCacheModified.longValue());
         }
         // if it is our main-file, we have to remember...
         String mainFile = (String) reportDescription.get(MANIFESTFILE_MANDATORY_MAINFILE);
         if (jrxmlFileName.indexOf(mainFile) != -1) {
            reportDescription.put(REPORTJAR_ENTRY_MAINJASPERFILEPATH, jasperFileName);
         }
      }
      return true;
   }

   /**
    * Registers all the language kits for the given reports dir.
    *
    * @param jarsWorkDir a <code>File</code> representing the working dir of a report jar.
    */
   private void registerLanguageKits(File jarsWorkDir) {
      File[] languageKits = jarsWorkDir.listFiles(new OLKFilter());
      for (int i = 0; i < languageKits.length; i++) {
         File languageKitFile = languageKits[i];
         try {
            BufferedInputStream buff = new BufferedInputStream(languageKitFile.toURL().openStream());
            XLanguageKit languageKit = OpLanguageKitFile.loadLanguageKit(buff);
            XLocaleManager.registerOverriddingLanguageKit(languageKit, true);
            buff.close();
            logger.debug("Registered language kit: " + languageKitFile.getName());
         }
         catch (IOException e) {
            logger.error("Cannot load language kit from " + languageKitFile.getAbsolutePath(), e);
         }
      }
   }

   /**
    * Returns an array of URLs for each of the working directories of the registered reports.
    * This is used to be able to dynamically load form provider, via a modified class loader.
    *
    * @return a <code>URL[]</code> representing urls out of the report directories.
    */
   private URL[] getReportsWorkingDirsUrls() {
      if (this.reportWorkingDirs == null) {
         List result = new ArrayList();
         Iterator reportNameIt = jasperReports.keySet().iterator();
         while (reportNameIt.hasNext()) {
            String reportName = (String) reportNameIt.next();
            String reportWorkDir = getReportJarExpressFilesPath(reportName);
            try {
               URL dirUrl = new File(reportWorkDir).toURL();
               result.add(dirUrl);
            }
            catch (MalformedURLException e) {
               logger.error("Cannot get URL for the working directory " + reportWorkDir + " of report " + reportName, e);
            }
         }
         this.reportWorkingDirs = (URL[]) result.toArray(new URL[0]);
      }
      return this.reportWorkingDirs;
   }

   /**
    * A usefull method, which deletes a whole file-tree. may be optimized by
    * better error-handling ;-)
    *
    * @param dir the File to be deleted
    * @return true if everything went well, otherwise false.
    */
   public static synchronized boolean deleteDirAndContent(File dir) {
      if (dir.exists()) {
         // if it is a dir,
         if (dir.isDirectory()) {
            // we have to handle it recursively
            return delDir(dir);
         }
         // otherwise it is just a file
         return dir.delete();
      }
      // it was obviously deleted...
      return true;
   }

   /**
    * Recursive-method to delete a whole dir-structure
    *
    * @param dir the dir to delete
    * @return true if everything worked well, otherwise false
    */
   private static boolean delDir(File dir) {
      boolean ret = true;
      File[] files = dir.listFiles();
      for (int i = 0; i < files.length; i++) {
         if (files[i].isDirectory()) {
            ret = ret && delDir(files[i]);
         }
         else {
            ret = ret && files[i].delete();
         }
      }
      ret = ret && dir.delete();
      return ret;
   }

   /**
    * This class filters files ending with ".jrxml"...
    *
    * @author jmersmann
    */
   private class JRXMLFilter implements FilenameFilter {

      /**
       * @see FilenameFilter#accept(java.io.File,String)
       */
      public boolean accept(File dir, String name) {
         return name.endsWith(REPORTFILE_SOURCE_EXTENSION);
      }
   }

   /**
    * This class filters files ending with ".olk.xml"...
    *
    * @author jmersmann
    */
   private class OLKFilter implements FilenameFilter {

      /**
       * @see FilenameFilter#accept(java.io.File,String)
       */
      public boolean accept(File dir, String name) {
         return name.endsWith(MANDATORY_i18FILENAME_EXTENSION);
      }
   }

   /**
    * This class filters, which files are to be accepted as Report-containers
    * (normally will be jars...
    *
    * @author jmersmann
    */
   private class ReportFilter implements FilenameFilter {

      /**
       * This method filters for a given directory all Jars
       *
       * @see java.io.FilenameFilter#accept(java.io.File,java.lang.String)
       */
      public boolean accept(File dir, String name) {
         // we really only want to deal with jar-files...
         if (name.indexOf(".jar") == -1) {
            return false;
         }
         return checkReportJar(new File(dir + "/" + name), dirScanNeeded);
      }
   }
}
