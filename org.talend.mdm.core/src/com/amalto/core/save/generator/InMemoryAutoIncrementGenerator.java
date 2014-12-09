package com.amalto.core.save.generator;

import com.amalto.core.ejb.ItemPOJO;
import com.amalto.core.ejb.ItemPOJOPK;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJOPK;
import com.amalto.core.server.XmlServer;
import com.amalto.core.util.Util;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;

import java.util.Properties;

class InMemoryAutoIncrementGenerator implements AutoIdGenerator {

    private static final Logger            logger         = Logger.getLogger(InMemoryAutoIncrementGenerator.class);

    private static final DataClusterPOJOPK DC             = new DataClusterPOJOPK(XSystemObjects.DC_CONF.getName());

    private static final String[]          IDS            = new String[] { "AutoIncrement" };                       // $NON-NLS-1$

    private static final String            CONCEPT        = "AutoIncrement";                                        // $NON-NLS-1$

    public static String                   AUTO_INCREMENT = "Auto_Increment";                                       // $NON-NLS-1$

    private static Properties              CONFIGURATION  = null;

    private boolean                        wasInitCalled  = false;

    // This method is not secure in clustered environments (when several MDM nodes share same database).
    // See com.amalto.core.save.generator.StorageAutoIncrementGenerator for better concurrency support.
    @Override
    public String generateId(String dataClusterName, String conceptName, String keyElementName) {
        if (!wasInitCalled) {
            init();
        }
        String key = dataClusterName + "." + conceptName + "." + keyElementName;
        long num;
        String n = CONFIGURATION.getProperty(key);
        if (n == null) {
            num = 0;
        } else {
            num = Long.valueOf(n);
        }
        num++;
        CONFIGURATION.setProperty(key, String.valueOf(num));
        return String.valueOf(num);
    }

    @Override
    public void saveState(XmlServer server) {
        try {
            server.start(XSystemObjects.DC_CONF.getName());
            String xmlString = Util.convertAutoIncrement(CONFIGURATION);
            server.putDocumentFromString(xmlString, CONCEPT, DC.getUniqueId(), null);
            server.commit(XSystemObjects.DC_CONF.getName());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public synchronized void init() {
        if (wasInitCalled) {
            return;
        }
        // first try Current path
        CONFIGURATION = new Properties();
        try {
            ItemPOJOPK pk = new ItemPOJOPK(DC, CONCEPT, IDS);
            ItemPOJO itempojo = ItemPOJO.load(pk);
            if (itempojo == null) {
                logger.info("Could not load configuration from database, use default configuration."); //$NON-NLS-1$
            } else {
                String xml = itempojo.getProjectionAsString();
                if (xml != null && xml.trim().length() > 0) {
                    CONFIGURATION = Util.convertAutoIncrement(xml);
                }
            }
            wasInitCalled = true;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }
}
