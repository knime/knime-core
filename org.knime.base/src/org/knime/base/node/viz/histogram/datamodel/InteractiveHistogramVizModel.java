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
 * -------------------------------------------------------------------
 *
 * History
 *    26.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.base.node.viz.histogram.util.ColorColumn;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Extends the {@link AbstractHistogramVizModel} to allow hiliting and
 * column changing.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramVizModel extends AbstractHistogramVizModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InteractiveHistogramVizModel.class);

    /**
     * Compares the value on the given column index with the given
     * {@link DataValueComparator} of to rows.
     * @author Tobias Koetter, University of Konstanz
     */
    private class RowComparator implements Comparator<DataRow> {

        private DataValueComparator m_colComparator;

        private int m_colIdx;

        /**Constructor for class InteractiveHistogramVizModel.RowComparator.
         * @param comparator the {@link DataValueComparator} to use
         * @param colIdx the column index to compare
         *
         */
        public RowComparator(final DataValueComparator comparator,
                final int colIdx) {
            if (comparator == null) {
                throw new IllegalArgumentException(
                        "Column comparator must not be null");
            }
            m_colComparator = comparator;
            m_colIdx = colIdx;
        }

        /**
         * @param comparator the new {@link DataValueComparator} to use
         * @param colIdx the new column index to compare
         */
        public void update(final DataValueComparator comparator,
                final int colIdx) {
            m_colIdx = colIdx;
            m_colComparator = comparator;
        }

        /**
         * {@inheritDoc}
         */
        public int compare(final DataRow o1, final DataRow o2) {
            return m_colComparator.compare(o1.getCell(m_colIdx),
                    o2.getCell(m_colIdx));
        }

    }

    private final DataTableSpec m_tableSpec;

    private int m_xColIdx = -1;

    private DataColumnSpec m_xColSpec;

    private Collection<ColorColumn> m_aggrColumns;

    private RowComparator m_rowComparator;

    private final List<DataRow> m_dataRows;

    private boolean m_isSorted = false;

    /**Constructor for class InteractiveHistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     * @param spec the {@link DataTableSpec}
     * @param rows the {@link DataRow}
     * @param xColSpec the {@link DataColumnSpec} of the selected x column
     * @param aggrColumns the selected aggregation columns
     * @param noOfBins the number of bins to create
     */
    public InteractiveHistogramVizModel(final List<Color> rowColors,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final DataTableSpec spec,  final List<DataRow> rows,
            final DataColumnSpec xColSpec,
            final Collection<ColorColumn> aggrColumns, final int noOfBins) {
        super(rowColors, aggrMethod, layout, noOfBins);
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification must not be null");
        }
        if (xColSpec == null) {
            throw new IllegalArgumentException(
            "No column specification found for selected binning column");
        }
        if (rows == null) {
            throw new IllegalArgumentException("Rows must not be null");
        }
