package org.esa.cci.sst;

/**
 * For filtering target variables in MMD variable configuration files.
 *
 * @author Ralf Quast
 */
public interface Predicate {

    boolean test(String s);
}
