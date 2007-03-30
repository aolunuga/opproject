/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

/**
 * Classs representing a persistence exception.
 *
 * @author horia.chiorean
 */
public class OpPersistenceException extends RuntimeException {

   /**
    * @see Exception#Exception(Throwable)
    */
   public OpPersistenceException(Throwable cause) {
      super(cause);
   }
}
