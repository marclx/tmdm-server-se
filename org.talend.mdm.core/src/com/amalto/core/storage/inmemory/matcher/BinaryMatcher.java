package com.amalto.core.storage.inmemory.matcher;

import com.amalto.core.query.user.Predicate;
import com.amalto.core.storage.record.DataRecord;
import org.apache.commons.lang.NotImplementedException;

public class BinaryMatcher implements Matcher {

    private final Matcher left;

    private final Predicate predicate;

    private final Matcher right;

    public BinaryMatcher(Matcher left, Predicate predicate, Matcher right) {
        this.left = left;
        this.predicate = predicate;
        this.right = right;
    }

    @Override
    public boolean match(DataRecord record) {
        if (predicate == Predicate.AND) {
            return left.match(record) && right.match(record);
        } else if (predicate == Predicate.OR) {
            return left.match(record) || right.match(record);
        } else {
            throw new NotImplementedException();
        }
    }
}
