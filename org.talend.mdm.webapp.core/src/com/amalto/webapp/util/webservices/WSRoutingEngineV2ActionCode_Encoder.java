// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation 
// Generated source version: 1.1.2

package com.amalto.webapp.util.webservices;


import com.sun.xml.rpc.encoding.simpletype.*;
import javax.xml.namespace.QName;
import com.sun.xml.rpc.streaming.*;

public class WSRoutingEngineV2ActionCode_Encoder extends SimpleTypeEncoderBase {
    
    private static final SimpleTypeEncoder encoder = XSDStringEncoder.getInstance();
    private static final WSRoutingEngineV2ActionCode_Encoder instance = new WSRoutingEngineV2ActionCode_Encoder();
    
    private WSRoutingEngineV2ActionCode_Encoder() {
    }
    
    public static SimpleTypeEncoder getInstance() {
        return instance;
    }
    
    public String objectToString(Object obj, XMLWriter writer) throws Exception {
        java.lang.String value = ((WSRoutingEngineV2ActionCode)obj).getValue();
        return encoder.objectToString(value, writer);
    }
    
    public Object stringToObject(String str, XMLReader reader) throws Exception {
        return WSRoutingEngineV2ActionCode.fromValue((java.lang.String)encoder.stringToObject(str, reader));
    }
    
}