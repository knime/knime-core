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
 * ------------------------------------------------------------------------
 *
 * History
 *   16.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.type;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.base.node.jsnippet.type.flowvar.DoubleFlowVarToJava;
import org.knime.base.node.jsnippet.type.flowvar.IntFlowVarToJava;
import org.knime.base.node.jsnippet.type.flowvar.StringFlowVarToJava;
import org.knime.base.node.jsnippet.type.flowvar.TypeConverter;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * A central place for type converters for data cells and flow variables.
 *
 * @author Heiko Hofer
 */
public final class TypeProvider {
    private static TypeProvider provider;

    private Map<Type, TypeConverter> m_flowVarConverter;

    /** Prevent creation of class instances. */
    private TypeProvider() {
        // Converters for flow variables
        m_flowVarConverter = new LinkedHashMap<Type, TypeConverter>();
        m_flowVarConverter.put(Type.DOUBLE, new DoubleFlowVarToJava());
        m_flowVarConverter.put(Type.INTEGER, new IntFlowVarToJava());
        m_flowVarConverter.put(Type.STRING, new StringFlowVarToJava());
    }

    /**
     * Get default type provider.
     * @return the default instance
     */
    public static TypeProvider getDefault() {
        if (null == provider) {
            provider = new TypeProvider();
        }
        return provider;
    }

    /**
     * Get list of possible flow variable types.
     * @return the list of flow variables types
     */
    public Collection<Type> getTypes() {
        return m_flowVarConverter.keySet();
    }

    /**
     * Get the type converter for the give flow variable type.
     * @param type the flow variable type
     * @return the type converter for the given flow variable type
     */
    public TypeConverter getTypeConverter(final Type type) {
        return m_flowVarConverter.get(type);
    }

}
