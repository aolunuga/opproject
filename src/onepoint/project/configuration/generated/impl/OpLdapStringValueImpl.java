//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.08.14 at 02:38:34 PM EEST 
//


package onepoint.project.configuration.generated.impl;

public class OpLdapStringValueImpl implements onepoint.project.configuration.generated.OpLdapStringValue, com.sun.xml.bind.JAXBObject, onepoint.project.configuration.generated.impl.runtime.UnmarshallableObject, onepoint.project.configuration.generated.impl.runtime.XMLSerializable, onepoint.project.configuration.generated.impl.runtime.ValidatableObject
{

    protected com.sun.xml.bind.util.ListImpl _Replace;
    protected java.lang.String _Value;
    protected boolean has_Synched;
    protected boolean _Synched;
    protected boolean has_Fixed;
    protected boolean _Fixed;
    public final static java.lang.Class version = (onepoint.project.configuration.generated.impl.JAXBVersion.class);
    private static com.sun.msv.grammar.Grammar schemaFragment;

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (onepoint.project.configuration.generated.OpLdapStringValue.class);
    }

    protected com.sun.xml.bind.util.ListImpl _getReplace() {
        if (_Replace == null) {
            _Replace = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _Replace;
    }

    public java.util.List getReplace() {
        return _getReplace();
    }

    public java.lang.String getValue() {
        return _Value;
    }

    public void setValue(java.lang.String value) {
        _Value = value;
    }

    public boolean isSynched() {
        if (!has_Synched) {
            return javax.xml.bind.DatatypeConverter.parseBoolean(com.sun.xml.bind.DatatypeConverterImpl.installHook("true"));
        } else {
            return _Synched;
        }
    }

    public void setSynched(boolean value) {
        _Synched = value;
        has_Synched = true;
    }

    public boolean isFixed() {
        if (!has_Fixed) {
            return javax.xml.bind.DatatypeConverter.parseBoolean(com.sun.xml.bind.DatatypeConverterImpl.installHook("false"));
        } else {
            return _Fixed;
        }
    }

    public void setFixed(boolean value) {
        _Fixed = value;
        has_Fixed = true;
    }

    public onepoint.project.configuration.generated.impl.runtime.UnmarshallingEventHandler createUnmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context) {
        return new onepoint.project.configuration.generated.impl.OpLdapStringValueImpl.Unmarshaller(context);
    }

    public void serializeBody(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_Replace == null)? 0 :_Replace.size());
        while (idx1 != len1) {
            context.startElement("", "replace");
            int idx_0 = idx1;
            context.childAsURIs(((com.sun.xml.bind.JAXBObject) _Replace.get(idx_0 ++)), "Replace");
            context.endNamespaceDecls();
            int idx_1 = idx1;
            context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _Replace.get(idx_1 ++)), "Replace");
            context.endAttributes();
            context.childAsBody(((com.sun.xml.bind.JAXBObject) _Replace.get(idx1 ++)), "Replace");
            context.endElement();
        }
    }

    public void serializeAttributes(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_Replace == null)? 0 :_Replace.size());
        if (has_Fixed) {
            context.startAttribute("", "fixed");
            try {
                context.text(javax.xml.bind.DatatypeConverter.printBoolean(((boolean) _Fixed)), "Fixed");
            } catch (java.lang.Exception e) {
                onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endAttribute();
        }
        if (has_Synched) {
            context.startAttribute("", "synched");
            try {
                context.text(javax.xml.bind.DatatypeConverter.printBoolean(((boolean) _Synched)), "Synched");
            } catch (java.lang.Exception e) {
                onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endAttribute();
        }
        if (_Value!= null) {
            context.startAttribute("", "value");
            try {
                context.text(((java.lang.String) _Value), "Value");
            } catch (java.lang.Exception e) {
                onepoint.project.configuration.generated.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endAttribute();
        }
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public void serializeURIs(onepoint.project.configuration.generated.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_Replace == null)? 0 :_Replace.size());
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public java.lang.Class getPrimaryInterface() {
        return (onepoint.project.configuration.generated.OpLdapStringValue.class);
    }

    public com.sun.msv.verifier.DocumentDeclaration createRawValidator() {
        if (schemaFragment == null) {
            schemaFragment = com.sun.xml.bind.validator.SchemaDeserializer.deserialize((
 "\u00ac\u00ed\u0000\u0005sr\u0000\u001fcom.sun.msv.grammar.SequenceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.su"
+"n.msv.grammar.BinaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0004exp1t\u0000 Lcom/sun/msv/gra"
+"mmar/Expression;L\u0000\u0004exp2q\u0000~\u0000\u0002xr\u0000\u001ecom.sun.msv.grammar.Expressi"
+"on\u00f8\u0018\u0082\u00e8N5~O\u0002\u0000\u0002L\u0000\u0013epsilonReducibilityt\u0000\u0013Ljava/lang/Boolean;L\u0000\u000b"
+"expandedExpq\u0000~\u0000\u0002xpppsq\u0000~\u0000\u0000ppsq\u0000~\u0000\u0000ppsr\u0000\u001dcom.sun.msv.grammar."
+"ChoiceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0001ppsr\u0000 com.sun.msv.grammar.OneOrMor"
+"eExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001ccom.sun.msv.grammar.UnaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000"
+"\u0003expq\u0000~\u0000\u0002xq\u0000~\u0000\u0003sr\u0000\u0011java.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee\u0002\u0000\u0001Z\u0000\u0005valuexp\u0000ps"
+"r\u0000\'com.sun.msv.grammar.trex.ElementPattern\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\tname"
+"Classt\u0000\u001fLcom/sun/msv/grammar/NameClass;xr\u0000\u001ecom.sun.msv.gramm"
+"ar.ElementExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002Z\u0000\u001aignoreUndeclaredAttributesL\u0000\fcont"
+"entModelq\u0000~\u0000\u0002xq\u0000~\u0000\u0003q\u0000~\u0000\u000ep\u0000sq\u0000~\u0000\u0000ppsq\u0000~\u0000\u000fpp\u0000sq\u0000~\u0000\bppsq\u0000~\u0000\nq\u0000~"
+"\u0000\u000epsr\u0000 com.sun.msv.grammar.AttributeExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003expq\u0000~\u0000"
+"\u0002L\u0000\tnameClassq\u0000~\u0000\u0010xq\u0000~\u0000\u0003q\u0000~\u0000\u000epsr\u00002com.sun.msv.grammar.Expres"
+"sion$AnyStringExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003sq\u0000~\u0000\r\u0001q\u0000~\u0000\u001asr\u0000 com"
+".sun.msv.grammar.AnyNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.sun.msv.gram"
+"mar.NameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpsr\u00000com.sun.msv.grammar.Expression"
+"$EpsilonExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003q\u0000~\u0000\u001bq\u0000~\u0000 sr\u0000#com.sun.msv"
+".grammar.SimpleNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\tlocalNamet\u0000\u0012Ljava/lang"
+"/String;L\u0000\fnamespaceURIq\u0000~\u0000\"xq\u0000~\u0000\u001dt\u00006onepoint.project.config"
+"uration.generated.OpLdapReplacet\u0000+http://java.sun.com/jaxb/x"
+"jc/dummy-elementssq\u0000~\u0000\bppsq\u0000~\u0000\u0017q\u0000~\u0000\u000epsr\u0000\u001bcom.sun.msv.grammar"
+".DataExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\u0002dtt\u0000\u001fLorg/relaxng/datatype/Datatype;L\u0000"
+"\u0006exceptq\u0000~\u0000\u0002L\u0000\u0004namet\u0000\u001dLcom/sun/msv/util/StringPair;xq\u0000~\u0000\u0003pps"
+"r\u0000\"com.sun.msv.datatype.xsd.QnameType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000*com.sun."
+"msv.datatype.xsd.BuiltinAtomicType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000%com.sun.msv"
+".datatype.xsd.ConcreteType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\'com.sun.msv.datatyp"
+"e.xsd.XSDatatypeImpl\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\fnamespaceUriq\u0000~\u0000\"L\u0000\btypeNa"
+"meq\u0000~\u0000\"L\u0000\nwhiteSpacet\u0000.Lcom/sun/msv/datatype/xsd/WhiteSpaceP"
+"rocessor;xpt\u0000 http://www.w3.org/2001/XMLSchemat\u0000\u0005QNamesr\u00005co"
+"m.sun.msv.datatype.xsd.WhiteSpaceProcessor$Collapse\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002"
+"\u0000\u0000xr\u0000,com.sun.msv.datatype.xsd.WhiteSpaceProcessor\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000"
+"\u0000xpsr\u00000com.sun.msv.grammar.Expression$NullSetExpression\u0000\u0000\u0000\u0000\u0000"
+"\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003ppsr\u0000\u001bcom.sun.msv.util.StringPair\u00d0t\u001ejB\u008f\u008d\u00a0\u0002\u0000\u0002L\u0000\tl"
+"ocalNameq\u0000~\u0000\"L\u0000\fnamespaceURIq\u0000~\u0000\"xpq\u0000~\u00003q\u0000~\u00002sq\u0000~\u0000!t\u0000\u0004typet\u0000"
+")http://www.w3.org/2001/XMLSchema-instanceq\u0000~\u0000 sq\u0000~\u0000!t\u0000\u0007repl"
+"acet\u0000\u0000q\u0000~\u0000 sq\u0000~\u0000\bppsq\u0000~\u0000\u0017q\u0000~\u0000\u000epsq\u0000~\u0000(ppsr\u0000$com.sun.msv.datat"
+"ype.xsd.BooleanType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000-q\u0000~\u00002t\u0000\u0007booleanq\u0000~\u00006q\u0000~\u0000"
+"8sq\u0000~\u00009q\u0000~\u0000Fq\u0000~\u00002sq\u0000~\u0000!t\u0000\u0005fixedq\u0000~\u0000@q\u0000~\u0000 sq\u0000~\u0000\bppsq\u0000~\u0000\u0017q\u0000~\u0000\u000e"
+"pq\u0000~\u0000Csq\u0000~\u0000!t\u0000\u0007synchedq\u0000~\u0000@q\u0000~\u0000 sq\u0000~\u0000\bppsq\u0000~\u0000\u0017q\u0000~\u0000\u000epsq\u0000~\u0000(pp"
+"sr\u0000#com.sun.msv.datatype.xsd.StringType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001Z\u0000\risAlway"
+"sValidxq\u0000~\u0000-q\u0000~\u00002t\u0000\u0006stringsr\u00005com.sun.msv.datatype.xsd.White"
+"SpaceProcessor$Preserve\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u00005\u0001q\u0000~\u00008sq\u0000~\u00009q\u0000~\u0000Sq\u0000~"
+"\u00002sq\u0000~\u0000!t\u0000\u0005valueq\u0000~\u0000@q\u0000~\u0000 sr\u0000\"com.sun.msv.grammar.Expression"
+"Pool\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\bexpTablet\u0000/Lcom/sun/msv/grammar/Expression"
+"Pool$ClosedHash;xpsr\u0000-com.sun.msv.grammar.ExpressionPool$Clo"
+"sedHash\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000\u0003I\u0000\u0005countB\u0000\rstreamVersionL\u0000\u0006parentt\u0000$Lcom/s"
+"un/msv/grammar/ExpressionPool;xp\u0000\u0000\u0000\f\u0001pq\u0000~\u0000Nq\u0000~\u0000\u0013q\u0000~\u0000\fq\u0000~\u0000&q\u0000"
+"~\u0000Aq\u0000~\u0000\u0016q\u0000~\u0000\u0006q\u0000~\u0000\u0005q\u0000~\u0000Jq\u0000~\u0000\tq\u0000~\u0000\u0015q\u0000~\u0000\u0007x"));
        }
        return new com.sun.msv.verifier.regexp.REDocumentDeclaration(schemaFragment);
    }

    public class Unmarshaller
        extends onepoint.project.configuration.generated.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context) {
            super(context, "-------------");
        }

        protected Unmarshaller(onepoint.project.configuration.generated.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return onepoint.project.configuration.generated.impl.OpLdapStringValueImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  0 :
                        attIdx = context.getAttribute("", "fixed");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText1(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                    case  3 :
                        attIdx = context.getAttribute("", "synched");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 6;
                            eatText2(v);
                            continue outer;
                        }
                        state = 6;
                        continue outer;
                    case  6 :
                        attIdx = context.getAttribute("", "value");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 9;
                            eatText3(v);
                            continue outer;
                        }
                        state = 9;
                        continue outer;
                    case  10 :
                        if (("from" == ___local)&&("" == ___uri)) {
                            _getReplace().add(((onepoint.project.configuration.generated.impl.OpLdapReplaceImpl) spawnChildFromEnterElement((onepoint.project.configuration.generated.impl.OpLdapReplaceImpl.class), 11, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        break;
                    case  9 :
                        if (("replace" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 10;
                            return ;
                        }
                        state = 12;
                        continue outer;
                    case  12 :
                        if (("replace" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 10;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        private void eatText1(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _Fixed = javax.xml.bind.DatatypeConverter.parseBoolean(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_Fixed = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText2(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _Synched = javax.xml.bind.DatatypeConverter.parseBoolean(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_Synched = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText3(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _Value = value;
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
                    case  0 :
                        attIdx = context.getAttribute("", "fixed");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText1(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                    case  3 :
                        attIdx = context.getAttribute("", "synched");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 6;
                            eatText2(v);
                            continue outer;
                        }
                        state = 6;
                        continue outer;
                    case  6 :
                        attIdx = context.getAttribute("", "value");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 9;
                            eatText3(v);
                            continue outer;
                        }
                        state = 9;
                        continue outer;
                    case  9 :
                        state = 12;
                        continue outer;
                    case  12 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  11 :
                        if (("replace" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 12;
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
                    case  0 :
                        if (("fixed" == ___local)&&("" == ___uri)) {
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
                    case  3 :
                        if (("synched" == ___local)&&("" == ___uri)) {
                            state = 4;
                            return ;
                        }
                        state = 6;
                        continue outer;
                    case  6 :
                        if (("value" == ___local)&&("" == ___uri)) {
                            state = 7;
                            return ;
                        }
                        state = 9;
                        continue outer;
                    case  9 :
                        state = 12;
                        continue outer;
                    case  12 :
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
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
                    case  0 :
                        attIdx = context.getAttribute("", "fixed");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText1(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                    case  3 :
                        attIdx = context.getAttribute("", "synched");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 6;
                            eatText2(v);
                            continue outer;
                        }
                        state = 6;
                        continue outer;
                    case  2 :
                        if (("fixed" == ___local)&&("" == ___uri)) {
                            state = 3;
                            return ;
                        }
                        break;
                    case  5 :
                        if (("synched" == ___local)&&("" == ___uri)) {
                            state = 6;
                            return ;
                        }
                        break;
                    case  6 :
                        attIdx = context.getAttribute("", "value");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 9;
                            eatText3(v);
                            continue outer;
                        }
                        state = 9;
                        continue outer;
                    case  8 :
                        if (("value" == ___local)&&("" == ___uri)) {
                            state = 9;
                            return ;
                        }
                        break;
                    case  9 :
                        state = 12;
                        continue outer;
                    case  12 :
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
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
                        case  0 :
                            attIdx = context.getAttribute("", "fixed");
                            if (attIdx >= 0) {
                                final java.lang.String v = context.eatAttribute(attIdx);
                                state = 3;
                                eatText1(v);
                                continue outer;
                            }
                            state = 3;
                            continue outer;
                        case  7 :
                            state = 8;
                            eatText3(value);
                            return ;
                        case  3 :
                            attIdx = context.getAttribute("", "synched");
                            if (attIdx >= 0) {
                                final java.lang.String v = context.eatAttribute(attIdx);
                                state = 6;
                                eatText2(v);
                                continue outer;
                            }
                            state = 6;
                            continue outer;
                        case  4 :
                            state = 5;
                            eatText2(value);
                            return ;
                        case  6 :
                            attIdx = context.getAttribute("", "value");
                            if (attIdx >= 0) {
                                final java.lang.String v = context.eatAttribute(attIdx);
                                state = 9;
                                eatText3(v);
                                continue outer;
                            }
                            state = 9;
                            continue outer;
                        case  1 :
                            state = 2;
                            eatText1(value);
                            return ;
                        case  9 :
                            state = 12;
                            continue outer;
                        case  12 :
                            revertToParentFromText(value);
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