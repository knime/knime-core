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
 *
 */
package org.knime.base.node.stats.correlation.cronbach;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.statistics.StatisticCalculator;
import org.knime.base.data.statistics.calculation.Variance;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 *
 * @author Iris Adae, University of Konstanz
 */
final class CronbachNodeModel extends NodeModel {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(CorrelationComputeNodeModel.class);

    private SettingsModelColumnFilter2 m_columnFilterModel;
    /** One input, one output.
     */
    CronbachNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable in = (BufferedDataTable)inData[0];
        final DataTableSpec inSpec = in.getDataTableSpec();
        ColumnRearranger filteredTableRearranger = new ColumnRearranger(inSpec);
        String[] includeNames = m_columnFilterModel.applyTo(inSpec).getIncludes();
        filteredTableRearranger.keepOnly(includeNames);
        final BufferedDataTable filteredTable = exec.createColumnRearrangeTable(
                in, filteredTableRearranger, exec.createSilentSubExecutionContext(0.0));
        final DataTableSpec filteredTableSpec = filteredTable.getDataTableSpec();

        // step1 get variance for all columns
        Variance my = new Variance(filteredTableSpec.getColumnNames());
        StatisticCalculator sc = new StatisticCalculator(filteredTableSpec, my);
        sc.evaluate(filteredTable, exec.createSubExecutionContext(0.5));

        double[] sum = new double[filteredTable.getRowCount()];

        // step2 get variance for the overall sum
        ExecutionContext exec2 = exec.createSubExecutionContext(0.5);
        int rowCount = filteredTable.getRowCount();
        int i = 0;
        for (DataRow row : filteredTable) {
            sum[i] = 0;
            exec2.checkCanceled();
            exec2.setProgress(i * 1.0 / rowCount, "Statisics calculation row " + i + " of " + rowCount);
            for (DataCell cell : row) {
                if (!cell.isMissing()) {
                    double value = ((DoubleValue)cell).getDoubleValue();
                    sum[i] += value;
                } else {
                    throw new InvalidSettingsException("Missing Values are not supported. "
                            + "Please resolve them with the Missing Value node.");
                }
            }
            i++;
        }

        exec.setMessage("Caluating Crombach over all Columns");
        double cronbach = 0;
        for (String s : filteredTableSpec.getColumnNames()) {
            cronbach += my.getResult(s);
            exec.checkCanceled();
        }

        org.apache.commons.math3.stat.descriptive.moment.Variance v =
            new org.apache.commons.math3.stat.descriptive.moment.Variance();
        cronbach /= v.evaluate(sum);

        double k = filteredTableSpec.getNumColumns();
        cronbach = k / (k - 1) * (1.0 - cronbach);
        BufferedDataContainer out = exec.createDataContainer(getDataTableSpec());

        if (in.getRowCount() <= 0) {
            setWarningMessage("Empty input table, no value calculated!");
        }

        DataRow r = new DefaultRow(new RowKey("Cronbach"), new DoubleCell(cronbach));
        out.addRowToTable(r);
        out.close();
        return new BufferedDataTable[]{out.getTable()};

    }

    /**
     * @return the data table spec of this node.
     */
    private DataTableSpec getDataTableSpec() {
        DataColumnSpecCreator dcs = new DataColumnSpecCreator("Cronbach", DoubleCell.TYPE);
        return new DataTableSpec(dcs.createSpec());
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
        if (!in.containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException("No double compatible columns in input");
        }
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
            throw new InvalidSettingsException("Please include at least two numerical columns!");
        }
        return new PortObjectSpec[]{getDataTableSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_columnFilterModel != null) {
            m_columnFilterModel.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        createColumnFilterModel().validateSettings(settings);
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

    /** Factory method to instantiate a default settings object, used
     * in constructor and in dialog.
     * @return A new default settings object.
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("include-list", DoubleValue.class);
    }

}
