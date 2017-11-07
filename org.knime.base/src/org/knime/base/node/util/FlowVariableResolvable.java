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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.util;

import java.util.NoSuchElementException;

import org.knime.base.util.flowvariable.FlowVariableProvider;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Implemented and used by a node model to parse text expressions containing flow variable identifiers such as
 * $${Inameofintvar}$$.
 *
 * @author Thomas Gabriel, KNIME AG, Zurich, Switzerland
 * @since 2.7
 * @deprecated Use {@link FlowVariableProvider} instead
 */
@Deprecated
public interface FlowVariableResolvable {

    /**
     * Delegate access to flow variable of type INTEGER.
     *
     * @param name identifier for flow variable
     * @return int value
     */
    public int delegatePeekFlowVariableInt(final String name);

    /**
     * Delegate access to flow variable of type DOUBLE.
     *
     * @param name identifier for flow variable
     * @return double value
     */
    public double delegatePeekFlowVariableDouble(final String name);

    /**
     * Delegate access to flow variable of type STRING.
     *
     * @param name identifier for flow variable
     * @return String value
     */
    public String delegatePeekFlowVariableString(final String name);

    /**
     * Used to parse the a script containing flow and workflow variables.
     *
     * @deprecated use {@link org.knime.base.util.flowvariable.FlowVariableResolver} instead
     */
    @Deprecated
    public static final class FlowVariableResolver {
        private FlowVariableResolver() {
            // empty
        }

        /**
         * Parses the given text and replaces all variable placeholders by their actual value.
         *
         * @param text the text to parse (for instance an R script).
         * @param model delegator to to retrieve variables
         * @return the changed text with placeholders replaced by variables.
         * @throws NoSuchElementException If a variable cannot be resolved
         */
        @Deprecated
        public static String parse(final String text, final FlowVariableResolvable model) {
            return org.knime.base.util.flowvariable.FlowVariableResolver.parse(text, new FlowVariableProvider() {

                @Override
                public String peekFlowVariableString(final String name) {
                    return model.delegatePeekFlowVariableString(name);
                }

                @Override
                public int peekFlowVariableInt(final String name) {
                    return model.delegatePeekFlowVariableInt(name);
                }

                @Override
                public double peekFlowVariableDouble(final String name) {
                    return model.delegatePeekFlowVariableDouble(name);
                }
            });
        }

        /**
         * Replaces and returns the given flow variable.
         *
         * @param var flow variable to be extended
         * @return the new variable as string with pre- and suffix for INTEGER, DOUBLE and STRING types
         * @deprecated use
         * {@link org.knime.base.util.flowvariable.FlowVariableResolver#getPlaceHolderForVariable(FlowVariable)}
         * instead
         */
        @Deprecated
        public static String getPlaceHolderForVariable(final FlowVariable var) {
            return org.knime.base.util.flowvariable.FlowVariableResolver.getPlaceHolderForVariable(var);
        }
    }
}
