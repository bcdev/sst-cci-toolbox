/*
 * Copyright (C) 2012  Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA
 */

package org.esa.cci.sst.example;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template resolver.
 */
public class TemplateResolver {

    private final Properties properties;
    private final Pattern pattern;

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
     * @return the resolved property value.
     */
    public String resolveProperty(String key) {
        return resolve(properties.getProperty(key));
    }

    /**
     * Makes a three-pass replacement for all occurrences of <code>${property-name}</code>
     * in a given string with the value of the corresponding property.
     *
     * @param string the string.
     * @return the string with replacements made.
     */
    public final String resolve(String string) {
        return resolve(string, 3);
    }

    /**
     * Makes an n-pass replacement for all occurrences of <code>${property-name}</code>
     * in a given string with the value of the corresponding property.
     *
     * @param string the string of interest.
     * @param n      the number of passes.
     * @return the string with replacements made.
     */
    private String resolve(String string, int n) {
        if (string == null) {
            return string;
        }

        final StringBuilder sb = new StringBuilder(string);

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

    public String resolve(String line, Writer writer) throws IOException {
        if (line == null) {
            return line;
        }

        final StringBuilder sb = new StringBuilder(line);

        Matcher matcher = pattern.matcher(sb.toString());

        for (int i = 0; i < 1; i++) {
            int start = 0;
            while (matcher.find(start)) {
                start = matcher.start();
                final int end = matcher.end();

                final String key = sb.substring(start + 2, end - 1);
                final String replacement = properties.getProperty(key, System.getProperty(key));

                if (replacement != null) {
                    writer.write(sb.toString().substring(0, start));
                    final Scanner scanner = new Scanner(new File(replacement), "US-ASCII");
                    scanner.useLocale(Locale.US);
                    try {
                        while (scanner.hasNextLine()) {
                            writer.write(scanner.nextLine());
                            if (scanner.hasNextLine()) {
                                writer.write("\n");
                            }
                        }
                    } finally {
                        scanner.close();
                    }
                    sb.delete(0, end);
                    matcher = pattern.matcher(sb.toString());
                    start = 0;
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
