/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.resource.XLanguageKit;
import onepoint.resource.XLocaleManager;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;

import java.util.*;

public abstract class OpModule {

   /**
    * This class' logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpModule.class);

   private String name;
   private String caption;
   private String version;
   private List<OpPrototypeFile> prototypeFiles;
   private List<OpPrototype> prototypes;
   private List<OpServiceFile> serviceFiles;
   private List<XService> services;
   private List<OpLanguageKitPath> languageKitPaths;
   private List<XLanguageKit> languageKits;
   private List<OpTool> tools;
   private List<OpToolGroup> toolGroups;
   private String extendedModule;
   private Map<String, OpTool> toolsMap;
   private Map<String, OpToolGroup> toolGroupsMap;
   private Map<String, OpPrototype> prototypesMap;

   /**
    * A list of module names this module depends on
    */
   private Set<String> dependencies;

   public OpModule() {
      prototypeFiles = new ArrayList<OpPrototypeFile>();
      prototypes = new ArrayList<OpPrototype>();
      serviceFiles = new ArrayList<OpServiceFile>();
      services = new ArrayList<XService>();
      languageKitPaths = new ArrayList<OpLanguageKitPath>();
      languageKits = new ArrayList<XLanguageKit>();
      tools = new ArrayList<OpTool>();
      toolGroups = new ArrayList<OpToolGroup>();
      toolsMap = new HashMap<String, OpTool>();
      toolGroupsMap = new HashMap<String, OpToolGroup>();
      dependencies = new HashSet<String>();
      prototypesMap = new HashMap<String, OpPrototype>();
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

   public String getVersion() {
      return version;
   }

   final void setPrototypeFiles(List<OpPrototypeFile> prototype_files) {
      prototypeFiles = prototype_files;
   }

   final void setServiceFiles(List<OpServiceFile> serviceFiles) {
      this.serviceFiles = serviceFiles;
   }

   final void setLanguageKitPaths(List<OpLanguageKitPath> languageKitPaths) {
      this.languageKitPaths = languageKitPaths;
   }

   public final Iterator<XLanguageKit> getLanguageKits() {
      return languageKits.iterator();
   }

   final void setTools(List<OpTool> tools) {
      this.tools = tools;
   }

   public final Iterator<OpTool> getTools() {
      return tools.iterator();
   }

   final void setToolGroups(List<OpToolGroup> toolGroups) {
      this.toolGroups = toolGroups;
   }

   public final Iterator<OpToolGroup> getToolGroups() {
      return toolGroups.iterator();
   }

   public String getExtendedModule() {
      return extendedModule;
   }

   public void setExtendedModule(String extendedModule) {
      this.extendedModule = extendedModule;
   }

   private Map<String, OpTool> getToolsMap() {
      if (toolsMap.size() != tools.size()) {
         for (OpTool opTool : tools) {
            toolsMap.put(opTool.getName(), opTool);
         }
      }
      return toolsMap;
   }

   private Map<String, OpToolGroup> getToolGroupsMap() {
      if (toolGroupsMap.size() != toolGroups.size()) {
         for (OpToolGroup opToolGroup : toolGroups) {
            toolGroupsMap.put(opToolGroup.getName(), opToolGroup);
         }
      }
      return toolGroupsMap;
   }

   public List<XLanguageKit> getLanguageKitsList() {
      return languageKits;
   }

   public List<XService> getServicesList() {
      return services;
   }

   public List<OpPrototype> getPrototypesList() {
      return prototypes;
   }

   /**
    * Gets the list of dependencies for this module.
    *
    * @return a <code>List(String)</code> representing a list of module names.
    */
   public Set<String> getDependencies() {
      return dependencies;
   }

   /**
    * Sets the list of dependencies for this module.
    *
    * @param dependencies a <code>List(String)</code> representing a list of module names.
    */
   public void setDependencies(Set<String> dependencies) {
      this.dependencies = dependencies;
   }

   public void start(OpProjectSession session) {
   }

   public void stop(OpProjectSession session) {
   }

   /**
    * Registers this module into the application.
    */
   public void register() {
      this.registerPrototypes();
      this.registerServices();
      this.registerLanguageKits();
      this.registerToolGroups();
      this.registerTools();
   }

   /**
    * Registers this module's prototypes with the type manager.
    */
   private void registerPrototypes() {
      for (OpPrototype prototype : this.prototypes) {
         OpTypeManager.registerPrototype(prototype);
      }
   }

   /**
    * Registers this module's services with the service manager.
    */
   private void registerServices() {
      for (XService service : this.services) {
         XServiceManager.registerService(service);
      }
   }

   /**
    * Registers this module's language kits with the locale manager.
    */
   private void registerLanguageKits() {
      for (XLanguageKit languageKit : this.languageKits) {
         if (this.getExtendedModule() != null) {
            XLocaleManager.registerOverriddingLanguageKit(languageKit, false);
         }
         else {
            XLocaleManager.registerLanguageKit(languageKit);
         }
      }
   }

   /**
    * Registers this module's groups.
    */
   private void registerToolGroups() {
      for (OpToolGroup toolGroup : this.toolGroups) {
         OpToolManager.registerGroup(toolGroup);
      }
   }

   /**
    * Registers this module's tools.
    */
   private void registerTools() {
      for (OpTool tool : this.tools) {
         if (tool.getGroupRef() != null) {
            tool.setGroup(OpToolManager.getGroup(tool.getGroupRef()));
         }
         if (tool.isMultiUserOnly() == null || (tool.isMultiUserOnly().booleanValue() && OpEnvironmentManager.isMultiUser())) {
            OpToolManager.registerTool(tool);
         }
      }
   }

   /**
    * Loads this module's parts
    */
   void loadParts() {
      this.loadPrototypes();
      this.loadLanguageKits();
      this.loadServices();
   }

   /**
    * Loads this module prototypes.
    */
   private void loadPrototypes() {
      // Load prototypes
      logger.info("Loading prototypes of module '" + this.getName() + "'...");
      this.prototypesMap.clear();
      for (OpPrototypeFile prototypeFile : prototypeFiles) {
         OpPrototype prototype = prototypeFile.loadPrototype();
         prototypes.add(prototype);
         this.prototypesMap.put(prototype.getName(), prototype);
      }
      logger.info("Prototypes of module '" + this.getName() + "' loaded.");
   }

   /**
    * Loads this module's language kits.
    */
   private void loadLanguageKits() {
      // Load language-kits
      logger.info("Loading language-kits of module '" + this.getName() + "'...");
      for (OpLanguageKitPath languageKitPath : languageKitPaths) {
         languageKits.addAll(languageKitPath.loadLanguageKits());
      }
      logger.info("Language-kits of module '" + this.getName() + "' loaded.");
   }

   private void loadServices() {
      logger.info("Loading services of module '" + this.getName() + "'...");
      for (OpServiceFile serviceFile : this.serviceFiles) {
         services.add(serviceFile.loadService());
      }
      logger.info("Services of module '" + this.getName() + "' loaded.");
   }

   /**
    * Extends this module with another module, merging all "internal" data like: prototypes,
    * tools, groups etc.
    *
    * @param parentModule a <code>OpModule</code> which has to be extended.
    */
   void extend(OpModule parentModule) {
      this.languageKits.addAll(0, parentModule.getLanguageKitsList());
      this.services.addAll(0, parentModule.getServicesList());

      this.extendPrototypes(parentModule);
      this.extendTools(parentModule);
      this.extendToolGroups(parentModule);
   }

   /**
    * Extends the prototypes from the parent module.
    *
    * @param parentModule a <code>OpModule</code> representing the parent module.
    */
   private void extendPrototypes(OpModule parentModule) {
      List<OpPrototype> parentPrototypes = parentModule.getPrototypesList();
      for (OpPrototype parentPrototype : parentPrototypes) {
         OpPrototype currentPrototype = this.prototypesMap.get(parentPrototype.getName());
         if (currentPrototype == null) {
            this.prototypes.add(parentPrototype);
            this.prototypesMap.put(parentPrototype.getName(), parentPrototype);
         }
         else {
            currentPrototype.extend(parentPrototype);
         }
      }
   }

   /**
    * Extends the tool groups from the parent module.
    *
    * @param parentModule a <code>OpModule</code> representing the parent module.
    */
   private void extendToolGroups(OpModule parentModule) {
      Iterator<OpToolGroup> it = parentModule.getToolGroups();
      while (it.hasNext()) {
         OpToolGroup parentToolGroup = it.next();
         if (!(this.getToolGroupsMap().containsKey(parentToolGroup.getName()))) {
            this.toolGroups.add(parentToolGroup);
         }
      }
   }

   /**
    * Extends this module's tools with the tools from the parent.
    *
    * @param parentModule a <code>OpModule</code> representing the parent module.
    */
   private void extendTools(OpModule parentModule) {
      Iterator<OpTool> it = parentModule.getTools();
      while (it.hasNext()) {
         OpTool parentTool = it.next();
         if (!(this.getToolsMap().containsKey(parentTool.getName()))) {
            this.tools.add(parentTool);
         }
      }
   }

   /**
    * Checks this module using the given server session on the same thread as the caller.
    * @param session a <code>OpProjectSession</code> the server session
    */
   public void check(OpProjectSession session) {
      for (OpModuleChecker moduleChecker : getCheckerList()) {
         moduleChecker.check(session);
      }
   }

   public List<OpModuleChecker> getCheckerList() {
      return new ArrayList<OpModuleChecker>();
   }

}
