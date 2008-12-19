/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.persistence;

import java.sql.Timestamp;

/**
 * @author dfreis
 *
 */
public interface OpObjectIfc extends OpLocatable{

   public abstract void setCreated(Timestamp created);

   public abstract Timestamp getCreated();

   public abstract void setModified(Timestamp modified);

   public abstract Timestamp getModified();

}