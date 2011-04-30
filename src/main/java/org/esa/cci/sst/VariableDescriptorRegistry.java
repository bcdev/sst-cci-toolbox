package org.esa.cci.sst;

import org.esa.cci.sst.data.VariableDescriptor;
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
 * A registry of {@link VariableDescriptor}s.
 * <p/>
 * The registry is used as an access point for all {@link VariableDescriptor}s that describe
 * the target variables in an MMD file.
 *
 * @author Ralf Quast
 */
public class VariableDescriptorRegistry {

    private final Map<String, VariableDescriptor> descriptorsByName;
    private final Map<VariableDescriptor, Rule> rulesByTarget;
    private final Map<VariableDescriptor, VariableDescriptor> descriptorsByTarget;

    private VariableDescriptorRegistry() {
        descriptorsByName = new HashMap<String, VariableDescriptor>();
        rulesByTarget = new HashMap<VariableDescriptor, Rule>();
        descriptorsByTarget = new HashMap<VariableDescriptor, VariableDescriptor>();
    }

    /**
     * Returns the unique instance of this registry.
     *
     * @return the unique instance of this registry.
     */
    public static VariableDescriptorRegistry getInstance() {
        return Holder.uniqueInstance;
    }

    public List<String> registerDescriptors(InputStream is) throws ParseException {
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
     * as argument to the descriptor supplied as argument.
     * <p/>
     * The target descriptor resulting from
     * <code>
     * rule.apply(descriptor)
     * </code>
     * is registered by name and associated with the rule and the descriptor supplied
     * as arguments.
     *
     * @param rule       The rule.
     * @param descriptor The descriptor.
     *
     * @return the descriptor resulting from {@code rule.apply(descriptor)}.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    public VariableDescriptor register(Rule rule, VariableDescriptor descriptor) throws RuleException {
        synchronized (this) {
            final VariableDescriptor result = rule.apply(descriptor);
            descriptorsByName.put(result.getName(), result);
            rulesByTarget.put(result, rule);
            descriptorsByTarget.put(result, descriptor);

            return result;
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
     * Returns the rule associated with the target descriptor supplied as argument.
     *
     * @param targetDescriptor The target descriptor.
     *
     * @return the rule associated with the descriptor supplied as argument.
     */
    public Rule getRule(VariableDescriptor targetDescriptor) {
        synchronized (rulesByTarget) {
            return rulesByTarget.get(targetDescriptor);
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

        private static final VariableDescriptorRegistry uniqueInstance = new VariableDescriptorRegistry();
    }


    private void parseIdentity(List<String> nameList, String sourceName) throws Exception {
        ensureSourceDescriptorExists(sourceName);
        final VariableDescriptor sourceDescriptor = getDescriptor(sourceName);
        final Rule rule = RuleFactory.getInstance().getRule("Identity");
        final VariableDescriptor targetDescriptor = register(rule, sourceDescriptor);
        nameList.add(targetDescriptor.getName());
    }

    private void parseRenaming(List<String> nameList, String targetName, String sourceName) throws Exception {
        ensureSourceDescriptorExists(sourceName);
        final VariableDescriptor sourceDescriptor = getDescriptor(sourceName);
        final Rule rule = RuleFactory.getInstance().getRenamingRule(targetName);
        final VariableDescriptor targetDescriptor = register(rule, sourceDescriptor);
        nameList.add(targetDescriptor.getName());
    }

    private void parseRule(List<String> nameList, String targetName, String sourceName, String spec) throws Exception {
        ensureSourceDescriptorExists(sourceName);
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

    private void ensureSourceDescriptorExists(String sourceName) {
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

}
