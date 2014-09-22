package org.talend.mdm.query;

import com.amalto.core.query.user.Condition;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.MetadataRepository;

interface ConditionProcessor {
    Condition process(JsonObject condition, MetadataRepository repository);
}
