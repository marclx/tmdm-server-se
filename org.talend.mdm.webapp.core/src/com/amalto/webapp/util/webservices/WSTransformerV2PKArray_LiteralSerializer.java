// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation 
// Generated source version: 1.1.2

package com.amalto.webapp.util.webservices;

import com.sun.xml.rpc.encoding.*;
import com.sun.xml.rpc.encoding.xsd.XSDConstants;
import com.sun.xml.rpc.encoding.literal.*;
import com.sun.xml.rpc.encoding.literal.DetailFragmentDeserializer;
import com.sun.xml.rpc.encoding.simpletype.*;
import com.sun.xml.rpc.encoding.soap.SOAPConstants;
import com.sun.xml.rpc.encoding.soap.SOAP12Constants;
import com.sun.xml.rpc.streaming.*;
import com.sun.xml.rpc.wsdl.document.schema.SchemaConstants;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.ArrayList;

public class WSTransformerV2PKArray_LiteralSerializer extends LiteralObjectSerializerBase implements Initializable  {
    private static final QName ns1_wsTransformerV2PK_QNAME = new QName("", "wsTransformerV2PK");
    private static final QName ns2_WSTransformerV2PK_TYPE_QNAME = new QName("urn-com-amalto-xtentis-webservice", "WSTransformerV2PK");
    private CombinedSerializer ns2_myWSTransformerV2PK_LiteralSerializer;
    
    public WSTransformerV2PKArray_LiteralSerializer(QName type, String encodingStyle) {
        this(type, encodingStyle, false);
    }
    
    public WSTransformerV2PKArray_LiteralSerializer(QName type, String encodingStyle, boolean encodeType) {
        super(type, true, encodingStyle, encodeType);
    }
    
    public void initialize(InternalTypeMappingRegistry registry) throws Exception {
        ns2_myWSTransformerV2PK_LiteralSerializer = (CombinedSerializer)registry.getSerializer("", com.amalto.webapp.util.webservices.WSTransformerV2PK.class, ns2_WSTransformerV2PK_TYPE_QNAME);
    }
    
    public Object doDeserialize(XMLReader reader,
        SOAPDeserializationContext context) throws Exception {
        com.amalto.webapp.util.webservices.WSTransformerV2PKArray instance = new com.amalto.webapp.util.webservices.WSTransformerV2PKArray();
        Object member=null;
        QName elementName;
        List values;
        Object value;
        
        reader.nextElementContent();
        elementName = reader.getName();
        if ((reader.getState() == XMLReader.START) && (elementName.equals(ns1_wsTransformerV2PK_QNAME))) {
            values = new ArrayList();
            for(;;) {
                elementName = reader.getName();
                if ((reader.getState() == XMLReader.START) && (elementName.equals(ns1_wsTransformerV2PK_QNAME))) {
                    value = ns2_myWSTransformerV2PK_LiteralSerializer.deserialize(ns1_wsTransformerV2PK_QNAME, reader, context);
                    if (value == null) {
                        throw new DeserializationException("literal.unexpectedNull");
                    }
                    values.add(value);
                    reader.nextElementContent();
                } else {
                    break;
                }
            }
            member = new com.amalto.webapp.util.webservices.WSTransformerV2PK[values.size()];
            member = values.toArray((Object[]) member);
            instance.setWsTransformerV2PK((com.amalto.webapp.util.webservices.WSTransformerV2PK[])member);
        }
        else if(!(reader.getState() == XMLReader.END)) {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        
        XMLReaderUtil.verifyReaderState(reader, XMLReader.END);
        return (Object)instance;
    }
    
    public void doSerializeAttributes(Object obj, XMLWriter writer, SOAPSerializationContext context) throws Exception {
        com.amalto.webapp.util.webservices.WSTransformerV2PKArray instance = (com.amalto.webapp.util.webservices.WSTransformerV2PKArray)obj;
        
    }
    public void doSerialize(Object obj, XMLWriter writer, SOAPSerializationContext context) throws Exception {
        com.amalto.webapp.util.webservices.WSTransformerV2PKArray instance = (com.amalto.webapp.util.webservices.WSTransformerV2PKArray)obj;
        
        if (instance.getWsTransformerV2PK() != null) {
            for (int i = 0; i < instance.getWsTransformerV2PK().length; ++i) {
                ns2_myWSTransformerV2PK_LiteralSerializer.serialize(instance.getWsTransformerV2PK()[i], ns1_wsTransformerV2PK_QNAME, null, writer, context);
            }
        }
    }
}
