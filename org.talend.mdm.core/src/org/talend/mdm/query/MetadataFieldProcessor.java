package org.talend.mdm.query;

import com.amalto.core.query.user.TypedExpression;
import com.amalto.core.query.user.metadata.MetadataField;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.MetadataRepository;

class MetadataFieldProcessor implements TypedExpressionProcessor {

    public static final TypedExpressionProcessor INSTANCE = new MetadataFieldProcessor();

    private MetadataFieldProcessor() {
    }

    @Override
    public TypedExpression process(JsonObject element, MetadataRepository repository) {
        String metadataFieldName = element.get("metadata").getAsString(); //$NON-NLS-1$ //$NON-NLS-2$
        MetadataField metadataField = MetadataField.Factory.getMetadataField(metadataFieldName);
        if (metadataField == null) {
            throw new IllegalArgumentException("Metadata field '" + metadataFieldName + "' does not exist.");
        }
        return metadataField;
    }
}
