//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.09.03 at 07:54:45 AM CEST 
//


package onepoint.project.configuration.generated;


/**
 * Java content class for OpConfig complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/usr/users/dfreis/projects/opproject/src/onepoint/project/configuration/configuration.xsd line 4)
 * <p>
 * <pre>
 * &lt;complexType name="OpConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="database" type="{}OpDatabaseConfig" minOccurs="0"/>
 *         &lt;element name="database-path" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="browser" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="smtp-server" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="log-file" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="log-level" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="cache-size" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="jes-debugging" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="secure-service" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="resource-cache-size" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="backup-path" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="max-attachment-size" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="shared-secret" type="{}OpSecret" minOccurs="0"/>
 *         &lt;element name="report-workflow" type="{}OpReportWorkflow" minOccurs="0"/>
 *         &lt;element name="ldap" type="{}OpLdapConfig" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface OpConfig {


    /**
     * Gets the value of the smtpServer property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getSmtpServer();

    /**
     * Sets the value of the smtpServer property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setSmtpServer(java.lang.String value);

    /**
     * Gets the value of the cacheSize property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getCacheSize();

    /**
     * Sets the value of the cacheSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setCacheSize(java.lang.String value);

    /**
     * Gets the value of the jesDebugging property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getJesDebugging();

    /**
     * Sets the value of the jesDebugging property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setJesDebugging(java.lang.String value);

    /**
     * Gets the value of the logLevel property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getLogLevel();

    /**
     * Sets the value of the logLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setLogLevel(java.lang.String value);

    /**
     * Gets the value of the sharedSecret property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpSecret}
     */
    onepoint.project.configuration.generated.OpSecret getSharedSecret();

    /**
     * Sets the value of the sharedSecret property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpSecret}
     */
    void setSharedSecret(onepoint.project.configuration.generated.OpSecret value);

    /**
     * Gets the value of the ldap property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapConfig}
     */
    onepoint.project.configuration.generated.OpLdapConfig getLdap();

    /**
     * Sets the value of the ldap property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapConfig}
     */
    void setLdap(onepoint.project.configuration.generated.OpLdapConfig value);

    /**
     * Gets the value of the database property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpDatabaseConfig}
     */
    onepoint.project.configuration.generated.OpDatabaseConfig getDatabase();

    /**
     * Sets the value of the database property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpDatabaseConfig}
     */
    void setDatabase(onepoint.project.configuration.generated.OpDatabaseConfig value);

    /**
     * Gets the value of the resourceCacheSize property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getResourceCacheSize();

    /**
     * Sets the value of the resourceCacheSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setResourceCacheSize(java.lang.String value);

    /**
     * Gets the value of the logFile property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getLogFile();

    /**
     * Sets the value of the logFile property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setLogFile(java.lang.String value);

    /**
     * Gets the value of the databasePath property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getDatabasePath();

    /**
     * Sets the value of the databasePath property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setDatabasePath(java.lang.String value);

    /**
     * Gets the value of the browser property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getBrowser();

    /**
     * Sets the value of the browser property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setBrowser(java.lang.String value);

    /**
     * Gets the value of the maxAttachmentSize property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getMaxAttachmentSize();

    /**
     * Sets the value of the maxAttachmentSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setMaxAttachmentSize(java.lang.String value);

    /**
     * Gets the value of the secureService property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getSecureService();

    /**
     * Sets the value of the secureService property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setSecureService(java.lang.String value);

    /**
     * Gets the value of the reportWorkflow property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpReportWorkflow}
     */
    onepoint.project.configuration.generated.OpReportWorkflow getReportWorkflow();

    /**
     * Sets the value of the reportWorkflow property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpReportWorkflow}
     */
    void setReportWorkflow(onepoint.project.configuration.generated.OpReportWorkflow value);

    /**
     * Gets the value of the backupPath property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getBackupPath();

    /**
     * Sets the value of the backupPath property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setBackupPath(java.lang.String value);

}