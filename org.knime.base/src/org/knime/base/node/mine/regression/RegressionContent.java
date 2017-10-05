/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * Created on 2014.01.28. by Gabor Bakos
 */
package org.knime.base.node.mine.regression;

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
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
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
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.util.Pair;

/**
 * Base class for the learned statistics of the (linear or polynomial) regression models. <br>
 *
 * @author Gabor Bakos
 * @author Adrian Nembach, KNIME.com
 * @since 3.4
 */
public abstract class RegressionContent {

    /** The output port object spec. */
    protected PMMLPortObjectSpec m_outSpec;

    /** The nominal column names. */
    protected List<String> m_factorList;

    /** The values of nominal column names. */
    protected Map<String, List<DataCell>> m_factorDomainValues;

    /** The selected numeric value list. */
    protected List<String> m_covariateList;

    /**
     * The matrix containing the coefficients. The rows are the different degrees, while the columns correspond to the
     * selected columns.
     * */
    protected RealMatrix m_beta;

    /** The covariance matrix. The rows are the different degrees, while the columns correspond to the selected columns. */
    protected RealMatrix m_covMat;

    /**
     * the <a href="http://www.xycoon.com/coefficient1.htm"> coefficient of multiple determination</a>, usually denoted
     * r-square.
     */
    protected double m_rSquared;

    /** the adjusted R-squared statistic. */
    protected double m_adjustedRSquared;

    /** the mean of the parameters. */
    protected double[] m_means;

    /** The number of data values in the training data set. */
    protected int m_valueCount;

    /** The maximal exponent of the model. */
    protected int m_maxExponent;

    /** false when the estimate should go through the origin. */
    protected boolean m_includeConstant;

    /** offset value (a user defined intercept). */
    protected double m_offsetValue;

    /**
     * Warnings message.
     * @since 2.11
     */
    protected String m_warningMessage;

    /** Computes the standard error. */
    private RealMatrix getStdErrorMatrix() {
        // the standard error estimate
        RealMatrix stdErr = MatrixUtils.createRealMatrix(1, m_covMat.getRowDimension());
        for (int i = 0; i < m_covMat.getRowDimension(); i++) {
            stdErr.setEntry(0, i, sqrt(abs(m_covMat.getEntry(i, i))));
        }
        return stdErr;
    }

    /** Computes the t-value statistic. */
    private RealMatrix getTValueMatrix() {
        RealMatrix stdErr = getStdErrorMatrix();
        // Wald's statistic
        RealMatrix tValueMatrix = MatrixUtils.createRealMatrix(1, m_covMat.getRowDimension());
        for (int i = 0; i < m_covMat.getRowDimension(); i++) {
            tValueMatrix.setEntry(0, i, m_beta.getEntry(0, i) / stdErr.getEntry(0, i));
        }
        return tValueMatrix;
    }

    /**
     * Computes the two-tailed p-values of the t-test.
     */
    private RealMatrix getPValueMatrix() {
        RealMatrix tValues = getTValueMatrix();
        // p-value
        RealMatrix pValueMatrix = MatrixUtils.createRealMatrix(1, m_covMat.getRowDimension());
        try {
            for (int i = 0; i < m_covMat.getRowDimension(); i++) {
                TDistribution distribution = new TDistribution(getN() - m_covMat.getRowDimension());
                double t = Math.abs(tValues.getEntry(0, i));
                pValueMatrix.setEntry(0, i, 2 * (1 - distribution.cumulativeProbability(t)));
            }
        } catch (NotStrictlyPositiveException e) {
            for (int i = m_covMat.getRowDimension(); i-- > 0;) {
                pValueMatrix.setEntry(0, i, Double.NaN);
            }
        }
        return pValueMatrix;
    }

    /**
     * Get number of values from the training data.
     *
     * @return the number of training data points
     */
    private int getN() {
        return m_valueCount;
    }

    /**
     * Get the means of the parameters.
     *
     * @return the means of the parameters
     */
    public double[] getMeans() {
        return m_means;
    }

    /**
     * Returns the parameters. The follow the notation rule: - for covariate (numeric learning column): "column_name" -
     * for factors (nominal learning columns) there are n-1 entries when n is the number of domain values:
     * "column_name=domain_value"
     *
     * @return the parameters
     */
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<String>();

