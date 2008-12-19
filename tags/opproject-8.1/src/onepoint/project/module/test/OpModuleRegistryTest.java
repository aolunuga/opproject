/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module.test;

import junit.framework.TestCase;
import onepoint.project.module.OpModuleRegistry;
import onepoint.project.module.OpModuleRegistryLoader;
import onepoint.project.module.OpModule;
import onepoint.project.module.OpModuleDependencyException;

import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Test case for the functionality offered by the <code>OpModuleRegistry</code> class.
 *
 * @author horia.chiorean
 */
public class OpModuleRegistryTest extends TestCase {

   /**
    * Module names, as defined in the coressponding *.oxm.xml files.
    */
   private static final String MODULE_1_NAME = "module_1";
   private static final String MODULE_2_NAME = "module_2";
   private static final String MODULE_3_NAME = "module_3";
   private static final String MODULE_4_NAME = "module_4";
   private static final String MODULE_7_NAME = "module_7";

   /**
    * The registry loader used to load the registry files.
    */
   private final OpModuleRegistryLoader registryLoader = new OpModuleRegistryLoader();

   /**
    * Tests that 2 modules without dependency are loaded correctly.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testLoadWithoutDependency()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_1.oxr.xml");
      OpModuleRegistry registry = registryLoader.loadModuleRegistry(registryInputStream);
      Map<String, OpModule> modules = registry.getModules();
      List<OpModule> modulesList = new ArrayList<OpModule>(modules.values());
      assertEquals("Invalid nr of loaded modules:", 2, modules.size());
      OpModule module = modules.get(MODULE_1_NAME);
      assertNotNull(MODULE_1_NAME + "  wasn't found:", module);
      assertEquals("The position of  " + MODULE_1_NAME + " isn't correct:", 0, modulesList.indexOf(module));
      module = modules.get(MODULE_2_NAME);
      assertNotNull(MODULE_2_NAME + " wasn't found:", module);
      assertEquals("The position of  " + MODULE_2_NAME + " isn't correct:", 1, modulesList.indexOf(module));
   }

   /**
    * Tests that 2 modules with a simple (direct) dependency and in correct order in the registry
    * are loaded correctly.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testLoadSimpleDependencyNormalOrder()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_2.oxr.xml");
      OpModuleRegistry registry = registryLoader.loadModuleRegistry(registryInputStream);
      internalTestSimpleDependency(registry);
   }

   /**
    * Tests that 2 modules with a simple (direct) dependency and in reverse order in the registry
    * are loaded correctly.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testLoadSimpleDependencyReverseOrder()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_3.oxr.xml");
      OpModuleRegistry registry = registryLoader.loadModuleRegistry(registryInputStream);
      internalTestSimpleDependency(registry);
   }

   /**
    * Asserts a certain order for the "simple'" dependency tests.
    *
    * @param registry a <code>OpModuleRegistry</code> representing the module registry
    */
   private void internalTestSimpleDependency(OpModuleRegistry registry) {
      registry.loadModules();
      Map<String, OpModule> modules = registry.getModules();
      List<OpModule> modulesList = new ArrayList<OpModule>(modules.values());
      assertEquals("Invalid nr of loaded modules:", 2, modules.size());
      OpModule module = modules.get(MODULE_2_NAME);
      assertNotNull(MODULE_2_NAME + "  wasn't found:", module);
      assertEquals("The position of  " + MODULE_2_NAME + " isn't correct:", 0, modulesList.indexOf(module));
      module = modules.get(MODULE_3_NAME);
      assertNotNull(MODULE_3_NAME + " wasn't found:", module);
      assertEquals("The position of  " + MODULE_3_NAME + " isn't correct:", 1, modulesList.indexOf(module));
   }

