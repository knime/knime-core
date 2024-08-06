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
 *   Oct 20, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.func;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.knime.core.node.func.ArgumentDefinition.PrimitiveArgumentType;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class NodeFuncApi implements ApiDefinition {

    final String m_name;

    final String m_description;

    final PortDefinition[] m_inputs;

    final PortDefinition[] m_outputs;

    final ArgumentDefinition[] m_arguments;

    private static final Pattern VALID_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$"); //NOSONAR

    private NodeFuncApi(final Builder builder) {
        m_name = builder.m_name;
        m_description = builder.m_description;
        m_inputs = builder.m_inputs.toArray(PortDefinition[]::new);
        m_outputs = builder.m_outputs.toArray(PortDefinition[]::new);
        m_arguments = builder.m_arguments.toArray(ArgumentDefinition[]::new);
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public String getDescription() {
        return m_description;
    }

    /**
     * @return the inputs of the node
     */
    public PortDefinition[] getInputs() {
        return m_inputs;
    }

    /**
     * @return the output of the nodes
     */
    public ArgumentDefinition[] getArguments() {
        return m_arguments;
    }

    /**
     * @return the outputs of the node
     */
    public PortDefinition[] getOutputs() {
        return m_outputs;
    }

    /**
     * @param name of the NodeFunc (must be unique)
     * @return a builder for a NodeFuncApi
     */
    public static Builder builder(final String name) {
        return new Builder(name);
    }

    /**
     * Builder for {@link NodeFuncApi}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class Builder {

        private final String m_name;

        private String m_description = "";

        private final List<PortDefinition> m_inputs = new ArrayList<>();

        private final List<PortDefinition> m_outputs = new ArrayList<>();

        private final List<ArgumentDefinition> m_arguments = new ArrayList<>();

        private Builder(final String name) {
            checkName(name);
            m_name = name;
        }

        private static void checkName(final String name) {
            CheckUtils.checkArgumentNotNull(name, "Name must not be null.");
            if (!VALID_NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException("The given name '%s' is not valid.".formatted(name));
            }
        }

        /**
         * @param description of what the NodeFunc does. Should be comprehensive.
         * @return this Builder
         */
        public Builder withDescription(final String description) {
            CheckUtils.checkArgumentNotNull(description);
            m_description = description;
            return this;
        }

        /**
         * @param name of the input table
         * @param description of what the input table contains
         * @return this Builder
         */
        public Builder withInputTable(final String name, final String description) {
            checkName(name);
            m_inputs.add(new DefaultPortDefinition(name, description));
            return this;
        }

        /**
         * @param name of the output table
         * @param description of what the output table represents or contains
         * @return this Builder
         */
        public Builder withOutputTable(final String name, final String description) {
            checkName(name);
            m_outputs.add(new DefaultPortDefinition(name, description));
            return this;
        }

        private Builder withArgument(final String name, final String description, final ArgumentType type,
            final boolean isOptional) {
            checkName(name);
            m_arguments.add(new DefaultArgumentDefinition(name, description, type, isOptional));
            return this;
        }

        /**
         * Adds a new argument to the NodeFunc API.
         *
         * @param name of the argument
         * @param description of the argument
         * @param type of the argument
         * @return this builder
         */
        public Builder withArgument(final String name, final String description, final ArgumentType type) {
            return withArgument(name, description, type, false);
        }

        /**
         * Adds a new optional argument to the NodeFunc API.
         *
         * @param name of the argument
         * @param description of the argument
         * @param type of the argument
         * @return this builder
         */
        public Builder withOptionalArgument(final String name, final String description ,final ArgumentType type) {
            return withArgument(name, description, type, true);
        }

        /**
         * Adds an integer argument.
         *
         * @param name of the argument (must not contain whitespaces, - or other special characters)
         * @param description of what the argument is there for
         * @return this builder
         */
        public Builder withIntArgument(final String name, final String description) {
            return withArgument(name, description, PrimitiveArgumentType.INT, false);
        }

        /**
         * Adds a long argument.
         *
         * @param name of the argument (must not contain whitespaces, - or other special characters)
         * @param description of what the argument is there for
         * @return this builder
         */
        public Builder withLongArgument(final String name, final String description) {
            return withArgument(name, description, PrimitiveArgumentType.LONG, false);
        }

        /**
         * Adds an optional long argument, i.e. the argument must not be declared.
         *
         * @param name of the argument (must not contain whitespaces, - or other special characters)
         * @param description of what the argument is there for
         * @return this builder
         */
        public Builder withOptionalLongArgument(final String name, final String description) {
            return withArgument(name, description, PrimitiveArgumentType.LONG, true);
        }

        /**
         * Adds a double argument.
         *
         * @param name of the argument (must not contain whitespaces, - or other special characters)
         * @param description of what the argument is there for
         * @return this builder
         */
        public Builder withDoubleArgument(final String name, final String description) {
            return withArgument(name, description, PrimitiveArgumentType.DOUBLE, false);
        }

        /**
         * Adds a string argument.
         *
         * @param name of the argument (must not contain whitespaces, - or other special characters)
         * @param description of what the argument is there for
         * @return this builder
         */
        public Builder withStringArgument(final String name, final String description) {
            return withArgument(name, description, PrimitiveArgumentType.STRING, false);
        }

        /**
         * Adds an optional string argument.
         *
         * @param name of the argument (must not contain whitespace, - or other special characters)
         * @param description of what the argument is there for
         * @return this builder
         */
        public Builder withOptionalStringArgument(final String name, final String description) {
            return withArgument(name, description, PrimitiveArgumentType.STRING, true);
        }

        /**
         * Adds a boolean argument.
         *
         * @param name of the argument (must not contain whitespaces, - or other special characters)
         * @param description of wht the argument is there fore
         * @return this builder
         */
        public Builder withBooleanArgument(final String name, final String description) {
            return withArgument(name, description, PrimitiveArgumentType.BOOLEAN, false);
        }

        /**
         * Adds an optional boolean argument.
         *
         * @param name of the argument (must not contain whitespace, - or other special characters)
         * @param description of what the argument is there for
         * @return this builder
         * @since 5.3
         */
        public Builder withOptionalBooleanArgument(final String name, final String description) {
            return withArgument(name, description, PrimitiveArgumentType.BOOLEAN, true);
        }

        /**
         * @return the final {@link NodeFuncApi}
         */
        public NodeFuncApi build() {
            return new NodeFuncApi(this);
        }

    }

}
