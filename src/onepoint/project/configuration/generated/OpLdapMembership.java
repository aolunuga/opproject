//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.11.08 at 02:58:36 PM CET 
//


package onepoint.project.configuration.generated;


/**
 * Java content class for OpLdapMembership complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/C:/cruiseControl/projects/onepoint/opproject/src/onepoint/project/configuration/configuration.xsd line 223)
 * <p>
 * <pre>
 * &lt;complexType name="OpLdapMembership">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="replace" type="{}OpLdapReplace" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="fixed" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="query" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="query-base" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="query-scope">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="object"/>
 *             &lt;enumeration value="onelevel"/>
 *             &lt;enumeration value="subtree"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="url" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface OpLdapMembership {


    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getValue();

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setValue(java.lang.String value);

    /**
     * Gets the value of the queryBase property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getQueryBase();

    /**
     * Sets the value of the queryBase property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setQueryBase(java.lang.String value);

    /**
     * Gets the value of the query property.
     * 
     */
    boolean isQuery();

    /**
     * Sets the value of the query property.
     * 
     */
    void setQuery(boolean value);

    /**
     * Gets the value of the fixed property.
     * 
     */
    boolean isFixed();

    /**
     * Sets the value of the fixed property.
     * 
     */
    void setFixed(boolean value);

    /**
     * Gets the value of the Replace property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the Replace property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReplace().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link onepoint.project.configuration.generated.OpLdapReplace}
     * 
     */
    java.util.List getReplace();

    /**
     * Gets the value of the url property.
     * 
     */
    boolean isUrl();

    /**
     * Sets the value of the url property.
     * 
     */
    void setUrl(boolean value);

    /**
     * Gets the value of the queryScope property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getQueryScope();

    /**
     * Sets the value of the queryScope property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setQueryScope(java.lang.String value);

}
