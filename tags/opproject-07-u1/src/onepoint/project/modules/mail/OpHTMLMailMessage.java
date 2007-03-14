/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.mail;

import java.util.ArrayList;
import java.util.Iterator;

public class OpHTMLMailMessage extends OpMailMessage {

	public final static String TEXT_HTML = "text/html";
	public final static String IMAGE_KEY = "image";

	// *** List of inline images
	private ArrayList _image_file_names = new ArrayList();

	public final void addInlineImage(String image_file_name) {
		// *** Add inline image to list
		_image_file_names.add(image_file_name);
		// *** Add to content w/correct cid-reference and syntax
		_content.append("<img src=\"cid:");
		_content.append(IMAGE_KEY);
		_content.append(_image_file_names.size());
		_content.append("\">");
	}

	public final Iterator getImageFileNames() {
		return _image_file_names.iterator();
	}

}
