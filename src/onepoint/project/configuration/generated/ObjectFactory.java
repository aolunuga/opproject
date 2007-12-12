//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.12.03 at 02:00:42 PM GMT 
//


package onepoint.project.configuration.generated;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the onepoint.project.configuration.generated package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Configuration_QNAME = new QName("", "configuration");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: onepoint.project.configuration.generated
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link OpMesageQueueConfig }
     * 
     */
    public OpMesageQueueConfig createOpMesageQueueConfig() {
        return new OpMesageQueueConfig();
    }

    /**
     * Create an instance of {@link OpLdapGroupMapping }
     * 
     */
    public OpLdapGroupMapping createOpLdapGroupMapping() {
        return new OpLdapGroupMapping();
    }

    /**
     * Create an instance of {@link OpLdapUserMapping }
     * 
     */
    public OpLdapUserMapping createOpLdapUserMapping() {
        return new OpLdapUserMapping();
    }

    /**
     * Create an instance of {@link OpLdapGroupSearchConfig }
     * 
     */
    public OpLdapGroupSearchConfig createOpLdapGroupSearchConfig() {
        return new OpLdapGroupSearchConfig();
    }

    /**
     * Create an instance of {@link OpDatabaseConfig }
     * 
     */
    public OpDatabaseConfig createOpDatabaseConfig() {
        return new OpDatabaseConfig();
    }

    /**
     * Create an instance of {@link OpReportWorkflow }
     * 
     */
    public OpReportWorkflow createOpReportWorkflow() {
        return new OpReportWorkflow();
    }

    /**
     * Create an instance of {@link OpLdapMembership }
     * 
     */
    public OpLdapMembership createOpLdapMembership() {
        return new OpLdapMembership();
    }

    /**
     * Create an instance of {@link OpLdapReplace }
     * 
     */
    public OpLdapReplace createOpLdapReplace() {
        return new OpLdapReplace();
    }

    /**
     * Create an instance of {@link OpLdapGroupConfig }
     * 
     */
    public OpLdapGroupConfig createOpLdapGroupConfig() {
        return new OpLdapGroupConfig();
    }

    /**
     * Create an instance of {@link OpLdapGroupFilterConfig }
     * 
     */
    public OpLdapGroupFilterConfig createOpLdapGroupFilterConfig() {
        return new OpLdapGroupFilterConfig();
    }

    /**
     * Create an instance of {@link OpConfigDatabasePassword }
     * 
     */
    public OpConfigDatabasePassword createOpConfigDatabasePassword() {
        return new OpConfigDatabasePassword();
    }

    /**
     * Create an instance of {@link OpLdapConnectionConfig }
     * 
     */
    public OpLdapConnectionConfig createOpLdapConnectionConfig() {
        return new OpLdapConnectionConfig();
    }

    /**
     * Create an instance of {@link OpLdapUserConfig }
     * 
     */
    public OpLdapUserConfig createOpLdapUserConfig() {
        return new OpLdapUserConfig();
    }

    /**
     * Create an instance of {@link OpSecret }
     * 
     */
    public OpSecret createOpSecret() {
        return new OpSecret();
    }

    /**
     * Create an instance of {@link OpLdapUserSearchConfig }
     * 
     */
    public OpLdapUserSearchConfig createOpLdapUserSearchConfig() {
        return new OpLdapUserSearchConfig();
    }

    /**
     * Create an instance of {@link OpLdapStringValue }
     * 
     */
    public OpLdapStringValue createOpLdapStringValue() {
        return new OpLdapStringValue();
    }

    /**
     * Create an instance of {@link OpScheduleConfig }
     * 
     */
    public OpScheduleConfig createOpScheduleConfig() {
        return new OpScheduleConfig();
    }

    /**
     * Create an instance of {@link OpConfig }
     * 
     */
    public OpConfig createOpConfig() {
        return new OpConfig();
    }

    /**
     * Create an instance of {@link OpLdapConfig }
     * 
     */
    public OpLdapConfig createOpLdapConfig() {
        return new OpLdapConfig();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link OpConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "configuration")
    public JAXBElement<OpConfig> createConfiguration(OpConfig value) {
        return new JAXBElement<OpConfig>(_Configuration_QNAME, OpConfig.class, null, value);
    }

}
