package org.talend.mdm.query;

import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import static com.amalto.core.query.user.UserQueryBuilder.count;
import static com.amalto.core.query.user.UserQueryBuilder.distinct;

class CountProcessor implements TypedExpressionProcessor {

    public static final TypedExpressionProcessor INSTANCE = new CountProcessor();

    private CountProcessor() {
    }

    @Override
    public TypedExpression process(JsonObject element, MetadataRepository repository) {
        JsonObject count = element.get("count").getAsJsonObject(); //$NON-NLS-1
        return count(Deserializer.getTypedExpression(count).process(count, repository));
    }
}
