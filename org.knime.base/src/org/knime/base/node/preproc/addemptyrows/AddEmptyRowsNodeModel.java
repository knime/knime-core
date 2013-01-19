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
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.addemptyrows;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
 * This is the model implementation of AddEmptyRows. Adds a certain number of
 * empty rows with missing values or a given constant.
 *
 * @author Bernd Wiswedel
 */
public class AddEmptyRowsNodeModel extends NodeModel {

    private AddEmptyRowsConfig m_config;

    /**
     * Constructor for the node model.
     */
    protected AddEmptyRowsNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable in = inData[0];
        int nrRowsToAdd;
        if (m_config.isAtLeastMode()) {
            if (in.getRowCount() < m_config.getRowCount()) {
                nrRowsToAdd = m_config.getRowCount() - in.getRowCount();
            } else {
                nrRowsToAdd = 0;
            }
        } else {
            nrRowsToAdd = m_config.getRowCount();
        }
        // do not copy data if there are enough rows
        if (nrRowsToAdd == 0) {
            return inData;
        }
        exec.setMessage("Creating append table");
        ExecutionContext createContext = exec.createSubExecutionContext(0.5);
        ExecutionContext checkContext = exec.createSubExecutionContext(0.5);
        BufferedDataTable appendTable =
                createNewRowsTable(in.getDataTableSpec(), nrRowsToAdd,
                        createContext);
        exec.setMessage("Checking output");
        BufferedDataTable outTable =
                exec.createConcatenateTable(checkContext, in, appendTable);
        return new BufferedDataTable[]{outTable};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_config == null) {
            throw new InvalidSettingsException("No configuration available");
        }
        return new DataTableSpec[]{inSpecs[0]};
    }

    private BufferedDataTable createNewRowsTable(final DataTableSpec inSpec,
            final int rowCount, final ExecutionContext subExec)
            throws CanceledExecutionException {
        DataCell[] cells = new DataCell[inSpec.getNumColumns()];
        for (int c = 0; c < cells.length; c++) {
            DataType type = inSpec.getColumnSpec(c).getType();
            if (type.isASuperTypeOf(DoubleCell.TYPE)) {
                if (m_config.isUseMissingDouble()) {
                    cells[c] = DataType.getMissingCell();
                } else {
                    cells[c] = new DoubleCell(m_config.getFillValueDouble());
                }
            } else if (type.isASuperTypeOf(IntCell.TYPE)) {
                if (m_config.isUseMissingInt()) {
                    cells[c] = DataType.getMissingCell();
                } else {
                    cells[c] = new IntCell(m_config.getFillValueInt());
                }
            } else if (type.isASuperTypeOf(StringCell.TYPE)) {
                if (m_config.isUseMissingString()) {
                    cells[c] = DataType.getMissingCell();
                } else {
                    cells[c] = new StringCell(m_config.getFillValueString());
                }
            } else {
                cells[c] = DataType.getMissingCell();
            }
        }
        BufferedDataContainer cont = subExec.createDataContainer(inSpec);
        for (int i = 0; i < rowCount; i++) {
            RowKey key = new RowKey(m_config.getNewRowKeyPrefix() + i);
            subExec.setProgress(i / (double)rowCount, "Creating row \"" + key
                    + "\", " + i + "/" + rowCount);
            subExec.checkCanceled();
            cont.addRowToTable(new DefaultRow(key, cells));
        }
        cont.close();
        return cont.getTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            m_config.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        AddEmptyRowsConfig config = new AddEmptyRowsConfig();
        config.loadSettingsFrom(settings);
        m_config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new AddEmptyRowsConfig().loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

}
