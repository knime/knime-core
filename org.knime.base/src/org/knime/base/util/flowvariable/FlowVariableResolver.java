/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 */
package org.knime.base.util.flowvariable;

import java.util.NoSuchElementException;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Used to parse the a script containing flow and workflow variables.
 *
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 *
 * @since 2.10
 */
public final class FlowVariableResolver {
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
    public static String parse(final String text, final FlowVariableProvider model) {
        return parse(text, model, FlowVariableEscaper.DEFAULT_INSTANCE);
    }

    /**
     * Parses the given text and replaces all variable placeholders by their actual value.
     *
     * @param text the text to parse (for instance an R script).
     * @param model delegator to to retrieve variables
     * @return the changed text with placeholders replaced by variables.
     * @param escaper A non-null escaper object (possibly sub-class to escape strings etc.)
     * @throws NoSuchElementException If a variable cannot be resolved
     */
    public static String parse(final String text, final FlowVariableProvider model,
        final FlowVariableEscaper escaper) {
        String command = new String(text);
        int currentIndex = 0;
        do {
            currentIndex = command.indexOf("$${", currentIndex);
            if (currentIndex < 0) {
                break;
            }
            int endIndex = command.indexOf("}$$", currentIndex);
            if (endIndex < 0) {
                String badVarName;
                if (command.length() - currentIndex > 20) {
                    badVarName = command.substring(currentIndex, currentIndex + 19) + "...";
                } else {
                    badVarName = command.substring(currentIndex);
                }
                throw new IllegalArgumentException("Variable identifier \"" + badVarName + "\" is not closed");
            }
            String var = command.substring(currentIndex + 4, endIndex);
            switch (command.charAt(currentIndex + 3)) {
                case 'I':
                    String s = escaper.readInt(model, var);
                    command = command.replace("$${I" + var + "}$$", s);
                    break;
                case 'D':
                    s = escaper.readDouble(model, var);
                    command = command.replace("$${D" + var + "}$$", s);
                    break;
                case 'S':
                    s = escaper.readString(model, var);
                    command = command.replace("$${S" + var + "}$$", s);
                    break;
                default:
                    String badVarName;
                    if (endIndex > currentIndex) {
                        badVarName = command.substring(currentIndex, endIndex + "}$$".length());
                    } else if (command.indexOf("\n", currentIndex) >= 0) {
                        badVarName = command.substring(currentIndex, command.indexOf("\n", currentIndex)) + "...";
                    } else if (command.length() - currentIndex > 20) {
                        badVarName = command.substring(currentIndex, currentIndex + 20) + "...";
                    } else {
                        badVarName = command.substring(currentIndex);
                    }
                    throw new IllegalArgumentException("Invalid flow variable identifier \"" + badVarName + "\"; "
                        + "it should start with a type identifer.");
            }
        } while (true);
        return command;
    }

    /**
     * Replaces and returns the given flow variable.
     *
     * @param var flow variable to be extended
     * @return the new variable as string with pre- and suffix for INTEGER, DOUBLE and STRING types
     */
    public static String getPlaceHolderForVariable(final FlowVariable var) {
        switch (var.getType()) {
            case INTEGER:
                return "$${I" + var.getName() + "}$$";
            case DOUBLE:
                return "$${D" + var.getName() + "}$$";
            case STRING:
                return "$${S" + var.getName() + "}$$";
            default:
                throw new RuntimeException("Unsupported flow variable type '" + var.getType() + "'");
        }
    }

    /**
     * Provides generic access to flow variables of the given provider. The given class determines also the
     * corresponding {@link org.knime.core.node.workflow.FlowVariable.Type}.
     *
     * @param provider the provider to receive the flow-variable from.
     * @param name of the flow variable to receive
     * @param clazz the type of the flow variable
     * @param <T> the java type of the variable
     * @return the flow variable instance
     * @throws IllegalArgumentException if the given type is not supported, currently only Double, Integer and String
     *             are supported
     * @throws NoSuchElementException if there is no flow variable with the given name
     * @throws NullPointerException if any argument <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFlowVariable(final FlowVariableProvider provider,
        final String name, final Class<T> clazz) {
        CheckUtils.checkNotNull(name);
        CheckUtils.checkNotNull(provider);
        CheckUtils.checkNotNull(clazz);
        if (Integer.class.equals(clazz)) {
            return (T)Integer.valueOf(provider.peekFlowVariableInt(name));
        } else if (Double.class.equals(clazz)) {
            return (T)Double.valueOf(provider.peekFlowVariableDouble(name));
        } else if (String.class.equals(clazz)) {
            return (T)provider.peekFlowVariableString(name);
        } else {
            throw new IllegalArgumentException("Invalid variable class: " + clazz);
        }
    }

    /**
     * Reads the actual value from the {@link FlowVariableResolver} and returns their string representation. This class
     * can be sub-classed when it becomes necessary to escape certain values, e.g. to put a string always in quotes.
     */
    public static class FlowVariableEscaper {

        /** Default instance that does no escaping, just reads the value. */
        public static final FlowVariableEscaper DEFAULT_INSTANCE = new FlowVariableEscaper();

        /** Create new escaper (only to be used by sub-classes). */
        protected FlowVariableEscaper() {
        }

        /**
         * Read a double from {@link FlowVariableProvider#peekFlowVariableDouble(String)} and return its string.
         *
         * @param model to read from
         * @param var The name of the variable.
         * @return The string value
         * @throws NullPointerException If either argument is null
         * @throws NoSuchElementException If a variable with that name is not defined.
         */
        public String readDouble(final FlowVariableProvider model, final String var) {
            final double d = model.peekFlowVariableDouble(var);
            return Double.toString(d);
        }

        /**
         * Read an integer from {@link FlowVariableProvider#peekFlowVariableInt(String)} and return its string.
         *
         * @param model to read from
         * @param var The name of the variable.
         * @return The string value
         * @throws NullPointerException If either argument is null
         * @throws NoSuchElementException If a variable with that name is not defined.
         */
        public String readInt(final FlowVariableProvider model, final String var) {
            final int i = model.peekFlowVariableInt(var);
            return Integer.toString(i);
        }

        /**
         * Read a string from {@link FlowVariableProvider#peekFlowVariableString(String)} and return it.
         *
         * @param model to read from
         * @param var The name of the variable.
         * @return The string value
         * @throws NullPointerException If either argument is null
         * @throws NoSuchElementException If a variable with that name is not defined.
         */
        public String readString(final FlowVariableProvider model, final String var) {
            return model.peekFlowVariableString(var);
        }
    }
}