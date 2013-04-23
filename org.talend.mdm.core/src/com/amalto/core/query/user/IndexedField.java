/*
 * Copyright (C) 2006-2013 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.query.user;


import org.talend.mdm.commmon.metadata.FieldMetadata;

/**
 *
 */
public class IndexedField extends Field {

    private final int position;

    public IndexedField(FieldMetadata fieldMetadata, int position) {
        super(fieldMetadata);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public Expression normalize() {
        return this;
    }
}