/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Node;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.configuration.OpNewConfigurationHandler;
import onepoint.project.tools.generated.ObjectFactory;
import onepoint.project.tools.generated.OpLanguageKit;
import onepoint.project.tools.generated.OpParameter;
import onepoint.project.tools.generated.OpResource;
import onepoint.project.tools.generated.OpResourceMap;

/**
 * @author dfreis
 *
 */
public class OpResourceTool {

   /**
    * the package containing the jaxb auto generated classes
    */
   private final static String JAXB_CLASS_PACKAGE = "onepoint.project.tools.generated";

   private final static String[] ALL_LANGS = { "en", "de", "fr", "ru", "es" };
   /**
    * the logger
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpResourceTool.class);

   private static final String[] SRC_DIRS = 
      { "opproject/src", "opproject_asp/src", "opproject_closed/src" };

   private String referenceLang;

   private String[] destLang = ALL_LANGS;

   private File srcPath;

   private File destPath;

   /**
    * reads a configuration file.
    *
    * @param resourceFile the config file to read
    * @return a class representing the config file.
    * @throws JAXBException         in case of an JAXB error.
    * @throws FileNotFoundException if no file was found.
    */
   private OpLanguageKit readResourceFile(File resourceFile)
        throws JAXBException, FileNotFoundException {
      JAXBContext jc = JAXBContext.newInstance(JAXB_CLASS_PACKAGE);
      Unmarshaller unmarshaller = jc.createUnmarshaller();
      unmarshaller.setEventHandler(new ValidationEventHandler() {
         // allow unmarshalling to continue even if there are errors
         public boolean handleEvent(ValidationEvent ve) {
            // ignore warnings
            if (ve.getSeverity() != ValidationEvent.WARNING) {
               ValidationEventLocator vel = ve.getLocator();
               logger.fatal("xmlfile not valid: Line:Col[" + vel.getLineNumber() +
                    ":" + vel.getColumnNumber() +
                    "]:" + ve.getMessage());
               return false;
            }
            return true;
         }
      });
      FileInputStream is = new FileInputStream(resourceFile);

      JAXBElement poElement = (JAXBElement) unmarshaller.unmarshal(is);
      return (OpLanguageKit) poElement.getValue();
   }

   /**
    * @param args
    * @pre
    * @post
    */
   public static void main(String[] args) {
      OpResourceTool tool = new OpResourceTool();
      
      Options options = new Options();
      options.addOption(new Option("help", "print this message"));
      options.addOption(OptionBuilder.withArgName("s")
            .withLongOpt("source")
           .hasArg()
           .withDescription("use given directory as the root dir for opproject sources." +
                " This directory is treversed to find all resource files.")
           .create("opsrc"));
      options.addOption(OptionBuilder.withArgName("d")
            .withLongOpt("dest")
           .hasArg()
           .withDescription("use given directory as the root dir for all created resource files." +
                " These directories will contain a subdirectory with the name of the language.")
           .create("opdest"));
      options.addOption(OptionBuilder.withArgName("r")
            .withLongOpt("reference-lang")
            .hasArg()
            .withDescription("use given directory as the root dir for opproject sources." +
                 " This directory is treversed to find all resource files.")
            .create("reflang"));
      CommandLineParser parser = new GnuParser();
      try {
         // parse the command line arguments
         CommandLine line = parser.parse(options, args);
         if (line.getOptions().length == 0 || line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("OpResourceTool", options);
            System.exit(0);
         }
         String opSource = line.getOptionValue("opsrc");
         if (opSource != null) {
            tool.setOpSourcePath(opSource);
         }
         String opDest = line.getOptionValue("opdest");
         if (opDest != null) {
            tool.setOpDestinationPath(opDest);
         }
         String resourceLang = line.getOptionValue("reflang");
         if (resourceLang != null) {
            tool.setReferenceLang(resourceLang);
         }
         tool.start();
      }
      catch (ParseException exp) {
         // oops, something went wrong
         System.err.println("Parsing failed.  Reason: " + exp.getMessage());
         System.exit(-1);
      }
      catch (IOException exc) {
         // oops, something went wrong
         System.err.println("IO Exception.  Reason: " + exc.getMessage());
         System.exit(-1);
      }
   }

   /**
    * @throws IOException 
    * 
    */
   private void start() throws IOException {
      FilenameFilter filter = new FilenameFilter() {
         public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.endsWith("_"+OpResourceTool.this.referenceLang+".olk.xml");
          }
      };
      FilenameFilter dirFilter = new FilenameFilter() {
         public boolean accept(File dir, String name) {
            File toTest = new File(dir, name);
            return (toTest.isDirectory());
          }
      };

