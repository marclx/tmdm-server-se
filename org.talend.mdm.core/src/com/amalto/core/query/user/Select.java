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
import com.amalto.core.metadata.ContainedTypeFieldMetadata;
import com.amalto.core.metadata.FieldMetadata;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class Select implements Expression {

    private static final Logger LOGGER = Logger.getLogger(Select.class);

    private final List<TypedExpression> selectedFields = new LinkedList<TypedExpression>();

    private final List<Join> joins = new LinkedList<Join>();

    private final List<ComplexTypeMetadata> types = new LinkedList<ComplexTypeMetadata>();

    private final Paging paging = new Paging();

    private Condition condition;

    private String revisionId = "1"; //$NON-NLS-1$

    private OrderBy orderBy;

    private boolean isProjection;

    public Select() {
    }

    public void addType(ComplexTypeMetadata metadata) {
        types.add(metadata);
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    public List<TypedExpression> getSelectedFields() {
        return selectedFields;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public OrderBy getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * @return <code>false</code> if the query is supposed to return a whole instance or <code>true</code> if the query
     *         is selecting few fields (that may or not belong to the same type).
     */
    public boolean isProjection() {
        return isProjection;
    }

    public void setProjection(boolean projection) {
        isProjection = projection;
    }

    public void addJoin(Join join) {
        joins.add(join);
    }

    public List<Join> getJoins() {
        return joins;
    }

    public List<ComplexTypeMetadata> getTypes() {
        return types;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public Paging getPaging() {
        return paging;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public Expression normalize() {
        if (condition != null) {
            condition = (Condition) condition.normalize();
        }
        Iterator<TypedExpression> selectedFieldsIterator = selectedFields.iterator();
        while (selectedFieldsIterator.hasNext()) {
            TypedExpression selectedField = selectedFieldsIterator.next();
            if (selectedField instanceof Field) {
                FieldMetadata fieldMetadata = ((Field) selectedField).getFieldMetadata();
                if (fieldMetadata instanceof ContainedTypeFieldMetadata) {
                    LOGGER.warn("Field '" + fieldMetadata.getName() + "' does not contain value and will be ignored.");
                    selectedFieldsIterator.remove();
                }
            }
        }
        if (selectedFields.isEmpty()) {
            isProjection = false;
        }
        return this;
    }

    /**
     * @return A copy of this {@link Select} instance. This is a shallow copy (only Select instance is new, all referenced
     *         objects such as {@link Condition}, {@link Paging}... are not copied).
     */
    public Select copy() {
        Select copy = new Select();
        copy.setRevisionId(this.revisionId);
        for (TypedExpression selectedField : selectedFields) {
            copy.getSelectedFields().add(selectedField);
        }
        copy.setCondition(this.condition);
        copy.setOrderBy(this.orderBy);
        copy.setProjection(this.isProjection);
        for (Join join : joins) {
            copy.getJoins().add(join);
        }
        for (ComplexTypeMetadata type : types) {
            copy.addType(type);
        }
        copy.getPaging().setLimit(this.paging.getLimit());
        copy.getPaging().setStart(this.paging.getStart());
        return copy;
    }
}