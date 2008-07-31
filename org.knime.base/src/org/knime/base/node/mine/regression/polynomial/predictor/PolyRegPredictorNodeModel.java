/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.mine.regression.polynomial.predictor;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This node predicts numerical values using the coefficients learned by
 * a {@link org.knime.base.node.mine.regression.polynomial.learner.PolyRegLearnerNodeModel}.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegPredictorNodeModel extends NodeModel {
    private final DataColumnSpecCreator m_colSpecCreator =
            new DataColumnSpecCreator("PolyReg prediction", DoubleCell.TYPE);

    private double[] m_betas;

    private int m_degree;

    private String[] m_columnNames;

    /**
     * Creates a new polynomial regression predictor node.
     */
    public PolyRegPredictorNodeModel() {
        super(1, 1, 1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_columnNames == null || m_betas == null) {
            throw new InvalidSettingsException("Model not loaded yet");
        }
        for (String colname : m_columnNames) {
            int colIndex = inSpecs[0].findColumnIndex(colname);
            if (colIndex == -1) {
                throw new InvalidSettingsException(
                        "Table does not contain a column with name '" + colname
                                + "'.");
            }

            if (!inSpecs[0].getColumnSpec(colIndex).getType().isCompatible(
                    DoubleValue.class)) {
                throw new InvalidSettingsException("The column '" + colname
                        + "' is not numeric.");
            }
        }

        return new DataTableSpec[]{AppendedColumnTable.getTableSpec(inSpecs[0],
                m_colSpecCreator.createSpec())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        if (predParams != null) {
            m_degree = predParams.getInt("degree");
            m_columnNames = predParams.getStringArray("columnNames");
            m_betas = predParams.getDoubleArray("betas");
        } else {
            m_columnNames = null;
            m_betas = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final int[] attributeMap = new int[m_columnNames.length];
        int k = 0;
        for (String colname : m_columnNames) {
            attributeMap[k++] =
                    inData[0].getDataTableSpec().findColumnIndex(colname);
        }

        ColumnRearranger c = new ColumnRearranger(inData[0].getDataTableSpec());
        c.append(new CellFactory() {
            public DataCell[] getCells(final DataRow row) {
                double sum = m_betas[0];
                int betaCount = 1;
                for (int j = 0; j < attributeMap.length; j++) {
                    if (row.getCell(attributeMap[j]).isMissing()) {
                        return new DataCell[]{DataType.getMissingCell()};
                    }
                    final double value =
                            ((DoubleValue)row.getCell(attributeMap[j]))
                                    .getDoubleValue();
                    double poly = 1;
                    for (int d = 1; d <= m_degree; d++) {
                        poly *= value;
                        sum += m_betas[betaCount++] * poly;
                    }
                }

                return new DataCell[]{new DoubleCell(sum)};
            }

            public DataColumnSpec[] getColumnSpecs() {
                return new DataColumnSpec[]{m_colSpecCreator.createSpec()};
            }

            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor execMon) {
                execMon.setProgress(curRowNr / (double)rowCount);
            }
        });

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], c, exec)};
    }
}
