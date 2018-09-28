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
 * Created on 2013.08.22. by Gabor Bakos
 */
package org.knime.base.node.rules.engine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.pmml.PMMLRuleEditorNodeDialog;
import org.knime.base.node.rules.engine.rsyntax.AbstractRuleParser;
import org.knime.base.node.rules.engine.rsyntax.PMMLRuleParser;
import org.knime.base.node.rules.engine.rsyntax.VariableRuleParser;
import org.knime.base.node.util.JavaScriptingCompletionProvider;
import org.knime.core.node.util.rsyntaxtextarea.KnimeCompletionProvider;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 * The settings to configure the config dialog and the parsers.
 *
 * @author Gabor Bakos
 */
public enum RuleNodeSettings {
    /** The Rule Engine node. */
    RuleEngine,
    /** The Rule-based Row Filter node. */
    RuleFilter,
    /** The Rule-based Row Splitter node. */
    RuleSplitter,
    /** The Rule Engine Variable node. */
    VariableRule {
        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Operators> supportedOperators() {
            return Collections.unmodifiableSet(NO_MISSING);
        }
    },
    /** The PMML Rule Editor node. */
    PMMLRule {
        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Operators> supportedOperators() {
            return Collections.unmodifiableSet(NO_REGEX);
        }
    };

    private static final Set<Operators> ALL_OPS = EnumSet.allOf(Operators.class);

    private static final Set<Operators> NO_MISSING = EnumSet.allOf(Operators.class);

    private static final Set<Operators> NO_REGEX = EnumSet.allOf(Operators.class);
    static {
        NO_MISSING.remove(Operators.MISSING);
        NO_REGEX.remove(Operators.MATCHES);
        NO_REGEX.remove(Operators.LIKE);
    }

    /**
     * @return Allow {@link Operators#MISSING} or not in the parsed expressions.
     */
    public boolean allowMissing() {
        return this != VariableRule;
    }

    /**
     * @return Allow columns or not in the parsed expressions.
     */
    public boolean allowColumns() {
        return this != VariableRule;
    }

    /**
     * @return Allow table properties (ROWID, ROWINDEX, ROWCOUNT) or not in the parsed expressions.
     */
    public boolean allowTableProperties() {
        return this != VariableRule && this != PMMLRule;
    }

    /**
     * @return Allow the regular expression operators ({@link Operators#LIKE}, {@link Operators#MATCHES}) in the parsed
     *         expressions or not?
     */
    public boolean allowRegexes() {
        return this != PMMLRule;
    }

    /**
     * @return {@code true} iff the outcomes can only be either TRUE or FALSE.
     */
    public boolean onlyBooleanOutcome() {
        return this == RuleSplitter || this == RuleFilter;
    }

    /**
     * @return May the outcomes contain Boolean values (TRUE or FALSE)?
     */
    public boolean allowBooleanOutcome() {
        return this != VariableRule;
    }

    /**
     * @return May the parsed expressions contain flow variables?
     */
    public boolean allowFlowVariables() {
        return this != PMMLRule;
    }

    /**
     * @return The {@link RSyntaxTextArea#setSyntaxEditingStyle(String) syntax style key} for the editor.
     */
    public String syntaxKey() {
        switch (this) {
            case PMMLRule:
                return PMMLRuleParser.SYNTAX_STYLE_RULE_PMML;
            case RuleEngine:
            case RuleFilter:
            case RuleSplitter:
                return AbstractRuleParser.SYNTAX_STYLE_RULE;
            case VariableRule:
                return VariableRuleParser.SYNTAX_STYLE_VARIABLE_RULE;
            default:
                throw new UnsupportedOperationException("Not supported: " + this);
        }
    }

    /**
     * @return The supported operators.
     */
    public Set<Operators> supportedOperators() {
        return Collections.unmodifiableSet(ALL_OPS);
    }

    /**
     * @return The {@link CompletionProvider} for the node.
     */
    public KnimeCompletionProvider completionProvider() {
        if (this == PMMLRule) {
            return new KnimeCompletionProvider() {
                @Override
                public String escapeColumnName(final String colName) {
                    return "$" + colName + "$";
                }

                @Override
                public String escapeFlowVariableName(final String varName) {
                    throw new UnsupportedOperationException("Flow variables are not supported.");
                }

            };
        }
        if (this == VariableRule) {
            return new KnimeCompletionProvider() {
                @Override
                public String escapeColumnName(final String colName) {
                    throw new UnsupportedOperationException("No columns!");
                }

                @Override
                public String escapeFlowVariableName(final String varName) {
                    return "$${" + varName + "}$$";
                }
            };
        }
        return new JavaScriptingCompletionProvider();
    }

    /**
     * @return The label of the checkbox.
     * @deprecated No longer used.
     * @see #topText()
     * @see #bottomText()
     * @see #selectionText()
     */
    @Deprecated
    public String inclusionText() {
        if (this == RuleFilter) {
            return "include if first matching rule is TRUE";
        }
        if (this == RuleSplitter) {
            return "TRUE to first, FALSE to second output table";
        }
        return null;
    }

    /**
     * @return The text for the option moving the TRUE matches to the top (or single) outport.
     */
    public String topText() {
        return textForFilters("Include TRUE matches", "first output table");
    }

    /**
     * @return The text for the option moving the TRUE matches to the bottom (or non-existing) outport.
     */
    public String bottomText() {
        return textForFilters("Exclude TRUE matches", "second output table");
    }

    /**
     * @return For filters, splitters the text before the options.
     */
    public String selectionText() {
        return textForFilters("", "TRUE matches go to ");
    }

    private String textForFilters(final String filter, final String splitter) {
        if (this == RuleFilter) {
            return filter;
        }
        if (this == RuleSplitter) {
            return splitter;
        }
        assert false;
        return null;
    }

    /**
     * @return Do we have an output? {@code true} Or we just filter? ({@code false})
     */
    public boolean hasOutput() {
        return this != RuleFilter && this != RuleSplitter;
    }

    /**
     * @return The default text for the node.
     */
    public String defaultText() {
        switch (this) {
            case PMMLRule:
                return PMMLRuleEditorNodeDialog.RULE_LABEL;
            case RuleEngine:
                return RuleEngineNodeDialog.RULE_LABEL;
            case RuleFilter:
            case RuleSplitter:
                return RuleEngineNodeDialog.FILTER_RULE_LABEL;
            case VariableRule:
                return RuleEngineVariableNodeDialog.RULE_LABEL;
            default:
                throw new UnsupportedOperationException("Not supported: " + this);
        }
    }

    private static final String[] NO_EXPRESSIONS = new String[0];

    private static final String[] ALL_EXPRESSIONS = new String[]{Expression.ROWID, Expression.ROWINDEX,
        Expression.ROWCOUNT};

    /**
     * @return The supported expressions (ROWID, ROWINDEX, ROWCOUNT) for the node.
     */
    public String[] expressions() {
        switch (this) {
            case PMMLRule:
                return NO_EXPRESSIONS;
            case RuleEngine:
                return ALL_EXPRESSIONS.clone();
            case RuleFilter:
            case RuleSplitter:
                return ALL_EXPRESSIONS.clone();
            case VariableRule:
                return NO_EXPRESSIONS;
            default:
                throw new UnsupportedOperationException("Not supported: " + this);
        }
    }
}
