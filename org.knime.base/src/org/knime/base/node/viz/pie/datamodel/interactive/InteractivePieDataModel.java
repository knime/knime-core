/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 *
 * History
 *    18.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel.interactive;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.viz.pie.datamodel.PieDataModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieDataModel extends PieDataModel {

    private final DataTableSpec m_spec;

    private final List<DataRow> m_dataRows;

    /**Constructor for class InteractivePieDataModel.
     * @param spec the {@link DataTableSpec}
     * @param noOfRows the optional number of rows to initialize the row array
     */
    public InteractivePieDataModel(final DataTableSpec spec,
            final int noOfRows) {
        super(true);
        m_spec = spec;
        m_dataRows = new ArrayList<DataRow>(noOfRows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDataRow(final DataRow row,  final Color rowColor,
            final DataCell pieCell, final DataCell aggrCell) {
        m_dataRows.add(row);
    }


    /**
     * @return all data rows
     */
    public List<DataRow> getDataRows() {
        return Collections.unmodifiableList(m_dataRows);
    }


    /**
     * @return the {@link DataTableSpec}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @param colName the column name to get the spec for
     * @return the {@link DataColumnSpec} or <code>null</code> if the name is
     * not in the spec
     */
    public DataColumnSpec getColSpec(final String colName) {
        return m_spec.getColumnSpec(colName);
    }

    /**
     * @param colName the column name to get the index for
     * @return the index of the given column name
     */
    public int getColIndex(final String colName) {
        return m_spec.findColumnIndex(colName);
    }

    /**
     * @param row the row to get the color for
     * @return the color of this row
     */
    public Color getRowColor(final DataRow row) {
        return m_spec.getRowColor(row).getColor(false, false);
    }
}
