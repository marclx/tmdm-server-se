// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation （1.1.2_01，编译版 R40）
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSRole {
    protected java.lang.String name;
    protected java.lang.String description;
    protected com.amalto.core.webservice.WSRoleSpecification[] specification;
    
    public WSRole() {
    }
    
    public WSRole(java.lang.String name, java.lang.String description, com.amalto.core.webservice.WSRoleSpecification[] specification) {
        this.name = name;
        this.description = description;
        this.specification = specification;
    }
    
    public java.lang.String getName() {
        return name;
    }
    
    public void setName(java.lang.String name) {
        this.name = name;
    }
    
    public java.lang.String getDescription() {
        return description;
    }
    
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    
    public com.amalto.core.webservice.WSRoleSpecification[] getSpecification() {
        return specification;
    }
    
    public void setSpecification(com.amalto.core.webservice.WSRoleSpecification[] specification) {
        this.specification = specification;
    }
}
