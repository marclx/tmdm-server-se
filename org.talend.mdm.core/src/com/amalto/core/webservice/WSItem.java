// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation （1.1.2_01，编译版 R40）
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSItem {
    protected com.amalto.core.webservice.WSDataClusterPK wsDataClusterPK;
    protected java.lang.String dataModelName;
    protected java.lang.String conceptName;
    protected java.lang.String[] ids;
    protected long insertionTime;
    protected java.lang.String taskId;
    protected java.lang.String content;
    
    public WSItem() {
    }
    
    public WSItem(WSDataClusterPK wsDataClusterPK, String dataModelName, String conceptName, String[] ids, long insertionTime, String taskId, String content) {
        this.wsDataClusterPK = wsDataClusterPK;
        this.dataModelName = dataModelName;
        this.conceptName = conceptName;
        this.ids = ids;
        this.insertionTime = insertionTime;
        this.taskId = taskId;
        this.content = content;
    }
    
    public com.amalto.core.webservice.WSDataClusterPK getWsDataClusterPK() {
        return wsDataClusterPK;
    }
    
    public void setWsDataClusterPK(com.amalto.core.webservice.WSDataClusterPK wsDataClusterPK) {
        this.wsDataClusterPK = wsDataClusterPK;
    }
    
    public java.lang.String getDataModelName() {
        return dataModelName;
    }
    
    public void setDataModelName(java.lang.String dataModelName) {
        this.dataModelName = dataModelName;
    }
    
    public java.lang.String getConceptName() {
        return conceptName;
    }
    
    public void setConceptName(java.lang.String conceptName) {
        this.conceptName = conceptName;
    }
    
    public java.lang.String[] getIds() {
        return ids;
    }
    
    public void setIds(java.lang.String[] ids) {
        this.ids = ids;
    }
    
    public long getInsertionTime() {
        return insertionTime;
    }
    
    public void setInsertionTime(long insertionTime) {
        this.insertionTime = insertionTime;
    }
    
    public java.lang.String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(java.lang.String taskId) {
        this.taskId = taskId;
    }
    
    public java.lang.String getContent() {
        return content;
    }
    
    public void setContent(java.lang.String content) {
        this.content = content;
    }
}
