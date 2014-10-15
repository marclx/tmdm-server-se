package org.talend.mdm.query;

import com.amalto.core.query.user.TypedExpression;
import com.google.gson.JsonObject;
import org.talend.mdm.commmon.metadata.MetadataRepository;

/**
 *
 */
interface TypedExpressionProcessor {
    TypedExpression process(JsonObject element, MetadataRepository repository);
}
