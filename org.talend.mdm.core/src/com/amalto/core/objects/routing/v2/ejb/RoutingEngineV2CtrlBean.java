package com.amalto.core.objects.routing.v2.ejb;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.TimedObject;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.naming.NamingException;
import javax.xml.transform.TransformerException;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.util.core.MDMConfiguration;

import bsh.EvalError;
import bsh.Interpreter;

import com.amalto.core.ejb.ItemPOJO;
import com.amalto.core.ejb.ItemPOJOPK;
import com.amalto.core.ejb.local.ItemCtrl2Local;
import com.amalto.core.objects.routing.v2.ejb.local.RoutingOrderV2CtrlLocal;
import com.amalto.core.objects.routing.v2.ejb.local.RoutingRuleCtrlLocal;
import com.amalto.core.objects.universe.ejb.UniversePOJO;
import com.amalto.core.server.RoutingEngine;
import com.amalto.core.util.LocalUser;
import com.amalto.core.util.SynchronizedNow;
import com.amalto.core.util.Util;
import com.amalto.core.util.XtentisException;

/**
 * @author bgrieder
 * @ejb.bean name="RoutingEngineV2Ctrl" display-name="RoutingEngineV2Ctrl" description="Routing Engine"
 * jndi-name="amalto/remote/core/routingenginev2ctrl" local-jndi-name = "amalto/local/core/routingenginev2ctrl"
 * type="Stateless" view-type="both"
 * @ejb.remote-facade
 * @ejb.permission view-type = "remote" role-name = "administration"
 * @ejb.permission view-type = "local" unchecked = "true"
 */
public class RoutingEngineV2CtrlBean implements SessionBean, TimedObject, RoutingEngine {

    /**
     * Delay after which the routing rule is executed when the rule must be executed "now"
     */
    private final static long DELAY = 15;

    private final static String LOGGING_EVENT = "logging_event"; //$NON-NLS-1$

    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'm'SSS"); //$NON-NLS-1$

    private final static Logger LOGGER = Logger.getLogger(RoutingEngineV2CtrlBean.class);

    private final static SynchronizedNow synchronizedNow = new SynchronizedNow();

    private SessionContext context;

    /**
     * DocumentCtrlBean.java Constructor
     */
    public RoutingEngineV2CtrlBean() {
    }

