package org.talend.mdm.query;

import com.amalto.core.query.user.Expression;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 *
 */
public class QueryParser {

    private final MetadataRepository repository;

    private QueryParser(MetadataRepository repository) {
        this.repository = repository;
    }

    public static QueryParser newParser(MetadataRepository repository) {
        if(repository == null) {
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
        GsonBuilder builder = new GsonBuilder();
        Deserializer deserializer = new Deserializer(repository);
        builder.registerTypeAdapter(Expression.class, deserializer);
        Gson gson = builder.create();
        return gson.fromJson(new InputStreamReader(query), Expression.class);
    }

}
