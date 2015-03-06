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

package com.amalto.core.query;

import static com.amalto.core.query.user.UserQueryBuilder.*;
import static com.amalto.core.query.user.UserStagingQueryBuilder.error;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.amalto.core.query.user.*;
import com.amalto.xmlserver.interfaces.IWhereItem;
import com.amalto.xmlserver.interfaces.WhereAnd;
import com.amalto.xmlserver.interfaces.WhereCondition;
import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;

import com.amalto.core.storage.FullTextResultsWriter;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.exception.FullTextQueryCompositeKeyException;
import com.amalto.core.storage.hibernate.HibernateStorage;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.DataRecordReader;
import com.amalto.core.storage.record.DataRecordWriter;
import com.amalto.core.storage.record.ViewSearchResultsWriter;
import com.amalto.core.storage.record.XmlStringDataRecordReader;

@SuppressWarnings("nls")
public class StorageFullTextTest extends StorageTestCase {

    private static Logger LOG = Logger.getLogger(StorageFullTextTest.class);

    private void populateData() {
        DataRecordReader<String> factory = new XmlStringDataRecordReader();
        List<DataRecord> allRecords = new LinkedList<DataRecord>();
        allRecords.add(factory.read("1", repository, productFamily, "<ProductFamily>\n" + "    <Id>1</Id>\n"
                + "    <Name>Product family #1</Name>\n" + "</ProductFamily>"));
        allRecords.add(factory.read("1", repository, productFamily, "<ProductFamily>\n" + "    <Id>2</Id>\n"
                + "    <Name>Product family #2</Name>\n" + "</ProductFamily>"));
        allRecords.add(factory.read("1", repository, productFamily, "<ProductFamily>\n" + "    <Id>3</Id>\n"
                + "    <Name>Product family #3</Name>\n" + "</ProductFamily>"));
        allRecords.add(factory.read("1", repository, productFamily, "<ProductFamily>\n" + "    <Id>4</Id>\n"
                + "    <Name>Product family #4</Name>\n" + "</ProductFamily>"));
        allRecords.add(factory.read("1", repository, product, "<Product>\n" + "    <Id>1</Id>\n" + "    <Name>talend</Name>\n"
                + "    <ShortDescription>Short description word</ShortDescription>\n"
                + "    <LongDescription>Long description</LongDescription>\n" + "    <Price>10</Price>\n" + "    <Features>\n"
                + "        <Sizes>\n" + "            <Size>Small</Size>\n" + "            <Size>Medium</Size>\n"
                + "            <Size>Large</Size>\n" + "        </Sizes>\n" + "        <Colors>\n"
                + "            <Color>Blue</Color>\n" + "            <Color>Red</Color>\n" + "        </Colors>\n"
                + "    </Features>\n" + "    <Status>Pending</Status>\n" + "    <Supplier>[1]</Supplier>\n" + "</Product>"));
        allRecords.add(factory.read("1", repository, product, "<Product>\n" + "    <Id>2</Id>\n"
                + "    <Name>Renault car</Name>\n" + "    <ShortDescription>A car</ShortDescription>\n"
                + "    <LongDescription>Long description 2</LongDescription>\n" + "    <Price>10</Price>\n" + "    <Features>\n"
                + "        <Sizes>\n" + "            <Size>Large</Size>\n" + "        <Size>Large</Size></Sizes>\n" + "        <Colors>\n"
                + "            <Color>Blue 2</Color>\n" + "            <Color>Blue 1</Color>\n"
                + "            <Color>Klein blue2</Color>\n" + "        </Colors>\n" + "    </Features>\n"
                + "    <Family>[1]</Family>\n" + "    <Status>Pending</Status>\n" + "    <Supplier>[2]</Supplier>\n"
                + "    <Supplier>[1]</Supplier>\n" + "</Product>"));
        allRecords.add(factory.read("1", repository, supplier, "<Supplier>\n" + "    <Id>1</Id>\n"
                + "    <SupplierName>Renault</SupplierName>\n" + "    <Contact>" + "        <Name>Jean Voiture</Name>\n"
                + "        <Phone>33123456789</Phone>\n" + "        <Email>test@test.org</Email>\n" + "    </Contact>\n"
                + "</Supplier>"));
        allRecords.add(factory.read("1", repository, supplier, "<Supplier>\n" + "    <Id>2</Id>\n"
                + "    <SupplierName>Starbucks Talend</SupplierName>\n" + "    <Contact>" + "        <Name>Jean Cafe</Name>\n"
                + "        <Phone>33234567890</Phone>\n" + "        <Email>test@testfactory.org</Email>\n" + "    </Contact>\n"
                + "</Supplier>"));
        allRecords.add(factory.read("1", repository, supplier, "<Supplier>\n" + "    <Id>3</Id>\n"
                + "    <SupplierName>Talend</SupplierName>\n" + "    <Contact>" + "        <Name>Jean Paul</Name>\n"
                + "        <Phone>33234567890</Phone>\n" + "        <Email>test@talend.com</Email>\n" + "    </Contact>\n"
                + "</Supplier>"));
        allRecords.add(factory.read("1", repository, supplier, "<Supplier>\n" + "    <Id>4</Id>\n"
                + "    <SupplierName>IdSoftware</SupplierName>\n" + "    <Contact>" + "        <Name>John Carmack</Name>\n"
                + "        <Phone>123456789</Phone>\n" + "        <Email></Email>\n" + "    </Contact>\n" + "</Supplier>"));
        allRecords
                .add(factory
                        .read("1",
                                repository,
                                country,
                                "<Country><id>1</id><creationDate>2010-10-10</creationDate><creationTime>2010-10-10T00:00:01</creationTime><name>France</name></Country>"));
        try {
            storage.begin();
            storage.update(allRecords);
            storage.commit();
        } catch (Exception e) {
            storage.rollback();
            throw new RuntimeException(e);
        } finally {
            storage.end();
        }
    }

