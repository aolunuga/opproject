//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.11.08 at 02:58:36 PM CET 
//


package onepoint.project.configuration.generated;


/**
 * Java content class for OpConfigDatabasePassword complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/C:/cruiseControl/projects/onepoint/opproject/src/onepoint/project/configuration/configuration.xsd line 39)
 * <p>
 * <pre>
 * &lt;complexType name="OpConfigDatabasePassword">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="encrypted" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface OpConfigDatabasePassword {


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
     * Gets the value of the encrypted property.
     * 
     */
    boolean isEncrypted();

    /**
     * Sets the value of the encrypted property.
     * 
     */
    void setEncrypted(boolean value);

}
