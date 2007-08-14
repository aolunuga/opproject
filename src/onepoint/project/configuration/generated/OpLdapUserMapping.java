//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.08.14 at 02:38:34 PM EEST 
//


package onepoint.project.configuration.generated;


/**
 * Java content class for OpLdapUserMapping complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/D:/Codemart/OnePoint%20Project/trunk/opproject/src/onepoint/project/configuration/configuration.xsd line 120)
 * <p>
 * <pre>
 * &lt;complexType name="OpLdapUserMapping">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="OpUser.Name" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.Password" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.FirstName" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.LastName" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.Email" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.Description" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.Language" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.Level" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.Phone" type="{}OpLdapStringValue" minOccurs="0"/>
 *         &lt;element name="OpUser.Mobile" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.Fax" type="{}OpLdapStringValue"/>
 *         &lt;element name="OpUser.DisplayName" type="{}OpLdapStringValue" minOccurs="0"/>
 *         &lt;element name="OpUser.GroupMembership" type="{}OpLdapMembership" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface OpLdapUserMapping {


    /**
     * Gets the value of the opUserPassword property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserPassword();

    /**
     * Sets the value of the opUserPassword property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserPassword(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserDescription property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserDescription();

    /**
     * Sets the value of the opUserDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserDescription(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the OpUserGroupMembership property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the OpUserGroupMembership property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOpUserGroupMembership().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link onepoint.project.configuration.generated.OpLdapMembership}
     * 
     */
    java.util.List getOpUserGroupMembership();

    /**
     * Gets the value of the opUserLanguage property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserLanguage();

    /**
     * Sets the value of the opUserLanguage property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserLanguage(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserMobile property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserMobile();

    /**
     * Sets the value of the opUserMobile property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserMobile(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserPhone property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserPhone();

    /**
     * Sets the value of the opUserPhone property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserPhone(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserFirstName property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserFirstName();

    /**
     * Sets the value of the opUserFirstName property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserFirstName(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserEmail property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserEmail();

    /**
     * Sets the value of the opUserEmail property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserEmail(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserDisplayName property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserDisplayName();

    /**
     * Sets the value of the opUserDisplayName property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserDisplayName(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserName property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserName();

    /**
     * Sets the value of the opUserName property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserName(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserFax property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserFax();

    /**
     * Sets the value of the opUserFax property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserFax(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserLastName property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserLastName();

    /**
     * Sets the value of the opUserLastName property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserLastName(onepoint.project.configuration.generated.OpLdapStringValue value);

    /**
     * Gets the value of the opUserLevel property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    onepoint.project.configuration.generated.OpLdapStringValue getOpUserLevel();

    /**
     * Sets the value of the opUserLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapStringValue}
     */
    void setOpUserLevel(onepoint.project.configuration.generated.OpLdapStringValue value);

}
