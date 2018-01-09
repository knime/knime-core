/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.regression.polynomial.learner2;

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
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.regression.ModelSpecificationException;
import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.regression.MissingValueHandling;
import org.knime.base.node.mine.regression.PMMLRegressionTranslator;
import org.knime.base.node.mine.regression.PMMLRegressionTranslator.NumericPredictor;
import org.knime.base.node.mine.regression.PMMLRegressionTranslator.RegressionTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
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
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.util.Pair;
import org.xml.sax.SAXException;

/**
 * This node performs polynomial regression on an input table with numeric-only columns. The user can choose the maximum
 * degree the built polynomial should have.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.10
 */
public class PolyRegLearnerNodeModel extends NodeModel implements DataProvider {
    private final PolyRegLearnerSettings m_settings = new PolyRegLearnerSettings();

    private double[] m_betas;

    private double m_squaredError;

    private String[] m_columnNames;

    private double[] m_meanValues;

    private boolean[] m_colSelected;

    private boolean m_pmmlInEnabled;

    private PolyRegViewData m_viewData = new PolyRegViewData(new double[0], new double[0], new double[0], new double[0], new double[0], Double.NaN, Double.NaN, new String[0],
        0, "N/A", null);

    /** Statistics (third) output table specification. */
    private static final DataTableSpec STATS_SPEC;
    static {
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(
            new DataColumnSpecCreator("Variable", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Exponent", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Coeff.", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Std. Err.", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("t-value", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("P>|t|", DoubleCell.TYPE).createSpec()
            );
        STATS_SPEC = creator.createSpec();
    }

    /**
     * Creates a new model for the polynomial regression learner node with optional PMML input.
     */
    public PolyRegLearnerNodeModel() {
        this(true);
    }

    /**
     * Creates a new model for the polynomial regression learner node.
     * @param pmmlInEnabled if true, the node has an optional PMML input
     */
    public PolyRegLearnerNodeModel(final boolean pmmlInEnabled) {
        super(pmmlInEnabled ? new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE_OPTIONAL} : new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{PMMLPortObject.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE});
        m_pmmlInEnabled = pmmlInEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec tableSpec = (DataTableSpec)inSpecs[0];
        PMMLPortObjectSpec pmmlSpec = m_pmmlInEnabled ? (PMMLPortObjectSpec)inSpecs[1] : null;
        String[] selectedCols = computeSelectedColumns(tableSpec);
        m_columnNames = selectedCols;
        for (String colName : selectedCols) {
            DataColumnSpec dcs = tableSpec.getColumnSpec(colName);
            if (dcs == null) {
                throw new InvalidSettingsException("Selected column '" + colName + "' does not exist in input table");
            }

            if (!dcs.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Selected column '" + dcs.getName()
                    + "' from the input table is not a numeric column.");
            }
        }

        if (m_settings.getTargetColumn() == null) {
            throw new InvalidSettingsException("No target column selected");
        }
        if (tableSpec.findColumnIndex(m_settings.getTargetColumn()) == -1) {
            throw new InvalidSettingsException("Target column '" + m_settings.getTargetColumn() + "' does not exist.");
        }

        DataColumnSpecCreator crea = new DataColumnSpecCreator("PolyReg prediction", DoubleCell.TYPE);
        DataColumnSpec col1 = crea.createSpec();

        crea = new DataColumnSpecCreator("Prediction Error", DoubleCell.TYPE);
        DataColumnSpec col2 = crea.createSpec();

