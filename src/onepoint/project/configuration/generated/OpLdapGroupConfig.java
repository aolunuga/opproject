//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.08.02 at 12:39:24 PM EEST 
//


package onepoint.project.configuration.generated;


/**
 * Java content class for OpLdapGroupConfig complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/C:/Work/OnePoint/trunk/opproject/src/onepoint/project/configuration/configuration.xsd line 137)
 * <p>
 * <pre>
 * &lt;complexType name="OpLdapGroupConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="retrieval" type="{}OpLdapGroupSearchConfig"/>
 *         &lt;element name="mapping" type="{}OpLdapGroupMapping"/>
 *         &lt;element name="filter" type="{}OpLdapGroupFilterConfig"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface OpLdapGroupConfig {


    /**
     * Gets the value of the mapping property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapGroupMapping}
     */
    onepoint.project.configuration.generated.OpLdapGroupMapping getMapping();

    /**
     * Sets the value of the mapping property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapGroupMapping}
     */
    void setMapping(onepoint.project.configuration.generated.OpLdapGroupMapping value);

    /**
     * Gets the value of the filter property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapGroupFilterConfig}
     */
    onepoint.project.configuration.generated.OpLdapGroupFilterConfig getFilter();

    /**
     * Sets the value of the filter property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapGroupFilterConfig}
     */
    void setFilter(onepoint.project.configuration.generated.OpLdapGroupFilterConfig value);

    /**
     * Gets the value of the retrieval property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapGroupSearchConfig}
     */
    onepoint.project.configuration.generated.OpLdapGroupSearchConfig getRetrieval();

    /**
     * Sets the value of the retrieval property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapGroupSearchConfig}
     */
    void setRetrieval(onepoint.project.configuration.generated.OpLdapGroupSearchConfig value);

}
