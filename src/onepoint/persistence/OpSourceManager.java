/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.Hashtable;

public class OpSourceManager {

	private static Hashtable sources; // Mapping of source names to mounted object sources
	private static OpSource defaultSource; // Default object source

	// In the future maybe JNDI-lookup instead of static?

	static {
		sources = new Hashtable();
		defaultSource = null;
	}

	public static void registerSource(OpSource source) {
		// To do: Check for uniqueness of name
		if (source.getName() != null)
			sources.put(source.getName(), source);
		// Invoke on-register callback
		source.onRegister();
	}

	public static void setDefaultSource(OpSource source) {
		// *** Check if source is registered
		defaultSource = source;
	}

	public static OpSource getDefaultSource() {
		return defaultSource;
	}

	public static OpSource getSource(String name) {
		return (OpSource) (sources.get(name));
	}

	// Where to mount data sources (in sessions or here, more globally)?
	// ==> Maybe both: Global for everyone and just have to be opened in a session?!
	// ==> Master/default-source should be defined here

	// ==> We could mimik Unix here: Only "root/system" is allowed to mount/unmount
	// (in a session, but it is "remembered" persistently in the broker)

}
