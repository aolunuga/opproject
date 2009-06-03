//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.03.25 at 04:44:46 PM CET 
//


package onepoint.project.configuration.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OpLdapGroupSearchConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OpLdapGroupSearchConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="search-filter" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="search-base" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="search-scope">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;enumeration value="object"/>
 *               &lt;enumeration value="onelevel"/>
 *               &lt;enumeration value="subtree"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OpLdapGroupSearchConfig", propOrder = {
    "searchFilter",
    "searchBase",
    "searchScope"
})
public class OpLdapGroupSearchConfig {

    @XmlElement(name = "search-filter", required = true)
    protected String searchFilter;
    @XmlElement(name = "search-base", required = true)
    protected String searchBase;
    @XmlElement(name = "search-scope", required = true, defaultValue = "onelevel")
    protected String searchScope;

    /**
     * Gets the value of the searchFilter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSearchFilter() {
        return searchFilter;
    }

    /**
     * Sets the value of the searchFilter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSearchFilter(String value) {
        this.searchFilter = value;
    }

    /**
     * Gets the value of the searchBase property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSearchBase() {
        return searchBase;
    }

    /**
     * Sets the value of the searchBase property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSearchBase(String value) {
        this.searchBase = value;
    }

    /**
     * Gets the value of the searchScope property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSearchScope() {
        return searchScope;
    }

    /**
     * Sets the value of the searchScope property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSearchScope(String value) {
        this.searchScope = value;
    }

}
