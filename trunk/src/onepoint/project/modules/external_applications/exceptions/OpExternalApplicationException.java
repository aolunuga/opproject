package onepoint.project.modules.external_applications.exceptions;

public class OpExternalApplicationException extends Exception {
   
   public final static int CONNECTION_EXCEPTION = 1;
   public final static int HTTP_EXCEPTION = 2;
   public final static int REQUEST_IO_EXCEPTION = 3;
   public final static int RESPONSE_IO_EXCEPTION = 4;
   public final static int RESPONSE_FORMAT_EXCEPTION = 5;
   public final static int APPLICATION_ERROR_EXCEPTION = 6;
   public final static int INTERNAL_ERROR_EXCEPTION = 99;
   
   private int code = 0;
   private String app;
   
   public OpExternalApplicationException(int code, String app, String msg) {
      super(msg);
      this.code = code;
      this.app = app;
   }

   public OpExternalApplicationException(int code, String app, String msg, Throwable cause) {
      super(msg, cause);
      this.code = code;
      this.app = app;
   }

   public int getCode() {
      return code;
   }

   public String getApp() {
      return app;
   }
   
}
