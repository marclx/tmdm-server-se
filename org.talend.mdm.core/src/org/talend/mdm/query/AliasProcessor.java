package org.talend.mdm.query;

import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import static com.amalto.core.query.user.UserQueryBuilder.alias;

class AliasProcessor implements TypedExpressionProcessor {

    public static final TypedExpressionProcessor INSTANCE = new AliasProcessor();

    private AliasProcessor() {
    }

    @Override
    public TypedExpression process(JsonObject element, MetadataRepository repository) {
        JsonElement alias = element.get("alias"); //$NON-NLS-1$
        JsonArray array = alias.getAsJsonArray();
        String aliasName = null;
        TypedExpression aliasedExpression = null;
        for (int j = 0; j < array.size(); j++) {
            JsonObject item = array.get(j).getAsJsonObject();
            if (item.has("name")) { //$NON-NLS-1$
                aliasName = item.get("name").getAsString(); //$NON-NLS-1$
            } else {
                aliasedExpression = Deserializer.getTypedExpression(item).process(item, repository);
            }
        }
        if (aliasName == null || aliasedExpression == null) {
            throw new IllegalArgumentException("Malformed query (field '" + alias + "' is malformed).");
        }
        return alias(aliasedExpression, aliasName);
    }
}
