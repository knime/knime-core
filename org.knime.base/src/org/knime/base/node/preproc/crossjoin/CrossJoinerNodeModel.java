/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 */
package org.knime.base.node.preproc.crossjoin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * This is the model implementation of CrossJoiner.
 *
 *
 * @author Alexander Fillbrunn, Universität Konstanz
 * @author Iris Adae, Universität Konstanz
 */
final class CrossJoinerNodeModel extends NodeModel {

    private final SettingsModelString m_rightColumnNameSuffix = createRightColumnNameSuffixSettingsModel();

    private final SettingsModelIntegerBounded m_cacheSize = createCacheSizeSettingsModel();

    private final SettingsModelString m_rkseparator = createRowKeySeparatorSettingsModel();

    private final SettingsModelBoolean m_showLeft = createshowFirstRowIdsSettingsModel();

    private final SettingsModelString m_nameLeft = createFirstRowIdsNameSettingsModel(m_showLeft);

    private final SettingsModelBoolean m_showRight = createshowSecondRowIdsSettingsModel();

    private final SettingsModelString m_nameRight = createSecondRowIdsNameSettingsModel(m_showRight);

    /**
     * Constructor for the node model.
     */
    protected CrossJoinerNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        // initialize the output table
        final BufferedDataTableRowOutput output = new BufferedDataTableRowOutput(
            exec.createDataContainer(createSpec(inData[0].getDataTableSpec(), inData[1].getDataTableSpec())));

        // join the tables
        crossJoin(new DataTableRowInput(inData[0]), inData[1], output, inData[0].size() * inData[1].size(), exec);

