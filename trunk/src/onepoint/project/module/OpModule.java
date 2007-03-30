/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.project.OpProjectSession;

import java.util.*;

public class OpModule {

   private String name;
   private String caption;
   private String version;
   private List prototypeFiles;
   private List prototypes;
   private List serviceFiles;
   private List services;
   private List languageKitPaths;
   private List languageKits;
   private List tools;
   private List groups;
   private String extendedModule;
   private Map toolsMap;
   private Map groupsMap;

   public OpModule() {
      prototypeFiles = new ArrayList();
      prototypes = new ArrayList();
      serviceFiles = new ArrayList();
      services = new ArrayList();
      languageKitPaths = new ArrayList();
      languageKits = new ArrayList();
      tools = new ArrayList();
      groups = new ArrayList();
      toolsMap = new HashMap();
      groupsMap = new HashMap();
   }

   final void setName(String name) {
      this.name = name;
   }

   public final String getName() {
      return name;
   }

   final public void setCaption(String caption) {
      this.caption = caption;
   }

   public final String getCaption() {
      return caption;
   }

   final public void setVersion(String version) {
      this.version = version;
   }

   public final String getVersion() {
      return version;
   }

   final void setPrototypeFiles(List prototype_files) {
      prototypeFiles = prototype_files;
   }

   public final Iterator getPrototypeFiles() {
      return prototypeFiles.iterator();
   }

   final void setPrototypes(List prototypes) {
      this.prototypes = prototypes;
   }

   public final Iterator getPrototypes() {
      return prototypes.iterator();
   }

   final void setServiceFiles(List service_files) {
      serviceFiles = service_files;
   }

   public final Iterator getServiceFiles() {
      return serviceFiles.iterator();
   }

   final void setServices(List services) {
      this.services = services;
   }

   public final Iterator getServices() {
      return services.iterator();
   }

   final void setLanguageKitPaths(List language_kit_paths) {
      languageKitPaths = language_kit_paths;
   }

   public final Iterator getLanguageKitPaths() {
      return languageKitPaths.iterator();
   }

   final void setLanguageKits(List language_kits) {
      languageKits = language_kits;
   }

   public final Iterator getLanguageKits() {
      return languageKits.iterator();
   }

   final void setTools(List tools) {
      this.tools = tools;
   }

   public final Iterator getTools() {
      return tools.iterator();
   }

   final void setGroups(List groups) {
      this.groups = groups;
   }

   public final Iterator getGroups() {
      return groups.iterator();
   }

   public String getExtendedModule() {
      return extendedModule;
   }

   public void setExtendedModule(String extendedModule) {
      this.extendedModule = extendedModule;
   }

   public Map getToolsMap() {
      if (toolsMap.size() != tools.size()) {
         for (Iterator it = tools.iterator(); it.hasNext();) {
            OpTool opTool = (OpTool) it.next();
            toolsMap.put(opTool.getName(), opTool);
         }
      }
      return toolsMap;
   }

   public Map getGroupsMap() {
      if (groupsMap.size() != groups.size()) {
         for (Iterator it = groups.iterator(); it.hasNext();) {
            OpToolGroup opToolGroup = (OpToolGroup) it.next();
            groupsMap.put(opToolGroup.getName(), opToolGroup);
         }
      }
      return groupsMap;
   }

   public List getLanguageKitsList() {
      return languageKits;
   }

   public List getServicesList() {
      return services;
   }

   public List getPrototypesList() {
      return prototypes;
   }

   public List getGroupsList() {
      return groups;
   }

   public List getToolsList() {
      return tools;
   }

   public void install(OpProjectSession session) {
   }

   public void remove(OpProjectSession session) {
   }

   public void setup(OpProjectSession session) {
   }

   public void start(OpProjectSession session) {
   }

   public void stop(OpProjectSession session) {
   }

   /**
    * Upgrades a module from a previous version to a new version.
    *
    * @param session   a <code>OpProjectSession</code> representing a server session.
    * @param dbVersion The current version of the DB.
    */
   public void upgrade(OpProjectSession session, int dbVersion) {
   }

}