        for (String colName : m_outSpec.getLearningFields()) {
            if (m_factorList.contains(colName)) {
                Iterator<DataCell> designIter = m_factorDomainValues.get(colName).iterator();
                if (!designIter.hasNext()) {
                    continue;
                }
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
    public Map<Pair<String, Integer>, Double> getCoefficients() {
        return getValues(m_beta);
    }

    /**
     * Returns the parameters mapped to the standard error.
     *
     * @return the parameters mapped to the standard error
     */
    public Map<Pair<String, Integer>, Double> getStandardErrors() {
        RealMatrix stdErr = getStdErrorMatrix();
        return getValues(stdErr);
    }

    /**
     * Returns the parameters mapped to the t-value.
     *
     * @return the parameters mapped to the t-value
     */
    public Map<Pair<String, Integer>, Double> getTValues() {
        RealMatrix matrix = getTValueMatrix();
        return getValues(matrix);
    }

    /**
     * Returns the parameters mapped to the p-value.
     *
     * @return the parameters mapped to the p-value
     */
    public Map<Pair<String, Integer>, Double> getPValues() {
        RealMatrix matrix = getPValueMatrix();
        return getValues(matrix);
    }

    /**
     * Returns the parameters mapped to the values of the given matrix.
     *
     * @param matrix the matrix with the raw data
     * @return the variables and the exponents mapped to values of the given matrix
     * @since 3.4
     */
    protected Map<Pair<String, Integer>, Double> getValues(final RealMatrix matrix) {
        Map<Pair<String, Integer>, Double> coefficients = new HashMap<Pair<String, Integer>, Double>();
        int p = m_includeConstant ? 1 : 0;
        for (String colName : m_outSpec.getLearningFields()) {
            if (m_factorList.contains(colName)) {
                Iterator<DataCell> designIter = m_factorDomainValues.get(colName).iterator();
                if (!designIter.hasNext()) {
                    continue;
                }
                // Omit first
                designIter.next();
                while (designIter.hasNext()) {
                    DataCell dvValue = designIter.next();
                    String variable = colName + "=" + dvValue;
                    double coeff = matrix.getEntry(0, p);
                    coefficients.put(Pair.create(variable, 1), coeff);
                    p++;
                }
            } else {
                String variable = colName;
                double coeff = matrix.getEntry(0, p);
                coefficients.put(Pair.create(variable, 1), coeff);
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
        return m_beta.getEntry(0, 0);
    }

    /**
     * Returns the value of the intercept's standard error.
     *
     * @return the value of the intercept's standard error
     */
    public double getInterceptStdErr() {
        RealMatrix stdErr = getStdErrorMatrix();
        return stdErr.getEntry(0, 0);
    }

    /**
     * Returns the value of the intercept's t-value.
     *
     * @return the value of the intercept's t-value
     */
    public double getInterceptTValue() {
        RealMatrix matrix = getTValueMatrix();
        return matrix.getEntry(0, 0);
    }

    /**
     * Returns the value of the intercept's p-value.
     *
     * @return the value of the intercept's p-value
     */
    public double getInterceptPValue() {
        RealMatrix matrix = getPValueMatrix();
        return matrix.getEntry(0, 0);
    }

    /**
     * Get the covariates.
     *
     * @return the list of covariates.
     */
    public List<String> getCovariates() {
        return m_covariateList;
    }

    /**
     * Creates a BufferedDataTable with the estimated parameters and statistics.
     *
     * @param exec The execution context
     * @return a port object
     */
    public BufferedDataTable createTablePortObject(final ExecutionContext exec) {
        DataTableSpec tableOutSpec = outputTableSpec();
        BufferedDataContainer dc = exec.createDataContainer(tableOutSpec);
        List<String> parameters = this.getParameters();
        int c = 0;
        Map<Pair<String, Integer>, Double> coefficients = this.getCoefficients();
        Map<Pair<String, Integer>, Double> stdErrs = this.getStandardErrors();
        Map<Pair<String, Integer>, Double> zScores = this.getTValues();
        Map<Pair<String, Integer>, Double> pValues = this.getPValues();

        for (int degree = 1; degree <= m_maxExponent; ++degree) {
            for (String parameter : parameters) {
                Pair<String, Integer> key = Pair.create(parameter, degree);
                List<DataCell> cells = new ArrayList<DataCell>();
                cells.add(new StringCell(parameter));
                addDegree(degree, cells);
                cells.add(value(coefficients.get(key)));
                cells.add(value(stdErrs.get(key)));
                cells.add(value(zScores.get(key)));
                cells.add(value(pValues.get(key)));
                c++;
                dc.addRowToTable(new DefaultRow("Row" + c, cells));
            }
        }

        if (m_includeConstant) {
            List<DataCell> cells = new ArrayList<DataCell>();
            cells.add(new StringCell("Intercept"));
            addDegree(0, cells);
            cells.add(value(this.getIntercept()));
            cells.add(value(this.getInterceptStdErr()));
            cells.add(value(this.getInterceptTValue()));
            cells.add(value(this.getInterceptPValue()));
            c++;
            dc.addRowToTable(new DefaultRow("Row" + c, cells));
        }

        dc.close();
        return dc.getTable();
    }

    /**
     * @param d A value.
     * @return Missing value (in case of {@link Double#NaN} or {@code null}) or a {@link DoubleCell}.
     */
    protected DataCell value(final Double d) {
        return d == null || Double.isNaN(d) ? DataType.getMissingCell() : new DoubleCell(d);
    }

    /**
     * @return The output table spec for the statistics table.
     */
    protected abstract DataTableSpec outputTableSpec();

    /**
     * Adds the degree/exponent information to a row (which is represented as a {@link List} of {@link DataCell}s).
     *
     * @param degree The degree (exponent) of the coefficient.
     * @param cells The cells to add at the end the degree.
     */
    protected abstract void addDegree(int degree, List<DataCell> cells);

    /**
     * Creates a new PMML General Regression Content from this linear regression model.
     *
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
        if (m_includeConstant) {
            // Define the intercept
            parameterList.add(new PMMLParameter("p" + p, "Intercept"));
            paramMatrix.add(new PMMLPCell("p" + p, m_beta.getEntry(0, 0), 1));
            p++;
        }
        for (String colName : m_outSpec.getLearningFields()) {
            if (m_factorList.contains(colName)) {
                Iterator<DataCell> designIter = m_factorDomainValues.get(colName).iterator();
                if (!designIter.hasNext()) {
                    continue;
                }
                // Omit first
                designIter.next();
                while (designIter.hasNext()) {
                    DataCell dvValue = designIter.next();
                    String pName = "p" + p;
                    parameterList.add(new PMMLParameter(pName, "[" + colName + "=" + dvValue + "]"));

                    ppMatrix.add(new PMMLPPCell(dvValue.toString(), colName, pName));
                    paramMatrix.add(new PMMLPCell(pName, m_beta.getEntry(0, p), 1));

                    p++;
                }
            } else {
                String pName = "p" + p;
                parameterList.add(new PMMLParameter("p" + p, colName));
                ppMatrix.add(new PMMLPPCell("1", colName, pName));
                paramMatrix.add(new PMMLPCell(pName, m_beta.getEntry(0, p), 1));

                p++;
            }
        }

        List<PMMLPCovCell> pCovMatrix = new ArrayList<PMMLPCovCell>();

        PMMLGeneralRegressionContent content =
            new PMMLGeneralRegressionContent(ModelType.generalLinear, "KNIME Linear Regression",
                FunctionName.regression, "LinearRegression", parameterList.toArray(new PMMLParameter[0]),
                factors.toArray(new PMMLPredictor[0]), covariates.toArray(new PMMLPredictor[0]),
                ppMatrix.toArray(new PMMLPPCell[0]), pCovMatrix.toArray(new PMMLPCovCell[0]),
                paramMatrix.toArray(new PMMLPCell[0]));
        if (!m_includeConstant) {
            content.setOffsetValue(m_offsetValue);
        }
        return content;
    }

    /** The key for the selected columns. */
    protected static final String CFG_LEARNING_COLS = "learning_cols";

    /** The key for the number of training rows. */
    protected static final String CFG_VALUE_COUNT = "value_count";

    /** The key for the numeric columns. */
    protected static final String CFG_COVARIATES = "covariates";

    /** The key for the coefficients. */
    protected static final String CFG_COEFFICIENTS = "coefficients";

    /** The key for the covariance matrix. */
    protected static final String CFG_COVARIANCE_MATRIX = "covariance_matrix";

    /** The key for R^2. */
    protected static final String CFG_R_SQUARED = "r_squared";

    /** The key for adjusted R^2. @see {@link RegressionContent#getAdjustedRSquared()} */
    protected static final String CFG_ADJUSTED_R_SQUARED = "adjusted_r_squared";

    /** The key for the column means (for scatter plots). */
    protected static final String CFG_MEANS = "means";

    /** The key for the nominal columns. */
    protected static final String CFG_FACTORS = "factors";

    /** The key for whether include constant in estimation or not. {@link #getIncludeConstant()} */
    protected static final String CFG_INCLUDE_CONSTANT = "include_constant";

    /** The key for the custom offset value. */
    protected static final String CFG_OFFSET_VALUE = "offset_value";

    /** The column to estimate. */
    protected static final String CFG_TARGET = "target";

    /**
     * @param spec The input table spec.
     * @param target The target column name.
     * @param learningCols The columns used for learning.
     * @return The {@link PMMLPortObjectSpec} to corresponding to the generated {@link PMMLPortObject}.
     */
    protected static PMMLPortObjectSpec createSpec(final DataTableSpec spec, final String target,
        final String[] learningCols) {
        PMMLPortObjectSpecCreator c = new PMMLPortObjectSpecCreator(spec);
        c.setTargetColName(target);
        c.setLearningColsNames(Arrays.asList(learningCols));
        return c.createSpec();
    }

    /**
     * Converts a {@link RealMatrix} to an array.
     *
     * @param matrix A {@link RealMatrix}.
     * @return The array in order of {@code (0, 0)}, ... {@code (0, n)}, {@code (1, 0)}, ... {@code (m, n)}.
     * @since 3.4
     */
    protected static double[] toArray(final RealMatrix matrix) {
        int m = matrix.getRowDimension();
        int n = matrix.getColumnDimension();
        int length = m * n;
        double[] array = new double[length];
        int c = 0;
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < n; k++) {
                array[c] = matrix.getEntry(i, k);
                c++;
            }
        }
        return array;
    }

    /**
     * Save internals to the given content.
     *
     * @param parContent the content used as a storage
     */
    public void save(final ModelContentWO parContent) {
        parContent.addString(CFG_TARGET, m_outSpec.getTargetFields().get(0));
        parContent.addStringArray(CFG_LEARNING_COLS, m_outSpec.getLearningFields().toArray(new String[0]));
        parContent.addInt(CFG_VALUE_COUNT, m_valueCount);
        parContent.addStringArray(CFG_FACTORS, m_factorList.toArray(new String[0]));
        parContent.addStringArray(CFG_COVARIATES, m_covariateList.toArray(new String[0]));
        parContent.addDoubleArray(CFG_COVARIANCE_MATRIX, toArray(m_covMat));
        parContent.addDoubleArray(CFG_COEFFICIENTS, toArray(m_beta));
        parContent.addDouble(CFG_OFFSET_VALUE, m_offsetValue);
        parContent.addBoolean(CFG_INCLUDE_CONSTANT, m_includeConstant);
        parContent.addDouble(CFG_R_SQUARED, m_rSquared);
        parContent.addDouble(CFG_ADJUSTED_R_SQUARED, m_adjustedRSquared);
        parContent.addDoubleArray(CFG_MEANS, m_means);
    }

    /**
     * Converts an array to a {@link RealMatrix}.
     *
     * @param array A double array.
     * @param colCount The number to split the array second dimension ({@code n} in {@code m*n}).
     * @return The {@link RealMatrix} in the order of ({@code 0}, ... {@code length / colCount - 1};
     *         {@code length / colCount}, ... {@code length - 1}.
     * @since 3.4
     */
    protected static RealMatrix toMatrix(final double[] array, final int colCount) {
        int length = array.length;
        int m = length / colCount;
        int n = colCount;
        assert length == m * n;
        RealMatrix matrix = MatrixUtils.createRealMatrix(m, n);
        int c = 0;
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < n; k++) {
                matrix.setEntry(i, k, array[c]);
                c++;
            }
        }
        return matrix;
    }

