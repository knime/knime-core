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
package org.knime.core.customization.repository;

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
public final class NodesCustomization {

    /** Used in NOOP customization, allows all installed nodes to be viewed. */
    public static final NodesCustomization VIEW_ALL =
            new NodesCustomization(GovernEnum.VIEW, RuleEnum.WHITELIST, TrueFilter.INSTANCE);

    /** Used in NOOP customization, allows all installed nodes to be used. */
    public static final NodesCustomization USE_ALL =
            new NodesCustomization(GovernEnum.USE, RuleEnum.WHITELIST, TrueFilter.INSTANCE);

    /**
     * Governs node visibility (VIEW) or usability (USE) in the node repository.
     */
    public enum GovernEnum {
        VIEW("view"), //
        USE("use");

        private final String m_value;

        GovernEnum(final String value) {
            m_value = value;
        }

        @JsonValue
        String getValue() {
            return m_value;
        }

        @JsonCreator
        static GovernEnum fromValue(final String value) {
            return Arrays.stream(GovernEnum.values()) //
                .filter(govern -> govern.getValue().equals(value)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("Unexpected value '" + value + "'"));
        }
    }

    /**
     * Defines inclusion (WHITELIST) or exclusion (BLACKLIST) rules for nodes.
     */
    enum RuleEnum {
        BLACKLIST("blacklist"), //
        WHITELIST("whitelist");

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

    private final GovernEnum m_govern;
    private final RuleEnum m_rule;
    private final INodeFilter m_filter;

    /**
     * Jackson deserializer.
     */
    @JsonCreator
    NodesCustomization( //
        @JsonProperty("govern") final GovernEnum govern, //
        @JsonProperty("rule") final RuleEnum rule, //
        @JsonProperty("filter") final INodeFilter filter) {
        m_govern = CheckUtils.checkArgumentNotNull(govern, "GovernEnum cannot be null");
        m_rule = CheckUtils.checkArgumentNotNull(rule, "RuleEnum cannot be null");
        m_filter = CheckUtils.checkArgumentNotNull(filter, "Filter cannot be null");
    }

    /**
     * @return What this customization applies to, viewing or using.
     */
    public GovernEnum getGovern() {
        return m_govern;
    }

    RuleEnum getRule() {
        return m_rule;
    }

    INodeFilter getFilter() {
        return m_filter;
    }

    /**
     * Checks if a node matches the customization rules.
     *
     * @param nodeAndID Node identifier, as per {@link org.knime.core.node.NodeFactoryId}.
     * @return true if the node matches the rules, false otherwise.
     */
    public boolean isAllowed(final String nodeAndID) {
        final var isMatchFilter = m_filter.matches(nodeAndID);
        return (m_rule == RuleEnum.WHITELIST) == isMatchFilter;
    }

    @Override
    public String toString() {
        return String.format("NodesCustomization{govern=%s, rule=%s, filter=%s}", m_govern, m_rule, m_filter);
    }
}
