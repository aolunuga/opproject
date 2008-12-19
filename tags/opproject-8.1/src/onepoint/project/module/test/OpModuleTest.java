/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module.test;

import junit.framework.TestCase;
import onepoint.project.module.OpModule;
import onepoint.project.module.OpModuleFile;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpMember;
import onepoint.persistence.OpField;
import onepoint.persistence.OpRelationship;

/**
 * Test case for the <code>OpModule</code> class.
 * <FIXME author="Horia Chiorean" description="Add test cases for group, tool, language extensions">
 *
 * @author horia.chiorean
 */
public class OpModuleTest extends TestCase {

   /**
    * Tests that module extension works correctly at the prototype level.
    *
    * @throws Exception if anything fails.
    */
   public void testModuleExtensionPrototypeLevel()
        throws Exception {
      OpModuleFile parentModuleFile = new OpModuleFile();
      parentModuleFile.setFileName("/module/test/test_module_1.oxm.xml");
      OpModule parentModule = parentModuleFile.loadModule();
      assertNotNull("The parent module was not loaded ", parentModule);
      assertEquals("Invalid number of prototypes for the parent module ", 1, parentModule.getPrototypesList().size());

      OpModuleFile childModuleFile = new OpModuleFile();
      childModuleFile.setFileName("/module/test/test_module_2.oxm.xml");
      OpModule childModule = childModuleFile.loadModule();
      assertNotNull("The child module was not loaded", childModule);
      assertEquals("Invalid number of prototypes for the child module ", 1, childModule.getPrototypesList().size());

      OpPrototype childPrototype = childModule.getPrototypesList().get(0);
      assertEquals("The child prototype has a wrong number of members ", 7, childPrototype.getDeclaredSize());

      assertField(childPrototype, "Field1", "Integer");
      assertField(childPrototype, "Field2", "Integer");
      assertField(childPrototype, "Field3", "Integer");
      assertField(childPrototype, "Field4", "String");

      assertRelationship(childPrototype, "Relationship1" , "Type1");
      assertRelationship(childPrototype, "Relationship2" , "Type3");
      assertRelationship(childPrototype, "Relationship3" , "Type4");
   }

   /**
    * Asserts that a field matches the given criterias.
    *
    * @param prototype  a <code>OpPrototype</code> representing the prototype which contains the field.
    * @param fieldName a <code>String</code> representing the name of the field.
    * @param expectedType a <code>String</code> representing the name of the expected field type.
    */
   private void assertField(OpPrototype prototype, String fieldName, String expectedType) {
      OpMember  field = prototype.getDeclaredMember(fieldName);
      assertNotNull("Member not found ", field);
      assertTrue("The member is not a field ", field instanceof OpField);
      assertEquals("The field has an invalid type ", expectedType, field.getTypeName());
   }

   /**
    * Asserts that a relationship matches the given criteria.
    *
    * @param prototype  a <code>OpPrototype</code> representing the prototype which contains the relationship.
    * @param relationshipName a <code>String</code> representing the name of the relationship.
    * @param expectedType a <code>String</code> representing the name of the expected relationship type.
    */
   private void assertRelationship(OpPrototype prototype, String relationshipName, String expectedType) {
      OpMember relationship = prototype.getDeclaredMember(relationshipName);
      assertNotNull("Member not found ", relationship);
      assertTrue("The member is not a relationship ", relationship instanceof OpRelationship);
      assertEquals("The relationship has an invalid type ", expectedType, relationship.getTypeName());
   }

}
