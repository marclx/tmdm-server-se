/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.save;

import com.amalto.core.objects.UpdateReportPOJO;
import com.amalto.core.history.DeleteType;
import com.amalto.core.history.MutableDocument;
import com.amalto.core.save.context.DocumentSaver;
import com.amalto.core.save.context.SaverContextFactory;
import com.amalto.core.save.context.SaverSource;
import com.amalto.core.save.context.StorageDocument;
import com.amalto.core.schema.validation.SkipAttributeDocumentBuilder;
import com.amalto.core.schema.validation.Validator;
import com.amalto.core.schema.validation.XmlSchemaValidator;
import com.amalto.core.util.OutputReport;
import com.amalto.core.util.Util;
import com.amalto.core.util.ValidateException;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.server.MockMetadataRepositoryAdmin;
import com.amalto.core.server.server.MockServerLifecycle;
import com.amalto.core.storage.*;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.DataRecordReader;
import com.amalto.core.storage.record.XmlStringDataRecordReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

@SuppressWarnings("nls")
public class DocumentSaveTest extends TestCase {

    public static final boolean USE_STORAGE_OPTIMIZATIONS = true;

    private static Logger LOG = Logger.getLogger(DocumentSaveTest.class);

    private XPath xPath = XPathFactory.newInstance().newXPath();

    static {
        LOG.info("Setting up MDM server environment...");
        ServerContext.INSTANCE.get(new MockServerLifecycle());
        MDMConfiguration.getConfiguration().setProperty("xmlserver.class", "com.amalto.core.storage.DispatchWrapper");
        SaverSession.setDefaultCommitter(new MockCommitter());
        LOG.info("MDM server environment set.");
    }

    @Override
    public void setUp() throws Exception {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        xPath = xPathFactory.newXPath();
        xPath.setNamespaceContext(new TestNamespaceContext());
    }

    private Object evaluate(Element committedElement, String path) throws XPathExpressionException {
        return xPath.evaluate(path, committedElement, XPathConstants.STRING);
    }

    public void testTypeCacheInvalidate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test1_original.xml", "metadata1.xsd");
        assertNull(source.getLastInvalidatedTypeCache());

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        assertTrue(committer.hasSaved());

        session = SaverSession.newSession(source);
        session.invalidateTypeCache("DStar");
        session.end(committer);
        assertEquals("DStar", source.getLastInvalidatedTypeCache());

