//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.12.03 at 02:00:42 PM GMT 
//


package onepoint.project.configuration.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OpLdapGroupConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
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
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OpLdapGroupConfig", propOrder = {
    "retrieval",
    "mapping",
    "filter"
})
public class OpLdapGroupConfig {

    @XmlElement(required = true)
    protected OpLdapGroupSearchConfig retrieval;
    @XmlElement(required = true)
    protected OpLdapGroupMapping mapping;
    @XmlElement(required = true)
    protected OpLdapGroupFilterConfig filter;

    /**
     * Gets the value of the retrieval property.
     * 
     * @return
     *     possible object is
     *     {@link OpLdapGroupSearchConfig }
     *     
     */
    public OpLdapGroupSearchConfig getRetrieval() {
        return retrieval;
    }

    /**
     * Sets the value of the retrieval property.
     * 
     * @param value
     *     allowed object is
     *     {@link OpLdapGroupSearchConfig }
     *     
     */
    public void setRetrieval(OpLdapGroupSearchConfig value) {
        this.retrieval = value;
    }

    /**
     * Gets the value of the mapping property.
     * 
     * @return
     *     possible object is
     *     {@link OpLdapGroupMapping }
     *     
     */
    public OpLdapGroupMapping getMapping() {
        return mapping;
    }

    /**
     * Sets the value of the mapping property.
     * 
     * @param value
     *     allowed object is
     *     {@link OpLdapGroupMapping }
     *     
     */
    public void setMapping(OpLdapGroupMapping value) {
        this.mapping = value;
    }

    /**
     * Gets the value of the filter property.
     * 
     * @return
     *     possible object is
     *     {@link OpLdapGroupFilterConfig }
     *     
     */
    public OpLdapGroupFilterConfig getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     * 
     * @param value
     *     allowed object is
     *     {@link OpLdapGroupFilterConfig }
     *     
     */
    public void setFilter(OpLdapGroupFilterConfig value) {
        this.filter = value;
    }

}
