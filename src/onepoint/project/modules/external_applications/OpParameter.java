package onepoint.project.modules.external_applications;

import onepoint.persistence.OpObject;

public class OpParameter extends OpObject {
   
   private String name;
   private String value;
   
   private OpParameter() {
      name = null;
      value = null;
   }
   
   public OpParameter(String name, String value) {
      this.name = name;
      this.value = value;
   }
   
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

}
