/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on 2013.04.26. by Gabor
 */
package org.knime.base.node.rules.engine.manipulator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.base.node.rules.engine.Rule.BooleanConstants;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.util.ManipulatorProvider;
import org.knime.core.util.Pair;

/**
 * A {@link ManipulatorProvider} for the Rule Engine nodes.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public class RuleManipulatorProvider implements ManipulatorProvider {
    private static final RuleManipulatorProvider PROVIDER = new RuleManipulatorProvider(true, true);

    private static final RuleManipulatorProvider VARIABLE_PROVIDER = new RuleManipulatorProvider(true, false);

    private static final RuleManipulatorProvider PMML_PROVIDER = new RuleManipulatorProvider(false, true);

    /**
     * @return the provider for rules with missing and regexes.
     */
    public static RuleManipulatorProvider getProvider() {
        return PROVIDER;
    }

    /**
     * @return the provider for PMML rules.
     * @since 2.9
     */
    public static RuleManipulatorProvider getPMMLProvider() {
        return PMML_PROVIDER;
    }

    /**
     * The provider applicable for the node.
     *
     * @param rns The type of node.
     * @return The provider.
     */
    public static RuleManipulatorProvider getProvider(final RuleNodeSettings rns) {
        switch (rns) {
            case PMMLRule:
                return PMML_PROVIDER;
            case RuleEngine:
                return PROVIDER;
            case RuleFilter:
                return PROVIDER;
            case RuleSplitter:
                return PROVIDER;
            case VariableRule:
                return VARIABLE_PROVIDER;
            default:
                throw new IllegalArgumentException("Unknown node type: " + rns);
        }
    }

    /**
     * @return the provider for rules without columns. (So no missing operator.)
     * @since 2.9
     */
    public static RuleManipulatorProvider getVariableProvider() {
        return VARIABLE_PROVIDER;
    }

    private final EnumSet<Operators> m_infixOps = EnumSet.of(Operators.AND, /*Operators.CONTAINS,*/Operators.EQ,
        Operators.GE, Operators.GT, Operators.IN, Operators.LE, Operators.LIKE, Operators.LT, Operators.MATCHES,
        Operators.OR, Operators.XOR);

    private final EnumSet<Operators> m_prefixOps = EnumSet.of(Operators.MISSING, Operators.NOT);

    private final EnumSet<Operators> m_numericOps = EnumSet.of(Operators.EQ, Operators.GE, Operators.GT, Operators.LE,
        Operators.LT);

    private final EnumSet<Operators> m_columnOps = EnumSet.of(Operators.MISSING);

    private final EnumSet<Operators> m_logicalOps = EnumSet.of(Operators.NOT, Operators.AND, Operators.OR,
        Operators.XOR);

    private final EnumSet<Operators> m_collectionOps = EnumSet.of(Operators.IN);

    private final EnumSet<Operators> m_stringOps = EnumSet
        .of(Operators.LIKE, Operators.MATCHES/*, Operators.CONTAINS*/);

    private final SortedMap<String, Collection<Manipulator>> m_manipulators =
        new TreeMap<String, Collection<Manipulator>>();

    private final Collection<Manipulator> m_allManipulators = createCollection();

    private final EnumMap<Operators, String> m_operatorDescriptions = new EnumMap<Operators, String>(Operators.class);
    {
        String shortCircuit = "<br/>(Short-circuit evaluation.)";
        String connectiveFormat =
            "Logical %1$s of two boolean expressions.%nYou can use this in a sequence, like "
                + "<tt>A %2$s B %2$s C</tt> without parenthesis, but it has no precedence regarding to %3$s or %4$s, "
                + "so you have to use parenthesis around the logical connectives if you want to combine them.";
        String compareFormat = "Left %s right";
        m_operatorDescriptions.put(Operators.AND, String.format(connectiveFormat, "and", "AND", "OR", "XOR")
            + shortCircuit);
        m_operatorDescriptions.put(Operators.EQ, String.format(compareFormat, "="));
        m_operatorDescriptions.put(Operators.GE, String.format(compareFormat, "&ge;"));
        m_operatorDescriptions.put(Operators.GT, String.format(compareFormat, "&gt;"));
        m_operatorDescriptions.put(Operators.IN, "Checks whether the value of the left expression is contained in "
            + "the list of right expressions.<br/>For example: " + "<tt>$Col0$ IN (\"Hello\", \"World\")</tt>");
        m_operatorDescriptions.put(Operators.LE, String.format(compareFormat, "&le;"));
        m_operatorDescriptions.put(Operators.LIKE, "Checks whether the value of the left expression is "
            + "like the wildcard pattern defined by the right expression"
            + "<br/>For example: <tt>$Col0$ LIKE \"H?llo*\"</tt>");
        m_operatorDescriptions.put(Operators.LT, String.format(compareFormat, "&lt;"));
        m_operatorDescriptions.put(Operators.MATCHES,
            "The string on the left matches the regular expression on the right. The regular expression can be " +
            "enclosed in double quotes (\") or in slashes (/). The latter allows for escaping with backslash, e.g. " +
            "if you want to match a slash itself.");
        m_operatorDescriptions.put(Operators.MISSING,
            "Checks whether the argument (a column) contains a missing value or not.");
        m_operatorDescriptions.put(Operators.NOT, "Logical negation of a boolean expression.");
        m_operatorDescriptions.put(Operators.OR, String.format(connectiveFormat, "or", "OR", "AND", "XOR")
            + shortCircuit);
        m_operatorDescriptions.put(Operators.XOR, String.format(connectiveFormat, "exclusive or", "XOR", "AND", "OR"));
    }

    /**
     * Constructs the default manipulators based on the information about {@link Operators}.
     *
     * @param includeRegEx Include the string operators, or not.
     * @param includeMissing Include the missing operator, or not.
     */
    protected RuleManipulatorProvider(final boolean includeRegEx, final boolean includeMissing) {
        super();
        final List<Pair<String, EnumSet<Operators>>> categs =
            Arrays.asList(new Pair<String, EnumSet<Operators>>("Numeric", m_numericOps),
                new Pair<String, EnumSet<Operators>>("Logical", m_logicalOps), new Pair<String, EnumSet<Operators>>(
                    "Collection", m_collectionOps), new Pair<String, EnumSet<Operators>>("String", m_stringOps),
                new Pair<String, EnumSet<Operators>>("Column", m_columnOps));
        for (final Pair<String, EnumSet<Operators>> pair : categs) {
            if (pair.getSecond().equals(m_stringOps) && !includeRegEx) {
                continue;
            }
            if (pair.getSecond().equals(m_columnOps) && !includeMissing) {
                continue;
            }
            m_manipulators.put(pair.getFirst(), createCollection());
            for (final Operators op : pair.getSecond()) {
                final Manipulator manipulator;
                if (m_infixOps.contains(op)) {
                    manipulator =
                        new InfixManipulator(op.toString(), pair.getFirst(), "? " + op + " ?",
                            m_operatorDescriptions.get(op), Boolean.class);
                } else if (m_prefixOps.contains(op)) {
                    manipulator =
                        new PrefixUnaryManipulator(op.toString(), pair.getFirst(), op + " ?",
                            m_operatorDescriptions.get(op), Boolean.class);
                } else {
                    throw new IllegalStateException("Unknown operator: " + op);
                }
                m_manipulators.get(pair.getFirst()).add(manipulator);
                m_allManipulators.add(manipulator);
            }
        }
        final String constantCategory = "Constant";
        Collection<Manipulator> constantManipulators = createCollection();
        m_manipulators.put(constantCategory, constantManipulators);
        for (BooleanConstants constant : BooleanConstants.values()) {
            Manipulator manipulator =
                new ConstantManipulator(constant.toString(), constantCategory, constant.toString(), "The "
                    + constant.name().toLowerCase() + " logical value.", Boolean.class);
            constantManipulators.add(manipulator);
            m_allManipulators.add(manipulator);
        }
        m_manipulators.put(constantCategory, constantManipulators);
        m_manipulators.put(ALL_CATEGORY, m_allManipulators);
    }

    /**
     * @return A new {@link SortedSet} with comparator based on {@link Manipulator#getDisplayName()}.
     */
    private SortedSet<Manipulator> createCollection() {
        return new TreeSet<Manipulator>(new Comparator<Manipulator>() {
            @Override
            public int compare(final Manipulator o1, final Manipulator o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getCategories() {
        return Collections.unmodifiableCollection(m_manipulators.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Manipulator> getManipulators(final String category) {
        return m_manipulators.containsKey(category) ? Collections.unmodifiableCollection(m_manipulators.get(category))
            : Collections.<Manipulator> emptySet();
    }
}
