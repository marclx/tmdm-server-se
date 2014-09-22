package org.talend.mdm.query;

import com.amalto.core.query.user.Condition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import static com.amalto.core.query.user.UserQueryBuilder.and;

class AndProcessor implements ConditionProcessor {

    static ConditionProcessor INSTANCE = new AndProcessor();

    private AndProcessor() {
    }

    @Override
    public Condition process(JsonObject condition, MetadataRepository repository) {
        JsonArray and = condition.get("and").getAsJsonArray(); //$NON-NLS-1$
        if (and.size() != 2) {
            throw new IllegalArgumentException("Malformed query (and is supposed to get 2 conditions - got " + and.size() + " -).");
        }
        Condition[] conditions = new Condition[2];
        for (int i = 0; i < and.size(); i++) {
            JsonObject object = and.get(i).getAsJsonObject();
            conditions[i] = Deserializer.getProcessor(object).process(object, repository);
        }
        return and(conditions[0], conditions[1]);
    }
}
