/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.mail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

public class OpMailMessage {
	
	public final static String TEXT_PLAIN = "text/plain";
	
	private InternetAddress _from;
	private ArrayList _tos = new ArrayList();
	private ArrayList _ccs = new ArrayList();
	private ArrayList _bccs = new ArrayList();
	private String _subject;
	protected StringBuffer _content = new StringBuffer();
	
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
		_from = new InternetAddress(email);
	}
	
	public final InternetAddress getFrom() {
		return _from;
	}

   public final void addTo(String email) throws AddressException {
      _tos.add(new InternetAddress(email));
   }

	public final void addTo(String email, String name) throws UnsupportedEncodingException{
		_tos.add(new InternetAddress(email, name));
	}

   public final Iterator getTos() {
      return _tos.iterator();
   }

   public final void addCC(String email) throws AddressException {
      _ccs.add(new InternetAddress(email));
   }

	public final void addCC(String email, String name) throws UnsupportedEncodingException {
		_ccs.add(new InternetAddress(email, name));
	}

   public final Iterator getCCs() {
      return _ccs.iterator();
   }

   public final void addBCC(String email) throws AddressException {
      _bccs.add(new InternetAddress(email));
   }

	public final void addBCC(String email, String name) throws UnsupportedEncodingException {
		_bccs.add(new InternetAddress(email, name));
	}

   public final Iterator getBCCs() {
      return _bccs.iterator();
   }

	public final void setSubject(String subject) {
		_subject = subject;
	}
	
	public final String getSubject() {
		return _subject;
	}
	
	public final void addContent(String content) {
		_content.append(content);
	}
	
	public final String content() {
		return _content.toString();
	}
	
}

