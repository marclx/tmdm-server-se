package org.talend.mdm.query;

import com.amalto.core.query.user.TypedExpression;
import com.amalto.core.query.user.UserQueryBuilder;
import com.amalto.core.query.user.metadata.MetadataField;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import static com.amalto.core.query.user.UserQueryBuilder.distinct;

class DistinctProcessor implements TypedExpressionProcessor {

    public static final TypedExpressionProcessor INSTANCE = new DistinctProcessor();

    private DistinctProcessor() {
    }

    @Override
    public TypedExpression process(JsonObject element, MetadataRepository repository) {
        JsonObject distinct = element.get("distinct").getAsJsonObject(); //$NON-NLS-1
        return distinct(Deserializer.getTypedExpression(distinct).process(distinct, repository));
    }
}
