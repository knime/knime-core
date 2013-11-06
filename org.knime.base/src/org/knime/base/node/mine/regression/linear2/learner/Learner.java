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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   22.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.regression.MillerUpdatingRegression;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.UpdatingMultipleLinearRegression;
import org.knime.base.node.mine.regression.RegressionTrainingData;
import org.knime.base.node.mine.regression.RegressionTrainingRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

import Jama.Matrix;

/**
 * A Linear Regression Learner.
 *
 * @author Heiko Hofer
 */
final class Learner {
    /** Logger to print debug info to. */
    private static final NodeLogger LOGGER = NodeLogger
          .getLogger(Learner.class);

    private final PMMLPortObjectSpec m_outSpec;

    private boolean m_includeConstant;

    /** offset value (a user defined intercept). */
    private double m_offsetValue;



    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     */
    Learner(final PMMLPortObjectSpec spec) {
        this(spec, true, 0.0);
    }


    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     * @param includeConstant include a constant automatically
     * @param offsetValue offset value (a user defined intercept)
     */
    Learner(final PMMLPortObjectSpec spec, final boolean includeConstant, final double offsetValue) {
        m_outSpec = spec;
        m_includeConstant = includeConstant;
        m_offsetValue = offsetValue;
    }

    /**
     * @param data The data table.
     * @param exec The execution context used for reporting progress.
     * @return An object which holds the results.
     * @throws CanceledExecutionException When method is cancelled
     */
    public LinearRegressionContent perform(final DataTable data,
            final ExecutionContext exec) throws CanceledExecutionException {
        exec.checkCanceled();

        RegressionTrainingData trainingData = new RegressionTrainingData(data, m_outSpec);

        UpdatingMultipleLinearRegression regr = new MillerUpdatingRegression(
                trainingData.getRegressorCount(), m_includeConstant);

        int rowCount = 0;
        for (RegressionTrainingRow row : trainingData) {
            double[] parameter = row.getParameter().getArray()[0];
            regr.addObservation(parameter, row.getTarget() - m_offsetValue);
            rowCount++;
        }

        RegressionResults result = regr.regress();

        Matrix beta = new Matrix(result.getParameterEstimates(), 1);



        List<String> factorList = new ArrayList<String>();
        List<String> covariateList = new ArrayList<String>();
        Map<String, List<DataCell>> factorDomainValues =
            new HashMap<String, List<DataCell>>();
        for (int i : trainingData.getActiveCols()) {
            if (trainingData.getIsNominal().get(i)) {
                String factor =
                    data.getDataTableSpec().getColumnSpec(i).getName();
                factorList.add(factor);
                List<DataCell> values = trainingData.getDomainValues().get(i);
                factorDomainValues.put(factor, values);
            } else {
                covariateList.add(
                        data.getDataTableSpec().getColumnSpec(i).getName());
            }
        }


        // The covariance matrix
        int dim = result.getNumberOfParameters();
        Matrix covMat = new Matrix(dim, dim);
        for (int i = 0; i < dim; i++) {
            for (int k = 0; k < dim; k++) {
                covMat.set(i, k, result.getCovarianceOfParameters(i, k));
            }
        }

        LinearRegressionContent content = new LinearRegressionContent(m_outSpec,
            rowCount, factorList, covariateList, beta, m_includeConstant, m_offsetValue, covMat,
            result.getRSquared(), result.getAdjustedRSquared());
        return content;
    }
}
