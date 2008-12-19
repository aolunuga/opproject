/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.user;

import java.util.Set;

import onepoint.persistence.OpObjectIfc;


/**
 * @author dfreis
 *
 */
public interface OpLockable extends OpObjectIfc {

   public abstract void setLocks(Set<OpLock> locks);

   public abstract Set<OpLock> getLocks();

   public abstract void addLock(OpLock lock);

   public abstract void removeLock(OpLock lock);
}