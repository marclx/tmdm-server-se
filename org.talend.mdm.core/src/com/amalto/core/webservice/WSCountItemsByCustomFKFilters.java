// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation （1.1.2_01，编译版 R40）
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSCountItemsByCustomFKFilters {
    protected com.amalto.core.webservice.WSDataClusterPK wsDataClusterPK;
    protected java.lang.String conceptName;
    protected java.lang.String injectedXpath;
    
    public WSCountItemsByCustomFKFilters() {
    }
    
    public WSCountItemsByCustomFKFilters(com.amalto.core.webservice.WSDataClusterPK wsDataClusterPK, java.lang.String conceptName, java.lang.String injectedXpath) {
        this.wsDataClusterPK = wsDataClusterPK;
        this.conceptName = conceptName;
        this.injectedXpath = injectedXpath;
    }
    
    public com.amalto.core.webservice.WSDataClusterPK getWsDataClusterPK() {
        return wsDataClusterPK;
    }
    
    public void setWsDataClusterPK(com.amalto.core.webservice.WSDataClusterPK wsDataClusterPK) {
        this.wsDataClusterPK = wsDataClusterPK;
    }
    
    public java.lang.String getConceptName() {
        return conceptName;
    }
    
    public void setConceptName(java.lang.String conceptName) {
        this.conceptName = conceptName;
    }
    
    public java.lang.String getInjectedXpath() {
        return injectedXpath;
    }
    
    public void setInjectedXpath(java.lang.String injectedXpath) {
        this.injectedXpath = injectedXpath;
    }
}
