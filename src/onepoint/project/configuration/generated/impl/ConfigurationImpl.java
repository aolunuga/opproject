//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.08.14 at 02:38:34 PM EEST 
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
+"q\u0000~\u0000\nppsr\u0000\u001dcom.sun.msv.grammar.ChoiceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\bpps"
+"q\u0000~\u0000\u0000sr\u0000\u0011java.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee\u0002\u0000\u0001Z\u0000\u0005valuexp\u0000p\u0000sq\u0000~\u0000\u0007ppsq"
+"\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0017ppsr\u0000 com.sun.msv.grammar.OneOrMoreExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002"
+"\u0000\u0000xr\u0000\u001ccom.sun.msv.grammar.UnaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\u0003expq\u0000~\u0000\u0003xq\u0000~"
+"\u0000\u0004q\u0000~\u0000\u001bpsr\u0000 com.sun.msv.grammar.AttributeExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003ex"
+"pq\u0000~\u0000\u0003L\u0000\tnameClassq\u0000~\u0000\u0001xq\u0000~\u0000\u0004q\u0000~\u0000\u001bpsr\u00002com.sun.msv.grammar.E"
+"xpression$AnyStringExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0004sq\u0000~\u0000\u001a\u0001q\u0000~\u0000%sr"
+"\u0000 com.sun.msv.grammar.AnyNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.sun.msv"
+".grammar.NameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpsr\u00000com.sun.msv.grammar.Expre"
+"ssion$EpsilonExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0004q\u0000~\u0000&q\u0000~\u0000+sr\u0000#com.su"
+"n.msv.grammar.SimpleNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\tlocalNamet\u0000\u0012Ljava"
+"/lang/String;L\u0000\fnamespaceURIq\u0000~\u0000-xq\u0000~\u0000(t\u00009onepoint.project.c"
+"onfiguration.generated.OpDatabaseConfigt\u0000+http://java.sun.co"
+"m/jaxb/xjc/dummy-elementssq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpsr\u0000\u001bcom.sun.msv"
+".grammar.DataExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\u0002dtt\u0000\u001fLorg/relaxng/datatype/Dat"
+"atype;L\u0000\u0006exceptq\u0000~\u0000\u0003L\u0000\u0004namet\u0000\u001dLcom/sun/msv/util/StringPair;x"
+"q\u0000~\u0000\u0004ppsr\u0000\"com.sun.msv.datatype.xsd.QnameType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000*"
+"com.sun.msv.datatype.xsd.BuiltinAtomicType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000%com"
+".sun.msv.datatype.xsd.ConcreteType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\'com.sun.msv"
+".datatype.xsd.XSDatatypeImpl\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\fnamespaceUriq\u0000~\u0000-L"
+"\u0000\btypeNameq\u0000~\u0000-L\u0000\nwhiteSpacet\u0000.Lcom/sun/msv/datatype/xsd/Whi"
+"teSpaceProcessor;xpt\u0000 http://www.w3.org/2001/XMLSchemat\u0000\u0005QNa"
+"mesr\u00005com.sun.msv.datatype.xsd.WhiteSpaceProcessor$Collapse\u0000"
+"\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000,com.sun.msv.datatype.xsd.WhiteSpaceProcessor\u0000\u0000"
+"\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpsr\u00000com.sun.msv.grammar.Expression$NullSetExpress"
+"ion\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0004ppsr\u0000\u001bcom.sun.msv.util.StringPair\u00d0t\u001ejB\u008f\u008d"
+"\u00a0\u0002\u0000\u0002L\u0000\tlocalNameq\u0000~\u0000-L\u0000\fnamespaceURIq\u0000~\u0000-xpq\u0000~\u0000>q\u0000~\u0000=sq\u0000~\u0000,t"
+"\u0000\u0004typet\u0000)http://www.w3.org/2001/XMLSchema-instanceq\u0000~\u0000+sq\u0000~\u0000"
+",t\u0000\bdatabaset\u0000\u0000q\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppsq\u0000~\u00003ppsr\u0000"
+"#com.sun.msv.datatype.xsd.StringType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001Z\u0000\risAlwaysVa"
+"lidxq\u0000~\u00008q\u0000~\u0000=t\u0000\u0006stringsr\u00005com.sun.msv.datatype.xsd.WhiteSpa"
+"ceProcessor$Preserve\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000@\u0001q\u0000~\u0000Csq\u0000~\u0000Dq\u0000~\u0000Rq\u0000~\u0000=s"
+"q\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\rdatabase-pathq\u0000~\u0000"
+"Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Osq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bp"
+"q\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\u0007browserq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001b"
+"p\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Osq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\u000bs"
+"mtp-serverq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Osq\u0000~\u0000\u0017"
+"ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\blog-fileq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~"
+"\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Osq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000F"
+"q\u0000~\u0000+sq\u0000~\u0000,t\u0000\tlog-levelq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007"
+"ppq\u0000~\u0000Osq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\ncache-siz"
+"eq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Osq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q"
+"\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\rjes-debuggingq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017pp"
+"sq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Osq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000"
+"+sq\u0000~\u0000,t\u0000\u000esecure-serviceq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000"
+"\u0007ppq\u0000~\u0000Osq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\u0013resource"
+"-cache-sizeq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Osq\u0000~\u0000"
+"\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\u000bbackup-pathq\u0000~\u0000Kq\u0000~\u0000+"
+"sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppq\u0000~\u0000Osq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q"
+"\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000\u0013max-attachment-sizeq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000"
+"\u0000q\u0000~\u0000\u001bp\u0000sq\u0000~\u0000\u0007ppsq\u0000~\u0000\u0000pp\u0000sq\u0000~\u0000\u0017ppsq\u0000~\u0000\u001fq\u0000~\u0000\u001bpsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~"
+"\u0000%q\u0000~\u0000)q\u0000~\u0000+sq\u0000~\u0000,t\u00005onepoint.project.configuration.generate"
+"d.OpLdapConfigq\u0000~\u00000sq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,"
+"t\u0000\u0004ldapq\u0000~\u0000Kq\u0000~\u0000+sq\u0000~\u0000\u0017ppsq\u0000~\u0000\"q\u0000~\u0000\u001bpq\u0000~\u00006q\u0000~\u0000Fq\u0000~\u0000+sq\u0000~\u0000,t\u0000"
+"\rconfigurationq\u0000~\u0000Ksr\u0000\"com.sun.msv.grammar.ExpressionPool\u0000\u0000\u0000"
+"\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\bexpTablet\u0000/Lcom/sun/msv/grammar/ExpressionPool$Cl"
+"osedHash;xpsr\u0000-com.sun.msv.grammar.ExpressionPool$ClosedHash"
+"\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000\u0003I\u0000\u0005countB\u0000\rstreamVersionL\u0000\u0006parentt\u0000$Lcom/sun/msv/"
+"grammar/ExpressionPool;xp\u0000\u0000\u00009\u0001pq\u0000~\u0000\u0012q\u0000~\u0000Nq\u0000~\u0000\\q\u0000~\u0000cq\u0000~\u0000jq\u0000~\u0000"
+"qq\u0000~\u0000xq\u0000~\u0000\u0011q\u0000~\u0000\u007fq\u0000~\u0000\u0086q\u0000~\u0000\u008dq\u0000~\u0000\u0094q\u0000~\u0000\u009bq\u0000~\u0000\fq\u0000~\u0000\u001cq\u0000~\u0000\u00a2q\u0000~\u0000\u0016q\u0000~\u0000"
+"\u0010q\u0000~\u00001q\u0000~\u0000Vq\u0000~\u0000]q\u0000~\u0000dq\u0000~\u0000kq\u0000~\u0000rq\u0000~\u0000yq\u0000~\u0000\u0080q\u0000~\u0000\u0087q\u0000~\u0000\u008eq\u0000~\u0000\u0095q\u0000~\u0000"
+"\rq\u0000~\u0000\u0018q\u0000~\u0000\u009cq\u0000~\u0000\u00a9q\u0000~\u0000\u00a0q\u0000~\u0000\u00adq\u0000~\u0000\tq\u0000~\u0000!q\u0000~\u0000\u0013q\u0000~\u0000\u00a5q\u0000~\u0000\u0014q\u0000~\u0000Lq\u0000~\u0000"
+"Zq\u0000~\u0000aq\u0000~\u0000hq\u0000~\u0000oq\u0000~\u0000vq\u0000~\u0000}q\u0000~\u0000\u0084q\u0000~\u0000\u008bq\u0000~\u0000\u0092q\u0000~\u0000\u0099q\u0000~\u0000\u001eq\u0000~\u0000\u00a4q\u0000~\u0000"
+"\u000eq\u0000~\u0000\u000bq\u0000~\u0000\u0015q\u0000~\u0000\u000fx"));
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
                    case  0 :
                        if (("configuration" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        break;
                    case  1 :
                        if (("ldap" == ___local)&&("" == ___uri)) {
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