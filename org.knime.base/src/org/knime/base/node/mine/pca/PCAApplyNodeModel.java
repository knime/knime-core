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
 */
package org.knime.base.node.mine.pca;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import Jama.Matrix;

/**
 * PCA Predictor.
 * 
 * @author uwe, University of Konstanz
 */
public class PCAApplyNodeModel extends NodeModel {
    /** config string for determining if source columns are to be removed. */
    static final String REMOVE_COLUMNS = "removeColumns";

    /**
     * create node.
     */
    protected PCAApplyNodeModel() {
        super(new PortType[]{PCAModelPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});

    }

    /**
     * Config key, for the number of dimensions the original data is reduced to.
     */
    protected static final String RESULT_DIMENSIONS = "result_dimensions";

    /** Index of input data port. */
    public static final int DATA_INPORT = 1;

    /** Index of model data port. */
    public static final int MODEL_INPORT = 0;

    /** Index of input data port. */
    public static final int DATA_OUTPORT = 0;

    /** number of dimensions to reduce to. */
    private final SettingsModelString m_reduceToDimensions =
            new SettingsModelString(PCANodeModel.RESULT_DIMENSIONS, "");

    /** remove original columns? */
    private final SettingsModelBoolean m_removeOriginalCols =
            new SettingsModelBoolean(REMOVE_COLUMNS, false);

    /** fail on missing data? */
    private final SettingsModelBoolean m_failOnMissingValues =
            new SettingsModelBoolean(PCANodeModel.FAIL_MISSING, false);

    private String[] m_inputColumnNames = {};

    private final SettingsModel[] m_settingsModels =
            {m_reduceToDimensions, m_removeOriginalCols, m_failOnMissingValues};

    private int[] m_inputColumnIndices;

    /**
     * Performs the PCA.
     * 
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        // remove all non-numeric columns from the input date
        // final DataTable filteredTable =
        // filterNonNumericalColumns(inData[DATA_INPORT]);
        final String dimValue = m_reduceToDimensions.getStringValue();

        int dimensions =
                dimValue.length() > 0 ? Integer
                        .parseInt(dimValue.split(" ")[0]) : 2;
        // adjust to selected numerical columns
        if (dimensions > m_inputColumnNames.length || dimensions < 1) {
            // throw new IllegalArgumentException(
            // "invalid number of dimensions to reduce to: " + dimensions);

            dimensions = m_inputColumnIndices.length;
            setWarningMessage("result dimensions resetted to " + dimensions);

        }
        if (m_failOnMissingValues.getBooleanValue()) {
            for (final DataRow row : (DataTable)inData[DATA_INPORT]) {
                for (int i = 0; i < m_inputColumnIndices.length; i++) {
                    if (row.getCell(m_inputColumnIndices[i]).isMissing()) {
                        throw new IllegalArgumentException(
                                "data table contains missing values");
                    }
                }
            }

        }
        final PCAModelPortObject model =
                (PCAModelPortObject)inData[MODEL_INPORT];
        final Matrix eigenvectors =
                EigenValue.getSortedEigenVectors(model.getEigenVectors(), model
                        .getEigenvalues(), dimensions);
        eigenvectors.transpose();
        final DataColumnSpec[] specs =
                PCANodeModel.createAddTableSpec(
                        (DataTableSpec)inData[DATA_INPORT].getSpec(),
                        dimensions);
        final int dim = dimensions;
        final CellFactory fac = new CellFactory() {

            @Override
            public DataCell[] getCells(final DataRow row) {
                return PCANodeModel.convertInputRow(eigenvectors, row, model
                        .getCenter(), m_inputColumnIndices, dim,
                        m_failOnMissingValues.getBooleanValue());
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {

                return specs;
            }

            @Override
            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor texec) {
                texec.setProgress((double)curRowNr / rowCount);

            }

        };

        final ColumnRearranger cr =
                new ColumnRearranger((DataTableSpec)inData[DATA_INPORT]
                        .getSpec());
        cr.append(fac);
        if (m_removeOriginalCols.getBooleanValue()) {
            cr.remove(m_inputColumnNames);
        }
        final BufferedDataTable result =
                exec.createColumnRearrangeTable(
                        (BufferedDataTable)inData[DATA_INPORT], cr, exec);
        final PortObject[] out = {result};
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        m_inputColumnNames =
                ((PCAModelPortObjectSpec)inSpecs[MODEL_INPORT])
                        .getColumnNames();
        if (m_inputColumnNames.length == 0) {
            throw new InvalidSettingsException("no columns for pca chosen");
        }
        m_inputColumnIndices = new int[m_inputColumnNames.length];
        int index = 0;
        for (final String colName : m_inputColumnNames) {
            final DataColumnSpec colspec =
                    ((DataTableSpec)inSpecs[DATA_INPORT])
                            .getColumnSpec(colName);
            if (colspec == null) {
                throw new InvalidSettingsException(
                        "unable to find input column " + colName);
            }
            if (!colspec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("column \"" + colName
                        + "\" is not compatible with double");
            }
            m_inputColumnIndices[index++] =
                    ((DataTableSpec)inSpecs[DATA_INPORT])
                            .findColumnIndex(colName);
        }

        final String dimValue = m_reduceToDimensions.getStringValue();

        int resultDimensions =
                dimValue.equals("") ? 2 : Integer
                        .parseInt(dimValue.split(" ")[0]);
        if (resultDimensions > m_inputColumnIndices.length) {
            resultDimensions = m_inputColumnIndices.length;
            setWarningMessage("result dimensions resetted to "
                    + resultDimensions);
        }
        final DataColumnSpec[] specs =
                PCANodeModel.createAddTableSpec(
                        (DataTableSpec)inSpecs[DATA_INPORT], resultDimensions);

        final DataTableSpec dts =
                AppendedColumnTable.getTableSpec(
                        (DataTableSpec)inSpecs[DATA_INPORT], specs);
        if (m_removeOriginalCols.getBooleanValue()) {
            final ColumnRearranger columnRearranger = new ColumnRearranger(dts);
            columnRearranger.remove(m_inputColumnIndices);
            return new DataTableSpec[]{columnRearranger.createSpec()};
        }

        return new DataTableSpec[]{dts};

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
    protected void reset() {
        m_inputColumnNames = new String[]{};
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
