/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
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
