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
 *   12.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner.gradientboosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataIndexManager;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
import org.knime.core.data.RowKey;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

import com.google.common.primitives.Doubles;

/**
 *
 * @author Adrian Nembach
 */
public abstract class AbstractGradientBoostingLearner {

    private final TreeData m_data;

    private final DataIndexManager m_indexManager;

    private final GradientBoostingLearnerConfiguration m_config;

    public AbstractGradientBoostingLearner(final GradientBoostingLearnerConfiguration config, final TreeData data) {
        m_data = data;
        m_indexManager = new DataIndexManager(data);
        m_config = config;
    }

    public TreeData getData() {
        return m_data;
    }

    public TreeTargetNumericColumnData getTarget() {
        return (TreeTargetNumericColumnData)m_data.getTargetColumn();
    }

    public DataIndexManager getIndexManager() {
        return m_indexManager;
    }

    public GradientBoostingLearnerConfiguration getConfig() {
        return m_config;
    }

    public abstract AbstractGradientBoostingModel learn(final ExecutionMonitor exec) throws CanceledExecutionException, InterruptedException, ExecutionException;

    protected static void adaptPredictionPrev(final double[] predictionPrev, final double[] predictionNewModel,
        final double coefficient) {
        for (int i = 0; i < predictionPrev.length; i++) {
            predictionPrev[i] += coefficient * predictionNewModel[i];
        }
    }

    protected double[] calculateMeanPrediction(final TreeTargetNumericColumnData target) {
        int numRows = target.getNrRows();
        final double[] mean = new double[numRows];
        double meanVal = 0;
        for (int i = 0; i < numRows; i++) {
            meanVal += target.getValueFor(i);
        }
        meanVal /= numRows;
        for (int i = 0; i < numRows; i++) {
            mean[i] = meanVal;
        }
        return mean;
    }

    protected TreeData calculateResidualData(final double[] previousPrediction, final LossFunction lossFunction) {
        TreeTargetNumericColumnData actual = (TreeTargetNumericColumnData)getData().getTargetColumn();
        double[] residualData = new double[actual.getNrRows()];
        assert residualData.length == previousPrediction.length;
        RowKey[] rowKeysAsArray = new RowKey[actual.getNrRows()];
        for (int i = 0; i < residualData.length; i++) {
            double actualVal = actual.getValueFor(i);
            residualData[i] = -lossFunction.calculateGradient(actualVal, previousPrediction[i]);
//            assert roughlyEqual(previousPrediction[i] + residualData[i], actualVal, 0.00001);
            rowKeysAsArray[i] = actual.getRowKeyFor(i);
        }
        TreeTargetNumericColumnMetaData metaData = actual.getMetaData();
        TreeTargetNumericColumnData residualTarget =
            new TreeTargetNumericColumnData(metaData, rowKeysAsArray, residualData);
        return new TreeData(getData().getColumns(), residualTarget, getData().getTreeType());
    }

    protected TreeData createResidualDataFromArray(final double[] residualData, final TreeData actualData) {
        TreeTargetNumericColumnData actual = (TreeTargetNumericColumnData)actualData.getTargetColumn();
        RowKey[] rowKeysAsArray = new RowKey[actual.getNrRows()];
        for (int i = 0; i < rowKeysAsArray.length; i++) {
            rowKeysAsArray[i] = actual.getRowKeyFor(i);
        }
        TreeTargetNumericColumnMetaData metaData = actual.getMetaData();
        TreeTargetNumericColumnData residualTarget =
            new TreeTargetNumericColumnData(metaData, rowKeysAsArray, residualData);
        return new TreeData(getData().getColumns(), residualTarget, getData().getTreeType());
    }

    private boolean roughlyEqual(final double val1, final double val2, final double epsilon) {
        return val1 - val2 < epsilon;
    }

    /**
     * Creates a PredictorRecord from the inMemory TreeData object
     *
     * @param data
     * @param indexManager
     * @param rowIdx
     * @return a PredictorRecord for the row at <b>rowIdx</b> in <b>data</b>
     */
    public static PredictorRecord createPredictorRecord(final TreeData data, final DataIndexManager indexManager,
        final int rowIdx) {
        Map<String, Object> valMap = new HashMap<String, Object>();
        for (TreeAttributeColumnData column : data.getColumns()) {
            TreeAttributeColumnMetaData meta = column.getMetaData();
            valMap.put(meta.getAttributeName(), column.getValueAt(indexManager.getIndicesInColumn(meta.getAttributeIndex())[rowIdx]));
        }
        return new PredictorRecord(valMap);
    }


    protected double[] predictTreeModel(final TreeModelRegression tree) {
        final double[] prediction = new double[m_data.getNrRows()];
        for (int i = 0; i < prediction.length; i++) {
            prediction[i] = tree.findMatchingNode(createPredictorRecord(m_data, m_indexManager, i)).getMean();
        }
        return prediction;
    }

    protected static double calcMedian(final double[] values) {
        if (values.length == 1) {
            return values[0];
        }
        Integer[] idx = new Integer[values.length];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = i;
        }
        Comparator<Integer> idxComp = new Comparator<Integer>() {
            @Override
            public int compare(final Integer arg0, final Integer arg1) {
                return Doubles.compare(values[arg0], values[arg1]);
            }
        };
        Arrays.sort(idx, idxComp);
        int medianIndex = values.length / 2;
        if (values.length % 2 == 0) {
            return (values[idx[medianIndex - 1]] + values[idx[medianIndex]]) / 2;
        }
        return values[idx[medianIndex]];
    }

    // The leafs could be also determined during tree construction with minimal additional effort
    protected static TreeNodeRegression[] getLeafs(final TreeModelRegression tree) {
        LinkedList<TreeNodeRegression> toProcess = new LinkedList<TreeNodeRegression>();
        toProcess.add(tree.getRootNode());
        ArrayList<TreeNodeRegression> leafs = new ArrayList<TreeNodeRegression>();
        while (!toProcess.isEmpty()) {
            TreeNodeRegression node = toProcess.poll();
            int nrChildren = node.getNrChildren();
            if (nrChildren == 0) {
                leafs.add(node);
            } else {
                for (int i = 0; i < nrChildren; i++) {
                    toProcess.add(node.getChild(i));
                }
            }
        }
        return leafs.toArray(new TreeNodeRegression[leafs.size()]);
    }

   }
