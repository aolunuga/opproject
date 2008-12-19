/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentVersion;
import onepoint.project.modules.settings.OpSetting;
import onepoint.service.XSizeInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class OpContent extends OpObject {

	public final static String CONTENT = "OpContent";
	public final static String MEDIA_TYPE = "MediaType";
	public final static String ATTACHMENTS = "Attachments";
	public final static String ATTACHMENT_VERSIONS = "AttachmentVersions";
	public final static String DOCUMENT_NODES = "DocumentNodes";
	public final static String DOCUMENTS = "Documents";
	public final static String SIZE = "Size";
	public final static String STREAM = "Stream";

	private int refCount;
	private String mediaType; // MIME-type
	private long size;
	private XSizeInputStream stream;
	private Set<OpAttachment> attachments = new HashSet<OpAttachment>();
	private Set<OpAttachmentVersion> attachmentVersions = new HashSet<OpAttachmentVersion>();
	private Set<OpDocumentNode> documentNodes = new HashSet<OpDocumentNode>();
	private Set<OpDocument> documents = new HashSet<OpDocument>();
	private Set<OpSetting> settings;

	/**
	 * Default constructor
	 */
	@Deprecated
	public OpContent() {
		// requiered by Hibernate and back-up
	}

	/**
	 * Create a new instance of <code>OpContent</code> based on the specified stream
	 *
	 * @param stream an <code>XSizeInputStream</code> instance
	 */
	public OpContent(XSizeInputStream stream) {
		this.stream = stream;
		this.size = stream == null ? 0 : stream.getSize();
	}

	/**
	 * Create a new instance of <code>OpContent</code> based on the specified stream and the specified size
	 *
	 * @param stream the <code>InputStream</code> instance that contains the data
	 * @param size   the number of bytes from the provided stream. [0..Long.MAX_VALUE]
	 */
	public OpContent(InputStream stream, long size) {
		this.stream = new XSizeInputStream(stream, size);
		this.size = size;
	}

	public OpContent(byte[] data) {
		this(new ByteArrayInputStream(data),  data.length);
	}
   
	public void setRefCount(int refCount) {
		this.refCount = refCount;
	}

	public int getRefCount() {
		return refCount;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public String getMediaType() {
		return mediaType;
	}

	/**
	 * Sets the size of the Content. Only for internal use. (Hibernate)
	 *
	 * @param size the size of the Content data in bytes
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * Gets the size of the Content.
	 *
	 * @return the size of the Content data in bytes
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Sets the input stream to be written into the database. This method will overwrite the value of the <code>size</code> code.
	 *
	 * @param stream an <code>XSizeInputStream</code> instance. Ignored if null.
	 */
	public void setStream(XSizeInputStream stream) {
		if (stream != null) {
			if (stream.getSize() == XSizeInputStream.UNKNOW_STREAM_SIZE) {
				stream.setSize(this.size);  // if the stream doesn't have the sized set, use the persisted value
			}
			this.stream = stream;
		}
	}

	/**
	 * Gets the input stream from the database. The size of the stream is defined by the <code>size</code> property.
	 *
	 * @return an <code>XSizeInputStream</code> instance.
	 */
	public XSizeInputStream getStream() {
		if (stream != null && stream.getSize() == XSizeInputStream.UNKNOW_STREAM_SIZE) {
			stream.setSize(size); // if stream doesn't have the size set (retrieved from database), set it using the persisted size
		}
		return stream;
	}

	public byte[] toByteArray() {
		InputStream dataStream = stream.getInputStream();
		ByteArrayOutputStream bis = new ByteArrayOutputStream(); 
		byte[] buffer = new byte[4096];
		int read;
		try {
			read = dataStream.read(buffer);
			while (read > 0) {
				bis.write(buffer, 0, read);
				read = dataStream.read(buffer);
			}
			return bis.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Sets the input stream to be written into the database. This method will overwrite the value of the <code>size</code> code.
	 * Should not be used. Present only for back-up compatibility.
	 *
	 * @param stream an <code>XSizeInputStream</code> instance. Ignored if null.
	 * @see OpContent#setStream(onepoint.service.XSizeInputStream)
	 */
	@Deprecated
	public void setBytes(XSizeInputStream stream) {
		setStream(stream);
	}

	//
	public void setAttachments(Set<OpAttachment> attachments) {
		this.attachments = attachments;
	}

	public void addAttachment(OpAttachment tgt) {
		if (getAttachments() == null) {
			setAttachments(new HashSet<OpAttachment>());
		}
		if (getAttachments().add(tgt)) {
			tgt.setContent(this);
			setRefCount(getRefCount() + 1);
		}
	}

	public void removeAttachment(OpAttachment tgt) {
		if (getAttachments() == null) {
			return;
		}
		if (getAttachments().remove(tgt)) {
			tgt.setContent(null);
			setRefCount(getRefCount() - 1);
		}
	}

	public Set<OpAttachment> getAttachments() {
		return attachments;
	}

	public Set<OpDocumentNode> getDocumentNodes() {
		return documentNodes;
	}

	public void setDocumentNodes(Set<OpDocumentNode> documentNodes) {
		this.documentNodes = documentNodes;
	}

	public Set<OpDocument> getDocuments() {
		return documents;
	}

	public void setDocuments(Set<OpDocument> documents) {
		this.documents = documents;
	}

	public Set<OpAttachmentVersion> getAttachmentVersions() {
		return attachmentVersions;
	}

	public void setAttachmentVersions(Set<OpAttachmentVersion> attachmentVersions) {
		this.attachmentVersions = attachmentVersions;
	}

	public void addAttachmentVersion(OpAttachmentVersion av) {
		if (getAttachmentVersions() == null) {
			setAttachmentVersions(new HashSet<OpAttachmentVersion>());
		}
		if (getAttachmentVersions().add(av)) {
			av.setContent(this);
			setRefCount(getRefCount() + 1);
		}
	}

	public void removeAttachmentVersion(OpAttachmentVersion av) {
		if (getAttachmentVersions() == null) {
			return;
		}
		if (getAttachmentVersions().remove(av)) {
			av.setContent(this);
			setRefCount(getRefCount() - 1);
		}
	}

	public Set<OpSetting> getSettings() {
		return settings;
	}

	public void setSettings(Set<OpSetting> settings) {
		this.settings = settings;
	}

	public void addSetting(OpSetting setting) {
		if (getSettings() == null) {
			setSettings(new HashSet<OpSetting>());
		}
		if (getSettings().add(setting)) {
			setting.setContent(this);
			setRefCount(getRefCount() + 1);
		}
	}

	public void removeSetting(OpSetting setting) {
		if (getSettings() == null) {
			return;
		}
		if (getSettings().remove(setting)) {
			setting.setContent(this);
			setRefCount(getRefCount() - 1);
		}
	}

}
