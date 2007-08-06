//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.08.02 at 12:39:24 PM EEST 
//


package onepoint.project.configuration.generated.impl;

public class OpDatabaseConfigImpl implements onepoint.project.configuration.generated.OpDatabaseConfig, com.sun.xml.bind.JAXBObject, onepoint.project.configuration.generated.impl.runtime.UnmarshallableObject, onepoint.project.configuration.generated.impl.runtime.XMLSerializable, onepoint.project.configuration.generated.impl.runtime.ValidatableObject
{

    protected java.lang.String _DatabaseType;
    protected java.lang.String _DatabaseUrl;
    protected java.lang.String _DatabaseDriver;
    protected java.lang.String _Name;
    protected java.lang.String _DatabaseLogin;
    protected java.lang.String _ConnectionPoolMaxSize;
    protected java.lang.String _ConnectionPoolMinSize;
    protected java.lang.String _DatabasePath;
    protected onepoint.project.configuration.generated.OpConfigDatabasePassword _DatabasePassword;
    public final static java.lang.Class version = (onepoint.project.configuration.generated.impl.JAXBVersion.class);
    private static com.sun.msv.grammar.Grammar schemaFragment;

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (onepoint.project.configuration.generated.OpDatabaseConfig.class);
    }

    public java.lang.String getDatabaseType() {
        return _DatabaseType;
    }

    public void setDatabaseType(java.lang.String value) {
        _DatabaseType = value;
    }

    public java.lang.String getDatabaseUrl() {
        return _DatabaseUrl;
    }

    public void setDatabaseUrl(java.lang.String value) {
        _DatabaseUrl = value;
    }

    public java.lang.String getDatabaseDriver() {
        return _DatabaseDriver;
    }

    public void setDatabaseDriver(java.lang.String value) {
        _DatabaseDriver = value;
    }

    public java.lang.String getName() {
        return _Name;
    }

    public void setName(java.lang.String value) {
        _Name = value;
    }

    public java.lang.String getDatabaseLogin() {
        return _DatabaseLogin;
    }

    public void setDatabaseLogin(java.lang.String value) {
        _DatabaseLogin = value;
    }

    public java.lang.String getConnectionPoolMaxSize() {
        return _ConnectionPoolMaxSize;
    }

    public void setConnectionPoolMaxSize(java.lang.String value) {
        _ConnectionPoolMaxSize = value;
    }

    public java.lang.String getConnectionPoolMinSize() {
        return _ConnectionPoolMinSize;
    }

    public void setConnectionPoolMinSize(java.lang.String value) {
        _ConnectionPoolMinSize = value;
    }

    public java.lang.String getDatabasePath() {
        return _DatabasePath;
    }

    public void setDatabasePath(java.lang.String value) {
        _DatabasePath = value;
    }

    public onepoint.project.configuration.generated.OpConfigDatabasePassword getDatabasePassword() {
        return _DatabasePassword;
    }

    public void setDatabasePassword(onepoint.project.configuration.generated.OpConfigDatabasePassword value) {
        _DatabasePassword = value;
    }

    public onepoint.project.configuration.generated.impl.runtime.UnmarshallingEventHandler createUnmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context) {
        return new onepoint.project.configuration.generated.impl.OpDatabaseConfigImpl.Unmarshaller(context);
    }

    public void serializeBody(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        context.startElement("", "database-type");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _DatabaseType), "DatabaseType");
        } catch (java.lang.Exception e) {
            onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "database-driver");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _DatabaseDriver), "DatabaseDriver");
        } catch (java.lang.Exception e) {
            onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "database-url");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _DatabaseUrl), "DatabaseUrl");
        } catch (java.lang.Exception e) {
            onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "database-login");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _DatabaseLogin), "DatabaseLogin");
        } catch (java.lang.Exception e) {
            onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "database-password");
        context.childAsURIs(((com.sun.xml.bind.JAXBObject) _DatabasePassword), "DatabasePassword");
        context.endNamespaceDecls();
        context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _DatabasePassword), "DatabasePassword");
        context.endAttributes();
        context.childAsBody(((com.sun.xml.bind.JAXBObject) _DatabasePassword), "DatabasePassword");
        context.endElement();
        if (_DatabasePath!= null) {
            context.startElement("", "database-path");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(((java.lang.String) _DatabasePath), "DatabasePath");
            } catch (java.lang.Exception e) {
                onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (_ConnectionPoolMinSize!= null) {
            context.startElement("", "connection-pool-min-size");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(((java.lang.String) _ConnectionPoolMinSize), "ConnectionPoolMinSize");
            } catch (java.lang.Exception e) {
                onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (_ConnectionPoolMaxSize!= null) {
            context.startElement("", "connection-pool-max-size");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(((java.lang.String) _ConnectionPoolMaxSize), "ConnectionPoolMaxSize");
            } catch (java.lang.Exception e) {
                onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
    }

    public void serializeAttributes(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        if (_Name!= null) {
            context.startAttribute("", "name");
            try {
                context.text(((java.lang.String) _Name), "Name");
            } catch (java.lang.Exception e) {
                onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endAttribute();
        }
    }

    public void serializeURIs(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
    }

    public java.lang.Class getPrimaryInterface() {
        return (onepoint.project.configuration.generated.OpDatabaseConfig.class);
    }

    public com.sun.msv.verifier.DocumentDeclaration createRawValidator() {
        if (schemaFragment == null) {
            schemaFragment = com.sun.xml.bind.validator.SchemaDeserializer.deserialize((
 "\u00ac\u00ed\u0000\u0005sr\u0000\u001fcom.sun.msv.grammar.SequenceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.su"
+"n.msv.grammar.BinaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0004exp1t\u0000 Lcom/sun/msv/gra"
+"mmar/Expression;L\u0000\u0004exp2q\u0000~\u0000\u0002xr\u0000\u001ecom.sun.msv.grammar.Expressi"
+"on\u00f8\u0018\u0082\u00e8N5~O\u0002\u0000\u0002L\u0000\u0013epsilonReducibilityt\u0000\u0013Ljava/lang/Boolean;L\u0000\u000b"
+"expandedExpq\u0000~\u0000\u0002xpppsr\u0000!com.sun.msv.grammar.InterleaveExp\u0000\u0000\u0000"
+"\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0001ppsq\u0000~\u0000\u0006ppsq\u0000~\u0000\u0006ppsq\u0000~\u0000\u0006ppsq\u0000~\u0000\u0006ppsq\u0000~\u0000\u0006ppsq\u0000~"
+"\u0000\u0006ppsr\u0000\'com.sun.msv.grammar.trex.ElementPattern\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000"
+"\tnameClasst\u0000\u001fLcom/sun/msv/grammar/NameClass;xr\u0000\u001ecom.sun.msv."
+"grammar.ElementExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002Z\u0000\u001aignoreUndeclaredAttributesL\u0000"
+"\fcontentModelq\u0000~\u0000\u0002xq\u0000~\u0000\u0003pp\u0000sq\u0000~\u0000\u0000ppsr\u0000\u001bcom.sun.msv.grammar.D"
+"ataExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\u0002dtt\u0000\u001fLorg/relaxng/datatype/Datatype;L\u0000\u0006e"
+"xceptq\u0000~\u0000\u0002L\u0000\u0004namet\u0000\u001dLcom/sun/msv/util/StringPair;xq\u0000~\u0000\u0003ppsr\u0000"
+"#com.sun.msv.datatype.xsd.StringType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001Z\u0000\risAlwaysVa"
+"lidxr\u0000*com.sun.msv.datatype.xsd.BuiltinAtomicType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000"
+"xr\u0000%com.sun.msv.datatype.xsd.ConcreteType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\'com."
+"sun.msv.datatype.xsd.XSDatatypeImpl\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\fnamespaceUr"
+"it\u0000\u0012Ljava/lang/String;L\u0000\btypeNameq\u0000~\u0000\u001bL\u0000\nwhiteSpacet\u0000.Lcom/s"
+"un/msv/datatype/xsd/WhiteSpaceProcessor;xpt\u0000 http://www.w3.o"
+"rg/2001/XMLSchemat\u0000\u0006stringsr\u00005com.sun.msv.datatype.xsd.White"
+"SpaceProcessor$Preserve\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000,com.sun.msv.datatype.x"
+"sd.WhiteSpaceProcessor\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xp\u0001sr\u00000com.sun.msv.grammar."
+"Expression$NullSetExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003ppsr\u0000\u001bcom.sun.m"
+"sv.util.StringPair\u00d0t\u001ejB\u008f\u008d\u00a0\u0002\u0000\u0002L\u0000\tlocalNameq\u0000~\u0000\u001bL\u0000\fnamespaceUR"
+"Iq\u0000~\u0000\u001bxpq\u0000~\u0000\u001fq\u0000~\u0000\u001esr\u0000\u001dcom.sun.msv.grammar.ChoiceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002"
+"\u0000\u0000xq\u0000~\u0000\u0001ppsr\u0000 com.sun.msv.grammar.AttributeExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003"
+"expq\u0000~\u0000\u0002L\u0000\tnameClassq\u0000~\u0000\u000fxq\u0000~\u0000\u0003sr\u0000\u0011java.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee"
+"\u0002\u0000\u0001Z\u0000\u0005valuexp\u0000psq\u0000~\u0000\u0013ppsr\u0000\"com.sun.msv.datatype.xsd.QnameTyp"
+"e\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0018q\u0000~\u0000\u001et\u0000\u0005QNamesr\u00005com.sun.msv.datatype.xsd."
+"WhiteSpaceProcessor$Collapse\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000!q\u0000~\u0000$sq\u0000~\u0000%q\u0000~\u0000"
+"0q\u0000~\u0000\u001esr\u0000#com.sun.msv.grammar.SimpleNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\tl"
+"ocalNameq\u0000~\u0000\u001bL\u0000\fnamespaceURIq\u0000~\u0000\u001bxr\u0000\u001dcom.sun.msv.grammar.Nam"
+"eClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpt\u0000\u0004typet\u0000)http://www.w3.org/2001/XMLSchem"
+"a-instancesr\u00000com.sun.msv.grammar.Expression$EpsilonExpressi"
+"on\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003sq\u0000~\u0000+\u0001q\u0000~\u0000:sq\u0000~\u00004t\u0000\rdatabase-typet\u0000\u0000sq\u0000~"
+"\u0000\u000epp\u0000sq\u0000~\u0000\u0000ppq\u0000~\u0000\u0016sq\u0000~\u0000\'ppsq\u0000~\u0000)q\u0000~\u0000,pq\u0000~\u0000-q\u0000~\u00006q\u0000~\u0000:sq\u0000~\u00004t"
+"\u0000\u000fdatabase-driverq\u0000~\u0000>sq\u0000~\u0000\u000epp\u0000sq\u0000~\u0000\u0000ppq\u0000~\u0000\u0016sq\u0000~\u0000\'ppsq\u0000~\u0000)q\u0000"
+"~\u0000,pq\u0000~\u0000-q\u0000~\u00006q\u0000~\u0000:sq\u0000~\u00004t\u0000\fdatabase-urlq\u0000~\u0000>sq\u0000~\u0000\u000epp\u0000sq\u0000~\u0000\u0000"
+"ppq\u0000~\u0000\u0016sq\u0000~\u0000\'ppsq\u0000~\u0000)q\u0000~\u0000,pq\u0000~\u0000-q\u0000~\u00006q\u0000~\u0000:sq\u0000~\u00004t\u0000\u000edatabase-"
+"loginq\u0000~\u0000>sq\u0000~\u0000\u000epp\u0000sq\u0000~\u0000\u0000ppsq\u0000~\u0000\u000epp\u0000sq\u0000~\u0000\'ppsr\u0000 com.sun.msv."
+"grammar.OneOrMoreExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001ccom.sun.msv.grammar.Unary"
+"Exp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\u0003expq\u0000~\u0000\u0002xq\u0000~\u0000\u0003q\u0000~\u0000,psq\u0000~\u0000)q\u0000~\u0000,psr\u00002com.sun"
+".msv.grammar.Expression$AnyStringExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003"
+"q\u0000~\u0000;q\u0000~\u0000Zsr\u0000 com.sun.msv.grammar.AnyNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000"
+"~\u00005q\u0000~\u0000:sq\u0000~\u00004t\u0000Aonepoint.project.configuration.generated.Op"
+"ConfigDatabasePasswordt\u0000+http://java.sun.com/jaxb/xjc/dummy-"
+"elementssq\u0000~\u0000\'ppsq\u0000~\u0000)q\u0000~\u0000,pq\u0000~\u0000-q\u0000~\u00006q\u0000~\u0000:sq\u0000~\u00004t\u0000\u0011database"
+"-passwordq\u0000~\u0000>sq\u0000~\u0000\'ppsq\u0000~\u0000\u000eq\u0000~\u0000,p\u0000sq\u0000~\u0000\u0000ppq\u0000~\u0000\u0016sq\u0000~\u0000\'ppsq\u0000~"
+"\u0000)q\u0000~\u0000,pq\u0000~\u0000-q\u0000~\u00006q\u0000~\u0000:sq\u0000~\u00004t\u0000\rdatabase-pathq\u0000~\u0000>q\u0000~\u0000:sq\u0000~\u0000"
+"\'ppsq\u0000~\u0000\u000eq\u0000~\u0000,p\u0000sq\u0000~\u0000\u0000ppq\u0000~\u0000\u0016sq\u0000~\u0000\'ppsq\u0000~\u0000)q\u0000~\u0000,pq\u0000~\u0000-q\u0000~\u00006q"
+"\u0000~\u0000:sq\u0000~\u00004t\u0000\u0018connection-pool-min-sizeq\u0000~\u0000>q\u0000~\u0000:sq\u0000~\u0000\'ppsq\u0000~\u0000"
+"\u000eq\u0000~\u0000,p\u0000sq\u0000~\u0000\u0000ppq\u0000~\u0000\u0016sq\u0000~\u0000\'ppsq\u0000~\u0000)q\u0000~\u0000,pq\u0000~\u0000-q\u0000~\u00006q\u0000~\u0000:sq\u0000~"
+"\u00004t\u0000\u0018connection-pool-max-sizeq\u0000~\u0000>q\u0000~\u0000:sq\u0000~\u0000\'ppsq\u0000~\u0000)q\u0000~\u0000,pq"
+"\u0000~\u0000\u0016sq\u0000~\u00004t\u0000\u0004nameq\u0000~\u0000>q\u0000~\u0000:sr\u0000\"com.sun.msv.grammar.Expressio"
+"nPool\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\bexpTablet\u0000/Lcom/sun/msv/grammar/Expressio"
+"nPool$ClosedHash;xpsr\u0000-com.sun.msv.grammar.ExpressionPool$Cl"
+"osedHash\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000\u0003I\u0000\u0005countB\u0000\rstreamVersionL\u0000\u0006parentt\u0000$Lcom/"
+"sun/msv/grammar/ExpressionPool;xp\u0000\u0000\u0000\u001e\u0001pq\u0000~\u0000\fq\u0000~\u0000\rq\u0000~\u0000yq\u0000~\u0000\tq"
+"\u0000~\u0000Rq\u0000~\u0000\u0005q\u0000~\u0000Wq\u0000~\u0000\u0012q\u0000~\u0000@q\u0000~\u0000Fq\u0000~\u0000Lq\u0000~\u0000fq\u0000~\u0000mq\u0000~\u0000tq\u0000~\u0000dq\u0000~\u0000kq"
+"\u0000~\u0000rq\u0000~\u0000\u000bq\u0000~\u0000(q\u0000~\u0000Aq\u0000~\u0000Gq\u0000~\u0000Mq\u0000~\u0000`q\u0000~\u0000gq\u0000~\u0000nq\u0000~\u0000\nq\u0000~\u0000\bq\u0000~\u0000uq"
+"\u0000~\u0000Tq\u0000~\u0000\u0007x"));
        }
        return new com.sun.msv.verifier.regexp.REDocumentDeclaration(schemaFragment);
    }

    public class Unmarshaller
        extends onepoint.project.configuration.generated.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context) {
            super(context, "--------------------");
        }

        protected Unmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return onepoint.project.configuration.generated.impl.OpDatabaseConfigImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        if (("connection-pool-max-size" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 18;
                            return ;
                        }
                        if (("connection-pool-min-size" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 8;
                            return ;
                        }
                        if (("database-path" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 12;
                            return ;
                        }
                        if (("database-password" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 10;
                            return ;
                        }
                        if (("database-login" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 16;
                            return ;
                        }
                        if (("database-url" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 4;
                            return ;
                        }
                        if (("database-driver" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 6;
                            return ;
                        }
                        if (("database-type" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 14;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  10 :
                        attIdx = context.getAttribute("", "encrypted");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().enterElement(___uri, ___local, ___qname, __atts);
                            return ;
                        }
                        break;
                    case  0 :
                        attIdx = context.getAttribute("", "name");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText1(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        private void eatText1(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _Name = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        public void leaveElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  13 :
                        if (("database-path" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  3 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  10 :
                        attIdx = context.getAttribute("", "encrypted");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveElement(___uri, ___local, ___qname);
                            return ;
                        }
                        break;
                    case  17 :
                        if (("database-login" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  9 :
                        if (("connection-pool-min-size" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  5 :
                        if (("database-url" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  11 :
                        if (("database-password" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  7 :
                        if (("database-driver" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  0 :
                        attIdx = context.getAttribute("", "name");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText1(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                    case  19 :
                        if (("connection-pool-max-size" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  15 :
                        if (("database-type" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
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
                    case  10 :
                        if (("encrypted" == ___local)&&("" == ___uri)) {
                            _DatabasePassword = ((onepoint.project.configuration.generated.impl.OpConfigDatabasePasswordImpl) spawnChildFromEnterAttribute((onepoint.project.configuration.generated.impl.OpConfigDatabasePasswordImpl.class), 11, ___uri, ___local, ___qname));
                            return ;
                        }
                        break;
                    case  0 :
                        if (("name" == ___local)&&("" == ___uri)) {
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
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
                    case  10 :
                        attIdx = context.getAttribute("", "encrypted");
                        if (attIdx >= 0) {
                            context.consumeAttribute(attIdx);
                            context.getCurrentHandler().leaveAttribute(___uri, ___local, ___qname);
                            return ;
                        }
                        break;
                    case  0 :
                        attIdx = context.getAttribute("", "name");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText1(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                    case  2 :
                        if (("name" == ___local)&&("" == ___uri)) {
                            state = 3;
                            return ;
                        }
                        break;
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
                        case  8 :
                            state = 9;
                            eatText2(value);
                            return ;
                        case  12 :
                            state = 13;
                            eatText3(value);
                            return ;
                        case  3 :
                            revertToParentFromText(value);
                            return ;
                        case  4 :
                            state = 5;
                            eatText4(value);
                            return ;
                        case  10 :
                            attIdx = context.getAttribute("", "encrypted");
                            if (attIdx >= 0) {
                                context.consumeAttribute(attIdx);
                                context.getCurrentHandler().text(value);
                                return ;
                            }
                            _DatabasePassword = ((onepoint.project.configuration.generated.impl.OpConfigDatabasePasswordImpl) spawnChildFromText((onepoint.project.configuration.generated.impl.OpConfigDatabasePasswordImpl.class), 11, value));
                            return ;
                        case  0 :
                            attIdx = context.getAttribute("", "name");
                            if (attIdx >= 0) {
                                final java.lang.String v = context.eatAttribute(attIdx);
                                state = 3;
                                eatText1(v);
                                continue outer;
                            }
                            state = 3;
                            continue outer;
                        case  1 :
                            state = 2;
                            eatText1(value);
                            return ;
                        case  14 :
                            state = 15;
                            eatText5(value);
                            return ;
                        case  18 :
                            state = 19;
                            eatText6(value);
                            return ;
                        case  16 :
                            state = 17;
                            eatText7(value);
                            return ;
                        case  6 :
                            state = 7;
                            eatText8(value);
                            return ;
                    }
                } catch (java.lang.RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

        private void eatText2(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ConnectionPoolMinSize = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText3(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _DatabasePath = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText4(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _DatabaseUrl = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText5(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _DatabaseType = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText6(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ConnectionPoolMaxSize = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText7(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _DatabaseLogin = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText8(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _DatabaseDriver = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

    }

}
