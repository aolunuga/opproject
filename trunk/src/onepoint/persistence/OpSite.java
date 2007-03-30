/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.Set;

public class OpSite extends OpObject {

   public final static String SITE = "OpSite";

   private String name;
   private Set objects;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setObjects(Set objects) {
      this.objects = objects;
   }

   public Set getObjects() {
      return objects;
   }

}
