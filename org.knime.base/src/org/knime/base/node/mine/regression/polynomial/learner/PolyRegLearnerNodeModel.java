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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.regression.PMMLRegressionTranslator;
import org.knime.base.node.mine.regression.PMMLRegressionTranslator.NumericPredictor;
import org.knime.base.node.mine.regression.PMMLRegressionTranslator.RegressionTable;
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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.xml.sax.SAXException;

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
        super(new PortType[] {BufferedDataTable.TYPE,
                new PortType(PMMLPortObject.class, true)},
                    new PortType[] {BufferedDataTable.TYPE,
                            PMMLPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec tableSpec = (DataTableSpec)inSpecs[0];
        PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)inSpecs[1];
        String[] selectedCols = computeSelectedColumns(tableSpec);
        for (String colName : selectedCols) {
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

        DataColumnSpecCreator crea = new DataColumnSpecCreator(
                "PolyReg prediction", DoubleCell.TYPE);
        DataColumnSpec col1 = crea.createSpec();

        crea = new DataColumnSpecCreator("Prediction Error", DoubleCell.TYPE);
        DataColumnSpec col2 = crea.createSpec();

        return new PortObjectSpec[]{
                AppendedColumnTable.getTableSpec(tableSpec, col1, col2),
                createModelSpec(pmmlSpec, tableSpec)};
    }

    private PMMLPortObjectSpec createModelSpec(
            final PMMLPortObjectSpec inModelSpec,
            final DataTableSpec inDataSpec)
        throws InvalidSettingsException {
        String[] selectedCols = computeSelectedColumns(inDataSpec);
        DataColumnSpec[] usedColumns =
            new DataColumnSpec[selectedCols.length + 1];
        int k = 0;
        Set<String> hash = new HashSet<String>(Arrays.asList(selectedCols));
        for (DataColumnSpec dcs : inDataSpec) {
            if (hash.contains(dcs.getName())) {
                usedColumns[k++] = dcs;
            }
        }

        usedColumns[k++] = inDataSpec.getColumnSpec(
                m_settings.getTargetColumn());

        DataTableSpec tableSpec = new DataTableSpec(usedColumns);
        PMMLPortObjectSpecCreator crea =
                new PMMLPortObjectSpecCreator(inModelSpec, inDataSpec);
        crea.setLearningCols(tableSpec);
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
        DataTableSpec inSpec = inTable.getDataTableSpec();

        final int colCount = inSpec.getNumColumns();
        String[] selectedCols = computeSelectedColumns(inSpec);
        Set<String> hash = new HashSet<String>(Arrays.asList(selectedCols));
        m_colSelected = new boolean[colCount];
        for (int i = 0; i < colCount; i++) {
            m_colSelected[i] = hash.contains(
                    inTable.getDataTableSpec().getColumnSpec(i).getName());
        }

        final int rowCount = inTable.getRowCount();
        final int independentVariables = selectedCols.length;
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
            throw new ArithmeticException("The attributes of the data samples"
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

        m_columnNames = selectedCols;
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

        // handle the optional PMML input
        PMMLPortObject inPMMLPort = (PMMLPortObject)inData[1];

        PortObject[] bdt =
                new PortObject[]{
                        exec.createColumnRearrangeTable(inTable, crea, exec
                                .createSubProgress(0.6)),
                        createPMMLModel(inPMMLPort,
                                inTable.getDataTableSpec())};
        m_squaredError /= rowCount;
        return bdt;
    }

    private PMMLPortObject createPMMLModel(final PMMLPortObject inPMMLPort,
            final DataTableSpec inSpec)
        throws InvalidSettingsException, SAXException {
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
        PMMLPortObjectSpec pmmlSpec = null;
        if (inPMMLPort != null) {
            pmmlSpec = inPMMLPort.getSpec();
        }
        PMMLPortObjectSpec spec = createModelSpec(pmmlSpec, inSpec);


        /* To maintain compatibility with the previous SAX-based implementation.
         * */
        String targetField = "Response";
        List<String> targetFields = spec.getTargetFields();
        if (!targetFields.isEmpty()) {
            targetField = targetFields.get(0);
        }

        PMMLPortObject outPMMLPort = new PMMLPortObject(spec,
                inPMMLPort, inSpec);

        PMMLRegressionTranslator trans = new PMMLRegressionTranslator(
                "KNIME Polynomial Regression", "PolynomialRegression",
                tab, targetField);
        outPMMLPort.addModelTranslater(trans);

        return outPMMLPort;
    }

    private CellFactory getCellFactory(final int dependentIndex) {
        final int degree = m_settings.getDegree();

        return new CellFactory() {
            @Override
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

            @Override
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

            @Override
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

    /** Depending on whether the includeAll flag is set, it determines the list
     * of learning (independent) columns. If the flag is not set, it returns
     * the list stored in m_settings.
     * @param spec to get column names from.
     * @return The list of learning columns.
     * @throws InvalidSettingsException If no valid columns are in the spec.
     */
    private String[] computeSelectedColumns(final DataTableSpec spec)
    throws InvalidSettingsException {
        String[] includes;
        String target = m_settings.getTargetColumn();
        if (m_settings.isIncludeAll()) {
            List<String> includeList = new ArrayList<String>();
            for (DataColumnSpec s : spec) {
                if (s.getType().isCompatible(DoubleValue.class)) {
                    String name = s.getName();
                    if (!name.equals(target)) {
                        includeList.add(name);
                    }
                }
            }
            includes = includeList.toArray(new String[includeList.size()]);
            if (includes.length == 0) {
                throw new InvalidSettingsException("No double-compatible "
                        + "variables (learning columns) in input table");
            }
        } else {
            Set<String> selSettings = m_settings.getSelectedColumns();
            if (selSettings == null || selSettings.isEmpty()) {
                throw new InvalidSettingsException("No settings available");
            }
            includes = selSettings.toArray(new String[selSettings.size()]);
        }
        return includes;
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
    @Override
    public DataArray getDataArray(final int index) {
        return m_rowContainer;
    }
}
