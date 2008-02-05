/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.persistence.hibernate;

/**
 * @author dfreis
 *
 */
public interface OpEntityEventListener {

   /**
    * @param opevent
    * @pre
    * @post
    */
   void entityChangedEvent(OpEvent opevent);

}
