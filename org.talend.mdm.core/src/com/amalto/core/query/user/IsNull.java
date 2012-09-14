/*
 * Copyright (C) 2006-2012 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.query.user;

public class IsNull extends Condition {

    private final TypedExpression field;

    public IsNull(TypedExpression field) {
        this.field = field;
    }

    public TypedExpression getField() {
        return field;
    }

    public Expression normalize() {
        return this;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}