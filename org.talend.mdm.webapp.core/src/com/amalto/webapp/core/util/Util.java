// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package com.amalto.webapp.core.util;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.CreateException;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.amalto.core.ejb.UpdateReportPOJO;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.security.SimpleGroup;
import org.talend.mdm.commmon.util.core.EDBType;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.talend.mdm.commmon.util.datamodel.management.BusinessConcept;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import com.amalto.commons.core.utils.XMLUtils;
import com.amalto.core.delegator.ILocalUser;
import com.amalto.core.ejb.ItemPOJO;
import com.amalto.core.ejb.local.XmlServerSLWrapperLocal;
import com.amalto.core.objects.transformers.v2.ejb.TransformerV2POJOPK;
import com.amalto.core.objects.view.ejb.ViewPOJO;
import com.amalto.core.util.LocalUser;
import com.amalto.core.util.XtentisException;
import com.amalto.core.webservice.*;
import com.amalto.webapp.core.bean.Configuration;
import com.amalto.webapp.core.dmagent.SchemaWebAgent;
import com.amalto.webapp.core.json.JSONArray;
import com.amalto.webapp.core.json.JSONObject;
import com.sun.org.apache.xpath.internal.XPathAPI;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSElementDecl;

public class Util {

    private static String port = null;

    static {
        port = MDMConfiguration.getHttpPort();
    }

    private static String endpoint_address = "http://localhost:" + port + "/talend/TalendPort"; //$NON-NLS-1$ //$NON-NLS-2$

    private static Pattern TOTAL_COUNT_PATTERN = Pattern.compile("<totalCount>(.*)</totalCount>"); //$NON-NLS-1$

    public static int _AUTO_ = 0;

    public static int _FORCE_RMI_ = 1;

    public static int _FORCE_WEB_SERVICE_ = 2;

    public static String DATE_FORMAT_PREFIX = "%t"; //$NON-NLS-1$  

    private static final String PROVISIONING_CONCEPT = "User"; //$NON-NLS-1$

    private static final String DATACLUSTER_PK = "PROVISIONING"; //$NON-NLS-1$

    public static final String DEFAULT_LANGUAGE = "en"; //$NON-NLS-1$

    private static final Logger LOGGER = Logger.getLogger(Util.class);

    /*********************************************************************
     * WEB SERVICES
     *********************************************************************/

    public static XtentisPort getPort() throws XtentisWebappException {
        AjaxSubject as;
        try {
            as = Util.getAjaxSubject();
        } catch (Exception e) {
            LOGGER.error("Unable to access the logged user data", e); //$NON-NLS-1$
            throw new XtentisWebappException("Unable to access the logged user data"); //$NON-NLS-1$
        }
        if (as == null) {
            throw new XtentisWebappException("Session Expired"); //$NON-NLS-1$
        }
        String[] mdm = as.getMDMData();
        String url = "http://" + mdm[0] + "/talend/TalendPort"; //$NON-NLS-1$ //$NON-NLS-2$
        return Util.getPort(url, mdm[1], mdm[2]);
    }

    public static XtentisPort getPort(String username, String password) throws XtentisWebappException {
        return getPort(endpoint_address, username, password, _AUTO_);
    }

    public static XtentisPort getPort(String endpointAddress, String username, String password) throws XtentisWebappException {
        return getPort(endpointAddress, username, password, _AUTO_);
    }

    public static XtentisPort getPort(String endpointAddress, String username, String password, int force)
            throws XtentisWebappException {
        XtentisPort xtentisPort;
        if (force == _FORCE_RMI_) {
            xtentisPort = getRMIEndPoint();
        } else if (force == _FORCE_WEB_SERVICE_) {
            throw new org.apache.commons.lang.NotImplementedException();
        } else if (endpointAddress.contains("localhost")) { //$NON-NLS-1$
            xtentisPort = getRMIEndPoint();
        } else {
            xtentisPort = getRMIEndPoint();
        }
        return XtentisWebPort.wrap(xtentisPort);
    }

    private static XtentisPort getRMIEndPoint() throws XtentisWebappException {
        try {
            return (XtentisPort) Class.forName("com.amalto.core.ejb.local.XtentisWSBean").newInstance(); //$NON-NLS-1$
        } catch (Exception e) {
            throw new XtentisWebappException(e);
        }
    }

