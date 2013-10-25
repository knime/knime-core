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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.regression.MillerUpdatingRegression;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.UpdatingMultipleLinearRegression;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
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



    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     */
    Learner(final PMMLPortObjectSpec spec) {
        this(spec, true);
    }

    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     * @param includeConstant include a constant automatically
     */
    Learner(final PMMLPortObjectSpec spec, final boolean includeConstant) {
        m_outSpec = spec;
        m_includeConstant = includeConstant;
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

        TrainingData trainingData = new TrainingData(data, m_outSpec);

        UpdatingMultipleLinearRegression regr = new MillerUpdatingRegression(
                trainingData.getRegressorCount(), m_includeConstant);

        int rowCount = 0;
        for (TrainingRow row : trainingData) {
            double[] parameter = row.getParameter().getArray()[0];
            regr.addObservation(parameter, row.getTarget());
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
            rowCount, factorList, covariateList, beta, covMat,
            result.getRSquared(), result.getAdjustedRSquared());
        return content;
    }



    /** This class is a decorator for a DataTable.*/
    private static class TrainingData implements Iterable<TrainingRow> {
        private DataTable m_data;
        private List<Integer> m_learningCols;
        private Integer m_target;
        private Map<Integer, Boolean> m_isNominal;
        private Map<Integer, List<DataCell>> m_domainValues;

        private int m_parameterCount;

        /**
         * @param data training data.
         * @param spec port object spec.
         */
        public TrainingData(final DataTable data,
                final PMMLPortObjectSpec spec) {
            m_data = data;
            m_learningCols = new ArrayList<Integer>();
            m_isNominal = new HashMap<Integer, Boolean>();
            m_domainValues = new HashMap<Integer, List<DataCell>>();

            DataTableSpec inSpec = data.getDataTableSpec();
            m_parameterCount = 0;
            for (DataColumnSpec colSpec : spec.getLearningCols()) {
                int i = inSpec.findColumnIndex(colSpec.getName());
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    // Create Design Variables
                    m_learningCols.add(i);
                    m_isNominal.put(i, true);
                    List<DataCell> valueList = new ArrayList<DataCell>();
                    valueList.addAll(colSpec.getDomain().getValues());
                    Collections.sort(valueList,
                            colSpec.getType().getComparator());
                    m_domainValues.put(i, valueList);
                    m_parameterCount += valueList.size() - 1;
                } else {
                    m_learningCols.add(i);
                    m_isNominal.put(i, false);
                    m_domainValues.put(i, null);
                    m_parameterCount++;
                }
            }
            // the target
            DataColumnSpec colSpec = spec.getTargetCols().get(0);
            m_target = inSpec.findColumnIndex(colSpec.getName());
            if (colSpec.getType().isCompatible(NominalValue.class)) {
                // Create Design Variables
                m_isNominal.put(m_target, true);
                List<DataCell> valueList = new ArrayList<DataCell>();
                valueList.addAll(colSpec.getDomain().getValues());
                Collections.sort(valueList,
                        colSpec.getType().getComparator());
                m_domainValues.put(m_target, valueList);
            } else {
                m_isNominal.put(m_target, false);
                m_domainValues.put(m_target, null);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<TrainingRow> iterator() {
            return new TrainingDataIterator(m_data.iterator(), m_target,
                    m_parameterCount, m_learningCols,
                    m_isNominal, m_domainValues);
        }

        /**
         * @return the regressorCount
         */
        public int getRegressorCount() {
            return m_parameterCount;
        }


        /**
         * @return the indices
         */
        public List<Integer> getActiveCols() {
            return m_learningCols;
        }

        /**
         * @return the isDesignVariable
         */
        public Map<Integer, Boolean> getIsNominal() {
            return m_isNominal;
        }

        /**
         * @return the values
         */
        public Map<Integer, List<DataCell>> getDomainValues() {
            return m_domainValues;
        }


    }

    /** This is a decorator for a iterator over DataRows.*/
    private static class TrainingDataIterator implements Iterator<TrainingRow> {
        private Iterator<DataRow> m_iter;

        private int m_target;
        private int m_parameterCount;
        private List<Integer> m_learningCols;
        private Map<Integer, Boolean> m_isNominal;
        private Map<Integer, List<DataCell>> m_domainValues;

        /**
         * @param iter the underlying iterator
         * @param parameterCount number of parameters which will be generated
         * from the learning columns
         * @param learningCols indices of the learning columns
         * @param isNominal whether a learning column is nominal
         * @param domainValues the domain values of the nominal learning columns
         * @param target the index of the target value
         */
        public TrainingDataIterator(final Iterator<DataRow> iter,
                final int target,
                final int parameterCount,
                final List<Integer> learningCols,
                final Map<Integer, Boolean> isNominal,
                final Map<Integer, List<DataCell>> domainValues) {
            m_iter = iter;
            m_target = target;
            m_parameterCount = parameterCount;
            m_learningCols = learningCols;
            m_isNominal = isNominal;
            m_domainValues = domainValues;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_iter.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TrainingRow next() {
            return new TrainingRow(m_iter.next(), m_target,
                    m_parameterCount, m_learningCols,
                    m_isNominal, m_domainValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /** A decorator for a data row. */
    private static class TrainingRow {
        private double m_target;
        private Matrix m_parameter;

        /**
         * @param row The underlying row
         * @param parameterCount number of parameters which will be generated
         * from the learning columns
         * @param learningCols indices of the learning columns
         * @param isNominal whether a learning column is nominal
         * @param domainValues the domain values of the nominal learning columns
         * @param target the index of the target value
         */
        public TrainingRow(final DataRow row,
                final int target,
                final int parameterCount,
                final List<Integer> learningCols,
                final Map<Integer, Boolean> isNominal,
                final Map<Integer, List<DataCell>> domainValues) {
            m_parameter = new Matrix(1, parameterCount);
            int c = 0;
            for (int i : learningCols) {
                if (isNominal.get(i)) {
                    DataCell cell = row.getCell(i);
                    checkMissing(cell);
                    int index = domainValues.get(i).indexOf(cell);
                    if (index < 0) {
                        throw new IllegalStateException("DataCell \""
                        + cell.toString()
                        + "\" is not in the DataColumnDomain. Please apply a "
                        + "Domain Calculator on the columns with nominal "
                        + "values.");
                    }
                    for (int k = 1; k < domainValues.get(i).size(); k++) {
                        if (k == index) {
                            m_parameter.set(0, c, 1.0);
                        } else {
                            m_parameter.set(0, c, 0.0);
                        }
                        c++;
                    }
                } else {
                    DataCell cell = row.getCell(i);
                    checkMissing(cell);
                    DoubleValue value = (DoubleValue)cell;
                    m_parameter.set(0, c, value.getDoubleValue());
                    c++;
                }
            }

            DataCell targetCell = row.getCell(target);
            checkMissing(targetCell);
            if (isNominal.get(target)) {
                m_target = domainValues.get(target).indexOf(targetCell);
                if (m_target < 0) {
                    throw new IllegalStateException("DataCell \""
                    + row.getCell(target).toString()
                    + "\" is not in the DataColumnDomain of target column. "
                    + "Please apply a "
                    + "Domain Calculator on the target column.");
                }
            } else {
                m_target = ((DoubleValue)targetCell).getDoubleValue();
            }
        }


        private void checkMissing(final DataCell cell) {
            if (cell.isMissing()) {
                throw new IllegalStateException("Missing values are not "
                        + "supported by this node.");
            }
        }

        /**
         * The value of the target for this row.
         * @return the value of the target.
         */
        public double getTarget() {
            return m_target;
        }

        /**
         * Returns a {@link Matrix} with values of the parameters retrieved
         * from the learning columns.
         * @return the parameters
         */
        public Matrix getParameter() {
            return m_parameter;
        }
    }
}
