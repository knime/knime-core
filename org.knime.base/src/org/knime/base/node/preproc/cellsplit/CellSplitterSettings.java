/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jun 20, 2007 (ohl): created
 */
package org.knime.base.node.preproc.cellsplit;

import java.util.Vector;

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 * Extends the user settings object to a general splitter settings object. In
 * addition to the user settings it stores column types and number of columns,
 * the values analyzed during execute. These values are not saved.
 * 
 * @author ohl, University of Konstanz
 */
class CellSplitterSettings extends CellSplitterUserSettings {

    private Vector<DataType> m_types = new Vector<DataType>();

    /**
     * Creates a new settings object with no (or default) settings.
     */
    CellSplitterSettings() {
        super();
    }

    /**
     * Creates a new settings object with the value from the specified settings
     * object. If the values in there incomplete it throws an Exception. The
     * values can be validated (checked for consistency and validity) with the
     * getStatus method.
     * 
     * 
     * @param values the config object to read the settings values from
     * @throws InvalidSettingsException if the values in the settings object are
     *             incomplete.
     */
    CellSplitterSettings(final NodeSettingsRO values)
            throws InvalidSettingsException {
        super(values);
    }

    /**
     * Adds the type of a new column at the end of the column list.
     * 
     * @param type the type of the new column.
     */
    void addColumnOfType(final DataType type) {
        m_types.add(type);
    }

    /**
     * Replaces the type of an already found column. Used during column type
     * guessing.
     * 
     * @param colIdx the index of the column which gets a new type
     * @param newType the new type of the specified column
     */
    void replaceTypeOfColumn(final int colIdx, final DataType newType) {
        m_types.set(colIdx, newType);
    }

    /**
     * Return the type of a column previously added. 
     * 
     * @param colIdx the column to get the type for.
     * @return the guessed column type of the specified column. 
     */
    DataType getTypeOfColumn(final int colIdx) {
        return m_types.get(colIdx);
    }

    /**
     * @return the number of column found during type guessing
     */
    int getNumOfColsGuessed() {
        return m_types.size();
    }
}
