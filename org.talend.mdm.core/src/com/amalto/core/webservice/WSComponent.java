// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation ��1.1.2_01������� R40��
// Generated source version: 1.1.2

package com.amalto.core.webservice;


import java.util.Map;
import java.util.HashMap;

public class WSComponent {
    private java.lang.String value;
    private static Map valueMap = new HashMap();
    public static final String _DataManagerString = "DataManager";
    public static final String _ServiceString = "Service";
    public static final String _ConnectorString = "Connector";
    
    public static final java.lang.String _DataManager = new java.lang.String(_DataManagerString);
    public static final java.lang.String _Service = new java.lang.String(_ServiceString);
    public static final java.lang.String _Connector = new java.lang.String(_ConnectorString);
    
    public static final WSComponent DataManager = new WSComponent(_DataManager);
    public static final WSComponent Service = new WSComponent(_Service);
    public static final WSComponent Connector = new WSComponent(_Connector);
    
    protected WSComponent(java.lang.String value) {
        this.value = value;
        valueMap.put(this.toString(), this);
    }
    
    public java.lang.String getValue() {
        return value;
    }
    
    public static WSComponent fromValue(java.lang.String value)
        throws java.lang.IllegalStateException {
        if (DataManager.value.equals(value)) {
            return DataManager;
        } else if (Service.value.equals(value)) {
            return Service;
        } else if (Connector.value.equals(value)) {
            return Connector;
        }
        throw new IllegalArgumentException();
    }
    
    public static WSComponent fromString(String value)
        throws java.lang.IllegalStateException {
        WSComponent ret = (WSComponent)valueMap.get(value);
        if (ret != null) {
            return ret;
        }
        if (value.equals(_DataManagerString)) {
            return DataManager;
        } else if (value.equals(_ServiceString)) {
            return Service;
        } else if (value.equals(_ConnectorString)) {
            return Connector;
        }
        throw new IllegalArgumentException();
    }
    
    public String toString() {
        return value.toString();
    }
    
    private Object readResolve()
        throws java.io.ObjectStreamException {
        return fromValue(getValue());
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof WSComponent)) {
            return false;
        }
        return ((WSComponent)obj).value.equals(value);
    }
    
    public int hashCode() {
        return value.hashCode();
    }
}
