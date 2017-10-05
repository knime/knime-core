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
 * -------------------------------------------------------------------
 *
 * History
 *   22.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.polynomial.learner2;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.knime.base.node.mine.regression.RegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.FunctionName;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.ModelType;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCovCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLParameter;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPredictor;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.util.Pair;

/**
 * Utility class that stores results of linear regression models. It is used by the learner node model and the predictor
 * node model.
 *
 * @author Heiko Hofer
 * @author Adrian Nembach, KNIME.com
 * @since 3.3
 */
final class PolyRegContent extends RegressionContent {
    private static final DataTableSpec m_tableOutSpec =
            new DataTableSpec("Coefficients and Statistics", new String[]{"Variable", "Exponent", "Coeff.",
                "Std. Err.", "t-value", "P>|t|"}, new DataType[]{StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE,
                DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE});


    /**
     * Empty constructor used for serialization.
     */
    private PolyRegContent() {
        super(0, false);
        // Internal use only.
        m_outSpec = null;
        m_valueCount = -1;
        m_rSquared = Double.NaN;
        m_adjustedRSquared = Double.NaN;
        m_factorList = null;
        m_factorDomainValues = new HashMap<String, List<DataCell>>();
        m_covariateList = null;
        m_beta = null;
        m_offsetValue = -1;
        m_covMat = null;
        m_means = null;
    }

    /**
     * Create new instance.
     *
     * @param outSpec the spec of the output
     * @param valueCount the number of data values in the training data set
     * @param factorList the factors (nominal parameters)
     * @param covariateList the covariates (numeric parameters)
     * @param beta the estimated regression factors
     * @param offsetValue offset value (a user defined intercept)
     * @param covMat the covariance matrix
     * @param rSquared the r-square value
     * @param adjustedRSquared the adjusted r-quare value
     * @param stats summary statistics of the parameters (for all powers)
     * @param maxExponent The maximal exponent in the model.
     */
    PolyRegContent(final PMMLPortObjectSpec outSpec, final int valueCount, final List<String> factorList,
        final List<String> covariateList, final RealMatrix beta, final double offsetValue, final RealMatrix covMat,
        final double rSquared, final double adjustedRSquared, final SummaryStatistics[] stats, final int maxExponent) {
        super(maxExponent, true);
        m_outSpec = outSpec;
        m_valueCount = valueCount;
        m_rSquared = rSquared;
        m_adjustedRSquared = adjustedRSquared;
        m_factorList = factorList;
        m_factorDomainValues = new HashMap<String, List<DataCell>>();
        m_covariateList = covariateList;
        m_beta = beta;
        m_offsetValue = offsetValue;
        m_covMat = covMat;
        //We computed the statistics for all powers, but we need the mean only for the linear tags
        m_means = new double[stats.length / m_maxExponent];
        for (int i = 0; i < m_means.length; i++) {
            m_means[i] = stats[i].getMean();
        }
        init();
    }

    /**
     * Returns the parameters mapped to the values of the given matrix.
     *
     * @param matrix the matrix with the raw data
     * @return the variables mapped to values of the given matrix
     */
    @Override
    protected Map<Pair<String, Integer>, Double> getValues(final RealMatrix matrix) {
        Map<Pair<String, Integer>, Double> coefficients = new HashMap<Pair<String, Integer>, Double>();
        int p = m_includeConstant ? 1 : 0;
        for (int degree = 1; degree <= m_maxExponent; ++degree) {
            for (String colName : m_outSpec.getLearningFields()) {
                if (m_factorList.contains(colName)) {
                    Iterator<DataCell> designIter = m_factorDomainValues.get(colName).iterator();
                    // Omit first
                    designIter.next();
                    while (designIter.hasNext()) {
                        DataCell dvValue = designIter.next();
                        String variable = colName + "=" + dvValue;
                        double coeff = matrix.getEntry(0, p);
                        coefficients.put(Pair.create(variable, degree), coeff);
                        p++;
                    }
                } else {
                    String variable = colName;
                    double coeff = matrix.getEntry(0, p);
                    coefficients.put(Pair.create(variable, degree), coeff);
                    p++;
                }
            }
        }
        return coefficients;
    }


    /**
     * Creates a new PMML General Regression Content from this polynomial regression model.
     *
     * @return the PMMLGeneralRegressionContent
     */
    @Override
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
                parameterList.add(new PMMLParameter(pName, colName));
                ppMatrix.add(new PMMLPPCell("1", colName, pName));
                paramMatrix.add(new PMMLPCell(pName, m_beta.getEntry(0, p), 1));

                p++;
            }
        }

        List<PMMLPCovCell> pCovMatrix = new ArrayList<PMMLPCovCell>();

        PMMLGeneralRegressionContent content =
            new PMMLGeneralRegressionContent(ModelType.generalLinear, "KNIME Polynomial Regression",
                FunctionName.regression, "LinearRegression", parameterList.toArray(new PMMLParameter[0]),
                factors.toArray(new PMMLPredictor[0]), covariates.toArray(new PMMLPredictor[0]),
                ppMatrix.toArray(new PMMLPPCell[0]), pCovMatrix.toArray(new PMMLPCovCell[0]),
                paramMatrix.toArray(new PMMLPCell[0]));
        if (!m_includeConstant) {
            content.setOffsetValue(m_offsetValue);
        }
        return content;
    }

    private static final String CFG_MAX_EXPONENT = "max_exponent";

    /**
     * @param parContent the content that holds the internals
     * @param spec the data table spec of the training data
     * @return a instance with he loaded values
     * @throws InvalidSettingsException when data are not well formed
     */
    protected static PolyRegContent load(final ModelContentRO parContent, final DataTableSpec spec)
        throws InvalidSettingsException {
        PolyRegContent c = new PolyRegContent();
        String target = parContent.getString(CFG_TARGET);
        String[] learningCols = parContent.getStringArray(CFG_LEARNING_COLS);
        c.m_outSpec = createSpec(spec, target, learningCols);
        c.m_valueCount = parContent.getInt(CFG_VALUE_COUNT);

        String[] factors = parContent.getStringArray(CFG_FACTORS);
        c.m_factorList = Arrays.asList(factors);

        String[] covariates = parContent.getStringArray(CFG_COVARIATES);
        c.m_covariateList = Arrays.asList(covariates);

        double[] coeff = parContent.getDoubleArray(CFG_COEFFICIENTS);
        c.m_beta = toMatrix(coeff, coeff.length);

        double[] covMat = parContent.getDoubleArray(CFG_COVARIANCE_MATRIX);
        c.m_covMat = toMatrix(covMat, coeff.length);

        c.m_rSquared = parContent.getDouble(CFG_R_SQUARED);
        c.m_adjustedRSquared = parContent.getDouble(CFG_ADJUSTED_R_SQUARED);

        c.m_means = parContent.getDoubleArray(CFG_MEANS);
        c.m_maxExponent = parContent.getInt(CFG_MAX_EXPONENT);

        c.init();

        return c;
    }

    /**
     * Save internals to the given content.
     *
     * @param parContent the content used as a storage
     */
    @Override
    public void save(final ModelContentWO parContent) {
        super.save(parContent);
        parContent.addInt(CFG_MAX_EXPONENT, m_maxExponent);
    }

    /**
     * @return The maximal exponent in the model.
     */
    public int getMaxExponent() {
        return m_maxExponent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addDegree(final int degree, final List<DataCell> cells) {
        cells.add(new IntCell(degree));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec outputTableSpec() {
        return m_tableOutSpec;
    }
}
