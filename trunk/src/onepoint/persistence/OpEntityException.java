package onepoint.persistence;

/**
 * Entity exception. Used by entities at validation time.
 *
 * @author : mihai.costin
 */
public class OpEntityException extends RuntimeException {

   /**
    * Exception error code.
    */
   private int errorCode;

   public OpEntityException(int code) {
      super("Error Code : " + code);
      errorCode = code;
   }

   /**
    * @return The wrapped error code.
    */
   public int getErrorCode() {
      return errorCode;
   }
}
