/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.02.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.node.mine.regression.RegressionTrainingData;
import org.knime.base.node.mine.regression.RegressionTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.ClassificationTrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.MaxPathStrategy;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.MultinomialRegression;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.MultinomialRegression.ProblemFormulation;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.PathStrategy;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Logistic regression learner that is based on the glmnet algorithm.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class GlmNetLogRegLearner implements LogRegLearner {

    private final PMMLPortObjectSpec m_pmmlSpec;
    private final DataTableSpec m_tableSpec;
    private final List<DataColumnSpec> m_specialColumns;
    private final DataCell m_targetReferenceCategory;
    private final boolean m_sortTargetCategories;
    private final boolean m_sortFactorsCategories;

    /**
     *
     */
    public GlmNetLogRegLearner(final PMMLPortObjectSpec spec,
    final DataTableSpec tableSpec,
    final List<DataColumnSpec> specialColumns, final DataCell targetReferenceCategory,
    final boolean sortTargetCategories,
    final boolean sortFactorsCategories) {
        m_pmmlSpec = spec;
        m_tableSpec = tableSpec;
        m_specialColumns = specialColumns;
        m_targetReferenceCategory = targetReferenceCategory;
        m_sortTargetCategories = sortTargetCategories;
        m_sortFactorsCategories = sortFactorsCategories;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogisticRegressionContent learn(final RegressionTrainingData data, final ExecutionMonitor progressMonitor)
        throws CanceledExecutionException, InvalidSettingsException {
        ClassData classData = new ClassData(data);
        double alpha = 0.8;
        double lambda = 0.2;
        PathStrategy pathStrategy = /*new SingleLambdaPathStrategy(0.2)*/new MaxPathStrategy(lambda, alpha, 100, classData);
        MultinomialRegression learner = new MultinomialRegression(ProblemFormulation.Conventional, pathStrategy, alpha);
        double[][] beta = learner.fit(classData);

        List<String> factorList = new ArrayList<String>();
        List<String> covariateList = new ArrayList<String>();
        Map<String, List<DataCell>> factorDomainValues =
            new HashMap<String, List<DataCell>>();
        for (int i : data.getActiveCols()) {
            DataColumnSpec columnSpec = m_tableSpec.getColumnSpec(i);
            if (data.getIsNominal().get(i)) {
                String factor =
                    columnSpec.getName();
                factorList.add(factor);
                List<DataCell> values = data.getDomainValues().get(i);
                factorDomainValues.put(factor, values);
            } else {
                if (columnSpec.getType().isCompatible(BitVectorValue.class) || columnSpec.getType().isCompatible(ByteVectorValue.class) ) {
                    int length = data.getVectorLengths().getOrDefault(i, 0).intValue();
                    for (int j = 0; j < length; ++j) {
                        covariateList.add(columnSpec.getName() + "[" + j + "]");
                    }
                } else {
                    covariateList.add(
                        columnSpec.getName());
                }
            }
        }

        final Map<? extends Integer, Integer> vectorIndexLengths = data.getVectorLengths();
        final Map<String, Integer> vectorLengths = new LinkedHashMap<String, Integer>();
        for (DataColumnSpec spec: m_specialColumns) {
            int colIndex = m_tableSpec.findColumnIndex(spec.getName());
            if (colIndex >= 0) {
                vectorLengths.put(spec.getName(), vectorIndexLengths.get(colIndex));
            }
        }
        int cols = beta[0].length;
        RealMatrix betaMat = MatrixUtils.createRealMatrix(1, beta.length * cols);
        for (int i = 0; i < beta.length; i++) {
            for (int j = 0; j < beta[i].length; j++) {
                betaMat.setEntry(0, i * cols + j, beta[i][j]);
            }
        }
        RealMatrix covMat = MatrixUtils.createRealMatrix(classData.getFeatureCount() + 1 , classData.getFeatureCount() + 1);
        // create content
        LogisticRegressionContent content =
            new LogisticRegressionContent(m_pmmlSpec,
                    factorList, covariateList, vectorLengths,
                    m_targetReferenceCategory, m_sortTargetCategories, m_sortFactorsCategories,
                    betaMat, 0, covMat, 0);
        return content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWarningMessage() {
        return "";
    }

    private class ClassData implements ClassificationTrainingData {

        private final List<ClassificationTrainingRow> m_rows;
        private final int m_catCount;
        private final int m_fetCount;
        private int m_iteratorCalls;

        public ClassData(final RegressionTrainingData data) {
            int nrows = (int)data.getRowCount();
            int nfet = data.getRegressorCount();
            m_fetCount = nfet;
            m_rows = new ArrayList<ClassificationTrainingRow>(nrows);
            for (RegressionTrainingRow row : data) {
                m_rows.add(new ClassDataRow(row));
            }
            m_catCount = data.getDomainValues().get(data.getTargetIndex()).size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return m_rows.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getFeatureCount() {
            return m_fetCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<ClassificationTrainingRow> iterator() {
//            return new ClassDataRowIterator(m_data.iterator());
//            System.out.println("Iterator called " + (++m_iteratorCalls));
            return m_rows.iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCategoryCount() {
            return m_catCount;
        }

    }

    private static class ClassDataRow implements ClassificationTrainingRow {

        private final double[] m_data;
        private final int m_cat;

        public ClassDataRow(final RegressionTrainingRow row) {
            m_data = row.getParameter().getRow(0);
            m_cat = (int)row.getTarget();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public double getFeature(final int idx) {
            return idx == 0.0 ? 1.0 : m_data[idx - 1];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCategory() {
            return m_cat;
        }

    }

//    private class ClassDataRowIterator implements Iterator<ClassificationTrainingRow> {
//
//        private final Iterator<RegressionTrainingRow> m_iter;
//
//        ClassDataRowIterator(final Iterator<RegressionTrainingRow> iter) {
//            m_iter = iter;
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public boolean hasNext() {
//            return m_iter.hasNext();
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public ClassDataRow next() {
//            return new ClassDataRow(m_iter.next());
//        }
//
//    }
//
//    private class ClassDataRow implements ClassificationTrainingRow {
//
//        final RegressionTrainingRow m_row;
//
//        ClassDataRow(final RegressionTrainingRow row) {
//            m_row = row;
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public double getFeature(final int idx) {
//            return m_row.getParameterApache().getEntry(0, idx);
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public int getCategory() {
//            return (int)m_row.getTarget();
//        }
//
//    }

}
