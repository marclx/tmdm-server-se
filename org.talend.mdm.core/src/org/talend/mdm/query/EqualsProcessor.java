package org.talend.mdm.query;

import static com.amalto.core.query.user.UserQueryBuilder.eq;

import java.util.Map;
import java.util.Set;

import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.Condition;
import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class EqualsProcessor implements ConditionProcessor {

    static ConditionProcessor INSTANCE = new EqualsProcessor();

    private EqualsProcessor() {
    }

    @Override
    public Condition process(JsonObject condition, MetadataRepository repository) {
        JsonObject eq = condition.get("eq").getAsJsonObject();
        String value = eq.get("value").getAsString(); //$NON-NLS-1$
        TypedExpressionProcessor processor = Deserializer.getTypedExpression(eq);
        TypedExpression left = processor.process(eq, repository);
        if (left == null || value == null) {
            throw new IllegalArgumentException("Malformed query (missing field conditions in condition).");
        }
        return eq(left, value);
    }
}
