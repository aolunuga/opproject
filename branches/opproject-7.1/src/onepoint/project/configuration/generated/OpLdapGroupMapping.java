//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.12.13 at 08:01:57 AM GMT 
//


package onepoint.project.configuration.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for OpLdapGroupMapping complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="OpLdapGroupMapping">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="OpGroup.Description" type="{}OpLdapStringValue" minOccurs="0"/>
 *         &lt;element name="OpGroup.DisplayName" type="{}OpLdapStringValue" minOccurs="0"/>
 *         &lt;element name="OpGroup.Membership" type="{}OpLdapMembership" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="OpGroup.ParentMembership" type="{}OpLdapMembership" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OpLdapGroupMapping", propOrder = {
     "opGroupDescription",
     "opGroupDisplayName",
     "opGroupMembership",
     "opGroupParentMembership"
     })
public class OpLdapGroupMapping {

   @XmlElement(name = "OpGroup.Description")
   protected OpLdapStringValue opGroupDescription;
   @XmlElement(name = "OpGroup.DisplayName")
   protected OpLdapStringValue opGroupDisplayName;
   @XmlElement(name = "OpGroup.Membership")
   protected List<OpLdapMembership> opGroupMembership;
   @XmlElement(name = "OpGroup.ParentMembership")
   protected List<OpLdapMembership> opGroupParentMembership;

   /**
    * Gets the value of the opGroupDescription property.
    *
    * @return possible object is
    *         {@link OpLdapStringValue }
    */
   public OpLdapStringValue getOpGroupDescription() {
      return opGroupDescription;
   }

   /**
    * Sets the value of the opGroupDescription property.
    *
    * @param value allowed object is
    *              {@link OpLdapStringValue }
    */
   public void setOpGroupDescription(OpLdapStringValue value) {
      this.opGroupDescription = value;
   }

   /**
    * Gets the value of the opGroupDisplayName property.
    *
    * @return possible object is
    *         {@link OpLdapStringValue }
    */
   public OpLdapStringValue getOpGroupDisplayName() {
      return opGroupDisplayName;
   }

   /**
    * Sets the value of the opGroupDisplayName property.
    *
    * @param value allowed object is
    *              {@link OpLdapStringValue }
    */
   public void setOpGroupDisplayName(OpLdapStringValue value) {
      this.opGroupDisplayName = value;
   }

   /**
    * Gets the value of the opGroupMembership property.
    * <p/>
    * <p/>
    * This accessor method returns a reference to the live list,
    * not a snapshot. Therefore any modification you make to the
    * returned list will be present inside the JAXB object.
    * This is why there is not a <CODE>set</CODE> method for the opGroupMembership property.
    * <p/>
    * <p/>
    * For example, to add a new item, do as follows:
    * <pre>
    *    getOpGroupMembership().add(newItem);
    * </pre>
    * <p/>
    * <p/>
    * <p/>
    * Objects of the following type(s) are allowed in the list
    * {@link OpLdapMembership }
    */
   public List<OpLdapMembership> getOpGroupMembership() {
      if (opGroupMembership == null) {
         opGroupMembership = new ArrayList<OpLdapMembership>();
      }
      return this.opGroupMembership;
   }

   /**
    * Gets the value of the opGroupParentMembership property.
    * <p/>
    * <p/>
    * This accessor method returns a reference to the live list,
    * not a snapshot. Therefore any modification you make to the
    * returned list will be present inside the JAXB object.
    * This is why there is not a <CODE>set</CODE> method for the opGroupParentMembership property.
    * <p/>
    * <p/>
    * For example, to add a new item, do as follows:
    * <pre>
    *    getOpGroupParentMembership().add(newItem);
    * </pre>
    * <p/>
    * <p/>
    * <p/>
    * Objects of the following type(s) are allowed in the list
    * {@link OpLdapMembership }
    */
   public List<OpLdapMembership> getOpGroupParentMembership() {
      if (opGroupParentMembership == null) {
         opGroupParentMembership = new ArrayList<OpLdapMembership>();
      }
      return this.opGroupParentMembership;
   }

}