    public static String getXML(Class<?> c, String filename) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(c.getResourceAsStream(filename)));
        String xml = ""; //$NON-NLS-1$
        String line;
        while ((line = in.readLine()) != null) {
            xml += line + "\n"; //$NON-NLS-1$
        }
        return xml;
    }

    /**
     * Join an array of strings into a single string using a separator
     *
     * @return a single string or null
     */
    public static String joinStrings(String[] strings, String separator) {
        if (strings == null) {
            return null;
        }
        String res = ""; //$NON-NLS-1$
        for (int i = 0; i < strings.length; i++) {
            res += (i > 0) ? separator : ""; //$NON-NLS-1$
            res += strings[i];
        }
        return res;
    }

    /**
     * Returns the first part - eg. the concept - from the path
     *
     * @return the Concept Name
     */
    public static String getConceptFromPath(String path) {
        if (path == null || path.trim().length() == 0) {
            return null;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Pattern p = Pattern.compile("(.*?)[\\[|/].*"); //$NON-NLS-1$
        if (!path.endsWith("/")) {
            path += "/"; //$NON-NLS-1$
        }
        Matcher m = p.matcher(path);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    public static String getForeignPathFromPath(String path) {
        int pos = path.indexOf("["); //$NON-NLS-1$
        if (pos != -1) {
            return path.substring(0, pos);
        }
        return path;

    }

    public static WSWhereCondition getConditionFromPath(String path) {
        Pattern p = Pattern.compile("(.*?)\\[(.*?)(&=|!=|>=|<=|>|<|=)(.*?)\\].*"); //$NON-NLS-1$
        if (!path.endsWith("/")) { //$NON-NLS-1$
            path += "/"; //$NON-NLS-1$
        }
        Matcher m = p.matcher(path);
        if (m.matches()) {
            WSWhereCondition wc = new WSWhereCondition();
            wc.setLeftPath(m.group(2).trim());
            com.amalto.core.webservice.WSWhereOperator operator = changeToWSOperator(m.group(3));
            wc.setOperator(operator);
            wc.setRightValueOrPath(m.group(4).trim().replaceAll("'|\"", "")); //$NON-NLS-1$ //$NON-NLS-2$
            wc.setSpellCheck(true);
            wc.setStringPredicate(WSStringPredicate.NONE);
            return wc;
        }
        return null;
    }

    public static WSWhereItem getConditionFromFKFilter(String foreignKey, String foreignKeyInfo, String fkFilter) {
        return getConditionFromFKFilter(foreignKey, foreignKeyInfo, fkFilter, true);
    }

    public static WSWhereItem getConditionFromFKFilter(String foreignKey, String foreignKeyInfo, String fkFilter,
            boolean formatFkValue) {
        if (fkFilter == null || fkFilter.length() == 0) {
            return null;
        }
        if (fkFilter.equals("null")) {
            return null;
        }
        int additionalInfo = fkFilter.indexOf("-", fkFilter.lastIndexOf("#") > -1 ? fkFilter.lastIndexOf("#") + 1 : 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        String additionalValue = null;
        if (additionalInfo != -1) {
            additionalValue = fkFilter.substring(additionalInfo + 1);
            fkFilter = fkFilter.substring(0, additionalInfo);
        }
        String[] criterias = fkFilter.split("#"); //$NON-NLS-1$
        ArrayList<WSWhereItem> condition = new ArrayList<WSWhereItem>();
        for (String cria : criterias) {
            String[] values = cria.split("\\$\\$"); //$NON-NLS-1$
            WSWhereCondition wc = Util.convertLine(values);
            if (wc != null) {
                if (formatFkValue) {
                    if (isFkPath(values[0])) {
                        wc.setRightValueOrPath(wrapFkValue(wc.getRightValueOrPath()));
                    } else {
                        wc.setRightValueOrPath(unwrapFkValue(wc.getRightValueOrPath()));
                    }
                }
                condition.add(new WSWhereItem(wc, null, null));
            }
        }
        if (additionalInfo != -1) {
            String[] keyInfos = (foreignKeyInfo != null && !foreignKeyInfo.trim().isEmpty()) ? foreignKeyInfo.split(",") //$NON-NLS-1$
                    : foreignKey.split(","); //$NON-NLS-1$
            for (String keyInfo : keyInfos) {
                String[] values = new String[] { keyInfo, "Contains", additionalValue.equals(".*") ? "." : additionalValue }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                WSWhereCondition wc = Util.convertLine(values);
                if (wc != null) {
                    condition.add(new WSWhereItem(wc, null, null));
                }
            }
        }
        if (condition.size() > 0) {
            WSWhereItem whereItem;
            if (MDMConfiguration.isSqlDataBase()) {
                whereItem = makeWhereItem(condition);
            } else {
                WSWhereAnd and = new WSWhereAnd(condition.toArray(new WSWhereItem[condition.size()]));
                whereItem = new WSWhereItem(null, and, null);
            }
            return whereItem;
        } else {
            return null;
        }
    }

    public static WSWhereItem makeWhereItem(List<WSWhereItem> conditions) {
        List<Object> conds = new ArrayList<Object>();
        for (int i = 0; i < conditions.size(); i++) {
            WSWhereItem item = conditions.get(i);
            conds.add(item);
            if (i < conditions.size() - 1) {
                String predicate = item.getWhereCondition().getStringPredicate().getValue();
                if (WSStringPredicate.NOT.getValue().equals(predicate)) {
                    predicate = WSStringPredicate.AND.getValue();
                } else if (WSStringPredicate.EXACTLY.getValue().equals(predicate)) {
                    predicate = WSStringPredicate.AND.getValue();
                } else if (WSStringPredicate.STRICTAND.getValue().equals(predicate)) {
                    predicate = WSStringPredicate.AND.getValue();
                } else if (WSStringPredicate.NONE.getValue().equals(predicate)) {
                    predicate = WSStringPredicate.AND.getValue();
                }
                conds.add(predicate);
            }
        }
        Stack<String> stackOp = new Stack<String>();
        List<Object> rpn = new ArrayList<Object>();
        for (Object item : conds) {
            if (item instanceof WSWhereItem) {
                rpn.add(item);
            } else {
                String predicate = (String) item;
                while (!stackOp.isEmpty()) {
                    rpn.add(stackOp.pop());
                }
                stackOp.push(predicate);
            }
        }
        while (!stackOp.isEmpty()) {
            rpn.add(stackOp.pop());
        }
        Stack<WSWhereItem> whereStack = new Stack<WSWhereItem>();
        for (Object o : rpn) {
            if (o instanceof WSWhereItem) {
                whereStack.push((WSWhereItem) o);
            } else if (o instanceof String) {
                if (WSStringPredicate.OR.getValue().equals(o)) {
                    WSWhereItem item1 = whereStack.pop();
                    WSWhereItem item2 = whereStack.pop();
                    WSWhereOr or = new WSWhereOr(new WSWhereItem[] { item2, item1 });
                    whereStack.push(new WSWhereItem(null, null, or));
                } else if (WSStringPredicate.AND.getValue().equals(o)) {
                    WSWhereItem item1 = whereStack.pop();
                    WSWhereItem item2 = whereStack.pop();
                    WSWhereAnd and = new WSWhereAnd(new WSWhereItem[] { item2, item1 });
                    whereStack.push(new WSWhereItem(null, and, null));
                }
            }
        }
        return whereStack.pop();
    }

    public static WSWhereCondition convertLine(String[] values) {
        if (values.length < 3) {
            return null;
        }
        WSWhereCondition wc = new WSWhereCondition();
        wc.setLeftPath(values[0]);
        if (values.length >= 3) {
            WSWhereOperator operator = null;
            if (values[1].equals("Contains")) {
                operator = WSWhereOperator.CONTAINS;
            } else if (values[1].equals("Contains Text Of")) {
                operator = WSWhereOperator.JOIN;
            } else if (values[1].equals("=")) {
                operator = WSWhereOperator.EQUALS;
            } else if (values[1].equals(">")) {
                operator = WSWhereOperator.GREATER_THAN;
            } else if (values[1].equals(">=")) {
                operator = WSWhereOperator.GREATER_THAN_OR_EQUAL;
            } else if (values[1].equals("<")) {
                operator = WSWhereOperator.LOWER_THAN;
            } else if (values[1].equals("<=")) {
                operator = WSWhereOperator.LOWER_THAN_OR_EQUAL;
            } else if (values[1].equals("!=")) {
                operator = WSWhereOperator.NOT_EQUALS;
            } else if (values[1].equals("Starts With")) {
                operator = WSWhereOperator.STARTSWITH;
            } else if (values[1].equals("Strict Contains")) {
                operator = WSWhereOperator.STRICTCONTAINS;
            }
            wc.setOperator(operator);
            if (values[2] != null && values[2].matches("^\".*\"$")) {
                values[2] = values[2].substring(1, values[2].length() - 1);
            }
            wc.setRightValueOrPath(values[2]);
        }
        if (values.length >= 4) {
            WSStringPredicate predicate = null;
            if (values[3].equals("")) {
                predicate = WSStringPredicate.NONE;
            } else if (values[3].equals("Or")) {
                predicate = WSStringPredicate.OR;
            }
            if (values[3].equals("And")) {
                predicate = WSStringPredicate.AND;
            }
            if (values[3].equals("Strict And")) {
                predicate = WSStringPredicate.STRICTAND;
            }
            if (values[3].equals("Exactly")) {
                predicate = WSStringPredicate.EXACTLY;
            }
            if (values[3].equals("Not")) {
                predicate = WSStringPredicate.NOT;
            }
            wc.setStringPredicate(predicate);
        } else {
            wc.setStringPredicate(WSStringPredicate.NONE);
        }
        return wc;
    }

    public static String wrapFkValue(String value) {
        if (value.startsWith("[") && value.endsWith("]")) { //$NON-NLS-1$//$NON-NLS-2$
            return value;
        }
        return "[" + value + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String unwrapFkValue(String value) {
        if (value.startsWith("[") && value.endsWith("]")) { //$NON-NLS-1$ //$NON-NLS-2$
            if (value.contains("][")) { //$NON-NLS-1$
                return value;
            } else {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    public static boolean isFkPath(String fkPath) {
        String concept = fkPath.split("/")[0]; //$NON-NLS-1$
        try {
            BusinessConcept businessConcept = SchemaWebAgent.getInstance().getBusinessConcept(concept);
            businessConcept.load();
            Map<String, String> fkMap = businessConcept.getForeignKeyMap();
            if (fkMap != null && fkMap.containsKey("/" + fkPath)) { //$NON-NLS-1$
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static com.amalto.core.webservice.WSWhereOperator changeToWSOperator(String operator) {
        if ("=".equals(operator)) {
            return com.amalto.core.webservice.WSWhereOperator.EQUALS;
        }
        if ("!=".equals(operator)) {
            return com.amalto.core.webservice.WSWhereOperator.NOT_EQUALS;
        }
        if ("<".equals(operator)) {
            return com.amalto.core.webservice.WSWhereOperator.LOWER_THAN;
        }
        if ("<=".equals(operator)) {
            return com.amalto.core.webservice.WSWhereOperator.LOWER_THAN_OR_EQUAL;
        }
        if (">".equals(operator)) {
            return com.amalto.core.webservice.WSWhereOperator.GREATER_THAN;
        }
        if (">=".equals(operator)) {
            return com.amalto.core.webservice.WSWhereOperator.GREATER_THAN_OR_EQUAL;
        }
        if ("&=".equals(operator)) {
            return com.amalto.core.webservice.WSWhereOperator.CONTAINS;
        }
        return null;
    }

    /**
     * Generates an xml string from a node (not pretty formatted)
     * 
     * @param n the node
     * @return the xml string
     * @throws Exception
     */
    public static String nodeToString(Node n) throws Exception {
        StringWriter sw = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("omit-xml-declaration", "yes"); //$NON-NLS-1$ //$NON-NLS-2$
        transformer.setOutputProperty("indent", "yes"); //$NON-NLS-1$ //$NON-NLS-2$
        transformer.transform(new DOMSource(n), new StreamResult(sw));
        return sw.toString();
    }

    /**
     * Get a nodelist from an xPath
     * 
     * @throws Exception
     */
    public static NodeList getNodeList(Document d, String xPath) throws Exception {
        return getNodeList(d.getDocumentElement(), xPath, null, null);
    }

    /**
     * Get a nodelist from an xPath
     * 
     * @throws Exception
     */
    public static NodeList getNodeList(Node contextNode, String xPath) throws Exception {
        return getNodeList(contextNode, xPath, null, null);
    }

    /**
     * Get a nodelist from an xPath
     * 
     * @throws Exception
     */
    public static NodeList getNodeList(Node contextNode, String xPath, String namespace, String prefix) throws Exception {
        XObject xo = XPathAPI.eval(contextNode, xPath, (namespace == null) ? contextNode : Util.getRootElement("nsholder", //$NON-NLS-1$
                namespace, prefix));
        if (xo.getType() != XObject.CLASS_NODESET) {
            return null;
        }
        return xo.nodelist();
    }

    /**
     * Returns a namespaced root element of a document Useful to create a namespace holder element
     * 
     * @return the root Element
     */
    public static Element getRootElement(String elementName, String namespace, String prefix) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            Document namespaceHolder = impl.createDocument(namespace, (prefix == null ? "" : prefix + ":") + elementName, null); //$NON-NLS-1$ //$NON-NLS-2$
            Element rootNS = namespaceHolder.getDocumentElement();
            rootNS.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + prefix, namespace); //$NON-NLS-1$ //$NON-NLS-2$
            return rootNS;
        } catch (Exception e) {
            String err = "Error creating a namespace holder document: " + e.getLocalizedMessage(); //$NON-NLS-1$
            throw new Exception(err);
        }
    }

    public static Document parse(String xmlString) throws Exception {
        return parse(xmlString, null);
    }

    public static Document parse(String xmlString, String schema) throws Exception {
        // parse
        Document d;
        SAXErrorHandler seh = new SAXErrorHandler();
        try {
            // initialize the sax parser which uses Xerces
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl"); //$NON-NLS-1$ //$NON-NLS-2$
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Schema validation based on schemaURL
            factory.setNamespaceAware(true);
            factory.setValidating((schema != null));
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$ //$NON-NLS-2$
            if (schema != null) {
                factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", new InputSource(new StringReader( //$NON-NLS-1$
                        schema)));
            }
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(seh);
            d = builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            String err = "Unable to parse the document" + ": " + e.getClass().getName() + ": " + e.getLocalizedMessage() + "\n " //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    + xmlString;
            throw new Exception(err);
        }
        // check if document parsed correctly against the schema
        if (schema != null) {
            String errors = seh.getErrors();
            if (!errors.equals("")) { //$NON-NLS-1$
                String err = "Document  did not parse against schema: \n" + errors + "\n" + xmlString; //$NON-NLS-1$//$NON-NLS-2$
                throw new Exception(err);
            }
        }
        return d;
    }

    public static String[] getTextNodes(Node contextNode, String xPath) throws XtentisWebappException {
        return getTextNodes(contextNode, xPath, contextNode);
    }

    public static String[] getTextNodes(Node contextNode, String xPath, Node namespaceNode) throws XtentisWebappException {
        String[] results;
        // test for hard-coded values
        if (xPath.startsWith("\"") && xPath.endsWith("\"")) {
            return new String[] { xPath.substring(1, xPath.length() - 1) };
        }

        // test for incomplete path (elements missing /text())
        if (!xPath.matches(".*@[^/\\]]+")) {
            if (!xPath.endsWith(")")) {
                xPath += "/text()"; //$NON-NLS-1$
            }
        }
        try {
            XObject xo = XPathAPI.eval(contextNode, xPath, namespaceNode);
            if (xo.getType() == XObject.CLASS_NODESET) {
                NodeList l = xo.nodelist();
                int len = l.getLength();
                results = new String[len];
                for (int i = 0; i < len; i++) {
                    Node n = l.item(i);
                    results[i] = n.getNodeValue();
                }
            } else {
                results = new String[] { xo.toString() };
            }
        } catch (Exception e) {
            String err = "Unable to get the text node(s) of " + xPath + ": " + e.getClass().getName() + ": " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + e.getLocalizedMessage();
            throw new XtentisWebappException(err);
        }
        return results;
    }

    public static String getFirstTextNode(Node contextNode, String xPath, Node namespaceNode) throws XtentisWebappException {
        String[] res = getTextNodes(contextNode, xPath, namespaceNode);
        if (res.length == 0) {
            return null;
        }
        return res[0];
    }

    /**
     * Get the first text node matching the Xpath
     * 
     * @return the String or null if not found
     * @throws XtentisWebappException
     */
    public static String getFirstTextNode(Node contextNode, String xPath) throws XtentisWebappException {
        return getFirstTextNode(contextNode, xPath, contextNode);
    }

    /*********************************************************************
     * JACC - JAAS
     *********************************************************************/
    public static AjaxSubject getAjaxSubject() throws PolicyContextException {
        // Retrieve the subject
        String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container"; //$NON-NLS-1$
        Subject sub = (Subject) PolicyContext.getContext(SUBJECT_CONTEXT_KEY);

        return new AjaxSubject(sub);

    }

    public static String getPrincipalMember(String key) throws Exception {
        String result = ""; //$NON-NLS-1$
        // Get the Authenticated Subject
        Subject subject = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container"); //$NON-NLS-1$
        // Now look for a Group
        Set<Principal> principals = subject.getPrincipals(Principal.class);
        for (Principal p : principals) {
            if (p instanceof SimpleGroup) {
                SimpleGroup sg = (SimpleGroup) p;
                if (key.equals(sg.getName())) {
                    Enumeration<?> en = sg.members();
                    while (en.hasMoreElements()) {
                        String info = en.nextElement().toString();
                        result = result + "," + info; //$NON-NLS-1$
                    }
                }
            }
        }
        if (result.length() > 0) {
            result = result.substring(1);
        }
        return result;
    }

    public static String getLoginUserName() throws Exception {
        return getPrincipalMember("Username"); //$NON-NLS-1$
    }

    public static String getLoginUniverse() throws Exception {
        return getPrincipalMember("Universe"); //$NON-NLS-1$
    }

    public static String getLoginRoles() throws Exception {
        return getPrincipalMember("Roles"); //$NON-NLS-1$
    }

    public static Element getLoginProvisioningFromDB() throws Exception {
        WSItem item = Util.getPort().getItem(
                new WSGetItem(new WSItemPK(new WSDataClusterPK("PROVISIONING"), "User", new String[] { Util //$NON-NLS-1$ //$NON-NLS-2$
                        .getLoginUserName() })));
        String userString = item.getContent();
        return (Element) Util.getNodeList(Util.parse(userString), "//User").item(0);
    }

    /**
     * Computes an md5 hash of a string.
     * 
     * @param text the hashed string
     * @return the string hash
     * @exception NullPointerException if text is null
     */
    public static byte[] md5(String text, String charset) {
        // arguments check
        if (text == null) {
            throw new NullPointerException("null text"); //$NON-NLS-1$
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
            md.update(text.getBytes(charset));
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot find MD5 algorithm"); //$NON-NLS-1$
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No such encoding: " + charset); //$NON-NLS-1$
        }
    }

    /**
     * Computes an md5 hash and returns the result as a string in hexadecimal format.
     * 
     * @param text the hashed string
     * @return the string hash
     * @exception NullPointerException if text is null
     */
    public static String md5AsHexString(String text, String charset) {
        return toHexString(md5(text, charset));
    }

    /**
     * Returns a string in the hexadecimal format.
     * 
     * @param bytes the converted bytes
     * @return the hexadecimal string representing the bytes data
     * @throws IllegalArgumentException if the byte array is null
     */
    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("byte array must not be null"); //$NON-NLS-1$
        }
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            hex.append(Character.forDigit((aByte & 0XF0) >> 4, 16));
            hex.append(Character.forDigit((aByte & 0X0F), 16));
        }
        return hex.toString();
    }

    /**
     * store the info of datacluster and datamodel to PROVISIONING.
     */
    public static void storeProvisioning(String username, String xmlString) throws Exception {
        XmlServerSLWrapperLocal server = com.amalto.core.util.Util.getXmlServerCtrlLocal();
        server.start("PROVISIONING"); //$NON-NLS-1$
        server.putDocumentFromString(xmlString, "PROVISIONING" + "." + "User" + "." + username, "PROVISIONING", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        server.commit("PROVISIONING"); //$NON-NLS-1$
        ItemPOJO.clearCache();
    }

    /**
     * @return gives the operator associated to the string 'option'
     */
    public static WSWhereOperator getOperator(String option) {
        WSWhereOperator res = null;
        if (option.equalsIgnoreCase("CONTAINS")) {
            res = WSWhereOperator.CONTAINS;
        } else if (option.equalsIgnoreCase("EQUALS")) {
            res = WSWhereOperator.EQUALS;
        } else if (option.equalsIgnoreCase("GREATER_THAN")) {
            res = WSWhereOperator.GREATER_THAN;
        } else if (option.equalsIgnoreCase("GREATER_THAN_OR_EQUAL")) {
            res = WSWhereOperator.GREATER_THAN_OR_EQUAL;
        } else if (option.equalsIgnoreCase("JOIN")) {
            res = WSWhereOperator.JOIN;
        } else if (option.equalsIgnoreCase("LOWER_THAN")) {
            res = WSWhereOperator.LOWER_THAN;
        } else if (option.equalsIgnoreCase("LOWER_THAN_OR_EQUAL")) {
            res = WSWhereOperator.LOWER_THAN_OR_EQUAL;
        } else if (option.equalsIgnoreCase("NOT_EQUALS")) {
            res = WSWhereOperator.NOT_EQUALS;
        } else if (option.equalsIgnoreCase("STARTSWITH")) {
            res = WSWhereOperator.STARTSWITH;
        } else if (option.equalsIgnoreCase("STRICTCONTAINS")) {
            res = WSWhereOperator.STRICTCONTAINS;
        } else if (option.equalsIgnoreCase("FULLTEXTSEARCH")) {
            res = WSWhereOperator.FULLTEXTSEARCH;
        } else if (option.equalsIgnoreCase("EMPTY_NULL")) {
            res = WSWhereOperator.EMPTY_NULL;
        }
        return res;
    }

    public static List<String> getElementValues(Node n) throws Exception {
        List<String> l = new ArrayList<String>();
        NodeList list = n.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nValue = com.amalto.core.util.Util.getFirstTextNode(node, "."); //$NON-NLS-1$
                if (!hasChildren(node)) {
                    l.add(nValue);
                }
            }
        }
        return l;
    }

    public static boolean hasChildren(Node node) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    public static WSWhereItem buildWhereItems(String criteria) {
        Pattern p = Pattern.compile("\\((.*)\\)"); //$NON-NLS-1$
        Matcher m = p.matcher(criteria);
        if (m.matches()) {
            criteria = m.group(1);
        }
        String[] criterias = criteria.split("[\\s]+OR[\\s]+"); //$NON-NLS-1$
        ArrayList<WSWhereItem> conditions = new ArrayList<WSWhereItem>();
        for (String cria : criterias) {
            ArrayList<WSWhereItem> condition = new ArrayList<WSWhereItem>();
            m = p.matcher(cria);
            if (m.matches()) {
                cria = m.group(1);
            }
            String[] subCriterias = cria.split("[\\s]+AND[\\s]+"); //$NON-NLS-1$
            // add by ymli; fix the bug: 0011974. remove "(" at the left and ")" at the right
            for (String subCria : subCriterias) {
                if (subCria.startsWith("(")) { //$NON-NLS-1$
                    subCria = subCria.substring(1);
                }
                if (subCria.endsWith(")")) {
                    subCria = subCria.substring(0, subCria.length() - 1);
                }
                WSWhereItem whereItem = buildWhereItem(subCria);
                if (whereItem != null) {
                    condition.add(whereItem);
                }
            }
            if (condition.size() > 0) {
                WSWhereAnd and = new WSWhereAnd(condition.toArray(new WSWhereItem[condition.size()]));
                WSWhereItem whand = new WSWhereItem(null, and, null);
                conditions.add(whand);
            }
        }
        WSWhereOr or = new WSWhereOr(conditions.toArray(new WSWhereItem[conditions.size()]));
        return new WSWhereItem(null, null, or);
    }

    public static WSWhereItem buildWhereItem(String criteria) {
        WSWhereItem wi;
        String[] filters = criteria.split(" "); //$NON-NLS-1$
        String filterXpaths, filterOperators, filterValues;
        filterXpaths = filters[0];
        filterOperators = filters[1];
        if (filters.length <= 2) {
            filterValues = " "; //$NON-NLS-1$
        } else if (filters.length == 3) {
            filterValues = filters[2];
        } else {// more than 3 mean filterValues contains whitespace
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < filters.length; i++) {
                sb.append(filters[i]);
                if (i < filters.length - 1) {
                    sb.append(" ");//$NON-NLS-1$
                }
            }
            filterValues = sb.toString();
        }
        if (filterXpaths == null || filterXpaths.trim().equals("")) {
            return null;
        }
        WSWhereCondition wc = new WSWhereCondition(filterXpaths, Util.getOperator(filterOperators), filterValues,
                WSStringPredicate.NONE, false);
        ArrayList<WSWhereItem> conditions = new ArrayList<WSWhereItem>();
        WSWhereItem item = new WSWhereItem(wc, null, null);
        conditions.add(item);
        if (conditions.size() == 0) {
            wi = null;
        } else {
            WSWhereAnd and = new WSWhereAnd(conditions.toArray(new WSWhereItem[conditions.size()]));
            wi = new WSWhereItem(null, and, null);
        }
        return wi;
    }

    // added by lzhang, for 22001
    // explicit replaced current element with root element
    public static String getFormatedFKInfo(String info, String conceptName) {
        info = info.substring(info.startsWith("/") ? 1 : 0); //$NON-NLS-1$
        String formatedInfo = info;
        if (info.startsWith("./")) {
            formatedInfo = info.replaceFirst("./", conceptName + "/"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return formatedInfo;
    }

    public static String getForeignKeyList(int start, int limit, String value, String xpathForeignKey,
            String xpathInfoForeignKey, String fkFilter, boolean isCount) throws Exception {
        if (xpathForeignKey == null) {
            return null;
        }
        String initXpathForeignKey = getForeignPathFromPath(xpathForeignKey);
        WSWhereCondition whereCondition = getConditionFromPath(xpathForeignKey);
        WSWhereItem whereItem = null;
        if (whereCondition != null) {
            whereItem = new WSWhereItem(whereCondition, null, null);
        }
        if (!isCustomFilter(fkFilter)) {
            // get FK filter
            WSWhereItem fkFilterWi = getConditionFromFKFilter(xpathForeignKey, xpathInfoForeignKey, fkFilter);
            if (fkFilterWi != null) {
                whereItem = fkFilterWi;
            }
        }
        Configuration config = Configuration.getInstance();
        // aiming modify there is bug when initXpathForeignKey when it's like 'conceptname/key'
        // so we convert initXpathForeignKey to 'conceptname'
        initXpathForeignKey = initXpathForeignKey.split("/")[0]; //$NON-NLS-1$
        // end
        xpathInfoForeignKey = xpathInfoForeignKey == null ? "" : xpathInfoForeignKey; //$NON-NLS-1$
        // foreign key set by business concept
        if (initXpathForeignKey.split("/").length == 1) { //$NON-NLS-1$
            // determine if we have xPath Infos: e.g. labels to display
            String[] xpathInfos = new String[1];
            if (xpathInfoForeignKey.trim().length() != 0) {
                xpathInfos = xpathInfoForeignKey.split(","); //$NON-NLS-1$
            } else {
                xpathInfos[0] = initXpathForeignKey;
            }
            // aiming add .* to value
            value = value == null ? "" : value; //$NON-NLS-1$
            // end
            // build query - add a content condition on the pivot if we search for a particular value
            // hshu fix bug 0013849: Lazy loading of FK picker always get records from the first 20 records of all
            // records
            if (value != null && !"".equals(value.trim()) && !".*".equals(value.trim())) { //$NON-NLS-1$ //$NON-NLS-2$
                List<WSWhereItem> condition = new ArrayList<WSWhereItem>();
                if (whereItem != null) {
                    condition.add(whereItem);
                }
                String queryKeyWord = isCount ? " CONTAINS " : " EQUALS "; //$NON-NLS-1$ //$NON-NLS-2$
                String fkWhere = initXpathForeignKey + "/../*" + queryKeyWord + value; //$NON-NLS-1$ 
                if (xpathInfoForeignKey.trim().length() > 0) {
                    StringBuilder ids = new StringBuilder();
                    String realXpathForeignKey = null; // In studio, ForeignKey = ConceptName, but not ConceptName/Id
                    if (!xpathForeignKey.contains("/")) { //$NON-NLS-1$
                        String[] fks = getBusinessConceptKeys(initXpathForeignKey);
                        if (fks != null && fks.length > 0) {
                            realXpathForeignKey = fks[0];
                            for (int i = 0; i < fks.length; i++) {
                                ids.append(fks[i]).append(queryKeyWord).append(value);
                                if (i != fks.length - 1) {
                                    ids.append(" OR "); //$NON-NLS-1$
                                }
                            }
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    if (isCount) {
                        for (String fkInfo : xpathInfos) {
                            sb.append(
                                    fkInfo.startsWith(".") ? convertAbsolutePath( //$NON-NLS-1$
                                            (realXpathForeignKey != null && realXpathForeignKey.trim().length() > 0) ? realXpathForeignKey
                                                    : xpathForeignKey, fkInfo)
                                            : fkInfo).append(" CONTAINS ").append(value); //$NON-NLS-1$
                            sb.append(" OR "); //$NON-NLS-1$
                        }
                    }
                    if (realXpathForeignKey != null) {
                        sb.append(ids.toString());
                    } else {
                        sb.append(xpathForeignKey).append(queryKeyWord).append(value);
                    }
                    fkWhere = sb.toString();
                }
                WSWhereItem wc = buildWhereItems(fkWhere);
                condition.add(wc);
                WSWhereAnd and = new WSWhereAnd(condition.toArray(new WSWhereItem[condition.size()]));
                WSWhereItem whereAnd = new WSWhereItem(null, and, null);
                if (whereAnd != null) {
                    whereItem = whereAnd;
                }
            }
            // add the xPath Infos Path
            ArrayList<String> xPaths = new ArrayList<String>();
            for (String xpathInfo : xpathInfos) {
                xPaths.add(getFormatedFKInfo(xpathInfo.replaceFirst(initXpathForeignKey, initXpathForeignKey),
                        initXpathForeignKey));
            }
            // add the key paths last, since there may be multiple keys
            xPaths.add(initXpathForeignKey + "/../../i"); //$NON-NLS-1$
            // order by
            String orderByPath = null;
            if (!"".equals(xpathInfoForeignKey) && xpathInfoForeignKey != null) { //$NON-NLS-1$
                orderByPath = getFormatedFKInfo(xpathInfos[0].replaceFirst(initXpathForeignKey, initXpathForeignKey),
                        initXpathForeignKey);
            }
            // Run the query
            String[] results;
            if (!isCustomFilter(fkFilter)) {
                results = getPort().xPathsSearch(
                        new WSXPathsSearch(new WSDataClusterPK(config.getCluster()), null, new WSStringArray(xPaths
                                .toArray(new String[xPaths.size()])), whereItem, -1, start, limit, orderByPath, null, true))
                        .getStrings();
            } else {
                results = getPort().getItemsByCustomFKFilters(
                        new WSGetItemsByCustomFKFilters(new WSDataClusterPK(config.getCluster()), initXpathForeignKey,
                                new WSStringArray(xPaths.toArray(new String[xPaths.size()])), getInjectedXpath(fkFilter), start,
                                limit, orderByPath, null, true, whereItem)).getStrings();
            }
            if (results == null) {
                results = new String[] { "0" }; // No result (count = 0) //$NON-NLS-1$
            }
            JSONObject json = new JSONObject();
            JSONArray rows = new JSONArray();
            json.put("rows", rows); //$NON-NLS-1$
            // add (?i) to incasesensitive
            // parse the results - each result contains the xPathInfo values, followed by the keys
            // the first row is totalCount
            // TMDM-2834: Starts from second result (first one is count).
            for (int i = 1; i < results.length; i++) {
                // process no infos case
                if (!results[i].startsWith("<result>")) { //$NON-NLS-1$
                    results[i] = "<result>" + results[i] + "</result>"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                results[i] = results[i].replaceAll("\\n", "");// replace \n //$NON-NLS-1$ //$NON-NLS-2$
                results[i] = results[i].replaceAll(">(\\s+)<", "><"); // replace spaces between elements //$NON-NLS-1$ //$NON-NLS-2$
                Element root = parse(results[i]).getDocumentElement();

                // recover keys - change the parsing method(It is different order when db is SQL but not XML db)
                String keys = ""; //$NON-NLS-1$
                NodeList nodes = getNodeList(root, "//i"); //$NON-NLS-1$
                if (nodes != null) {
                    // when isCount = false, SQL db result(<result><Name>test</Name><Id>1</Id></result>)
                    if (nodes.getLength() == 0) {
                        nodes = getNodeList(root, xpathForeignKey.split("/")[1]); //$NON-NLS-1$
                    }
                    for (int j = 0; j < nodes.getLength(); j++) {
                        if (nodes.item(j) instanceof Element) {
                            keys += "[" + (nodes.item(j).getTextContent() == null ? "" : nodes.item(j).getTextContent()) + "]"; //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
                        }
                    }
                }
                // recover xPathInfos
                String infos = ""; //$NON-NLS-1$
                // if no xPath Infos given, use the key values
                if (xpathInfos.length == 0 || "".equals(xpathInfoForeignKey) || xpathInfoForeignKey == null) { //$NON-NLS-1$
                    infos = keys;
                } else {
                    // build a dash separated string of xPath Infos
                    for (String xpath : xpathInfos) {
                        String fkInfoValue = getFirstTextNode(root, xpath.split("/")[1]); //$NON-NLS-1$
                        fkInfoValue = fkInfoValue != null && fkInfoValue.trim().length() > 0 ? fkInfoValue : ""; //$NON-NLS-1$
                        if (infos.length() == 0) {
                            infos += fkInfoValue;
                        } else {
                            infos += "-" + fkInfoValue; //$NON-NLS-1$
                        }
                    }
                }
                if ((!keys.equals("[]") && !keys.equals("")) || (!infos.equals("") && !infos.equals("[]"))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    JSONObject row = new JSONObject();
                    row.put("keys", keys); //$NON-NLS-1$
                    row.put("infos", infos); //$NON-NLS-1$
                    rows.put(row);
                }
            }
            // edit by ymli; fix the bug:0011918: set the pageSize correctly.
            // FIXME: why do you invoke this method twice
            if (isCount) {
                Matcher matcher = TOTAL_COUNT_PATTERN.matcher(results[0]);
                if (matcher.matches()) {
                    String count = matcher.group(1);
                    json.put("count", count); //$NON-NLS-1$
                } else {
                    throw new IllegalArgumentException("Total count '" + results[0] + "' does not match expected format"); //$NON-NLS-1$ //$NON-NLS-2$
                }

            }
            return json.toString();
        }
        throw new Exception("Unexpected concept name: '" + initXpathForeignKey + "'"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String countForeignKey_filter(String xpathForeignKey, String xpathForeignKeyinfo, String fkFilter)
            throws Exception {
        return countForeignKey_filter(xpathForeignKey, xpathForeignKeyinfo, fkFilter, null);
    }

    public static String countForeignKey_filter(String xpathForeignKey, String xpathForeignKeyinfo, String fkFilter,
            String criteriaValue) throws Exception {
        Configuration config = Configuration.getInstance();
        String conceptName = getConceptFromPath(xpathForeignKey);
        boolean isCustom = isCustomFilter(fkFilter);
        String count;
        if (!isCustom) {
            WSWhereCondition whereCondition = getConditionFromPath(xpathForeignKey);
            WSWhereItem whereItem = null;
            if (whereCondition != null) {
                whereItem = new WSWhereItem(whereCondition, null, null);
            }
            // get FK filter
            WSWhereItem fkFilterWi = getConditionFromFKFilter(xpathForeignKey, xpathForeignKeyinfo, fkFilter);
            if (fkFilterWi != null) {
                whereItem = fkFilterWi;
            }
            if (criteriaValue != null && criteriaValue.trim().length() > 0) {
                List<WSWhereItem> condition = new ArrayList<WSWhereItem>();
                if (whereItem != null) {
                    condition.add(whereItem);
                }
                String criteriaCondition;
                if (MDMConfiguration.getDBType().getName().equals(EDBType.QIZX.getName())) {
                    criteriaCondition = conceptName + "/../* CONTAINS "; //$NON-NLS-1$
                } else {
                    criteriaCondition = conceptName + "/../. CONTAINS "; //$NON-NLS-1$
                }
                criteriaCondition += criteriaValue;
                WSWhereItem wc = buildWhereItem(criteriaCondition);
                condition.add(wc);
                WSWhereAnd and = new WSWhereAnd(condition.toArray(new WSWhereItem[condition.size()]));
                whereItem = new WSWhereItem(null, and, null);
            }
            count = getPort().count(new WSCount(new WSDataClusterPK(config.getCluster()), conceptName, whereItem,// null,
                    -1)).getValue();
        } else {
            String injectedXpath = getInjectedXpath(fkFilter);

            count = getPort().countItemsByCustomFKFilters(
                    new WSCountItemsByCustomFKFilters(new WSDataClusterPK(config.getCluster()), conceptName, injectedXpath))
                    .getValue();
        }
        return count;
    }

    public static String getInjectedXpath(String fkFilter) {
        return fkFilter.substring(6);
    }

    public static boolean isCustomFilter(String fkFilter) {
        boolean isCustom = false;
        if (fkFilter != null && fkFilter.startsWith("$CFFP:")) {
            isCustom = true;
        }
        return isCustom;
    }

    public static boolean isTransformerExist(String transformerPK) {
        try {
            boolean isBeforeSavingTransformerExist = false;
            @SuppressWarnings("unchecked")
            Collection<TransformerV2POJOPK> wst = com.amalto.core.util.Util.getTransformerV2CtrlLocal().getTransformerPKs("*"); //$NON-NLS-1$
            for (TransformerV2POJOPK id : wst) {
                if (id.getIds()[0].equals(transformerPK)) {
                    isBeforeSavingTransformerExist = true;
                    break;
                }
            }
            return isBeforeSavingTransformerExist;
        } catch (XtentisException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (CreateException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkDCAndDM(String dataContainer, String dataModel) {
        Configuration config;
        try {
            config = Configuration.getInstance(true);
            return config.getCluster().equals(dataContainer) && config.getModel().equals(dataModel);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Deprecated
    public static String formatDate(String formats, Calendar date) {
        String formatValue = formats;
        if (formats.startsWith(DATE_FORMAT_PREFIX)) {
            while (true) {
                if (!formatValue.contains(DATE_FORMAT_PREFIX)) {
                    break;
                }
                String format = formatValue.substring(formatValue.indexOf(DATE_FORMAT_PREFIX),
                        formatValue.indexOf(DATE_FORMAT_PREFIX) + 3);
                String valueStr = String.format(java.util.Locale.ENGLISH, format, date);
                formatValue = formatValue.replace(format, valueStr);
            }
        } else {
            formatValue = String.format(java.util.Locale.ENGLISH, formats, date);
        }

        return formatValue;
    }

    public static String convertAbsolutePath(String currentPath, String xpath) {
        StringBuilder sb = new StringBuilder();
        String[] ops = xpath.split("/"); //$NON-NLS-1$
        String[] eles = currentPath.split("/"); //$NON-NLS-1$
        int num = 0;
        if (xpath.startsWith("..")) { //$NON-NLS-1$
            for (String op : ops) {
                if (op.equals("..")) { //$NON-NLS-1$
                    num += 1;
                }
            }
            for (int i = 0; i < eles.length - num; i++) {
                sb.append(eles[i]);
                sb.append("/"); //$NON-NLS-1$
            }
        } else if (xpath.startsWith(".")) { //$NON-NLS-1$
            sb.append(eles[0]);
            sb.append("/"); //$NON-NLS-1$
        }
        sb.append(ops[ops.length - 1]);
        return sb.toString();
    }

    public static String[] getBusinessConceptKeys(String concept) throws Exception {
        Configuration config = Configuration.getConfiguration();
        String model = config.getModel();

        WSConceptKey key = getPort().getBusinessConceptKey(new WSGetBusinessConceptKey(new WSDataModelPK(model), concept));
        WSConceptKey copyKey = new WSConceptKey();
        copyKey.setFields((String[]) ArrayUtils.clone(key.getFields()));
        copyKey.setSelector(key.getSelector());

        String[] keys = copyKey.getFields();
        for (int i = 0; i < keys.length; i++) {
            if (".".equals(key.getSelector())) {
                keys[i] = concept + "/" + keys[i]; //$NON-NLS-1$ 
            } else {
                keys[i] = key.getSelector() + keys[i];
            }
        }
        return keys;
    }

    public static boolean causeIs(Throwable throwable, Class<?> cls) {
        Throwable currentCause = throwable;
        while (currentCause != null) {
            if (cls.isInstance(currentCause)) {
                return true;
            }
            currentCause = currentCause.getCause();
        }
        return false;
    }

    public static <T> T cause(Throwable throwable, Class<T> cls) {
        Throwable currentCause = throwable;
        while (currentCause != null) {
            if (cls.isInstance(currentCause)) {
                return (T) currentCause;
            }
            currentCause = currentCause.getCause();
        }
        return null;
    }

    @Deprecated
    public static String getExceptionMessage(String message, String language) {
        if (message == null || message.contains("<msg/>")) {
            return ""; //$NON-NLS-1$
        }
        // Message tip : "<msg>[EN:validate error][FR:validate error]</msg>"
        if (message.contains("<msg>")) { //$NON-NLS-1$
            int index = message.indexOf(language.toUpperCase() + ":"); //$NON-NLS-1$
            if (index != -1) {
                return message.substring(index + language.length() + 1, index + message.substring(index).indexOf("]")); //$NON-NLS-1$
            } else {
                return message.substring(message.indexOf("<msg>"), message.indexOf("</msg>") + 6); //$NON-NLS-1$//$NON-NLS-2$
            }
        }
        return message;
    }

    public static Set<String> getNoAccessRoleSet(XSElementDecl decl) {
        Set<String> roleSet = new HashSet<String>();
        XSAnnotation xsa = decl.getAnnotation();
        if (xsa == null) {
            return roleSet;
        }
        Element el = (Element) xsa.getAnnotation();
        NodeList annotList = el.getChildNodes();
        for (int k = 0; k < annotList.getLength(); k++) {
            if ("appinfo".equals(annotList.item(k).getLocalName())) { //$NON-NLS-1$
                Node source1 = annotList.item(k).getAttributes().getNamedItem("source"); //$NON-NLS-1$         
                if (source1 == null) {
                    continue;
                }
                String appinfoSource = annotList.item(k).getAttributes().getNamedItem("source").getNodeValue(); //$NON-NLS-1$

                if ("X_Hide".equals(appinfoSource)) { //$NON-NLS-1$
                    roleSet.add(annotList.item(k).getFirstChild().getNodeValue());
                }
            }
        }
        return roleSet;
    }

    public static boolean isAuth(Set<String> roleSet) throws Exception {
        String roles = getPrincipalMember("Roles"); //$NON-NLS-1$
        String[] roleArr = roles.split(","); //$NON-NLS-1$
        for (String role : roleArr) {
            if (roleSet.contains(role)) {
                return false;
            }
        }
        return true;
    }

    public static void filterAuthViews(Map<String, String> viewMap) throws Exception {
        if (viewMap == null || viewMap.size() == 0) {
            return;
        }
        if (Webapp.INSTANCE.isEnterpriseVersion()) {
            ILocalUser localUser = LocalUser.getLocalUser();
            for (String viewInstanceId : viewMap.keySet()) {
                if (!localUser.userCanWrite(ViewPOJO.class, viewInstanceId)
                        && !localUser.userCanRead(ViewPOJO.class, viewInstanceId)) {
                    viewMap.remove(viewInstanceId);
                }
            }
        }
    }

    public static String getDefaultLanguage() throws Exception {
        String defaultLanguage = ""; //$NON-NLS-1$
        String userName = Util.getAjaxSubject().getUsername();
        WSItemPK itemPK = new WSItemPK(new WSDataClusterPK(DATACLUSTER_PK), PROVISIONING_CONCEPT, new String[] { userName });
        if (userName != null && userName.length() > 0) {
            Document doc = XMLUtils.parse(Util.getPort().getItem(new WSGetItem(itemPK)).getContent());
            if (doc.getElementsByTagName("language") != null) { //$NON-NLS-1$
                Node language = doc.getElementsByTagName("language").item(0); //$NON-NLS-1$
                if (language != null) {
                    return language.getTextContent();
                }
            }
        }
        return defaultLanguage;
    }

    // Should only be used for non-data related changes (leave to core modules creation of UPDATE update reports).
    public static String createUpdateReport(String[] ids, String concept, String operationType) throws Exception {
        if (!operationType.equals(UpdateReportPOJO.OPERATION_TYPE_ACTION)) {
            // This method should never implement anything but update reports for "ACTION" type (no UPDATE nor DELETE in here).
            throw new IllegalArgumentException("Only '" + UpdateReportPOJO.OPERATION_TYPE_ACTION + "' action is supported.");
        }
        Configuration config = Configuration.getInstance();
        String dataModelPK = config.getModel() == null ? StringUtils.EMPTY : config.getModel();
        String dataClusterPK = config.getCluster() == null ? StringUtils.EMPTY : config.getCluster();
        String username = Util.getLoginUserName();
        StringBuilder keyBuilder = new StringBuilder();
        if (ids != null) {
            for (int i = 0; i < ids.length; i++) {
                keyBuilder.append(ids[i]);
                if (i != ids.length - 1) {
                    keyBuilder.append('.');
                }
            }
        }
        String key = keyBuilder.length() == 0 ? "null" : keyBuilder.toString(); //$NON-NLS-1$
        return "<Update><UserName>" + username + "</UserName><Source>genericUI</Source><TimeInMillis>" //$NON-NLS-1$ //$NON-NLS-2$
                + System.currentTimeMillis() + "</TimeInMillis><OperationType>" + operationType //$NON-NLS-1$
                + "</OperationType><RevisionID>null</RevisionID><DataCluster>" + dataClusterPK  //$NON-NLS-1$
                + "</DataCluster><DataModel>" + dataModelPK + "</DataModel><Concept>" + StringEscapeUtils.escapeXml(concept) //$NON-NLS-1$ //$NON-NLS-2$
                + "</Concept><Key>" + StringEscapeUtils.escapeXml(key) + "</Key>" + "</Update>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
