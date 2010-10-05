/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
package org.knime.base.node.mine.regression.logistic.learner;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionPortObject;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCovCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLParameter;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPredictor;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.FunctionName;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.ModelType;
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
 * Utility class that stores results of logistic regression
 * models. It is used by the learner node model and the predictor node model.
 *
 * @author Heiko Hofer
 */
public final class LogisticRegressionContent {
    private PMMLPortObjectSpec m_outSpec;

    private List<String> m_factorList;
    private Map<String, List<DataCell>> m_factorDomainValues;
    private List<String> m_covariateList;

    private List<DataCell> m_targetCategories;

    private Matrix m_beta;

    private double m_loglike;
    private Matrix m_covMat;

    private int m_iter;


    /**
     * Create new instance.
     * @param outSpec the spec of the output
     * @param factorList the factors (nominla parameters)
     * @param covariateList the covariates (numeric parameters)
     * @param beta the estimated regression factors
     * @param loglike the estimated likelihood
     * @param covMat the covariance matrix
     * @param iter
     */
    LogisticRegressionContent(
            final PMMLPortObjectSpec outSpec,
            final List<String> factorList,
            final List<String> covariateList,
            final Matrix beta, final double loglike,
            final Matrix covMat, final int iter) {
        m_iter = iter;
        m_outSpec = outSpec;
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
        String target = outSpec.getTargetFields().get(0);
        DataColumnSpec colSpec = inSpec.getColumnSpec(target);
        List<DataCell> domainValues = new ArrayList<DataCell>();
        domainValues.addAll(colSpec.getDomain().getValues());
        Collections.sort(domainValues, colSpec.getType().getComparator());
        m_targetCategories = domainValues;
        m_beta = beta;
        m_loglike = loglike;
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

    /** Computes the Wald's statistic. */
    private Matrix getZScoreMatrix() {
        Matrix stdErr = getStdErrorMatrix();
        // Wald's statistic
        Matrix waldStat = new Matrix(1, m_covMat.getRowDimension());
        for (int i = 0; i < m_covMat.getRowDimension(); i++) {
            waldStat.set(0, i, m_beta.get(0, i) / stdErr.get(0, i));
        }
        return waldStat;
    }

    /** Computes the two-tailed p-values of the z-test, which can be calculated
     *  by 2*Phi(−|Z|), where Phi is the standard normal cumulative
     *  distribution function
     */
    private Matrix getPValueMatrix() {
        Matrix zScore = getZScoreMatrix();
        // p-value
        Matrix pvalue = new Matrix(1, m_covMat.getRowDimension());
        for (int i = 0; i < m_covMat.getRowDimension(); i++) {
            double absZ = Math.abs(zScore.get(0, i));
            pvalue.set(0, i, 2*Gaussian.Phi(-absZ));
        }
        return pvalue;
    }


    /**
     * @return the likelihood
     */
    public double getEstimatedLikelihood() {
        return m_loglike;
    }

    /**
     * @return the number of irls iterations
     */
    public int getIterationCount() {
        return m_iter;
    }

    /**
     * Logits are elements of the target domain values except of the last one.
     *
     * @return the logits
     */
    public List<DataCell> getLogits() {
        List<DataCell> logits = new ArrayList<DataCell>();
        logits.addAll(m_targetCategories);
        logits.remove(logits.size() - 1);
        return logits;
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
     * Returns the parameters mapped to the coefficients for the given logit.
     *
     * @param logit the logit
     * @return the variables mapped to the coefficients
     */
    public Map<String, Double> getCoefficients(final DataCell logit) {
        return getValues(logit, m_beta);
    }

    /**
     * Returns the parameters mapped to the standard error for the given logit.
     *
     * @param logit the logit
     * @return the parameters mapped to the standard error
     */
    public Map<String, Double> getStandardErrors(final DataCell logit) {
        Matrix stdErr = getStdErrorMatrix();
        return getValues(logit, stdErr);
    }

    /**
     * Returns the parameters mapped to the z-score for the given logit.
     *
     * @param logit the logit
     * @return the parameters mapped to the z-score
     */
    public Map<String, Double> getZScores(final DataCell logit) {
        Matrix zScore = getZScoreMatrix();
        return getValues(logit, zScore);
    }

    /**
     * Returns the parameters mapped to the p-value for the given logit.
     *
     * @param logit the logit
     * @return the parameters mapped to the p-value
     */
    public Map<String, Double> getPValues(final DataCell logit) {
        Matrix pValue = getPValueMatrix();
        return getValues(logit, pValue);
    }

    private Map<String, Double> getValues(final DataCell logit,
            final Matrix matrix) {
        assert m_targetCategories.contains(logit);

        Map<String, Double> coefficients = new HashMap<String, Double>();
        int pCount = m_beta.getColumnDimension()
            / (m_targetCategories.size() - 1);
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
                    int k = m_targetCategories.indexOf(logit);
                    double coeff = matrix.get(0, p + (k * pCount));
                    coefficients.put(variable, coeff);
                    p++;
                }
            } else {
                String variable = colName;
                int k = m_targetCategories.indexOf(logit);
                double coeff = matrix.get(0, p + (k * pCount));
                coefficients.put(variable, coeff);
                p++;
            }
        }
        return coefficients;
    }

    /**
     * Returns the value of the intercept for the given logit.
     *
     * @param logit the logit
     * @return the value of the intercept
     */
    public double getIntercept(final DataCell logit) {
        return getInterceptValue(logit, m_beta);
    }

    /**
     * Returns the value of the intercept's standard error for the given logit.
     *
     * @param logit the logit
     * @return the value of the intercept's standard error
     */
    public double getInterceptStdErr(final DataCell logit) {
        Matrix stdErr = getStdErrorMatrix();
        return getInterceptValue(logit, stdErr);
    }

    /**
     * Returns the value of the intercept's z-score for the given logit.
     *
     * @param logit the logit
     * @return the value of the intercept's z-score
     */
    public double getInterceptZScore(final DataCell logit) {
        Matrix zScore = getZScoreMatrix();
        return getInterceptValue(logit, zScore);
    }

    /**
     * Returns the value of the intercept's p-value.
     *
     * @param logit the logit
     * @return the value of the intercept's p-value
     */
    public double getInterceptPValue(final DataCell logit) {
        double absZ = Math.abs(getInterceptZScore(logit));
        return 2*Gaussian.Phi(-absZ);
    }

    private double getInterceptValue(final DataCell logit,
            final Matrix matrix) {
        assert m_targetCategories.contains(logit);

        int pCount = m_beta.getColumnDimension()
        / (m_targetCategories.size() - 1);
        int k = m_targetCategories.indexOf(logit);
        return matrix.get(0, k * pCount);
    }

    /**
     * Creates a BufferedDataTable with the
     * @param exec The execution context
     * @return a port object
     */
    public BufferedDataTable createTablePortObject(
            final ExecutionContext exec) {
        DataTableSpec tableOutSpec = new DataTableSpec(
                "Coefficients and Statistics", new String[] {
                "Logit", "Variable", "Coeff.", "Std. Err.", "z-score"
                , "P>|z|"},
                new DataType[] {StringCell.TYPE, StringCell.TYPE,
                DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE
                , DoubleCell.TYPE});
        BufferedDataContainer dc = exec.createDataContainer(tableOutSpec);
        List<DataCell> logits = this.getLogits();
        List<String> parameters = this.getParameters();
        int c = 0;
        for (DataCell logit : logits) {
            Map<String, Double> coefficients =
                this.getCoefficients(logit);
            Map<String, Double> stdErrs =
                this.getStandardErrors(logit);
            Map<String, Double> zScores =
                this.getZScores(logit);
            Map<String, Double> pValues =
                this.getPValues(logit);

            for (String parameter : parameters) {
                List<DataCell> cells = new ArrayList<DataCell>();
                cells.add(new StringCell(logit.toString()));
                cells.add(new StringCell(parameter));
                cells.add(new DoubleCell(coefficients.get(parameter)));
                cells.add(new DoubleCell(stdErrs.get(parameter)));
                cells.add(new DoubleCell(zScores.get(parameter)));
                cells.add(new DoubleCell(pValues.get(parameter)));
                c++;
                dc.addRowToTable(new DefaultRow("Row" + c, cells));
            }
            List<DataCell> cells = new ArrayList<DataCell>();
            cells.add(new StringCell(logit.toString()));
            cells.add(new StringCell("Constant"));
            cells.add(new DoubleCell(this.getIntercept(logit)));
            cells.add(new DoubleCell(this.getInterceptStdErr(logit)));
            cells.add(new DoubleCell(this.getInterceptZScore(logit)));
            cells.add(new DoubleCell(this.getInterceptPValue(logit)));
            c++;
            dc.addRowToTable(new DefaultRow("Row" + c, cells));
        }
        dc.close();
        return dc.getTable();
    }

    /**
     * Creates a new PMML regression port object from this logistic regression
     * model.
     *
     * @return a port object
     * @throws InvalidSettingsException if the settings are invalid
     */
    public PMMLGeneralRegressionPortObject createPMMLPortObject()
            throws InvalidSettingsException {
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
        int pCount = m_beta.getColumnDimension()
            / (m_targetCategories.size() - 1);
        int p = 0;
        parameterList.add(new PMMLParameter("p" + p, "Intercept"));
        for (int k = 0; k < m_targetCategories.size() - 1; k++) {
            paramMatrix.add(new PMMLPCell("p" + p,
                    m_beta.get(0, p + (k * pCount)), 1,
                    m_targetCategories.get(k).toString()));
        }
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

                    ppMatrix.add(new PMMLPPCell(dvValue.toString(), colName,
                            pName));
                    for (int k = 0; k < m_targetCategories.size() - 1; k++) {
                        paramMatrix.add(new PMMLPCell(pName,
                                m_beta.get(0, p + (k * pCount)), 1,
                                m_targetCategories.get(k).toString()));
                    }
                    p++;
                }
            } else {
                String pName = "p" + p;
                parameterList.add(new PMMLParameter("p" + p, colName));
                ppMatrix.add(new PMMLPPCell("1", colName, pName));
                for (int k = 0; k < m_targetCategories.size() - 1; k++) {
                    paramMatrix.add(new PMMLPCell(pName,
                            m_beta.get(0, p + (k * pCount)), 1,
                            m_targetCategories.get(k).toString()));
                }
                p++;
            }
        }

        // TODO PCovMatrix
        List<PMMLPCovCell> pCovMatrix = new ArrayList<PMMLPCovCell>();

        PMMLGeneralRegressionContent content =
            new PMMLGeneralRegressionContent(ModelType.multinomialLogistic,
                    "KNIME Logistic Regression",
                    FunctionName.classification,
                    "LogisticRegression",
                    parameterList.toArray(new PMMLParameter[0]),
                    factors.toArray(new PMMLPredictor[0]),
                    covariates.toArray(new PMMLPredictor[0]),
                    ppMatrix.toArray(new PMMLPPCell[0]),
                    pCovMatrix.toArray(new PMMLPCovCell[0]),
                    paramMatrix.toArray(new PMMLPCell[0]));

        return new PMMLGeneralRegressionPortObject(m_outSpec, content);
    }

    private static final String CFG_TARGET = "target";
    private static final String CFG_LEARNING_COLS = "learning_cols";
    private static final String CFG_FACTORS = "factors";
    private static final String CFG_COVARIATES = "covariates";
    private static final String CFG_COEFFICIENTS = "coefficients";
    private static final String CFG_COVARIANCE_MATRIX = "covariance_matrix";
    private static final String CFG_LOG_LIKELIHOOD = "likelihood";
    private static final String CFG_ITER = "iteration";


    /**
     * @param parContent the content that holds the internals
     * @param spec the data table spec of the training data
     * @return a instance with he loaded values
     * @throws InvalidSettingsException when data are not well formed
     */
    static LogisticRegressionContent load(
            final ModelContentRO parContent,
            final DataTableSpec spec) throws InvalidSettingsException  {
        String target = parContent.getString(CFG_TARGET);
        String[] learningCols = parContent.getStringArray(CFG_LEARNING_COLS);
        PMMLPortObjectSpec pmmlSpec = createSpec(spec, target, learningCols);

        String[] factors = parContent.getStringArray(CFG_FACTORS);
        String[] covariates = parContent.getStringArray(CFG_COVARIATES);
        double[] coeff = parContent.getDoubleArray(CFG_COEFFICIENTS);
        double likelihood = parContent.getDouble(CFG_LOG_LIKELIHOOD);
        double[] covMat = parContent.getDoubleArray(CFG_COVARIANCE_MATRIX);
        int iter = parContent.getInt(CFG_ITER);
        return new LogisticRegressionContent(pmmlSpec,
                Arrays.asList(factors), Arrays.asList(covariates),
                toMatrix(coeff, coeff.length), likelihood,
                toMatrix(covMat, coeff.length), iter);


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
        parContent.addStringArray(CFG_LEARNING_COLS,
                m_outSpec.getLearningFields().toArray(new String[0]));
        parContent.addStringArray(CFG_FACTORS,
                m_factorList.toArray(new String[0]));
        parContent.addStringArray(CFG_COVARIATES,
                m_covariateList.toArray(new String[0]));
        parContent.addDoubleArray(CFG_COVARIANCE_MATRIX,
                toArray(m_covMat));
        parContent.addDoubleArray(CFG_COEFFICIENTS, toArray(m_beta));
        parContent.addDouble(CFG_LOG_LIKELIHOOD, m_loglike);
        parContent.addInt(CFG_ITER, m_iter);
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
     * Gaussian function see
     *        George Marsaglia, Evaluating the Normal Distribution
     *        Journal of Statistical Software
     *        July 2004, Volume 11, Issue 4. http://www.jstatsoft.org/
     *
     * @author Heiko Hofer
     */

    private static class Gaussian {

        // standard gaussian probability function
        public static double phi(final double x) {
            return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
        }

        // standard Gaussian cumulative distributaion function,
        // uses Taylor approximation
        public static double Phi(final double z) {
            if (z < -8.0) return 0.0;
            if (z >  8.0) return 1.0;
            double sum = 0.0, term = z;
            for (int i = 3; sum + term != sum; i += 2) {
                sum  = sum + term;
                term = term * z * z / i;
            }
            return 0.5 + sum * phi(z);
        }

    }
}
