package org.talend.mdm.query;

import com.amalto.core.query.user.Expression;
import com.google.gson.GsonBuilder;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import java.io.*;

/**
 *
 */
public class QueryParser {

    private final GsonBuilder builder;

    private QueryParser(MetadataRepository repository) {
        builder = new GsonBuilder();
        builder.registerTypeAdapter(Expression.class, new Deserializer(repository));
    }

    public static QueryParser newParser(MetadataRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Metadata repository cannot be null.");
        }
        return new QueryParser(repository);
    }

    public Expression parse(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null.");
        }
        try {
            return parse(new ByteArrayInputStream(query.getBytes("UTF-8"))); //$NON-NLS-1$
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding when parsing query from string.", e);
        }
    }

    public Expression parse(InputStream query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null.");
        }
        return parse(new InputStreamReader(query));
    }

    public Expression parse(Reader query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null.");
        }
        return builder.create().fromJson(query, Expression.class);
    }

}
