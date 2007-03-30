/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

public final class OpLocator {

	public final static String LOCATOR_POSTFIX = "xid";
	public final static char LOCATOR_SEPARATOR = '.';

	private OpPrototype _prototype;
	private long _id;

	public OpLocator(OpObject object) {
		_prototype = object.getPrototype();
		_id = object.getID();
	}

	public OpLocator(OpPrototype prototype, long id) {
		_prototype = prototype;
		_id = id;
	}

	public final OpPrototype getPrototype() {
		return _prototype;
	}

	public final long getID() {
		return _id;
	}

	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(_prototype.getName());
		buffer.append(LOCATOR_SEPARATOR);
		buffer.append(_id);
		buffer.append(LOCATOR_SEPARATOR);
		buffer.append(LOCATOR_POSTFIX);
		return buffer.toString();
	}

   public static String locatorString(OpObject object) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(object.getPrototype().getName());
      buffer.append(LOCATOR_SEPARATOR);
      buffer.append(object.getID());
      buffer.append(LOCATOR_SEPARATOR);
      buffer.append(LOCATOR_POSTFIX);
      return buffer.toString();
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
		if (separator_index1 == -1)
			return null;
		String prototype_name = s.substring(0, separator_index1);
		OpPrototype prototype = OpTypeManager.getPrototype(prototype_name);
		if (prototype == null)
			return null;
		int separator_index2 = s.indexOf(LOCATOR_SEPARATOR, separator_index1 + 1);
		if (separator_index2 == -1)
			return null;
		long id = Long.parseLong(s.substring(separator_index1 + 1, separator_index2));
		if (!s.substring(separator_index2 + 1, separator_index2 + 4).equals(LOCATOR_POSTFIX))
			return null;
		return new OpLocator(prototype, id);
	}

}
