// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation （1.1.2_01，编译版 R40）
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSPutItemWithReport {
    protected com.amalto.core.webservice.WSPutItem wsPutItem;
    protected java.lang.String source;
    protected java.lang.Boolean invokeBeforeSaving;
    
    public WSPutItemWithReport() {
    }
    
    public WSPutItemWithReport(com.amalto.core.webservice.WSPutItem wsPutItem, java.lang.String source, java.lang.Boolean invokeBeforeSaving) {
        this.wsPutItem = wsPutItem;
        this.source = source;
        this.invokeBeforeSaving = invokeBeforeSaving;
    }
    
    public com.amalto.core.webservice.WSPutItem getWsPutItem() {
        return wsPutItem;
    }
    
    public void setWsPutItem(com.amalto.core.webservice.WSPutItem wsPutItem) {
        this.wsPutItem = wsPutItem;
    }
    
    public java.lang.String getSource() {
        return source;
    }
    
    public void setSource(java.lang.String source) {
        this.source = source;
    }
    
    public java.lang.Boolean getInvokeBeforeSaving() {
        return invokeBeforeSaving;
    }
    
    public void setInvokeBeforeSaving(java.lang.Boolean invokeBeforeSaving) {
        this.invokeBeforeSaving = invokeBeforeSaving;
    }
}
