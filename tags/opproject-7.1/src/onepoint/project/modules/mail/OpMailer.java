/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.mail;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

public class OpMailer implements Runnable {

   private static final XLog log = XLogFactory.getServerLogger(OpMailer.class);

	// *** Maybe more concrete: XSMTPMailer

	// Java Mail string constants
	public final static String SMTP = "smtp";
	public final static String MAIL_SMTP_HOST = "mail.smtp.host";
	public final static String RELATED = "related"; // Related body part
	public final static String CONTENT_ID = "Content-ID";

	private static String smtpHostName = null;

   private ArrayList messages;

   public OpMailer() {}
   
   public OpMailer(ArrayList messages) {
      this.messages = messages;
   }

	public static void setSMTPHostName(String smtp_host_name) {
		smtpHostName = smtp_host_name;
	}

   public static void sendMessageAsynchronous(OpMailMessage message) {
      if (smtpHostName != null) {
         ArrayList messages = new ArrayList();
         messages.add(message);
         Thread t = new Thread(new OpMailer(messages));
         t.start();
      }
   }

   public void run() {
      try {
         sendMessages(messages);
      } catch (Exception e) {
         log.warn("Could not send email: ", e);
      }
   }

	public void sendMessage(OpMailMessage message) throws MessagingException {
		ArrayList messages = new ArrayList();
		messages.add(message);
		sendMessages(messages);
	}

	public void sendMessages(ArrayList messages) throws MessagingException {
		Properties properties = new Properties();
		properties.put(MAIL_SMTP_HOST, smtpHostName);
		// *** TODO: Probably implement simple authenticator (login, password)
		// ==> Maybe don't need it: There is login/password in Transport.connect
		Session session = Session.getDefaultInstance(properties);
		Transport transport = session.getTransport(SMTP);
		// *** TODO: Most probably add identifaction here (if required)
		// ==> Arguments are '_login' and '_password'
		transport.connect(smtpHostName, null, null);
		Iterator i = messages.iterator();
		while (i.hasNext()) {
			OpMailMessage message = (OpMailMessage) (i.next());
			MimeMessage mime_message = new MimeMessage(session);
			// *** Set parameters of mime-message
			InternetAddress[] from = new InternetAddress[1];
			from[0] = message.getFrom();
			mime_message.addFrom(from);
			InternetAddress to = null;
			Iterator tos = message.getTos();
			while (tos.hasNext()) {
				to = (InternetAddress)(tos.next());
				mime_message.addRecipient(Message.RecipientType.TO, to);
			}
         InternetAddress cc = null;
         Iterator ccs = message.getCCs();
         while (ccs.hasNext()) {
            cc = (InternetAddress)(ccs.next());
            mime_message.addRecipient(Message.RecipientType.CC, cc);
         }
         InternetAddress bcc = null;
         Iterator bccs = message.getBCCs();
         while (bccs.hasNext()) {
            bcc = (InternetAddress)(bccs.next());
            mime_message.addRecipient(Message.RecipientType.BCC, bcc);
         }

			mime_message.setSubject(message.getSubject());
			if (message instanceof OpHTMLMailMessage) {
				// Create multi-part message
				OpHTMLMailMessage html_message = (OpHTMLMailMessage)message;
				MimeMultipart multi_part = new MimeMultipart(RELATED);
				// Add content
				BodyPart body_part = new MimeBodyPart();
				body_part.setContent(message.content(), OpHTMLMailMessage.TEXT_HTML);
				multi_part.addBodyPart(body_part);
				// Add inline image attachments
				Iterator image_file_names = html_message.getImageFileNames();
				String image_file_name = null;
				FileDataSource file_data_source = null;
				int image_number = 0;
				StringBuffer inline_image_name = null;
				while (image_file_names.hasNext()) {
					image_file_name = (String)(image_file_names.next());
					image_number ++;
					body_part = new MimeBodyPart();
					file_data_source = new FileDataSource(image_file_name);
					body_part.setDataHandler(new DataHandler(file_data_source));
					inline_image_name = new StringBuffer();
					inline_image_name.append('<');
					inline_image_name.append(OpHTMLMailMessage.IMAGE_KEY);
					inline_image_name.append(image_number);
					inline_image_name.append('>');
					body_part.setHeader(CONTENT_ID, inline_image_name.toString());
					multi_part.addBodyPart(body_part);
				}
			}
			else
				mime_message.setContent(message.content(), OpMailMessage.TEXT_PLAIN);
			// Add content body part
			
			// *** Send message
			transport.sendMessage(mime_message, mime_message.getAllRecipients());
		}
		transport.close();
	}

}