        String conceptName = "crossreferencing";
        session.invalidateTypeCache(conceptName);
        assertNull(source.getSchemasAsString().get(conceptName));
        session.getSaverSource().getSchema(conceptName);
        assertEquals("testdata", source.getSchemasAsString().get(conceptName));
    }

    public void testValidationWithXSINamespace() throws Exception {
        InputStream contractXML = DocumentSaveTest.class.getResourceAsStream("contract.xml");
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document contract = new SkipAttributeDocumentBuilder(documentBuilderFactory.newDocumentBuilder(), true)
                .parse(contractXML);

        XmlSchemaValidator validator = new XmlSchemaValidator("", DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"),
                Validator.NO_OP_VALIDATOR);
        validator.validate(contract.getDocumentElement());
    }

    public void testCreate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, false, "test1_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Chicago", evaluate(committedElement, "/Agency/Name"));
        assertEquals("Chicago", evaluate(committedElement, "/Agency/City"));
    }

    public void testCreateEntityByStandaloneProcessCallWorkflow() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test57.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "workflow", recordXml, false, true,
                true, true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Chicago", evaluate(committedElement, "/Agency/Name"));
        assertEquals("Chicago", evaluate(committedElement, "/Agency/City"));
    }

    public void testUpdateEntityByStandaloneProcessCallWorkflow() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test57_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test57.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "workflow", recordXml, false, true,
                true, true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Chicago", evaluate(committedElement, "/Agency/Name"));
        assertEquals("Chicago", evaluate(committedElement, "/Agency/City"));
    }

    public void testCreateWithInheritanceType() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, false, "test22_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test22.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, true, true,
                true, true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("", evaluate(committedElement, "/Contract/detail[1]/@xsi:type"));
        assertEquals("", evaluate(committedElement, "/Contract/detail[1]/code"));
    }

    public void testCreateSecurity() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("", evaluate(committedElement, "/Agency/Zip"));
        assertEquals("", evaluate(committedElement, "/Agency/State"));
    }

    public void testCreateWithUUIDOverwrite() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test23.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertNotSame("100", evaluate(committedElement, "/Agency/Id")); // Id is expected to be overwritten in case of
        // creation
        assertEquals(1, saver.getSavedId().length);
        assertNotSame("100", saver.getSavedId()[0]);
    }

    public void testCreateWithUUIDAndAUTOINCIgnore() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("Personne.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Vinci", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "Personne.xsd");
        ((TestSaverSource) source).setUserName("System_Admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("Personne.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Vinci", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertNull(Util.getFirstTextNode(committedElement, "/Personne/Contextes/Contexte/IdContexte"));
        assertEquals("[2]", evaluate(committedElement, "/Personne/TypePersonneFk"));

        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("Personne2.xml");
        context = session.getContextFactory().create("MDM", "Vinci", "Source", recordXml, true, true, true, true, false);
        saver = context.createSaver();
        saver.save(session, context);
        committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        committedElement = committer.getCommittedElement();
        assertNull(Util.getFirstTextNode(committedElement, "/Personne/Contextes/Contexte/IdContexte"));
        assertEquals("[1]", evaluate(committedElement, "/Personne/TypePersonneFk"));

    }

    public void testCreateWithMultiOccurrenceUUID() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("MultiOccurrenceUUID.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("MultiOccurrenceUUID", repository);

        TestSaverSource source = new TestSaverSource(repository, false, "", "MultiOccurrenceUUID.xsd");
        source.setUserName("System_Admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("NewMultiOccurrenceUUID.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "MultiOccurrenceUUID", "Source", recordXml,
                true, true, true, true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());

        Element committedElement = committer.getCommittedElement();

        assertEquals("111", evaluate(committedElement, "/EntityA/Id"));

        assertEquals("lab1", evaluate(committedElement, "/EntityA/nodes/node[1]/label"));
        assertNotNull(evaluate(committedElement, "/EntityA/nodes/node[1]/uuid"));
        assertTrue(evaluate(committedElement, "/EntityA/nodes/node[1]/uuid").toString().length() > 0);

        assertEquals("lab2", evaluate(committedElement, "/EntityA/nodes/node[2]/label"));
        assertNotNull(evaluate(committedElement, "/EntityA/nodes/node[2]/uuid"));
        assertTrue(evaluate(committedElement, "/EntityA/nodes/node[2]/uuid").toString().length() > 0);

        assertEquals("lab3", evaluate(committedElement, "/EntityA/nodes/node[3]/label"));
        assertNotNull(evaluate(committedElement, "/EntityA/nodes/node[3]/uuid"));
        assertTrue(evaluate(committedElement, "/EntityA/nodes/node[3]/uuid").toString().length() > 0);
    }

    public void testReplaceWithUUIDOverwrite() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test23_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test23.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        // Id is expected to be overwritten in case of creation
        assertEquals("100", evaluate(committedElement, "/Agency/Id"));
        // Id is expected to be overwritten in case of creation
        assertEquals("http://www.newSite2.org", evaluate(committedElement, "/Agency/Information/MoreInfo[2]"));
        assertEquals(1, saver.getSavedId().length);
        assertEquals("100", saver.getSavedId()[0]);
    }

    public void testCreateWithAutoIncrementOverwrite() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, false, "", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test24.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        assertFalse(source.hasSavedAutoIncrement());
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(source.hasSavedAutoIncrement());
        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        // Id is expected to be overwritten in case of creation
        assertNotSame("100", evaluate(committedElement, "/ProductFamily/Id"));
    }

    public void testCreateFailure() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test10.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        try {
            saver.save(session, context);
            fail();
        } catch (SaveException e) {
            assertTrue(e.getBeforeSavingMessage().isEmpty());
        }
    }

    public void testCreateWithForeignKeyType() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata9.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "metadata9.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test26.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("16.99", evaluate(committedElement, "/Product/Price"));
        assertEquals("ProductFamily", evaluate(committedElement, "/Product/Family/@tmdm:type"));
    }

    public void testReplaceWithForeignKeyType() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata9.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test26_original.xml", "metadata9.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test26.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("16.99", evaluate(committedElement, "/Product/Price"));
    }

    public void testPartialUpdate() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test1_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream partialUpdateContent = new ByteArrayInputStream(
                ("<Agency>\n" + "    <Id>5258f292-5670-473b-bc01-8b63434682f3</Id>\n" + "    <Information>\n"
                        + "        <MoreInfo>http://www.mynewsite.fr</MoreInfo>\n" + "    </Information>\n" + "</Agency>\n")
                        .getBytes("UTF-8"));
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "DStar", "Source",
                partialUpdateContent, true, true, "/Agency/Information/MoreInfo", "", -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("http://www.mynewsite.fr", evaluate(committedElement, "/Agency/Information/MoreInfo[1]"));
    }

    public void testPartialUpdateWithOverwrite() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test1_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream partialUpdateContent = new ByteArrayInputStream(
                ("<Agency>\n" + "    <Id>5258f292-5670-473b-bc01-8b63434682f3</Id>\n" + "    <Information>\n"
                        + "        <MoreInfo>http://www.mynewsite.fr</MoreInfo>\n" + "    </Information>\n" + "</Agency>\n")
                        .getBytes("UTF-8"));
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "DStar", "Source",
                partialUpdateContent, true, true, "/", "/", -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("http://www.mynewsite.fr", evaluate(committedElement, "/Agency/Information/MoreInfo[1]"));
        assertEquals("", evaluate(committedElement, "/Agency/Information/MoreInfo[2]"));
    }

    public void testUpdate() throws Exception {
        // TODO Test for modification of id (this test modifies id but this is intentional).
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test1_original.xml", "metadata1.xsd");
        source.setUserName("System_Admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Chicago", evaluate(committedElement, "/Agency/Name"));
        assertEquals("Chicago", evaluate(committedElement, "/Agency/City"));
        assertEquals("", evaluate(committedElement, "/Agency/State"));
    }

    public void testUpdateOnNonExisting() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        try {
            saver.save(session, context);
            fail("Expected an exception (update on a record that does not exist.");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testUpdateConf() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test1_original.xml", "metadata1.xsd");
        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertFalse(source.hasCalledInitAutoIncrement);

        repository.load(DocumentSaveTest.class.getResourceAsStream("CONF.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("CONF", repository);

        source = new TestSaverSource(repository, true, "test_conf_original.xml", "CONF.xsd");
        source.setUserName("admin");

        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("test_conf.xml");
        context = session.getContextFactory().create("MDM", "CONF", false, recordXml);
        saver = context.createSaver();
        saver.save(session, context);
        committer = new MockCommitter();
        session.end(committer);

        Element committedElement = committer.getCommittedElement();
        assertEquals("<AutoIncrement>" + "<id>AutoIncrement</id>" + "<entry>"
                + "<key>[HEAD].CoreTestsContainer.auto_increment.auto_increment</key>" + "<value>1</value>" + "</entry>"
                + "<entry>" + "<key>[HEAD].Product.ProductFamily.Id</key>" + "<value>30</value>" + "</entry>" + "<entry>"
                + "<key>[HEAD].CoreTestsContainer.auto_increment1.auto_increment1</key>" + "<value>1</value>" + "</entry>"
                + "</AutoIncrement>", Util.nodeToString(committedElement, true, false));
        assertTrue(source.hasCalledInitAutoIncrement);

    }

    public void testWithClone() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, true, "test13_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test13.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("ContractDetailSubType", evaluate(committedElement, "/Contract/detail[1]/@xsi:type"));
        assertEquals("ContractDetailSubType", evaluate(committedElement, "/Contract/detail[2]/@xsi:type"));
        assertEquals("sdfsdf", evaluate(committedElement, "/Contract/detail[2]/code"));
        assertEquals("sdfsdf", evaluate(committedElement, "/Contract/detail[2]/features/actor"));
    }

    public void testSubclassTypeChange() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, true, "test14_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test14.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("ContractDetailType", evaluate(committedElement, "/Contract/detail[1]/@xsi:type"));
        assertEquals("sdfsdf", evaluate(committedElement, "/Contract/detail[1]/code"));
    }

    public void testSubclassTypeChange2() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, true, "test15_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test15.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("ContractDetailSubType", evaluate(committedElement, "/Contract/detail[1]/@xsi:type"));
        assertEquals("cccccc", evaluate(committedElement, "/Contract/detail[1]/code"));
        assertEquals("sdfsdf", evaluate(committedElement, "/Contract/detail[1]/features/actor"));
    }

    public void testUpdateSequence() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, true, "test16_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test16.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }

    public void testSubclassTypeChange3() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, true, "test16_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test16.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("ContractDetailSubType2", evaluate(committedElement, "/Contract/detail[1]/@xsi:type"));
        assertEquals("sdfsdf", evaluate(committedElement, "/Contract/detail[1]/code"));
    }

    public void testUpdateSecurity() throws Exception {
        // TODO Test for modification of id (this test modifies id but this is intentional).
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test8_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test8.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        Element committedElement = committer.getCommittedElement();
        assertEquals("04102", evaluate(committedElement, "/Agency/Zip"));
        assertEquals("ME", evaluate(committedElement, "/Agency/State"));

        // Test with replace.
        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("test8.xml");
        context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true, false, false);
        saver = context.createSaver();
        saver.save(session, context);
        session.end(committer);
        committedElement = committer.getCommittedElement();
        assertEquals("04102", evaluate(committedElement, "/Agency/Zip"));
        assertEquals("ME", evaluate(committedElement, "/Agency/State"));

        // Test changing user name (and user's roles).
        source.setUserName("admin");
        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("test8.xml");
        context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true, false, false);
        saver = context.createSaver();
        saver.save(session, context);
        session.end(committer);
        committedElement = committer.getCommittedElement();
        assertEquals("10001", evaluate(committedElement, "/Agency/Zip"));
        assertEquals("NY", evaluate(committedElement, "/Agency/State"));
    }

    public void testReplaceSecurity() throws Exception {
        // TODO Test for modification of id (this test modifies id but this is intentional).
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test8_original.xml", "metadata1.xsd");

        // Test with replace.
        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test8.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        Element committedElement = committer.getCommittedElement();
        assertEquals("04102", evaluate(committedElement, "/Agency/Zip"));
        assertEquals("ME", evaluate(committedElement, "/Agency/State"));
    }

    public void testNoUpdate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test4_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test4.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertFalse(committer.hasSaved());
    }

    public void testLegacyUpdate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test5_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test5.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Description", evaluate(committedElement, "/Product/Description"));
        assertEquals("60", evaluate(committedElement, "/Product/Price"));
        assertEquals("Lemon", evaluate(committedElement, "/Product/Features/Colors/Color[1]"));
        assertEquals("Light Blue", evaluate(committedElement, "/Product/Features/Colors/Color[2]"));
    }

    public void testLegacyUpdate2() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test6_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test6.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Description", evaluate(committedElement, "/Product/Description"));
        assertEquals("60", evaluate(committedElement, "/Product/Price"));
        assertEquals("Lemon", evaluate(committedElement, "/Product/Features/Colors/Color[1]"));
        assertEquals("Light Blue", evaluate(committedElement, "/Product/Features/Colors/Color[2]"));
    }

    public void testLegacyUpdate3() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test7_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test7.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("60", evaluate(committedElement, "/Product/Price"));
    }

    public void testSchematronValidation() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test9_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test9.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("JohDo", evaluate(committedElement, "/Agent/Id"));
        assertEquals("John", evaluate(committedElement, "/Agent/Firstname"));
        assertEquals("Doe", evaluate(committedElement, "/Agent/Lastname"));
        assertEquals("1", evaluate(committedElement, "/Agent/CommissionCode"));
        assertEquals("2010-01-01", evaluate(committedElement, "/Agent/StartDate"));
    }

    public void testSystemUpdate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("updateReport.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("UpdateReport", repository);

        SaverSource source = new TestSaverSource(repository, true, "test3_original.xml", "updateReport.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test3.xml");
        DocumentSaverContext context = session.getContextFactory().create("UpdateReport", "UpdateReport", "Source", recordXml,
                false, true, true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals(UpdateReportPOJO.OPERATION_TYPE_LOGICAL_DELETE, evaluate(committedElement, "/Update/OperationType"));
    }

    public void testSystemCreate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("updateReport.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("UpdateReport", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "updateReport.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test3.xml");
        DocumentSaverContext context = session.getContextFactory().create("UpdateReport", "UpdateReport", "Source", recordXml,
                true, true, false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals(UpdateReportPOJO.OPERATION_TYPE_LOGICAL_DELETE, evaluate(committedElement, "/Update/OperationType"));
    }

    public void testUpdateReportChangeToSubType() throws Exception {

        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, true, "test63_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test63.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("ContractDetailSubType", evaluate(committedElement, "/Contract/detail[1]/@xsi:type"));
        assertEquals("code-new", evaluate(committedElement, "/Contract/detail[1]/code"));

        // test update report
        MutableDocument updateReportDocument = context.getUpdateReportDocument();
        assertNotNull(updateReportDocument);
        Document doc = updateReportDocument.asDOM();
        String path = (String) evaluate(doc.getDocumentElement(), "Item[1]/path");
        String oldValue = (String) evaluate(doc.getDocumentElement(), "Item[1]/oldValue");
        String newValue = (String) evaluate(doc.getDocumentElement(), "Item[1]/newValue");
        assertEquals("comment[1]", path);
        assertEquals("comment-original", oldValue);
        assertEquals("comment-new", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[2]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[2]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[2]/newValue");
        assertEquals("detail[1]/@xsi:type", path);
        assertEquals("ContractDetailType", oldValue);
        assertEquals("ContractDetailSubType", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[3]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[3]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[3]/newValue");
        assertEquals("detail[1]/code", path);
        assertEquals("code-original", oldValue);
        assertEquals("code-new", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[4]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[4]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[4]/newValue");
        assertEquals("detail[1]/features/actor", path);
        assertEquals("", oldValue);
        assertEquals("actor-new", newValue);
    }

    public void testUpdateReportChangeToSuperTypeAction() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, true, "test64_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);

        InputStream recordXml2 = DocumentSaveTest.class.getResourceAsStream("test64.xml");
        DocumentSaverContext context2 = session.getContextFactory().create("MDM", "Contract", "Source", recordXml2, false, true,
                true, false, false);
        DocumentSaver saver2 = context2.createSaver();
        saver2.save(session, context2);
        MockCommitter committer2 = new MockCommitter();
        session.end(committer2);

        assertTrue(committer2.hasSaved());
        Element committedElement2 = committer2.getCommittedElement();
        assertEquals("ContractDetailType", evaluate(committedElement2, "/Contract/detail[1]/@xsi:type"));
        assertEquals("code-new", evaluate(committedElement2, "/Contract/detail[1]/code"));

        // test update report
        MutableDocument updateReportDocument2 = context2.getUpdateReportDocument();
        assertNotNull(updateReportDocument2);
        Document doc = updateReportDocument2.asDOM();
        String path = (String) evaluate(doc.getDocumentElement(), "Item[1]/path");
        String oldValue = (String) evaluate(doc.getDocumentElement(), "Item[1]/oldValue");
        String newValue = (String) evaluate(doc.getDocumentElement(), "Item[1]/newValue");
        assertEquals("comment[1]", path);
        assertEquals("comment-original", oldValue);
        assertEquals("comment-new", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[2]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[2]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[2]/newValue");
        assertEquals("detail[1]/features/boolValue", path);
        assertEquals("true", oldValue);
        assertEquals("null", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[3]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[3]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[3]/newValue");
        assertEquals("detail[1]/ReadOnlyEle", path);
        assertEquals("[readOnlyEle-original]", oldValue);
        assertEquals("null", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[4]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[4]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[4]/newValue");
        assertEquals("detail[1]/boolTest", path);
        assertEquals("true", oldValue);
        assertEquals("null", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[5]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[5]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[5]/newValue");
        assertEquals("detail[1]/features/vendor", path);
        assertEquals("[vendor-original]", oldValue);
        assertEquals("null", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[6]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[6]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[6]/newValue");
        assertEquals("detail[1]/features/actor", path);
        assertEquals("actor-original", oldValue);
        assertEquals("null", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[7]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[7]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[7]/newValue");
        assertEquals("detail[1]/features", path);
        assertEquals("", oldValue);
        assertEquals("null", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[8]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[8]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[8]/newValue");
        assertEquals("detail[1]/@xsi:type", path);
        assertEquals("ContractDetailSubType", oldValue);
        assertEquals("ContractDetailType", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[9]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[9]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[9]/newValue");
        assertEquals("detail[1]/code", path);
        assertEquals("code-original", oldValue);
        assertEquals("code-new", newValue);

    }

    public void testUpdateFKToSuperType() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata18.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("FKChangeTest", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test66_original.xml", "metadata18.xsd");
        source.setUserName("administrator");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test66.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "FKChangeTest", "Source", recordXml, false,
                true, true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }
        
    public void testProductUpdate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test2_original.xml", "metadata1.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test2.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("60", evaluate(committedElement, "/Product/Price"));
        assertEquals("TT", evaluate(committedElement, "/Product/Description"));
        assertEquals("New product name", evaluate(committedElement, "/Product/Name"));
    }

    public void testProductUpdate2() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test11_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test11.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("222", evaluate(committedElement, "/Product/Price"));
        assertEquals("Small", evaluate(committedElement, "/Product/Features/Sizes/Size[1]"));
        assertEquals("", evaluate(committedElement, "/Product/Features/Sizes/Size[2]"));
        assertEquals("", evaluate(committedElement, "/Product/Features/Sizes/Size[3]"));
        assertEquals("White", evaluate(committedElement, "/Product/Features/Colors/Color[1]"));
        assertEquals("", evaluate(committedElement, "/Product/Features/Colors/Color[2]"));
        assertEquals("", evaluate(committedElement, "/Product/Features/Colors/Color[3]"));
    }

    public void testProductPartialUpdate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test11_original.xml", "metadata1.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream partialUpdateContent = new ByteArrayInputStream(("<Product>\n" + "    <Id>1</Id>\n" + "    <Features>\n"
                + "        <Colors>" + "           <Color>Light Pink</Color>\n" + "        </Colors>\n" + "    </Features>\n"
                + "</Product>\n").getBytes("UTF-8"));
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "DStar", "Source",
                partialUpdateContent, true, false, "/Product/Features/Colors/Color", "", -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals("Light Pink", evaluate(committer.getCommittedElement(), "/Product/Features/Colors/Color[4]"));
    }

    public void testProductPartialUpdateWithIndex() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test11_original.xml", "metadata1.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream partialUpdateContent = new ByteArrayInputStream(("<Product>\n" + "    <Id>1</Id>\n" + "    <Features>\n"
                + "        <Colors>" + "           <Color>Light Pink</Color>\n" + "        </Colors>\n" + "    </Features>\n"
                + "</Product>\n").getBytes("UTF-8"));
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "DStar", "Source",
                partialUpdateContent, true, false, "/Product/Features/Colors/Color", "", 1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals("Light Pink", evaluate(committer.getCommittedElement(), "/Product/Features/Colors/Color[1]"));
        assertEquals("Khaki", evaluate(committer.getCommittedElement(), "/Product/Features/Colors/Color[4]"));
    }

    public void testProductPartialUpdate2() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test26_original.xml", "metadata1.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream partialUpdateContent = new ByteArrayInputStream(("<Product>\n" + "    <Id>1</Id>\n" + "    <Features>\n"
                + "        <Colors>" + "           <Color>Light Pink</Color>\n" + "        </Colors>\n" + "    </Features>\n"
                + "</Product>\n").getBytes("UTF-8"));
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "DStar", "Source",
                partialUpdateContent, true, true, "/Product/Features/Colors/Color", "", -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals("Light Pink", evaluate(committer.getCommittedElement(), "/Product/Features/Colors/Color[1]"));
    }

    public void testInheritance() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata2.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test12_original.xml", "metadata2.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test12.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertFalse(committer.hasSaved()); // Should be true once there are actual changes in test12.xml
    }

    public void testBeforeSavingWithAlterRecord() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        //
        boolean isOK = true;
        boolean newOutput = true;
        SaverSource source = new AlterRecordTestSaverSource(repository, false, "test1_original.xml", isOK, newOutput);

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        assertEquals("change the value successfully!", saver.getBeforeSavingMessage());
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        assertTrue(committer.hasSaved());

        //
        isOK = false;
        newOutput = true;
        source = new AlterRecordTestSaverSource(repository, false, "test1_original.xml", isOK, newOutput);

        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true, true, false);
        saver = context.createSaver();
        try {
            saver.save(session, context);
            fail("Expected an exception.");
        } catch (SaveException e) {
            // Expected
            assertEquals("change the value failed!", e.getBeforeSavingMessage());
        }
        committer = new MockCommitter();
        session.end(committer);
        assertFalse(committer.hasSaved());

        //
        isOK = false;
        newOutput = false;
        source = new AlterRecordTestSaverSource(repository, false, "test1_original.xml", isOK, newOutput);

        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true, true, false);
        saver = context.createSaver();
        try {
            saver.save(session, context);
            fail("Expected an exception.");
        } catch (SaveException e) {
            // Expected
            assertEquals("change the value failed!", e.getBeforeSavingMessage());
        }
        committer = new MockCommitter();
        session.end(committer);
        assertFalse(committer.hasSaved());

        //
        isOK = true;
        newOutput = false;
        source = new AlterRecordTestSaverSource(repository, false, "test1_original.xml", isOK, newOutput);

        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true, true, false);
        saver = context.createSaver();
        saver.save(session, context);
        assertEquals("change the value successfully!", saver.getBeforeSavingMessage());
        committer = new MockCommitter();
        session.end(committer);
        assertTrue(committer.hasSaved());

        
    }
    
    public void testUpdateReportAfterBeforeSavingProcess() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);
        
        // Test updateReport
        boolean isOK = true;
        boolean newOutput = true;
        SaverSource source = new AlterRecordTestSaverSource(repository, true, "test1_original.xml", isOK, newOutput);

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        assertEquals("change the value successfully!", saver.getBeforeSavingMessage());

        String lineSeparator = System.getProperty("line.separator");
        StringBuilder expectedUserXmlBuilder = new StringBuilder(
                "<Agency xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        expectedUserXmlBuilder.append(lineSeparator);
        expectedUserXmlBuilder.append("<Id>5258f292-5670-473b-bc01-8b63434682f3</Id>");
        expectedUserXmlBuilder.append(lineSeparator);
        expectedUserXmlBuilder.append("<Name>beforeSaving_Agency</Name>");
        expectedUserXmlBuilder.append(lineSeparator);
        expectedUserXmlBuilder.append("</Agency>");
        expectedUserXmlBuilder.append(lineSeparator);
        String expectedUserXml = expectedUserXmlBuilder.toString();

        assertEquals(expectedUserXml, context.getUserDocument().exportToString());
        MutableDocument updateReportDocument = context.getUpdateReportDocument();
        assertNotNull(updateReportDocument);
        Document doc = updateReportDocument.asDOM();
        String path = (String) evaluate(doc.getDocumentElement(), "Item[1]/path");
        String oldValue = (String) evaluate(doc.getDocumentElement(), "Item[1]/oldValue");
        String newValue = (String) evaluate(doc.getDocumentElement(), "Item[1]/newValue");
        assertEquals("Name", path);
        assertEquals("Portland", oldValue);
        assertEquals("Chicago", newValue);

        path = (String) evaluate(doc.getDocumentElement(), "Item[2]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[2]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[2]/newValue");
        assertEquals("City", path);
        assertEquals("Portland", oldValue);
        assertEquals("Chicago", newValue);
        
        path = (String) evaluate(doc.getDocumentElement(), "Item[3]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[3]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[3]/newValue");
        assertEquals("Information/MoreInfo[2]", path);
        assertEquals("", oldValue);
        assertEquals("http://www.newSite2.org", newValue);
        
        path = (String) evaluate(doc.getDocumentElement(), "Item[4]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[4]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[4]/newValue");
        assertEquals("Information/MoreInfo[1]", path);
        assertEquals("", oldValue);
        assertEquals("http://www.newSite.org", newValue);
        
        path = (String) evaluate(doc.getDocumentElement(), "Item[5]/path");
        oldValue = (String) evaluate(doc.getDocumentElement(), "Item[5]/oldValue");
        newValue = (String) evaluate(doc.getDocumentElement(), "Item[5]/newValue");
        assertEquals("Name", path);
        assertEquals("Portland", oldValue);
        assertEquals("beforeSaving_Agency", newValue);
        
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        assertTrue(committer.hasSaved());
    }

    public void testBeforeSavingWithAutoIncrementPkRecord() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        boolean isOK = true;
        boolean newOutput = true;
        TestSaverSource source = new TestSaverSourceWithOutputReportItem(repository, false, "", isOK, newOutput);

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("autoIncrementPK.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        assertEquals("Save the value successfully!", saver.getBeforeSavingMessage());
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(source.hasSavedAutoIncrement());
        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("0", evaluate(committedElement, "/ProductFamily/Id"));
    }

    public void testCreatePerformance() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, false, "test1_original.xml", "metadata1.xsd");
        source.setUserName("admin");
        {
            SaverSession session = SaverSession.newSession(source);
            {
                for (int i = 0; i < 10; i++) {
                    InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
                    DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true,
                            true, true, false, false);
                    DocumentSaver saver = context.createSaver();
                    saver.save(session, context);
                }
            }
            session.end(new MockCommitter());
        }

        long saveTime = System.nanoTime();
        {
            SaverSession session = SaverSession.newSession(source);
            {
                for (int i = 0; i < 200; i++) {
                    InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
                    DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true,
                            true, true, false, false);
                    DocumentSaver saver = context.createSaver();
                    saver.save(session, context);
                }
            }
            session.end(new MockCommitter());
        }
        LOG.info("Time (mean): " + (System.nanoTime() - saveTime) / 200f / 1000f / 1000f + " ms.");
    }

    public void testUpdatePerformance() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test1_original.xml", "metadata1.xsd");
        source.setUserName("admin");
        {
            SaverSession session = SaverSession.newSession(source);
            {
                for (int i = 0; i < 10; i++) {
                    InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
                    DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false,
                            true, true, false, false);
                    DocumentSaver saver = context.createSaver();
                    saver.save(session, context);
                }
            }
            session.end(new MockCommitter());
        }

        long saveTime = System.currentTimeMillis();
        long max = 0;
        long min = Long.MAX_VALUE;
        {
            SaverSession session = SaverSession.newSession(source);
            {
                for (int i = 0; i < 200; i++) {
                    long singleExecTime = System.currentTimeMillis();
                    {
                        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
                        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml,
                                false, true, true, false, false);
                        DocumentSaver saver = context.createSaver();
                        saver.save(session, context);
                    }
                    singleExecTime = (System.currentTimeMillis() - singleExecTime);
                    if (singleExecTime > max) {
                        max = singleExecTime;
                    }
                    if (singleExecTime < min) {
                        min = singleExecTime;
                    }
                }
            }
            session.end(new MockCommitter());
        }
        LOG.info("Time (mean): " + (System.currentTimeMillis() - saveTime) / 200f + " ms.");
        LOG.info("Time (min): " + min);
        LOG.info("Time (max): " + max);
    }

    public void testConcurrentUpdates() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        SaverSource source = new TestSaverSource(repository, true, "test1_original.xml", "metadata1.xsd");
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        Set<UpdateRunnable> updateRunnables = new HashSet<UpdateRunnable>();
        for (int i = 0; i < 10; i++) {
            updateRunnables.add(new UpdateRunnable(source));
        }
        Set<Thread> updateThreads = new HashSet<Thread>();
        for (Runnable updateRunnable : updateRunnables) {
            updateThreads.add(new Thread(updateRunnable));
        }
        for (Thread updateThread : updateThreads) {
            updateThread.start();
        }
        for (Thread updateThread : updateThreads) {
            updateThread.join();
        }

        for (UpdateRunnable updateRunnable : updateRunnables) {
            assertTrue(updateRunnable.isSuccess());
        }
    }

    public void test18() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test18.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, true, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }

    public void test19() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        SaverSource source = new TestSaverSource(repository, true, "test19_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test19.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("my code", evaluate(committedElement, "/Contract/detail/code"));
    }

    public void testAddComplexElementToSequence() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata4.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Artikel", repository);

        SaverSource source = new TestSaverSource(repository, true, "test20_original.xml", "metadata4.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test20.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Artikel", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        Element committedElement = committer.getCommittedElement();
        assertEquals("444", evaluate(committedElement, "/testImport/KeyMapping/Keys[4]/Key"));
        assertEquals("444", evaluate(committedElement, "/testImport/KeyMapping/Keys[4]/System"));
        assertEquals("333", evaluate(committedElement, "/testImport/KeyMapping/Keys[3]/Key"));
        assertEquals("333", evaluate(committedElement, "/testImport/KeyMapping/Keys[3]/System"));
        assertEquals("222", evaluate(committedElement, "/testImport/KeyMapping/Keys[2]/Key"));
        assertEquals("222", evaluate(committedElement, "/testImport/KeyMapping/Keys[2]/System"));
        assertEquals("111", evaluate(committedElement, "/testImport/KeyMapping/Keys[1]/Key"));
        assertEquals("111", evaluate(committedElement, "/testImport/KeyMapping/Keys[1]/System"));
    }

    public void testRemoveComplexElementFromSequence() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata4.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Artikel", repository);

        SaverSource source = new TestSaverSource(repository, true, "test21_original.xml", "metadata4.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test21.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Artikel", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("", evaluate(committedElement, "/testImport/KeyMapping/Keys[3]/Key"));
        assertEquals("", evaluate(committedElement, "/testImport/KeyMapping/Keys[3]/System"));
        assertEquals("222", evaluate(committedElement, "/testImport/KeyMapping/Keys[2]/Key"));
        assertEquals("222", evaluate(committedElement, "/testImport/KeyMapping/Keys[2]/System"));
        assertEquals("111", evaluate(committedElement, "/testImport/KeyMapping/Keys[1]/Key"));
        assertEquals("111", evaluate(committedElement, "/testImport/KeyMapping/Keys[1]/System"));
    }

    public void test25() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata3.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contract", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test25_original.xml", "metadata3.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test25.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contract", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }

    public void test27() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata6.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("OM5", repository);

        SaverSource source = new TestSaverSource(repository, true, "test27_original.xml", "metadata6.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test27.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "OM5", "BusinessFunction", recordXml, false,
                true, false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }

    public void test28() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test28", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test28_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test28.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test28", "admin", recordXml, true,
                false, "/Organisation/Contacts/Contact", // Loop (Pivot)
                "SpecialisationContactType/NatureLocalisationFk", // Key
                -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals(
                "40-142",
                evaluate(committedElement,
                        "/Organisation/Contacts/Contact[1]/SpecialisationContactType/AdressePostale/CodePostal"));
        assertEquals("Test",
                evaluate(committedElement, "/Organisation/Contacts/Contact[1]/SpecialisationContactType/AdressePostale/Ville"));
        assertEquals("Test",
                evaluate(committedElement, "/Organisation/Contacts/Contact[1]/SpecialisationContactType/AdressePostale/Region"));
        assertEquals(
                "40-143",
                evaluate(committedElement,
                        "/Organisation/Contacts/Contact[2]/SpecialisationContactType/AdressePostale/CodePostal"));
        assertEquals("Katowice1",
                evaluate(committedElement, "/Organisation/Contacts/Contact[2]/SpecialisationContactType/AdressePostale/Ville"));
        assertEquals("Slaskie1",
                evaluate(committedElement, "/Organisation/Contacts/Contact[2]/SpecialisationContactType/AdressePostale/Region"));
    }

    public void test29() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        // there's a small change in this model that differs from QA: ThirdEntity is a xsd:sequence (not a xsd:all)
        // so order of elements will be tested during XSD validation.
        repository.load(DocumentSaveTest.class.getResourceAsStream("CoreTestsModel.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test29_original.xml", "CoreTestsModel.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test29.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "Test", "Source", recordXml, false, true,
                false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Test", evaluate(committedElement, "/ThirdEntity/optionalDetails/mandatory1"));
        assertEquals("123", evaluate(committedElement, "/ThirdEntity/mandatoryDetails/mandatoryUbounded4"));
    }

    public void test30() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test28", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test30_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test30.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test28", "admin", recordXml, true,
                false, "/Organisation/Contacts/Contact", "SpecialisationContactType/NatureLocalisationFk", -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals(
                "[6]",
                evaluate(committer.getCommittedElement(),
                        "/Organisation/Contacts/Contact[1]/SpecialisationContactType/NatureLocalisationFk"));
        assertEquals(
                "[7]",
                evaluate(committer.getCommittedElement(),
                        "/Organisation/Contacts/Contact[2]/SpecialisationContactType/NatureLocalisationFk"));
    }

    public void test31() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test31", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test31_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test31.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test31", "admin", recordXml, true,
                true, "/Societe/Contacts/Contact/", "/StatutContactFk", -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals(
                "[5]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[1]/SpecialisationContactType/NatureTelephoneFk"));
        assertEquals("", evaluate(committer.getCommittedElement(), "/Societe/Contacts/Contact[2]"));
    }

    public void test32() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test32", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test32_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test32.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test32", "admin", recordXml, true,
                false, "/Societe/Contacts/Contact", "SpecialisationContactType/NatureLocalisationFk", -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals(
                "[1]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[1]/SpecialisationContactType/NatureTelephoneFk"));
        assertEquals(
                "[1]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[2]/SpecialisationContactType/NatureTelephoneFk"));
        assertEquals(
                "[3]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[3]/SpecialisationContactType/NatureLocalisationFk"));
    }

    public void test33() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test33", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test33_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test32.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test33", "admin", recordXml, true,
                false, "/Societe/Contacts/Contact", "SpecialisationContactType/NatureLocalisationFk", -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals(
                "[1]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[1]/SpecialisationContactType/NatureTelephoneFk"));
        assertEquals(
                "[1]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[2]/SpecialisationContactType/NatureTelephoneFk"));
        assertEquals(
                "[3]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[3]/SpecialisationContactType/NatureLocalisationFk"));
        assertEquals(
                "[3]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[4]/SpecialisationContactType/NatureLocalisationFk"));
    }

    public void test34() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test34", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test34_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test34.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test34", "admin", recordXml, true,
                true, "/Organisation/Contacts/Contact", "SpecialisationContactType/NatureLocalisationFk", -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals(
                "[5]",
                evaluate(committer.getCommittedElement(),
                        "/Organisation/Contacts/Contact[1]/SpecialisationContactType/NatureLocalisationFk"));
        assertEquals(
                "Apartado 111 - Abrunheira",
                evaluate(committer.getCommittedElement(),
                        "/Organisation/Contacts/Contact[1]/SpecialisationContactType/AdressePostale/Adresse1"));
    }

    public void test36() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test36", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test36_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test36.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test36", "admin", recordXml, true,
                false, "/Societe/Contacts/Contact", "SpecialisationContactType/NatureLocalisationFk", -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals(
                "[1]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[1]/SpecialisationContactType/NatureTelephoneFk"));
        assertEquals(
                "[3]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[2]/SpecialisationContactType/NatureLocalisationFk"));
        assertEquals(
                "[4]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[3]/SpecialisationContactType/NatureLocalisationFk"));
    }

    public void test37() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test37", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test37_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test37.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test37", "admin", recordXml, true,
                true, "/Societe/Contacts/Contact", "SpecialisationContactType/NatureLocalisationFk", -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        // overwrite = true
        assertEquals(
                "[3]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[1]/SpecialisationContactType/NatureLocalisationFk"));
        assertEquals(
                "92501",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[1]/SpecialisationContactType/AdressePostale/CodePostal"));
        assertEquals(
                "[4]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[2]/SpecialisationContactType/NatureLocalisationFk"));

        // overwrite = false
        source = new TestSaverSource(repository, true, "test37_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("test37.xml");
        context = session.getContextFactory().createPartialUpdate("MDM", "Test37", "admin", recordXml, true, false,
                "/Societe/Contacts/Contact", "SpecialisationContactType/NatureLocalisationFk", -1, false);
        saver = context.createSaver();
        saver.save(session, context);
        committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals(
                "[3]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[1]/SpecialisationContactType/NatureLocalisationFk"));
        assertEquals(
                "92500",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[1]/SpecialisationContactType/AdressePostale/CodePostal"));
        assertEquals(
                "[3]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[2]/SpecialisationContactType/NatureLocalisationFk"));
        assertEquals(
                "92501",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[2]/SpecialisationContactType/AdressePostale/CodePostal"));
        assertEquals(
                "[4]",
                evaluate(committer.getCommittedElement(),
                        "/Societe/Contacts/Contact[3]/SpecialisationContactType/NatureLocalisationFk"));
    }

    public void test38() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test38", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test38_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test38.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test38", "admin", recordXml, true,
                false, "/Societe/Contacts/Contact", "/SpecialisationContactType/NatureTelephoneFk", -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals("", evaluate(committer.getCommittedElement(), "/Societe/Contacts/Contact[8]"));
        assertEquals("+33 0 00 00 00 00",
                evaluate(committer.getCommittedElement(), "/Societe/Contacts/Contact[7]/SpecialisationContactType/Numero"));
        assertEquals("+33 1 47 16 02 03",
                evaluate(committer.getCommittedElement(), "/Societe/Contacts/Contact[5]/SpecialisationContactType/Numero"));
    }

    public void test39() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("CoreTestsModel.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test39", repository);

        TestSaverSource source = new TestSaverSource(repository, false, "", "CoreTestsModel.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test39.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "test39", "Source", recordXml, true, true,
                true, true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertNotNull(evaluate(committedElement, "/ComplexTypes/son1"));
        assertNotNull(evaluate(committedElement, "/ComplexTypes/father1"));
    }

    public void test40() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("CoreTestsModel.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test40", repository);

        TestSaverSource source = new TestSaverSource(repository, false, "test40_original.xml", "CoreTestsModel.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test40.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "test40", "Source", recordXml, true, true,
                false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("1", evaluate(committedElement, "/ComplexTypes/son/Name"));
    }

    public void test42() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test42", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test42_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test42.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("Product", "test42", "Source", recordXml,
                true, false, "Personne/Contextes/Contexte", "", -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("[0000]", evaluate(committedElement, "/Personne/Contextes/Contexte[2]/OrganisationFk"));
    }

    public void test43() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test43", repository);

        TestSaverSource source = new TestSaverSource(repository, false, "", "metadata1.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test43.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "test43", "Source", recordXml, true, true,
                false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("", evaluate(committedElement, "/Product/Features/Colors/Color"));
    }

    public void test44() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata8.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test44", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test44_original.xml", "metadata8.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test44.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "test44", "Source", recordXml, false, true,
                false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("bob", evaluate(committedElement, "/Create_Supplier/Supplier_Name"));
        assertEquals("123456789", evaluate(committedElement, "/Create_Supplier/Company_RegNbr"));
    }

    public void test45() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata8.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test45", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test45_original.xml", "metadata8.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test45.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "test45", "Source", recordXml, false, true,
                false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Purchasing", evaluate(committedElement, "/Create_Supplier/Contact_Details/contact_role"));
        assertEquals("Test", evaluate(committedElement, "/Create_Supplier/Supplier_Address/postal_code"));
    }

    public void testPolymorphismForeignKey() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata10.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Product", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test46_original.xml", "metadata10.xsd");
        source.setUserName("Demo_Manager");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test46.xml");
        DocumentSaverContext context = session.getContextFactory().create("TestFK", "Product", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Company", evaluate(committedElement, "/Product/supplier/@tmdm:type"));
        assertEquals("[company]", evaluate(committedElement, "/Product/supplier"));
    }

    public void test46() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata11.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test47", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test47_original.xml", "metadata11.xsd");
        source.setUserName("administrator");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test47.xml");
        DocumentSaverContext context = session.getContextFactory().create("Eda", "test47", "Source", recordXml, false, true,
                false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }

    public void test47() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata12.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test48", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test48_original.xml", "metadata12.xsd");
        source.setUserName("administrator");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test48.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "test48", "Source", recordXml, false, true,
                false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }

    public void testPolymorphismForeignKeys() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata10.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Product", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test49_original.xml", "metadata10.xsd");
        source.setUserName("Demo_Manager");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test49.xml");
        DocumentSaverContext context = session.getContextFactory().create("TestFK", "Product", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Company", evaluate(committedElement, "/Product/supplier/@tmdm:type"));
        assertEquals("[companyEntity]", evaluate(committedElement, "/Product/supplier"));
    }

    public void testPolymorphismCache() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata10.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Individual", repository);

        TestSaverSource source = new TestSaverSource(repository, false, "", "metadata10.xsd");
        source.setUserName("Demo_User");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test51.xml");
        DocumentSaverContext context = session.getContextFactory().create("TestFK", "Individual", "Source", recordXml, true,
                true, true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }

    public void testPartialUpdateWithEmptyString() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test50", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test50_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test50.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test50", "Source", recordXml,
                true, false, "Personne/Contextes/Contexte", "IdContexte", -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals("", evaluate(committer.getCommittedElement(), "/Personne/Contextes/Contexte[2]/DateFinContexte"));
        assertEquals("[1]", evaluate(committer.getCommittedElement(), "/Personne/Contextes/Contexte[2]/OrganisationFk"));
    }

    public void testSaveWithPolymorphismNode() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("Personne.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Vinci", repository);

        TestSaverSource source = new TestSaverSource(repository, false, "", "Personne.xsd");
        source.setUserName("System_Admin");

        // 1. /Personne/Contextes/Contexte all of nodes are empty(including Polymorphism node:
        // SpecialisationContactType)
        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("Personne.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Vinci", "Source", recordXml, true, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertNull(Util.getFirstTextNode(committedElement,
                "/Personne/Contextes/Contexte/Contacts/Contact/SpecialisationContactType"));
        assertNotNull(evaluate(committedElement, "/Personne/IdMDM"));
        assertEquals("[2]", evaluate(committedElement, "/Personne/TypePersonneFk"));
        assertEquals("3", evaluate(committedElement, "/Personne/NomUsuel"));
        assertEquals("3", evaluate(committedElement, "/Personne/PrenomUsuel"));

        // 2. /Personne/Contextes/Contexte all of nodes are empty(excluding Polymorphism node:
        // SpecialisationContactType=SpecialisationContactEmail)
        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("Personne3.xml");
        context = session.getContextFactory().create("MDM", "Vinci", "Source", recordXml, true, true, true, true, false);
        saver = context.createSaver();
        try {
            saver.save(session, context);
            fail("mandatory nodes must be filled");
        } catch (Exception e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof ValidateException);
        }

        // 3. /Personne/Contextes/Contexte all of mandatory nodes are filled(including Polymorphism node:
        // SpecialisationContactType=SpecialisationContactEmail)
        session = SaverSession.newSession(source);
        recordXml = DocumentSaveTest.class.getResourceAsStream("Personne4.xml");
        context = session.getContextFactory().create("MDM", "Vinci", "Source", recordXml, true, true, true, true, false);
        saver = context.createSaver();
        saver.save(session, context);
        committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        committedElement = committer.getCommittedElement();
        assertEquals(
                "SpecialisationContactEmail",
                evaluate(committedElement,
                        "/Personne/Contextes/Contexte[1]/Contacts/Contact[1]/SpecialisationContactType/@xsi:type"));
        assertEquals("[1]", Util.getFirstTextNode(committedElement,
                "/Personne/Contextes/Contexte/Contacts/Contact/SpecialisationContactType/NatureEmailFk"));
        assertEquals("[1]",
                Util.getFirstTextNode(committedElement, "/Personne/Contextes/Contexte/Contacts/Contact/StatutContactFk"));
        assertEquals("[1]", Util.getFirstTextNode(committedElement, "/Personne/Contextes/Contexte/StatutContexteFk"));
        assertNotNull(evaluate(committedElement, "/Personne/Contextes/Contexte/IdContexte"));
        assertNotNull(evaluate(committedElement, "/Personne/IdMDM"));
        assertEquals("[2]", evaluate(committedElement, "/Personne/TypePersonneFk"));
        assertEquals("3", evaluate(committedElement, "/Personne/NomUsuel"));
        assertEquals("3", evaluate(committedElement, "/Personne/PrenomUsuel"));
    }

    public void test50() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test50", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test50_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test50.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Test50", "Source", recordXml, false, false,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }

    public void test53() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata12.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test53", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test53_original.xml", "metadata12.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test53.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "Test53", "Source", recordXml, false, false,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertNotNull(committer.getCommittedElement());
    }

    public void test58() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata14.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test58", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test58_original.xml", "metadata14.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test58.xml");
        DocumentSaverContext context = session.getContextFactory().create("Product", "Test58", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertNotNull(committedElement);
        assertEquals("", evaluate(committedElement, "/Territory/countries/country_relation[2]/country_id/@tmdm:type"));
    }

    public void test59() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test59", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test59_original.xml", "metadata7.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test59.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("MDM", "Test59", "admin", recordXml, true,
                false, "/Personne/Contextes/Contexte", // Loop (Pivot)
                "IdContexte", // Key
                -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("[7]", evaluate(committedElement, "/Personne/Contextes/Contexte/TypeContexteFk"));
        assertEquals("[8]", evaluate(committedElement, "/Personne/Contextes/Contexte/StatutContexteFk"));
        assertEquals("[9]", evaluate(committedElement, "/Personne/Contextes/Contexte/Contacts/Contact/StatutContactFk"));
        assertEquals(
                "[10]",
                evaluate(committedElement,
                        "/Personne/Contextes/Contexte/Contacts/Contact/SpecialisationContactType/NatureEmailFk"));
    }

    public void test60() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7_vinci.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test60", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test60_original.xml", "metadata7_vinci.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test60.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("Vinci", "Test60", "genericUI", recordXml,
                true, false, "/Societe/ListeEtablissements/CodeOSMOSE", // Loop (Pivot)
                null, // Key
                -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("[10702E0031]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[1]"));
        assertEquals("[10702E0032]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[2]"));
        assertEquals("[10702E0033]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[3]"));
        assertEquals("[10702E0034]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[4]"));
        assertEquals("[10702E0035]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[5]"));

    }

    public void test60Ex1() throws Exception {
        final MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7_vinci.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test60Ex1", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test60_originalEx1.xml", "metadata7_vinci.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test60Ex1.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("Vinci", "Test60Ex1", "genericUI",
                recordXml, true, false, "/Societe/ListeEtablissements/CodeOSMOSE", // Loop (Pivot)
                null, // Key
                -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();

        assertEquals("[10702E0035]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[1]"));
        assertEquals("[10702E0032]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[2]"));
        assertEquals("[10702E0033]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[3]"));
        assertEquals("[10702E0034]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[4]"));
    }

    public void test60Ex2() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata7_vinci.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Test60Ex2", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test60_originalEx2.xml", "metadata7_vinci.xsd");
        source.setUserName("admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test60Ex2.xml");
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("Vinci", "Test60Ex2", "genericUI",
                recordXml, true, false, "/Societe/ListeEtablissements/CodeOSMOSE", // Loop (Pivot)
                null, // Key
                -1, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);
        assertTrue(committer.hasSaved());

        Element committedElement = committer.getCommittedElement();
        assertEquals("[10702E0035]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[1]"));
        assertEquals("[10702E0032]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[2]"));
        assertEquals("[10702E0033]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[3]"));
        assertEquals("[10702E0033]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[4]"));
        assertEquals("[10702E0034]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[5]"));
        assertEquals("[10702E0035]", evaluate(committedElement, "/Societe/ListeEtablissements/CodeOSMOSE[6]"));
    }

    public void testRemoveSimpleTypeNodeWithOccurrence() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test52_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test52.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Description", evaluate(committedElement, "/Product/Description"));
        assertEquals("60", evaluate(committedElement, "/Product/Price"));
        assertEquals("Lemon", evaluate(committedElement, "/Product/Features/Colors/Color[1]"));
        assertEquals("", evaluate(committedElement, "/Product/Features/Colors/Color[2]"));
    }

    public void testUpdateShouldNotResetMultiOccurrenceFieldsDefinedInAnnonymousTypes() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test68_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test68.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Description value", evaluate(committedElement, "/Product/Description"));
        assertEquals("60", evaluate(committedElement, "/Product/Price"));
        assertEquals("Small", evaluate(committedElement, "/Product/Features/Sizes/Size[1]"));
        assertEquals("X-Large", evaluate(committedElement, "/Product/Features/Sizes/Size[2]"));
        assertEquals("Lemon", evaluate(committedElement, "/Product/Features/Colors/Color[1]"));
        assertEquals("Light Blue", evaluate(committedElement, "/Product/Features/Colors/Color[2]"));
    }

    public void testDeleteMultiOccurrenceFieldDefinedInReusableTypes() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata20.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("metadata20.xsd", repository);

        SaverSource source = new TestSaverSource(repository, true, "test69_original.xml", "metadata20.xsd");
        ((TestSaverSource) source).setUserName("System_Admin");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test69.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "metadata20.xsd", "Source", recordXml, false, true, true,
                true, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("", evaluate(committedElement, "/E/a[2]"));
    }

    public void testRemoveComplexTypeNodeWithOccurrence() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata13.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test55_original.xml", "metadata13.xsd");
        ((TestSaverSource) source).setUserName("System_Admin");
        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test55.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, false,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Doggie t-shirt from American Apparel", evaluate(committedElement, "/Product/Description"));
        assertEquals("16.99", evaluate(committedElement, "/Product/Price"));
        assertEquals("Small", evaluate(committedElement, "/Product/Features/Sizes/Size[1]"));
        assertEquals("1", evaluate(committedElement, "/Product/yu[1]/subelement"));
        assertEquals("2", evaluate(committedElement, "/Product/yu[2]/subelement"));
        assertEquals("", evaluate(committedElement, "/Product/yu[3]/subelement"));
    }

    public void testProductFamilyUpdate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata1.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("DStar", repository);

        SaverSource source = new TestSaverSource(repository, true, "test54_original.xml", "metadata1.xsd");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test54.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Description", evaluate(committedElement, "/Product/Description"));
        assertEquals("60", evaluate(committedElement, "/Product/Price"));
        assertEquals("[2]", evaluate(committedElement, "/Product/Family"));
    }

    public void testUserPartialUpdate() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("PROVISIONING.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("PROVISIONING", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test56_original.xml", "PROVISIONING.xsd");
        source.setUserName("admin");

        final MockCommitter committer = new MockCommitter();
        SaverSession session = new SaverSession(source) {

            @Override
            protected Committer getDefaultCommitter() {
                return committer;
            }
        };
        InputStream partialUpdateContent = new ByteArrayInputStream(("<User>\n" + "    <username>user</username>\n"
                + "        <roles>" + "           <role>System_Interactive</role>\n" + " <role>Demo_User</role>\n" + "</roles>\n"
                + "</User>\n").getBytes("UTF-8"));
        DocumentSaverContext context = session.getContextFactory().createPartialUpdate("PROVISIONING", "PROVISIONING", "Source",
                partialUpdateContent, true, false, "/User/roles/role", "", -1, true);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        session.end(committer);

        assertTrue(committer.hasSaved());
        assertEquals("System_Interactive", evaluate(committer.getCommittedElement(), "/User/roles/role[1]"));
        assertEquals("Demo_User", evaluate(committer.getCommittedElement(), "/User/roles/role[2]"));
    }

    public void test61() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata15.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test61", repository);

        SaverSource source = new TestSaverSource(repository, false, null, "metadata15.xsd");
        ((TestSaverSource) source).setUserName("System_Admin");
        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test61_1.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "test61", "Source", recordXml, true, true, true,
                false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("0", evaluate(committedElement, "/Individual/PartyPK"));

        recordXml = DocumentSaveTest.class.getResourceAsStream("test61_2.xml");
        context = session.getContextFactory().create("MDM", "test61", "Source", recordXml, true, true, true, false, false);
        saver = context.createSaver();
        saver.save(session, context);
        committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        committedElement = committer.getCommittedElement();
        assertEquals("1", evaluate(committedElement, "/Company/PartyPK"));

    }

    public void test62() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata11.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contrat", repository);

        SaverSource source = new TestSaverSource(repository, true, "test62_original.xml", "metadata11.xsd");
        ((TestSaverSource) source).setUserName("administrator");
        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test62.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contrat", "Source", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("[40]", evaluate(committedElement, "/Contrat/detailContrat/Perimetre/entitesPresentes/EDAs/EDA/eda"));
    }

    public void test65() throws Exception {
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata11.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("Contrat", repository);

        SaverSource source = new TestSaverSource(repository, false, "", "metadata11.xsd");
        ((TestSaverSource) source).setUserName("administrator");
        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test65.xml");
        DocumentSaverContext context = session.getContextFactory().create("MDM", "Contrat", "Source", recordXml, true, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("", evaluate(committedElement, "/Contrat/detailContrat[@xsi:type]"));
    }

    public void test67_update_multiOccurrenceField_SubType() throws Exception {
        // TMDM-7407: when Subtype has field of Annonymous Type
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata19.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("test", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test67_original.xml", "metadata19.xsd");
        source.setUserName("administrator");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test67.xml");
        DocumentSaverContext context = session.getContextFactory().create("test", "test", "genericUI", recordXml, false, true,
                true, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
    }
    
    public void test68() throws Exception {
        // TMDM-7765: Test for null FK during element's type change.
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata21.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("metadata21.xsd", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test70_original.xml", "metadata21.xsd");
        source.setUserName("administrator");

        SaverSession session = SaverSession.newSession(source);
        InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test70.xml");
        DocumentSaverContext context = session.getContextFactory().create("metadata21.xsd", "metadata21.xsd", "genericUI", recordXml, false, true,
                false, false, false);
        DocumentSaver saver = context.createSaver();
        saver.save(session, context);
        MockCommitter committer = new MockCommitter();
        session.end(committer);

        assertTrue(committer.hasSaved());
        Element committedElement = committer.getCommittedElement();
        assertEquals("Format_Date", evaluate(committedElement, "/EntiteA/format/@xsi:type"));
    }
    
    public void test69() throws Exception {
        // TMDM-7460: test for FK update during element's type change
        MetadataRepository repository = new MetadataRepository();
        repository.load(DocumentSaveTest.class.getResourceAsStream("metadata21.xsd"));
        MockMetadataRepositoryAdmin.INSTANCE.register("metadata21.xsd", repository);

        TestSaverSource source = new TestSaverSource(repository, true, "test71_original.xml", "metadata21.xsd");
        source.setUserName("administrator");
        SaverSession session = SaverSession.newSession(source);

        InputStream recordXml2 = DocumentSaveTest.class.getResourceAsStream("test71.xml");
        DocumentSaverContext context2 = session.getContextFactory().create("metadata21.xsd", "metadata21.xsd", "genericUI", recordXml2, false, false,
                false, false, false);
        DocumentSaver saver2 = context2.createSaver();
        saver2.save(session, context2);
        MockCommitter committer2 = new MockCommitter();
        session.end(committer2);

        assertTrue(committer2.hasSaved());
        Element committedElement2 = committer2.getCommittedElement();
        assertEquals("Format_Entier", evaluate(committedElement2, "/EntiteA/format/@xsi:type"));
        assertEquals("[e1]", evaluate(committedElement2, "/EntiteA/format/CodeUniteMesure"));
        String datarecordXml = context2.getDatabaseDocument().exportToString();        
        assertEquals(datarecordXml, "<EntiteA><codeA>a1</codeA><format xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"Format_Entier\"><CodeUniteMesure>[e1]</CodeUniteMesure></format></EntiteA>");
    }

    private static class MockCommitter implements SaverSession.Committer {

        private MutableDocument lastSaved;

        private boolean hasSaved = false;

        @Override
        public void begin(String dataCluster) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Start on '" + dataCluster + "'");
            }
        }

        @Override
        public void commit(String dataCluster) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Commit on '" + dataCluster + "'");
            }
        }

        @Override
        public void save(com.amalto.core.history.Document item) {
            hasSaved = true;
            lastSaved = (MutableDocument) item;
            if (LOG.isDebugEnabled()) {
                LOG.debug(item.exportToString());
            }
        }

        @Override
        public void delete(com.amalto.core.history.Document document, DeleteType deleteType) {
            // TODO
        }

        @Override
        public void rollback(String dataCluster) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Rollback on '" + dataCluster + "'");
            }
        }

        public Element getCommittedElement() {
            return lastSaved.asDOM().getDocumentElement();
        }

        public boolean hasSaved() {
            return hasSaved;
        }
    }

    private static class TestSaverSource implements SaverSource {

        private final MetadataRepository repository;

        private final boolean exist;

        private final String originalDocumentFileName;

        private MetadataRepository updateReportRepository;

        private String userName = "User";

        private String lastInvalidatedTypeCache;

        private final String schemaFileName;

        private boolean hasSavedAutoIncrement;

        private boolean hasCalledInitAutoIncrement;

        private final Map<String, String> schemasAsString = new HashMap<String, String>();

        private int currentId = 0;

        public TestSaverSource(MetadataRepository repository, boolean exist, String originalDocumentFileName,
                String schemaFileName) {
            this.repository = repository;
            this.exist = exist;
            this.originalDocumentFileName = originalDocumentFileName;
            this.schemaFileName = schemaFileName;
        }

        @Override
        public MutableDocument get(String dataClusterName, String dataModelName, String typeName, String[] key) {
            try {
                ComplexTypeMetadata type = repository.getComplexType(typeName);
                DocumentBuilder documentBuilder;
                documentBuilder = new SkipAttributeDocumentBuilder(SaverContextFactory.DOCUMENT_BUILDER, false);
                Document databaseDomDocument = documentBuilder.parse(DocumentSaveTest.class
                        .getResourceAsStream(originalDocumentFileName));
                Element userXmlElement = getUserXmlElement(databaseDomDocument);
                if (USE_STORAGE_OPTIMIZATIONS) {
                    DataRecordReader<String> reader = new XmlStringDataRecordReader();
                    DataRecord dataRecord = reader.read(repository, type, Util.nodeToString(userXmlElement));
                    return new StorageDocument(dataClusterName, repository, dataRecord);
                } else {
                    return new DOMDocument(userXmlElement, type, dataClusterName, dataClusterName);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static Element getUserXmlElement(Document databaseDomDocument) {
            NodeList userXmlPayloadElement = databaseDomDocument.getElementsByTagName("p"); //$NON-NLS-1$
            if (userXmlPayloadElement.getLength() > 1) {
                throw new IllegalStateException("Document has multiple payload elements.");
            }
            Node current = userXmlPayloadElement.item(0).getFirstChild();
            while (current != null) {
                if (current instanceof Element) {
                    return (Element) current;
                }
                current = current.getNextSibling();
            }
            throw new IllegalStateException("Element 'p' is expected to have an XML element as child.");
        }

        @Override
        public boolean exist(String dataCluster, String dataModelName, String typeName, String[] key) {
            return exist;
        }

        @Override
        public synchronized MetadataRepository getMetadataRepository(String dataModelName) {
            if ("UpdateReport".equals(dataModelName)) {
                if (updateReportRepository == null) {
                    updateReportRepository = new MetadataRepository();
                    updateReportRepository.load(DocumentSaveTest.class.getResourceAsStream("updateReport.xsd"));
                }
                return updateReportRepository;
            }
            return repository;
        }

        @Override
        public InputStream getSchema(String dataModelName) {
            if (schemasAsString.get(dataModelName) == null) {
                String schemaAsString = "testdata";
                schemasAsString.put(dataModelName, schemaAsString);
            }
            return DocumentSaveTest.class.getResourceAsStream(schemaFileName);
        }

        @Override
        public OutputReport invokeBeforeSaving(DocumentSaverContext context, MutableDocument updateReportDocument) {
            String message = "<report><message type=\"info\">change the value successfully!</message></report>";
            return new OutputReport(message, null);
        }

        @Override
        public Set<String> getCurrentUserRoles() {
            if ("User".equals(userName)) {
                return Collections.singleton("User");
            } else {
                return Collections.singleton("System_Admin");
            }
        }

        @Override
        public String getUserName() {
            return userName;
        }

        @Override
        public String getLegitimateUser() {
            return getUserName();
        }

        @Override
        public boolean existCluster(String dataClusterName) {
            return true;
        }

        @Override
        public void resetLocalUsers() {
            // nothing to do
        }

        @Override
        public void initAutoIncrement() {
            hasCalledInitAutoIncrement = true;
        }

        @Override
        public void routeItem(String dataCluster, String typeName, String[] id) {
            // nothing to do
        }

        @Override
        public void invalidateTypeCache(String dataModelName) {
            lastInvalidatedTypeCache = dataModelName;
            schemasAsString.remove(dataModelName);
        }

        @Override
        public void saveAutoIncrement() {
            hasSavedAutoIncrement = true;
        }

        @Override
        public String nextAutoIncrementId(String dataCluster, String dataModel, String conceptName) {
            return String.valueOf(currentId++);
        }

        public boolean hasSavedAutoIncrement() {
            return hasSavedAutoIncrement;
        }

        public String getLastInvalidatedTypeCache() {
            return lastInvalidatedTypeCache;
        }

        public Map<String, String> getSchemasAsString() {
            return schemasAsString;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

    }

    private static class AlterRecordTestSaverSource extends DocumentSaveTest.TestSaverSource {

        private final boolean OK;

        private final boolean newOutput;

        public AlterRecordTestSaverSource(MetadataRepository repository, boolean exist, String fileName, boolean OK,
                boolean newOutput) {
            super(repository, exist, fileName, "metadata1.xsd");
            this.OK = OK;
            this.newOutput = newOutput;
        }

        @Override
        public OutputReport invokeBeforeSaving(DocumentSaverContext context, MutableDocument updateReportDocument) {
            String message = "<report><message type=\"info\">change the value successfully!</message></report>";
            if (!OK) {
                message = "<report><message type=\"error\">change the value failed!</message></report>";
            }
            String item = null;
            OutputReport report = new OutputReport(message, item);

            if (newOutput) {
                item = "<exchange><item>"
                        + "<Agency><Id>5258f292-5670-473b-bc01-8b63434682f3</Id><Name>beforeSaving_Agency</Name></Agency></item></exchange>";
                report.setItem(item);
            }
            return report;
        }
    }

    private static class TestSaverSourceWithOutputReportItem extends DocumentSaveTest.TestSaverSource {

        private final boolean OK;

        private final boolean newOutput;

        public TestSaverSourceWithOutputReportItem(MetadataRepository repository, boolean exist, String fileName, boolean OK,
                boolean newOutput) {
            super(repository, exist, fileName, "metadata1.xsd");
            this.OK = OK;
            this.newOutput = newOutput;
        }

        @Override
        public OutputReport invokeBeforeSaving(DocumentSaverContext context, MutableDocument updateReportDocument) {
            String message = "<report><message type=\"info\">Save the value successfully!</message></report>";
            if (!OK) {
                message = "<report><message type=\"error\">Save the value failed!</message></report>";
            }
            String item = null;
            OutputReport report = new OutputReport(message, item);

            if (newOutput) {
                item = "<exchange><item>" + "<ProductFamily><Name>testAutoIncrementPK</Name></ProductFamily></item></exchange>";
                report.setItem(item);
            }
            return report;
        }
    }

    private static class UpdateRunnable implements Runnable {

        private final SaverSource source;

        private boolean success = false;

        public UpdateRunnable(SaverSource source) {
            this.source = source;
        }

        @Override
        public void run() {
            SaverSession session = SaverSession.newSession(source);
            {
                for (int i = 0; i < 100; i++) {
                    InputStream recordXml = DocumentSaveTest.class.getResourceAsStream("test1.xml");
                    DocumentSaverContext context = session.getContextFactory().create("MDM", "DStar", "Source", recordXml, false,
                            true, true, false, false);
                    DocumentSaver saver = context.createSaver();
                    saver.save(session, context);
                }
            }
            session.end(new MockCommitter());
            success = true;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    private static class TestNamespaceContext implements NamespaceContext {

        private Map<String, String> declaredPrefix = new HashMap<String, String>();

        private TestNamespaceContext() {
            declaredPrefix.put("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            declaredPrefix.put("tmdm", SkipAttributeDocumentBuilder.TALEND_NAMESPACE);
        }

        @Override
        public String getNamespaceURI(String prefix) {
            return declaredPrefix.get(prefix);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            Set<Map.Entry<String, String>> entries = declaredPrefix.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                if (entry.getValue().equals(namespaceURI)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return declaredPrefix.keySet().iterator();
        }
    }

    private static class TestUserDelegator implements SecuredStorage.UserDelegator {

        boolean isActive = true;

        public void setActive(boolean active) {
            isActive = active;
        }

        @Override
        public boolean hide(FieldMetadata field) {
            return isActive && field.getHideUsers().contains("System_Users");
        }

        @Override
        public boolean hide(ComplexTypeMetadata type) {
            return isActive && type.getHideUsers().contains("System_Users");
        }
    }
}
