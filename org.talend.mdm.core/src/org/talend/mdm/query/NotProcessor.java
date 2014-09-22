package org.talend.mdm.query;

import static com.amalto.core.query.user.UserQueryBuilder.not;

import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.Condition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class NotProcessor implements ConditionProcessor {

    static ConditionProcessor INSTANCE = new NotProcessor();

    private NotProcessor() {
    }

    @Override
    public Condition process(JsonObject condition, MetadataRepository repository) {
        JsonObject not = condition.get("not").getAsJsonObject(); //$NON-NLS-1$
        return not(Deserializer.getProcessor(not).process(not, repository));
    }
}
