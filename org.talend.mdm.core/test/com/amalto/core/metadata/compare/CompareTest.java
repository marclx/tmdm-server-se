/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.metadata.compare;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.talend.mdm.commmon.metadata.compare.Change;
import org.talend.mdm.commmon.metadata.compare.Compare;
import org.talend.mdm.commmon.metadata.compare.HibernateStorageImpactAnalyzer;
import org.talend.mdm.commmon.metadata.compare.ImpactAnalyzer;

import com.amalto.core.ejb.ObjectPOJO;
import com.amalto.core.metadata.ClassRepository;
import com.amalto.core.objects.datamodel.ejb.DataModelPOJO;
import com.amalto.core.query.StorageTestCase;
import com.amalto.core.query.user.Expression;
import com.amalto.core.server.MockServerLifecycle;
import com.amalto.core.server.ServerContext;
import com.amalto.core.storage.SecuredStorage;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.datasource.DataSourceDefinition;
import com.amalto.core.storage.hibernate.HibernateStorage;

@SuppressWarnings("nls")
public class CompareTest extends TestCase {

    private static Logger LOG = Logger.getLogger(StorageTestCase.class);

    public void test1() throws Exception {
        MetadataRepository original = new MetadataRepository();
        original.load(CompareTest.class.getResourceAsStream("schema1_1.xsd"));
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema1_2.xsd"));
        Compare.DiffResults diffResults = Compare.compare(original, updated);
        assertEquals(4, diffResults.getActions().size());
        assertEquals(1, diffResults.getModifyChanges().size());
        assertEquals(3, diffResults.getRemoveChanges().size());

        ImpactAnalyzer analyzer = new HibernateStorageImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(2, sort.get(ImpactAnalyzer.Impact.HIGH).size());
        assertEquals(2, sort.get(ImpactAnalyzer.Impact.MEDIUM).size());
    }

