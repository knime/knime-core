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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   12.05.2010 (hofer): created
 */
package org.knime.base.node.viz.crosstable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.general.CountOperator;
import org.knime.base.data.aggregation.numerical.SumOperator;
import org.knime.base.node.preproc.groupby.BigGroupByTable;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.base.node.viz.crosstable.CrosstabStatisticsCalculator.CrosstabStatistics;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;

/**
 * This is the model for the Crosstab node.
 *
 * @author Heiko Hofer
 */
public class CrosstabNodeModel extends NodeModel
        implements BufferedDataTableHolder {
    private final CrosstabNodeSettings m_settings;
    private BufferedDataTable m_outTable;
    private BufferedDataTable m_statOutTable;
    private CrosstabTotals m_totals;
    private CrosstabStatistics m_statistics;

    /**
     * Node returns a new hilite handler instance.
     */
    private final HiLiteTranslator m_hilite = new HiLiteTranslator();
    private static final String INTERNALS_FILE_NAME = "hilite_mapping.xml.gz";


    /**
     * Creates a new model with no input port and one output port.
     */
    public CrosstabNodeModel() {
        super(1, 2);
        m_settings = new CrosstabNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // validate settings for the row variable column
        if (null == m_settings.getRowVarColumn()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(StringValue.class)
                        || c.getType().isCompatible(DoubleValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_settings.setRowVarColumn(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_settings.setRowVarColumn(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
                throw new InvalidSettingsException("No column with allowed "
                        + "type (eg. String, Integer) in input table.");
            }
        }
        if (null == m_settings.getColVarColumn()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(StringValue.class)
                        || c.getType().isCompatible(DoubleValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                throw new InvalidSettingsException("Only one column "
                        + "with allowed "
                        + "type (eg. String, Integer) in input table.");
            } else if (compatibleCols.size() == 2) {
                // auto-configure
                m_settings.setColVarColumn(compatibleCols.get(1));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_settings.setColVarColumn(compatibleCols.get(1));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(1) + "\".");
            } else {
                throw new InvalidSettingsException("No column with allowed "
                        + "type (eg. String, Integer) in input table.");
            }
            // Auto configure weight column
            weightColumnAutoConfigure(inSpecs[0]);
        }
        if (m_settings.getRowVarColumn().equals(
                m_settings.getColVarColumn())) {
            throw new InvalidSettingsException("Please choose different "
                    + "colums for \"Row variable\" and \"Column variable\".");
        }
        if (null != m_settings.getWeightColumn()
                && inSpecs[0].findColumnIndex(
                        m_settings.getWeightColumn()) < 0) {
            throw new InvalidSettingsException("The "
                    + "\"Weight column\" is not found in the input.");
        }
        return new DataTableSpec[]{createOutSpec(inSpecs[0]),
                CrosstabStatistics.createSpec()};
    }

    private void weightColumnAutoConfigure(final DataTableSpec spec)
    throws InvalidSettingsException {
        List<String> compatibleCols = new ArrayList<String>();
        for (DataColumnSpec c : spec) {
            if (c.getType().isCompatible(DoubleValue.class)) {
                compatibleCols.add(c.getName());
            }
        }
        if (null != m_settings.getRowVarColumn()) {
            compatibleCols.remove(m_settings.getRowVarColumn());
        }
        if (null != m_settings.getColVarColumn()) {
            compatibleCols.remove(m_settings.getColVarColumn());
        }
        if (compatibleCols.size() == 1) {
            // auto-configure
            m_settings.setWeightColumn(compatibleCols.get(0));
        } else if (compatibleCols.size() > 1) {
            // auto-guessing
            m_settings.setWeightColumn(compatibleCols.get(0));
            setWarningMessage("Auto guessing: using column \""
                + compatibleCols.get(0) + "\" for weights.");
        } else {
            throw new InvalidSettingsException("No column with allowed "
                + "type (eg. Double, Integer) in input table.");
        }
    }

    private DataTableSpec createOutSpec(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        List<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        DataColumnSpec rowColSpec = inSpec.getColumnSpec(
                m_settings.getRowVarColumn());
        if (null == rowColSpec) {
            throw new InvalidSettingsException("The column used as "
                    + "\"Row variable\" is not found in the input.");
        }
        cspecs.add(rowColSpec);
        DataColumnSpec colColSpec = inSpec.getColumnSpec(
                m_settings.getColVarColumn());
        if (null == colColSpec) {
            throw new InvalidSettingsException("The column used as "
                    + "\"Column variable\" is not found in the input.");
        }
        cspecs.add(colColSpec);
        for (String col : m_settings.getProperties()) {
            cspecs.add(new DataColumnSpecCreator(col, DoubleCell.TYPE)
                    .createSpec());
        }
        return new DataTableSpec(cspecs.toArray(
                new DataColumnSpec[cspecs.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable table = inData[0];
        List<String> cols = Arrays.asList(new String[]{
                m_settings.getRowVarColumn(),
                m_settings.getColVarColumn()
        });
        GroupByTable groupTable = createGroupByTable(
                exec.createSubExecutionContext(0.6), table, cols);

        BufferedDataTable freqTable = groupTable.getBufferedTable();
        // the index of the row variable in the group table
        int rowVarI = 0;
        // the index of the column variable in the group table
        int colVarI = 1;
        // the index of the frequency in the group table
        int freqI = 2;

        CrosstabTotals totals = computeTotals(freqTable, rowVarI, colVarI,
                freqI);
        CrosstabProperties naming = CrosstabProperties.create(
                m_settings.getNamingVersion());
        CrosstabStatisticsCalculator stats = new CrosstabStatisticsCalculator(
                freqTable, rowVarI,
                colVarI, freqI, totals, naming);
        stats.run(exec.createSubExecutionContext(0.1));
        BufferedDataTable propsTable = stats.getTable();
        int cellChiSquareI = propsTable.getDataTableSpec().findColumnIndex(
                naming.getCellChiSquareName());

        // create output table
        BufferedDataContainer cont = exec.createDataContainer(
                createOutSpec(table.getSpec()));
        RowIterator freqIter = freqTable.iterator();
        RowIterator statsIter = propsTable.iterator();

        Map<String, Integer> props = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < m_settings.getProperties().size();
               i++) {
            String prop = m_settings.getProperties().get(i);
            props.put(prop, i + 2);
        }

        for (int i = 0; i < freqTable.getRowCount(); i++) {
            DataCell[] cells = new DataCell[props.size() + 2];
            DataRow freqRow = freqIter.next();
            // add the row variable
            DataCell rowVar = freqRow.getCell(rowVarI);
            cells[0] = rowVar;
            // add the column variable
            DataCell colVar = freqRow.getCell(colVarI);
            cells[1] = colVar;
            // the frequency
            DataCell freqCell = freqRow.getCell(freqI);
            double freq = freqCell.isMissing() ? 0.0
                : ((DoubleValue)freqCell).getDoubleValue();
            addToCells(cells, props, naming.getFrequencyName(),
                    new DoubleCell(freq));
            // the cell chi-square
            DataRow statsRow = statsIter.next();
            addToCells(cells, props, naming.getCellChiSquareName(),
                    statsRow.getCell(cellChiSquareI));

            // the total
            double total = totals.getTotal();
            addToCells(cells, props, naming.getTotalCountName(),
                    new DoubleCell(total));

            // the rowTotal
            double rowTotal = totals.getRowTotal().get(rowVar);
            addToCells(cells, props, naming.getTotalRowCountName(),
                    new DoubleCell(rowTotal));

            // the column Total
            double colTotal = totals.getColTotal().get(colVar);
            addToCells(cells, props, naming.getTotalColCountName(),
                    new DoubleCell(colTotal));

            // the percent = frequency / total
            double percent = 100 * (freq / total);
            addToCells(cells, props, naming.getPercentName(),
                    new DoubleCell(percent));

            // the row percent
            double rowPercent = 0.0 == freq ? 0.0
                    : 100.0 * (freq / rowTotal);
            addToCells(cells, props, naming.getRowPercentName(),
                    new DoubleCell(rowPercent));

            // the col percent
            double colPercent = 0.0 == freq ? 0.0
                    : 100.0 * (freq / colTotal);
            addToCells(cells, props, naming.getColPercentName(),
                    new DoubleCell(colPercent));

            // the expected frequency
            double expected = 0.0 == total ? 0.0
                    : colTotal / total * rowTotal;
            addToCells(cells, props, naming.getExpectedFrequencyName(),
                    new DoubleCell(expected));

            // the deviation (the difference of the frequency to the
            // expected frequency)
            double deviation = freq - expected;
            addToCells(cells, props, naming.getDeviationName(),
                    new DoubleCell(deviation));


            DefaultRow row = new DefaultRow(RowKey.createRowKey(i), cells);
            cont.addRowToTable(row);
        }
        cont.close();
        m_outTable = cont.getTable();
        m_statistics = stats.getStatistics();
        m_statOutTable = stats.getStatistics().getTable();
        m_totals = totals;

        return new BufferedDataTable[]{m_outTable, m_statOutTable};
    }

    /** add the value to the cells.*/
    private void addToCells(final DataCell[] cells,
            final Map<String, Integer> props,
            final String col, final DataCell value) {
        if (props.containsKey(col)) {
            int index = props.get(col);
            cells[index] = value;
        }
    }

    /**
     * Computes row and column totals of the cross tabulation.
     */
    private CrosstabTotals computeTotals(final BufferedDataTable freqTable,
            final int rowVarI, final int colVarI, final int freqI) {
        Map<DataCell, Double> rowTotal =
            new LinkedHashMap<DataCell, Double>();
        Map<DataCell, Double> colTotal =
            new LinkedHashMap<DataCell, Double>();
        double total = 0;
        for (DataRow row : freqTable) {
            DataCell rowVar = row.getCell(rowVarI);
            DataCell colVar = row.getCell(colVarI);
            DataCell freqCell = row.getCell(freqI);
            double freq = freqCell.isMissing() ? 0.0
                : ((DoubleValue)freqCell).getDoubleValue();
            // sum up the row totals
            addToTotals(rowTotal, rowVar, freq);
            // sum up the column totals
            addToTotals(colTotal, colVar, freq);
            // the total
            total += freq;
        }

        // sort since we depend on a correct ordering in e.g. the view
        if (rowTotal.size() > 1) {
            rowTotal = sortTotals(rowTotal);
        }
        if (colTotal.size() > 1) {
            colTotal = sortTotals(colTotal);
        }

        return new CrosstabTotals(rowTotal, colTotal, total);
    }

    private void addToTotals(final Map<DataCell, Double> totals,
            final DataCell var, final double freq) {
        double rowValue = totals.containsKey(var)
            ? totals.get(var) + freq : freq;
        totals.put(var, rowValue);
    }

    private Map<DataCell, Double> sortTotals(
            final Map<DataCell, Double> totals) {
        List<DataCell> keys = new ArrayList<DataCell>();
        keys.addAll(totals.keySet());
        Collections.sort(keys, keys.get(0).getType().getComparator());
        Map<DataCell, Double> sorted =
            new LinkedHashMap<DataCell, Double>();
        for (DataCell key : keys) {
            sorted.put(key, totals.get(key));
        }
        return sorted;
    }

    /**
     * Create group-by table.
     * @param exec execution context
     * @param table input table to group
     * @param groupByCols column selected for group-by operation
     * @return table with group and aggregation columns
     * @throws CanceledExecutionException if the group-by table generation was
     *         canceled externally
     */
    private final GroupByTable createGroupByTable(final ExecutionContext exec,
            final BufferedDataTable table, final List<String> groupByCols)
            throws CanceledExecutionException {
        final int maxUniqueVals = Integer.MAX_VALUE;
        final boolean sortInMemory = false;
        final boolean enableHilite = m_settings.getEnableHiliting();
        final boolean retainOrder = false;

        final ColumnNamePolicy colNamePolicy =
            ColumnNamePolicy.AGGREGATION_METHOD_COLUMN_NAME;
        final GlobalSettings globalSettings = new GlobalSettings(maxUniqueVals);

        ColumnAggregator collAggregator = null;
        if (null != m_settings.getWeightColumn()) {
            final String weightColumn = m_settings.getWeightColumn();
            // the column aggregator for the weighting column
            final boolean inclMissing = false;
            DataColumnSpec originalColSpec =
                table.getDataTableSpec().getColumnSpec(weightColumn);
            OperatorColumnSettings opColSettings = new OperatorColumnSettings(
                    inclMissing, originalColSpec);
            collAggregator = new ColumnAggregator(
                    originalColSpec,
                    new NonNegativeSumOperator(globalSettings, opColSettings),
                    inclMissing);
        } else {
            // use any column, does not matter as long as it exists and
            // include missing is true;
            final boolean inclMissing = true;
            DataColumnSpec originalColSpec =
                table.getDataTableSpec().getColumnSpec(groupByCols.get(0));
            OperatorColumnSettings opColSettings = new OperatorColumnSettings(
                    inclMissing, originalColSpec);
            collAggregator = new ColumnAggregator(
                    originalColSpec,
                    new CountOperator(globalSettings, opColSettings),
                    inclMissing);
        }

        GroupByTable resultTable = new BigGroupByTable(exec, table, groupByCols,
                    new ColumnAggregator[]{collAggregator},
                    globalSettings, sortInMemory, enableHilite, colNamePolicy,
                    retainOrder);

        if (enableHilite) {
            setHiliteMapping(new DefaultHiLiteMapper(
                    resultTable.getHiliteMapping()));
        }
        // check for skipped columns
        final String warningMsg = resultTable.getSkippedGroupsMessage(3, 3);
        if (warningMsg != null) {
            setWarningMessage(warningMsg);
        }
        return resultTable;
    }


    /**
     * Returns <code>true</code> if model is available, i.e. node has been
     * executed.
     *
     * @return if model has been executed
     */
    boolean isDataAvailable() {
        return m_outTable != null && m_statOutTable != null;
    }

    /**
     * Get the output table of the crosstab node.
     *
     * @return the output table
     */
    BufferedDataTable getOutTable() {
        return m_outTable;
    }

    /**
     * Get the settings of the node.
     *
     * @return the settings object
     */
    CrosstabNodeSettings getSettings() {
        return m_settings;
    }

    /**
     * Get a wrapper for the row count totals, the column count totals and
     * the total count.
     * @return the wrapper with the totals
     */
    CrosstabTotals getTotals() {
        if (null != m_totals) {
            return m_totals;
        }
        if (null != m_outTable) {
            DataTableSpec spec = m_outTable.getDataTableSpec();
            int rowVarI = spec.findColumnIndex(m_settings.getRowVarColumn());
            int colVarI = spec.findColumnIndex(m_settings.getColVarColumn());
            CrosstabProperties naming = CrosstabProperties.create(
                    m_settings.getNamingVersion());
            int freqI = spec.findColumnIndex(naming.getFrequencyName());
            m_totals = computeTotals(m_outTable, rowVarI, colVarI, freqI);
            return m_totals;
        } else {
            return null;
        }
    }

    /**
     * Get a wrapper for the statistics.
     * @return the wrapper with the statistics
     */
    CrosstabStatistics getStatistics() {
        if (null != m_statistics) {
            return m_statistics;
        }
        if (null != m_statOutTable) {
            m_statistics = new CrosstabStatistics(m_statOutTable);
            return m_statistics;
        } else {
            return null;
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{
                m_outTable, m_statOutTable};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        if (tables.length > 1) {
            m_outTable = tables[0];
            m_statOutTable = tables[1];
        }
    }

    /**
     * Applies a new mapping to the hilite translator.
     * @param mapper new hilite mapping, or null
     */
    protected final void setHiliteMapping(final DefaultHiLiteMapper mapper) {
        m_hilite.setMapper(mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_hilite.setMapper(null);
        m_outTable = null;
        m_statOutTable = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        m_hilite.removeAllToHiliteHandlers();
        m_hilite.addToHiLiteHandler(hiLiteHdl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hilite.getFromHiLiteHandler();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        if (m_settings.getEnableHiliting()) {
            final NodeSettingsRO config = NodeSettings
                    .loadFromXML(new FileInputStream(new File(nodeInternDir,
                            INTERNALS_FILE_NAME)));
            try {
                setHiliteMapping(DefaultHiLiteMapper.load(config));
                m_hilite.addToHiLiteHandler(getInHiLiteHandler(0));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        if (m_settings.getEnableHiliting()) {
            final NodeSettings config = new NodeSettings("hilite_mapping");
            final DefaultHiLiteMapper mapper = (DefaultHiLiteMapper) m_hilite
                    .getMapper();
            if (mapper != null) {
                mapper.save(config);
            }
            config.saveToXML(new FileOutputStream(new File(nodeInternDir,
                    INTERNALS_FILE_NAME)));
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new CrosstabNodeSettings().loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    /**
     * Container class holding the row count totals, the column count totals
     * and the total count of the cross tabulation.
     *
     * @author Heiko Hofer
     */
    static class CrosstabTotals {
        private final Map<DataCell, Double> m_rowTotal;
        private final Map<DataCell, Double> m_colTotal;
        private final double m_total;

        /**
         * @param rowTotal the row count totals
         * @param colTotal the column count totals
         * @param total the total count
         */
        public CrosstabTotals(final Map<DataCell, Double> rowTotal,
                final Map<DataCell, Double> colTotal, final double total) {
            super();
            this.m_rowTotal = rowTotal;
            this.m_colTotal = colTotal;
            this.m_total = total;
        }

        /**
         * Get the row count totals.
         * @return the row count totals
         */
        Map<DataCell, Double> getRowTotal() {
            return m_rowTotal;
        }

        /**
         * Get the column count totals.
         * @return the column count totals
         */
        Map<DataCell, Double> getColTotal() {
            return m_colTotal;
        }

        /**
         * Get the total count.
         * @return the total count
         */
        double getTotal() {
            return m_total;
        }
    }

    private class NonNegativeSumOperator extends SumOperator {

        /**
         * @param globalSettings
         * @param opColSettings
         */
        public NonNegativeSumOperator(final GlobalSettings globalSettings,
                final OperatorColumnSettings opColSettings) {
            super(globalSettings, opColSettings);
            // TODO Auto-generated constructor stub
        }

        /**
         * @param operatorData
         * @param globalSettings
         * @param opColSettings
         */
        public NonNegativeSumOperator(final OperatorData operatorData,
                final GlobalSettings globalSettings,
                final OperatorColumnSettings opColSettings) {
            super(operatorData, globalSettings, opColSettings);
            // TODO Auto-generated constructor stub
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            final double d = ((DoubleValue)cell).getDoubleValue();
            if (d < 0) {
                setWarningMessage("The weight columns contains negative"
                        + " which will be ignored.");
                return false;
            } else {
                return super.computeInternal(cell);
            }
        }

    }
}
