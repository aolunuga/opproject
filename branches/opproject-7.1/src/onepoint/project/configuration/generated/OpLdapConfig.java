//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.09.03 at 07:54:45 AM CEST 
//


package onepoint.project.configuration.generated;


/**
 * Java content class for OpLdapConfig complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/usr/users/dfreis/projects/opproject/src/onepoint/project/configuration/configuration.xsd line 60)
 * <p>
 * <pre>
 * &lt;complexType name="OpLdapConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence minOccurs="0">
 *         &lt;element name="connection" type="{}OpLdapConnectionConfig"/>
 *         &lt;element name="update-schedule" type="{}OpLdapScheduleConfig" maxOccurs="unbounded"/>
 *         &lt;element name="users" type="{}OpLdapUserConfig"/>
 *         &lt;element name="groups" type="{}OpLdapGroupConfig" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface OpLdapConfig {


    /**
     * Gets the value of the groups property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapGroupConfig}
     */
    onepoint.project.configuration.generated.OpLdapGroupConfig getGroups();

    /**
     * Sets the value of the groups property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapGroupConfig}
     */
    void setGroups(onepoint.project.configuration.generated.OpLdapGroupConfig value);

    /**
     * Gets the value of the users property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapUserConfig}
     */
    onepoint.project.configuration.generated.OpLdapUserConfig getUsers();

    /**
     * Sets the value of the users property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapUserConfig}
     */
    void setUsers(onepoint.project.configuration.generated.OpLdapUserConfig value);

    /**
     * Gets the value of the UpdateSchedule property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the UpdateSchedule property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUpdateSchedule().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link onepoint.project.configuration.generated.OpLdapScheduleConfig}
     * 
     */
    java.util.List getUpdateSchedule();

    /**
     * Gets the value of the connection property.
     * 
     * @return
     *     possible object is
     *     {@link onepoint.project.configuration.generated.OpLdapConnectionConfig}
     */
    onepoint.project.configuration.generated.OpLdapConnectionConfig getConnection();

    /**
     * Sets the value of the connection property.
     * 
     * @param value
     *     allowed object is
     *     {@link onepoint.project.configuration.generated.OpLdapConnectionConfig}
     */
    void setConnection(onepoint.project.configuration.generated.OpLdapConnectionConfig value);

}