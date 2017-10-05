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
 * History
 *   02.10.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.predictor2;

import java.util.ArrayList;
import java.util.Arrays;

import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * This {@link CellFactory} produces the class values for each
 * input {@link DataRow}.
 * <p>Despite being public no official API.
 * @author cebron, University of Konstanz
 */
public final class SVMPredictor implements CellFactory {

    private Svm[] m_svms;

    private int[] m_colindices;

    private final boolean m_appendProbabilities;

    private final String m_predictionColumnName;

    private final String m_suffix;

    private final String m_trainingColumn;

    /**
     * Constructor.
     * @param trainingColumn the name of the training column.
     * @param svms the Support Vector Machine(s) to use.
     * @param colindices the column indices to use in each row.
     * @param predictionColumnName the prediction column name.
     * @param addProbabilities add probabilities or not?
     * @param probabilitySuffix the suffix for the probability columns.
     */
    public SVMPredictor(final String trainingColumn, final Svm[] svms, final int[] colindices, final String predictionColumnName, final boolean addProbabilities, final String probabilitySuffix) {
        m_trainingColumn = trainingColumn;
        m_svms = svms;
        m_colindices = colindices;
        m_predictionColumnName = predictionColumnName;
        m_appendProbabilities = addProbabilities;
        m_suffix = probabilitySuffix;

    }
    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        ArrayList<Double> values = new ArrayList<Double>();
        for (int i = 0; i < m_colindices.length; i++) {
            if (row.getCell(m_colindices[i]).isMissing()) {
                if (m_appendProbabilities) {
                    DataCell[] ret = new DataCell[1 + m_svms.length];
                    Arrays.fill(ret, new MissingCell("Missing value in input data."));
                    return ret;
                }
                return new DataCell[]{DataType.getMissingCell()};
            }
            DoubleValue dv = (DoubleValue) row.getCell(m_colindices[i]);
            values.add(dv.getDoubleValue());
        }
        String classvalue = doPredict(values);
        if (m_appendProbabilities) {
            DataCell[] ret = new DataCell[m_svms.length + 1];
            double[] probabilities = computeProbabilities(values);
            assert ret.length == probabilities.length + 1: ret.length + " vs. " + (probabilities.length + 1);
            for (int i = ret.length - 1; i-->0;) {
                ret[i] = new DoubleCell(probabilities[i]);
            }
            ret[probabilities.length] = new StringCell(classvalue);
            return ret;
        }
        return new DataCell[]{new StringCell(classvalue)};
    }

    /**
     * @param values
     * @return
     */
    private double[] computeProbabilities(final ArrayList<Double> values) {
        //Based on Platt: Probabilistic Outputs for SVMs and...
        //and Wu, Lin, Weng: Probability Estimates for Multi-class ...
        // Price et al.: Pairwise neural network classifiers ...
        double[] f = new double[m_svms.length];
        for (int i = m_svms.length; i-->0;) {
            f[i] = m_svms[i].distance(new DoubleVector(values, Integer.toString(i)));
        }
        //TODO implement when the model contains the proper statistics
//        double[][] pairwise = new double[m_svms.length][m_svms.length];
//        for (int i = m_svms.length; i-->0;) {
//            for (int j = m_svms.length; j-->0;) {
//                pairwise[i][j] =
//            }
//        }
        //Using simple logistic link function, proposed by
        //G. Wahba: Multivariate function and operator estimation, ...
        //G. Wahba: Support vector machines, reproducing kernel hilbert spaces...
        double[] p = new double[m_svms.length];
        double sum = 0;
        for (int i = p.length; i-->0;) {
            p[i] = 1 / (1+ Math.exp(-f[i]));
            sum += p[i];
        }
        for (int i = 0; i < p.length; i++) {
            p[i] /= sum;
        }
        return p;
    }
    /**
     * Given a vector, find out it's class.
     *
     * @param values the parameters.
     */
    private String doPredict(final ArrayList<Double> values) {
        DoubleVector vector = new DoubleVector(values, "not_known_yet");
        int pos = 0;
        double bestDistance = m_svms[0].distance(vector);
        for (int i = 1; i < m_svms.length; ++i) {
            double newDist = m_svms[i].distance(vector);
            if (newDist > bestDistance) {
                pos = i;
                bestDistance = newDist;
            }
        }
        return m_svms[pos].getPositive();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        DataColumnSpecCreator colspeccreator =
                new DataColumnSpecCreator(m_predictionColumnName, StringCell.TYPE);
        if (m_appendProbabilities) {
            final DataColumnSpec[] ret = new DataColumnSpec[m_svms.length + 1];
            PredictorHelper ph = PredictorHelper.getInstance();
            final DataColumnSpecCreator creator = new DataColumnSpecCreator("Dummy", DoubleCell.TYPE);
            creator.setDomain(new DataColumnDomainCreator(new DoubleCell(0), new DoubleCell(1)).createDomain());
            for (int i = m_svms.length; i-->0;) {
                String name = ph.probabilityColumnName(m_trainingColumn, m_svms[i].getPositive(), m_suffix);
                creator.setName(name);
                ret[i] = creator.createSpec();
            }
            ret[m_svms.length] = colspeccreator.createSpec();
            return ret;
        }
        return new DataColumnSpec[]{colspeccreator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount, "Classifying");
    }

}