//        if (aggrColumns == null || aggrColumns.size() < 1) {
//            throw new IllegalArgumentException("At least one aggregation "
//                    + "column should be selected");
//
//        }
        if (noOfBins < 1) {
            throw new IllegalArgumentException("Number of bins should be > 0");
        }
        m_tableSpec = spec;
        m_dataRows = rows;
        m_aggrColumns = aggrColumns;
        if (aggrColumns != null && aggrColumns.size() > 1) {
            setShowBarOutline(true);
            setShowBinOutline(true);
        } else {
            setShowBarOutline(false);
            setShowBinOutline(false);
        }
        setXColumn(xColSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setNoOfBins(final int noOfBins) {
        if (super.setNoOfBins(noOfBins)) {
            createBins();
            return true;
        }
        return false;
    }

    /**
     * @param xColSpec the new x column specification
     * @return <code>true</code> if the variable has changed
     */
    public boolean setXColumn(final DataColumnSpec xColSpec) {
        if (xColSpec == null) {
            throw new IllegalArgumentException(
                    "Binning column specification must not be null");
        }
        final int xColIdx = m_tableSpec.findColumnIndex(xColSpec.getName());
        if (xColIdx < 0) {
            throw new IllegalArgumentException("Binning column not found");
        }
        if (xColIdx == m_xColIdx) {
            return false;
        }
        m_xColSpec = xColSpec;
        m_xColIdx = xColIdx;
        m_isSorted = false;
        final DataType xColType = m_xColSpec.getType();
        if (m_rowComparator == null) {
            m_rowComparator =
                new RowComparator(xColType.getComparator(), m_xColIdx);
        } else {
            m_rowComparator.update(xColType.getComparator(), m_xColIdx);
        }
//        if (BinningUtil.binNominal(m_xColSpec, getNoOfBins())) {
//            setBinNominal(true);
//        } else {
//            final boolean wasNominal = isBinNominal();
//            setBinNominal(false);
//            //if we have binned nominal reset the number of bins to default
//            if (wasNominal) {
//                updateNoOfBins(DEFAULT_NO_OF_BINS);
//            }
//        }
        if (xColType.isCompatible(
                DoubleValue.class)) {
            final boolean wasNominal = isBinNominal();
            setBinNominal(false);
            //if we have binned nominal reset the number of bins to default
            if (wasNominal) {
                updateNoOfBins(DEFAULT_NO_OF_BINS);
            }

        } else {
            setBinNominal(true);
        }
        createBins();
        return true;
    }

    /**
     * @param aggrCols the new aggregation columns
     * @return <code>true</code> if the variable has changed
     */
    public boolean setAggregationColumns(
            final Collection<ColorColumn> aggrCols) {
//        if (aggrCols == null || aggrCols.size() < 1) {
//            throw new IllegalArgumentException(
//                    "Aggregation column must not be null");
//        }
        if (aggrCols == null || aggrCols.size() < 1) {
            //force the aggregation method to be count
            if (!AggregationMethod.COUNT.equals(getAggregationMethod())) {
                setAggregationMethod(AggregationMethod.COUNT);
            }
        }
        if (m_aggrColumns != null && aggrCols != null
                && m_aggrColumns.size() == aggrCols.size()
                && m_aggrColumns.containsAll(aggrCols)) {
            return false;
        }
        if ((m_aggrColumns == null || m_aggrColumns.size() <= 1)
                && (aggrCols != null && aggrCols.size() > 1)) {
            setShowBarOutline(true);
            setShowBinOutline(true);
        } else if (m_aggrColumns != null && m_aggrColumns.size() > 1
                && (aggrCols == null || aggrCols.size() < 2)) {
            setShowBarOutline(false);
            setShowBinOutline(false);
        }
        m_aggrColumns = aggrCols;
//        createBins();
        //reset all bins and add the rows to the cleaned bins
        final boolean showMissingWas = isShowMissingValBin();
        setShowMissingValBin(false);
        final List<BinDataModel> bins = getBins();
        final BinDataModel missingValueBin = getMissingValueBin();
        for (final BinDataModel bin : bins) {
            bin.clear();
        }
        missingValueBin.clear();
        addRows2Bins(bins, missingValueBin);
        setShowMissingValBin(showMissingWas);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColorColumn> getAggrColumns() {
        return m_aggrColumns;
    }

    /**
     * @return the data rows in ascending order
     */
    private List<DataRow> getSortedRows() {
        if (!m_isSorted) {
            Collections.sort(m_dataRows, m_rowComparator);
        }
        return m_dataRows;
    }

    /**
     * Creates the bins for the currently set binning information
     * and adds all data rows to the corresponding bin.
     */
    private void createBins() {
        LOGGER.debug("Entering createBins() of class HistogramVizModel.");
        final long startBinTimer = System.currentTimeMillis();
        List<InteractiveBinDataModel> bins;
        if (isBinNominal()) {
            bins = BinningUtil.createInteractiveNominalBins(getXColumnSpec());
        } else {
            //create the new bins
            bins = BinningUtil.createInteractiveIntervalBins(getXColumnSpec(),
                    getNoOfBins());
        }
        final BinDataModel missingValBin = new InteractiveBinDataModel(
                AbstractHistogramVizModel.MISSING_VAL_BAR_CAPTION, 0, 0);
        final long startAddRowTimer = System.currentTimeMillis();
        addRows2Bins(bins, missingValBin);
        final long end = System.currentTimeMillis();
        //add the created bins to the super implementation
        setBins(bins, missingValBin);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" Total time to create " + bins.size() + " bins: "
                    + (end - startBinTimer) + " in ms.\n"
                    + "Time to create bins: "
                    + (startAddRowTimer - startBinTimer)
                    + " in ms.\n"
                    + "Time to add rows: "
                    + (end - startAddRowTimer) + " in ms.");
            LOGGER.debug("Exiting createBins() of class HistogramVizModel.");
        }
    }

    /**
     *This method should loop through all data rows and should add each row
     *to the corresponding bin by calling the
     *{@link #addDataRow2Bin(int, DataCell, Color, DataCell,
     *Collection, DataCell[])} method.
     * @param missingValBin the bin for missing values
     * @param bins the different bins
     */
    private void addRows2Bins(final List<? extends BinDataModel> bins,
            final BinDataModel missingValBin) {
//      add the data rows to the new bins
        int startBin = 0;
        if (m_aggrColumns == null || m_aggrColumns.size() < 1) {
            //if the user hasn't selected a aggregation column
            for (final DataRow row : getSortedRows()) {
                final DataCell xCell = row.getCell(m_xColIdx);
                final Color color =
                    m_tableSpec.getRowColor(row).getColor(false, false);
                final RowKey id = row.getKey();
                try {
                    startBin = BinningUtil.addDataRow2Bin(
                            isBinNominal(), bins, missingValBin, startBin,
                            xCell, color, id, m_aggrColumns,
                            DataType.getMissingCell());
                } catch (final IllegalArgumentException e) {
                    if (!BinningUtil.checkDomainRange(xCell,
                            getXColumnSpec())) {
                        throw new IllegalStateException(
                            "Invalid column domain for column "
                            + m_xColSpec.getName()
                            + ". " + e.getMessage());
                    }
                    throw e;
                }
            }
        } else {
            final DataTableSpec tableSpec = getTableSpec();
            final int aggrSize = m_aggrColumns.size();
            final int[] aggrIdx = new int[aggrSize];
            int i = 0;
            for (final ColorColumn aggrColumn : m_aggrColumns) {
                aggrIdx[i++] = tableSpec.findColumnIndex(
                        aggrColumn.getColumnName());
            }
            for (final DataRow row : getSortedRows()) {
                final DataCell xCell = row.getCell(m_xColIdx);
                final Color color =
                    m_tableSpec.getRowColor(row).getColor(false, false);
                final RowKey id = row.getKey();
                final DataCell[] aggrVals = new DataCell[aggrSize];
                for (int j = 0, length = aggrIdx.length; j < length; j++) {
                    aggrVals[j] = row.getCell(aggrIdx[j]);
                }
                try {
                    startBin = BinningUtil.addDataRow2Bin(
                            isBinNominal(), bins, missingValBin, startBin,
                            xCell, color, id, m_aggrColumns, aggrVals);
                } catch (final IllegalArgumentException e) {
                        if (!BinningUtil.checkDomainRange(xCell,
                                getXColumnSpec())) {
                            throw new IllegalStateException(
                                "Invalid column domain for column "
                                + m_xColSpec.getName()
                                + ". " + e.getMessage());
                        }
                        throw e;
                    }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsHiliting() {
        return true;
    }

    /**
     * @return the {@link DataTableSpec} of the table on which this
     * histogram based on
     */
    public DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> getHilitedKeys() {
        final Set<RowKey> keys = new HashSet<RowKey>();
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                if (bar.isSelected()) {
                    final Collection<BarElementDataModel>
                    elements = bar.getElements();
                    for (final BarElementDataModel element : elements) {
                        keys.addAll((element).getHilitedKeys());
                    }
                }
            }
        }
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> getSelectedKeys() {
        final Set<RowKey> keys = new HashSet<RowKey>();
        for (final BinDataModel bin : getBins()) {
            if (bin.isSelected()) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    if (bar.isSelected()) {
                        final Collection<BarElementDataModel>
                        elements = bar.getElements();
                        for (final BarElementDataModel element
                                : elements) {
                            if (element.isSelected()) {
                                keys.addAll((element).getKeys());
                            }
                        }
                    }
                }
            }
        }
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateHiliteInfo(final Set<RowKey> hilited,
            final boolean hilite) {
        LOGGER.debug("Entering updateHiliteInfo(hilited, hilite) "
                + "of class InteractiveHistogramVizModel.");
        if (hilited == null || hilited.size() < 1) {
            return;
        }
        final long startTime = System.currentTimeMillis();
        final HistogramHiliteCalculator calculator = getHiliteCalculator();
        for (final BinDataModel bin : getBins()) {
            if (hilite) {
                ((InteractiveBinDataModel)bin).setHilitedKeys(hilited,
                        calculator);
            } else {
                ((InteractiveBinDataModel)bin).removeHilitedKeys(hilited,
                        calculator);
            }
        }
        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for updateHiliteInfo: " + durationTime + " ms");
        LOGGER.debug("Exiting updateHiliteInfo(hilited, hilite) "
                + "of class InteractiveHistogramVizModel.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiliteAll() {
        for (final BinDataModel bin : getBins()) {
            ((InteractiveBinDataModel)bin).clearHilite();
        }
    }
}
