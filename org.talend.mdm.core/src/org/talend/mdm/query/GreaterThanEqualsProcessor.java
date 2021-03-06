package org.talend.mdm.query;

import com.amalto.core.query.user.Condition;
import com.amalto.core.query.user.TypedExpression;

import static com.amalto.core.query.user.UserQueryBuilder.gte;

class GreaterThanEqualsProcessor extends BasicConditionProcessor {

    static ConditionProcessor INSTANCE = new GreaterThanEqualsProcessor();

    private GreaterThanEqualsProcessor() {
    }

    @Override
    protected Condition buildCondition(TypedExpression expression, String value) {
        return gte(expression, value);
    }

    @Override
    protected Condition buildCondition(TypedExpression expression, TypedExpression value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getConditionElement() {
        return "gte"; //$NON-NLS-1
    }
}
