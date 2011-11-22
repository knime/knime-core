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
 * -------------------------------------------------------------------
 * 
 * History
 *   04.10.2006 (uwe): created
 */
package org.knime.base.node.mine.pca;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Node for PCA learning.
 * 
 * @author uwe, University of Konstanz
 */
public class PCAComputeNodeModel extends NodeModel {

    /** Index of input data port. */
    static final int DATA_INPORT = 0;

    /** indices of input columns. */
    private int[] m_inputColumnIndices = {};

    /** numeric columns to be used as input. */
    private final SettingsModelFilterString m_inputColumns =
        new SettingsModelFilterString(PCANodeModel.INPUT_COLUMNS);

    /** <code>true</code> if we fail on missing data. */
    private final SettingsModelBoolean m_failOnMissingValues =
        new SettingsModelBoolean(PCANodeModel.FAIL_MISSING, false);



    private String[] m_inputColumnNames;

    /**
     * create node model.
     */
    public PCAComputeNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
                BufferedDataTable.TYPE, BufferedDataTable.TYPE,
                PCAModelPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        if (!(inData[DATA_INPORT] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Datatable as input expected");
        }
        final BufferedDataTable dataTable =
            (BufferedDataTable)inData[DATA_INPORT];
        if (dataTable.getRowCount() == 0) {
            throw new IllegalArgumentException("Input table is empty!");
        }

        final double[] meanVector =
            PCANodeModel.getMeanVector(dataTable, m_inputColumnIndices,
                    m_failOnMissingValues.getBooleanValue(), exec
                    .createSubExecutionContext(0.4));
        final double[][] m =
            new double[m_inputColumnIndices.length][m_inputColumnIndices.length];
        exec.checkCanceled();
        final int missingValues =
            PCANodeModel.getCovarianceMatrix(exec
                    .createSubExecutionContext(0.4), dataTable,
                    m_inputColumnIndices, meanVector, m);
        if (missingValues > 0) {
            if (m_failOnMissingValues.getBooleanValue()) {
                throw new IllegalArgumentException(
                        "missing, infinite or impossible values in table");
            }
            setWarningMessage(missingValues
                    + " rows ignored because of missing, "
                    + "infinite or impossible values");
        }
        exec.checkCanceled();
        final Matrix covarianceMatrix = new Matrix(m);
        exec.setProgress("calculation of spectral decomposition");
        final EigenvalueDecomposition evd = covarianceMatrix.eig();
        exec.checkCanceled();
        exec.setProgress(0.9);
        final Matrix d = evd.getD();
        final double[] evs = new double[d.getRowDimension()];
        for (int i = 0; i < evs.length; i++) {
            evs[i] = d.get(i, i);
        }
        exec.checkCanceled();

        return new PortObject[]{
                PCANodeModel.createCovarianceTable(exec, m, m_inputColumnNames),
                PCANodeModel.createDecompositionOutputTable(exec
                        .createSubExecutionContext(0.1), evd, m_inputColumnNames),
                        new PCAModelPortObject(evd.getV().getArray(), evs,
                                m_inputColumnNames, meanVector)};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_inputColumns.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
    throws InvalidSettingsException {
        if (m_inputColumns.getIncludeList().size() == 0) {
            selectDefaultColumns(inSpecs);
        } else {

            m_inputColumnIndices =
                new int[m_inputColumns.getIncludeList().size()];
            m_inputColumnNames =
                new String[m_inputColumns.getIncludeList().size()];

            int colIndex = 0;
            for (final String colName : m_inputColumns.getIncludeList()) {
                final DataColumnSpec colspec =
                    ((DataTableSpec)inSpecs[DATA_INPORT])
                    .getColumnSpec(colName);
                if (colspec == null
                        || !colspec.getType().isCompatible(DoubleValue.class)) {
                    setWarningMessage("column \"" + colName
                            + "\" not found, selected default columns");
                    selectDefaultColumns(inSpecs);
                    break;
                }
                m_inputColumnIndices[colIndex] =
                    ((DataTableSpec)inSpecs[DATA_INPORT])
                    .findColumnIndex(colName);
                m_inputColumnNames[colIndex] = colName;
                colIndex++;
            }
        }

        return new PortObjectSpec[]{
                PCANodeModel.createCovarianceMatrixSpec(m_inputColumnNames),
                PCANodeModel
                .createDecompositionTableSpec(m_inputColumnNames),
                new PCAModelPortObjectSpec(m_inputColumnNames)};
    }

    private void selectDefaultColumns(final PortObjectSpec[] inSpecs) {
        m_inputColumnIndices =
            PCANodeModel
            .getDefaultColumns((DataTableSpec)inSpecs[DATA_INPORT]);
        m_inputColumnNames = new String[m_inputColumnIndices.length];
        for (int i = 0; i < m_inputColumnIndices.length; i++) {
            m_inputColumnNames[i] =
                ((DataTableSpec)inSpecs[DATA_INPORT]).getColumnSpec(m_inputColumnIndices[i])
                .getName();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_inputColumnIndices = new int[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inputColumns.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_inputColumns.validateSettings(settings);
        m_failOnMissingValues.validateSettings(settings);
    }

}
