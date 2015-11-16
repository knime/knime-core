/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   14.10.2015 (Adrian Nembach): created
 */

package org.knime.base.node.preproc.rank;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * This is the model implementation of Rank. This node ranks the input data based on the selected ranking field and
 * ranking mode
 *
 * @author Adrian Nembach, KNIME GmbH Konstanz
 */
public class RankNodeModel extends NodeModel {

    enum RankMode {

        STANDARD("Standard"),
        DENSE("Dense"),
        ORDINAL("Ordinal");


        private final String m_string;

        private RankMode(final String str) {
            m_string = str;
        }

        @Override
        public String toString() {
            return m_string;
        }
    }

    /** initial default values. */
    static final RankMode DEFAULT_RANKMODE = RankMode.STANDARD;

    static final String DEFAULT_RANKOUTCOLNAME = "rank";

    static final boolean DEFAULT_RETAINROWORDER = false;

    static final boolean DEFAULT_RANKASLONG = false;

    // available ranking modes
//    static final String[] AVAILABLE_RANKMODES = new String[]{"Standard", "Dense", "Ordinal"};

    // SettingsModels
    private final SettingsModelStringArray m_rankColumns = createRankColumnsModel();

    private final SettingsModelStringArray m_groupColumns = createGroupColumnsModel();

    private final SettingsModelString m_rankMode = createRankModeModel();

    private final SettingsModelStringArray m_rankOrder = createRankOrderModel();

    private final SettingsModelString m_rankOutColName = createRankOutColNameModel();

    private final SettingsModelBoolean m_retainRowOrder = createRetainRowOrderModel();

    private final SettingsModelBoolean m_rankAsLong = createRankAsLongModel();

    // static initiators for SettingsModels
    static SettingsModelStringArray createRankColumnsModel() {
        return new SettingsModelStringArray("RankingColumns", new String[]{});
    }

    static SettingsModelStringArray createGroupColumnsModel() {
        return new SettingsModelStringArray("GroupColumns", new String[]{});
    }

    static SettingsModelString createRankModeModel() {
        return new SettingsModelString("RankMode", DEFAULT_RANKMODE.toString());
    }

    static SettingsModelString createRankOutColNameModel() {
        return new SettingsModelString("RankOutFieldName", DEFAULT_RANKOUTCOLNAME);
    }

    static SettingsModelBoolean createRetainRowOrderModel() {
        return new SettingsModelBoolean("RetainRowOrder", DEFAULT_RETAINROWORDER);
    }

    static SettingsModelStringArray createRankOrderModel() {
        return new SettingsModelStringArray("RankOrder", new String[]{});
    }

    static SettingsModelBoolean createRankAsLongModel() {
        return new SettingsModelBoolean("RankAsLong", DEFAULT_RANKASLONG);
    }

    /**
     * Constructor for the node model.
     */
    protected RankNodeModel() {
        super(1, 1);
    }

    private class OrderCellFactory extends SingleCellFactory {

        private long m_rowNum = 0;

        /**
         * @param newColSpec
         */
        public OrderCellFactory(final DataColumnSpec newColSpec) {
            super(newColSpec);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            return new LongCell(m_rowNum++);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        BufferedDataTable table = inData[0];

        if (table == null) {
            throw new IllegalArgumentException("No input table found");
        }
        if (table.size() < 1) {
            setWarningMessage("Empty input table found");
        }

        // get table spec
        DataTableSpec inSpec = table.getDataTableSpec();

        // get grouping columns
        List<String> groupCols = Arrays.asList(m_groupColumns.getStringArrayValue());
        // get ranking columns
        List<String> rankCols = Arrays.asList(m_rankColumns.getStringArrayValue());

        // get indices of ranking and grouping columns
        int[] groupColIndices = getIndicesFromColNameList(groupCols, inSpec);
        int[] rankColIndices = getIndicesFromColNameList(rankCols, inSpec);
        // get rank mode
        String rankMode = m_rankMode.getStringValue();

        // calculate number of steps
        double numSteps = 2;
        if (m_retainRowOrder.getBooleanValue()) {
            numSteps += 3;
        }

        // insert extra column containing the original order of the input table
        final String rowOrder = "rowOrder";
        if (m_retainRowOrder.getBooleanValue()) {
            ColumnRearranger cr = new ColumnRearranger(inSpec);
            DataColumnSpec rowOrderSpec = new DataColumnSpecCreator(rowOrder, LongCell.TYPE).createSpec();
            OrderCellFactory cellFac = new OrderCellFactory(rowOrderSpec);
            cr.append(cellFac);
            table = exec.createColumnRearrangeTable(table, cr, exec.createSubProgress(1 / numSteps));
            inSpec = table.getDataTableSpec();
        }

        // set boolean array to indicate ascending ranking columns
        String[] orderRank = m_rankOrder.getStringArrayValue();
        boolean[] ascRank = new boolean[orderRank.length];
        for (int i = 0; i < ascRank.length; i++) {
            ascRank[i] = (orderRank[i].equals("Ascending")) ? true : false;
        }

        // sort by rank
        BufferedDataTable sortedTable =
            new BufferedDataTableSorter(table, rankCols, ascRank).sort(exec.createSubExecutionContext(1 / numSteps));

        // prepare appending of rank column
        ColumnRearranger columnRearranger = new ColumnRearranger(sortedTable.getDataTableSpec());
        DataColumnSpec newColSpec = null;
        boolean rankAsLong = m_rankAsLong.getBooleanValue();
        if (rankAsLong) {
            newColSpec = new DataColumnSpecCreator(m_rankOutColName.getStringValue(), LongCell.TYPE).createSpec();
        } else {
            newColSpec = new DataColumnSpecCreator(m_rankOutColName.getStringValue(), IntCell.TYPE).createSpec();
        }

        int initialHashtableCapacity = 11;
        if (!groupCols.isEmpty()) {
            initialHashtableCapacity = (int)Math.sqrt(table.size());
        }

        // append rank column
        columnRearranger.append(new RankCellFactory(newColSpec, groupColIndices, rankColIndices, rankMode, rankAsLong,
            initialHashtableCapacity));
        BufferedDataTable out = exec.createColumnRearrangeTable(sortedTable, columnRearranger,
            exec.createSubExecutionContext(1 / numSteps));

        if (m_retainRowOrder.getBooleanValue()) {
            // recover row order
            LinkedList<String> sortBy = new LinkedList<String>();
            sortBy.add(rowOrder);
            out = new BufferedDataTableSorter(out, sortBy, new boolean[]{true})
                .sort(exec.createSubExecutionContext(1 / numSteps));
            // remove order column
            ColumnRearranger cr = new ColumnRearranger(out.getDataTableSpec());
            cr.remove(rowOrder);
            out = exec.createColumnRearrangeTable(out, cr, exec.createSubExecutionContext(1 / numSteps));
        }

        return new BufferedDataTable[]{out};
    }