    @Override
    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
        context = ctx;
    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
    }

    /**
     * Create method
     * 
     * @ejb.create-method view-type = "local"
     */
    public void ejbCreate() throws javax.ejb.CreateException {
    }

    /**
     * Post Create method
     */
    public void ejbPostCreate() throws javax.ejb.CreateException {
    }

    /**
     * Routes a document
     * 
     * @return the list of routing rules PKs that matched
     * @throws XtentisException
     * @ejb.interface-method view-type = "both"
     * @ejb.facade-method
     */
    @Override
    public RoutingRulePOJOPK[] route(ItemPOJOPK itemPOJOPK) throws XtentisException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("route() ROUTING Item " + itemPOJOPK.getUniqueID());
        }
        // check subscription engine state
        if (RoutingEngineV2POJO.getInstance().getStatus() == RoutingEngineV2POJO.STOPPED) {
            // the routing engine is stopped of in Error - throw an Exception
            String err = "Unable to route the Item '" + itemPOJOPK.getUniqueID() + "'. The Subscription Engine is stopped.";
            if (LOGGING_EVENT.equals(itemPOJOPK.getConceptName())) {
                LOGGER.info("ERROR SYSTRACE: " + err);
            } else {
                LOGGER.error(err);
            }
            throw new XtentisException(err);
        }
        // Retrieve the Routing Rule Controller
        RoutingRuleCtrlLocal routingRuleCtrl;
        try {
            routingRuleCtrl = Util.getRoutingRuleCtrlLocal();
        } catch (NamingException e) {
            String err = "Unable to route the Item '" + itemPOJOPK.getUniqueID() + "'. "
                    + "The routing rules controller cannot be found: " + e.getMessage();
            if (LOGGING_EVENT.equals(itemPOJOPK.getConceptName())) {
                LOGGER.info("ERROR SYSTRACE: " + err, e);
            } else {
                LOGGER.error(err, e);
            }
            throw new XtentisException(err, e);
        } catch (CreateException e) {
            String err = "Unable to route the Item '" + itemPOJOPK.getUniqueID() + "'. "
                    + "The routing rules controller cannot be created: " + e.getMessage();
            if (LOGGING_EVENT.equals(itemPOJOPK.getConceptName())) {
                LOGGER.info(err, e);
            } else {
                LOGGER.error(err, e);
            }
            throw new XtentisException(err, e);
        }
        // Retrieve the Routing Rule Controller
        RoutingOrderV2CtrlLocal routingOrderCtrl;
        try {
            routingOrderCtrl = Util.getRoutingOrderV2CtrlLocal();
        } catch (NamingException e) {
            String err = "Unable to route the Item '" + itemPOJOPK.getUniqueID() + "'. "
                    + "The Routing Orders controller cannot be found: " + e.getMessage();
            if (LOGGING_EVENT.equals(itemPOJOPK.getConceptName())) {
                LOGGER.info("ERROR SYSTRACE: " + err, e);
            } else {
                LOGGER.error(err, e);
            }
            throw new XtentisException(err, e);
        } catch (CreateException e) {
            String err = "Unable to route the Item '" + itemPOJOPK.getUniqueID() + "'. "
                    + "The Routing Orders controller cannot be created: " + e.getMessage();
            if (LOGGING_EVENT.equals(itemPOJOPK.getConceptName())) {
                LOGGER.info(err, e);
            } else {
                LOGGER.error(err, e);
            }
            throw new XtentisException(err, e);
        }
        // Retrieve the Item Controller
        ItemCtrl2Local itemCtrl;
        try {
            itemCtrl = Util.getItemCtrl2Local();
        } catch (NamingException e) {
            String err = "Unable to route the Item '" + itemPOJOPK.getUniqueID() + "'. "
                    + "The Items controller cannot be found: " + e.getMessage();
            if (LOGGING_EVENT.equals(itemPOJOPK.getConceptName())) {
                LOGGER.info("ERROR SYSTRACE: " + err, e);
            } else {
                LOGGER.error(err, e);
            }
            throw new XtentisException(err, e);
        } catch (CreateException e) {
            String err = "Unable to route the Item '" + itemPOJOPK.getUniqueID() + "'. "
                    + "The items controller cannot be created: " + e.getMessage();
            if (LOGGING_EVENT.equals(itemPOJOPK.getConceptName())) {
                LOGGER.info(err, e);
            } else {
                LOGGER.error(err, e);
            }
            throw new XtentisException(err, e);
        }
        // The cached ItemPOJO - will only be retrieved if needed: we have expressions on the routing rules
        ItemPOJO itemPOJO = null;
        // Rules that matched
        ArrayList<RoutingRulePOJO> routingRulesThatMatched = new ArrayList<RoutingRulePOJO>();
        ArrayList<RoutingRulePOJOPK> matchedRoutingRulesPks = new ArrayList<RoutingRulePOJOPK>();
        // loop over the known rules
        Collection<RoutingRulePOJOPK> routingRulePOJOPKs = routingRuleCtrl.getRoutingRulePKs("*"); //$NON-NLS-1$
        List<String> serviceJndiList = Util.getRuntimeServiceJndiList();
        for (RoutingRulePOJOPK routingRulePOJOPK : routingRulePOJOPKs) {
            RoutingRulePOJO routingRule = routingRuleCtrl.getRoutingRule(routingRulePOJOPK);
            if (routingRule.isDeActive()) {
                LOGGER.info(routingRule.getName() + " disabled, skip it!");
                continue;
            }
            // check integrity of Routing Rule
            if (!serviceJndiList.contains(routingRule.getServiceJNDI())) {
                LOGGER.info("Unable to lookup \"" + routingRule.getServiceJNDI() + "\", this service not bound!");
                continue;
            }
            // check if document type is OK
            if (!"*".equals(routingRule.getConcept())) { //$NON-NLS-1$
                String docType = itemPOJOPK.getConceptName();
                if (!docType.equals(routingRule.getConcept())) {
                    continue;
                }
            }
            // check if all routing rule expression matches - null: always matches
            // aiming modify see 4572 add condition to check
            if (routingRule.getCondition() == null || routingRule.getCondition().trim().length() == 0) {
                boolean matches = true;
                Collection<RoutingRuleExpressionPOJO> routingExpressions = routingRule.getRoutingExpressions();
                if (routingExpressions != null) {
                    for (RoutingRuleExpressionPOJO routingExpression : routingExpressions) {
                        // retrieve the itemPOJO if not already done
                        if (itemPOJO == null) {
                            itemPOJO = itemCtrl.getItem(itemPOJOPK);
                        }
                        // Match the rule
                        if (!ruleExpressionMatches(itemPOJO, routingExpression)) {
                            matches = false;
                            break;
                        }
                    }
                }
                if (!matches) {
                    continue;
                }
            } else {
                String condition = routingRule.getCondition();
                String compileCondition = compileCondition(condition);
                Collection<RoutingRuleExpressionPOJO> routingExpressions = routingRule.getRoutingExpressions();
                Interpreter ntp = new Interpreter();
                try {
                    for (RoutingRuleExpressionPOJO pojo : routingExpressions) {
                        if (pojo.getName() != null && pojo.getName().trim().length() > 0) {
                            Pattern p1 = Pattern.compile(pojo.getName(), Pattern.CASE_INSENSITIVE);
                            Matcher m1 = p1.matcher(condition);
                            while (m1.find()) {
                                if (itemPOJO == null) {
                                    itemPOJO = itemCtrl.getItem(itemPOJOPK);
                                }
                                ntp.set(m1.group(), ruleExpressionMatches(itemPOJO, pojo));
                            }
                        }
                    }
                    // compile
                    ntp.eval("truth = " + compileCondition + ";"); //$NON-NLS-1$ //$NON-NLS-2$
                    boolean truth = (Boolean) ntp.get("truth"); //$NON-NLS-1$
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(condition + " : " + truth);
                    }
                    if (truth) {
                        LOGGER.info("Trigger \"" + (routingRule.getName() == null ? "" : routingRule.getName()) + "\" matched! ");
                    }
                    if (!truth) {
                        continue;
                    }
                } catch (EvalError e) {
                    String err = "Condition compile error :" + e.getMessage();
                    LOGGER.error(err, e);
                    throw new XtentisException(err, e);
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("route() Routing Rule MATCH '" + routingRulePOJOPK.getUniqueId() + "' for item '"
                        + itemPOJOPK.getUniqueID() + "'");
            }
            // increment matching routing rules counter
            routingRulesThatMatched.add(routingRule);
            matchedRoutingRulesPks.add(routingRulePOJOPK);
        }
        Collections.sort(routingRulesThatMatched);
        for (RoutingRulePOJO routingRule : routingRulesThatMatched) {
            // create the routing Order
            Date now = new Date(synchronizedNow.getTime());
            String name = itemPOJOPK.getUniqueID() + "-" + sdf.format(now);
            // bind a universe to a Routing Order
            String bindingUniverseName = null;
            UniversePOJO universePOJO;
            if (LocalUser.getCurrentSubject() != null) {
                universePOJO = LocalUser.getLocalUser().getUniverse();
                if (universePOJO != null && universePOJO.getName() != null) {
                    bindingUniverseName = universePOJO.getName();
                }
            }
            // bind a bindingUserToken to a Routing Order
            String userToken = Util.getUsernameAndPasswordToken();
            String bindingUserToken = userToken;
            try {
                bindingUserToken = new String(Base64.encodeBase64(userToken.getBytes("UTF-8"))); //$NON-NLS-1$
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Unsupported encoding during trigger evaluation.", e);
            }
            ActiveRoutingOrderV2POJO routingOrderPOJO = new ActiveRoutingOrderV2POJO(name,
                    routingRule.isSynchronous() ? now.getTime() : now.getTime() + DELAY, itemPOJOPK, "Routing of '"
                            + itemPOJOPK.getUniqueID() + "' to service '"
                            + routingRule.getServiceJNDI().replaceFirst("amalto/local/service/", "") + "'"
                            + " activated by trigger '" + routingRule.getPK().getUniqueId() + "'", routingRule.getServiceJNDI(),
                    routingRule.getParameters(), bindingUniverseName, bindingUserToken);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Routing Order " + routingOrderPOJO.getName() + " bind universe "
                        + routingOrderPOJO.getBindingUniverseName());
            }
            // there is one case where everything is run now
            if (routingRule.isSynchronous()) {
                if (RoutingEngineV2POJO.getInstance().getStatus() == RoutingEngineV2POJO.RUNNING
                        || Boolean.valueOf(MDMConfiguration.getConfiguration().getProperty(
                                "routing.engine.rules.runsynconpause", "false"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    LOGGER.info("Trigger \"" + (routingRule.getName() == null ? "" : routingRule.getName()) + "\" activated! ");
                    routingOrderCtrl.executeSynchronously(routingOrderPOJO);
                    continue;
                }
            }
            // save the routing order for later routing
            routingOrderCtrl.putRoutingOrder(routingOrderPOJO);
            LOGGER.info("Trigger \"" + (routingRule.getName() == null ? "" : routingRule.getName()) + "\" activated! ");
            // make sure that the thread is started
            if (RoutingEngineV2POJO.getInstance().getStatus() == RoutingEngineV2POJO.RUNNING) {
                Collection<Timer> timers = context.getTimerService().getTimers();
                if ((timers == null) || (timers.size() < 1)) {
                    createTimer(new RoutingEngineV2POJOPK(RoutingEngineV2POJO.getInstance().getPK()), RoutingEngineV2POJO
                            .getInstance().getMinRunPeriodMillis());
                }
            }
        }
        if (routingRulesThatMatched.size() == 0) {
            String err = "Unable to find a routing rule for document " + itemPOJOPK.getUniqueID();
            LOGGER.info(err);
            return new RoutingRulePOJOPK[0];
        }
        return matchedRoutingRulesPks.toArray(new RoutingRulePOJOPK[matchedRoutingRulesPks.size()]);
    }

    private static String compileCondition(String condition) {
        String compiled = condition;
        compiled = compiled.replaceAll("(\\s+)([aA][nN][dD])(\\s+|\\(+)", "$1&&$3"); //$NON-NLS-1$ //$NON-NLS-2$
        compiled = compiled.replaceAll("(\\s+)([oO][rR])(\\s+|\\(+)", "$1||$3"); //$NON-NLS-1$//$NON-NLS-2$
        compiled = compiled.replaceAll("(\\s*)([nN][oO][tT])(\\s+|\\(+)", "$1!$3"); //$NON-NLS-1$ //$NON-NLS-2$
        return compiled;
    }

    /**
     * Check that a rule actually matches a document
     * 
     * @return true if it matches
     * @throws XtentisException
     */
    private boolean ruleExpressionMatches(ItemPOJO itemPOJO, RoutingRuleExpressionPOJO exp) throws XtentisException {
        int contentInt, expInt;
        String expXpath = exp.getXpath();
        if (!expXpath.startsWith("/")) { //$NON-NLS-1$
            expXpath = "/" + expXpath; //$NON-NLS-1$
        }
        try {
            String[] contents = Util.getTextNodes(itemPOJO.getProjection(), expXpath);
            if (contents.length == 0 && exp.getOperator() == RoutingRuleExpressionPOJO.IS_NULL) {
                return true;
            }
            boolean flag = false;
            for (String content : contents) {
                if (flag) {
                    break;
                }
                switch (exp.getOperator()) {
                case RoutingRuleExpressionPOJO.CONTAINS:
                    if (content != null && content.contains(exp.getValue())) {
                        flag = true;
                    }
                    break;
                case RoutingRuleExpressionPOJO.EQUALS:
                    if (content != null && content.equals(exp.getValue())) {
                        flag = true;
                    }
                    break;
                case RoutingRuleExpressionPOJO.GREATER_THAN:
                    try {
                        expInt = Integer.parseInt(exp.getValue());
                    } catch (Exception e) {
                        continue;
                    }
                    try {
                        contentInt = Integer.parseInt(content);
                    } catch (Exception e) {
                        continue;
                    }
                    if (content != null && contentInt > expInt) {
                        flag = true;
                    }
                    break;
                case RoutingRuleExpressionPOJO.GREATER_THAN_OR_EQUAL:
                    try {
                        expInt = Integer.parseInt(exp.getValue());
                    } catch (Exception e) {
                        continue;
                    }
                    try {
                        contentInt = Integer.parseInt(content);
                    } catch (Exception e) {
                        continue;
                    }
                    if (content != null && contentInt >= expInt) {
                        flag = true;
                    }
                    break;
                case RoutingRuleExpressionPOJO.IS_NOT_NULL:
                    flag = content != null;
                    break;
                case RoutingRuleExpressionPOJO.LOWER_THAN:
                    try {
                        expInt = Integer.parseInt(exp.getValue());
                    } catch (Exception e) {
                        continue;
                    }
                    try {
                        contentInt = Integer.parseInt(content);
                    } catch (Exception e) {
                        continue;
                    }
                    if (content != null && contentInt < expInt) {
                        flag = true;
                    }
                    break;
                case RoutingRuleExpressionPOJO.LOWER_THAN_OR_EQUAL:
                    try {
                        expInt = Integer.parseInt(exp.getValue());
                    } catch (Exception e) {
                        continue;
                    }
                    try {
                        contentInt = Integer.parseInt(content);
                    } catch (Exception e) {
                        continue;
                    }
                    if (content != null && contentInt <= expInt) {
                        flag = true;
                    }
                    break;
                case RoutingRuleExpressionPOJO.MATCHES:
                    if (content != null && content.matches(exp.getValue())) {
                        flag = true;
                    }
                    break;
                case RoutingRuleExpressionPOJO.NOT_EQUALS:
                    if (content != null && !content.equals(exp.getValue())) {
                        flag = true;
                    }
                    break;
                case RoutingRuleExpressionPOJO.STARTSWITH:
                    if (content != null && content.startsWith(exp.getValue())) {
                        flag = true;
                    }
                    break;
                }
            }
            return flag;
        } catch (TransformerException e) {
            String err = "Subscription rule expression match: unable extract xpath '" + exp.getXpath() + "' from Item '"
                    + itemPOJO.getItemPOJOPK().getUniqueID() + "': " + e.getLocalizedMessage();
            LOGGER.error(err, e);
            throw new XtentisException(err, e);
        }
    }

    /*****************************************************************
     * Engine LifeCycle
     *****************************************************************/
    /**
     * Starts/restarts the router
     * 
     * @throws XtentisException
     * @ejb.interface-method view-type = "both"
     * @ejb.facade-method
     */
    @Override
    public void start() throws XtentisException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("start()");
        }
        RoutingEngineV2POJO.getInstance().setStatus(RoutingEngineV2POJO.RUNNING);
        // make sure that the thread is started
        Collection<Timer> timers = context.getTimerService().getTimers();
        if ((timers == null) || timers.isEmpty()) {
            createTimer(new RoutingEngineV2POJOPK(RoutingEngineV2POJO.getInstance().getPK()), RoutingEngineV2POJO.getInstance()
                    .getMinRunPeriodMillis());
        }
    }

    /**
     * Stops the routing queue
     * 
     * @throws XtentisException
     * @ejb.interface-method view-type = "both"
     * @ejb.facade-method
     */
    @Override
    public void stop() throws XtentisException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("stop()");
        }
        RoutingEngineV2POJO.getInstance().setStatus(RoutingEngineV2POJO.STOPPED);
    }

    /**
     * Toggle suspend a routing queue
     * 
     * @throws XtentisException
     * @ejb.interface-method view-type = "both"
     * @ejb.facade-method
     */
    @Override
    public void suspend(boolean suspend) throws XtentisException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("suspend() " + suspend);
        }
        if (suspend) {
            RoutingEngineV2POJO.getInstance().setStatus(RoutingEngineV2POJO.SUSPENDED);
        } else {
            RoutingEngineV2POJO.getInstance().setStatus(RoutingEngineV2POJO.RUNNING);
            Collection<Timer> timers = context.getTimerService().getTimers();
            if ((timers == null) || timers.isEmpty()) {
                createTimer(new RoutingEngineV2POJOPK(RoutingEngineV2POJO.getInstance().getPK()), RoutingEngineV2POJO
                        .getInstance().getMinRunPeriodMillis());
            }
        }
    }

    /**
     * Toggle suspend a routing queue
     * 
     * @throws XtentisException
     * @ejb.interface-method view-type = "both"
     * @ejb.facade-method
     */
    @Override
    public int getStatus() throws XtentisException {
        return RoutingEngineV2POJO.getInstance().getStatus();
    }

    /*****************************************************************
     * T I M E R
     *****************************************************************/

    /**
     * @return a TimerHandle
     */
    private TimerHandle createTimer(RoutingEngineV2POJOPK routingEnginePOJOPK, long ms) {
        ms = Math.max(ms, RoutingEngineV2POJO.getInstance().getMinRunPeriodMillis());
        TimerService timerService = context.getTimerService();
        Timer timer = timerService.createTimer(ms, routingEnginePOJOPK);
        return timer.getHandle();
    }

    @Override
    public void ejbTimeout(Timer timer) {
        // for the moment we have a single static routing engine
        RoutingEngineV2POJO routingEngine = RoutingEngineV2POJO.getInstance();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("ejbTimeout() Running Engine on Thread id " + Thread.currentThread().getId());
        }
        // if routing engine is not running stop here
        if (routingEngine.getStatus() != RoutingEngineV2POJO.RUNNING) {
            return;
        }
        // capture start time to keep intervals regular as much as we can
        long startTime = System.currentTimeMillis();
        // extra time - if we want to increase the duration between two runs to provide throttling
        long extraTime = 0;
        // fetch routing Order Controller
        RoutingOrderV2CtrlLocal routingOrderCtrl;
        try {
            routingOrderCtrl = Util.getRoutingOrderV2CtrlLocal();
        } catch (NamingException e) {
            // mark the engine as stopped
            routingEngine.setStatus(RoutingEngineV2POJO.STOPPED);
            String err = "Unable to execute the scheduled routing orders. The Routing Orders controller cannot be found: "
                    + e.getMessage();
            LOGGER.error(err, e);
            return;
        } catch (CreateException e) {
            // mark the engine as stopped
            routingEngine.setStatus(RoutingEngineV2POJO.STOPPED);
            String err = "Unable to execute the scheduled routing orders. The Routing Orders cannot be created: "
                    + e.getMessage();
            LOGGER.error(err, e);
            return;
        }
        // garbage collect considered dead routing orders
        if (startTime - routingEngine.getLastDeadRoutingOrdersSweep() > routingEngine.getMaxExecutionTimeMillis() / 2) {
            // update the last run time
            routingEngine.setLastDeadRoutingOrdersSweep(startTime);
            // catch the max execution time
            long maxExecutionTime = routingEngine.getMaxExecutionTimeMillis();
            // search routing orders that must be run
            ActiveRoutingOrderV2POJO[] routingOrders = null;
            try {
                routingOrders = routingOrderCtrl.findDeadRoutingOrders(startTime - maxExecutionTime, 100);
            } catch (XtentisException e) {
                // mark the engine as stopped
                // DISABLED: we can go on anyway routingEngine.setStatus(RoutingEngineV2POJO.STOPPED);
                String err = "Unable to fetch dead routing orders: " + e.getMessage();
                LOGGER.info("ERROR SYSTRACE " + err, e);
            }
            // move the caught ones to error
            if (routingOrders != null) {
                for (ActiveRoutingOrderV2POJO routingOrder : routingOrders) {
                    String message = "Routing Order " + routingOrder.getAbstractRoutingOrderPOJOPK().getUniqueId()
                            + " has exceeded the max execution time of " + maxExecutionTime + " milliseconds.";
                    // Log
                    LOGGER.error(message);
                    // move to FAILED
                    try {
                        // Create Failed Routing Order
                        FailedRoutingOrderV2POJO failedRO = new FailedRoutingOrderV2POJO(routingOrder);
                        // Remove Current active one
                        routingOrderCtrl.removeRoutingOrder(routingOrder.getAbstractRoutingOrderPOJOPK());
                        // save failed Routing Order
                        failedRO.setMessage(failedRO.getMessage() + "\n---> FAILED " + sdf.format(new Date()) + ": " + message);
                        routingOrderCtrl.putRoutingOrder(failedRO);
                    } catch (XtentisException e) {
                        String err = "Unable to move dead routing order '"
                                + routingOrder.getAbstractRoutingOrderPOJOPK().getUniqueId() + "' to failed queue: "
                                + e.getMessage();
                        LOGGER.info("ERROR SYSTRACE " + err, e);
                    }
                }
            }
        }
        // get the number of simultaneous executors
        int numberOfExecutors = routingEngine.getMaxNumberOfExecutors();
        // garbage Collect executors
        HashMap<String, RoutingEngineV2ExecutorPOJO> previousRunExecutors = new HashMap<String, RoutingEngineV2ExecutorPOJO>(
                routingEngine.getExecutors());
        HashMap<String, RoutingEngineV2ExecutorPOJO> thisRunExecutors = new HashMap<String, RoutingEngineV2ExecutorPOJO>();
        ArrayList<AbstractRoutingOrderV2POJOPK> slotedRoutingOrderIDs = new ArrayList<AbstractRoutingOrderV2POJOPK>();
        Set<String> tokens = previousRunExecutors.keySet();
        for (String token : tokens) {
            RoutingEngineV2ExecutorPOJO routingEngineV2ExecutorPOJO = previousRunExecutors.get(token);
            // check if routing order still active
            try {
                AbstractRoutingOrderV2POJOPK routingOrderPK = routingEngineV2ExecutorPOJO.getExecutingRoutingOrderPK();
                if (routingOrderCtrl.existsRoutingOrder(routingOrderPK) != null) {
                    thisRunExecutors.put(token, routingEngineV2ExecutorPOJO);
                    // Routing order already in slot
                    slotedRoutingOrderIDs.add(routingOrderPK);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("ejbTimeout() Executor of Routing Order " + routingOrderPK.getUniqueId() + " still running");
                    }
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("ejbTimeout() Executor of Routing Order " + routingOrderPK.getUniqueId()
                                + " stopped running");
                    }
                }
            } catch (XtentisException e) {
                // log a systrace
                String err = "Unable to check the existence of the Active Routing Order '"
                        + routingEngineV2ExecutorPOJO.getExecutingRoutingOrderPK().getUniqueId() + "'"
                        + ". The executor cannot be reclaimed.";
                LOGGER.error("ERROR SYSTRACE " + err, e);
                return;
            }
        }
        // update the Routing Engine with the garbage collected executors
        routingEngine.setExecutors(thisRunExecutors);
        // determine the number of available tokens
        // fill in an array will all the tokens (their number)
        ArrayList<String> availableTokens = new ArrayList<String>();
        for (int i = 0; i < numberOfExecutors; i++) {
            availableTokens.add(String.valueOf(i));
        }
        // remove existing executors
        tokens = thisRunExecutors.keySet();
        for (String token : tokens) {
            availableTokens.remove(token);
        }
        // stop here if no available slots
        if (availableTokens.size() == 0) {
            LOGGER.info("Subscription Engine Executors: all " + numberOfExecutors + " executors already busy");
            // catch time it took
            long duration = System.currentTimeMillis() - startTime;
            // restart process at FREQUENCY
            createTimer(new RoutingEngineV2POJOPK(routingEngine.getPK()), routingEngine.getRunPeriodMillis() - duration);
            return;
        }
        // search routing orders that must be run
        ActiveRoutingOrderV2POJO[] routingOrders = null;
        try {
            routingOrders = routingOrderCtrl.findActiveRoutingOrders(System.currentTimeMillis(), availableTokens.size());
        } catch (XtentisException e) {
            // mark the engine as stopped
            // Likely the database is overloaded - so just wait a bit
            String err = "Unable to fetch active routing orders: " + e.getMessage()
                    + ". Routing Engine will be pause for a few seconds.";
            LOGGER.info("ERROR SYSTRACE " + err, e);
            extraTime = 5000;
        }
        // if nothing to process - return
        if ((routingOrders == null) || (routingOrders.length == 0)) {
            // catch time it took
            long duration = System.currentTimeMillis() - startTime + extraTime;
            // restart process at FREQUENCY
            createTimer(new RoutingEngineV2POJOPK(routingEngine.getPK()), routingEngine.getRunPeriodMillis() - duration);
            return;
        }
        List<String> serviceJndiList = Util.getRuntimeServiceJndiList();
        // slot new routing orders and execute them
        int tokenIndex = 0;
        for (int i = 0; i < routingOrders.length && tokenIndex < availableTokens.size(); i++) {
            ActiveRoutingOrderV2POJO routingOrder = routingOrders[i];
            // make sure service JNDI is exist
            if (!serviceJndiList.contains(routingOrder.getServiceJNDI())) {
                LOGGER.info("The service jndi \"" + routingOrder.getServiceJNDI()
                        + "\"is not exist, please delete related Routing Order! ");
                continue;
            }
            // make sure it is not already in slot (though not yet started)
            if (slotedRoutingOrderIDs.contains(new ActiveRoutingOrderV2POJOPK(routingOrder.getPK()))) {
                continue;
            }
            // Not already in slot --> we will slot it and execute it
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ejbTimeout() Scheduling Routing Order " + routingOrder.getName() + " on thread ID "
                        + Thread.currentThread().getId() + " - " + (availableTokens.size() - i - 1) + " tokens left");
            }
            // slot
            String token = availableTokens.get(tokenIndex++);
            RoutingEngineV2ExecutorPOJO executor = new RoutingEngineV2ExecutorPOJO(token, new RoutingEngineV2POJOPK(
                    routingEngine.getPK()), routingOrder.getAbstractRoutingOrderPOJOPK());
            thisRunExecutors.put(token, executor);
            slotedRoutingOrderIDs.add(new ActiveRoutingOrderV2POJOPK(routingOrder.getPK()));
            // update routing Order
            routingOrder.setRoutingEnginePOJOPK(new RoutingEngineV2POJOPK(routingEngine.getPK()));
            routingOrder.setRoutingEngineToken(token);
            // execute
            try {
                routingOrderCtrl.executeAsynchronously(routingOrder);
            } catch (XtentisException e) {
                // mark the engine as stopped
                routingEngine.setStatus(RoutingEngineV2POJO.STOPPED);
                // log a systrace
                String err = "Unable to execute the routing order '" + routingOrder.getPK().getUniqueId() + "': "
                        + e.getMessage() + "The Engine will be paused for a few additional seconds";
                LOGGER.error("ERROR SYSTRACE " + err, e);
                extraTime = 5000;
                break;
            }
        }
        // update the Routing Engine with the scheduled or currently executing Executors -- BG I guess this should not
        // be here.....
        routingEngine.setExecutors(thisRunExecutors);
        // catch time it took
        long duration = System.currentTimeMillis() - startTime + extraTime;
        // restart process at FREQUENCY
        createTimer(new RoutingEngineV2POJOPK(routingEngine.getPK()), routingEngine.getRunPeriodMillis() - duration);
    }
}