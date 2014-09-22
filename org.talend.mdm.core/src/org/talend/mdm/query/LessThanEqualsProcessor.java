package org.talend.mdm.query;

import static com.amalto.core.query.user.UserQueryBuilder.lt;
import static com.amalto.core.query.user.UserQueryBuilder.lte;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.Condition;
import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class LessThanEqualsProcessor implements ConditionProcessor {

    static ConditionProcessor INSTANCE = new LessThanEqualsProcessor();

    private LessThanEqualsProcessor() {
    }

    @Override
    public Condition process(JsonObject condition, MetadataRepository repository) {
        JsonObject eq = condition.get("lte").getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = eq.entrySet();
        TypedExpression left = null;
        String value = null;
        for (Map.Entry<String, JsonElement> entry : entries) {
            if ("value".equals(entry.getKey())) { //$NON-NLS-1$
                value = eq.get("value").getAsString(); //$NON-NLS-1$
            } else if ("field".equals(entry.getKey())) { //$NON-NLS-1$
                String path = entry.getValue().getAsString();
                left = Deserializer.getField(repository, path);
            } else {
                throw new NotImplementedException("No support for '" + entry.getKey() + "'.");
            }
        }
        if (left == null || value == null) {
            throw new IllegalArgumentException("Malformed query (missing field conditions in condition).");
        }
        return lte(left, value);
    }
}
