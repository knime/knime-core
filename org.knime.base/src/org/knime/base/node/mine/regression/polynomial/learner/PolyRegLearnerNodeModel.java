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
package org.knime.base.node.mine.regression.polynomial.learner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.regression.PMMLRegressionContentHandler;
import org.knime.base.node.mine.regression.PMMLRegressionPortObject;
import org.knime.base.node.mine.regression.PMMLRegressionPortObject.NumericPredictor;
import org.knime.base.node.mine.regression.PMMLRegressionPortObject.RegressionTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.util.math.MathUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 * This node performs polynomial regression on an input table with numeric-only
 * columns. The user can choose the maximum degree the built polynomial should
 * have.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegLearnerNodeModel extends NodeModel implements
        DataProvider {
    private final PolyRegLearnerSettings m_settings =
            new PolyRegLearnerSettings();

    private double[] m_betas;

    private double m_squaredError;

    private String[] m_columnNames;

    private DataArray m_rowContainer;

    private double[] m_meanValues;

    private boolean[] m_colSelected;

    /**
     * Creates a new model for the polynomial regression learner node.
     */
    public PolyRegLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
                BufferedDataTable.TYPE, PMMLRegressionPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec tableSpec = (DataTableSpec)inSpecs[0];

        for (String colName : m_settings.selectedColumns()) {
            DataColumnSpec dcs = tableSpec.getColumnSpec(colName);
            if (dcs == null) {
                throw new InvalidSettingsException("Selected column '"
                        + colName + "' does not exist in input table");
            }

            if (!dcs.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Selected column '"
                        + dcs.getName()
                        + "' from the input table is not a numeric column.");
            }
        }

        if (m_settings.getTargetColumn() == null) {
            throw new InvalidSettingsException("No target column selected");
        }
        if (tableSpec.findColumnIndex(m_settings.getTargetColumn()) == -1) {
            throw new InvalidSettingsException("Target column '"
                    + m_settings.getTargetColumn() + "' does not exist.");
        }

        DataColumnSpecCreator crea =
                new DataColumnSpecCreator("PolyReg prediction", DoubleCell.TYPE);
        DataColumnSpec col1 = crea.createSpec();

        crea = new DataColumnSpecCreator("Prediction Error", DoubleCell.TYPE);
        DataColumnSpec col2 = crea.createSpec();

        return new PortObjectSpec[]{
                AppendedColumnTable.getTableSpec(tableSpec, col1, col2),
                createModelSpec(tableSpec)};
    }

    private PMMLPortObjectSpec createModelSpec(final DataTableSpec inSpec) 
        throws InvalidSettingsException {
        DataColumnSpec[] usedColumns =
                new DataColumnSpec[m_settings.selectedColumns().size() + 1];
        int k = 0;
        for (DataColumnSpec dcs : inSpec) {
            if (m_settings.selectedColumns().contains(dcs.getName())) {
                usedColumns[k++] = dcs;
            }
        }

        usedColumns[k++] = inSpec.getColumnSpec(m_settings.getTargetColumn());

        PMMLPortObjectSpecCreator crea =
                new PMMLPortObjectSpecCreator(new DataTableSpec(usedColumns));
        crea.setTargetCol(usedColumns[k - 1]);
        return crea.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inData[0];

        final int colCount = inTable.getDataTableSpec().getNumColumns();
        m_colSelected = new boolean[colCount];
        for (int i = 0; i < colCount; i++) {
            m_colSelected[i] =
                    m_settings.selectedColumns().contains(
                            inTable.getDataTableSpec().getColumnSpec(i)
                                    .getName());
        }

        final int rowCount = inTable.getRowCount();
        final int independentVariables = m_settings.selectedColumns().size();
        final int degree = m_settings.getDegree();
        final int dependentIndex =
                inTable.getDataTableSpec().findColumnIndex(
                        m_settings.getTargetColumn());

        double[][] xMat =
                new double[rowCount][1 + independentVariables * degree];
        double[][] yMat = new double[rowCount][1];

        int rowIndex = 0;
        for (DataRow row : inTable) {
            exec.checkCanceled();
            exec.setProgress(0.2 * rowIndex / rowCount);
            xMat[rowIndex][0] = 1;
            int colIndex = 1;
            for (int i = 0; i < row.getNumCells(); i++) {
                if ((m_colSelected[i] || (i == dependentIndex))
                        && row.getCell(i).isMissing()) {
                    throw new Exception(
                            "Missing values are not supported by this node.");
                }

                if (m_colSelected[i]) {
                    double val = ((DoubleValue)row.getCell(i)).getDoubleValue();
                    double poly = val;
                    xMat[rowIndex][colIndex] = poly;
                    colIndex++;

                    for (int d = 2; d <= degree; d++) {
                        poly *= val;
                        xMat[rowIndex][colIndex] = poly;
                        colIndex++;
                    }
                } else if (i == dependentIndex) {
                    double val = ((DoubleValue)row.getCell(i)).getDoubleValue();
                    yMat[rowIndex][0] = val;
                }
            }
            rowIndex++;
        }

        // compute X'
        double[][] xTransMat = MathUtils.transpose(xMat);
        exec.setProgress(0.24);
        exec.checkCanceled();
        // compute X'X
        double[][] xxMat = MathUtils.multiply(xTransMat, xMat);
        exec.setProgress(0.28);
        exec.checkCanceled();
        // compute X'Y
        double[][] xyMat = MathUtils.multiply(xTransMat, yMat);
        exec.setProgress(0.32);
        exec.checkCanceled();

        // compute (X'X)^-1
        double[][] xxInverse;
        try {
            xxInverse = MathUtils.inverse(xxMat);
            exec.setProgress(0.36);
            exec.checkCanceled();
        } catch (ArithmeticException ex) {
            throw new ArithmeticException("The attributes of the data samples "
                    + " are not mutually independent.");
        }

        // compute (X'X)^-1 * (X'Y)
        final double[][] betas = MathUtils.multiply(xxInverse, xyMat);
        exec.setProgress(0.4);

        // ColumnRearranger crea =
        // new ColumnRearranger(inData[1].getDataTableSpec());
        // crea.append(createCellFactory(betas, inData));

        m_betas = new double[independentVariables * degree + 1];
        for (int i = 0; i < betas.length; i++) {
            m_betas[i] = betas[i][0];
        }

        m_columnNames = m_settings.selectedColumns().toArray(new String[0]);
        String[] temp = new String[m_columnNames.length + 1];
        System.arraycopy(m_columnNames, 0, temp, 0, m_columnNames.length);
        temp[temp.length - 1] = m_settings.getTargetColumn();
        FilterColumnTable filteredTable = new FilterColumnTable(inTable, temp);

        m_rowContainer =
                new DefaultDataArray(filteredTable, 1, m_settings
                        .getMaxRowsForView());
        int ignore =
                m_rowContainer.getDataTableSpec().findColumnIndex(
                        m_settings.getTargetColumn());

        m_meanValues = new double[independentVariables];
        for (DataRow row : m_rowContainer) {
            int k = 0;
            for (int i = 0; i < row.getNumCells(); i++) {
                if (i != ignore) {
                    m_meanValues[k++] +=
                            ((DoubleValue)row.getCell(i)).getDoubleValue();
                }
            }
        }
        for (int i = 0; i < m_meanValues.length; i++) {
            m_meanValues[i] /= m_rowContainer.size();
        }

        ColumnRearranger crea =
                new ColumnRearranger(inTable.getDataTableSpec());
        crea.append(getCellFactory(inTable.getDataTableSpec().findColumnIndex(
                m_settings.getTargetColumn())));

        PortObject[] bdt =
                new PortObject[]{
                        exec.createColumnRearrangeTable(inTable, crea, exec
                                .createSubProgress(0.6)),
                        createPMMLModel(inTable.getDataTableSpec())};
        m_squaredError /= rowCount;
        return bdt;
    }

    private PMMLRegressionPortObject createPMMLModel(final DataTableSpec inSpec)
        throws InvalidSettingsException {
        NumericPredictor[] preds = new NumericPredictor[m_betas.length - 1];

        int deg = m_settings.getDegree();
        for (int i = 0; i < m_columnNames.length; i++) {
            for (int k = 0; k < deg; k++) {
                preds[i * deg + k] =
                        new NumericPredictor(m_columnNames[i], k + 1, m_betas[i
                                * deg + k + 1]);
            }
        }

        RegressionTable tab =
                new RegressionTable(m_betas[0], preds);

        PMMLPortObjectSpec spec = createModelSpec(inSpec);
        PMMLRegressionContentHandler ch =
                new PMMLRegressionContentHandler(spec);
        ch.setRegressionTable(tab);
        ch.setAlgorithmName("PolynomialRegression");
        ch.setModelName("KNIME Polynomial Regression");

        return new PMMLRegressionPortObject(spec, ch);
    }

    private CellFactory getCellFactory(final int dependentIndex) {
        final int degree = m_settings.getDegree();

        return new CellFactory() {
            public DataCell[] getCells(final DataRow row) {
                double sum = m_betas[0];
                int betaCount = 1;
                double y = 0;
                for (int col = 0; col < row.getNumCells(); col++) {
                    if ((col != dependentIndex) && m_colSelected[col]) {
                        final double value =
                                ((DoubleValue)row.getCell(col))
                                        .getDoubleValue();
                        double poly = 1;
                        for (int d = 1; d <= degree; d++) {
                            poly *= value;
                            sum += m_betas[betaCount++] * poly;
                        }
                    } else if (col == dependentIndex) {
                        y = ((DoubleValue)row.getCell(col)).getDoubleValue();
                    }
                }

                double err = Math.abs(sum - y);
                m_squaredError += err * err;

                return new DataCell[]{new DoubleCell(sum), new DoubleCell(err)};
            }

            public DataColumnSpec[] getColumnSpecs() {
                DataColumnSpecCreator crea =
                        new DataColumnSpecCreator("PolyReg prediction",
                                DoubleCell.TYPE);
                DataColumnSpec col1 = crea.createSpec();

                crea =
                        new DataColumnSpecCreator("Prediction Error",
                                DoubleCell.TYPE);
                DataColumnSpec col2 = crea.createSpec();
                return new DataColumnSpec[]{col1, col2};
            }

            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor execMon) {
                // do nothing
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "internals.xml");
        if (f.exists()) {
            NodeSettingsRO internals =
                    NodeSettings.loadFromXML(new BufferedInputStream(
                            new FileInputStream(f)));
            try {
                m_betas = internals.getDoubleArray("betas");
                m_columnNames = internals.getStringArray("columnNames");
                m_squaredError = internals.getDouble("squaredError");
                m_meanValues = internals.getDoubleArray("meanValues");
            } catch (InvalidSettingsException ex) {
                throw new IOException("Old or corrupt internals");
            }
        } else {
            throw new FileNotFoundException("Internals do not exist");
        }

        f = new File(nodeInternDir, "data.zip");
        if (f.exists()) {
            ContainerTable t = DataContainer.readFromZip(f);
            int rowCount = t.getRowCount();
            m_rowContainer = new DefaultDataArray(t, 1, rowCount, exec);
        } else {
            throw new FileNotFoundException("Internals do not exist");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_betas = null;
        m_columnNames = null;
        m_squaredError = 0;
        m_rowContainer = null;
        m_meanValues = null;
        m_colSelected = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_betas != null) {
            NodeSettings internals = new NodeSettings("internals");
            internals.addDoubleArray("betas", m_betas);
            internals.addStringArray("columnNames", m_columnNames);
            internals.addDouble("squaredError", m_squaredError);
            internals.addDoubleArray("meanValues", m_meanValues);

            internals.saveToXML(new BufferedOutputStream(new FileOutputStream(
                    new File(nodeInternDir, "internals.xml"))));

            File dataFile = new File(nodeInternDir, "data.zip");
            DataContainer.writeToZip(m_rowContainer, dataFile, exec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        PolyRegLearnerSettings s = new PolyRegLearnerSettings();
        s.loadSettingsFrom(settings);

        if (s.getTargetColumn() == null) {
            throw new InvalidSettingsException("No target column selected");
        }
    }

    /**
     * Returns the learned beta values.
     *
     * @return the beta values
     */
    double[] getBetas() {
        return m_betas;
    }

    /**
     * Returns the column names.
     *
     * @return the column names
     */
    String[] getColumnNames() {
        return m_columnNames;
    }

    /**
     * Returns the total squared error.
     *
     * @return the squared error
     */
    double getSquaredError() {
        return m_squaredError;
    }

    /**
     * Returns the degree of the regression function.
     *
     * @return the degree
     */
    int getDegree() {
        return m_settings.getDegree();
    }

    /**
     * Returns the target column's name.
     *
     * @return the target column's name
     */
    String getTargetColumn() {
        return m_settings.getTargetColumn();
    }

    /**
     * Returns the mean value of each input column.
     *
     * @return the mean values
     */
    double[] getMeanValues() {
        return m_meanValues;
    }

    /**
     * {@inheritDoc}
     */
    public DataArray getDataArray(final int index) {
        return m_rowContainer;
    }
}
