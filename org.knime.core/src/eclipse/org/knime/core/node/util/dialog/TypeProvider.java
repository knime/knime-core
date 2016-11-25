/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   24 Oct 2016 (albrecht): created
 */
package org.knime.core.node.util.dialog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * A central place for simple type definitions for data cells and flow variables.
 *
 * <p>This class might change and is not meant as public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz Germany
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class TypeProvider {

    private static TypeProvider provider;

    private List<DataType> m_columnTypes;
    private List<DataType> m_listColTypes;
    @SuppressWarnings("rawtypes")
    private Map<Type, Class> m_flowVarTypes;

    @SuppressWarnings("rawtypes")
    private TypeProvider() {

        // column types
        m_columnTypes = new ArrayList<DataType>();
        m_columnTypes.add(BooleanCell.TYPE);
        m_columnTypes.add(IntCell.TYPE);
        m_columnTypes.add(DoubleCell.TYPE);
        m_columnTypes.add(LongCell.TYPE);
        m_columnTypes.add(StringCell.TYPE);
        m_columnTypes.add(XMLCell.TYPE);
        m_columnTypes.add(DateAndTimeCell.TYPE);

        // list column types
        m_listColTypes = new ArrayList<DataType>();
        for (DataType type : m_columnTypes) {
            m_listColTypes.add(DataType.getType(ListCell.class, type));
        }

        // flow variable types
        m_flowVarTypes = new LinkedHashMap<Type, Class>();
        m_flowVarTypes.put(Type.DOUBLE, Double.class);
        m_flowVarTypes.put(Type.INTEGER, Integer.class);
        m_flowVarTypes.put(Type.STRING, String.class);
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
     * @return the flowVarTypes
     */
    @SuppressWarnings("rawtypes")
    public Map<Type, Class> getFlowVarTypes() {
        return m_flowVarTypes;
    }

    /**
     * @return the columnTypes
     */
    public List<DataType> getColumnTypes() {
        return m_columnTypes;
    }

    /**
     * @return the listColTypes
     */
    public List<DataType> getListColTypes() {
        return m_listColTypes;
    }

}
