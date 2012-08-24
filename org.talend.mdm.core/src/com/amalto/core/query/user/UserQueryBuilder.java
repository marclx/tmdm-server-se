/*
 * Copyright (C) 2006-2011 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.query.user;

import com.amalto.core.metadata.ComplexTypeMetadata;
import com.amalto.core.metadata.FieldMetadata;
import com.amalto.core.metadata.ReferenceFieldMetadata;
import org.apache.log4j.Logger;

import java.util.List;

/**
 *
 */
public class UserQueryBuilder {

    private static final Logger LOGGER = Logger.getLogger(UserQueryBuilder.class);

    private final Select select;

    private UserQueryBuilder(Select select) {
        this.select = select;
    }

    public static Condition and(Condition left, Condition right) {
        assertConditionsArguments(left, right);
        return new BinaryLogicOperator(left, Predicate.AND, right);
    }

    public static Condition not(Condition condition) {
        return new UnaryLogicOperator(condition, Predicate.NOT);
    }

    public static Condition or(Condition left, Condition right) {
        assertConditionsArguments(left, right);
        return new BinaryLogicOperator(left, Predicate.OR, right);
    }

    public static Condition startsWith(FieldMetadata field, String constant) {
        assertValueConditionArguments(field, constant);
        Field userField = new Field(field);
        return new Compare(userField, Predicate.STARTS_WITH, createConstant(userField, constant));
    }

    public static Condition startsWith(TypedExpression field, String constant) {
        assertValueConditionArguments(field, constant);
        return new Compare(field, Predicate.STARTS_WITH, createConstant(field, constant));
    }

    public static Condition gt(FieldMetadata field, String constant) {
        assertValueConditionArguments(field, constant);
        Field userField = new Field(field);
        return gt(userField, constant);
    }

    public static Condition gt(TypedExpression expression, String constant) {
        assertValueConditionArguments(expression, constant);
        return new Compare(expression, Predicate.GREATER_THAN, createConstant(expression, constant));
    }

    public static Condition gte(FieldMetadata field, String constant) {
        assertValueConditionArguments(field, constant);
        Field userField = new Field(field);
        return gte(userField, constant);
    }

    public static Condition gte(TypedExpression expression, String constant) {
        assertValueConditionArguments(expression, constant);
        return new Compare(expression, Predicate.GREATER_THAN_OR_EQUALS, createConstant(expression, constant));
    }

    public static Compare lt(FieldMetadata field, String constant) {
        assertValueConditionArguments(field, constant);
        Field userField = new Field(field);
        return lt(userField, constant);
    }

    public static Compare lt(TypedExpression expression, String constant) {
        return new Compare(expression, Predicate.LOWER_THAN, createConstant(expression, constant));
    }

    public static Compare lte(FieldMetadata field, String constant) {
        assertValueConditionArguments(field, constant);
        Field userField = new Field(field);
        return lte(userField, constant);
    }

    public static Compare lte(TypedExpression expression, String constant) {
        return new Compare(expression, Predicate.LOWER_THAN_OR_EQUALS, createConstant(expression, constant));
    }

    public static Compare lte(FieldMetadata left, FieldMetadata right) {
        return new Compare(new Field(left), Predicate.LOWER_THAN_OR_EQUALS, new Field(right));
    }

    public static Condition eq(TypedExpression expression, String constant) {
        assertValueConditionArguments(expression, constant);
        if (expression instanceof Field) {
            return eq(((Field) expression), constant);
        } else {
            return new Compare(expression, Predicate.EQUALS, createConstant(expression, constant));
        }
    }

    public static Condition eq(FieldMetadata field, String constant) {
        assertValueConditionArguments(field, constant);
        Field userField = new Field(field);
        return eq(userField, constant);
    }

    public static Condition eq(Field field, String constant) {
        assertValueConditionArguments(field, constant);
        if (field.getFieldMetadata() instanceof ReferenceFieldMetadata) {
            ReferenceFieldMetadata fieldMetadata = (ReferenceFieldMetadata) field.getFieldMetadata();
            return new Compare(field, Predicate.EQUALS, new Id(fieldMetadata.getReferencedType(), constant));
        } else {
            return new Compare(field, Predicate.EQUALS, createConstant(field, constant));
        }
    }

