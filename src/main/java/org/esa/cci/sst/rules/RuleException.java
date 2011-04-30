package org.esa.cci.sst.rules;

/**
 * The exception thrown by rules.
 *
 * @author Ralf Quast
 */
public final class RuleException extends Exception {

    RuleException(String message) {
        super(message);
    }

    RuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
