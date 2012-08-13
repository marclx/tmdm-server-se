// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.amalto.core.metadata.ComplexTypeMetadata;
import com.amalto.core.query.user.Select;
import com.amalto.core.query.user.UserQueryBuilder;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.DataRecordReader;
import com.amalto.core.storage.record.XmlStringDataRecordReader;

@SuppressWarnings("nls")
public class StorageRecordCreationTest extends StorageTestCase {

    private void populateData() {
        DataRecordReader<String> factory = new XmlStringDataRecordReader();

        List<DataRecord> allRecords = new LinkedList<DataRecord>();
        allRecords
                .add(factory
                        .read(1,
                                repository,
                                country,
                                "<Country><id>1000</id><name>France</name><creationDate>2010-05-10</creationDate><creationTime>2010-05-10T00:00:00</creationTime></Country>"));
        allRecords
                .add(factory
                        .read(1,
                                repository,
                                address,
                                "<Address><Id>1000</Id><Street>Street1</Street><country>[1000]</country><ZipCode>10000</ZipCode><City>City1</City><enterprise>false</enterprise></Address>"));

        try {
            storage.begin();
            storage.update(allRecords);
            storage.commit();
        } finally {
            storage.end();
        }
    }

    @Override
    public void setUp() throws Exception {
        populateData();
    }

    @Override
    public void tearDown() throws Exception {
        try {
            storage.begin();
            {
                UserQueryBuilder qb = from(person);
                storage.delete(qb.getSelect());

                qb = from(address);
                storage.delete(qb.getSelect());

                qb = from(country);
                storage.delete(qb.getSelect());
            }
            storage.commit();
        } finally {
            storage.end();
        }

    }

    public void testInsert() throws Exception {
        UserQueryBuilder qb = from(person).where(eq(person.getField("id"), "1001"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }

        DataRecordReader<String> factory = new XmlStringDataRecordReader();
        DataRecord record = factory
                .read(1,
                        repository,
                        person,
                        "<Person><id>1001</id><lastname>Dupond</lastname><middlename>David</middlename><firstname>Julien</firstname><age>10</age><score>10</score><addresses><address>[1000][false]</address></addresses><age>10</age><Status>Employee</Status></Person>");
        storage.begin();
        try {
            storage.update(Collections.singletonList(record));
            storage.commit();
        } finally {
            storage.end();
        }

        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testFailInsert() throws Exception {
        UserQueryBuilder qb = from(person).where(eq(person.getField("id"), "1002"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }

        DataRecordReader<String> factory = new XmlStringDataRecordReader();
        DataRecord record = factory
                .read(1,
                        repository,
                        person,
                        "<Person><id>1002</id><lastname>Dupond</lastname><middlename>David</middlename><firstname>Julien</firstname><age>10</age><score>10</score><addresses><address>[900][true]</address></addresses><age>10</age><Status>Employee</Status></Person>");
        storage.begin();
        storage.update(Collections.singletonList(record));
        try {
            storage.commit();
            fail("Should have failed due to integrity constraint on address column.");
        } catch (Exception e) {
            // Expected
            storage.rollback();
        } finally {
            storage.end();
        }

        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testFailTransaction() throws Exception {
        UserQueryBuilder failQuery = from(person).where(eq(person.getField("id"), "1002"));
        StorageResults results = storage.fetch(failQuery.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }

        UserQueryBuilder successQuery = from(person).where(eq(person.getField("id"), "1003"));
        results = storage.fetch(successQuery.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }

        DataRecordReader<String> factory = new XmlStringDataRecordReader();
        DataRecord fail = factory
                .read(1,
                        repository,
                        person,
                        "<Person><id>1002</id><lastname>Dupond</lastname><middlename>David</middlename><firstname>Julien</firstname><age>10</age><score>10</score><addresses><address>[900][false]</address></addresses><age>10</age><Status>Employee</Status></Person>");
        DataRecord success = factory
                .read(1,
                        repository,
                        person,
                        "<Person><id>1003</id><lastname>Dupond</lastname><middlename>David</middlename><firstname>Julien</firstname><age>10</age><score>10</score><addresses><address>[1000][true]</address></addresses><age>10</age><Status>Employee</Status></Person>");
        List<DataRecord> records = new LinkedList<DataRecord>();
        records.add(fail);
        records.add(success);

        storage.begin();
        storage.update(records);
        try {
            storage.commit();
            fail("Should have failed due to integrity constraint on address column.");
        } catch (Exception e) {
            // Expected
            storage.rollback();
        } finally {
            storage.end();
        }

        results = storage.fetch(failQuery.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }
        results = storage.fetch(successQuery.getSelect());
        try {
            assertEquals(0, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testLongEntityTypeName() throws Exception {
        ComplexTypeMetadata type = repository.getComplexType("EntityWithQuiteALongNameWithoutIncludingAnyUnderscore");

        DataRecordReader<String> factory = new XmlStringDataRecordReader();
        DataRecord record = factory
                .read(1, repository, type,
                        "<EntityWithQuiteALongNameWithoutIncludingAnyUnderscore><Id>1003</Id></EntityWithQuiteALongNameWithoutIncludingAnyUnderscore>");
        storage.begin();
        storage.update(record);
        storage.commit();

        Select select = from(type).getSelect();
        StorageResults results = storage.fetch(select);
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }
}