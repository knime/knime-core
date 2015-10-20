/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.missingvaluecolfilter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * The model for the missing value column filter which removes all columns with more missing values than a
 * certain percentage.
 *
 * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich, Switzerland
 */
public class MissingValueColumnFilterNodeModel extends NodeModel {
    private final DataColumnSpecFilterConfiguration m_conf = createDCSFilterConfiguration();

    private final SettingsModelDouble m_percentage = createSettingsModelNumber();

    /** Creates a new filter model with one and in- and output. */
    public MissingValueColumnFilterNodeModel() {
        super(1, 1);
    }

    /** Nothing to do. */
    @Override
    protected void reset() {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable inputTable = inData[0];
        DataTableSpec dataTableSpec = inputTable.getDataTableSpec();
        double[] percentages = new double[dataTableSpec.getNumColumns()];
        Arrays.fill(percentages, -1);

        String[] included = m_conf.applyTo(dataTableSpec).getIncludes();
        for (String column : included) {
            percentages[dataTableSpec.findColumnIndex(column)] = m_percentage.getDoubleValue();
        }

        long[] missingCount = new long[dataTableSpec.getNumColumns()];
        final double rowCount = inputTable.size();
        double processedRows = 0;
        for (DataRow row : inputTable) {
            exec.setProgress(processedRows++ / rowCount);

            for (int i = 0; i < row.getNumCells(); i++) {
                    if (row.getCell(i).isMissing()) {
                        missingCount[i]++;
                    }
            }
        }

        ColumnRearranger r = new ColumnRearranger(dataTableSpec);
        int alreadyRemoved = 0;
        for (int i = 0; i < percentages.length; i++) {
            if (percentages[i] > -1) {
                if ((missingCount[i] / rowCount) * 100 >= percentages[i]) {
                    r.remove(i - alreadyRemoved++);
                }
            }
        }

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inputTable, r, exec)};
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[0].getNumColumns() == 0) {
            throw new InvalidSettingsException("No input table available.");
        }
        // we have to take a look at the whole table
        return null;
    }

    /**
     * Writes number of filtered columns, and the names as {@link org.knime.core.data.DataCell} to the given settings.
     *
     * @param settings the object to save the settings into
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_conf.saveConfiguration(settings);
        m_percentage.saveSettingsTo(settings);
    }

    /**
     * Reads the filtered columns.
     *
     * @param settings to read from
     * @throws InvalidSettingsException if the settings does not contain the size or a particular column key
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_conf.loadConfigurationInModel(settings);
        m_percentage.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = createDCSFilterConfiguration();
        conf.loadConfigurationInModel(settings);

        SettingsModelDouble percentage = createSettingsModelNumber();
        percentage.validateSettings(settings);
    }

    /**
     * A new configuration to store the settings. Also enables the type filter.
     *
     * @return ...
     */
    static final DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration("column-filter");
    }

    /**
     * Configuration to save the percentage
     * @return a settings model to store an integer
     */
    static final SettingsModelDouble createSettingsModelNumber() {
        return new SettingsModelDoubleBounded("missing_value_percentage", 90, 0, 100);
    }
}
