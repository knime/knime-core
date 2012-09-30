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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
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

    /** Index of input data port. */
    public static final int DATA_INPORT = 1;

    /** Index of model data port. */
    public static final int MODEL_INPORT = 0;

    /** Index of input data port. */
    public static final int DATA_OUTPORT = 0;

    /**
     * Config key, for the minimum fraction of information to be preserved by
     * the projection. (based on training data)
     */

    public static final String MIN_QUALPRESERVATION = "dimension_selection";

    /** number of dimensions to reduce to. */
    private final SettingsModelPCADimensions m_dimSelection =
            new SettingsModelPCADimensions(MIN_QUALPRESERVATION, 2, 100, false);

    /** remove original columns? */
    private final SettingsModelBoolean m_removeOriginalCols =
            new SettingsModelBoolean(REMOVE_COLUMNS, false);

    /** fail on missing data? */
    private final SettingsModelBoolean m_failOnMissingValues =
            new SettingsModelBoolean(PCANodeModel.FAIL_MISSING, false);

    private String[] m_inputColumnNames = {};



    private int[] m_inputColumnIndices;

    /**
     * Performs the PCA.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        final PCAModelPortObject model =
                (PCAModelPortObject)inData[MODEL_INPORT];
        final int dimensions =
                m_dimSelection.getNeededDimensions();
        if (dimensions == -1) {
            throw new IllegalArgumentException(
                    "Number of dimensions not correct configured");
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

        final Matrix eigenvectors =
                EigenValue.getSortedEigenVectors(model.getEigenVectors(), model
                        .getEigenvalues(), dimensions);
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
                texec.setProgress((double)curRowNr / rowCount,
                        "converting input row " + curRowNr + " of " + rowCount);

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
        final PCAModelPortObjectSpec modelPort =
                (PCAModelPortObjectSpec)inSpecs[MODEL_INPORT];
        m_inputColumnNames = modelPort.getColumnNames();
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

        m_dimSelection.setEigenValues(modelPort.getEigenValues());

        int dimensions =
                m_dimSelection.getNeededDimensions();
        if (dimensions <= 0) {
            return null;
        }
        if (dimensions > m_inputColumnIndices.length) {
            m_dimSelection.setDimensionsSelected(true);
            dimensions = m_inputColumnIndices.length;
            m_dimSelection.setDimensions(dimensions);
            setWarningMessage("dimensions resetted to " + dimensions);
        }

        final DataColumnSpec[] specs =
                PCANodeModel.createAddTableSpec(
                        (DataTableSpec)inSpecs[DATA_INPORT], dimensions);

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
        m_dimSelection.loadSettingsFrom(settings);
        m_removeOriginalCols.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);
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
        m_dimSelection.saveSettingsTo(settings);
        m_removeOriginalCols.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        m_dimSelection.validateSettings(settings);
        m_removeOriginalCols.validateSettings(settings);
        m_failOnMissingValues.validateSettings(settings);
    }
}
