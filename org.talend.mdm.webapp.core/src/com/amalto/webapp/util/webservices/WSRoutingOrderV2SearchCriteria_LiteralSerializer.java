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

public class WSRoutingOrderV2SearchCriteria_LiteralSerializer extends LiteralObjectSerializerBase implements Initializable  {
    private static final QName ns1_status_QNAME = new QName("", "status");
    private static final QName ns2_WSRoutingOrderV2Status_TYPE_QNAME = new QName("urn-com-amalto-xtentis-webservice", "WSRoutingOrderV2Status");
    private CombinedSerializer ns2myns2_WSRoutingOrderV2Status__WSRoutingOrderV2Status_LiteralSerializer;
    private static final QName ns1_anyFieldContains_QNAME = new QName("", "anyFieldContains");
    private static final QName ns3_string_TYPE_QNAME = SchemaConstants.QNAME_TYPE_STRING;
    private CombinedSerializer ns3_myns3_string__java_lang_String_String_Serializer;
    private static final QName ns1_nameContains_QNAME = new QName("", "nameContains");
    private static final QName ns1_timeCreatedMin_QNAME = new QName("", "timeCreatedMin");
    private static final QName ns3_long_TYPE_QNAME = SchemaConstants.QNAME_TYPE_LONG;
    private CombinedSerializer ns3_myns3__long__long_Long_Serializer;
    private static final QName ns1_timeCreatedMax_QNAME = new QName("", "timeCreatedMax");
    private static final QName ns1_timeScheduledMin_QNAME = new QName("", "timeScheduledMin");
    private static final QName ns1_timeScheduledMax_QNAME = new QName("", "timeScheduledMax");
    private static final QName ns1_timeLastRunStartedMin_QNAME = new QName("", "timeLastRunStartedMin");
    private static final QName ns1_timeLastRunStartedMax_QNAME = new QName("", "timeLastRunStartedMax");
    private static final QName ns1_timeLastRunCompletedMin_QNAME = new QName("", "timeLastRunCompletedMin");
    private static final QName ns1_timeLastRunCompletedMax_QNAME = new QName("", "timeLastRunCompletedMax");
    private static final QName ns1_itemPKConceptContains_QNAME = new QName("", "itemPKConceptContains");
    private static final QName ns1_itemPKIDFieldsContain_QNAME = new QName("", "itemPKIDFieldsContain");
    private static final QName ns1_serviceJNDIContains_QNAME = new QName("", "serviceJNDIContains");
    private static final QName ns1_serviceParametersContain_QNAME = new QName("", "serviceParametersContain");
    private static final QName ns1_messageContain_QNAME = new QName("", "messageContain");
    
    public WSRoutingOrderV2SearchCriteria_LiteralSerializer(QName type, String encodingStyle) {
        this(type, encodingStyle, false);
    }
    
    public WSRoutingOrderV2SearchCriteria_LiteralSerializer(QName type, String encodingStyle, boolean encodeType) {
        super(type, true, encodingStyle, encodeType);
    }
    
    public void initialize(InternalTypeMappingRegistry registry) throws Exception {
        ns2myns2_WSRoutingOrderV2Status__WSRoutingOrderV2Status_LiteralSerializer = (CombinedSerializer)registry.getSerializer("", com.amalto.webapp.util.webservices.WSRoutingOrderV2Status.class, ns2_WSRoutingOrderV2Status_TYPE_QNAME);
        ns3_myns3_string__java_lang_String_String_Serializer = (CombinedSerializer)registry.getSerializer("", java.lang.String.class, ns3_string_TYPE_QNAME);
        ns3_myns3__long__long_Long_Serializer = (CombinedSerializer)registry.getSerializer("", long.class, ns3_long_TYPE_QNAME);
    }
    
