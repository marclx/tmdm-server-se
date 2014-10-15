package org.talend.mdm.query;

import static com.amalto.core.query.user.UserQueryBuilder.isa;

import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.Condition;
import com.amalto.core.query.user.Field;
import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonObject;

class IsProcessor implements ConditionProcessor {

    public static final ConditionProcessor INSTANCE = new IsProcessor();

    private IsProcessor() {
    }

    @Override
    public Condition process(JsonObject condition, MetadataRepository repository) {
        ComplexTypeMetadata type;
        JsonObject element = condition.get("is").getAsJsonObject(); //$NON-NLS-1$
        if (element.has("type")) { //$NON-NLS-1$
            String typeName = element.get("type").getAsString(); //$NON-NLS-1$
            type = (ComplexTypeMetadata) repository.getNonInstantiableType(repository.getUserNamespace(), typeName);
        } else {
            throw new IllegalArgumentException("Malformed query (no 'type' in IS condition).");
        }
        TypedExpression expression = Deserializer.getTypedExpression(element).process(element, repository);
        if (!(expression instanceof Field)) {
            throw new IllegalArgumentException("Malformed query (expect expression to be a Field in IS condition).");
        }
        return isa(((Field) expression).getFieldMetadata(), type);
    }
}
