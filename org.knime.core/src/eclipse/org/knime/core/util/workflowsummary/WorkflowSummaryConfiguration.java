/*
 * ------------------------------------------------------------------------
 *
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
 * History
 */
package org.knime.core.util.workflowsummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.util.XMLUtils;

/**
 * Object to configure the workflow summary generation, e.g. via {@link WorkflowSummaryGenerator}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class WorkflowSummaryConfiguration {

    final List<NodeID> m_nodesToIgnore;

    final SummaryFormat m_format;

    final boolean m_includeExecutionInfo;

    final UnaryOperator<String> m_textEncoder;

    /**
     * The final format of the workflow summary.
     */
    public enum SummaryFormat {
            /** as xml document */
            XML,
            /** as json document */
            JSON;
    }

    private WorkflowSummaryConfiguration(final Builder builder) {
        m_format = builder.m_format;
        m_nodesToIgnore = new ArrayList<>(builder.m_nodesToIgnore);
        m_includeExecutionInfo = builder.m_includeExecutionInfo;
        m_textEncoder = builder.m_textEncoder;
    }

    /**
     * Creates a new builder in order to create new config instances.
     *
     * @param format the summary format
     * @return a new builder instance
     */
    public static Builder builder(final SummaryFormat format) {
        return new Builder(format);
    }

    /**
     * Builder for workfow summary configuration objects.
     */
    public static class Builder {

        private SummaryFormat m_format;

        private UnaryOperator<String> m_textEncoder;

        private boolean m_includeExecutionInfo = false;

        private List<NodeID> m_nodesToIgnore = Collections.emptyList();

        private Builder(final SummaryFormat format) {
            m_format = format;
            switch (format) {
                case XML:
                    m_textEncoder = XMLUtils::escape;
                    break;
                case JSON:
                    m_textEncoder = null;
                    break;
            }
        }

        /**
         * Whether to include execution information in the summary, such as execution environment info (plugins, system
         * variables etc.), port object summaries or execution statistics.
         *
         * @param include if <code>true</code> the execution info will be included
         * @return this builder for chaining
         */
        public Builder includeExecutionInfo(final boolean include) {
            m_includeExecutionInfo = include;
            return this;
        }

        /**
         * List of nodes to be ignored in the workflow summary.
         *
         * @param nodesToIgnore the list
         * @return this builder for chaining
         */
        public Builder nodesToIgnore(final List<NodeID> nodesToIgnore) {
            m_nodesToIgnore = nodesToIgnore;
            return this;
        }

        /**
         * Creates a new workflow summary configuration instance from this builder.
         *
         * @return a new workflow summary configuration instance
         */
        public WorkflowSummaryConfiguration build() {
            return new WorkflowSummaryConfiguration(this);
        }
    }

}
