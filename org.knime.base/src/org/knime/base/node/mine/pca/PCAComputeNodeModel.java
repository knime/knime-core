/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   04.10.2006 (uwe): created
 */
/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
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

    /** all settings models of this node for iteration. */
    private final SettingsModel[] m_settingsModels =
            {m_inputColumns, m_failOnMissingValues};

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
                        m_failOnMissingValues.getBooleanValue());
        final double[][] m =
                new double[m_inputColumnIndices.length][m_inputColumnIndices.length];

        final int missingValues =
                PCANodeModel.getCovarianceMatrix(exec
                        .createSubExecutionContext(0.5), dataTable,
                        m_inputColumnIndices, meanVector, m);
        if (missingValues > 0) {
            if (m_failOnMissingValues.getBooleanValue()) {
                throw new IllegalArgumentException(
                        "missing, infinite or impossible values in table");
            }
            setWarningMessage(missingValues
                    + " rows ignored because of missing, infinite or impossible values");
        }

        final Matrix covarianceMatrix = new Matrix(m);
        exec.setProgress(0.6, "calculation of spectral decomposition");
        final EigenvalueDecomposition evd = covarianceMatrix.eig();
        final Matrix d = evd.getD();
        final double[] evs = new double[d.getRowDimension()];
        for (int i = 0; i < evs.length; i++) {
            evs[i] = d.get(i, i);
        }

        return new PortObject[]{
                PCANodeModel.createCovarianceTable(exec, m, m_inputColumnNames),
                PCANodeModel.createDecompositionOutputTable(exec, evs,
                        EigenValue.getSortedEigenVectors(evd.getV().getArray(),
                                evs, evs.length)),
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
        for (final SettingsModel s : this.m_settingsModels) {
            s.loadSettingsFrom(settings);
        }

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
                        .createDecompositionTableSpec(m_inputColumnIndices.length),
                new PCAModelPortObjectSpec(m_inputColumnNames)};
    }

    private void selectDefaultColumns(final PortObjectSpec[] inSpecs) {
        m_inputColumnIndices =
                PCANodeModel
                        .getDefaultColumns((DataTableSpec)inSpecs[DATA_INPORT]);
        m_inputColumnNames = new String[m_inputColumnIndices.length];
        for (int i = 0; i < m_inputColumnIndices.length; i++) {
            m_inputColumnNames[i] =
                    ((DataTableSpec)inSpecs[DATA_INPORT]).getColumnSpec(i)
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
        for (final SettingsModel s : this.m_settingsModels) {
            s.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (final SettingsModel s : this.m_settingsModels) {
            s.validateSettings(settings);
        }
    }

}
