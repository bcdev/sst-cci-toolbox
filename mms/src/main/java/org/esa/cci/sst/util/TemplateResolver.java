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

package org.esa.cci.sst.util;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for replacing property names occurring in s template string with
 * property values.
 *
 * @author Ralf Quast
 *
 * TODO - this class is duplicated in org.esa.sst.example. Remove from there and copy test class from there to here
 */
public class TemplateResolver {

    private final Properties properties;
    private final Pattern pattern;

    /**
     * Creates a new instance of this class, using the properties supplied as argument
     * for resolving templates.
     *
     * @param properties The properties used for resolving templates.
     */
    public TemplateResolver(Properties properties) {
        this(properties, "\\$\\{[\\w\\-\\.]+\\}");
    }

    private TemplateResolver(Properties properties, String regex) {
        this.properties = properties;
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Resolves the property with the given key.
     *
     * @param key the property key.
     *
     * @return the resolved property value.
     */
    public String resolveProperty(String key) {
        return resolve(properties.getProperty(key));
    }

    /**
     * Makes a two-pass replacement for all occurrences of <code>${property-name}</code>
     * in a given template string with the value of the corresponding property.
     *
     * @param template The template string.
     *
     * @return the template string with replacements made.
     */
    public final String resolve(String template) {
        return resolve(template, 2);
    }

    /**
     * Makes an n-pass replacement for all occurrences of <code>${property-name}</code>
     * in a given template string with the value of the corresponding property.
     *
     * @param template The template string.
     * @param n        The number of passes.
     *
     * @return the template string with replacements made.
     */
    private String resolve(String template, int n) {
        if (template == null) {
            return template;
        }

        final StringBuilder sb = new StringBuilder(template);

        Matcher matcher = pattern.matcher(sb.toString());

        for (int i = 0; i < n; i++) {
            int start = 0;
            while (matcher.find(start)) {
                start = matcher.start();
                final int end = matcher.end();

                final String key = sb.substring(start + 2, end - 1);
                final String replacement = properties.getProperty(key, System.getProperty(key));

                if (replacement != null) {
                    sb.replace(start, end, replacement);
                    matcher = pattern.matcher(sb.toString());
                    start += replacement.length();
                } else {
                    start += key.length() + 3;
                }
            }
        }

        return sb.toString();
    }

    public boolean canResolve(String string) {
        return !pattern.matcher(resolve(string)).find();
    }

    public boolean isResolved(String string) {
        return !pattern.matcher(string).find();
    }

}