   /**
    * Tests that 3 modules transitive dependency are loaded correctly.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testTransitiveDependencies1()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_4.oxr.xml");
      OpModuleRegistry registry = registryLoader.loadModuleRegistry(registryInputStream);
      internalTestTransitiveDependencies(registry);
   }

   /**
    * Tests that 3 modules transitive dependency are loaded correctly.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void tesTransitiveDependencies2()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_5.oxr.xml");
      OpModuleRegistry registry = registryLoader.loadModuleRegistry(registryInputStream);
      internalTestTransitiveDependencies(registry);
   }

   /**
    * Tests that 3 modules transitive dependency are loaded correctly.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testTransitiveDependencies3()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_6.oxr.xml");
      OpModuleRegistry registry = registryLoader.loadModuleRegistry(registryInputStream);
      internalTestTransitiveDependencies(registry);
   }

   /**
    * Tests that 3 modules transitive dependency are loaded correctly.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testMultipleDependencies1()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_9.oxr.xml");
      OpModuleRegistry registry = registryLoader.loadModuleRegistry(registryInputStream);
      internalTestMultipleDependencies(registry);
   }

   /**
    * Tests that 3 modules transitive dependency are loaded correctly.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testMultipleDependencies2()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_10.oxr.xml");
      OpModuleRegistry registry = registryLoader.loadModuleRegistry(registryInputStream);
      internalTestMultipleDependencies(registry);
   }

   /**
    * Asserts a certain order for the "transitive'" dependency tests.
    *
    * @param registry a <code>OpModuleRegistry</code> representing the module registry
    */
   private void internalTestTransitiveDependencies(OpModuleRegistry registry) {
      registry.loadModules();
      Map<String, OpModule> modules = registry.getModules();
      List<OpModule> modulesList = new ArrayList<OpModule>(modules.values());
      assertEquals("Invalid nr of loaded modules:", 3, modules.size());
      OpModule module = modules.get(MODULE_2_NAME);
      assertNotNull(MODULE_2_NAME + "  wasn't found:", module);
      assertEquals("The position of  " + MODULE_2_NAME + " isn't correct:", 0, modulesList.indexOf(module));
      module = modules.get(MODULE_3_NAME);
      assertNotNull(MODULE_3_NAME + " wasn't found:", module);
      assertEquals("The position of  " + MODULE_3_NAME + " isn't correct:", 1, modulesList.indexOf(module));
      module = modules.get(MODULE_4_NAME);
      assertNotNull(MODULE_4_NAME + " wasn't found:", module);
      assertEquals("The position of  " + MODULE_4_NAME + " isn't correct:", 2, modulesList.indexOf(module));
   }

   /**
    * Asserts a certain order for the "multiple'" dependency tests.
    *
    * @param registry a <code>OpModuleRegistry</code> representing the module registry
    */
   private void internalTestMultipleDependencies(OpModuleRegistry registry) {
      registry.loadModules();
      Map<String, OpModule> modules = registry.getModules();
      List<OpModule> modulesList = new ArrayList<OpModule>(modules.values());
      assertEquals("Invalid nr of loaded modules:", 3, modules.size());
      OpModule module = modules.get(MODULE_1_NAME);
      assertNotNull(MODULE_1_NAME + "  wasn't found:", module);
      assertEquals("The position of  " + MODULE_1_NAME + " isn't correct:", 0, modulesList.indexOf(module));
      module = modules.get(MODULE_2_NAME);
      assertNotNull(MODULE_2_NAME + " wasn't found:", module);
      assertEquals("The position of  " + MODULE_2_NAME + " isn't correct:", 1, modulesList.indexOf(module));
      module = modules.get(MODULE_7_NAME);
      assertNotNull(MODULE_7_NAME + " wasn't found:", module);
      assertEquals("The position of  " + MODULE_7_NAME + " isn't correct:", 2, modulesList.indexOf(module));
   }

   /**
    * Tests that a cyclic dependency doesn't work.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testCyclicDependencies()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_7.oxr.xml");
      try {
         registryLoader.loadModuleRegistry(registryInputStream);
      }
      catch (OpModuleDependencyException e) {
         return;
      }
      fail("A OpModuleDependencyException  was expected ");
   }

   /**
    * Tests that a cyclic dependency doesn't work.
    *
    * @throws Exception if anything unexpected happens.
    */
   public void testMissingDependencies()
        throws Exception {
      InputStream registryInputStream = OpModuleRegistryTest.class.getResourceAsStream("test_registry_8.oxr.xml");
      try {
         registryLoader.loadModuleRegistry(registryInputStream);
      }
      catch (OpModuleDependencyException e) {
         return;
      }
      fail("A OpModuleDependencyException  was expected ");
   }

}
