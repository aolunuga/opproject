//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.09.03 at 07:54:45 AM CEST 
//


package onepoint.project.configuration.generated.impl;

public class ConfigurationImpl
    extends onepoint.project.configuration.generated.impl.OpConfigImpl
    implements onepoint.project.configuration.generated.Configuration, com.sun.xml.bind.RIElement, com.sun.xml.bind.JAXBObject, onepoint.project.configuration.generated.impl.runtime.UnmarshallableObject, onepoint.project.configuration.generated.impl.runtime.XMLSerializable, onepoint.project.configuration.generated.impl.runtime.ValidatableObject
{

    public final static java.lang.Class version = (onepoint.project.configuration.generated.impl.JAXBVersion.class);
    private static com.sun.msv.grammar.Grammar schemaFragment;

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (onepoint.project.configuration.generated.Configuration.class);
    }

    public java.lang.String ____jaxb_ri____getNamespaceURI() {
        return "";
    }

    public java.lang.String ____jaxb_ri____getLocalName() {
        return "configuration";
    }

    public onepoint.project.configuration.generated.impl.runtime.UnmarshallingEventHandler createUnmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context) {
        return new onepoint.project.configuration.generated.impl.ConfigurationImpl.Unmarshaller(context);
    }

    public void serializeBody(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        context.startElement("", "configuration");
        super.serializeURIs(context);
        context.endNamespaceDecls();
        super.serializeAttributes(context);
        context.endAttributes();
        super.serializeBody(context);
        context.endElement();
    }

    public void serializeAttributes(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
    }

    public void serializeURIs(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
    }

    public java.lang.Class getPrimaryInterface() {
        return (onepoint.project.configuration.generated.Configuration.class);
    }

    public com.sun.msv.verifier.DocumentDeclaration createRawValidator() {
        if (schemaFragment == null) {
            schemaFragment = com.sun.xml.bind.validator.SchemaDeserializer.deserialize((
 "\u00ac\u00ed\u0000\u0005sr\u0000\'com.sun.msv.grammar.trex.ElementPattern\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000"
+"\tnameClasst\u0000\u001fLcom/sun/msv/grammar/NameClass;xr\u0000\u001ecom.sun.msv."
+"grammar.ElementExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002Z\u0000\u001aignoreUndeclaredAttributesL\u0000"
+"\fcontentModelt\u0000 Lcom/sun/msv/grammar/Expression;xr\u0000\u001ecom.sun."
+"msv.grammar.Expression\u00f8\u0018\u0082\u00e8N5~O\u0002\u0000\u0002L\u0000\u0013epsilonReducibilityt\u0000\u0013Lj"
+"ava/lang/Boolean;L\u0000\u000bexpandedExpq\u0000~\u0000\u0003xppp\u0000sr\u0000\u001fcom.sun.msv.gra"
+"mmar.SequenceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.sun.msv.grammar.BinaryExp"
+"\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0004exp1q\u0000~\u0000\u0003L\u0000\u0004exp2q\u0000~\u0000\u0003xq\u0000~\u0000\u0004ppsr\u0000!com.sun.msv.g"
+"rammar.InterleaveExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\bppsq\u0000~\u0000\nppsq\u0000~\u0000\nppsq\u0000~\u0000"
+"\nppsq\u0000~\u0000\nppsq\u0000~\u0000\nppsq\u0000~\u0000\nppsq\u0000~\u0000\nppsq\u0000~\u0000\nppsq\u0000~\u0000\nppsq\u0000~\u0000\npps"
+"q\u0000~\u0000\nppsq\u0000~\u0000\nppsq\u0000~\u0000\nppsr\u0000\u001dcom.sun.msv.grammar.ChoiceExp\u0000\u0000\u0000\u0000"
+"\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\bppsq\u0000~\u0000\u0000sr\u0000\u0011java.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee\u0002\u0000\u0001Z\u0000\u0005valu"
+"exp\u0000p\u0000sq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0019ppsr\u0000 com.sun.msv.grammar.OneOr"
+"MoreExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001ccom.sun.msv.grammar.UnaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000"
+"\u0001L\u0000\u0003expq\u0000~\u0000\u0003xq\u0000~\u0000\u0004q\u0000~\u0000\u001dpsr\u0000 com.sun.msv.grammar.AttributeExp"
+"\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003expq\u0000~\u0000\u0003L\u0000\tnameClassq\u0000~\u0000\u0001xq\u0000~\u0000\u0004q\u0000~\u0000\u001dpsr\u00002com.s"
+"un.msv.grammar.Expression$AnyStringExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~"
+"\u0000\u0004sq\u0000~\u0000\u001c\u0001q\u0000~\u0000\'sr\u0000 com.sun.msv.grammar.AnyNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000"
+"\u0000xr\u0000\u001dcom.sun.msv.grammar.NameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpsr\u00000com.sun.m"
+"sv.grammar.Expression$EpsilonExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0004q\u0000~\u0000"
+"(q\u0000~\u0000-sr\u0000#com.sun.msv.grammar.SimpleNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\tl"
+"ocalNamet\u0000\u0012Ljava/lang/String;L\u0000\fnamespaceURIq\u0000~\u0000/xq\u0000~\u0000*t\u00009on"
+"epoint.project.configuration.generated.OpDatabaseConfigt\u0000+ht"
+"tp://java.sun.com/jaxb/xjc/dummy-elementssq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001d"
+"psr\u0000\u001bcom.sun.msv.grammar.DataExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\u0002dtt\u0000\u001fLorg/rela"
+"xng/datatype/Datatype;L\u0000\u0006exceptq\u0000~\u0000\u0003L\u0000\u0004namet\u0000\u001dLcom/sun/msv/u"
+"til/StringPair;xq\u0000~\u0000\u0004ppsr\u0000\"com.sun.msv.datatype.xsd.QnameTyp"
+"e\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000*com.sun.msv.datatype.xsd.BuiltinAtomicType\u0000\u0000"
+"\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000%com.sun.msv.datatype.xsd.ConcreteType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000"
+"\u0000xr\u0000\'com.sun.msv.datatype.xsd.XSDatatypeImpl\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\fna"
+"mespaceUriq\u0000~\u0000/L\u0000\btypeNameq\u0000~\u0000/L\u0000\nwhiteSpacet\u0000.Lcom/sun/msv/"
+"datatype/xsd/WhiteSpaceProcessor;xpt\u0000 http://www.w3.org/2001"
+"/XMLSchemat\u0000\u0005QNamesr\u00005com.sun.msv.datatype.xsd.WhiteSpacePro"
+"cessor$Collapse\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000,com.sun.msv.datatype.xsd.White"
+"SpaceProcessor\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpsr\u00000com.sun.msv.grammar.Expressio"
+"n$NullSetExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0004ppsr\u0000\u001bcom.sun.msv.util.S"
+"tringPair\u00d0t\u001ejB\u008f\u008d\u00a0\u0002\u0000\u0002L\u0000\tlocalNameq\u0000~\u0000/L\u0000\fnamespaceURIq\u0000~\u0000/xpq"
+"\u0000~\u0000@q\u0000~\u0000?sq\u0000~\u0000.t\u0000\u0004typet\u0000)http://www.w3.org/2001/XMLSchema-in"
+"stanceq\u0000~\u0000-sq\u0000~\u0000.t\u0000\bdatabaset\u0000\u0000q\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000"
+"~\u0000\u0007ppsq\u0000~\u00005ppsr\u0000#com.sun.msv.datatype.xsd.StringType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001"
+"\u0002\u0000\u0001Z\u0000\risAlwaysValidxq\u0000~\u0000:q\u0000~\u0000?t\u0000\u0006stringsr\u00005com.sun.msv.datat"
+"ype.xsd.WhiteSpaceProcessor$Preserve\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000B\u0001q\u0000~\u0000Es"
+"q\u0000~\u0000Fq\u0000~\u0000Tq\u0000~\u0000?sq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\rd"
+"atabase-pathq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~"
+"\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\u0007browserq\u0000~\u0000Mq\u0000~\u0000-sq\u0000"
+"~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000"
+"Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\u000bsmtp-serverq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000"
+"~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\blog-fi"
+"leq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~\u0000\u0019ppsq\u0000~\u0000$"
+"q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\tlog-levelq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000"
+"~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq"
+"\u0000~\u0000.t\u0000\ncache-sizeq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000"
+"Qsq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\rjes-debuggingq\u0000"
+"~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000"
+"\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\u000esecure-serviceq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq"
+"\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-s"
+"q\u0000~\u0000.t\u0000\u0013resource-cache-sizeq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq"
+"\u0000~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\u000bbacku"
+"p-pathq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Qsq\u0000~\u0000\u0019ppsq"
+"\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\u0013max-attachment-sizeq\u0000~\u0000Mq\u0000"
+"~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0019ppsq\u0000~\u0000!q\u0000~\u0000\u001d"
+"psq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u0000\'q\u0000~\u0000+q\u0000~\u0000-sq\u0000~\u0000.t\u00001onepoint.project.config"
+"uration.generated.OpSecretq\u0000~\u00002sq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000"
+"Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\rshared-secretq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000s"
+"q\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0019ppsq\u0000~\u0000!q\u0000~\u0000\u001dpsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u0000\'q\u0000~\u0000+q\u0000"
+"~\u0000-sq\u0000~\u0000.t\u00009onepoint.project.configuration.generated.OpRepor"
+"tWorkflowq\u0000~\u00002sq\u0000~\u0000\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\u000fre"
+"port-workflowq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001dp\u0000sq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp"
+"\u0000sq\u0000~\u0000\u0019ppsq\u0000~\u0000!q\u0000~\u0000\u001dpsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u0000\'q\u0000~\u0000+q\u0000~\u0000-sq\u0000~\u0000.t\u00005one"
+"point.project.configuration.generated.OpLdapConfigq\u0000~\u00002sq\u0000~\u0000"
+"\u0019ppsq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\u0004ldapq\u0000~\u0000Mq\u0000~\u0000-sq\u0000~\u0000\u0019p"
+"psq\u0000~\u0000$q\u0000~\u0000\u001dpq\u0000~\u00008q\u0000~\u0000Hq\u0000~\u0000-sq\u0000~\u0000.t\u0000\rconfigurationq\u0000~\u0000Msr\u0000\"c"
+"om.sun.msv.grammar.ExpressionPool\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\bexpTablet\u0000/Lc"
+"om/sun/msv/grammar/ExpressionPool$ClosedHash;xpsr\u0000-com.sun.m"
+"sv.grammar.ExpressionPool$ClosedHash\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000\u0003I\u0000\u0005countB\u0000\rst"
+"reamVersionL\u0000\u0006parentt\u0000$Lcom/sun/msv/grammar/ExpressionPool;x"
+"p\u0000\u0000\u0000E\u0001pq\u0000~\u0000\u0017q\u0000~\u0000\fq\u0000~\u0000\rq\u0000~\u0000\u0013q\u0000~\u0000\u0010q\u0000~\u0000\u00b4q\u0000~\u0000\u00a7q\u0000~\u0000#q\u0000~\u0000\u00c1q\u0000~\u0000\tq\u0000~"
+"\u0000\u000eq\u0000~\u0000\u0015q\u0000~\u0000\u009dq\u0000~\u0000\u0018q\u0000~\u0000\u0096q\u0000~\u0000\u008fq\u0000~\u0000\u0088q\u0000~\u0000\u0081q\u0000~\u0000zq\u0000~\u0000sq\u0000~\u0000lq\u0000~\u0000eq\u0000~"
+"\u0000^q\u0000~\u0000Pq\u0000~\u0000{q\u0000~\u0000tq\u0000~\u0000mq\u0000~\u0000fq\u0000~\u0000_q\u0000~\u0000Xq\u0000~\u00003q\u0000~\u0000\u00a4q\u0000~\u0000\u001eq\u0000~\u0000\u0012q\u0000~"
+"\u0000\u0014q\u0000~\u0000\u00abq\u0000~\u0000\u009eq\u0000~\u0000\u0016q\u0000~\u0000\u00b3q\u0000~\u0000\u00a6q\u0000~\u0000 q\u0000~\u0000\u0097q\u0000~\u0000\u0090q\u0000~\u0000\u0089q\u0000~\u0000\u0082q\u0000~\u0000\u00b8q\u0000~"
+"\u0000\u00b1q\u0000~\u0000\u00c0q\u0000~\u0000\u00c5q\u0000~\u0000\u00beq\u0000~\u0000\u00c9q\u0000~\u0000\u000fq\u0000~\u0000\u009bq\u0000~\u0000\u0094q\u0000~\u0000\u008dq\u0000~\u0000\u0011q\u0000~\u0000\u0086q\u0000~\u0000\u007fq\u0000~"
+"\u0000xq\u0000~\u0000qq\u0000~\u0000jq\u0000~\u0000cq\u0000~\u0000\\q\u0000~\u0000Nq\u0000~\u0000\u000bq\u0000~\u0000\u00a2q\u0000~\u0000\u001aq\u0000~\u0000\u00afq\u0000~\u0000\u00bcx"));
        }
        return new com.sun.msv.verifier.regexp.REDocumentDeclaration(schemaFragment);
    }

    public class Unmarshaller
        extends onepoint.project.configuration.generated.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context) {
            super(context, "----");
        }

        protected Unmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return onepoint.project.configuration.generated.impl.ConfigurationImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  1 :
                        if (("ldap" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("report-workflow" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("shared-secret" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("max-attachment-size" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("backup-path" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("resource-cache-size" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("secure-service" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("jes-debugging" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("cache-size" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("log-level" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("log-file" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("smtp-server" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("browser" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("database-path" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        if (("database" == ___local)&&("" == ___uri)) {
                            spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        spawnHandlerFromEnterElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname, __atts);
                        return ;
                    case  0 :
                        if (("configuration" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        break;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        public void leaveElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  2 :
                        if (("configuration" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  1 :
                        spawnHandlerFromLeaveElement((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname);
                        return ;
                }
                super.leaveElement(___uri, ___local, ___qname);
                break;
            }
        }

        public void enterAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
                        return ;
                    case  1 :
                        spawnHandlerFromEnterAttribute((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname);
                        return ;
                }
                super.enterAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void leaveAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return ;
                    case  1 :
                        spawnHandlerFromLeaveAttribute((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, ___uri, ___local, ___qname);
                        return ;
                }
                super.leaveAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void handleText(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                try {
                    switch (state) {
                        case  3 :
                            revertToParentFromText(value);
                            return ;
                        case  1 :
                            spawnHandlerFromText((((onepoint.project.configuration.generated.impl.OpConfigImpl)onepoint.project.configuration.generated.impl.ConfigurationImpl.this).new Unmarshaller(context)), 2, value);
                            return ;
                    }
                } catch (java.lang.RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

    }

}
