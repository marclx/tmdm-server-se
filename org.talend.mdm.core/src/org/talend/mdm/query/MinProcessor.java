package org.talend.mdm.query;

import static com.amalto.core.query.user.UserQueryBuilder.min;

import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonObject;

class MinProcessor implements TypedExpressionProcessor {

    public static final TypedExpressionProcessor INSTANCE = new MinProcessor();

    private MinProcessor() {
    }

    @Override
    public TypedExpression process(JsonObject element, MetadataRepository repository) {
        JsonObject min = element.get("min").getAsJsonObject(); //$NON-NLS-1$
        TypedExpression expression = Deserializer.getTypedExpression(min).process(min, repository);
        return min(expression);
    }
}
