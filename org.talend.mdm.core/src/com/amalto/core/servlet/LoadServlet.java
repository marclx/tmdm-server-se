package com.amalto.core.servlet;

import com.amalto.core.ejb.local.XmlServerSLWrapperLocal;
import com.amalto.core.load.action.DefaultLoadAction;
import com.amalto.core.load.action.LoadAction;
import com.amalto.core.load.action.OptimizedLoadAction;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJO;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJOPK;
import com.amalto.core.objects.datacluster.ejb.local.DataClusterCtrlLocal;
import com.amalto.core.save.DocumentSaverContext;
import com.amalto.core.save.SaverSession;
import com.amalto.core.save.context.DocumentSaver;
import com.amalto.core.save.context.SaverContextFactory;
import com.amalto.core.server.MetadataRepositoryAdmin;
import com.amalto.core.server.ServerContext;
import com.amalto.core.util.Util;
import com.amalto.core.util.XSDKey;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.talend.mdm.commmon.util.core.EDBType;
import org.talend.mdm.commmon.util.core.EUUIDCustomType;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


/**
 * @author starkey
 */

public class LoadServlet extends HttpServlet {

    private static final String PARAMETER_CLUSTER = "cluster"; //$NON-NLS-1$

    private static final String PARAMETER_CONCEPT = "concept"; //$NON-NLS-1$

    private static final String PARAMETER_DATAMODEL = "datamodel"; //$NON-NLS-1$

    private static final String PARAMETER_VALIDATE = "validate"; //$NON-NLS-1$

    private static final String PARAMETER_SMARTPK = "smartpk"; //$NON-NLS-1$

    private static final Logger log = Logger.getLogger(LoadServlet.class);

    public static final Map<String, XSDKey> typeNameToKeyDef = new HashMap<String, XSDKey>();

    private static final String PARAMETER_ACTION = "action"; //$NON-NLS-1$

