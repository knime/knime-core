/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   13.05.2012 (kilian): created
 */
package org.knime.base.node.util.extracttabledimension;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Extracts number of rows and columns of input table and outputs numbers as
 * data table and flow variables.
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class ExtractTableDimensionNodeModel extends NodeModel {

    /**
     * Default name of output column.
     */
    public static final String DEF_COL_NAME = "Dimensions";

    /**
     * Default name of flow variable and row key for number of columns.
     */
    public static final String DEF_NOCOLS_VAR = "Number Columns";

    /**
     * Default name of flow variable and row key for number of rows.
     */
    public static final String DEF_NOROWS_VAR = "Number Rows";


    /**
     * Creates new instance of <code>ExtractTableDimensionNodeModel</code>.
     */
    public ExtractTableDimensionNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];

        pushFlowVariableInt(DEF_NOROWS_VAR, -1);
        pushFlowVariableInt(DEF_NOCOLS_VAR, spec.getNumColumns());

        return new DataTableSpec[] {createDataTableSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable data = inData[0];

        BufferedDataContainer bdc = exec.createDataContainer(
                createDataTableSpec());

        int noRows = data.getRowCount();
        int noCols = data.getDataTableSpec().getNumColumns();

        bdc.addRowToTable(new DefaultRow(DEF_NOROWS_VAR,
                new DataCell[]{new IntCell(noRows)}));
        bdc.addRowToTable(new DefaultRow(DEF_NOCOLS_VAR,
                new DataCell[]{new IntCell(noCols)}));
        bdc.close();

        pushFlowVariableInt(DEF_NOROWS_VAR, noRows);
        pushFlowVariableInt(DEF_NOCOLS_VAR, noCols);

        return new BufferedDataTable[] {bdc.getTable()};
    }


    /**
     * Creates and returns the data table spec of the output table.
     * @return The data table spec of the output table.
     */
    private static final DataTableSpec createDataTableSpec() {
        DataColumnSpec colSpec = new DataColumnSpecCreator(DEF_COL_NAME,
                IntCell.TYPE).createSpec();
        return new DataTableSpec(colSpec);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do ...
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }
}
