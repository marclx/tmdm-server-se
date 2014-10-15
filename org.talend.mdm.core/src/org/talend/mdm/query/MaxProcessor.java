package org.talend.mdm.query;

import static com.amalto.core.query.user.UserQueryBuilder.max;

import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonObject;

class MaxProcessor implements TypedExpressionProcessor {

    public static final TypedExpressionProcessor INSTANCE = new MaxProcessor();

    private MaxProcessor() {
    }

    @Override
    public TypedExpression process(JsonObject element, MetadataRepository repository) {
        JsonObject max = element.get("max").getAsJsonObject(); //$NON-NLS-1$
        TypedExpression expression = Deserializer.getTypedExpression(max).process(max, repository);
        return max(expression);
    }
}
