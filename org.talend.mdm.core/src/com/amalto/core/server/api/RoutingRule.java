/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.server.api;

import com.amalto.core.objects.routing.RoutingRulePOJO;
import com.amalto.core.objects.routing.RoutingRulePOJOPK;

public interface RoutingRule {

    /**
     * Creates or updates a RoutingRule
     */
    public RoutingRulePOJOPK putRoutingRule(RoutingRulePOJO routingRule) throws com.amalto.core.util.XtentisException;

    /**
     * Get RoutingRule
     */
    public RoutingRulePOJO getRoutingRule(RoutingRulePOJOPK pk) throws com.amalto.core.util.XtentisException;

    /**
     * Get a RoutingRule - no exception is thrown: returns null if not found
     */
    public RoutingRulePOJO existsRoutingRule(RoutingRulePOJOPK pk) throws com.amalto.core.util.XtentisException;

    /**
     * Remove a RoutingRule
     */
    public RoutingRulePOJOPK removeRoutingRule(RoutingRulePOJOPK pk) throws com.amalto.core.util.XtentisException;

    /**
     * Retrieve all RoutingRule PKs
     */
    public java.util.Collection<RoutingRulePOJOPK> getRoutingRulePKs(String regex) throws com.amalto.core.util.XtentisException;

}
