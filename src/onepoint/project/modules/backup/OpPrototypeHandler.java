/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpTypeManager;
import onepoint.project.util.Quadruple;
import onepoint.project.util.Triple;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

/**
 * Class that handles the parsing of <prototype> entities.
 */
public class OpPrototypeHandler implements XNodeHandler {

	/**
	 * This class's logger.
	 */
	private static final XLog logger = XLogFactory.getLogger(OpPrototypeHandler.class);

	/**
	 * @see XNodeHandler#newNode(onepoint.xml.XContext,String,java.util.HashMap)
	 */
	public Object newNode(XContext context, String name, HashMap attributes) {
		// Add empty backup member list with prototype name to restore context
		String prototypeName = (String) attributes.get(OpBackupManager.NAME);
		((OpRestoreContext) context).registerPrototype(prototypeName);
		return prototypeName;
	}

	/**
	 * @see XNodeHandler#addChildNode(onepoint.xml.XContext,Object,String,Object)
	 */
	public void addChildNode(XContext context, Object node, String child_name, Object child) {
		// Add backup members generated from field and relationship handlers
		List backupMembers = ((OpRestoreContext) context).getBackupMembers((String) node);
		backupMembers.add((OpBackupMember) child);
	}

	/**
	 * @see XNodeHandler#addNodeContent(onepoint.xml.XContext,Object,String)
	 */
	public void addNodeContent(XContext context, Object node, String content) {
	}

	/**
	 * @see XNodeHandler#nodeFinished(onepoint.xml.XContext,String,Object,Object)
	 */
	public void nodeFinished(XContext context, String name, Object node, Object parent) {
		// Iterate backup-members and set accessor methods
		String prototypeName = (String) node;
		List backupMembers = ((OpRestoreContext) context).getBackupMembers(prototypeName);
		OpPrototype prototype = OpTypeManager.getPrototype(prototypeName);
		if (prototype == null) {
			logger.error("No prototype named " + prototypeName + ". Will skip this prototype.");
		}
		Class accesorArgument = null;
		for (int i = 0; i < backupMembers.size(); i++) {
			OpBackupMember backupMember = (OpBackupMember) backupMembers.get(i);
			if (backupMember.relationship) {
				OpPrototype targetPrototype = OpTypeManager.getPrototypeByID(backupMember.typeId);
				if (targetPrototype != null) {
					accesorArgument = targetPrototype.getInstanceClass();
				}
				else {
					throw new OpBackupException("Unsupported prototype ID " + backupMember.typeId + " for " + prototypeName
							+ "." + backupMember.name);
				}
			}
			else {
				accesorArgument = OpBackupTypeManager.getPrimitiveJavaType(backupMember.typeId);
				if (accesorArgument == null) {
					throw new OpBackupException("Unsupported type ID " + backupMember.typeId + " for " + prototypeName
							+ "." + backupMember.name);
				}
			}
			//we should be somewhat graceful. It may happen, that entities vanish...
			if (prototype != null) {
				// Cache accessor method
				// (Note that we assume that persistent member names start with an upper-case letter)
				Class prototypeClass = prototype.getInstanceClass();
				while (true) {
					try {
						Triple<Class, String, String> oldMethod = new Triple<Class, String, String>(prototypeClass, "set"+backupMember.name, backupMember.typeString);
						Quadruple<Class, String, Class, String> newMethod = OpBackupTypeManager.getMappedMethod(prototypeClass, "set"+backupMember.name, backupMember.typeString);
						if (newMethod != null) {
							if (newMethod.getFirst() == null && newMethod.getSecond() == null && newMethod.getThird() == null && newMethod.getFourth() == null) { // method deleted
								backupMember.wasDeleted = true;
								break;
							}
							else {
								try {
									backupMember.accessor = newMethod.getFirst().getDeclaredMethod(newMethod.getSecond(), newMethod.getThird());
									backupMember.accessor.setAccessible(true);
									backupMember.convertMethod = newMethod.getFourth();
								}
								catch (NoSuchMethodException e) {
									backupMember.accessor = newMethod.getFirst().getMethod(newMethod.getSecond(), newMethod.getThird());
									backupMember.convertMethod = newMethod.getFourth();
								}
							}
						}
						else {
							try {
								backupMember.accessor = oldMethod.getFirst().getDeclaredMethod("set"+backupMember.name, accesorArgument);
								backupMember.accessor.setAccessible(true);
								backupMember.convertMethod = null;
							}
							catch (NoSuchMethodException e) {
								backupMember.accessor = oldMethod.getFirst().getMethod("set"+backupMember.name, accesorArgument);
								backupMember.convertMethod = null;
							}
						}
						break;
					}
					catch (NoSuchMethodException e) {
						// maybe method moved to super class
						if (prototypeClass == Object.class) {
							break;
						}
						prototypeClass = prototypeClass.getSuperclass();
					}
				}
				if (prototypeClass == Object.class && !backupMember.wasDeleted) {
					this.findAlternativeAccessors(prototype, backupMember);
				}
			}
			else {
				logger.error("cannot handle '" + prototypeName + "' as the corresponding prototype is missing in this version");
			}
		}
	}

	/**
	 * Tries to find accessors for a given backup memeber, in the event that the normal
	 * flow can't find any (most likely there was a type change).
	 *
	 * @param prototype    a <code>OpPrototype</code> instance.
	 * @param backupMember a <code>OpBackupMember</code> which is being searched.
	 */
	private void findAlternativeAccessors(OpPrototype prototype, OpBackupMember backupMember) {
		String methodName = "set" + backupMember.name;

		//try normal java types (for the set type id)
		Class accesorArgument = OpBackupTypeManager.getJavaType(backupMember.typeId);
		if (accesorArgument != null) {
			Class prototypeClass = prototype.getInstanceClass();
			while (true) {
				// try public methods
				try {
					backupMember.accessor = prototypeClass.getMethod(methodName, accesorArgument);
					return;
				}
				catch (NoSuchMethodException e) {
				}
				try {
					backupMember.accessor = prototypeClass.getDeclaredMethod(methodName, accesorArgument);
					backupMember.accessor.setAccessible(true);
					return;
				}
				catch (NoSuchMethodException e) {            	
					prototypeClass = prototypeClass.getSuperclass();
					if (prototypeClass == Object.class) {
						break;
					}
				}
			}
			logger.debug("No accessor found using type " + accesorArgument);
		}

		//try to see if we have a type change (and possibly a backup member change)
		Map<Integer, Class> typesMap = OpBackupTypeManager.getTypeJavaTypeMap();
		for (int type : typesMap.keySet()) {
			Class javaType = typesMap.get(type);
			try {
				backupMember.accessor = prototype.getInstanceClass().getMethod(methodName, javaType);
				backupMember.relationship = false;
				backupMember.typeId = type;
				return;
			}
			catch (NoSuchMethodException e) {
				logger.debug("No accessor found using type " + javaType);
			}
		}

		//try using all the primitives
		typesMap = OpBackupTypeManager.getTypePrimitiveJavaTypeMap();
		for (int type : typesMap.keySet()) {
			Class javaType = typesMap.get(type);
			try {
				backupMember.accessor = prototype.getInstanceClass().getMethod(methodName, javaType);
				backupMember.relationship = false;
				backupMember.typeId = type;
				return;
			}
			catch (NoSuchMethodException e) {
				logger.debug("No accessor found using type " + javaType);
			}
		}

		logger.error("No accessor found for " + methodName + "("+backupMember.typeString+") in class " + prototype.getName());
	}
}
