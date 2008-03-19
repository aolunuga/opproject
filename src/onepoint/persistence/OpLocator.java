/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

public final class OpLocator {

   public final static String LOCATOR_POSTFIX = "xid";
   public final static char LOCATOR_SEPARATOR = '.';

   private OpPrototype prototype;
   private long id;

   public OpLocator(OpObject object) {
      prototype = OpTypeManager.getPrototypeForObject(object);
      id = object.getID();
   }

   public OpLocator(OpPrototype prototype, long id) {
      this.prototype = prototype;
      this.id = id;
   }

   public final OpPrototype getPrototype() {
      return prototype;
   }

   public final long getID() {
      return id;
   }

   public final String toString() {
      return locatorString(prototype, id);
   }

   public static String locatorString(OpObject object) {
      return locatorString(OpTypeManager.getPrototypeForObject(object), object.getID());
   }

   public static String locatorString(OpPrototype prototype, long id) {
      return locatorString(prototype.getName(), id);
   }

   public static String locatorString(String prototypeName, long id) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(prototypeName);
      buffer.append(LOCATOR_SEPARATOR);
      buffer.append(id);
      buffer.append(LOCATOR_SEPARATOR);
      buffer.append(LOCATOR_POSTFIX);

      return buffer.toString();
   }

   public static OpLocator parseLocator(String s) {
      // Locator format: "<prototype-name>.<id>.xid"
      int separator_index1 = s.indexOf(LOCATOR_SEPARATOR);
      if (separator_index1 == -1) {
         return null;
      }
      String prototype_name = s.substring(0, separator_index1);
      OpPrototype prototype = OpTypeManager.getPrototype(prototype_name);
      if (prototype == null) {
         return null;
      }
      int separator_index2 = s.indexOf(LOCATOR_SEPARATOR, separator_index1 + 1);
      if (separator_index2 == -1) {
         return null;
      }
      long id = Long.parseLong(s.substring(separator_index1 + 1, separator_index2));
      if (!s.substring(separator_index2 + 1, separator_index2 + 4).equals(LOCATOR_POSTFIX)) {
         return null;
      }
      return new OpLocator(prototype, id);
   }

   /**
    * Checks if the specified string is a valid locator
    *
    * @param locator the string to verify, can be null
    * @return true if the string respect the locator format.
    */
   public static boolean validate(String locator) {
      return !(locator == null || locator.length() == 0) && parseLocator(locator) != null;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      OpLocator other = (OpLocator) obj;
      return (this.id == other.id && prototype.getName().equals(other.prototype.getName()));
   }
}
