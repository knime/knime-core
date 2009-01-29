/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
