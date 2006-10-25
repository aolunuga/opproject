/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.project.OpProjectSession;

import java.util.*;

public class OpModule {

   private String _name;
   private String _caption;
   private String _version;
   private ArrayList _prototype_files;
   private ArrayList _prototypes;
   private ArrayList _service_files;
   private ArrayList _services;
   private ArrayList _language_kit_files;
   private ArrayList _language_kits;
   private ArrayList _tools;
   private ArrayList _groups;
   private String extendedModule;
   private Map toolsMap = new HashMap();
   private Map groupsMap = new HashMap();

   final void setName(String name) {
      _name = name;
      _prototype_files = new ArrayList();
      _prototypes = new ArrayList();
      _service_files = new ArrayList();
      _services = new ArrayList();
      _language_kit_files = new ArrayList();
      _language_kits = new ArrayList();
      _tools = new ArrayList();
      _groups = new ArrayList();
   }

   public final String getName() {
      return _name;
   }

   final public void setCaption(String caption) {
      _caption = caption;
   }

   public final String getCaption() {
      return _caption;
   }

   final public void setVersion(String version) {
      _version = version;
   }

   public final String getVersion() {
      return _version;
   }

   final void setPrototypeFiles(ArrayList prototype_files) {
      _prototype_files = prototype_files;
   }

   public final Iterator getPrototypeFiles() {
      return _prototype_files.iterator();
   }

   final void setPrototypes(ArrayList prototypes) {
      _prototypes = prototypes;
   }

   public final Iterator getPrototypes() {
      return _prototypes.iterator();
   }

   final void setServiceFiles(ArrayList service_files) {
      _service_files = service_files;
   }

   public final Iterator getServiceFiles() {
      return _service_files.iterator();
   }

   final void setServices(ArrayList services) {
      _services = services;
   }

   public final Iterator getServices() {
      return _services.iterator();
   }

   final void setLanguageKitFiles(ArrayList language_kit_files) {
      _language_kit_files = language_kit_files;
   }

   public final Iterator getLanguageKitFiles() {
      return _language_kit_files.iterator();
   }

   final void setLanguageKits(ArrayList language_kits) {
      _language_kits = language_kits;
   }

   public final Iterator getLanguageKits() {
      return _language_kits.iterator();
   }

   final void setTools(ArrayList tools) {
      _tools = tools;
   }

   public final Iterator getTools() {
      return _tools.iterator();
   }

   final void setGroups(ArrayList groups) {
      _groups = groups;
   }

   public final Iterator getGroups() {
      return _groups.iterator();
   }

   public String getExtendedModule() {
      return extendedModule;
   }

   public void setExtendedModule(String extendedModule) {
      this.extendedModule = extendedModule;
   }

   public Map getToolsMap() {
      if (toolsMap.size() != _tools.size()) {
         for (Iterator it = _tools.iterator(); it.hasNext();) {
            OpTool opTool = (OpTool) it.next();
            toolsMap.put(opTool.getName(), opTool);
         }
      }
      return toolsMap;
   }

   public Map getGroupsMap() {
      if (groupsMap.size() != _groups.size()) {
         for (Iterator it = _groups.iterator(); it.hasNext();) {
            OpToolGroup opToolGroup = (OpToolGroup) it.next();
            groupsMap.put(opToolGroup.getName(), opToolGroup);
         }
      }
      return groupsMap;
   }

   public List getLanguageKitsList() {
      return _language_kits;
   }

   public List getServicesList() {
      return _services;
   }

   public List getPrototypesList() {
      return _prototypes;
   }

   public List getGroupsList() {
      return _groups;
   }

   public List getToolsList() {
      return _tools;
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

   /**
    * Performs custom operations after the module parts have been loaded.
    *
    * @throws OpModuleException if something should fail in the post-load operation.
    */
   public void postLoad()
        throws OpModuleException {
   }
}
