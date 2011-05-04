package org.esa.cci.sst;

import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.Rule;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.rules.RuleFactory;

import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * A registry for {@link Descriptor}s.
 * <p/>
 * The registry is used as an access point for all {@link Descriptor}s that describe
 * the target variables in an MMD file.
 *
 * @author Ralf Quast
 */
public class DescriptorRegistry {

    private final Map<String, VariableDescriptor> descriptorsByName;
    private final Map<VariableDescriptor, Rule> rulesByTarget;
    private final Map<VariableDescriptor, VariableDescriptor> descriptorsByTarget;

    private DescriptorRegistry() {
        descriptorsByName = new HashMap<String, VariableDescriptor>();
        rulesByTarget = new HashMap<VariableDescriptor, Rule>();
        descriptorsByTarget = new HashMap<VariableDescriptor, VariableDescriptor>();
    }

    /**
     * Returns the unique instance of this registry.
     *
     * @return the unique instance of this registry.
     */
    public static DescriptorRegistry getInstance() {
        return Holder.uniqueInstance;
    }

    /**
     * Registers descriptors defined in a 'mmd-variables.cfg' configuration file.
     *
     * @param is The input stream associated with the configuration file.
     *
     * @return the list of descriptor names being registered.
     *
     * @throws ParseException when the configuration could not be parsed.
     */
    public List<String> registerDescriptors(InputStream is) throws ParseException {
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
     * Registers a descriptor by name.
     *
     * @param descriptor The descriptor to be registered.
     */
    public void register(VariableDescriptor descriptor) {
        synchronized (this) {
            final VariableDescriptor previous = descriptorsByName.put(descriptor.getName(), descriptor);
            if (previous != null) {
                descriptorsByTarget.remove(previous);
                rulesByTarget.remove(previous);
            }
        }
    }

    /**
     * Registers the target descriptor, which results from applying the rule supplied
     * as argument to the source descriptor supplied as argument.
     * <p/>
     * The target descriptor resulting from
     * <code>
     * rule.apply(descriptor)
     * </code>
     * is registered by name and associated with the rule and the descriptor supplied
     * as arguments.
     *
     * @param rule             The rule.
     * @param sourceDescriptor The source descriptor.
     *
     * @return the descriptor resulting from {@code rule.apply(sourceDescriptor)}.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    public VariableDescriptor register(Rule rule, VariableDescriptor sourceDescriptor) throws RuleException {
        synchronized (this) {
            final VariableDescriptor targetDescriptor = rule.apply(sourceDescriptor);
            descriptorsByName.put(targetDescriptor.getName(), targetDescriptor);
            rulesByTarget.put(targetDescriptor, rule);
            descriptorsByTarget.put(targetDescriptor, sourceDescriptor);

            return targetDescriptor;
        }
    }

    /**
     * Clears the registry.
     */
    public void clear() {
        synchronized (this) {
            descriptorsByTarget.clear();
            rulesByTarget.clear();
            descriptorsByName.clear();
        }
    }

    /**
     * Returns the variable descriptor associated with the name supplied as argument.
     *
     * @param name The name of the variable descriptor.
     *
     * @return the variable descriptor associated with the name supplied as argument.
     */
    public VariableDescriptor getDescriptor(String name) {
        synchronized (descriptorsByName) {
            return descriptorsByName.get(name);
        }
    }

    /**
     * Returns a converter suitable for numeric conversions from a number complying
     * with the source descriptor associated with the target descriptor supplied as
     * argument into a number complying with the target descriptor.
     *
     * @param targetDescriptor The target descriptor.
     *
     * @return a converter suitable for numeric conversions into numbers complying
     *         with the target descriptor.
     */
    public Converter getConverter(VariableDescriptor targetDescriptor) {
        synchronized (this) {
            return new ConverterImpl(rulesByTarget.get(targetDescriptor), descriptorsByTarget.get(targetDescriptor));
        }
    }

    /**
     * Returns the source descriptor associated with the target descriptor supplied as argument.
     *
     * @param targetDescriptor The target descriptor.
     *
     * @return the source descriptor associated with the target descriptor supplied as argument.
     */
    public VariableDescriptor getSourceDescriptor(VariableDescriptor targetDescriptor) {
        synchronized (descriptorsByTarget) {
            return descriptorsByTarget.get(targetDescriptor);
        }
    }

    /**
     * Inquires the registry for a descriptor.
     *
     * @param name The descriptor name.
     *
     * @return {@code true} if a descriptor has been registered with the name supplied,
     *         {@code false} otherwise.
     */
    public boolean hasDescriptor(String name) {
        synchronized (descriptorsByName) {
            return descriptorsByName.containsKey(name);
        }
    }

    private static class Holder {

        private static final DescriptorRegistry uniqueInstance = new DescriptorRegistry();
    }


    private void parseIdentity(List<String> nameList, String sourceName) throws Exception {
        ensureSourceDescriptorIsRegistered(sourceName);
        final VariableDescriptor sourceDescriptor = getDescriptor(sourceName);
        final Rule rule = RuleFactory.getInstance().getRule("Identity");
        final VariableDescriptor targetDescriptor = register(rule, sourceDescriptor);
        nameList.add(targetDescriptor.getName());
    }

    private void parseRenaming(List<String> nameList, String targetName, String sourceName) throws Exception {
        ensureSourceDescriptorIsRegistered(sourceName);
        final VariableDescriptor sourceDescriptor = getDescriptor(sourceName);
        final Rule rule = RuleFactory.getInstance().getRenamingRule(targetName);
        final VariableDescriptor targetDescriptor = register(rule, sourceDescriptor);
        nameList.add(targetDescriptor.getName());
    }

    private void parseRule(List<String> nameList, String targetName, String sourceName, String spec) throws Exception {
        ensureSourceDescriptorIsRegistered(sourceName);
        final VariableDescriptor sourceDescriptor = getDescriptor(sourceName);
        final Rule rule;
        if (targetName.equals(sourceName)) {
            rule = RuleFactory.getInstance().getRule(spec);
        } else {
            rule = RuleFactory.getInstance().getRule(spec, targetName);
        }
        final VariableDescriptor targetDescriptor = register(rule, sourceDescriptor);
        nameList.add(targetDescriptor.getName());
    }

    private void ensureSourceDescriptorIsRegistered(String sourceName) {
        if (!hasDescriptor(sourceName)) {
            throw new IllegalArgumentException("Unknown variable descriptor '" + sourceName + "'.");
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
        private final VariableDescriptor sourceDescriptor;

        public ConverterImpl(Rule rule, VariableDescriptor sourceDescriptor) {
            this.rule = rule;
            this.sourceDescriptor = sourceDescriptor;
        }

        @Override
        public Number apply(Number number) throws RuleException {
            return rule.apply(number, sourceDescriptor);
        }
    }
}
