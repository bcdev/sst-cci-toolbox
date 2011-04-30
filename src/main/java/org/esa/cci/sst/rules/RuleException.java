package org.esa.cci.sst.rules;

/**
 * Rule exception.
 */
public class RuleException extends Exception {

    RuleException(String message) {
        super(message);
    }

    RuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