    public void test2() throws Exception {
        MetadataRepository original = new MetadataRepository();
        original.load(CompareTest.class.getResourceAsStream("schema2_1.xsd"));
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema2_2.xsd"));
        Compare.DiffResults diffResults = Compare.compare(original, updated);
        assertEquals(1, diffResults.getActions().size());
        assertEquals(0, diffResults.getModifyChanges().size());
        assertEquals(0, diffResults.getRemoveChanges().size());
        assertEquals(1, diffResults.getAddChanges().size());

        ImpactAnalyzer analyzer = new HibernateStorageImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.HIGH).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.MEDIUM).size());
        assertEquals(1, sort.get(ImpactAnalyzer.Impact.LOW).size());
    }

    public void test3() throws Exception {
        MetadataRepository original = new MetadataRepository();
        original.load(CompareTest.class.getResourceAsStream("schema3_1.xsd"));
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema3_2.xsd"));
        Compare.DiffResults diffResults = Compare.compare(original, updated);
        assertEquals(1, diffResults.getActions().size());
        assertEquals(1, diffResults.getModifyChanges().size());
        assertEquals(0, diffResults.getRemoveChanges().size());
        assertEquals(0, diffResults.getAddChanges().size());

        ImpactAnalyzer analyzer = new HibernateStorageImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(1, sort.get(ImpactAnalyzer.Impact.HIGH).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.MEDIUM).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.LOW).size());
    }

    public void test4() throws Exception {
        MetadataRepository original = new MetadataRepository();
        original.load(CompareTest.class.getResourceAsStream("schema4_1.xsd"));
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema4_2.xsd"));
        Compare.DiffResults diffResults = Compare.compare(original, updated);
        assertEquals(1, diffResults.getActions().size());
        assertEquals(1, diffResults.getModifyChanges().size());
        assertEquals(0, diffResults.getRemoveChanges().size());
        assertEquals(0, diffResults.getAddChanges().size());

        ImpactAnalyzer analyzer = new HibernateStorageImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(1, sort.get(ImpactAnalyzer.Impact.HIGH).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.MEDIUM).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.LOW).size());
    }

    public void test5() throws Exception {
        MetadataRepository original = new MetadataRepository();
        original.load(CompareTest.class.getResourceAsStream("schema5_1.xsd"));
        original = original.copy();
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema5_2.xsd")); //$NON-NLS-1$
        Compare.DiffResults diffResults = Compare.compare(original, updated);
        assertEquals(0, diffResults.getActions().size());
        assertEquals(0, diffResults.getModifyChanges().size());
        assertEquals(0, diffResults.getRemoveChanges().size());
        assertEquals(0, diffResults.getAddChanges().size());

        ImpactAnalyzer analyzer = new HibernateStorageImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.HIGH).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.MEDIUM).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.LOW).size());
    }

    public void test6_SystemDMs_CONF() throws Exception {
        Storage storage = new SecuredStorage(new HibernateStorage("MDM", StorageType.SYSTEM), new SecuredStorage.UserDelegator() {

            @Override
            public boolean hide(FieldMetadata field) {
                return false;
            }

            @Override
            public boolean hide(ComplexTypeMetadata type) {
                return false;
            }
        });
        ClassRepository repository = buildRepository();
        String[] models = new String[] { "/com/amalto/core/initdb/data/datamodel/CONF" };
        for (String model : models) {
            InputStream builtInStream = this.getClass().getResourceAsStream(model);
            if (builtInStream == null) {
                throw new RuntimeException("Built in model '" + model + "' cannot be found.");
            }
            try {
                DataModelPOJO modelPOJO = ObjectPOJO.unmarshal(DataModelPOJO.class, IOUtils.toString(builtInStream, "UTF-8")); //$NON-NLS-1$
                repository.load(new ByteArrayInputStream(modelPOJO.getSchema().getBytes("UTF-8")));
            } catch (Exception e) {
                throw new RuntimeException("Could not parse builtin data model '" + model + "'.", e);
            } finally {
                try {
                    builtInStream.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
        LOG.info("Storage prepared.");

        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema6_2.xsd"));
        Compare.DiffResults diffResults = Compare.compare(repository, updated);

        assertEquals(54, diffResults.getActions().size());

        ImpactAnalyzer analyzer = storage.getImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(0, sort.size());
    }

    public void test6_SystemDMs_Reporting() throws Exception {
        Storage storage = new SecuredStorage(new HibernateStorage("MDM", StorageType.SYSTEM), new SecuredStorage.UserDelegator() {

            @Override
            public boolean hide(FieldMetadata field) {
                return false;
            }

            @Override
            public boolean hide(ComplexTypeMetadata type) {
                return false;
            }
        });
        ClassRepository repository = buildRepository();
        String[] models = new String[] { "/com/amalto/core/initdb/data/datamodel/Reporting" };
        for (String model : models) {
            InputStream builtInStream = this.getClass().getResourceAsStream(model);
            if (builtInStream == null) {
                throw new RuntimeException("Built in model '" + model + "' cannot be found.");
            }
            try {
                DataModelPOJO modelPOJO = ObjectPOJO.unmarshal(DataModelPOJO.class, IOUtils.toString(builtInStream, "UTF-8")); //$NON-NLS-1$
                repository.load(new ByteArrayInputStream(modelPOJO.getSchema().getBytes("UTF-8")));
            } catch (Exception e) {
                throw new RuntimeException("Could not parse builtin data model '" + model + "'.", e);
            } finally {
                try {
                    builtInStream.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
        LOG.info("Storage prepared.");

        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema7_2.xsd"));
        Compare.DiffResults diffResults = Compare.compare(repository, updated);

        assertEquals(54, diffResults.getActions().size());

        ImpactAnalyzer analyzer = storage.getImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(0, sort.size());
    }

    public void test6_SystemDMs_PROVISIONING() throws Exception {
        Storage storage = new SecuredStorage(new HibernateStorage("MDM", StorageType.SYSTEM), new SecuredStorage.UserDelegator() {

            @Override
            public boolean hide(FieldMetadata field) {
                return false;
            }

            @Override
            public boolean hide(ComplexTypeMetadata type) {
                return false;
            }
        });
        ClassRepository repository = buildRepository();
        String[] models = new String[] { "/com/amalto/core/initdb/data/datamodel/PROVISIONING" };
        for (String model : models) {
            InputStream builtInStream = this.getClass().getResourceAsStream(model);
            if (builtInStream == null) {
                throw new RuntimeException("Built in model '" + model + "' cannot be found.");
            }
            try {
                DataModelPOJO modelPOJO = ObjectPOJO.unmarshal(DataModelPOJO.class, IOUtils.toString(builtInStream, "UTF-8")); //$NON-NLS-1$
                repository.load(new ByteArrayInputStream(modelPOJO.getSchema().getBytes("UTF-8")));
            } catch (Exception e) {
                throw new RuntimeException("Could not parse builtin data model '" + model + "'.", e);
            } finally {
                try {
                    builtInStream.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema8_2.xsd"));
        Compare.DiffResults diffResults = Compare.compare(repository, updated);

        assertEquals(54, diffResults.getActions().size());

        ImpactAnalyzer analyzer = storage.getImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(0, sort.size());
    }

    public void test6_SystemDMs_SearchTemplate() throws Exception {
        Storage storage = new SecuredStorage(new HibernateStorage("MDM", StorageType.SYSTEM), new SecuredStorage.UserDelegator() {

            @Override
            public boolean hide(FieldMetadata field) {
                return false;
            }

            @Override
            public boolean hide(ComplexTypeMetadata type) {
                return false;
            }
        });
        ClassRepository repository = buildRepository();
        String[] models = new String[] { "/com/amalto/core/initdb/data/datamodel/SearchTemplate" };
        for (String model : models) {
            InputStream builtInStream = this.getClass().getResourceAsStream(model);
            if (builtInStream == null) {
                throw new RuntimeException("Built in model '" + model + "' cannot be found.");
            }
            try {
                DataModelPOJO modelPOJO = ObjectPOJO.unmarshal(DataModelPOJO.class, IOUtils.toString(builtInStream, "UTF-8")); //$NON-NLS-1$
                repository.load(new ByteArrayInputStream(modelPOJO.getSchema().getBytes("UTF-8")));
            } catch (Exception e) {
                throw new RuntimeException("Could not parse builtin data model '" + model + "'.", e);
            } finally {
                try {
                    builtInStream.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema9_2.xsd"));
        Compare.DiffResults diffResults = Compare.compare(repository, updated);

        assertEquals(54, diffResults.getActions().size());

        ImpactAnalyzer analyzer = storage.getImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(0, sort.size());
    }

    public void test7() throws Exception {
        MetadataRepository original = new MetadataRepository();
        original.load(CompareTest.class.getResourceAsStream("schema10_1.xsd")); //$NON-NLS-1$
        original = original.copy();
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema10_2.xsd")); //$NON-NLS-1$
        Compare.DiffResults diffResults = Compare.compare(original, updated);
        assertEquals(1, diffResults.getActions().size());
        assertEquals(1, diffResults.getModifyChanges().size());
        assertEquals(0, diffResults.getRemoveChanges().size());
        assertEquals(0, diffResults.getAddChanges().size());

        ImpactAnalyzer analyzer = new HibernateStorageImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(1, sort.get(ImpactAnalyzer.Impact.HIGH).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.MEDIUM).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.LOW).size());
    }

    public void test8() throws Exception {
        MetadataRepository original = new MetadataRepository();
        original.load(CompareTest.class.getResourceAsStream("schema11_1.xsd")); //$NON-NLS-1$
        original = original.copy();
        MetadataRepository updated = new MetadataRepository();
        updated.load(CompareTest.class.getResourceAsStream("schema11_2.xsd")); //$NON-NLS-1$
        Compare.DiffResults diffResults = Compare.compare(original, updated);
        assertEquals(1, diffResults.getActions().size());
        assertEquals(1, diffResults.getModifyChanges().size());
        assertEquals(0, diffResults.getRemoveChanges().size());
        assertEquals(0, diffResults.getAddChanges().size());

        ImpactAnalyzer analyzer = new HibernateStorageImpactAnalyzer();
        Map<ImpactAnalyzer.Impact, List<Change>> sort = analyzer.analyzeImpacts(diffResults);
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.HIGH).size());
        assertEquals(0, sort.get(ImpactAnalyzer.Impact.MEDIUM).size());
        assertEquals(1, sort.get(ImpactAnalyzer.Impact.LOW).size());
    }

    @SuppressWarnings("rawtypes")
    private ClassRepository buildRepository() {
        ClassRepository repository = new ClassRepository();
        Class[] objectsToParse = new Class[ObjectPOJO.OBJECT_TYPES.length];
        int i = 0;
        for (Object[] objects : ObjectPOJO.OBJECT_TYPES) {
            objectsToParse[i++] = (Class) objects[1];
        }
        repository.load(objectsToParse);
        return repository;
    }
}
