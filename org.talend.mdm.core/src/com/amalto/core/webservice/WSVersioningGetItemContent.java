// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation （1.1.2_01，编译版 R40）
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSVersioningGetItemContent {
    protected java.lang.String versioningSystemName;
    protected com.amalto.core.webservice.WSItemPK wsItemPK;

    public WSVersioningGetItemContent() {
    }
    
    public WSVersioningGetItemContent(String versioningSystemName, WSItemPK wsItemPK) {
        this.versioningSystemName = versioningSystemName;
        this.wsItemPK = wsItemPK;
    }
    
    public java.lang.String getVersioningSystemName() {
        return versioningSystemName;
    }
    
    public void setVersioningSystemName(java.lang.String versioningSystemName) {
        this.versioningSystemName = versioningSystemName;
    }
    
    public com.amalto.core.webservice.WSItemPK getWsItemPK() {
        return wsItemPK;
    }
    
    public void setWsItemPK(com.amalto.core.webservice.WSItemPK wsItemPK) {
        this.wsItemPK = wsItemPK;
    }

}
