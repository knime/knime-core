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
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeLogger;

/**
 * This is the parent class of the histogram data models which holds
 * all rows to provide the dynamic binning functions, the histogram bins
 * and the selected aggregation columns.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramDataModel {

    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(FixedHistogramDataModel.class);
    
    private static final HistogramDataRowXComparator ASCENDING_ROW_SORTER =
        new HistogramDataRowXComparator();
        
    private final DataColumnSpec m_xColSpec;
   
    private final List<FixedHistogramDataRow> m_dataRows;
    
    private boolean m_rowsSorted = false;
    
    private final List<ColorColumn> m_aggrColumns;
    
    private final SortedSet<Color> m_barElementColors = 
        new TreeSet<Color>(new HSBColorComparator());

    /**Constructor for class HistogramDataModel.
     * @param xColSpec the column specification of the bin column
     * @param noOfRows the expected number of rows
     * @param aggrColumns the aggregation columns
     */
    public FixedHistogramDataModel(final DataColumnSpec xColSpec,  
            final int noOfRows, final ColorColumn... aggrColumns) {
        LOGGER.debug("Entering HistogramDataModel(xColSpec, aggrColumns) "
                + "of class HistogramDataModel.");
        if (xColSpec == null) {
            throw new IllegalArgumentException(
                    "X column specification shouldn't be null");
        }
        if (aggrColumns == null || aggrColumns.length < 1) {
            throw new IllegalArgumentException(
                    "No aggregation columns defined");
        }
        m_dataRows = new ArrayList<FixedHistogramDataRow>(noOfRows);
        m_aggrColumns = Arrays.asList(aggrColumns);
        m_xColSpec = xColSpec;
        final DataColumnDomain domain = m_xColSpec.getDomain();
        if (domain == null) {
            throw new IllegalArgumentException(
                    "The x column domain shouldn't be null");
        }
        LOGGER.debug("Exiting HistogramDataModel(xColSpec, aggrColumns) "
                + "of class HistogramDataModel.");
    }
   
    /**
     * Adds the given {@link FixedHistogramDataRow} to the histogram.
     * @param row the row to add
     */
    public void addDataRow(final FixedHistogramDataRow row) {
//        if (m_bins.size() < 1) {
//            //create the bins before adding a row to it
//            if (m_binNominal) {
//                createNominalBins();
//            } else {
//                createIntervalBins();
//            }
//        }
        if (row.getAggrVals().length != m_aggrColumns.size()) {
            //check at least if they have the same number of
            //aggregation columns is
            throw new IllegalArgumentException(
                    "No of defined aggregation columns and number of "
                    + "aggregation values in given row are unequal.");
        }
        m_dataRows.add(row);
        m_rowsSorted = false;
        final Color color = row.getColor();
        if (!m_barElementColors.contains(color)) {
            m_barElementColors.add(color);
        }
//        addDataRow2Bin(0, row);
    }

    /**
     * @return the x column name
     */
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * @return the x column specification
     */
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }
    /**
     * @return the columns to use for aggregation.
     * THIS IS AN UNMODIFIABLE {@link List}!
     */
    public List<ColorColumn> getAggrColumns() {
        return Collections.unmodifiableList(m_aggrColumns);
    }

    /**
     * @return all available element colors. This is the color the user has
     * set for one attribute in the ColorManager node.
     * THIS IS AN UNMODIFIABLE {@link SortedSet}!
     */
    public SortedSet<Color> getBarElementColors() {
        return Collections.unmodifiableSortedSet(m_barElementColors);
    }
    
    /**
     * @return a <code>List</code> of the data rows in ascending order.
     * THIS IS AN UNMODIFIABLE {@link List}!
     */
    public List<FixedHistogramDataRow> getSortedRows() {
        //sort the data rows to speedup the process if necessary
        if (!m_rowsSorted) {
            Collections.sort(m_dataRows, ASCENDING_ROW_SORTER);
            m_rowsSorted = true;
        }
        return Collections.unmodifiableList(m_dataRows);
    }
//
//    /**
//     * @see java.lang.Object#clone()
//     */
//    @Override
//    public HistogramDataModel clone() {
//        final long start = System.currentTimeMillis();
//        final HistogramDataModel clone = new HistogramDataModel(m_noOfBins, 
//                m_aggrMethod, m_layout, m_xColSpec, m_aggrColumns);
//        clone.m_barElementColors.addAll(m_barElementColors);
//        
//        for (BinDataModel bin : m_bins) {
//            clone.m_bins.add(bin.clone());   
//        }
//        
//        clone.m_dataRows.addAll(m_dataRows);
//        clone.m_rowsSorted = m_rowsSorted;
//        
//        clone.m_inclMissingValBin = m_inclMissingValBin;
//        clone.m_missingValueBin = m_missingValueBin;
//        final long end = System.currentTimeMillis();
//        LOGGER.debug("HistogramDataModel clone time(ms): " + (end - start));
//        return clone;
//    }
}
