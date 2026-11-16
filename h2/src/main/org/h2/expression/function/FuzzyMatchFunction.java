/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 */
package org.h2.expression.function;

import org.h2.engine.SessionLocal;
import org.h2.expression.Expression;
import org.h2.expression.TypedValueExpression;
import org.h2.message.DbException;
import org.h2.util.FuzzyMatchUtils;
import org.h2.value.TypeInfo;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;

/**
 * A fuzzy string matching function using bounded Levenshtein distance.
 */

public final class FuzzyMatchFunction extends FunctionN {

    public FuzzyMatchFunction() {
        super(new Expression[3]);
    }

    @Override
    protected Value getValue(SessionLocal session, Value v1, Value v2, Value v3) {
        boolean matches = FuzzyMatchUtils.matches(
                v1.getString(),
                v2.getString(),
                v3.getInt());

        return ValueBoolean.get(matches);
    }

    /**
     * Function for optimization. If all constants (all vals known before
     * execution), optimize the call and check if there was a null value.
     * If the right amount of args not given, throw error. 
     */
    @Override
    public Expression optimize(SessionLocal session) {
        boolean allConst = optimizeArguments(session, true);

        if (args.length != 3) {
            throw DbException.get(
                    org.h2.api.ErrorCode.INVALID_PARAMETER_COUNT_2,
                    getName(), "3");
        }

        type = TypeInfo.TYPE_BOOLEAN;

        if (allConst) {
            return TypedValueExpression.getTypedIfNull(getValue(session), type);
        }

        return this;
    }

    @Override
    public String getName() {
        return "FUZZY_MATCH";
    }
}