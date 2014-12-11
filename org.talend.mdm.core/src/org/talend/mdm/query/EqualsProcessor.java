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
        TypedExpressionProcessor processor = Deserializer.getTypedExpression(eq);
        TypedExpression left = processor.process(eq, repository);
        JsonElement valueElement = eq.get("value");
        if (left == null || valueElement == null) {
            throw new IllegalArgumentException("Malformed query (missing field conditions in condition).");
        }
        if (valueElement.isJsonPrimitive()) {
            String value = valueElement.getAsString(); //$NON-NLS-1$
            return eq(left, value);
        } else {
            JsonObject valueObject = valueElement.getAsJsonObject();
            TypedExpressionProcessor rightProcessor = Deserializer.getTypedExpression(valueObject);
            return eq(left, rightProcessor.process(valueObject, repository));
        }
    }
}
