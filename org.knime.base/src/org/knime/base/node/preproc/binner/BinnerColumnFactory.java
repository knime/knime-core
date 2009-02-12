/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   02.08.2005 (gabriel): created
 */
package org.knime.base.node.preproc.binner;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeSettingsWO;

/**
 * Factory to generate binned string cells from a selected column which can be
 * either replaced or appended.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class BinnerColumnFactory implements CellFactory {

    private final int m_columnIdx;

    private final Bin[] m_bins;

    private final DataColumnSpec m_columnSpec;

    private final boolean m_append;

    /**
     * A binned column created by name and a number of bins. The new, binned
     * column is either append or replaced in the current table.
     * 
     * @param columnIdx the column index to bin
     * @param name the new binned column name
     * @param bins a set of bins
     * @param append append or replace column
     */
    BinnerColumnFactory(final int columnIdx, final Bin[] bins,
            final String name, final boolean append) {
        m_columnIdx = columnIdx;
        m_bins = bins;
        // ensures that all bin names are available as possible values 
        // in the column spec, the buffereddatatable will iterate all values
        // (and hence determine the possible values) but some of the names 
        // might not be used in the table (no value for this bin)
        StringCell[] binNames = new StringCell[bins.length];
        for (int i = 0; i < binNames.length; i++) {
            binNames[i] = new StringCell(bins[i].getBinName());
        }
        DataColumnDomain dom = 
            new DataColumnDomainCreator(binNames).createDomain();
        DataColumnSpecCreator specCreator = 
            new DataColumnSpecCreator(name, StringCell.TYPE);
        specCreator.setDomain(dom);
        m_columnSpec = specCreator.createSpec();
        m_append = append;
    }

    /**
     * @return the column index to bin
     */
    int getColumnIndex() {
        return m_columnIdx;
    }

    /**
     * @return if this bin is appended to the table
     */
    boolean isAppendedColumn() {
        return m_append;
    }

    /**
     * @return the column name to append
     */
    DataColumnSpec getColumnSpec() {
        return m_columnSpec;
    }

    /**
     * @return number of bins
     */
    int getNrBins() {
        return m_bins.length;
    }

    /**
     * Return <code>Bin</code> for index.
     * 
     * @param index for this index
     * @return the assigned bin
     */
    Bin getBin(final int index) {
        return m_bins[index];
    }

    /**
     * Apply a value to this bining trying to cover it at all available
     * <code>Bin</code>s.
     * 
     * @param cell the value to cover
     * @return the bin's name as DataCell which cover's this value
     */
    DataCell apply(final DataCell cell) {
        if (cell.isMissing()) {
            return DataType.getMissingCell();
        }
        for (int i = 0; i < m_bins.length; i++) {
            if (m_bins[i].covers(cell)) {
                return new StringCell(m_bins[i].getBinName());
            }
        }
        return DataType.getMissingCell();
    }

    /**
     * General bin.
     */
    public interface Bin {

        /**
         * @return this bin's name
         */
        String getBinName();

        /**
         * @param value the double value
         * @return if covered by this interval
         */
        boolean covers(DataCell value);

        /**
         * Save this bin.
         * 
         * @param set to this settings
         */
        void saveToSettings(NodeSettingsWO set);

    }

    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        DataCell cell = row.getCell(m_columnIdx);
        return new DataCell[]{apply(cell)};
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        return new DataColumnSpec[]{m_columnSpec};
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(1.0 * curRowNr / rowCount, "Binning row: "
                + lastKey.getString());
    }
}
