//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.08.02 at 12:39:24 PM EEST 
//


package onepoint.project.configuration.generated;


/**
 * Java content class for OpLdapConnectionConfig complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/C:/Work/OnePoint/trunk/opproject/src/onepoint/project/configuration/configuration.xsd line 64)
 * <p>
 * <pre>
 * &lt;complexType name="OpLdapConnectionConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="connect-url" type="{http://www.w3.org/2001/XMLSchema}anyURI"/>
 *         &lt;element name="keystore" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="dns-url" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 *         &lt;element name="security-authentication" minOccurs="0">
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="none"/>
 *             &lt;enumeration value="simple"/>
 *             &lt;enumeration value="strong"/>
 *           &lt;/restriction>
 *         &lt;/element>
 *         &lt;element name="security-principal" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="security-credentials" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="security-protocol" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="password-hash-algorithm" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface OpLdapConnectionConfig {


    /**
     * Gets the value of the connectUrl property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getConnectUrl();

    /**
     * Sets the value of the connectUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setConnectUrl(java.lang.String value);

    /**
     * Gets the value of the passwordHashAlgorithm property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getPasswordHashAlgorithm();

    /**
     * Sets the value of the passwordHashAlgorithm property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setPasswordHashAlgorithm(java.lang.String value);

    /**
     * Gets the value of the securityProtocol property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getSecurityProtocol();

    /**
     * Sets the value of the securityProtocol property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setSecurityProtocol(java.lang.String value);

    /**
     * Gets the value of the dnsUrl property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getDnsUrl();

    /**
     * Sets the value of the dnsUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setDnsUrl(java.lang.String value);

    /**
     * Access the keystore, this is where the Root CA public key cert was installed
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getKeystore();

    /**
     * Access the keystore, this is where the Root CA public key cert was installed
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setKeystore(java.lang.String value);

    /**
     * Gets the value of the securityPrincipal property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getSecurityPrincipal();

    /**
     * Sets the value of the securityPrincipal property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setSecurityPrincipal(java.lang.String value);

    /**
     * Gets the value of the securityAuthentication property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getSecurityAuthentication();

    /**
     * Sets the value of the securityAuthentication property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setSecurityAuthentication(java.lang.String value);

    /**
     * Gets the value of the securityCredentials property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getSecurityCredentials();

    /**
     * Sets the value of the securityCredentials property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setSecurityCredentials(java.lang.String value);

}
