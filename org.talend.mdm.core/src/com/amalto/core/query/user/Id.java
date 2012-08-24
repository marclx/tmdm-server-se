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

import com.amalto.core.metadata.ComplexTypeMetadata;

public class Id implements Expression {

    private final ComplexTypeMetadata type;

    private final String id;

    public Id(ComplexTypeMetadata type, String id) {
        this.type = type;
        this.id = id;
    }

    public Expression normalize() {
        return this;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getId() {
        return id;
    }

    public ComplexTypeMetadata getType() {
        return type;
    }
}