package org.talend.mdm.query;

import com.amalto.core.query.user.*;
import com.google.gson.*;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import static com.amalto.core.query.user.UserQueryBuilder.and;
import static com.amalto.core.query.user.UserQueryBuilder.eq;
import static com.amalto.core.query.user.UserQueryBuilder.or;

/**
*
*/
class Deserializer implements InstanceCreator<Expression>, JsonDeserializer<Expression> {

    private final MetadataRepository repository;

    private UserQueryBuilder queryBuilder;

    Deserializer(MetadataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Expression createInstance(Type type) {
        // TODO
        return null;
    }

    @Override
    public Expression deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if(json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            if(!obj.has("select")) {
                throw new IllegalArgumentException("Malformed query (has top level object but hasn't 'select' in it).");
            }
            JsonObject select = obj.get("select").getAsJsonObject();
            if(!select.has("from")) {
                throw new IllegalArgumentException("Malformed query (expected 'from' object in 'select').");
            }
            // 'from' clause (selected types in query).
            JsonArray from = select.get("from").getAsJsonArray();
            for (int i = 0; i < from.size(); i++) {
                String typeName = from.get(i).getAsString();
                ComplexTypeMetadata type = repository.getComplexType(typeName);
                if(type == null) {
                    throw new IllegalArgumentException("Malformed query (type '" + typeName + "' does not exist).");
                }
                if(queryBuilder == null) {
                    queryBuilder = UserQueryBuilder.from(type);
                } else {
                    queryBuilder.and(type);
                }
            }
            // Sanity check: query builder should be initialized at this point
            if(queryBuilder == null) {
                throw new IllegalStateException("Expected query builder to be initialized.");
            }
            // Process conditions (conditions are optional)
            if(select.has("where")) {
                JsonObject where = select.get("where").getAsJsonObject();
                ConditionProcessor processor = getProcessor(where);
                queryBuilder.where(processor.process(where, repository));
            }
            // Process selected fields (optional)
            if(select.has("fields")) {
                JsonArray fields = select.get("fields").getAsJsonArray();
                for (int i = 0; i < fields.size(); i++) {
                    JsonObject fieldElement = fields.get(i).getAsJsonObject();
                    if(fieldElement.has("field")) {
                        String fieldName = fieldElement.get("field").getAsJsonObject().get("name").getAsString();
                        queryBuilder.select(getField(repository, fieldName));
                    } else {
                        throw new NotImplementedException("No support for '" + fieldElement + "'.");
                    }
                }
            }

        } else {
            throw new IllegalArgumentException("Malformed query (expected a top level object).");
        }
        return queryBuilder.getExpression();
    }

    private static ConditionProcessor getProcessor(JsonObject object) {
        if(object.has("eq")) {
            return new EqualsProcessor();
        } else if(object.has("and")) {
            return new AndProcessor();
        } else if(object.has("or")) {
            return new OrProcessor();
        } else {
            throw new NotImplementedException("No support for '" + object + "'.");
        }
    }

    static interface ConditionProcessor {
        Condition process(JsonObject condition, MetadataRepository repository);
    }

    static class OrProcessor implements ConditionProcessor {

        @Override
        public Condition process(JsonObject condition, MetadataRepository repository) {
            JsonArray and = condition.get("or").getAsJsonArray();
            if (and.size() != 2) {
                throw new IllegalArgumentException("Malformed query (or is supposed to get 2 conditions - got " + and.size() + " -).");
            }
            Condition[] conditions = new Condition[2];
            for (int i = 0; i < and.size(); i++) {
                JsonObject object = and.get(i).getAsJsonObject();
                conditions[i] = getProcessor(object).process(object, repository);
            }
            return or(conditions[0], conditions[1]);
        }
    }

    static class AndProcessor implements ConditionProcessor {

        @Override
        public Condition process(JsonObject condition, MetadataRepository repository) {
            JsonArray and = condition.get("and").getAsJsonArray();
            if(and.size() != 2) {
                throw new IllegalArgumentException("Malformed query (and is supposed to get 2 conditions - got " + and.size() + " -).");
            }
            Condition[] conditions = new Condition[2];
            for (int i = 0; i < and.size(); i++) {
                JsonObject object = and.get(i).getAsJsonObject();
                conditions[i] = getProcessor(object).process(object, repository);
            }
            return and(conditions[0], conditions[1]);
        }
    }

    static class EqualsProcessor implements ConditionProcessor {

        @Override
        public Condition process(JsonObject condition, MetadataRepository repository) {
            JsonObject eq = condition.get("eq").getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> entries = eq.entrySet();
            TypedExpression left = null;
            String value = null;
            for (Map.Entry<String, JsonElement> entry : entries) {
                if("value".equals(entry.getKey())) {
                    value = eq.get("value").getAsString();
                } else if("field".equals(entry.getKey())) {
                    String path = entry.getValue().getAsString();
                    left = getField(repository, path);
                } else {
                    throw new NotImplementedException("No support for '" + entry.getKey() + "'.");
                }
            }
            if(left == null || value == null) {
                throw new IllegalArgumentException("Malformed query (missing field conditions in condition).");
            }
            return eq(left, value);
        }
    }

    private static Field getField(MetadataRepository repository, String path) {
        String typeName = StringUtils.substringBefore(path, "/");
        ComplexTypeMetadata type = repository.getComplexType(typeName);
        if(type == null) {
            throw new IllegalArgumentException("Malformed query (type '" + typeName + "' does not exist).");
        }
        return new Field(type.getField(StringUtils.substringAfter(path, "/")));
    }
}
