//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.08.14 at 02:38:34 PM EEST 
//


package onepoint.project.configuration.generated;


/**
 * Java content class for OpLdapStringValue complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/D:/Codemart/OnePoint%20Project/trunk/opproject/src/onepoint/project/configuration/configuration.xsd line 183)
 * <p>
 * <pre>
 * &lt;complexType name="OpLdapStringValue">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="replace" type="{}OpLdapReplace" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="fixed" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="synched" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface OpLdapStringValue {


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
     * Gets the value of the synched property.
     * 
     */
    boolean isSynched();

    /**
     * Sets the value of the synched property.
     * 
     */
    void setSynched(boolean value);

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

}
