package org.talend.mdm.query;

import com.amalto.core.query.user.Condition;
import com.amalto.core.query.user.TypedExpression;

import static com.amalto.core.query.user.UserQueryBuilder.eq;

class EqualsProcessor extends BasicConditionProcessor {

    static ConditionProcessor INSTANCE = new EqualsProcessor();

    private EqualsProcessor() {
    }

    @Override
    protected Condition buildCondition(TypedExpression expression, String value) {
        return eq(expression, value);
    }

    @Override
    protected Condition buildCondition(TypedExpression expression, TypedExpression value) {
        return eq(expression, value);
    }

    @Override
    protected String getConditionElement() {
        return "eq"; //$NON-NLS-1
    }
}
