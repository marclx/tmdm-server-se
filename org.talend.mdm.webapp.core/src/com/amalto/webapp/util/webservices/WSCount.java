// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation ��1.1.2_01������� R40��
// Generated source version: 1.1.2

package com.amalto.webapp.util.webservices;


public class WSCount {
    protected com.amalto.webapp.util.webservices.WSDataClusterPK wsDataClusterPK;
    protected java.lang.String countPath;
    protected com.amalto.webapp.util.webservices.WSWhereItem whereItem;
    protected int spellTreshold;
    
    public WSCount() {
    }
    
    public WSCount(com.amalto.webapp.util.webservices.WSDataClusterPK wsDataClusterPK, java.lang.String countPath, com.amalto.webapp.util.webservices.WSWhereItem whereItem, int spellTreshold) {
        this.wsDataClusterPK = wsDataClusterPK;
        this.countPath = countPath;
        this.whereItem = whereItem;
        this.spellTreshold = spellTreshold;
    }
    
    public com.amalto.webapp.util.webservices.WSDataClusterPK getWsDataClusterPK() {
        return wsDataClusterPK;
    }
    
    public void setWsDataClusterPK(com.amalto.webapp.util.webservices.WSDataClusterPK wsDataClusterPK) {
        this.wsDataClusterPK = wsDataClusterPK;
    }
    
    public java.lang.String getCountPath() {
        return countPath;
    }
    
    public void setCountPath(java.lang.String countPath) {
        this.countPath = countPath;
    }
    
    public com.amalto.webapp.util.webservices.WSWhereItem getWhereItem() {
        return whereItem;
    }
    
    public void setWhereItem(com.amalto.webapp.util.webservices.WSWhereItem whereItem) {
        this.whereItem = whereItem;
    }
    
    public int getSpellTreshold() {
        return spellTreshold;
    }
    
    public void setSpellTreshold(int spellTreshold) {
        this.spellTreshold = spellTreshold;
    }
}