    @Override
    public void tearDown() throws Exception {
        storage.begin();
        {
            UserQueryBuilder qb = from(product);
            storage.delete(qb.getSelect());

            qb = from(productFamily);
            storage.delete(qb.getSelect());

            qb = from(supplier);
            storage.delete(qb.getSelect());

            qb = from(country);
            storage.delete(qb.getSelect());
        }
        storage.commit();
        storage.end();
    }

    @Override
    public void setUp() throws Exception {
        populateData();
        super.setUp();
    }

    public void testMatchesInSameInstance() throws Exception {
        UserQueryBuilder qb = from(country).where(fullText("2010"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            for (DataRecord result : results) {
                LOG.info("result = " + result);
            }
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }

        qb = from(product).where(fullText("Large"));
        results = storage.fetch(qb.getSelect());
        try {
            for (DataRecord result : results) {
                LOG.info("result = " + result);
            }
            assertEquals(2, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearch() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText("Renault"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            for (DataRecord result : results) {
                LOG.info("result = " + result);
            }
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchOrderBy() throws Exception {
        UserQueryBuilder qb = from(supplier).select(supplier.getField("Id")).where(fullText("Talend"))
                .orderBy(supplier.getField("Id"), OrderBy.Direction.ASC);

        StorageResults records = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, records.getCount());
            int currentId = -1;
            for (DataRecord record : records) {
                Integer id = Integer.parseInt((String) record.get("Id"));
                assertTrue(id > currentId);
                currentId = id;
            }
        } finally {
            records.close();
        }

        qb = from(supplier).select(supplier.getField("Id")).where(fullText("Talend"))
                .orderBy(supplier.getField("Id"), OrderBy.Direction.DESC);

        records = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, records.getCount());
            int currentId = Integer.MAX_VALUE;
            for (DataRecord record : records) {
                Integer id = Integer.parseInt((String) record.get("Id"));
                assertTrue(id < currentId);
                currentId = id;
            }
        } finally {
            records.close();
        }
    }
    
    public void testSimpleSearchOrderByWithContainsCondition() throws Exception {
        // Order by "Id" field
        UserQueryBuilder qb = from(productFamily).where(contains(productFamily.getField("Name"), "Product"))
                .orderBy(productFamily.getField("Id"), OrderBy.Direction.DESC);

        StorageResults records = storage.fetch(qb.getSelect());
        try {
            assertEquals(4, records.getCount());
            int currentId = Integer.MAX_VALUE;
            for (DataRecord record : records) {
                Integer id = Integer.parseInt((String) record.get("Id"));
                assertTrue(id < currentId);
                currentId = id;
            }            
        }  finally {
            records.close();
        }
        // Order by "Name" field
        qb = from(productFamily).where(contains(productFamily.getField("Name"), "Product"))
                .orderBy(productFamily.getField("Name"), OrderBy.Direction.DESC);

        records = storage.fetch(qb.getSelect());
        try {
            assertEquals(4, records.getCount());
            int currentId = Integer.MAX_VALUE;
            for (DataRecord record : records) {
                Integer id = Integer.parseInt((String) record.get("Id"));
                assertTrue(id < currentId);
                currentId = id;
            }            
        }  finally {
            records.close();
        }
    }

    public void testMultipleTypesSearch() throws Exception {
        UserQueryBuilder qb = from(supplier).and(product).where(fullText("Renault"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testDateSearch() throws Exception {
        UserQueryBuilder qb = from(country).where(fullText("2010-10-10"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testCollectionSearch() throws Exception {
        UserQueryBuilder qb = from(product).where(fullText("Blue"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithCondition() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText("Renault")).where(
                eq(supplier.getField("Contact/Name"), "Jean Voiture"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }

        qb = from(supplier).where(fullText("Renault")).where(eq(supplier.getField("Id"), "1"));
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }

        qb = from(supplier).where(fullText("Renault")).where(eq(supplier.getField("Id"), "2"));
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithWildcard() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText("*"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(4, results.getCount());
            int actualCount = 0;
            for (DataRecord result : results) {
                actualCount++;
            }
            assertEquals(4, actualCount);
        } finally {
            results.close();
        }
        //
        qb = from(supplier).where(fullText("**"));
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(4, results.getCount());
            int actualCount = 0;
            for (DataRecord result : results) {
                actualCount++;
            }
            assertEquals(4, actualCount);
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithWildcardOnTypes() throws Exception {
        UserQueryBuilder qb = from(supplier).and(product).where(fullText("*"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(6, results.getCount());
            int actualCount = 0;
            for (DataRecord result : results) {
                actualCount++;
            }
            assertEquals(6, actualCount);
        } finally {
            results.close();
        }
        //
        qb = from(supplier).and(product).where(fullText("**"));
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(6, results.getCount());
            int actualCount = 0;
            for (DataRecord result : results) {
                actualCount++;
            }
            assertEquals(6, actualCount);
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithSpace() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText(" "));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(4, results.getCount());
        } finally {
            results.close();
        }

        qb = from(supplier).where(fullText("     "));
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(4, results.getCount());
        } finally {
            results.close();
        }

        qb = from(supplier).and(product).where(fullText("     "));
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(6, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithContainsCondition() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText("Renault"))
                .where(contains(supplier.getField("Contact/Name"), "Jean"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }

        qb = from(supplier).where(fullText("Talend")).where(contains(supplier.getField("Contact/Name"), "Jean"));
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithContainsConditionAndNot() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText("Renault")).where(
                and(contains(supplier.getField("Contact/Name"), "Jean"), not(eq(supplier.getField("Id"), "0"))));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertNotSame(0, result.get("Id"));
            }
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithGreaterThanCondition() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText("Renault")).where(gt(supplier.getField("Id"), "1"));
        try {
            storage.fetch(qb.getSelect());
            fail("Expected: not supported.");
        } catch (Exception e) {
            // Expected.
        }

        qb = from(supplier).where(fullText("Renault")).where(gte(supplier.getField("Id"), "1"));
        try {
            storage.fetch(qb.getSelect());
            fail("Expected: not supported.");
        } catch (Exception e) {
            // Expected.
        }

        qb = from(supplier).where(fullText("Jean")).where(gte(supplier.getField("Id"), "2"));
        try {
            storage.fetch(qb.getSelect());
            fail("Expected: not supported.");
        } catch (Exception e) {
            // Expected.
        }
    }

    public void testSimpleSearchWithContainsAndIsEmptyNullCondition() throws Exception {
        UserQueryBuilder qb = from(supplier).where(
                and(contains(supplier.getField("Id"), "4"),
                        or(isEmpty(supplier.getField("Contact/Email")), isNull(supplier.getField("Contact/Email")))));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }

        qb = from(supplier).where(
                and(contains(supplier.getField("Contact/Name"), "John"),
                        or(isNull(supplier.getField("Contact/Email")), isEmpty(supplier.getField("Contact/Email")))));
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithLessThanCondition() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText("Renault")).where(lt(supplier.getField("Id"), "1"));
        try {
            storage.fetch(qb.getSelect());
            fail("Expected: not supported.");
        } catch (Exception e) {
            // Expected.
        }

        qb = from(supplier).where(fullText("Renault")).where(lte(supplier.getField("Id"), "1"));
        try {
            storage.fetch(qb.getSelect());
            fail("Expected: not supported.");
        } catch (Exception e) {
            // Expected.
        }
    }

    public void testSimpleSearchWithJoin() throws Exception {
        UserQueryBuilder qb = from(product).and(productFamily).selectId(product).select(productFamily.getField("Name"))
                .where(fullText("Renault")).join(product.getField("Family"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertNotNull(result.get("Id"));
                assertEquals("", result.get("Name"));
            }
        } finally {
            results.close();
        }

        qb = from(product).and(productFamily).selectId(product).select(productFamily.getField("Name")).where(fullText("Renault"))
                .join(product.getField("Family")).limit(20);

        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertNotNull(result.get("Id"));
                assertEquals("", result.get("Name"));
            }
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithProjection() throws Exception {
        UserQueryBuilder qb = from(supplier).select(supplier.getField("Contact/Name")).where(fullText("Renault"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertEquals(1, result.getSetFields().size());
                assertNotNull(result.get("Name"));
            }
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithProjectionAlias() throws Exception {
        UserQueryBuilder qb = from(supplier).select(alias(supplier.getField("Contact/Name"), "element")).where(
                fullText("Renault"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertEquals(1, result.getSetFields().size());
                assertNotNull(result.get("element"));
            }
        } finally {
            results.close();
        }

        qb = from(supplier).select(alias(supplier.getField("Contact/Name"), "element")).where(fullText("Renault")).start(0)
                .limit(20);
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertEquals(1, result.getSetFields().size());
                assertNotNull(result.get("element"));
            }
        } finally {
            results.close();
        }
    }

    public void testFKSearchWithProjection() throws Exception {
        UserQueryBuilder qb = from(product).select(product.getField("Family")).where(fullText("car"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            ViewSearchResultsWriter writer = new ViewSearchResultsWriter();
            StringWriter resultWriter = new StringWriter();
            for (DataRecord result : results) {
                writer.write(result, resultWriter);
            }
            assertEquals("<result xmlns:metadata=\"http://www.talend.com/mdm/metadata\" "
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" + "\t<Family>[1]</Family>\n" + "</result>",
                    resultWriter.toString());
        } finally {
            results.close();
        }
    }

    public void testMultipleTypesSearchWithCondition() throws Exception {
        UserQueryBuilder qb = from(supplier).where(fullText("Renault")).where(
                eq(supplier.getField("Contact/Name"), "Jean Voiture"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testFullTextSuggestion() throws Exception {
        try {
            storage.getFullTextSuggestion("Ren", Storage.FullTextSuggestion.START, 3);
            fail("Expected due to Lucene version being used.");
        } catch (Exception e) {
            // Expected.
        }
    }

    public void testFullTextAlternative() throws Exception {
        try {
            storage.getFullTextSuggestion("strabuks", Storage.FullTextSuggestion.ALTERNATE, 3);
            fail("Expected due to Lucene version being used");
        } catch (Exception e) {
            // Expected
        }

    }

    public void testFullTextResultsFormat() throws Exception {
        UserQueryBuilder qb = from(product).where(fullText("Renault"));

        StorageResults results = null;
        try {
            results = storage.fetch(qb.getSelect());
            assertEquals(1, results.getCount());

            DataRecordWriter writer = new FullTextResultsWriter("Renault");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (DataRecord result : results) {
                writer.write(result, output);
            }
            assertTrue(output.toString().contains("<b>Renault car</b>"));
        } finally {
            if (results != null) {
                results.close();
            }
        }
    }

    public void testFullTextResultsInNestedFormat() throws Exception {
        UserQueryBuilder qb = from(product).where(fullText("Klein"));

        StorageResults results = null;
        try {
            results = storage.fetch(qb.getSelect());
            assertEquals(1, results.getCount());

            DataRecordWriter writer = new FullTextResultsWriter("Klein");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (DataRecord result : results) {
                writer.write(result, output);
            }
            assertTrue(output.toString().contains("<b>Klein blue2</b>"));
        } finally {
            if (results != null) {
                results.close();
            }
        }
    }

    public void testNoFullText() throws Exception {
        Storage storage = new HibernateStorage("noFullText");
        try {
            storage.init(getDatasource("RDBMS-1-NO-FT"));
            storage.prepare(repository, Collections.<Expression> emptySet(), false, false);
            UserQueryBuilder qb = from(product).where(fullText("Test"));

            try {
                storage.fetch(qb.getSelect());
                fail("Full text is not enabled");
            } catch (Exception e) {
                assertEquals("Storage 'noFullText' is not configured to support full text queries.", e.getCause().getMessage());
            }
        } finally {
            storage.close();
        }
    }

    public void testSimpleWithoutFkValueSearch() throws Exception {
        UserQueryBuilder qb = from(product).select(product.getField("Family")).where(fullText("talend")).limit(20);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertEquals("", result.get("Family"));
            }
        } finally {
            results.close();
        }
    }

    public void testFullTestWithCompositeKeySearch() throws Exception {
        ComplexTypeMetadata a1 = repository.getComplexType("a1");
        ComplexTypeMetadata a2 = repository.getComplexType("a2");

        DataRecordReader<String> factory = new XmlStringDataRecordReader();
        List<DataRecord> allRecords = new LinkedList<DataRecord>();
        allRecords.add(factory.read("1", repository, a2,
                "<a2><subelement>1</subelement><subelement1>10</subelement1><b3>String b3</b3><b4>String b4</b4></a2>"));
        allRecords.add(factory.read("1", repository, a1,
                "<a1><subelement>1</subelement><subelement1>11</subelement1><b1>String b1</b1><b2>[1][10]</b2></a1>"));

        storage.begin();
        storage.update(allRecords);
        storage.commit();

        try {
            UserQueryBuilder qb = from(a1).selectId(a1).select(a1.getField("b1")).select(a1.getField("b2"))
                    .where(fullText("String")).limit(20);
            storage.begin();
            storage.fetch(qb.getSelect());
            fail();
        } catch (RuntimeException runtimeException) {
            if (FullTextQueryCompositeKeyException.class.isInstance(runtimeException.getCause())) {
                assertEquals("a1", runtimeException.getCause().getMessage());
            } else {
                throw runtimeException;
            }
            storage.rollback();
        }
    }

    public void testFieldFullText() throws Exception {
        UserQueryBuilder qb = from(product).where(fullText(product.getField("ShortDescription"), "description")).limit(20);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }

        qb = from(product).where(fullText(product.getField("ShortDescription"), "long")).limit(20);
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testFullTextAndRangeTimeQuery() throws Exception {
        UserQueryBuilder qb = from(product).where(
                and(fullText(product.getField("ShortDescription"), "description"),
                        and(gte(timestamp(), "0"), lte(timestamp(), String.valueOf(System.currentTimeMillis()))))).limit(20);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testTaskIdProjection() throws Exception {
        UserQueryBuilder qb = from(product).select(alias(taskId(), "taskId")).select(alias(error(), "error"))
                .where(fullText(product.getField("ShortDescription"), "description"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertNull(result.get("taskId"));
                assertNull(result.get("error"));
            }
        } finally {
            results.close();
        }

        qb = from(product).select(alias(taskId(), "taskId")).select(alias(error(), "error"))
                .where(fullText(product.getField("ShortDescription"), "description")).limit(20);
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertNull(result.get("taskId"));
                assertNull(result.get("error"));
            }
        } finally {
            results.close();
        }
    }

    public void testSearchOnContainedType() throws Exception {
        UserQueryBuilder qb = from(product).where(fullText(product.getField("Features"), "klein"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                Object object = result.get("Features/Colors/Color");
                assertTrue(object instanceof List);
                assertEquals(3, ((List) object).size());
                assertEquals("Klein blue2", ((List) object).get(2));
            }
        } finally {
            results.close();
        }

        qb = UserQueryBuilder.from(product);
        String fieldName = "Product/Features";
        IWhereItem item = new WhereAnd(Arrays.<IWhereItem> asList(new WhereCondition(fieldName, WhereCondition.FULLTEXTSEARCH,
                "klein", WhereCondition.NO_OPERATOR)));
        Condition condition = UserQueryHelper.buildCondition(qb, item, repository);
        Expression normalizedCondition = condition.normalize();
        assertTrue(normalizedCondition instanceof BinaryLogicOperator);
        assertTrue(((BinaryLogicOperator) normalizedCondition).getLeft() instanceof FieldFullText);
        qb = qb.where(condition);
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                Object object = result.get("Features/Colors/Color");
                assertTrue(object instanceof List);
                assertEquals(3, ((List) object).size());
                assertEquals("Klein blue2", ((List) object).get(2));
            }
        } finally {
            results.close();
        }
    }

    public void testTimeStampProjectionNoAlias() throws Exception {
        // TMDM-7737: Test metadata field projection *with* paging in query.
        UserQueryBuilder qb = from(product).select(timestamp()).where(fullText(product.getField("Features"), "klein")).start(0).limit(20);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }
}
