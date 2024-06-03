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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 24, 2024 (wiswedel): created
 */
package org.knime.core.customization.nodes.filter;

import java.util.Arrays;

import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Configuration for node visibility and usability in KNIME AP based on patterns.
 *
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel
 */
public final class NodesFilter {

    /** Used in default customization, allows all installed nodes to be viewed. */
    public static final NodesFilter VIEW_ALL =
        new NodesFilter(ScopeEnum.VIEW, RuleEnum.ALLOW, TruePredicate.INSTANCE);

    /** Used in default customization, allows all installed nodes to be used. */
    public static final NodesFilter USE_ALL =
        new NodesFilter(ScopeEnum.USE, RuleEnum.ALLOW, TruePredicate.INSTANCE);

    /**
     * Defines scope of this filter: node visibility (VIEW) or usability (USE) in the node repository.
     */
    public enum ScopeEnum {
            /**
             * Whether a node is listed in the node repository.
             */
            VIEW("view"), //
            /**
             * Whether a node is allowed to be instantiated in general (for listing in the node repository, for workflow
             * loading etc.). I.e. this scope 'includes' the view-scope.
             */
            USE("use");

        private final String m_value;

        ScopeEnum(final String value) {
            m_value = value;
        }

        @JsonValue
        String getValue() {
            return m_value;
        }

        @JsonCreator
        static ScopeEnum fromValue(final String value) {
            return Arrays.stream(ScopeEnum.values()) //
                .filter(scope -> scope.getValue().equals(value)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("Unexpected value '" + value + "'"));
        }
    }

    /**
     * Defines inclusion (ALLOW) or exclusion (DENY) rules for nodes.
     */
    enum RuleEnum {
        DENY("deny"), //
        ALLOW("allow");

        private final String m_value;

        RuleEnum(final String value) {
            m_value = value;
        }

        @JsonValue
        String getValue() {
            return m_value;
        }

        @JsonCreator
        static RuleEnum fromValue(final String value) {
            return Arrays.stream(RuleEnum.values()) //
                .filter(rule -> rule.getValue().equals(value)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("Unexpected value '" + value + "'"));
        }
    }

    private final ScopeEnum m_scope;
    private final RuleEnum m_rule;
    private final NodePredicate m_predicate;

    /**
     * Jackson deserializer.
     */
    @JsonCreator
    NodesFilter( //
        @JsonProperty("scope") final ScopeEnum scope, //
        @JsonProperty("rule") final RuleEnum rule, //
        @JsonProperty("predicate") final NodePredicate filter) {
        m_scope = CheckUtils.checkArgumentNotNull(scope, "ScopeEnum cannot be null");
        m_rule = CheckUtils.checkArgumentNotNull(rule, "RuleEnum cannot be null");
        m_predicate = CheckUtils.checkArgumentNotNull(filter, "Predicate cannot be null");
    }

    /**
     * @return What this filter applies to, viewing or using.
     */
    @JsonProperty
    public ScopeEnum getScope() {
        return m_scope;
    }

    @JsonProperty
    RuleEnum getRule() {
        return m_rule;
    }

    @JsonProperty
    NodePredicate getPredicate() {
        return m_predicate;
    }

    /**
     * Checks if a node matches the customization rules.
     *
     * @param factoryId Node identifier, as per {@link org.knime.core.node.NodeFactoryId}.
     * @return true if the node matches the rules, false otherwise.
     */
    public boolean isAllowed(final String factoryId) {
        final var isMatchFilter = m_predicate.matches(factoryId);
        return (m_rule == RuleEnum.ALLOW) == isMatchFilter;
    }

    @Override
    public String toString() {
        return String.format("NodesFilter{scope=%s, rule=%s, predicate=%s}", m_scope, m_rule, m_predicate);
    }
}
