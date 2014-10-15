package org.talend.mdm.query;

import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.MetadataRepository;

class FieldProcessor implements TypedExpressionProcessor {

    public static final TypedExpressionProcessor INSTANCE = new FieldProcessor();

    private FieldProcessor() {
    }

    @Override
    public TypedExpression process(JsonObject element, MetadataRepository repository) {
        String fieldName = element.get("field").getAsString(); //$NON-NLS-1$ //$NON-NLS-2$
        return Deserializer.getField(repository, fieldName);
    }
}
