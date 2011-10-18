/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   25.08.2011 (hofer): created
 */
package org.knime.base.node.preproc.colconvert.categorytonumber;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * CellFactory to process a single column for the category2number node.
 *
 * @author Heiko Hofer
 */
public class CategoryToNumberCellFactory implements CellFactory {
    private final DataColumnSpec[] m_columnSpecs;
    private final int m_index;
    private final Map<DataValue, DataCell[]> m_categories;
    private final Map<DataCell, IntCell> m_map;
    private final DataCell[] m_mapMissingTo;
    private final CategoryToNumberNodeSettings m_settings;
    private final String m_inColumn;
    private final String m_outColumn;


    /**
     * @param inSpec Spec of the input table
     * @param colName target column of the input table
     * @param settings the settings
     */
    public CategoryToNumberCellFactory(final DataTableSpec inSpec,
            final String colName,
            final CategoryToNumberNodeSettings settings) {
        String newColName = settings.getAppendColumns()
            ? DataTableSpec.getUniqueColumnName(inSpec,
                    colName + settings.getColumnSuffix())
            : colName;
        DataColumnSpecCreator creator = new DataColumnSpecCreator(
                newColName, IntCell.TYPE);
        m_columnSpecs = new DataColumnSpec[] {creator.createSpec()};
        m_index = inSpec.findColumnIndex(colName);
        m_categories = new HashMap<DataValue, DataCell[]>();
        m_map = new LinkedHashMap<DataCell, IntCell>();
        m_mapMissingTo = new DataCell[] {settings.getMapMissingTo()};
        m_settings = settings;
        m_inColumn = colName;
        m_outColumn = newColName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        DataCell key = row.getCell(m_index);
        if (key.isMissing()) {
            return m_mapMissingTo;
        }
        DataCell[] value = m_categories.get(key);
        if (value == null) {
            if (m_categories.size() >= m_settings.getMaxCategories()) {
                // maximum reached give error
                throw new IllegalStateException(
                        "Maximum number of categories reached for column: "
                        + m_columnSpecs[0].getName());
            }
            int number = m_settings.getStartIndex()
                + m_categories.size() * m_settings.getIncrement();
            IntCell cell = new IntCell(number);
            // Keep information in two maps for performance reasons
            // This avoids creating an array every time when the category
            // already exists.
            value = new DataCell[] {cell};
            m_categories.put(key, value);
            m_map.put(key, cell);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_columnSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey,
            final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount);
    }


    /**
     * Get PMML MapValues container object.
     * @return results for generating PMML MapValues Element.
     */
    public MapValuesConfiguration getConfig() {
        return new CategoryToNumberConfiguration(
                m_inColumn, m_outColumn, m_map,
                m_settings.getDefaultValue(),
                m_settings.getMapMissingTo());
    }

}
