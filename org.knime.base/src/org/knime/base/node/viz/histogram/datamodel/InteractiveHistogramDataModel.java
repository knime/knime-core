/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *    26.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;


/**
 * This data model holds all information (DataRows, DataTableSpec) to provide
 * the flexibility.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramDataModel {
    
    private final DataTableSpec m_tableSpec;
    
    private final SortedSet<Color> m_rowColors = 
        new TreeSet<Color>(HSBColorComparator.getInstance());
    
    private final List<DataRow> m_dataRows;
    
    /**Constructor for class InteractiveHistogramDataModel.
     * @param spec the{@link DataTableSpec}
     * @param expectedNoOfRows the expected number of rows
     * 
     */
    public InteractiveHistogramDataModel(final DataTableSpec spec,
            final int expectedNoOfRows) {
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification sholdn't be null");
        }
        m_tableSpec = spec;
        m_dataRows = new ArrayList<DataRow>(expectedNoOfRows);
    }
    
    /**
     * @param row the {@link DataRow} to add to this model
     */
    public void addDataRow(final DataRow row) {
        if (row == null) {
            throw new IllegalArgumentException("Row must not be null");
        }
        m_dataRows.add(row);
        final Color color = m_tableSpec.getRowColor(row).getColor(false, false);
        if (!m_rowColors.contains(color)) {
            m_rowColors.add(color);
        }
    }
    
    /**
     * @return the rowColors
     */
    public SortedSet<Color> getRowColors() {
        return m_rowColors;
    }
    
    /**
     * @return the dataRows
     */
    public List<DataRow> getDataRows() {
        return m_dataRows;
    }
    
    /**
     * @param idx the index of the column
     * @return the {@link DataColumnSpec} of the column with the given index
     */
    public DataColumnSpec getColumnSpec(final int idx) {
        return m_tableSpec.getColumnSpec(idx);
    }
}