    public Object doDeserialize(XMLReader reader,
        SOAPDeserializationContext context) throws Exception {
        com.amalto.webapp.util.webservices.WSRoutingOrderV2SearchCriteria instance = new com.amalto.webapp.util.webservices.WSRoutingOrderV2SearchCriteria();
        Object member=null;
        QName elementName;
        List values;
        Object value;
        
        reader.nextElementContent();
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_status_QNAME)) {
                member = ns2myns2_WSRoutingOrderV2Status__WSRoutingOrderV2Status_LiteralSerializer.deserialize(ns1_status_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setStatus((com.amalto.webapp.util.webservices.WSRoutingOrderV2Status)member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_status_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_anyFieldContains_QNAME)) {
                member = ns3_myns3_string__java_lang_String_String_Serializer.deserialize(ns1_anyFieldContains_QNAME, reader, context);
                instance.setAnyFieldContains((java.lang.String)member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_anyFieldContains_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_nameContains_QNAME)) {
                member = ns3_myns3_string__java_lang_String_String_Serializer.deserialize(ns1_nameContains_QNAME, reader, context);
                instance.setNameContains((java.lang.String)member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_nameContains_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_timeCreatedMin_QNAME)) {
                member = ns3_myns3__long__long_Long_Serializer.deserialize(ns1_timeCreatedMin_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTimeCreatedMin(((Long)member).longValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_timeCreatedMin_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_timeCreatedMax_QNAME)) {
                member = ns3_myns3__long__long_Long_Serializer.deserialize(ns1_timeCreatedMax_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTimeCreatedMax(((Long)member).longValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_timeCreatedMax_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_timeScheduledMin_QNAME)) {
                member = ns3_myns3__long__long_Long_Serializer.deserialize(ns1_timeScheduledMin_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTimeScheduledMin(((Long)member).longValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_timeScheduledMin_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_timeScheduledMax_QNAME)) {
                member = ns3_myns3__long__long_Long_Serializer.deserialize(ns1_timeScheduledMax_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTimeScheduledMax(((Long)member).longValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_timeScheduledMax_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_timeLastRunStartedMin_QNAME)) {
                member = ns3_myns3__long__long_Long_Serializer.deserialize(ns1_timeLastRunStartedMin_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTimeLastRunStartedMin(((Long)member).longValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_timeLastRunStartedMin_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_timeLastRunStartedMax_QNAME)) {
                member = ns3_myns3__long__long_Long_Serializer.deserialize(ns1_timeLastRunStartedMax_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTimeLastRunStartedMax(((Long)member).longValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_timeLastRunStartedMax_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_timeLastRunCompletedMin_QNAME)) {
                member = ns3_myns3__long__long_Long_Serializer.deserialize(ns1_timeLastRunCompletedMin_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTimeLastRunCompletedMin(((Long)member).longValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_timeLastRunCompletedMin_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_timeLastRunCompletedMax_QNAME)) {
                member = ns3_myns3__long__long_Long_Serializer.deserialize(ns1_timeLastRunCompletedMax_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTimeLastRunCompletedMax(((Long)member).longValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_timeLastRunCompletedMax_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_itemPKConceptContains_QNAME)) {
                member = ns3_myns3_string__java_lang_String_String_Serializer.deserialize(ns1_itemPKConceptContains_QNAME, reader, context);
                instance.setItemPKConceptContains((java.lang.String)member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_itemPKConceptContains_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_itemPKIDFieldsContain_QNAME)) {
                member = ns3_myns3_string__java_lang_String_String_Serializer.deserialize(ns1_itemPKIDFieldsContain_QNAME, reader, context);
                instance.setItemPKIDFieldsContain((java.lang.String)member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_itemPKIDFieldsContain_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_serviceJNDIContains_QNAME)) {
                member = ns3_myns3_string__java_lang_String_String_Serializer.deserialize(ns1_serviceJNDIContains_QNAME, reader, context);
                instance.setServiceJNDIContains((java.lang.String)member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_serviceJNDIContains_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_serviceParametersContain_QNAME)) {
                member = ns3_myns3_string__java_lang_String_String_Serializer.deserialize(ns1_serviceParametersContain_QNAME, reader, context);
                instance.setServiceParametersContain((java.lang.String)member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_serviceParametersContain_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_messageContain_QNAME)) {
                member = ns3_myns3_string__java_lang_String_String_Serializer.deserialize(ns1_messageContain_QNAME, reader, context);
                instance.setMessageContain((java.lang.String)member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_messageContain_QNAME, reader.getName() });
            }
        }
        else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        
        XMLReaderUtil.verifyReaderState(reader, XMLReader.END);
        return (Object)instance;
    }
    
    public void doSerializeAttributes(Object obj, XMLWriter writer, SOAPSerializationContext context) throws Exception {
        com.amalto.webapp.util.webservices.WSRoutingOrderV2SearchCriteria instance = (com.amalto.webapp.util.webservices.WSRoutingOrderV2SearchCriteria)obj;
        
    }
    public void doSerialize(Object obj, XMLWriter writer, SOAPSerializationContext context) throws Exception {
        com.amalto.webapp.util.webservices.WSRoutingOrderV2SearchCriteria instance = (com.amalto.webapp.util.webservices.WSRoutingOrderV2SearchCriteria)obj;
        
        if (instance.getStatus() == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns2myns2_WSRoutingOrderV2Status__WSRoutingOrderV2Status_LiteralSerializer.serialize(instance.getStatus(), ns1_status_QNAME, null, writer, context);
        ns3_myns3_string__java_lang_String_String_Serializer.serialize(instance.getAnyFieldContains(), ns1_anyFieldContains_QNAME, null, writer, context);
        ns3_myns3_string__java_lang_String_String_Serializer.serialize(instance.getNameContains(), ns1_nameContains_QNAME, null, writer, context);
        if (new Long(instance.getTimeCreatedMin()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns3_myns3__long__long_Long_Serializer.serialize(new Long(instance.getTimeCreatedMin()), ns1_timeCreatedMin_QNAME, null, writer, context);
        if (new Long(instance.getTimeCreatedMax()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns3_myns3__long__long_Long_Serializer.serialize(new Long(instance.getTimeCreatedMax()), ns1_timeCreatedMax_QNAME, null, writer, context);
        if (new Long(instance.getTimeScheduledMin()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns3_myns3__long__long_Long_Serializer.serialize(new Long(instance.getTimeScheduledMin()), ns1_timeScheduledMin_QNAME, null, writer, context);
        if (new Long(instance.getTimeScheduledMax()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns3_myns3__long__long_Long_Serializer.serialize(new Long(instance.getTimeScheduledMax()), ns1_timeScheduledMax_QNAME, null, writer, context);
        if (new Long(instance.getTimeLastRunStartedMin()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns3_myns3__long__long_Long_Serializer.serialize(new Long(instance.getTimeLastRunStartedMin()), ns1_timeLastRunStartedMin_QNAME, null, writer, context);
        if (new Long(instance.getTimeLastRunStartedMax()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns3_myns3__long__long_Long_Serializer.serialize(new Long(instance.getTimeLastRunStartedMax()), ns1_timeLastRunStartedMax_QNAME, null, writer, context);
        if (new Long(instance.getTimeLastRunCompletedMin()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns3_myns3__long__long_Long_Serializer.serialize(new Long(instance.getTimeLastRunCompletedMin()), ns1_timeLastRunCompletedMin_QNAME, null, writer, context);
        if (new Long(instance.getTimeLastRunCompletedMax()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns3_myns3__long__long_Long_Serializer.serialize(new Long(instance.getTimeLastRunCompletedMax()), ns1_timeLastRunCompletedMax_QNAME, null, writer, context);
        ns3_myns3_string__java_lang_String_String_Serializer.serialize(instance.getItemPKConceptContains(), ns1_itemPKConceptContains_QNAME, null, writer, context);
        ns3_myns3_string__java_lang_String_String_Serializer.serialize(instance.getItemPKIDFieldsContain(), ns1_itemPKIDFieldsContain_QNAME, null, writer, context);
        ns3_myns3_string__java_lang_String_String_Serializer.serialize(instance.getServiceJNDIContains(), ns1_serviceJNDIContains_QNAME, null, writer, context);
        ns3_myns3_string__java_lang_String_String_Serializer.serialize(instance.getServiceParametersContain(), ns1_serviceParametersContain_QNAME, null, writer, context);
        ns3_myns3_string__java_lang_String_String_Serializer.serialize(instance.getMessageContain(), ns1_messageContain_QNAME, null, writer, context);
    }
}
