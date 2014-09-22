package org.talend.mdm;

import com.amalto.core.query.user.*;
import junit.framework.TestCase;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.talend.mdm.query.QueryParser;

import java.io.InputStream;

/**
 *
 */
public class QueryParserTest extends TestCase {

    private MetadataRepository repository;

    public void setUp() throws Exception {
        super.setUp();
        repository = new MetadataRepository();
        repository.load(QueryParserTest.class.getResourceAsStream("metadata.xsd"));
    }

    public void testArguments() {
        try {
            QueryParser.newParser(null);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        QueryParser parser = QueryParser.newParser(new MetadataRepository());
        try {
            parser.parse(((String) null));
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            parser.parse(((InputStream) null));
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testQuery1() {
        QueryParser parser = QueryParser.newParser(repository);
        Expression expression = parser.parse(QueryParserTest.class.getResourceAsStream("query1.json")); //$NON-NLS-1$
        assertTrue(expression instanceof Select);
        Select select = (Select) expression;
        assertEquals(1, select.getTypes().size());
        assertEquals("Type1", select.getTypes().get(0).getName()); //$NON-NLS-1$
    }

    public void testQuery2() {
        QueryParser parser = QueryParser.newParser(repository);
        Expression expression = parser.parse(QueryParserTest.class.getResourceAsStream("query2.json")); //$NON-NLS-1$
        assertTrue(expression instanceof Select);
        Select select = (Select) expression;
        Condition condition = select.getCondition();
        assertNotNull(condition);
        assertEquals(Compare.class, condition.getClass());
        assertEquals(Predicate.EQUALS, ((Compare) condition).getPredicate());
        assertEquals(StringConstant.class, ((Compare) condition).getRight().getClass());
        assertEquals(Field.class, ((Compare) condition).getLeft().getClass());
    }

    public void testQuery3() {
        QueryParser parser = QueryParser.newParser(repository);
        Expression expression = parser.parse(QueryParserTest.class.getResourceAsStream("query3.json")); //$NON-NLS-1$
        assertTrue(expression instanceof Select);
        Select select = (Select) expression;
        Condition condition = select.getCondition();
        assertNotNull(condition);
        assertEquals(BinaryLogicOperator.class, condition.getClass());
        assertEquals(Predicate.AND, ((BinaryLogicOperator) condition).getPredicate());
        assertEquals(Compare.class, ((BinaryLogicOperator) condition).getLeft().getClass());
        assertEquals(Compare.class, ((BinaryLogicOperator) condition).getRight().getClass());
    }

    public void testQuery4() {
        QueryParser parser = QueryParser.newParser(repository);
        Expression expression = parser.parse(QueryParserTest.class.getResourceAsStream("query4.json")); //$NON-NLS-1$
        assertTrue(expression instanceof Select);
        Select select = (Select) expression;
        Condition condition = select.getCondition();
        assertNotNull(condition);
        assertEquals(BinaryLogicOperator.class, condition.getClass());
        assertEquals(Predicate.OR, ((BinaryLogicOperator) condition).getPredicate());
        assertEquals(Compare.class, ((BinaryLogicOperator) condition).getLeft().getClass());
        assertEquals(Compare.class, ((BinaryLogicOperator) condition).getRight().getClass());
    }

    public void testQuery5() {
        QueryParser parser = QueryParser.newParser(repository);
        Expression expression = parser.parse(QueryParserTest.class.getResourceAsStream("query5.json")); //$NON-NLS-1$
        assertTrue(expression instanceof Select);
        Select select = (Select) expression;
        Condition condition = select.getCondition();
        assertNotNull(condition);
        assertEquals(BinaryLogicOperator.class, condition.getClass());
        assertEquals(Predicate.OR, ((BinaryLogicOperator) condition).getPredicate());
        assertEquals(Compare.class, ((BinaryLogicOperator) condition).getLeft().getClass());
        assertEquals(BinaryLogicOperator.class, ((BinaryLogicOperator) condition).getRight().getClass());
        assertEquals(Predicate.AND, ((BinaryLogicOperator) ((BinaryLogicOperator) condition).getRight()).getPredicate());
    }

    public void testQuery6() {
        QueryParser parser = QueryParser.newParser(repository);
        Expression expression = parser.parse(QueryParserTest.class.getResourceAsStream("query6.json")); //$NON-NLS-1$
        assertTrue(expression instanceof Select);
        Select select = (Select) expression;
        assertEquals(1, select.getSelectedFields().size());
        assertEquals(Field.class, select.getSelectedFields().get(0).getClass());
    }
}
