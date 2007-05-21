/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import java.util.*;

public class OpModuleRegistry implements Iterable<OpModule>{
   /*list of module files*/
   private List<OpModuleFile> moduleFiles;
   /*insertion order map of modules <module name,OpModule> */
   private Map<String, OpModule> modules;

  public OpModuleRegistry() {
    moduleFiles = new ArrayList<OpModuleFile>();
    modules = new LinkedHashMap<String,OpModule>();
  }

   /**
    * Adds the <code>module_file</code> to list of module files
    *
    * @param module_file <code>OpModuleFile</code>
    */
   final void addModuleFile(OpModuleFile module_file) {
      moduleFiles.add(module_file);
   }

   /**
    * Puts the <code>module</code> to the modules map
    *
    * @param module <code>OpModule</code>
    */
   final void addModule(OpModule module) {
      // *** Check for uniqueness of module name?
      modules.put(module.getName(), module);
   }

  // *** removeModule(String name)

   /**
    * Returns the module with the give the key <code>name</code> form the modules map.
    *
    * @param name <code>String</code> the key whose associated module is to be returned.
    * @return <code>OpModule</code> the value asociated for this key or <code>null</code> if no value with this key exists.
    */
   public final OpModule getModule(String name) {
      return modules.get(name);
   }

   /**
    * Returns an iterator over the module files list
    *
    * @return <code>Iterator</code>
    */
   public final Iterator getModuleFiles() {
      return moduleFiles.iterator();
   }

   /**
    * Returns an iterator over the modules map values (collection of OpModule entries).
    *
    * @return <code>Iterator</code>
    */
   public final Iterator<OpModule> iterator() {
      return modules.values().iterator();
   }

}

