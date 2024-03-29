//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.03.25 at 04:44:46 PM CET 
//


package onepoint.project.configuration.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OpDatabaseConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OpDatabaseConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="database-type" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="database-driver" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="database-url" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="database-login" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="database-password" type="{}OpConfigDatabasePassword"/>
 *         &lt;element name="database-path" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="connection-pool-min-size" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="connection-pool-max-size" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OpDatabaseConfig", propOrder = {

})
public class OpDatabaseConfig {

    @XmlElement(name = "database-type", required = true, defaultValue = "MySQLInnoDB")
    protected String databaseType;
    @XmlElement(name = "database-driver", required = true, defaultValue = "com.mysql.jdbc.Driver")
    protected String databaseDriver;
    @XmlElement(name = "database-url", required = true, defaultValue = "jdbc:mysql://localhost:3306/opproject")
    protected String databaseUrl;
    @XmlElement(name = "database-login", required = true, defaultValue = "opproject")
    protected String databaseLogin;
    @XmlElement(name = "database-password", required = true)
    protected OpConfigDatabasePassword databasePassword;
    @XmlElement(name = "database-path")
    protected String databasePath;
    @XmlElement(name = "connection-pool-min-size")
    protected String connectionPoolMinSize;
    @XmlElement(name = "connection-pool-max-size")
    protected String connectionPoolMaxSize;
    @XmlAttribute
    protected String name;

    /**
     * Gets the value of the databaseType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatabaseType() {
        return databaseType;
    }

    /**
     * Sets the value of the databaseType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatabaseType(String value) {
        this.databaseType = value;
    }

    /**
     * Gets the value of the databaseDriver property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatabaseDriver() {
        return databaseDriver;
    }

    /**
     * Sets the value of the databaseDriver property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatabaseDriver(String value) {
        this.databaseDriver = value;
    }

    /**
     * Gets the value of the databaseUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    /**
     * Sets the value of the databaseUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatabaseUrl(String value) {
        this.databaseUrl = value;
    }

    /**
     * Gets the value of the databaseLogin property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatabaseLogin() {
        return databaseLogin;
    }

    /**
     * Sets the value of the databaseLogin property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatabaseLogin(String value) {
        this.databaseLogin = value;
    }

    /**
     * Gets the value of the databasePassword property.
     * 
     * @return
     *     possible object is
     *     {@link OpConfigDatabasePassword }
     *     
     */
    public OpConfigDatabasePassword getDatabasePassword() {
        return databasePassword;
    }

    /**
     * Sets the value of the databasePassword property.
     * 
     * @param value
     *     allowed object is
     *     {@link OpConfigDatabasePassword }
     *     
     */
    public void setDatabasePassword(OpConfigDatabasePassword value) {
        this.databasePassword = value;
    }

    /**
     * Gets the value of the databasePath property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatabasePath() {
        return databasePath;
    }

    /**
     * Sets the value of the databasePath property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatabasePath(String value) {
        this.databasePath = value;
    }

    /**
     * Gets the value of the connectionPoolMinSize property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConnectionPoolMinSize() {
        return connectionPoolMinSize;
    }

    /**
     * Sets the value of the connectionPoolMinSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConnectionPoolMinSize(String value) {
        this.connectionPoolMinSize = value;
    }

    /**
     * Gets the value of the connectionPoolMaxSize property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConnectionPoolMaxSize() {
        return connectionPoolMaxSize;
    }

    /**
     * Sets the value of the connectionPoolMaxSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConnectionPoolMaxSize(String value) {
        this.connectionPoolMaxSize = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

}
