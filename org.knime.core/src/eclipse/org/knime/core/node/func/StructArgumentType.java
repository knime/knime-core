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
 *   Nov 10, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.func;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents struct arguments that consist of multiple properties.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public interface StructArgumentType extends ArgumentType {
    /**
     * @return the properties of this struct
     */
    ArgumentDefinition[] getProperties();

    /**
     * @return a builder for StructArgumentTypes
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for StructArgumentType.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static class Builder {

        private List<ArgumentDefinition> m_properties = new ArrayList<>();

        private Builder() {
        }

        /**
         * @return the struct argument type
         */
        public StructArgumentType build() {
            return new DefaultStructArgumentType(m_properties.toArray(ArgumentDefinition[]::new));
        }

        /**
         * @param name of the property
         * @param description of the property
         * @param type of the property
         * @return this builder
         */
        public Builder withProperty(final String name, final String description, final ArgumentType type) {
            return withProperty(name, description, type, false);
        }

        /**
         * @param name of the property
         * @param description of the property
         * @param type of the property
         * @return this builder
         */
        public Builder withOptionalProperty(final String name, final String description, final ArgumentType type) {
            return withProperty(name, description, type, true);
        }

        private Builder withProperty(final String name, final String description, final ArgumentType type,
            final boolean isOptional) {
            m_properties.add(new DefaultArgumentDefinition(name, description, type, isOptional));
            return this;
        }
    }

}