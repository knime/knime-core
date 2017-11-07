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
 */
package org.knime.base.node.stats.correlation.rank;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.node.preproc.correlation.pmcc.PMCCPortObjectAndSpec;
import org.knime.base.util.HalfDoubleMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 *
 * @author wiswedel, University of Konstanz
 */
final class RankCorrelationComputeNodeModel extends NodeModel implements BufferedDataTableHolder {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(CorrelationComputeNodeModel.class);

    /** the configuration key for using spearmans Rhu.*/
     static final String CFG_SPEARMAN = "Spearmans Rho";
     /** the configuration key for using Kendalls Tau A.*/
     static final String CFG_KENDALLA = "Kendalls Tau A";
     /** the configuration key for using Kendalls Tau B.*/
     static final String CFG_KENDALLB = "Kendalls Tau B";
     /** the configuration key for using Goodman and Kruskals Gamma.*/
     static final String CFG_KRUSKALAL = "Goodman and Kruskal's Gamma";

    private SettingsModelColumnFilter2 m_columnFilterModel;
    private SettingsModelString m_corrType = createTypeModel();

    private BufferedDataTable m_correlationTable;

    /** One input, one output.
     */
    RankCorrelationComputeNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE, PMCCPortObjectAndSpec.TYPE, BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable in = (BufferedDataTable)inData[0];
        final DataTableSpec inSpec = in.getDataTableSpec();
        ColumnRearranger filteredTableRearranger = new ColumnRearranger(inSpec);
        String[] includeNames = m_columnFilterModel.applyTo(inSpec).getIncludes();
        filteredTableRearranger.keepOnly(includeNames);
        final BufferedDataTable filteredTable = exec.createColumnRearrangeTable(
                in, filteredTableRearranger, exec.createSilentSubExecutionContext(0.0));

        final BufferedDataTable noMissTable = filterMissings(filteredTable, exec);
        if (noMissTable.getRowCount() < filteredTable.getRowCount()) {
            setWarningMessage("Rows containing missing values are filtered. Please resolve them"
                 + " with the Missing Value node.");
        }
        double progStep1 = 0.48;
        double progStep2 = 0.48;
        double progFinish = 1.0 - progStep1 - progStep2;
        SortedCorrelationComputer calculator = new SortedCorrelationComputer();
        exec.setMessage("Generate ranking");
        ExecutionContext execStep1 = exec.createSubExecutionContext(progStep1);
        calculator.generateRank(noMissTable, execStep1);
        execStep1.setProgress(1.0);
        exec.setMessage("Calculating correlation values");

        ExecutionContext execStep2 = exec.createSubExecutionContext(progStep2);
        HalfDoubleMatrix correlationMatrix;
        if (m_corrType.getStringValue().equals(CFG_SPEARMAN)) {
            correlationMatrix = calculator.calculateSpearman(execStep2);
        } else {
            correlationMatrix = calculator.calculateKendallInMemory(m_corrType.getStringValue(), execStep2);
        }
        execStep2.setProgress(1.0);
        exec.setMessage("Assembling output");
        ExecutionContext execFinish = exec.createSubExecutionContext(progFinish);
        PMCCPortObjectAndSpec pmccModel = new PMCCPortObjectAndSpec(includeNames, correlationMatrix);
        BufferedDataTable out = pmccModel.createCorrelationMatrix(execFinish);
        m_correlationTable = out;
        if (in.getRowCount() == 0) {
            setWarningMessage("Empty input table! Generating missing values as correlation values.");
        }
        return new PortObject[]{out, pmccModel, calculator.getRankTable()};

    }

    /**
     * @param filteredTable a Buffered Data Table.
     * @param exec The execution context
     * @return the table without any rows containing missing values.
     */
    private BufferedDataTable filterMissings(final BufferedDataTable filteredTable, final ExecutionContext exec) {
        BufferedDataContainer tab = exec.createDataContainer(filteredTable.getDataTableSpec());
        for (DataRow row : filteredTable) {
            boolean includeRow = true;
            // check row for missingvalues
            for (DataCell cell : row) {
                if (cell.isMissing()) {
                    includeRow = false;
                    break;
                }
            }
            if (includeRow) {
                tab.addRowToTable(row);
            }
        }
        tab.close();
        return tab.getTable();
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec in = (DataTableSpec)inSpecs[0];
        final String[] includes;
        if (m_columnFilterModel == null) {
            m_columnFilterModel = createColumnFilterModel();
            // auto-configure, no previous configuration
            m_columnFilterModel.loadDefaults(in);
            includes = m_columnFilterModel.applyTo(in).getIncludes();
            setWarningMessage("Auto configuration: Using all suitable columns (in total " + includes.length + ")");
        } else {
            FilterResult applyTo = m_columnFilterModel.applyTo(in);
            includes = applyTo.getIncludes();
        }
        if (includes.length == 0) {
            throw new InvalidSettingsException("No columns selected");
        }
        return new PortObjectSpec[]{
            PMCCPortObjectAndSpec.createOutSpec(includes), new PMCCPortObjectAndSpec(includes), null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_columnFilterModel != null) {
            m_columnFilterModel.saveSettingsTo(settings);
        }
        m_corrType.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        createColumnFilterModel().validateSettings(settings);
        m_corrType.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (m_columnFilterModel == null) {
            m_columnFilterModel = createColumnFilterModel();
        }
        m_columnFilterModel.loadSettingsFrom(settings);
        m_corrType.loadSettingsFrom(settings);
    }

    /**
     * Getter for correlation table to display. <code>null</code> if not
     * executed.
     * @return the correlationTable
     */
    public DataTable getCorrelationTable() {
        return m_correlationTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** @return A new  settings object for filtering columns.
     */
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("include-list");
    }
    /**
     * @return a new model
     */
     static SettingsModelString createTypeModel() {
        return new SettingsModelString("corr-measure", CFG_SPEARMAN);
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_correlationTable};
    }

    /** {@inheritDoc} */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_correlationTable = tables[0];
    }

    /**
     * @return the list of all correlation types
     */
    static List<String> getCorrelationTypes() {
        LinkedList<String> ret = new LinkedList<>();
        ret.add(CFG_SPEARMAN);
        ret.add(CFG_KENDALLA);
        ret.add(CFG_KENDALLB);
        ret.add(CFG_KRUSKALAL);
        return ret;
    }


}
