/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.mail;

import onepoint.express.XExpressProxy;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.script.interpreter.XInterpreterException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

public class OpMailerProxy extends XExpressProxy {

   private static final XLog logger = XLogFactory.getServerLogger(OpMailerProxy.class);

	 // Class names
	 public final static String MAILER = OpMailer.class.getName().intern();
   public final static String MAIL_MESSAGE = OpMailMessage.class.getName().intern();

	 // Method names
   public final static String SEND_MESSAGE = "sendMessage".intern();
   public final static String SET_SMTP_HOST = "setSMTPHost".intern();
   public final static String SET_FROM = "setFrom".intern();
   public final static String ADD_TO = "addTo".intern();
   public final static String SET_SUBJECT = "setSubject".intern();
   public final static String ADD_CONTENT = "addContent".intern();


	// Class name array
	private final static String[] _class_names = { MAILER, MAIL_MESSAGE };

	public String[] getClassNames() {
		return _class_names;
	}

	public Object newInstance(String class_name) throws XInterpreterException {
      if (class_name == MAILER) {
         return new OpMailer();
      } else if (class_name == MAIL_MESSAGE) {
         return new OpMailMessage();
      } else {
         // ERROR
         throw new XInterpreterException("No class name " + class_name + " defined in this proxy");
      }
  }

	// TODO: We need static getters in order to access constants
	// ==> Provide static access to XDuration.DAYS, WEEKS and MONTHS in XDefaultProxy

	public Object invokeMethod(Object object, String method_name, Object[] arguments) throws XInterpreterException {
		if (object instanceof OpMailer) {
			if (method_name == SEND_MESSAGE) {
         if (arguments.length == 1) {
            try {
               ((OpMailer) object).sendMessage((OpMailMessage) (arguments[0]));
            }
            catch (MessagingException e) {
               logger.warn("Unable to send mail message:", e);
            }
         }
         else {
            throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
         }
         return null;
      } else if (method_name == SET_SMTP_HOST) {
         if (arguments.length == 1) {
            // TODO: Method should be static
            OpMailer.setSMTPHostName((String) arguments[0]);
         }
         else {
            throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
         }
         return null;
         }

			else
            throw new XInterpreterException("Class Mailer does not define a method named " + method_name);

		} else if (object instanceof OpMailMessage) {
         if (method_name == SET_FROM) {
            if (arguments.length == 1) {
               try {
                  ((OpMailMessage) object).setFrom((String) (arguments[0]));
               }
               catch (AddressException e) {
                  logger.warn("Error in setting From address in mail message:", e);
               }
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
            return null;
         } else if (method_name == ADD_TO) {
            if (arguments.length == 1) {
               try {
                  ((OpMailMessage) object).addTo((String) (arguments[0]));
               }
               catch (AddressException e) {
                  logger.warn("Error in setting To address in mail message:", e);
               }
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
            return null;
         } else if (method_name == SET_SUBJECT) {
            if (arguments.length == 1) {
               ((OpMailMessage) object).setSubject((String) (arguments[0]));
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
            return null;
         } else if (method_name == ADD_CONTENT) {
            if (arguments.length == 1) {
               ((OpMailMessage) object).addContent((String) (arguments[0]));
            }
            else {
               throw new XInterpreterException("Wrong number of arguments for method " + object.getClass().getName() + "." + method_name);
            }
            return null;
         } else
            throw new XInterpreterException("Class MailMessage does not define a method named " + method_name);

      } else {
			// ERROR
			throw new XInterpreterException("No method " + method_name + " defined in class " + object.getClass().getName());
		}
	}
}