    public LoadServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
        response.setContentType("text/html; charset=UTF-8"); //$NON-NLS-1$
        response.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
        // configure writer depending on logger configuration.
        PrintWriter writer = configureWriter(response);
        writer.write("<html><body>"); //$NON-NLS-1$
        writer.write(
                "<p><b>Load data into MDM</b><br/>" +
                        "Check jboss/server/default/log/server.log or the jboss console output to determine when load is completed</b></p>"
        );
        String dataClusterName = request.getParameter(PARAMETER_CLUSTER);
        String typeName = request.getParameter(PARAMETER_CONCEPT);
        String dataModelName = request.getParameter(PARAMETER_DATAMODEL);
        boolean needValidate = Boolean.valueOf(request.getParameter(PARAMETER_VALIDATE));
        boolean needAutoGenPK = Boolean.valueOf(request.getParameter(PARAMETER_SMARTPK));
        String action = request.getParameter(PARAMETER_ACTION);
        if (action == null || action.length() == 0) {
            action = getServletConfig().getInitParameter(PARAMETER_ACTION);
        }
        // We support only load as action here
        if (!"load".equalsIgnoreCase(action)) { //$NON-NLS-1$
            throw new ServletException(new UnsupportedOperationException("Action '" + action + "' isn't supported"));
        }
        LoadAction loadAction = getLoadAction(dataClusterName, typeName, dataModelName, needValidate, needAutoGenPK);
        if (needValidate && !loadAction.supportValidation()) {
            throw new ServletException(new UnsupportedOperationException("XML Validation isn't supported"));
        }
        // Get xml server and key information
        XmlServerSLWrapperLocal server;
        XSDKey keyMetadata;
        try {
            keyMetadata = getTypeKey(dataModelName, typeName);
            server = Util.getXmlServerCtrlLocal();
        } catch (Exception e) {
            throw new ServletException(e);
        }
        SaverSession session = SaverSession.newSession();
        SaverContextFactory contextFactory = session.getContextFactory();
        DocumentSaverContext context = contextFactory.createBulkLoad(dataClusterName, dataModelName, keyMetadata, request.getInputStream(), loadAction, server);
        DocumentSaver saver = context.createSaver();
        try {
            session.begin(dataClusterName);
            saver.save(session, context);
            session.end();
            // End the load (might persist counter state in case of autogen pk).
            loadAction.endLoad(server);
        } catch (Exception e) {
            try {
                session.abort();
            } catch (Exception rollbackException) {
                log.error("Ignoring rollback exception", rollbackException);
            }
            throw new ServletException(e);
        }
        writer.write("</body></html>"); //$NON-NLS-1$
    }

    protected LoadAction getLoadAction(String dataClusterName, String typeName, String dataModelName, boolean needValidate, boolean needAutoGenPK) {
        // Test if the data cluster actually exists
        DataClusterPOJO dataCluster = getDataCluster(dataClusterName);
        if (dataCluster == null) {
            throw new IllegalArgumentException("Data cluster '" + dataClusterName + "' does not exist.");
        }
        // Activate optimizations if Qizx is used.
        Object dbType = MDMConfiguration.getConfiguration().get("xmldb.type"); //$NON-NLS-1$
        boolean isUsingQizx = dbType != null && EDBType.QIZX.getName().equals(dbType.toString());
        // Activate optimizations if SQL is used.
        boolean isUsingSQL = MDMConfiguration.isSqlDataBase();
        LoadAction loadAction;
        if (needValidate || (!isUsingQizx && !isUsingSQL) || XSystemObjects.DC_PROVISIONING.getName().equals(dataClusterName)) {
            loadAction = new DefaultLoadAction(dataClusterName, dataModelName, needValidate);
        } else {
            loadAction = new OptimizedLoadAction(dataClusterName, typeName, dataModelName, needAutoGenPK);
        }
        if (log.isDebugEnabled()) {
            log.debug("Load action selected for load: " + loadAction.getClass().getName() + "(isUsingQizx: " + isUsingQizx //$NON-NLS-1$ //$NON-NLS-2$
                    + " / needValidate:" + needValidate + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return loadAction;
    }

    protected DataClusterPOJO getDataCluster(String dataClusterName) {
        DataClusterPOJO dataCluster;
        try {
            DataClusterCtrlLocal dataClusterCtrlLocal = Util.getDataClusterCtrlLocal();
            DataClusterPOJOPK dataClusterPOJOPK = new DataClusterPOJOPK(dataClusterName);
            dataCluster = dataClusterCtrlLocal.getDataCluster(dataClusterPOJOPK);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return dataCluster;
    }

    private XSDKey getTypeKey(String dataModelName, String typeName) throws Exception {
        MetadataRepositoryAdmin repositoryAdmin = ServerContext.INSTANCE.get().getMetadataRepositoryAdmin();
        MetadataRepository repository = repositoryAdmin.get(dataModelName);
        ComplexTypeMetadata type = repository.getComplexType(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Type '" + typeName + "' does not exist in data model '" + dataModelName + "'.");
        }
        String[] fields = new String[type.getKeyFields().size()];
        String[] fieldTypes = new String[type.getKeyFields().size()];
        int i = 0;
        for (FieldMetadata keyField : type.getKeyFields()) {
            fields[i] = keyField.getPath();
            String name = keyField.getType().getName();
            if (EUUIDCustomType.AUTO_INCREMENT.getName().equals(name) || EUUIDCustomType.UUID.getName().equals(name)) { // See TMDM-6687
                fieldTypes[i] = name;
            } else {
                fieldTypes[i] = "xsd:" + name; //$NON-NLS-1$
            }
            i++;
        }
        return new XSDKey(".", fields, fieldTypes);
    }

    /**
     * Returns a writer that does not print anything if logger hasn't DEBUG level.
     *
     * @param resp A servlet response output.
     * @return The same {@link HttpServletResponse} if debug <b>is</b> enabled, or a no-op one if debug is disabled.
     * @throws IOException Thrown by {@link javax.servlet.http.HttpServletResponse#getWriter()}.
     */
    private static PrintWriter configureWriter(HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        if (log.isDebugEnabled()) {
            return writer;
        } else {
            return new NoOpPrintWriter(writer);
        }
    }

    /**
     * A {@link PrintWriter} implementation that intercepts all write method calls.
     *
     * @see LoadServlet#configureWriter(javax.servlet.http.HttpServletResponse)
     */
    private static class NoOpPrintWriter extends PrintWriter {

        private NoOpPrintWriter(PrintWriter writer) {
            super(writer);
        }

        @Override
        public void write(int c) {
            // Nothing to do (debug isn't enabled)
        }

        @Override
        public void write(char[] buf, int off, int len) {
            // Nothing to do (debug isn't enabled)
        }

        @Override
        public void write(char[] buf) {
            // Nothing to do (debug isn't enabled)
        }

        @Override
        public void write(String s, int off, int len) {
            // Nothing to do (debug isn't enabled)
        }

        @Override
        public void write(String s) {
            // Nothing to do (debug isn't enabled)
        }
    }
}