    public static Condition isa(FieldMetadata field, ComplexTypeMetadata type) {
        return new Isa(new Field(field), type);
    }

    public UserQueryBuilder isa(ComplexTypeMetadata type) {
        if (select == null || select.getTypes().isEmpty()) {
            throw new IllegalStateException("No type is currently selected.");
        }
        where(new Isa(new ComplexTypeExpression(select.getTypes().get(0)), type));
        return this;
    }

    private static Expression createConstant(TypedExpression expression, String constant) {
        String fieldTypeName = expression.getTypeName();
        if ("integer".equals(fieldTypeName)  //$NON-NLS-1$
                || "positiveInteger".equals(fieldTypeName) //$NON-NLS-1$
                || "negativeInteger".equals(fieldTypeName) //$NON-NLS-1$
                || "nonPositiveInteger".equals(fieldTypeName) //$NON-NLS-1$
                || "nonNegativeInteger".equals(fieldTypeName) //$NON-NLS-1$
                || "unsignedInt".equals(fieldTypeName) //$NON-NLS-1$
                || "int".equals(fieldTypeName)) { //$NON-NLS-1$
            return new IntegerConstant(Integer.parseInt(constant));
        } else if ("string".equals(fieldTypeName) //$NON-NLS-1$
                || "hexBinary".equals(fieldTypeName) //$NON-NLS-1$
                || "base64Binary".equals(fieldTypeName) //$NON-NLS-1$
                || "anyURI".equals(fieldTypeName) //$NON-NLS-1$
                || "QName".equals(fieldTypeName)) { //$NON-NLS-1$
            return new StringConstant(constant);
        } else if ("date".equals(fieldTypeName)) { //$NON-NLS-1$
            return new DateConstant(constant);
        } else if ("dateTime".equals(fieldTypeName)) { //$NON-NLS-1$
            return new DateTimeConstant(constant);
        } else if ("time".equals(fieldTypeName) || "duration".equals(fieldTypeName)) { //$NON-NLS-1$ //$NON-NLS-2$
            return new TimeConstant(constant);
        } else if ("boolean".equals(fieldTypeName)) { //$NON-NLS-1$
            return new BooleanConstant(constant);
        } else if ("decimal".equals(fieldTypeName)) { //$NON-NLS-1$
            return new BigDecimalConstant(constant);
        } else if ("short".equals(fieldTypeName) || "unsignedShort".equals(fieldTypeName)) { //$NON-NLS-1$ //$NON-NLS-2$
            return new ShortConstant(constant);
        } else if ("byte".equals(fieldTypeName) || "unsignedByte".equals(fieldTypeName)) { //$NON-NLS-1$ //$NON-NLS-2$
            return new ByteConstant(constant);
        } else if ("long".equals(fieldTypeName) || "unsignedLong".equals(fieldTypeName)) { //$NON-NLS-1$ //$NON-NLS-2$
            return new LongConstant(constant);
        } else if ("double".equals(fieldTypeName)) { //$NON-NLS-1$
            return new DoubleConstant(constant);
        } else if ("float".equals(fieldTypeName)) { //$NON-NLS-1$
            return new FloatConstant(constant);
        } else {
            throw new IllegalArgumentException("Cannot create expression constant for expression type '" + expression.getTypeName() + "'");
        }
    }

    public static Condition emptyOrNull(FieldMetadata field) {
        assertNullField(field);
        // Only do a isEmpty operator if field type is string, for all other known cases, isNull is enough.
        if ("string".equals(field.getType().getName())) {
            return new BinaryLogicOperator(isEmpty(field), Predicate.OR, isNull(field));
        } else {
            return isNull(field);
        }
    }

    public static Condition emptyOrNull(TypedExpression field) {
        assertNullField(field);
        // Only do a isEmpty operator if field type is string, for all other known cases, isNull is enough.
        if ("string".equals(field.getTypeName())) {
            return new BinaryLogicOperator(isEmpty(field), Predicate.OR, isNull(field));
        } else {
            return isNull(field);
        }
    }

    public static Condition isEmpty(FieldMetadata field) {
        assertNullField(field);
        return new IsEmpty(new Field(field));
    }