      try {
         HashMap<File, HashMap<String, HashMap<String, String>>> map = new HashMap<File, HashMap<String,HashMap<String,String>>>();
         for (int pos = 0; pos < SRC_DIRS.length; pos++) {
            process(new File(srcPath, SRC_DIRS[pos]), map, filter, dirFilter);
         }
         for (int pos = 0; pos < destLang.length; pos++) {
            if (!destLang[pos].equals(referenceLang)) {
               HashMap<File,HashMap<String, HashSet<String[]>>> warnings = new HashMap<File, HashMap<String,HashSet<String[]>>>();
               HashMap<File, HashMap<String, HashSet<String>>> changedFiles = complete(destLang[pos], map, warnings);
               if (destPath != null) {
                  File langDir = new File(destPath, destLang[pos]);
                  FileWriter writer = new FileWriter(new File(langDir, "toTranslate.xml"));

                  writeIndexFile(writer, changedFiles);
                  writer.close();

                  writer = new FileWriter(new File(langDir, "warnings.xml"));
                  writeWarningsFile(writer, warnings);
                  writer.close();                  
               }
               else {
                  PrintWriter writer = new PrintWriter(System.out);
                  writeIndexFile(writer, changedFiles);
                  writer.flush();
                  writeWarningsFile(writer, warnings);
               }
               System.out.println("changed '"+changedFiles.size()+"' files for language '"+destLang[pos]+"'");
            }
         }
      }
      catch (FileNotFoundException exc) {
         // TODO Auto-generated catch block
         exc.printStackTrace();
      }
      catch (JAXBException exc) {
         // TODO Auto-generated catch block
         exc.printStackTrace();
      }
      
   }

   /**
    * @param changedFiles 
    * @param destPath
    * @param string
    * @throws IOException 
    * @pre
    * @post
    */
   private void writeIndexFile(Writer writer, HashMap<File,HashMap<String,HashSet<String>>> changedFiles) throws IOException {
      // write index file of non translated entries
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      for (Map.Entry<File, HashMap<String, HashSet<String>>> entry : changedFiles.entrySet()) {
         String file;
         if (destPath != null) {
            file = getRelativeFileName(entry.getKey(), srcPath);            
         }
         else {
            file = entry.getKey().getAbsolutePath();
         }
            
         writer.write("<file name=\""+file+"\">\n");
         for (Map.Entry<String, HashSet<String>> pair : entry.getValue().entrySet()) {
            writer.write("    <resource-map id=\""+pair.getKey()+"\">\n");
            for (String value : pair.getValue()) {
               writer.write("        <resource id=\""+value+"\" />\n");
            }
            writer.write("    </resource-map>\n");
         }
         writer.write("</file>\n");
      }
   }

   /**
    * @param changedFiles 
    * @param destPath
    * @param string
    * @throws IOException 
    * @pre
    * @post
    */
   private void writeWarningsFile(Writer writer, HashMap<File,HashMap<String,HashSet<String[]>>> changedFiles) throws IOException {
      // write index file of non translated entries
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      for (Map.Entry<File, HashMap<String, HashSet<String[]>>> entry : changedFiles.entrySet()) {
         String file;
         if (destPath != null) {
            file = getRelativeFileName(entry.getKey(), srcPath);            
         }
         else {
            file = entry.getKey().getAbsolutePath();
         }
            
         writer.write("<file name=\""+file+"\">\n");
         for (Map.Entry<String, HashSet<String[]>> pair : entry.getValue().entrySet()) {
            writer.write("    <resource-map id=\""+pair.getKey()+"\">\n");
            for (String[] values : pair.getValue()) {
               writer.write("        <resource id=\""+values[0]+"\">"+values[1]+"</resource>\n");
            }
            writer.write("    </resource-map>\n");
         }
         writer.write("</file>\n");
      }
   }

   /**
    * @param key
    * @param langDir
    * @return
    * @pre
    * @post
    */
   private String getRelativeFileName(File file, File dir) {
      return file.getAbsolutePath().substring(dir.getAbsolutePath().length()+1);
   }

   /**
    * @param srcPath2
    * @param string
    * @param map
    * @param filter
    * @param dirFilter
    * @throws JAXBException 
    * @throws IOException 
    * @pre
    * @post
    */
   private HashMap<File,HashMap<String,HashSet<String>>> complete(String lang, 
         HashMap<File, HashMap<String, HashMap<String, String>>> map,
         HashMap<File, HashMap<String, HashSet<String[]>>> warnings)
         throws JAXBException, IOException {
      HashMap<File, HashMap<String, HashSet<String>>> ret = new HashMap<File, HashMap<String,HashSet<String>>>();
      for (Map.Entry<File, HashMap<String, HashMap<String, String>>> entry : map.entrySet()) {
         File refFile = entry.getKey();
         File destFile = getDestFile(refFile, lang);
         HashMap<String, HashMap<String, String>> destMap;
         if (destFile.exists()) {
            destMap = process(destFile);
         }
         else {
            destMap = new HashMap<String, HashMap<String,String>>();
         }
         HashMap<String, HashSet<String[]>> warningsMap = new HashMap<String, HashSet<String[]>>();
         HashMap<String, HashSet<String>> added = complete(destMap, entry.getValue(), warningsMap);
         if (!added.isEmpty()) {
            ret.put(destFile, added);
         }
         if (!warningsMap.isEmpty()) {
            warnings.put(destFile, warningsMap);
         }
         if (!added.isEmpty() || destPath != null) { // if a destPath is given write all files
            File fileToWrite;
            if (destPath != null) {
               String relPath = getRelativeFileName(destFile, srcPath);
               fileToWrite = new File(new File(destPath, lang), relPath);
            }
            else {
               fileToWrite = destFile;
            }
            write(fileToWrite, destMap, lang, added);
         }
      }
      return ret;
   }

   /**
    * @param destFile
    * @param destMap
    * @param lang 
    * @param added
    * @throws JAXBException 
    * @throws IOException 
    * @pre
    * @post
    */
   private void write(File destFile,
         HashMap<String, HashMap<String, String>> destMap,
         String lang, HashMap<String,HashSet<String>> added) throws JAXBException, IOException {
//      for (Map.Entry<String, HashSet<String>> entry : added.entrySet()) {
//         System.out.print("adding: ");
//         for (String value : entry.getValue()) {
//            System.out.print(entry.getKey()+","+value);
//            System.out.println(" to file: "+destFile);
//         }
//      }

      if (!destFile.getParentFile().exists()) {
         destFile.getParentFile().mkdirs();
//         destFile.createNewFile();
      }
      JAXBContext jc = JAXBContext.newInstance(JAXB_CLASS_PACKAGE);
      ObjectFactory factory = new ObjectFactory();
      OpLanguageKit languageKit = factory.createOpLanguageKit();
      languageKit.setLocale(lang);
      List<OpResourceMap> resourceMaps = languageKit.getResourceMap();
      for (Map.Entry<String, HashMap<String, String>> entry : destMap.entrySet()) {
         OpResourceMap resourceMap = factory.createOpResourceMap();
         resourceMaps.add(resourceMap);
         resourceMap.setId(entry.getKey());
         List<OpResource> resources = resourceMap.getResource();
         HashSet<String> addedSet = added.get(entry.getKey());
         for (Map.Entry<String, String> pair : entry.getValue().entrySet()) {
            boolean wasAdded = false;
            if (addedSet != null) {
               wasAdded = addedSet.contains(pair.getKey());
            }
            OpResource resource = factory.createOpResource();
            resource.setId(pair.getKey());
            if (wasAdded) {
               resource.setTranslate(true);
            }
            resources.add(resource);
            
            List<Serializable> content = resource.getContent();
            content.add(pair.getValue());
         }
      }
      Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
            new Boolean(true));
      marshaller.marshal(factory.createLanguageKit(languageKit), new FileOutputStream(destFile));
   }

   /**
    * @param destMap
    * @param value
    * @return
    * @pre
    * @post
    */
   private HashMap<String, HashSet<String>> complete(HashMap<String, HashMap<String, String>> destMap,
         HashMap<String, HashMap<String, String>> srcMap, HashMap<String, HashSet<String[]>> warningsMap) {
      // remove unused entries
      HashSet<String> toRemove = new HashSet<String>();
      for (Map.Entry<String, HashMap<String, String>> entry : destMap.entrySet()) {
         String key = entry.getKey();
         HashMap<String, String> destValue = entry.getValue();
         
         HashMap<String, String> srcValue = srcMap.get(key);
         if (srcValue == null) {
            System.out.println("removing map for key: "+key);
            toRemove.add(key);
         }
      }
      for (String remove : toRemove) {
         destMap.remove(remove);
      }
      // add non existing ones
      HashMap<String, HashSet<String>> ret = new HashMap<String, HashSet<String>>();
      for (Map.Entry<String, HashMap<String, String>> entry : srcMap.entrySet()) {
         String key = entry.getKey();
         HashMap<String, String> srcValue = entry.getValue();
         
         HashMap<String, String> destValue = destMap.get(key);
         if (destValue == null) {
            destValue = new HashMap<String, String>();
            destMap.put(key, destValue);
         }
         HashSet<String[]> warningsSet = new HashSet<String[]>();
         HashSet<String> addedSet = complete(destValue, srcValue, warningsSet);
         if (!addedSet.isEmpty()) {
            ret.put(key, addedSet);
         }
         if (!warningsSet.isEmpty()) {
            warningsMap.put(key, warningsSet);
         }
      }
      return ret;
   }

   /**
    * @param destValue
    * @param srcValue
    * @return
    * @pre
    * @post
    */
   private HashSet<String> complete(HashMap<String, String> destValue,
         HashMap<String, String> srcValues, HashSet<String[]> warnings) {
      HashSet<String> ret = new HashSet<String>();
      for (Map.Entry<String, String> entry : srcValues.entrySet()) {
         String key = entry.getKey();
         String value = destValue.get(key);
         if (value == null) {
//            System.out.println("adding non existing: "+key);
            ret.add(key);
            destValue.put(key, entry.getValue());
         }
         else {
            if (value.equals(entry.getValue())) { // same values in two languages
               warnings.add(new String[] {key, value});
            }
         }
      }
      return ret;
   }

   /**
    * @param refFile
    * @param lang
    * @return
    * @pre
    * @post
    */
   private File getDestFile(File refFile, String lang) {
      File parent = refFile.getParentFile();
      String name = refFile.getName();
      String extension = "_"+OpResourceTool.this.referenceLang+".olk.xml";
      name = name.substring(0, name.length()-extension.length());
      extension = "_"+lang+".olk.xml";
      name += extension;
      return new File(parent, name);
   }

   /**
    * @param srcPath
    * @param filter
    * @throws JAXBException 
    * @throws FileNotFoundException 
    * @pre
    * @post
    */
   private void process(File path, HashMap<File, HashMap<String, HashMap<String, String>>> map, FilenameFilter filter, FilenameFilter dirFilter) throws FileNotFoundException, JAXBException {
      HashMap<File, HashMap<String, HashMap<String, String>>> ret = new HashMap<File, HashMap<String,HashMap<String,String>>>();
      String[] files = path.list(filter);
      File file;
      for (int pos = 0; pos < files.length; pos++) {
         file = new File(path, files[pos]);
         HashMap<String, HashMap<String, String>> filemap = process(file);
         map.put(file, filemap);
      }
      String[] dirs = path.list(dirFilter);
      for (int pos = 0; pos < dirs.length; pos++) {
         process(new File(path, dirs[pos]), map, filter, dirFilter);
      }
   }

   /**
    * @param file
    * @throws JAXBException 
    * @throws FileNotFoundException 
    * @pre
    * @post
    */
   private HashMap<String, HashMap<String, String>> process(File file) throws FileNotFoundException, JAXBException {
//      System.err.println("processing file: "+file);
      HashMap<String, HashMap<String, String>> resourceMapMap = new HashMap<String, HashMap<String, String>>(); 
      OpLanguageKit kit = readResourceFile(file);
      
      List<OpResourceMap> maps = kit.getResourceMap();
      for (OpResourceMap map : maps) {
         String id = map.getId();
         HashMap<String, String> resourceMap = new HashMap<String, String>(); 
         List<OpResource> resources = map.getResource();
         for (OpResource resource : resources) {
            String resourceId = resource.getId();
            StringBuffer contentBuffer = new StringBuffer();
            List<Serializable> contents = resource.getContent();
            for (Serializable content : contents) {
               if (content instanceof JAXBElement) {
                  JAXBElement<OpParameter> elem = (JAXBElement<OpParameter>) content;
                  contentBuffer.append("<parameter name=\"");
                  contentBuffer.append(elem.getValue().getName());
                  contentBuffer.append("\"/>");
               }
               else {
                  String cont = (String)content;
                  contentBuffer.append((String)content);
               }
            }
            resourceMap.put(resourceId, contentBuffer.toString());
         }
         resourceMapMap.put(id, resourceMap);
      }
//      System.err.println(root.getClass().getName());
      return resourceMapMap;
   }

   /**
    * @param referenceLang
    * @pre
    * @post
    */
   private void setReferenceLang(String referenceLang) {
      this.referenceLang = referenceLang;
   }

   /**
    * @param opSource
    * @pre
    * @post
    */
   private void setOpSourcePath(String opSource) {
      File srcPath = new File(opSource);
      if (!srcPath.exists()) {
         throw new IllegalArgumentException("src path '"+opSource+"' does not exist!");
      }
      this.srcPath = srcPath;
   }
   
   /**
    * @param opSource
    * @pre
    * @post
    */
   private void setOpDestinationPath(String opDestination) {
      File destPath = new File(opDestination);
      if (!destPath.exists()) {
         destPath.mkdirs();
         destPath.mkdir();
      }
      else {
         deleteDir(destPath);
         destPath.mkdir();
      }
      this.destPath = destPath;
   }

   private static boolean deleteDir(File dir) {
      if (dir.isDirectory()) {
          String[] children = dir.list();
          for (int i=0; i<children.length; i++) {
              boolean success = deleteDir(new File(dir, children[i]));
              if (!success) {
                  return false;
              }
          }
      }
  
      // The directory is now empty so delete it
      return dir.delete();
  }
}
