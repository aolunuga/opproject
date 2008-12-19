/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.mail;

import java.util.ArrayList;
import java.util.Iterator;

public class OpHTMLMailMessage extends OpMailMessage {

	public final static String TEXT_HTML = "text/html";
	public final static String IMAGE_KEY = "image";

	// *** List of inline images
	private ArrayList imageFileNames = new ArrayList();

	/**
	 * 
	 */
	public OpHTMLMailMessage() {
	}

	public final void addInlineImage(String image_file_name) {
		// *** Add inline image to list
		imageFileNames.add(image_file_name);
		// *** Add to content w/correct cid-reference and syntax
		content.append("<img src=\"cid:");
		content.append(IMAGE_KEY);
		content.append(imageFileNames.size());
		content.append("\">");
	}

	public final Iterator getImageFileNames() {
		return imageFileNames.iterator();
	}

}