    public static Condition isEmpty(TypedExpression typedExpression) {
        assertNullField(typedExpression);
        return or(new IsEmpty(typedExpression), isNull(typedExpression));
    }

    public static Condition isNull(FieldMetadata field) {
        assertNullField(field);
        return new IsNull(new Field(field));
    }

    public static Condition isNull(TypedExpression typedExpression) {
        assertNullField(typedExpression);
        return new IsNull(typedExpression);
    }

    public static Condition neq(FieldMetadata field, String constant) {
        assertValueConditionArguments(field, constant);
        return new UnaryLogicOperator(eq(field, constant), Predicate.NOT);
    }

    public static Condition neq(TypedExpression field, String constant) {
        assertValueConditionArguments(field, constant);
        return new UnaryLogicOperator(eq(field, constant), Predicate.NOT);
    }

    public static Condition fullText(String constant) {
        return new FullText(constant);
    }

    public static TypedExpression count() {
        return new Alias(new Count(), "count"); //$NON-NLS-1$
    }

    public static TypedExpression timestamp() {
        return new Timestamp();
    }

    public static TypedExpression revision() {
        return new Revision();
    }

    public static TypedExpression taskId() {
        return new TaskId();
    }

    public static UserQueryBuilder from(ComplexTypeMetadata type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }

        Select select = new Select();
        select.addType(type);
        return new UserQueryBuilder(select);
    }

    public UserQueryBuilder select(FieldMetadata... fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Fields cannot be null");
        }

        for (FieldMetadata field : fields) {
            select(field);
        }
        return this;
    }

    public UserQueryBuilder select(FieldMetadata field) {
        if (field == null) {
            throw new IllegalArgumentException("Field cannot be null");
        }

        select.getSelectedFields().add(new Field(field));
        select.setProjection(true);
        return this;
    }

    /**
     * Adds a {@link Condition} to the {@link Select} built by this {@link UserQueryBuilder}. If this method has previously
     * been called, a logic "and" condition is created between the existing condition ({@link com.amalto.core.query.user.Select#getCondition()}
     * and the <code>condition</code> parameter.
     *
     * @param condition A {@link Condition} to be added to the user query.
     * @return Same {@link UserQueryBuilder} for chained method calls.
     * @throws IllegalArgumentException If <code>condition</code> parameter is null.
     */
    public UserQueryBuilder where(Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }

        if (select.getCondition() == null) {
            select.setCondition(condition);
        } else {
            select.setCondition(and(select.getCondition(), condition));
        }

        return this;
    }

    public UserQueryBuilder orderBy(FieldMetadata field, OrderBy.Direction direction) {
        if (field == null) {
            throw new IllegalArgumentException("Field cannot be null");
        }

        Field userField = new Field(field);
        select.setOrderBy(new OrderBy(userField, direction));
        return this;
    }

    public UserQueryBuilder join(TypedExpression leftField, FieldMetadata rightField) {
        if (!(leftField instanceof Field)) {
            throw new IllegalArgumentException("Can not perform join on a non-user field (was " + leftField.getClass().getName() + ")");
        }
        return join(((Field) leftField).getFieldMetadata(), rightField);
    }

    /**
     * <p>
     * Join a type's field with another.
     * </p>
     * <p>
     * If left field is a FK, use this method for Joins when right field is a simple PK (i.e. joined entity does not
     * have composite id). If this is the case, consider using {@link #join(com.amalto.core.metadata.FieldMetadata)}.
     * </p>
     *
     * @param leftField  The left field for the join operation.
     * @param rightField The right field for the join operation.
     * @return Same {@link UserQueryBuilder} for chained method calls.
     */
    public UserQueryBuilder join(FieldMetadata leftField, FieldMetadata rightField) {
        if (leftField == null) {
            throw new IllegalArgumentException("Left field cannot be null");
        }
        if (rightField == null) {
            throw new IllegalArgumentException("Right field cannot be null");
        }
        if (leftField instanceof ReferenceFieldMetadata) {
            FieldMetadata leftReferencedField = ((ReferenceFieldMetadata) leftField).getReferencedField();
            if (!leftReferencedField.equals(rightField)) {
                throw new IllegalArgumentException("Left field '" + leftReferencedField.getName() + "' is a FK, but right field isn't the one left is referring to.");
            }
        }

        Field leftUserField = new Field(leftField);
        Field rightUserField = new Field(rightField);
        // Implicit select joined type if it isn't already selected
        if (!select.getTypes().contains(rightField.getContainingType())) {
            select.addType(rightField.getContainingType());
        }

        select.addJoin(new Join(leftUserField, rightUserField, JoinType.INNER));
        return this;
    }

    /**
     * <p>
     * Join a type's field with another. This method expects field to be a {@link ReferenceFieldMetadata} and automatically
     * creates a Join between <code>field</code> parameter and the field(s) it targets.
     * </p>
     *
     * @param field The left field for the join operation.
     * @return Same {@link UserQueryBuilder} for chained method calls.
     * @throws IllegalArgumentException If <code>field</code> is not a {@link ReferenceFieldMetadata}.
     */
    public UserQueryBuilder join(FieldMetadata field) {
        if (field == null) {
            throw new IllegalArgumentException("Field cannot be null");
        }
        if (!(field instanceof ReferenceFieldMetadata)) {
            throw new IllegalArgumentException("Field must be a reference field.");
        }

        return join(field, ((ReferenceFieldMetadata) field).getReferencedField());
    }

    public UserQueryBuilder start(int start) {
        if (start < 0) {
            throw new IllegalArgumentException("Start index must be positive");
        }

        select.getPaging().setStart(start);
        return this;
    }

    public UserQueryBuilder limit(int limit) {
        if (limit > 0) {
            // Only consider limit > 0 as worthy values.
            select.getPaging().setLimit(limit);
        }
        return this;
    }

    public Select getSelect() {
        if (select == null) {
            throw new IllegalStateException("No type has been selected");
        }

        return select;
    }

    public UserQueryBuilder and(ComplexTypeMetadata type) {
        select.addType(type);
        return this;
    }

    private static void assertConditionsArguments(Condition left, Condition right) {
        if (left == null) {
            throw new IllegalArgumentException("Left condition cannot be null");
        }
        if (right == null) {
            throw new IllegalArgumentException("Right condition cannot be null");
        }
    }

    private static void assertValueConditionArguments(Object field, String constant) {
        if (field == null) {
            throw new IllegalArgumentException("Field cannot be null");
        }
        if (constant == null) {
            throw new IllegalArgumentException("Constant cannot be null");
        }
    }

    private static void assertNullField(Object field) {
        if (field == null) {
            throw new IllegalArgumentException("Field cannot be null");
        }
    }

    public UserQueryBuilder select(TypedExpression expression) {
        select.getSelectedFields().add(expression);
        select.setProjection(true);
        return this;
    }

    public static TypedExpression alias(FieldMetadata field, String alias) {
        return alias(new Field(field), alias);
    }

    public static TypedExpression alias(TypedExpression expression, String alias) {
        return new Alias(expression, alias);
    }

    public UserQueryBuilder selectId(ComplexTypeMetadata typeMetadata) {
        List<FieldMetadata> keyFields = typeMetadata.getKeyFields();
        for (FieldMetadata keyField : keyFields) {
            select(keyField);
        }
        return this;
    }

    public UserQueryBuilder select(List<FieldMetadata> viewableFields) {
        if (viewableFields == null) {
            throw new IllegalArgumentException("Viewable fields cannot be null");
        }

        for (FieldMetadata viewableField : viewableFields) {
            select(viewableField);
        }
        return this;
    }

    public static Condition contains(FieldMetadata field, String value) {
        assertValueConditionArguments(field, value);
        Field userField = new Field(field);
        return contains(userField, value);
    }

    public static Condition contains(TypedExpression field, String value) {
        assertValueConditionArguments(field, value);
        Expression constant = createConstant(field, value);
        if (constant instanceof StringConstant) {
            return new Compare(field, Predicate.CONTAINS, constant);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Change CONTAINS to EQUALS for '" + field + "' (type: " + field.getTypeName() + ").");
            }
            return new Compare(field, Predicate.EQUALS, constant);
        }
    }
}