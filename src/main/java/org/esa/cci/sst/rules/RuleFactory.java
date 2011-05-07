/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.rules;

import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A simple rule factory.
 *
 * @author Ralf Quast
 */
public class RuleFactory {

    private final ConcurrentMap<String, Rule> bySimpleName;

    private RuleFactory() {
        bySimpleName = new ConcurrentHashMap<String, Rule>();
    }

    /**
     * Returns the unique instance of this factory.
     *
     * @return the unique instance of this factory.
     */
    public static RuleFactory getInstance() {
        return Holder.uniqueInstance;
    }

    /**
     * Returns a new instance of a renaming rule.
     *
     * @param targetName The target name ot the renaming rule.
     *
     * @return the renaming rule.
     */
    public Rule getRenamingRule(String targetName) {
        return new Renaming(targetName);
    }

    /**
     * Returns an instance of the rule the specification of which is supplied as
     * argument.
     *
     * @param specification The rule specification, which must be the simple name
     *                      of the rule class or a comma-separated list of simple
     *                      class names. The specification is right-associative.
     *
     * @return the rule.
     */
    public Rule getRule(String specification) {
        final String[] simpleNames = specification.split(",+");
        if (simpleNames.length == 1) {
            return getRuleBySimpleName(specification);
        }
        final Rule[] rules = new Rule[simpleNames.length];
        for (int i = 0; i < simpleNames.length; i++) {
            rules[i] = getRuleBySimpleName(simpleNames[i]);
        }
        return new RightAssociativeComposition(rules);
    }

    /**
     * Returns an instance of the rule the specification of which is supplied as
     * argument. The specified rule is composed with a renaming rule, which uses
     * the target name supplied as argument.
     *
     * @param specification The rule specification, which must be the simple name
     *                      of the rule class or a comma-separated list of simple
     *                      class names. The specification is right-associative.
     * @param targetName    The target name of the renaming rule.
     *
     * @return the rule.
     */
    public Rule getRule(String specification, String targetName) {
        final Rule r = getRenamingRule(targetName);
        final Rule s = getRule(specification);
        if (s instanceof RightAssociativeComposition) {
            return ((RightAssociativeComposition) s).append(r);
        }
        return new RightAssociativeComposition(r, s);
    }

    private Rule getRuleBySimpleName(String simpleName) {
        simpleName = simpleName.trim();
        try {
            if (!bySimpleName.containsKey(simpleName)) {
                final String name = getClass().getName().replace(getClass().getSimpleName(), simpleName);
                @SuppressWarnings({"unchecked"})
                final Class<? extends Rule> rule = (Class<? extends Rule>) Class.forName(name);
                bySimpleName.putIfAbsent(simpleName, rule.newInstance());
            }
            return bySimpleName.get(simpleName);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot create rule ''{0}''.", simpleName), e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot create rule ''{0}''.", simpleName), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot create rule ''{0}''.", simpleName), e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot create rule ''{0}''.", simpleName), e);
        }
    }

    private static class Holder {

        private static final RuleFactory uniqueInstance = new RuleFactory();
    }

}
