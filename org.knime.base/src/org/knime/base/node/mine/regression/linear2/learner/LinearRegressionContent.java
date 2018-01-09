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
 * -------------------------------------------------------------------
 *
 * History
 *   22.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.knime.base.node.mine.regression.RegressionContent;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Utility class that stores results of linear regression models. It is used by the learner node model and the predictor
 * node model.
 *
 * @author Heiko Hofer
 * @author Adrian Nembach, KNIME.com
 */
public final class LinearRegressionContent extends RegressionContent {
    private static final DataTableSpec m_tableOutSpec = new DataTableSpec("Coefficients and Statistics", new String[]{
        "Variable", "Coeff.", "Std. Err.", "t-value", "P>|t|"}, new DataType[]{StringCell.TYPE, DoubleCell.TYPE,
        DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE});

    /**
     * Empty constructor used for serialization.
     */
    private LinearRegressionContent() {
        super(1, false);
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
     * @param includeConstant false when the estimate should go through the origin
     * @param offsetValue offset value (a user defined intercept)
     * @param covMat the covariance matrix
     * @param rSquared the r-square value
     * @param adjustedRSquared the adjusted r-quare value
     * @param stats summary statistics of the parameters
     * @deprecated use the constructor with the warning message to keep
     */
    @Deprecated
    public LinearRegressionContent(final PMMLPortObjectSpec outSpec, final int valueCount,
        final List<String> factorList, final List<String> covariateList, final RealMatrix beta,
        final boolean includeConstant, final double offsetValue, final RealMatrix covMat, final double rSquared,
        final double adjustedRSquared, final SummaryStatistics[] stats) {
        this(outSpec, valueCount, factorList, covariateList, beta, includeConstant, offsetValue, covMat, rSquared, adjustedRSquared, stats, null);
    }

    /**
     * Create new instance.
     *
     * @param outSpec the spec of the output
     * @param valueCount the number of data values in the training data set
     * @param factorList the factors (nominal parameters)
     * @param covariateList the covariates (numeric parameters)
     * @param beta the estimated regression factors
     * @param includeConstant false when the estimate should go through the origin
     * @param offsetValue offset value (a user defined intercept)
     * @param covMat the covariance matrix
     * @param rSquared the r-square value
     * @param adjustedRSquared the adjusted r-quare value
     * @param stats summary statistics of the parameters
     * @param warningMessage the warning message to use if there was a problem; can be {@code null}
     */
    LinearRegressionContent(final PMMLPortObjectSpec outSpec, final int valueCount,
        final List<String> factorList, final List<String> covariateList, final RealMatrix beta,
        final boolean includeConstant, final double offsetValue, final RealMatrix covMat, final double rSquared,
        final double adjustedRSquared, final SummaryStatistics[] stats, final String warningMessage) {
        super(1, includeConstant);
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
        m_means = new double[stats.length];
        for (int i = 0; i < stats.length; i++) {
            m_means[i] = stats[i].getMean();
        }
        m_warningMessage = warningMessage;
        init();
    }

    /**
     * @param parContent the content that holds the internals
     * @param spec the data table spec of the training data
     * @return a instance with he loaded values
     * @throws InvalidSettingsException when data are not well formed
     */
    protected static LinearRegressionContent load(final ModelContentRO parContent, final DataTableSpec spec)
        throws InvalidSettingsException {
        LinearRegressionContent c = new LinearRegressionContent();
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

        c.m_includeConstant = parContent.getBoolean(CFG_INCLUDE_CONSTANT);
        c.m_offsetValue = parContent.getDouble(CFG_OFFSET_VALUE);

        c.m_rSquared = parContent.getDouble(CFG_R_SQUARED);
        c.m_adjustedRSquared = parContent.getDouble(CFG_ADJUSTED_R_SQUARED);

        c.m_means = parContent.getDoubleArray(CFG_MEANS);

        c.init();

        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addDegree(final int degree, final List<DataCell> cells) {
        //Do nothing to keep compatibility
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec outputTableSpec() {
        return m_tableOutSpec;
    }
}
