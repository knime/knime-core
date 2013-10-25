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
 * -------------------------------------------------------------------
 *
 * History
 *   22.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.TDistribution;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.FunctionName;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.ModelType;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCovCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLParameter;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPredictor;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

import Jama.Matrix;

/**
 * Utility class that stores results of linear regression
 * models. It is used by the learner node model and the predictor node model.
 *
 * @author Heiko Hofer
 */
public final class LinearRegressionContent {
    private final PMMLPortObjectSpec m_outSpec;

    private final List<String> m_factorList;
    private final Map<String, List<DataCell>> m_factorDomainValues;
    private final List<String> m_covariateList;

    private final Matrix m_beta;

    private final Matrix m_covMat;

    /** The number of data values in the training data set. */
    private int m_valueCount;

    /** the <a href="http://www.xycoon.com/coefficient1.htm">
     * coefficient of multiple determination</a>,
     * usually denoted r-square. */
    private double m_rSquared;

    /** the adjusted R-squared statistic. */
    private double m_adjustedRSquared;

    /**
     * Create new instance.
     * @param outSpec the spec of the output
     * @param valueCount the number of data values in the training data set
     * @param factorList the factors (nominla parameters)
     * @param covariateList the covariates (numeric parameters)
     * @param beta the estimated regression factors
     * @param covMat the covariance matrix
     * @param rSquared the r-square value
     * @param adjustedRSquared the adjusted r-quare value
     */
    LinearRegressionContent(
            final PMMLPortObjectSpec outSpec,
            final int valueCount,
            final List<String> factorList,
            final List<String> covariateList,
            final Matrix beta,
            final Matrix covMat,
            final double rSquared, final double adjustedRSquared) {
        m_outSpec = outSpec;
        m_valueCount = valueCount;
        m_rSquared = rSquared;
        m_adjustedRSquared = adjustedRSquared;
        m_factorList = factorList;
        m_factorDomainValues = new HashMap<String, List<DataCell>>();
        DataTableSpec inSpec = outSpec.getDataTableSpec();
        for (String factor : factorList) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(factor);
            List<DataCell> domainValues = new ArrayList<DataCell>();
            domainValues.addAll(colSpec.getDomain().getValues());
            Collections.sort(domainValues, colSpec.getType().getComparator());
            m_factorDomainValues.put(factor, domainValues);
        }
        m_covariateList = covariateList;
        m_beta = beta;
        m_covMat = covMat;
    }

    /** Computes the standard error. */
    private Matrix getStdErrorMatrix() {
        // the standard error estimate
        Matrix stdErr = new Matrix(1, m_covMat.getRowDimension());
        for (int i = 0; i < m_covMat.getRowDimension(); i++) {
            stdErr.set(0, i, sqrt(abs(m_covMat.get(i, i))));
        }
        return stdErr;
    }

    /** Computes the t-value statistic. */
    private Matrix getTValueMatrix() {
        Matrix stdErr = getStdErrorMatrix();
        // Wald's statistic
        Matrix tValueMatrix = new Matrix(1, m_covMat.getRowDimension());
        for (int i = 0; i < m_covMat.getRowDimension(); i++) {
            tValueMatrix.set(0, i, m_beta.get(0, i) / stdErr.get(0, i));
        }
        return tValueMatrix;
    }

    /**
     * Computes the two-tailed p-values of the t-test.
     */
    private Matrix getPValueMatrix() {
        Matrix tValues = getTValueMatrix();
        // p-value
        Matrix pValueMatrix = new Matrix(1, m_covMat.getRowDimension());
        for (int i = 0; i < m_covMat.getRowDimension(); i++) {
            TDistribution distribution = new TDistribution(getN() - m_covMat.getRowDimension() - 1);
            double t = Math.abs(tValues.get(0, i));
            pValueMatrix.set(0, i, 1 - distribution.cumulativeProbability(t));
        }
        return pValueMatrix;
    }

    /**
     * Get number of values from the training data.
     * @return the number of training data points
     */
    private int getN() {
        return m_valueCount;
    }

    /**
     * Returns the parameters. The follow the notation rule:
     *   - for covariate (numeric learning column):
     *      "column_name"
     *   - for factors (nominal learning columns) there are n-1 entries
     *     when n is the number of domain values:
     *      "column_name=domain_value"
     * @return the parameters
     */
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<String>();

        for (String colName : m_outSpec.getLearningFields()) {
            if (m_factorList.contains(colName)) {
                Iterator<DataCell> designIter =
                    m_factorDomainValues.get(colName).iterator();
                // Omit first
                designIter.next();
                while (designIter.hasNext()) {
                    DataCell dvValue = designIter.next();
                    String variable = colName + "=" + dvValue;
                    parameters.add(variable);
                }
            } else {
                String variable = colName;
                parameters.add(variable);
            }
        }
        return parameters;
    }

    /**
     * Returns the parameters mapped to the coefficients.
     *
     * @return the variables mapped to the coefficients
     */
    public Map<String, Double> getCoefficients() {
      return getValues(m_beta);
    }

    /**
     * Returns the parameters mapped to the standard error.
     *
     * @return the parameters mapped to the standard error
     */
    public Map<String, Double> getStandardErrors() {
        Matrix stdErr = getStdErrorMatrix();
        return getValues(stdErr);
    }

    /**
     * Returns the parameters mapped to the t-value.
     *
     * @return the parameters mapped to the t-value
     */
    public Map<String, Double> getTValues() {
        Matrix matrix = getTValueMatrix();
        return getValues(matrix);
    }

    /**
     * Returns the parameters mapped to the p-value.
     *
     * @return the parameters mapped to the p-value
     */
    public Map<String, Double> getPValues() {
        Matrix matrix = getPValueMatrix();
        return getValues(matrix);
    }

    /**
     * Returns the parameters mapped to the values of the given matrix.
     * @param matrix the matrix with the raw data
     * @return the variables mapped to values of the given matrix
     */
    private Map<String, Double> getValues(final Matrix matrix) {
        Map<String, Double> coefficients = new HashMap<String, Double>();
        int p = 1;
        for (String colName : m_outSpec.getLearningFields()) {
            if (m_factorList.contains(colName)) {
                Iterator<DataCell> designIter =
                    m_factorDomainValues.get(colName).iterator();
                // Omit first
                designIter.next();
                while (designIter.hasNext()) {
                    DataCell dvValue = designIter.next();
                    String variable = colName + "=" + dvValue;
                    double coeff = matrix.get(0, p);
                    coefficients.put(variable, coeff);
                    p++;
                }
            } else {
                String variable = colName;
                double coeff = matrix.get(0, p);
                coefficients.put(variable, coeff);
                p++;
            }
        }
        return coefficients;
    }

    /**
     * Returns the value of the intercept.
     *
     * @return the value of the intercept
     */
    public double getIntercept() {
        return m_beta.get(0, 0);
    }

    /**
     * Returns the value of the intercept's standard error.
     *
     * @return the value of the intercept's standard error
     */
    public double getInterceptStdErr() {
        Matrix stdErr = getStdErrorMatrix();
        return stdErr.get(0, 0);
    }

    /**
     * Returns the value of the intercept's t-value.
     *
     * @return the value of the intercept's t-value
     */
    public double getInterceptTValue() {
        Matrix matrix = getTValueMatrix();
        return matrix.get(0, 0);
    }

    /**
     * Returns the value of the intercept's p-value.
     *
     * @return the value of the intercept's p-value
     */
    public double getInterceptPValue() {
        Matrix matrix = getPValueMatrix();
        return matrix.get(0, 0);
    }

    /**
     * Creates a BufferedDataTable with the estimated parameters and statistics.
     * @param exec The execution context
     * @return a port object
     */
    public BufferedDataTable createTablePortObject(
            final ExecutionContext exec) {
        DataTableSpec tableOutSpec = new DataTableSpec(
                "Coefficients and Statistics", new String[] {
                  "Variable", "Coeff.", "Std. Err.", "t-value"
                , "P>|t|"},
                new DataType[] {StringCell.TYPE,
                DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE
                , DoubleCell.TYPE});
        BufferedDataContainer dc = exec.createDataContainer(tableOutSpec);
        List<String> parameters = this.getParameters();
        int c = 0;
        Map<String, Double> coefficients = this.getCoefficients();
        Map<String, Double> stdErrs = this.getStandardErrors();
        Map<String, Double> zScores = this.getTValues();
        Map<String, Double> pValues = this.getPValues();

        for (String parameter : parameters) {
            List<DataCell> cells = new ArrayList<DataCell>();
            cells.add(new StringCell(parameter));
            cells.add(new DoubleCell(coefficients.get(parameter)));
            cells.add(new DoubleCell(stdErrs.get(parameter)));
            cells.add(new DoubleCell(zScores.get(parameter)));
            cells.add(new DoubleCell(pValues.get(parameter)));
            c++;
            dc.addRowToTable(new DefaultRow("Row" + c, cells));
        }
        List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell("Intercept"));
        cells.add(new DoubleCell(this.getIntercept()));
        cells.add(new DoubleCell(this.getInterceptStdErr()));
        cells.add(new DoubleCell(this.getInterceptTValue()));
        cells.add(new DoubleCell(this.getInterceptPValue()));
        c++;
        dc.addRowToTable(new DefaultRow("Row" + c, cells));

        dc.close();
        return dc.getTable();
    }

    /**
     * Creates a new PMML General Regression Content from this linear
     * regression model.
     * @return the PMMLGeneralRegressionContent
     */
    public PMMLGeneralRegressionContent createGeneralRegressionContent() {
        List<PMMLPredictor> factors = new ArrayList<PMMLPredictor>();
        for (String factor : m_factorList) {
            PMMLPredictor predictor = new PMMLPredictor(factor);
            factors.add(predictor);
        }
        List<PMMLPredictor> covariates = new ArrayList<PMMLPredictor>();
        for (String covariate : m_covariateList) {
            PMMLPredictor predictor = new PMMLPredictor(covariate);
            covariates.add(predictor);
        }
        // the ParameterList, the PPMatrix and the ParamMatrix
        List<PMMLParameter> parameterList = new ArrayList<PMMLParameter>();
        List<PMMLPPCell> ppMatrix = new ArrayList<PMMLPPCell>();
        List<PMMLPCell> paramMatrix = new ArrayList<PMMLPCell>();

        int p = 0;
        // Define the intercept
        parameterList.add(new PMMLParameter("p" + p, "Intercept"));
        // TODO: define offsetValue of 0 when offset is not estimated
        paramMatrix.add(new PMMLPCell("p" + p, m_beta.get(0, 0), 1));
        p++;
        for (String colName : m_outSpec.getLearningFields()) {
            if (m_factorList.contains(colName)) {
                Iterator<DataCell> designIter =
                    m_factorDomainValues.get(colName).iterator();
                // Omit first
                designIter.next();
                while (designIter.hasNext()) {
                    DataCell dvValue = designIter.next();
                    String pName = "p" + p;
                    parameterList.add(new PMMLParameter(pName,
                            "[" + colName + "=" + dvValue + "]"));

                    ppMatrix.add(new PMMLPPCell(dvValue.toString(), colName, pName));
                    paramMatrix.add(new PMMLPCell(pName, m_beta.get(0, p), 1));

                    p++;
                }
            } else {
                String pName = "p" + p;
                parameterList.add(new PMMLParameter("p" + p, colName));
                ppMatrix.add(new PMMLPPCell("1", colName, pName));
                paramMatrix.add(new PMMLPCell(pName, m_beta.get(0, p), 1));

                p++;
            }
        }

        List<PMMLPCovCell> pCovMatrix = new ArrayList<PMMLPCovCell>();

        PMMLGeneralRegressionContent content = new PMMLGeneralRegressionContent(
                    ModelType.generalLinear,
                    "KNIME Linear Regression",
                    FunctionName.regression,
                    "LinearRegression",
                    parameterList.toArray(new PMMLParameter[0]),
                    factors.toArray(new PMMLPredictor[0]),
                    covariates.toArray(new PMMLPredictor[0]),
                    ppMatrix.toArray(new PMMLPPCell[0]),
                    pCovMatrix.toArray(new PMMLPCovCell[0]),
                    paramMatrix.toArray(new PMMLPCell[0]));
        return content;
    }

    private static final String CFG_TARGET = "target";
    private static final String CFG_LEARNING_COLS = "learning_cols";
    private static final String CFG_VALUE_COUNT = "value_count";
    private static final String CFG_FACTORS = "factors";
    private static final String CFG_COVARIATES = "covariates";
    private static final String CFG_COEFFICIENTS = "coefficients";
    private static final String CFG_COVARIANCE_MATRIX = "covariance_matrix";
    private static final String CFG_R_SQUARED = "r_squared";
    private static final String CFG_ADJUSTED_R_SQUARED = "adjusted_r_squared";


    /**
     * @param parContent the content that holds the internals
     * @param spec the data table spec of the training data
     * @return a instance with he loaded values
     * @throws InvalidSettingsException when data are not well formed
     */
    static LinearRegressionContent load(
            final ModelContentRO parContent,
            final DataTableSpec spec) throws InvalidSettingsException  {
        String target = parContent.getString(CFG_TARGET);
        String[] learningCols = parContent.getStringArray(CFG_LEARNING_COLS);
        PMMLPortObjectSpec pmmlSpec = createSpec(spec, target, learningCols);

        int valueCount = parContent.getInt(CFG_VALUE_COUNT);
        String[] factors = parContent.getStringArray(CFG_FACTORS);
        String[] covariates = parContent.getStringArray(CFG_COVARIATES);
        double[] coeff = parContent.getDoubleArray(CFG_COEFFICIENTS);
        double[] covMat = parContent.getDoubleArray(CFG_COVARIANCE_MATRIX);
        double rSquared = parContent.getDouble(CFG_R_SQUARED);
        double adjustedRSquared = parContent.getDouble(CFG_ADJUSTED_R_SQUARED);
        return new LinearRegressionContent(pmmlSpec, valueCount,
                Arrays.asList(factors), Arrays.asList(covariates),
                toMatrix(coeff, coeff.length),
                toMatrix(covMat, coeff.length),
                rSquared, adjustedRSquared);


    }

    private static PMMLPortObjectSpec createSpec(final DataTableSpec spec,
            final String target, final String[] learningCols) {
        PMMLPortObjectSpecCreator c = new PMMLPortObjectSpecCreator(spec);
        c.setTargetColName(target);
        c.setLearningColsNames(Arrays.asList(learningCols));
        return c.createSpec();
    }

    /**
     * Save internals to the given content.
     *
     * @param parContent the content used as a storage
     */
    void save(final ModelContentWO parContent) {
        parContent.addString(CFG_TARGET, m_outSpec.getTargetFields().get(0));
        parContent.addStringArray(CFG_LEARNING_COLS, m_outSpec.getLearningFields().toArray(new String[0]));
        parContent.addInt(CFG_VALUE_COUNT, m_valueCount);
        parContent.addStringArray(CFG_FACTORS, m_factorList.toArray(new String[0]));
        parContent.addStringArray(CFG_COVARIATES, m_covariateList.toArray(new String[0]));
        parContent.addDoubleArray(CFG_COVARIANCE_MATRIX, toArray(m_covMat));
        parContent.addDoubleArray(CFG_COEFFICIENTS, toArray(m_beta));
        parContent.addDouble(CFG_R_SQUARED, m_rSquared);
        parContent.addDouble(CFG_ADJUSTED_R_SQUARED, m_adjustedRSquared);
    }

    private static double[] toArray(final Matrix matrix) {
        int m = matrix.getRowDimension();
        int n = matrix.getColumnDimension();
        int length = m * n;
        double[] array = new double[length];
        int c = 0;
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < n; k++) {
                array[c] = matrix.get(i, k);
                c++;
            }
        }
        return array;
    }

    private static Matrix toMatrix(final double[] array,
            final int colCount) {
        int length = array.length;
        int m = length / colCount;
        int n = colCount;
        assert length == m * n;
        Matrix matrix = new Matrix(m, n);
        int c = 0;
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < n; k++) {
                matrix.set(i, k, array[c]);
                c++;
            }
        }
        return matrix;
    }
    /**
     * Returns the spec of the output.
     *
     * @return spec of the output
     */
    public PMMLPortObjectSpec getSpec() {
        return m_outSpec;
    }


    /**
     * <p>Returns the <a href="http://www.xycoon.com/coefficient1.htm">
     * coefficient of multiple determination</a>,
     * usually denoted r-square.</p>
     *
     * @return r-square, a double in the interval [0, 1]
     */
    public double getRSquared() {
        return m_rSquared;
    }

    /**
     * <p>Returns the adjusted R-squared statistic, defined by the formula <pre>
     * R<sup>2</sup><sub>adj</sub> = 1 - [SSR (n - 1)] / [SSTO (n - p)]
     * </pre>
     * where SSR is the sum of squared residuals},
     * SSTO is the total sum of squares}, n is the number
     * of observations and p is the number of parameters estimated (including the intercept).</p>
     *
     * <p>If the regression is estimated without an intercept term, what is returned is <pre>
     * <code> 1 - (1 - {@link #getRSquared()} ) * (n / (n - p)) </code>
     * </pre></p>
     *
     * @return adjusted R-Squared statistic
     */
    public double getAdjustedRSquared() {
        return m_adjustedRSquared;
    }


}
