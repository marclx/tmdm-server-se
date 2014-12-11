package org.talend.mdm.query;

import com.amalto.core.query.user.Condition;
import com.amalto.core.query.user.Field;
import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import static com.amalto.core.query.user.UserQueryBuilder.fullText;
import static com.amalto.core.query.user.UserQueryBuilder.isa;

class FullTextProcessor implements ConditionProcessor {

    public static final ConditionProcessor INSTANCE = new FullTextProcessor();

    private FullTextProcessor() {
    }

    @Override
    public Condition process(JsonObject condition, MetadataRepository repository) {
        String value;
        JsonObject element = condition.get("full_text").getAsJsonObject(); //$NON-NLS-1$
        if (element.has("value")) { //$NON-NLS-1$
            value = element.get("value").getAsString(); //$NON-NLS-1$
        } else {
            throw new IllegalArgumentException("Malformed query (no 'value' in FULL_TEXT condition).");
        }
        if (element.entrySet().size() > 1) {
            TypedExpression expression = Deserializer.getTypedExpression(element).process(element, repository);
            if (!(expression instanceof Field)) {
                throw new IllegalArgumentException("Malformed query (expect expression to be a Field in FULL_TEXT condition).");
            }
            return fullText(((Field) expression).getFieldMetadata(), value);
        } else {
            return fullText(value);
        }
    }
}
