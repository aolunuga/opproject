/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.documents;

import java.util.Set;

import onepoint.persistence.OpObjectIfc;

/**
 * @author dfreis
 *
 */
public interface OpDynamicResourceable extends OpObjectIfc {

   public abstract Set getDynamicResources();

   public abstract void setDynamicResources(Set dynamicResources);

}