package org.esa.cci.sst.rules;

/**
 * Rule exception.
 */
public class RuleException extends Exception {

    public RuleException(String message) {
        super(message);
    }

    public RuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
