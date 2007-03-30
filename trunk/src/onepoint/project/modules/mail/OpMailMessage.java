/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.mail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

public class OpMailMessage {
	
	public final static String TEXT_PLAIN = "text/plain";
	
	private InternetAddress from;
	private ArrayList tos = new ArrayList();
	private ArrayList ccs = new ArrayList();
	private ArrayList bccs = new ArrayList();
	private String subject;
	protected StringBuffer content = new StringBuffer();
	
	/*
	- message.setContent("Hello", "text/plain")
	- message.setSubject("First")
	- Address address = new InternetAddress("info@onepoint.at", "Info");
	- message.addFrom, addRecipient [Message.RecipientType.*]
	- HTML messages:
		- Simply use "text/html" in setContent()
		- For embedding inline images:
			- Create multi-part message and attach inline images
				- [See tutorial page in C:\java directory]
			- Reference inline images w/ "cid:attachment-id" URL
			- messageBodyPart.setHeader("Content-ID","<attachment-id>");
	*/

	public final void setFrom(String email) throws AddressException {
		from = new InternetAddress(email);
	}
	
	public final InternetAddress getFrom() {
		return from;
	}

   public final void addTo(String email) throws AddressException {
      tos.add(new InternetAddress(email));
   }

	public final void addTo(String email, String name) throws UnsupportedEncodingException{
		tos.add(new InternetAddress(email, name));
	}

   public final Iterator getTos() {
      return tos.iterator();
   }

   public final void addCC(String email) throws AddressException {
      ccs.add(new InternetAddress(email));
   }

	public final void addCC(String email, String name) throws UnsupportedEncodingException {
		ccs.add(new InternetAddress(email, name));
	}

   public final Iterator getCCs() {
      return ccs.iterator();
   }

   public final void addBCC(String email) throws AddressException {
      bccs.add(new InternetAddress(email));
   }

	public final void addBCC(String email, String name) throws UnsupportedEncodingException {
		bccs.add(new InternetAddress(email, name));
	}

   public final Iterator getBCCs() {
      return bccs.iterator();
   }

	public final void setSubject(String subject) {
		this.subject = subject;
	}
	
	public final String getSubject() {
		return subject;
	}
	
	public final void addContent(String content) {
		this.content.append(content);
	}
	
	public final String content() {
		return content.toString();
	}
	
}

