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

package org.esa.cci.sst;

import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.Rule;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.rules.RuleFactory;
import ucar.ma2.Array;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * A registry for column {@link Item}s.
 * <p/>
 * The registry is used as an access point for all {@link Item}s that describe
 * the target columns in an MMD file.
 *
 * @author Ralf Quast
 */
public class ColumnRegistry {

    private final Map<String, Item> columnsByName;
    private final Map<Item, Rule> rulesByTarget;
    private final Map<Item, Item> columnsByTarget;

    /**
     * Creates a new instance of this class.
     */
    public ColumnRegistry() {
        columnsByName = new HashMap<String, Item>();
        rulesByTarget = new HashMap<Item, Rule>();
        columnsByTarget = new HashMap<Item, Item>();
    }


    public List<String> registerColumns(File file) throws FileNotFoundException, ParseException {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return registerColumns(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Registers columns defined in a 'mmd-variables.cfg' configuration file.
     *
     * @param is The input stream associated with the configuration file.
     *
     * @return the list of column names being registered.
     *
     * @throws ParseException when the configuration could not be parsed.
     */
    public List<String> registerColumns(InputStream is) throws ParseException {
        synchronized (this) {
            final Scanner scanner = new Scanner(is, "US-ASCII");
            scanner.useLocale(Locale.ENGLISH);

            try {
                final ArrayList<String> nameList = new ArrayList<String>();
                for (int lineNumber = 0; scanner.hasNextLine(); lineNumber++) {
                    final String line = stripComment(scanner.nextLine()).trim();
                    final String[] tokens = line.split("\\s+");
                    try {
                        switch (tokens.length) {
                            case 1:
                                if (tokens[0].isEmpty()) {
                                    break;
                                }
                                // identity
                                parseIdentity(nameList, tokens[0]);
                                break;
                            case 2:
                                // variable renaming
                                parseRenaming(nameList, tokens[0], tokens[1]);
                                break;
                            default:
                                // more complex rule
                                parseRule(nameList, tokens[0], tokens[1], tokens[2]);
                                break;
                        }
                    } catch (Exception e) {
                        throw new ParseException(e.getMessage(), lineNumber);
                    }
                }
                return nameList;
            } finally {
                scanner.close();
            }
        }
    }

    /**
     * Registers a column by name.
     *
     * @param column The column to be registered.
     */
    public void register(Item column) {
        synchronized (this) {
            final Item previous = columnsByName.put(column.getName(), column);
            if (previous != null) {
                columnsByTarget.remove(previous);
                rulesByTarget.remove(previous);
            }
        }
    }

    /**
     * Registers the target column, which results from applying the rule supplied
     * as argument to the source column supplied as argument.
     * <p/>
     * The target column resulting from
     * <code>
     * rule.apply(column)
     * </code>
     * is registered by name and associated with the rule and the column supplied
     * as arguments.
     *
     * @param rule         The rule.
     * @param sourceColumn The source column.
     *
     * @return the column resulting from {@code rule.apply(sourceColumn)}.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    public Item register(Rule rule, Item sourceColumn) throws RuleException {
        synchronized (this) {
            final Item targetColumn = rule.apply(sourceColumn);
            columnsByName.put(targetColumn.getName(), targetColumn);
            rulesByTarget.put(targetColumn, rule);
            columnsByTarget.put(targetColumn, sourceColumn);

            return targetColumn;
        }
    }

    /**
     * Clears the registry.
     */
    public void clear() {
        synchronized (this) {
            columnsByTarget.clear();
            rulesByTarget.clear();
            columnsByName.clear();
        }
    }

    /**
     * Returns the variable column associated with the name supplied as argument.
     *
     * @param name The name of the variable column.
     *
     * @return the variable column associated with the name supplied as argument.
     */
    public Item getColumn(String name) {
        synchronized (columnsByName) {
            return columnsByName.get(name);
        }
    }

    /**
     * Returns a converter suitable for numeric conversions from a number complying
     * with the source column associated with the target column supplied as
     * argument into a number complying with the target column.
     *
     * @param targetColumn The target column.
     *
     * @return a converter suitable for numeric conversions into numbers complying
     *         with the target column.
     */
    public Converter getConverter(Item targetColumn) {
        synchronized (this) {
            return new ConverterImpl(rulesByTarget.get(targetColumn), columnsByTarget.get(targetColumn));
        }
    }

    /**
     * Returns the source column associated with the target column supplied as argument.
     *
     * @param targetColumn The target column.
     *
     * @return the source column associated with the target column supplied as argument.
     */
    public Item getSourceColumn(Item targetColumn) {
        synchronized (columnsByTarget) {
            return columnsByTarget.get(targetColumn);
        }
    }

    /**
     * Inquires the registry for a column.
     *
     * @param name The column name.
     *
     * @return {@code true} if a column has been registered with the name supplied,
     *         {@code false} otherwise.
     */
    public boolean hasColumn(String name) {
        synchronized (columnsByName) {
            return columnsByName.containsKey(name);
        }
    }

    private void parseIdentity(List<String> nameList, String sourceName) throws Exception {
        ensureSourceColumnIsRegistered(sourceName);
        final Item sourceColumn = getColumn(sourceName);
        final Rule rule = RuleFactory.getInstance().getRule("Identity");
        final Item targetColumn = register(rule, sourceColumn);
        nameList.add(targetColumn.getName());
    }

    private void parseRenaming(List<String> nameList, String targetName, String sourceName) throws Exception {
        ensureSourceColumnIsRegistered(sourceName);
        final Item sourceColumn = getColumn(sourceName);
        final Rule rule = RuleFactory.getInstance().getRenamingRule(targetName);
        final Item targetColumn = register(rule, sourceColumn);
        nameList.add(targetColumn.getName());
    }

    private void parseRule(List<String> nameList, String targetName, String sourceName, String spec) throws Exception {
        ensureSourceColumnIsRegistered(sourceName);
        final Item sourceColumn = getColumn(sourceName);
        final Rule rule;
        if (targetName.equals(sourceName)) {
            rule = RuleFactory.getInstance().getRule(spec);
        } else {
            rule = RuleFactory.getInstance().getRule(spec, targetName);
        }
        final Item targetColumn = register(rule, sourceColumn);
        nameList.add(targetColumn.getName());
    }

    private void ensureSourceColumnIsRegistered(String sourceName) {
        if (!hasColumn(sourceName)) {
            throw new IllegalArgumentException("Unknown column '" + sourceName + "'.");
        }
    }

    private String stripComment(String line) {
        final int i = line.indexOf('#');
        if (i != -1) {
            return line.substring(0, i);
        }
        return line;
    }

    private static class ConverterImpl implements Converter {

        private final Rule rule;
        private final Item sourceColumn;

        public ConverterImpl(Rule rule, Item sourceColumn) {
            this.rule = rule;
            this.sourceColumn = sourceColumn;
        }

        @Override
        public Array apply(Array numbers) throws RuleException {
            return rule.apply(numbers, sourceColumn);
        }
    }
}
