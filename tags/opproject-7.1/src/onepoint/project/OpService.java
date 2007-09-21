/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project;

/**
 * @author dfreis
 * Interface providing information concerning services.
 * Interface implemented by all Service Impls.
 */
public interface OpService {

   /**
    * gets the name of this service.
    * @return the Name of this service
    */
   public String getName();
}
