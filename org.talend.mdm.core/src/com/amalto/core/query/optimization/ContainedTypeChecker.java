package com.amalto.core.query.optimization;

import org.talend.mdm.commmon.metadata.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class ContainedTypeChecker extends DefaultMetadataVisitor<Boolean> {

    private final Set<String> containedTypes = new HashSet<String>();

    @Override
    public Boolean visit(EnumerationFieldMetadata enumField) {
        return false;
    }

    @Override
    public Boolean visit(ReferenceFieldMetadata referenceField) {
        return false;
    }

    @Override
    public Boolean visit(SimpleTypeFieldMetadata simpleField) {
        return false;
    }

    @Override
    public Boolean visit(ComplexTypeMetadata complexType) {
        Collection<FieldMetadata> fields = complexType.getFields();
        for (FieldMetadata field : fields) {
            if (field.accept(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visit(ContainedTypeFieldMetadata containedField) {
        if (containedTypes.add(containedField.getContainedType().getName())) {
            return super.visit(containedField);
        } else {
            return true;
        }
    }

    @Override
    public Boolean visit(ContainedComplexTypeMetadata type) {
        for (FieldMetadata field : type.getFields()) {
            if (field.accept(this)) {
                return true;
            }
        }
        return false;
    }

}
