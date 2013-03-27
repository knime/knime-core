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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 * History
 *   19.06.2007 (gabriel): created
 */
package org.knime.base.node.io.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseWriterConnection;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * NodeModel of the Database Delete node.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.7
 */
final class DBDeleteRowsNodeModel extends NodeModel {

    /** Config key for the where columns. */
    static final String KEY_WHERE_FILTER_COLUMN = "where_columns";
    private DataColumnSpecFilterConfiguration m_configWHERE;

    private DatabaseConnectionSettings m_loginConfig;

    /** Config key for the table name. */
    static final String KEY_TABLE_NAME = "table_name";
    private String m_tableName;

    /** Config key for the batch size. */
    static final String KEY_BATCH_SIZE = "batch_size";
    private int m_batchSize = 1;

    /** Create a new database UPDATE node model. */
    DBDeleteRowsNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check table name
        if (m_tableName == null || m_tableName.trim().isEmpty()) {
            throw new InvalidSettingsException(
                "Configure node and enter a valid table name.");
        }
        final List<String> whereIncludes = new ArrayList<String>(
                Arrays.asList(m_configWHERE.applyTo(inSpecs[0]).getIncludes()));
        if (whereIncludes.isEmpty()) {
            throw new InvalidSettingsException("No WHERE column selected.");
        }
        // forward input spec with one additional update (=int) column
        final ColumnRearranger colre = createColumnRearranger(inSpecs[0], new int[]{});
        return new DataTableSpec[]{colre.createSpec()};
    }

    private static final String DELETE_ROWS_COLUMN = "DeleteStatus";

    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final int[] updateStatus) {
        final String updateColumn = DataTableSpec.getUniqueColumnName(inSpec, DELETE_ROWS_COLUMN);
        final DataColumnSpec cspec = new DataColumnSpecCreator(updateColumn, IntCell.TYPE).createSpec();
        final ColumnRearranger rearr = new ColumnRearranger(inSpec);
        rearr.append(new SingleCellFactory(cspec) {
            private int m_rowCount = 0;
            @Override
            public DataCell getCell(final DataRow row) {
                return new IntCell(updateStatus[m_rowCount++]);
            }
        });
        return rearr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable inTable = inData[0];
        final DataTableSpec inSpec = inTable.getSpec();
        final String[] whereIncludes = m_configWHERE.applyTo(inSpec).getIncludes();
        // DELETE rows
        final int[] deleteStatus = new int[inTable.getRowCount()];
        DatabaseWriterConnection.deleteRows(m_loginConfig, m_tableName, inTable,
            whereIncludes, deleteStatus, exec, getCredentialsProvider(), m_batchSize);
        // create out table with update column
        final ColumnRearranger colre = createColumnRearranger(inTable.getSpec(), deleteStatus);
        final BufferedDataTable outTable = exec.createColumnRearrangeTable(inTable,
          colre, exec.createSubProgress(1.0));
        return new BufferedDataTable[]{outTable};
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // save login settings
        if (m_loginConfig != null) {
            m_loginConfig.saveConnection(settings);
        }
        // save table name
        settings.addString(KEY_TABLE_NAME, m_tableName);
        // save WHERE columns
        if (m_configWHERE != null) {
            m_configWHERE.saveConfiguration(settings);
        }
        // save batch size
        settings.addInt(KEY_BATCH_SIZE, m_batchSize);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // load WHERE columns
        final DataColumnSpecFilterConfiguration confWHERE =
            new DataColumnSpecFilterConfiguration(KEY_WHERE_FILTER_COLUMN);
        confWHERE.loadConfigurationInModel(settings);
        m_configWHERE = confWHERE;
        // load login settings
        m_loginConfig = new DatabaseConnectionSettings(settings, getCredentialsProvider());
        // load table name
        m_tableName = settings.getString(KEY_TABLE_NAME).trim();
        // load batch size
        m_batchSize = settings.getInt(KEY_BATCH_SIZE);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // validate login settings
        new DatabaseConnectionSettings(settings, getCredentialsProvider());
        // validate table name
        final String tableName = settings.getString(KEY_TABLE_NAME);
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new InvalidSettingsException(
                 "Configure node and enter a valid table name.");
        }
        // validate WHERE columns
        final DataColumnSpecFilterConfiguration confWHERE =
            new DataColumnSpecFilterConfiguration(KEY_WHERE_FILTER_COLUMN);
        confWHERE.loadConfigurationInModel(settings);
        // validate batch size
        final int batchSize = settings.getInt(KEY_BATCH_SIZE);
        if (batchSize <= 0) {
            throw new InvalidSettingsException("Batch size must be greater than 0, is " + batchSize);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // op op
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no op
    }

}
