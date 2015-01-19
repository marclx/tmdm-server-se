/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.query.user;

import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;

import java.util.*;

/**
 *
 */
public class Select implements Expression {

    private final List<TypedExpression> selectedFields = new LinkedList<TypedExpression>();

    private final List<Join> joins = new LinkedList<Join>();

    private final List<ComplexTypeMetadata> types = new LinkedList<ComplexTypeMetadata>();

    private final Paging paging = new Paging();

    private final List<OrderBy> orderBy = new LinkedList<OrderBy>();

    private Condition condition;

    private String revisionId = "1"; //$NON-NLS-1$

    private boolean isProjection;

    private boolean forUpdate = false;

    private At history;

    private boolean cache;

    public Select() {
    }

    public void addType(ComplexTypeMetadata metadata) {
        if (!types.contains(metadata)) {
            types.add(metadata);
        }
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

    public List<OrderBy> getOrderBy() {
        return orderBy;
    }

    public void addOrderBy(OrderBy orderBy) {
        if (orderBy != null) {
            this.orderBy.add(orderBy);
        }
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
        if (condition == UserQueryHelper.TRUE) {
            condition = null;
        }
        if (selectedFields.isEmpty()) {
            isProjection = false;
        }
        Set<OrderBy> uniqueOrderBy = new HashSet<OrderBy>();
        for (OrderBy current : orderBy) {
            uniqueOrderBy.add(current);
        }
        Iterator<OrderBy> iterator = orderBy.iterator();
        while (iterator.hasNext()) {
            OrderBy next = iterator.next();
            if (!uniqueOrderBy.remove(next)) {
                iterator.remove();
            }
        }
        return this;
    }

    @Override
    public boolean cache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
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
        for (OrderBy currentOrderBy : this.orderBy) {
            copy.addOrderBy(currentOrderBy);
        }
        copy.setProjection(this.isProjection);
        for (Join join : joins) {
            copy.getJoins().add(join);
        }
        for (ComplexTypeMetadata type : types) {
            copy.addType(type);
        }
        copy.getPaging().setLimit(this.paging.getLimit());
        copy.getPaging().setStart(this.paging.getStart());
        copy.setForUpdate(forUpdate);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Select)) {
            return false;
        }
        Select select = (Select) o;
        if (isProjection != select.isProjection) {
            return false;
        }
        if (condition != null ? !condition.equals(select.condition) : select.condition != null) {
            return false;
        }
        if (!joins.equals(select.joins)) {
            return false;
        }
        if (!orderBy.equals(select.orderBy)) {
            return false;
        }
        if (!paging.equals(select.paging)) {
            return false;
        }
        if (revisionId != null ? !revisionId.equals(select.revisionId) : select.revisionId != null) {
            return false;
        }
        if (!selectedFields.equals(select.selectedFields)) {
            return false;
        }
        if (!types.equals(select.types)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = selectedFields.hashCode();
        result = 31 * result + joins.hashCode();
        result = 31 * result + types.hashCode();
        result = 31 * result + paging.hashCode();
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        result = 31 * result + (revisionId != null ? revisionId.hashCode() : 0);
        result = 31 * result + (orderBy.hashCode());
        result = 31 * result + (isProjection ? 1 : 0);
        return result;
    }

    public boolean forUpdate() {
        return forUpdate;
    }

    public void setForUpdate(boolean forUpdate) {
        this.forUpdate = forUpdate;
    }

    public void setHistory(At history) {
        this.history = history;
    }

    public At getHistory() {
        return history;
    }
}