    /**
     * Constructor for {@link RegressionContent}.
     *
     * @param maxExponent The maximal exponent, should be {@code >= 1}.
     * @param includeConstant Should we compute the constant, or we get it as a parameter?
     */
    protected RegressionContent(final int maxExponent, final boolean includeConstant) {
        super();
        this.m_maxExponent = maxExponent;
        this.m_includeConstant = includeConstant;
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
     * <p>
     * Returns the <a href="http://www.xycoon.com/coefficient1.htm"> coefficient of multiple determination</a>, usually
     * denoted r-square.
     * </p>
     *
     * @return r-square, a double in the interval [0, 1]
     */
    public double getRSquared() {
        return m_rSquared;
    }

    /**
     * Returns true when the constant term (intercept) was estimated.
     *
     * @return the include constant property
     */
    public boolean getIncludeConstant() {
        return m_includeConstant;
    }

    /**
     * Get offset value (a user defined intercept).
     *
     * @return offset value (a user defined intercept)
     */
    public double getOffsetValue() {
        return m_offsetValue;
    }

    /**
     * Get the factors.
     *
     * @return the list of factors.
     */
    public List<String> getFactors() {
        return m_factorList;
    }

    /**
     * <p>
     * Returns the adjusted R-squared statistic, defined by the formula
     *
     * <pre>
     * R<sup>2</sup><sub>adj</sub> = 1 - [SSR (n - 1)] / [SSTO (n - p)]
     * </pre>
     *
     * where SSR is the sum of squared residuals}, SSTO is the total sum of squares}, n is the number of observations
     * and p is the number of parameters estimated (including the intercept).
     * </p>
     *
     * <p>
     * If the regression is estimated without an intercept term, what is returned is
     *
     * <pre>
     * <code> 1 - (1 - {@link #getRSquared()} ) * (n / (n - p)) </code>
     * </pre>
     *
     * </p>
     *
     * @return adjusted R-Squared statistic
     */
    public double getAdjustedRSquared() {
        return m_adjustedRSquared;
    }

    /**
     * Initializes the factor domain values.
     */
    protected void init() {
        DataTableSpec inSpec = m_outSpec.getDataTableSpec();
        for (String factor : m_factorList) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(factor);
            List<DataCell> domainValues = new ArrayList<DataCell>();
            domainValues.addAll(colSpec.getDomain().getValues());
            Collections.sort(domainValues, colSpec.getType().getComparator());
            m_factorDomainValues.put(factor, domainValues);
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < m_beta.getColumnDimension(); i++) {
            if (Double.isNaN(m_beta.getEntry(0, i))) {
                m_beta.setEntry(0, i, 0);
                //There are "size" columns
                int size = m_outSpec.getLearningFields().size();
                //In this context "- 1" is caused by the additional constant in the beginning of m_beta.
                //The indexes above "size" are for higher exponents for the same columns in the same order
                //So we use modulo to compute the column's index
                int index = size < i - 1 ? (i - 1) % size : i - 1;
                //The exponent is computed by dividing (and adding 1 as for the constant we do not have it).
                int exponent = (i - 1) / size + 1;
                buf.append(m_outSpec.getLearningFields().get(index));
                if (exponent > 1) {
                    buf.append("^" + exponent);
                }
                buf.append(", ");
            }
        }
        if (buf.length() > 0) {
            buf.delete(buf.length() - 2, buf.length());
            String message = "The following columns are redundant and will not contribute to the model: "
                    + buf.toString() + ". Coefficient statistics will not be accurate and contain missing information.";
            if ((m_warningMessage == null) || m_warningMessage.isEmpty()) {
                m_warningMessage = message;
            } else {
                m_warningMessage = "\n" + message;
            }
        }
    }

    /**
     * @return the warningMessage
     * @since 2.11
     */
    public String getWarningMessage() {
        return m_warningMessage;
    }

}