        // return the joined table
        return new BufferedDataTable[]{output.getDataTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                crossJoin((RowInput)inputs[0], (BufferedDataTable)((PortObjectInput)inputs[1]).getPortObject(),
                    (RowOutput)outputs[0], 0, exec);
            }
        };
    }

    /**
     * Cross joins the left and right data input and writes the result to the output.
     *
     * @param lData the left data input
     * @param rData the right data input
     * @param output the output, i.e., the joined table
     * @param totalNumRows the row count of the resulting table. Use 0 if it's not known in advance
     * @param exec the execution context
     * @throws Exception - If the execution was, e.g., interrupted or canceled
     */
    private void crossJoin(final RowInput lData, final BufferedDataTable rData, final RowOutput output,
        final long totalNumRows, final ExecutionContext exec) throws Exception {
        int chunksize = m_cacheSize.getIntValue();
        double rowCounter = 0;
        final List<DataRow> lDataChunk = new ArrayList<DataRow>();
        final List<DataRow> rDataChunk = new ArrayList<DataRow>();
        boolean finished = false;
        while (!finished) {
            // collect a chunk from the left data input
            fillLeftDataChunk(lDataChunk, lData, chunksize, exec);
            // check whether we reached the end of the left data input
            finished = lDataChunk.size() < chunksize;
            // start joining the chunked data rows of the left data input with the right input data
            if (!lDataChunk.isEmpty()) {
                try (CloseableRowIterator rRowIt = rData.iterator()) {
                    while (rRowIt.hasNext()) {
                        // collect a chunk from the right data input
                        fillRightDataChunk(rDataChunk, rRowIt, chunksize, exec);
                        // join the data rows
                        for (final DataRow lRow : lDataChunk) {
                            for (final DataRow rRow : rDataChunk) {
                                exec.checkCanceled();
                                final DataRow row = joinRows(lRow, rRow);
                                output.push(row);
                                exec.setProgress(++rowCounter / totalNumRows,
                                    () -> "Generating Row " + row.getKey().toString());
                            }
                        }
                        rDataChunk.clear();
                    }
                    lDataChunk.clear();
                }
            }
        }
        output.close();
    }

    /**
     * Fills the provided list with an unprocessed chunk of data rows.
     *
     * @param list the list to store the chunk of data rows to
     * @param rowIt the data row iterator
     * @param chunkSize the size of the chunk to be collected
     * @throws CanceledExecutionException - If the execution was canceled
     */
    private static void fillRightDataChunk(final List<DataRow> list, final CloseableRowIterator rowIt,
        final int chunkSize, final ExecutionContext exec) throws CanceledExecutionException {
        for (int i = 0; i < chunkSize && rowIt.hasNext(); i++) {
            exec.checkCanceled();
            list.add(rowIt.next());
        }
    }

    /**
     * Fills the provided list with an unprocessed chunk of data rows.
     *
     * @param list the list to store the chunk of data rows to
     * @param data the input holding the next chunk of data rows
     * @param chunkSize the size of the chunk to be collected
     * @param exec the execution context
     * @throws InterruptedException - If the execution was interrupted
     * @throws CanceledExecutionException - If the execution was canceled
     */
    private static void fillLeftDataChunk(final List<DataRow> list, final RowInput data, final int chunkSize,
        final ExecutionContext exec) throws InterruptedException, CanceledExecutionException {
        for (int i = 0; i < chunkSize; i++) {
            exec.checkCanceled();
            final DataRow row = data.poll();
            if (row == null) {
                return;
            }
            list.add(row);
        }
    }

    /**
     * Returns a new DataRow composed of the two rows to join. The row-key of the new DataRow is composed of the two
     * individual row-keys joined together with the selected {@link #m_rkseparator delimiter}. Depending on the settings
     * a new left and/or right column containing the corresponding row-keys is created.
     *
     * @param left the first data row (put at the beginning of the new one)
     * @param right the second data row (at the end of the new one)
     * @return a DataRow, containing the cells of both rows and if selected the (original) row-keys in additional
     *         columns
     * @since 2.9.1
     */
    private DataRow joinRows(final DataRow left, final DataRow right) {
        final boolean showLeft = m_showLeft.getBooleanValue();
        final boolean showRight = m_showRight.getBooleanValue();
        final String delimiter = m_rkseparator.getStringValue();

        int numCols = left.getNumCells() + right.getNumCells() + (showLeft ? 1 : 0) + (showRight ? 1 : 0);
        DataCell[] cells = new DataCell[numCols];

        for (int i = 0; i < left.getNumCells(); i++) {
            cells[i] = left.getCell(i);
        }
        for (int i = 0; i < right.getNumCells(); i++) {
            cells[i + left.getNumCells()] = right.getCell(i);
        }

        if (showLeft) {
            cells[left.getNumCells() + right.getNumCells()] = new StringCell(left.getKey().toString());
        }

        if (showRight) {
            cells[left.getNumCells() + right.getNumCells() + (showLeft ? 1 : 0)] =
                new StringCell(right.getKey().toString());
        }

        String newrowkey = left.getKey().getString() + delimiter + right.getKey().getString();
        return new DefaultRow(newrowkey, cells);
    }

    private DataTableSpec createSpec(final DataTableSpec left, final DataTableSpec right) {
        final boolean showLeft = m_showLeft.getBooleanValue();
        final boolean showRight = m_showRight.getBooleanValue();
        int numCols = left.getNumColumns() + right.getNumColumns() + (showLeft ? 1 : 0) + (showRight ? 1 : 0);
        DataColumnSpec[] colSpecs = new DataColumnSpec[numCols];

        final List<String> newcolumns = new LinkedList<String>();
        for (int i = 0; i < left.getNumColumns(); i++) {
            DataColumnSpecCreator c = new DataColumnSpecCreator(left.getColumnSpec(i));
            colSpecs[i] = c.createSpec();
        }
        for (int i = 0; i < right.getNumColumns(); i++) {
            DataColumnSpec spec = right.getColumnSpec(i);
            DataColumnSpecCreator c = new DataColumnSpecCreator(spec);
            String columnname = spec.getName();
            while (left.containsName(columnname) || newcolumns.contains(columnname)) {
                do {
                    columnname += m_rightColumnNameSuffix.getStringValue();
                } while (right.containsName(columnname));
            }
            if (columnname != spec.getName()) {
                // save the new name so we don't generate it twice
                newcolumns.add(columnname);
                // set the new column name to the column spec
                c.setName(columnname);
            }
            colSpecs[i + left.getNumColumns()] = c.createSpec();
        }

        if (showLeft || showRight) {
            DataTableSpec onlyData =
                new DataTableSpec(Arrays.copyOf(colSpecs, left.getNumColumns() + right.getNumColumns()));
            if (showLeft) {
                String colName = DataTableSpec.getUniqueColumnName(onlyData, m_nameLeft.getStringValue());
                DataColumnSpecCreator c = new DataColumnSpecCreator(colName, StringCell.TYPE);
                colSpecs[left.getNumColumns() + right.getNumColumns()] = c.createSpec();
                onlyData = new DataTableSpec(Arrays.copyOf(colSpecs, left.getNumColumns() + right.getNumColumns() + 1));
            }
            if (showRight) {
                String colName = DataTableSpec.getUniqueColumnName(onlyData, m_nameRight.getStringValue());
                DataColumnSpecCreator c = new DataColumnSpecCreator(colName, StringCell.TYPE);
                colSpecs[left.getNumColumns() + right.getNumColumns() + (showLeft ? 1 : 0)] = c.createSpec();
            }
        }
        return new DataTableSpec(colSpecs);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createSpec(inSpecs[0], inSpecs[1])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rightColumnNameSuffix.saveSettingsTo(settings);
        // new since 2.9.1
        m_cacheSize.saveSettingsTo(settings);
        m_rkseparator.saveSettingsTo(settings);
        m_showLeft.saveSettingsTo(settings);
        m_showRight.saveSettingsTo(settings);
        m_nameLeft.saveSettingsTo(settings);
        m_nameRight.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rightColumnNameSuffix.loadSettingsFrom(settings);
        // new since 2.9.1
        if (settings.containsKey(createCacheSizeSettingsModel().getKey())) {
            // one in, all in
            m_cacheSize.loadSettingsFrom(settings);
            m_rkseparator.loadSettingsFrom(settings);
            m_showLeft.loadSettingsFrom(settings);
            m_showRight.loadSettingsFrom(settings);
            m_nameLeft.loadSettingsFrom(settings);
            m_nameRight.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rightColumnNameSuffix.validateSettings(settings);
        SettingsModel createCloneWithValidatedValue = m_rightColumnNameSuffix.createCloneWithValidatedValue(settings);
        if (StringUtils.isBlank(((SettingsModelString)createCloneWithValidatedValue).getStringValue())) {
            throw new InvalidSettingsException("Empty values for suffix are not allowed!");
        }
        SettingsModel validatedNameLeft = m_nameLeft.createCloneWithValidatedValue(settings);
        if (StringUtils.isBlank(((SettingsModelString)validatedNameLeft).getStringValue())) {
            throw new InvalidSettingsException("Empty values for column name(top) are not allowed!");
        }
        SettingsModel validatedNameRight = m_nameRight.createCloneWithValidatedValue(settings);
        if (StringUtils.isBlank(((SettingsModelString)validatedNameRight).getStringValue())) {
            throw new InvalidSettingsException("Empty values for column name(bottom) are not allowed!");
        }
        // new since 2.9.1
        if (settings.containsKey(createCacheSizeSettingsModel().getKey())) {
            // one in, all in
            m_cacheSize.validateSettings(settings);
            m_rkseparator.validateSettings(settings);
            m_showLeft.validateSettings(settings);
            m_showRight.validateSettings(settings);
            m_nameLeft.validateSettings(settings);
            m_nameRight.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * @return the settings model for the cache size
     */
    static SettingsModelIntegerBounded createCacheSizeSettingsModel() {
        return new SettingsModelIntegerBounded("CFG_CACHE", 1, 1, Integer.MAX_VALUE);
    }

    /**
     * @return the SM for the string separating the two rowkeys
     */
    static SettingsModelString createRowKeySeparatorSettingsModel() {
        return new SettingsModelString("CFG_SEPARATOR", "_");
    }

    /**
     * @return the SM for showing the first tables rowids.
     */
    static SettingsModelBoolean createshowFirstRowIdsSettingsModel() {
        return new SettingsModelBoolean("CFG_SHOW_FIRST", false);
    }

    /**
     * @return the SM for showing the second tables rowids.
     */
    static SettingsModelBoolean createshowSecondRowIdsSettingsModel() {
        return new SettingsModelBoolean("CFG_SHOW_SECOND", false);
    }

    /**
     * @param showFirstRowIdsSettingsModel the enable checker model.
     * @return the column name for the first tables rowids
     */
    static SettingsModelString
        createFirstRowIdsNameSettingsModel(final SettingsModelBoolean showFirstRowIdsSettingsModel) {
        final SettingsModelString settingsModel = new SettingsModelString("CFG_FIRST_COLUMNNAME", "FirstRowIDs");
        showFirstRowIdsSettingsModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                settingsModel.setEnabled(showFirstRowIdsSettingsModel.getBooleanValue());
            }
        });
        settingsModel.setEnabled(showFirstRowIdsSettingsModel.getBooleanValue());
        return settingsModel;
    }

    /**
     * @param showSecondRowIdsSettingsModel the enable checker model.
     * @return the column name for the second tables rowids
     */
    static SettingsModelString
        createSecondRowIdsNameSettingsModel(final SettingsModelBoolean showSecondRowIdsSettingsModel) {
        final SettingsModelString settingsModel = new SettingsModelString("CFG_SECOND_COLUMNNAME", "SecondRowIDs");
        showSecondRowIdsSettingsModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                settingsModel.setEnabled(showSecondRowIdsSettingsModel.getBooleanValue());
            }
        });
        settingsModel.setEnabled(showSecondRowIdsSettingsModel.getBooleanValue());
        return settingsModel;
    }

    /**
     * Creates a settings model for the suffix of duplicate column names in the right table.
     *
     * @return the settings model
     */
    static SettingsModelString createRightColumnNameSuffixSettingsModel() {
        return new SettingsModelString("rigthSuffix", " (#1)");
    }
}
