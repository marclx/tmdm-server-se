// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation ��1.1.2_01������� R40��
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSGetChildrenItems {
    protected java.lang.String clusterName;
    protected java.lang.String conceptName;
    protected com.amalto.core.webservice.WSStringArray PKXpaths;
    protected java.lang.String FKXpath;
    protected java.lang.String labelXpath;
    protected java.lang.String fatherPK;
    
    public WSGetChildrenItems() {
    }
    
    public WSGetChildrenItems(java.lang.String clusterName, java.lang.String conceptName, com.amalto.core.webservice.WSStringArray PKXpaths, java.lang.String FKXpath, java.lang.String labelXpath, java.lang.String fatherPK) {
        this.clusterName = clusterName;
        this.conceptName = conceptName;
        this.PKXpaths = PKXpaths;
        this.FKXpath = FKXpath;
        this.labelXpath = labelXpath;
        this.fatherPK = fatherPK;
    }
    
    public java.lang.String getClusterName() {
        return clusterName;
    }
    
    public void setClusterName(java.lang.String clusterName) {
        this.clusterName = clusterName;
    }
    
    public java.lang.String getConceptName() {
        return conceptName;
    }
    
    public void setConceptName(java.lang.String conceptName) {
        this.conceptName = conceptName;
    }
    
    public com.amalto.core.webservice.WSStringArray getPKXpaths() {
        return PKXpaths;
    }
    
    public void setPKXpaths(com.amalto.core.webservice.WSStringArray PKXpaths) {
        this.PKXpaths = PKXpaths;
    }
    
    public java.lang.String getFKXpath() {
        return FKXpath;
    }
    
    public void setFKXpath(java.lang.String FKXpath) {
        this.FKXpath = FKXpath;
    }
    
    public java.lang.String getLabelXpath() {
        return labelXpath;
    }
    
    public void setLabelXpath(java.lang.String labelXpath) {
        this.labelXpath = labelXpath;
    }
    
    public java.lang.String getFatherPK() {
        return fatherPK;
    }
    
    public void setFatherPK(java.lang.String fatherPK) {
        this.fatherPK = fatherPK;
    }
}
