// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation （1.1.2_01，编译版 R40）
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSProcessFileUsingTransformerAsBackgroundJob {
    protected java.lang.String fileName;
    protected java.lang.String contentType;
    protected com.amalto.core.webservice.WSTransformerPK wsTransformerPK;
    protected com.amalto.core.webservice.WSOutputDecisionTable wsOutputDecisionTable;
    
    public WSProcessFileUsingTransformerAsBackgroundJob() {
    }
    
    public WSProcessFileUsingTransformerAsBackgroundJob(java.lang.String fileName, java.lang.String contentType, com.amalto.core.webservice.WSTransformerPK wsTransformerPK, com.amalto.core.webservice.WSOutputDecisionTable wsOutputDecisionTable) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.wsTransformerPK = wsTransformerPK;
        this.wsOutputDecisionTable = wsOutputDecisionTable;
    }
    
    public java.lang.String getFileName() {
        return fileName;
    }
    
    public void setFileName(java.lang.String fileName) {
        this.fileName = fileName;
    }
    
    public java.lang.String getContentType() {
        return contentType;
    }
    
    public void setContentType(java.lang.String contentType) {
        this.contentType = contentType;
    }
    
    public com.amalto.core.webservice.WSTransformerPK getWsTransformerPK() {
        return wsTransformerPK;
    }
    
    public void setWsTransformerPK(com.amalto.core.webservice.WSTransformerPK wsTransformerPK) {
        this.wsTransformerPK = wsTransformerPK;
    }
    
    public com.amalto.core.webservice.WSOutputDecisionTable getWsOutputDecisionTable() {
        return wsOutputDecisionTable;
    }
    
    public void setWsOutputDecisionTable(com.amalto.core.webservice.WSOutputDecisionTable wsOutputDecisionTable) {
        this.wsOutputDecisionTable = wsOutputDecisionTable;
    }
}