    private int[] getIndicesFromColNameList(final List<String> colNames, final DataTableSpec inSpec) {
        int[] colIndices = new int[colNames.size()];
        int iterator = 0;
        for (String colName : colNames) {
            colIndices[iterator++] = inSpec.findColumnIndex(colName);
        }
        return colIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        DataTableSpec tableSpec = inSpecs[0];
        String[] rankCols = m_rankColumns.getStringArrayValue();

        // check if atleast one ranking column is selected
        if (rankCols.length == 0) {
            throw new InvalidSettingsException("No ranking column is selected.");
        }

        // check if ranking columns are present in input table
        for (String colName : rankCols) {
            if (!tableSpec.containsName(colName)) {
                throw new InvalidSettingsException(
                    "The selected ranking column " + colName + "is not contained in the input table specification.");
            }
        }

        // check if grouping columns are present in input table
        String[] groupCols = m_groupColumns.getStringArrayValue();
        for (String colName : groupCols) {
            if (!tableSpec.containsName(colName)) {
                throw new InvalidSettingsException(
                    "The selected grouping column" + colName + "is not contained in the input table specification.");
            }
        }

        // check if a name for the column that will contain the ranks in the outputs is provided.
        if (m_rankOutColName.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("There is no name for the output rank column provided.");
        }

        return new DataTableSpec[]{createOutSpec(tableSpec, m_rankAsLong.getBooleanValue())};
    }

    // create the DataTableSpec for the output table
    private DataTableSpec createOutSpec(final DataTableSpec inSpec, final boolean rankAsLong) {

        DataColumnSpec outCol = null;
        if (rankAsLong) {
            outCol = new DataColumnSpecCreator(m_rankOutColName.getStringValue(), LongCell.TYPE).createSpec();
        } else {
            outCol = new DataColumnSpecCreator(m_rankOutColName.getStringValue(), IntCell.TYPE).createSpec();
        }
        return new DataTableSpec(inSpec, new DataTableSpec(outCol));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        m_groupColumns.saveSettingsTo(settings);
        m_rankColumns.saveSettingsTo(settings);
        m_rankMode.saveSettingsTo(settings);
        m_rankOrder.saveSettingsTo(settings);
        m_rankOutColName.saveSettingsTo(settings);
        m_retainRowOrder.saveSettingsTo(settings);
        m_rankAsLong.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_groupColumns.loadSettingsFrom(settings);
        m_rankColumns.loadSettingsFrom(settings);
        m_rankMode.loadSettingsFrom(settings);
        m_rankOrder.loadSettingsFrom(settings);
        m_rankOutColName.loadSettingsFrom(settings);
        m_retainRowOrder.loadSettingsFrom(settings);
        m_rankAsLong.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_groupColumns.validateSettings(settings);
        m_rankColumns.validateSettings(settings);
        m_rankMode.validateSettings(settings);
        m_rankOrder.validateSettings(settings);
        m_rankOutColName.validateSettings(settings);
        m_retainRowOrder.validateSettings(settings);
        m_rankAsLong.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

}
