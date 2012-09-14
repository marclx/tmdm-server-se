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

package com.amalto.core.storage.record;

import com.amalto.core.metadata.*;
import com.amalto.core.storage.hibernate.TypeMapping;
import com.amalto.core.storage.record.metadata.DataRecordMetadata;

import java.util.*;

public class DataRecord {

    private final ComplexTypeMetadata type;

    private final Map<FieldMetadata, Object> fieldToValue = new HashMap<FieldMetadata, Object>();

    private final DataRecordMetadata recordMetadata;

    private long revisionId;

    /**
     * @param type           Type as {@link ComplexTypeMetadata} of the data record
     * @param recordMetadata Record specific metadata (e.g. last modification timestamp...)
     */
    public DataRecord(ComplexTypeMetadata type, DataRecordMetadata recordMetadata) {
        this.type = type;
        this.recordMetadata = recordMetadata;
    }

    public long getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(long revisionId) {
        this.revisionId = revisionId;
    }

    public ComplexTypeMetadata getType() {
        return type;
    }

    public DataRecordMetadata getRecordMetadata() {
        return recordMetadata;
    }

    public Object get(FieldMetadata field) {
        ComplexTypeMetadata containingType = field.getContainingType();
        if (containingType != this.getType() && !this.getType().isAssignableFrom(containingType)) {
            Iterator<FieldMetadata> path = MetadataUtils.path(type, field).iterator();
            if (!path.hasNext()) {
                Object value = get(field.getName());
                if(value != null) { // Support explicit projection type fields
                    return value;
                }
                throw new IllegalArgumentException("Field '" + field.getName() + "' isn't reachable from type '" + type.getName() + "'");
            }
            DataRecord current = this;
            while (path.hasNext()) {
                FieldMetadata nextField = path.next();
                Object nextObject = current.fieldToValue.get(nextField);
                if (nextObject == null) {
                    return null;
                }
                if (path.hasNext()) {
                    if (!(nextObject instanceof DataRecord)) {
                        if (!path.hasNext()) {
                            return nextObject;
                        } else if (nextObject instanceof List) {
                            // TODO This is maybe (surely?) not what user expect, but there's no way to select the nth instance of a collection in query API.
                            nextObject = ((List) nextObject).get(0);
                        } else {
                            throw new IllegalStateException("Can not process value '" + nextObject + "'");
                        }
                    }
                    current = (DataRecord) nextObject;
                } else {
                    return nextObject;
                }
            }
            return null; // Not found.
        } else {
            return fieldToValue.get(field);
        }
    }

    public Object get(String fieldName) {
        StringTokenizer tokenizer = new StringTokenizer(fieldName, "/");
        DataRecord current = this;
        Object currentValue = null;
        while (tokenizer.hasMoreTokens()) {
            String currentPathElement = tokenizer.nextToken();
            currentValue = current.get(current.getType().getField(currentPathElement));
            if (tokenizer.hasMoreTokens()) {
                if (currentValue == null) {
                    // Means record does not own field last call to "tokenizer.nextToken()" returned.
                    return null;
                } else {
                    current = (DataRecord) currentValue;
                }
            }
        }
        return currentValue;
    }

    public void set(FieldMetadata field, Object o) {
        if (field == null) {
            throw new IllegalArgumentException("Field can not be null.");
        }

        if (!field.isMany()) {
            fieldToValue.put(field, o);
        } else {
            List list = (List) fieldToValue.get(field);
            if (field instanceof ReferenceFieldMetadata || field instanceof ContainedTypeFieldMetadata) {  // fields that may contain data records.
                if (list == null) {
                    list = new LinkedList();
                    fieldToValue.put(field, list);
                }
                list.add(o);
            } else {
                if (list == null && o instanceof List) {
                    fieldToValue.put(field, o);
                } else {
                    if (list == null) {
                        list = new LinkedList();
                        fieldToValue.put(field, list);
                    }
                    list.add(o);
                }
            }
        }
    }

    public <T> T convert(DataRecordConverter<T> converter, TypeMapping mapping) {
        return converter.convert(this, mapping);
    }

    public Set<FieldMetadata> getSetFields() {
        return fieldToValue.keySet();
    }

}