package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;

final class P implements Rule {

    @Override
    public Column apply(Column sourceColumn) throws RuleException {
        return null;
    }

    @Override
    public Number apply(Number number, Column sourceColumn) throws RuleException {
        return null;
    }
}