        return new PortObjectSpec[]{createModelSpec(pmmlSpec, tableSpec),
            AppendedColumnTable.getTableSpec(tableSpec, col1, col2), STATS_SPEC};
    }

    private PMMLPortObjectSpec createModelSpec(final PMMLPortObjectSpec inModelSpec, final DataTableSpec inDataSpec)
        throws InvalidSettingsException {
        String[] selectedCols = computeSelectedColumns(inDataSpec);
        DataColumnSpec[] usedColumns = new DataColumnSpec[selectedCols.length + 1];
        int k = 0;
        Set<String> hash = new HashSet<String>(Arrays.asList(selectedCols));
        for (DataColumnSpec dcs : inDataSpec) {
            if (hash.contains(dcs.getName())) {
                usedColumns[k++] = dcs;
            }
        }

        usedColumns[k++] = inDataSpec.getColumnSpec(m_settings.getTargetColumn());

        DataTableSpec tableSpec = new DataTableSpec(usedColumns);
        PMMLPortObjectSpecCreator crea = new PMMLPortObjectSpecCreator(inModelSpec, inDataSpec);
        crea.setLearningCols(tableSpec);
        crea.setTargetCol(usedColumns[k - 1]);
        return crea.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        DataTableSpec inSpec = inTable.getDataTableSpec();

        final int colCount = inSpec.getNumColumns();
        String[] selectedCols = computeSelectedColumns(inSpec);
        Set<String> hash = new HashSet<String>(Arrays.asList(selectedCols));
        m_colSelected = new boolean[colCount];
        for (int i = 0; i < colCount; i++) {
            m_colSelected[i] = hash.contains(inTable.getDataTableSpec().getColumnSpec(i).getName());
        }

        final int rowCount = inTable.getRowCount();

        String[] temp = new String[m_columnNames.length + 1];
        System.arraycopy(m_columnNames, 0, temp, 0, m_columnNames.length);
        temp[temp.length - 1] = m_settings.getTargetColumn();
        FilterColumnTable filteredTable = new FilterColumnTable(inTable, temp);
        final DataArray rowContainer = new DefaultDataArray(filteredTable, 1, m_settings.getMaxRowsForView());

        // handle the optional PMML input
        PMMLPortObject inPMMLPort = m_pmmlInEnabled ? (PMMLPortObject)inData[1] : null;

        PortObjectSpec[] outputSpec = configure((inPMMLPort == null) ? new PortObjectSpec[] {inData[0].getSpec(), null} : new PortObjectSpec[] {inData[0].getSpec(), inPMMLPort.getSpec()});
        Learner learner = new Learner((PMMLPortObjectSpec)outputSpec[0], 0d, m_settings.getMissingValueHandling() == MissingValueHandling.fail, m_settings.getDegree());
        try {
            PolyRegContent polyRegContent = learner.perform(inTable, exec);
            m_betas = fillBeta(polyRegContent);
            m_meanValues = polyRegContent.getMeans();

            ColumnRearranger crea = new ColumnRearranger(inTable.getDataTableSpec());
            crea.append(getCellFactory(inTable.getDataTableSpec().findColumnIndex(m_settings.getTargetColumn())));

            PortObject[] bdt =
                new PortObject[]{createPMMLModel(inPMMLPort, inSpec), exec.createColumnRearrangeTable(inTable, crea, exec.createSilentSubExecutionContext(.2)),
                    polyRegContent.createTablePortObject(exec.createSubExecutionContext(0.2))};
            m_squaredError /= rowCount;
            if (polyRegContent.getWarningMessage() != null) {
                setWarningMessage(polyRegContent.getWarningMessage());
            }

            double[] stdErrors = PolyRegViewData.mapToArray(polyRegContent.getStandardErrors(), m_columnNames, m_settings.getDegree(), polyRegContent.getInterceptStdErr());
            double[] tValues = PolyRegViewData.mapToArray(polyRegContent.getTValues(), m_columnNames, m_settings.getDegree(), polyRegContent.getInterceptTValue());
            double[] pValues = PolyRegViewData.mapToArray(polyRegContent.getPValues(), m_columnNames, m_settings.getDegree(), polyRegContent.getInterceptPValue());
            m_viewData =
                new PolyRegViewData(m_meanValues, m_betas, stdErrors, tValues, pValues, m_squaredError, polyRegContent.getAdjustedRSquared(), m_columnNames, m_settings.getDegree(),
                    m_settings.getTargetColumn(), rowContainer);
            return bdt;
        } catch (ModelSpecificationException e) {
            final String origWarning = getWarningMessage();
            final String warning =
                (origWarning != null && !origWarning.isEmpty()) ? (origWarning + "\n") : "" + e.getMessage();
            setWarningMessage(warning);
            final ExecutionContext subExec = exec.createSubExecutionContext(.1);
            final BufferedDataContainer empty = subExec.createDataContainer(STATS_SPEC);
            int rowIdx = 1;
            for (final String column : m_columnNames) {
                for (int d = 1; d <= m_settings.getDegree(); ++d) {
                    empty.addRowToTable(new DefaultRow("Row" + rowIdx++, new StringCell(column), new IntCell(d),
                        new DoubleCell(0.0d), DataType.getMissingCell(), DataType.getMissingCell(), DataType
                            .getMissingCell()));
                }
            }
            empty.addRowToTable(new DefaultRow("Row" + rowIdx, new StringCell("Intercept"), new IntCell(0),
                new DoubleCell(0.0d), DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell()));
            double[] nans = new double[m_columnNames.length * m_settings.getDegree() + 1];
            Arrays.fill(nans, Double.NaN);
            m_betas = new double[nans.length];
            //Mean only for the linear tags
            m_meanValues = new double[nans.length / m_settings.getDegree()];
            m_viewData =
                new PolyRegViewData(m_meanValues, m_betas, nans, nans, nans, m_squaredError, Double.NaN, m_columnNames,
                    m_settings.getDegree(), m_settings.getTargetColumn(), rowContainer);
            empty.close();
            ColumnRearranger crea = new ColumnRearranger(inTable.getDataTableSpec());
            crea.append(getCellFactory(inTable.getDataTableSpec().findColumnIndex(m_settings.getTargetColumn())));
            BufferedDataTable rearrangerTable = exec.createColumnRearrangeTable(inTable, crea, exec.createSubProgress(0.6));
            PMMLPortObject model = createPMMLModel(inPMMLPort, inTable.getDataTableSpec());

            PortObject[] bdt = new PortObject[]{model, rearrangerTable, empty.getTable()};
            return bdt;
        }
    }

    /**
     * @param polyRegContent
     * @return
     */
    private double[] fillBeta(final PolyRegContent polyRegContent) {
        Map<Pair<String, Integer>, Double> coefficients = polyRegContent.getCoefficients();
        double[] ret = new double[coefficients.size() + 1];
        ret[0] = polyRegContent.getIntercept();
        int deg = m_settings.getDegree();
        for (int i = 0; i < m_columnNames.length; i++) {
            for (int k = 1; k <= deg; k++) {
                ret[i * deg + k] = coefficients.get(Pair.create(m_columnNames[i], k));
            }
        }
        return ret;
    }

    private PMMLPortObject createPMMLModel(final PMMLPortObject inPMMLPort, final DataTableSpec inSpec)
        throws InvalidSettingsException, SAXException {
        NumericPredictor[] preds = new NumericPredictor[m_betas.length - 1];

        int deg = m_settings.getDegree();
        for (int i = 0; i < m_columnNames.length; i++) {
            for (int k = 0; k < deg; k++) {
                preds[i * deg + k] = new NumericPredictor(m_columnNames[i], k + 1, m_betas[i * deg + k + 1]);
            }
        }

        RegressionTable tab = new RegressionTable(m_betas[0], preds);
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

        PMMLPortObject outPMMLPort = new PMMLPortObject(spec, inPMMLPort, inSpec);

        PMMLRegressionTranslator trans =
            new PMMLRegressionTranslator("KNIME Polynomial Regression", "PolynomialRegression", tab, targetField);
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
                    if ((col == dependentIndex || m_colSelected[col]) && row.getCell(col).isMissing()) {
                        switch (m_settings.getMissingValueHandling()) {
                            case ignore:
                                return new DataCell[] {DataType.getMissingCell(), DataType.getMissingCell()};
                            case fail:
                                throw new IllegalStateException("Should failed earlier!");
                                default:
                                    throw new UnsupportedOperationException("Not supported missing handling strategy: " + m_settings.getMissingValueHandling());
                        }
                    }
                    if ((col != dependentIndex) && m_colSelected[col]) {
                        final double value = ((DoubleValue)row.getCell(col)).getDoubleValue();
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
                DataColumnSpecCreator crea = new DataColumnSpecCreator("PolyReg prediction", DoubleCell.TYPE);
                DataColumnSpec col1 = crea.createSpec();

                crea = new DataColumnSpecCreator("Prediction Error", DoubleCell.TYPE);
                DataColumnSpec col2 = crea.createSpec();
                return new DataColumnSpec[]{col1, col2};
            }

            @Override
            public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
                final ExecutionMonitor execMon) {
                // do nothing
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        File f = new File(nodeInternDir, "data.zip");
        final DataArray rowContainer;
        if (f.exists()) {
            ContainerTable t = DataContainer.readFromZip(f);
            int rowCount = t.getRowCount();
            rowContainer = new DefaultDataArray(t, 1, rowCount, exec);
        } else {
            throw new FileNotFoundException("Internals do not exist");
        }
        f = new File(nodeInternDir, "internals.xml");
        if (f.exists()) {
            NodeSettingsRO internals = NodeSettings.loadFromXML(new BufferedInputStream(new FileInputStream(f)));
            try {
                double[] betas = internals.getDoubleArray("betas");
                String[] columnNames = internals.getStringArray("columnNames");
                double squaredError = internals.getDouble("squaredError");
                double adjustedR2 = internals.getDouble("adjustedSquaredError", Double.NaN);
                double[] meanValues = internals.getDoubleArray("meanValues");
                double[] emptyArray = new double[betas.length];
                Arrays.fill(emptyArray, Double.NaN);
                double[] stdErrs = internals.getDoubleArray("stdErrors", emptyArray);
                double[] tValues = internals.getDoubleArray("tValues", emptyArray);
                double[] pValues = internals.getDoubleArray("pValues", emptyArray);
                m_viewData =
                    new PolyRegViewData(meanValues, betas, stdErrs, tValues, pValues, squaredError, adjustedR2, columnNames, m_settings.getDegree(),
                        m_settings.getTargetColumn(), rowContainer);
            } catch (InvalidSettingsException ex) {
                throw new IOException("Old or corrupt internals", ex);
            }
        } else {
            throw new FileNotFoundException("Internals do not exist");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        double[] noData = new double[0];
        m_viewData = new PolyRegViewData(noData, noData, noData, noData, noData, Double.NaN, Double.NaN, new String[0], 0, "N/A", null);
        m_betas = null;
        m_columnNames = null;
        m_squaredError = 0;
        m_meanValues = null;
        m_colSelected = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        if (m_viewData != null) {//Is this necessary?
            NodeSettings internals = new NodeSettings("internals");
            internals.addDoubleArray("betas", m_viewData.betas);
            internals.addStringArray("columnNames", m_viewData.columnNames);
            internals.addDouble("squaredError", m_viewData.squaredError);
            internals.addDouble("adjustedSquaredError", m_viewData.m_adjustedR2);
            internals.addDoubleArray("meanValues", m_viewData.meanValues);
            internals.addDoubleArray("stdErrors", m_viewData.m_stdErrs);
            internals.addDoubleArray("tValues", m_viewData.m_tValues);
            internals.addDoubleArray("pValues", m_viewData.m_pValues);

            internals
                .saveToXML(new BufferedOutputStream(new FileOutputStream(new File(nodeInternDir, "internals.xml"))));

            File dataFile = new File(nodeInternDir, "data.zip");
            DataContainer.writeToZip(m_viewData.getRowContainer(), dataFile, exec);
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
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        PolyRegLearnerSettings s = new PolyRegLearnerSettings();
        s.loadSettingsFrom(settings);

        if (s.getTargetColumn() == null) {
            throw new InvalidSettingsException("No target column selected");
        }
    }

    /**
     * Depending on whether the includeAll flag is set, it determines the list of learning (independent) columns. If the
     * flag is not set, it returns the list stored in m_settings.
     *
     * @param spec to get column names from.
     * @return The list of learning columns.
     * @throws InvalidSettingsException If no valid columns are in the spec.
     */
    private String[] computeSelectedColumns(final DataTableSpec spec) throws InvalidSettingsException {
        String target = m_settings.getTargetColumn();
        FilterResult filterResult = m_settings.getFilterConfiguration().applyTo(spec);
        String[] includes = filterResult.getIncludes();
//        boolean targetIsPresetSet = target != null;
        if (target == null) {
            if (spec.containsCompatibleType(DoubleValue.class)) {
                for (DataColumnSpec colSpec : spec) {
                    if (colSpec.getType().isCompatible(DoubleValue.class)) {
                        target = colSpec.getName();
                    }
                }
            } else {
                throw new InvalidSettingsException("No target column selected");
            }
            m_settings.setTargetColumn(target);
        }
        boolean targetIsIncluded = false;
        for (String incl : includes) {
            targetIsIncluded |= incl.equals(target);
        }
        if (targetIsIncluded) {
//            String warningMessage = "The selected columns " + Arrays.asList(includes)+" also contain the target column: " + target +", removing target!";
//            if (targetIsPresetSet) {
//                m_logger.warn(warningMessage);
//                setWarningMessage(warningMessage);
//            }
            List<String>tmp = new ArrayList<>(Arrays.asList(includes));
            tmp.remove(target);
            includes = tmp.toArray(new String[includes.length - 1]);
        }
        if (includes.length == 0) {
            throw new InvalidSettingsException("No double-compatible variables (learning columns) in input table");
        }
        return includes;
    }

    /**
     * {@inheritDoc}
     * @deprecated Use {@link #getViewData()} {@link PolyRegViewData#getRowContainer()} instead as this might cause inconsistencies!
     */
    @SuppressWarnings("javadoc")
    @Override
    @Deprecated
    public DataArray getDataArray(final int index) {
        return m_viewData != null ? m_viewData.getRowContainer() : null;
    }

    /**
     * Returns the data for the two views. Is never <code>null</code>.
     *
     * @return the view data
     */
    PolyRegViewData getViewData() {
        return m_viewData;
    }
}
