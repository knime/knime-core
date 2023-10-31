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

import org.knime.core.node.func.ArgumentDefinition.PrimitiveArgumentType;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class NodeFuncApi {

    final String m_name;

    final String m_description;

    final PortDefinition[] m_inputs;

    final PortDefinition[] m_outputs;

    final ArgumentDefinition[] m_arguments;

    private NodeFuncApi(final Builder builder) {
        m_name = builder.m_name;
        m_description = builder.m_description;
        m_inputs = builder.m_inputs.toArray(PortDefinition[]::new);
        m_outputs = builder.m_outputs.toArray(PortDefinition[]::new);
        m_arguments = builder.m_arguments.toArray(ArgumentDefinition[]::new);
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String m_name;

        private String m_description = "";

        private final List<PortDefinition> m_inputs = new ArrayList<>();

        private final List<PortDefinition> m_outputs = new ArrayList<>();

        private final List<ArgumentDefinition> m_arguments = new ArrayList<>();

        private Builder(final String name) {
            m_name = name;
        }

        public Builder withDescription(final String description) {
            CheckUtils.checkArgumentNotNull(description);
            m_description = description;
            return this;
        }

        public Builder withInputTable(final String name, final String description) {
            m_inputs.add(new DefaultPortDefinition(name, description));
            return this;
        }

        public Builder withOutputTable(final String name, final String description) {
            m_outputs.add(new DefaultPortDefinition(name, description));
            return this;
        }

        public Builder withArgument(final String name, final String description, final PrimitiveArgumentType type) {
            m_arguments.add(new DefaultArgumentDefinition(name, description, type, false));
            return this;
        }

        public Builder withOptionalArgument(final String name, final String description, final PrimitiveArgumentType type) {
            m_arguments.add(new DefaultArgumentDefinition(name, description, type, true));
            return this;
        }

        public NodeFuncApi build() {
            return new NodeFuncApi(this);
        }

    }

}
