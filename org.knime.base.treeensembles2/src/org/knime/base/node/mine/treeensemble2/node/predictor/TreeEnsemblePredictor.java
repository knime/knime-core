/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 18, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import java.util.HashMap;
import java.util.Map;

import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnData;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.node.predictor.classification.TreeEnsembleClassificationPredictorCellFactory;
import org.knime.base.node.mine.treeensemble2.node.predictor.regression.TreeEnsembleRegressionPredictorCellFactory;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeEnsemblePredictor {

    private final TreeEnsembleModelPortObjectSpec m_modelSpec;

    private final TreeEnsembleModelPortObject m_modelObject;

    private final DataTableSpec m_dataSpec;

    private final TreeEnsemblePredictorConfiguration m_configuration;

    private final ColumnRearranger m_predictionRearranger;

    private RowSample[] m_modelLearnRowSamples;

    private Map<RowKey, Integer> m_rowKeyToLearnIndex;

    /**
     * @param modelSpec
     * @param modelObject
     * @param dataSpec
     * @param configuration
     * @throws InvalidSettingsException
     */
    public TreeEnsemblePredictor(final TreeEnsembleModelPortObjectSpec modelSpec,
                                 final TreeEnsembleModelPortObject modelObject, final DataTableSpec dataSpec,
                                 final TreeEnsemblePredictorConfiguration configuration)
    throws InvalidSettingsException {
        m_modelSpec = modelSpec;
        m_modelObject = modelObject;
        m_dataSpec = dataSpec;
        m_configuration = configuration;
        boolean hasPossibleValues = modelSpec.getTargetColumnPossibleValueMap() != null;
        if (modelSpec.getTargetColumn().getType().isCompatible(DoubleValue.class)) {
            m_predictionRearranger = new ColumnRearranger(dataSpec);
            m_predictionRearranger.append(TreeEnsembleRegressionPredictorCellFactory.createFactory(this));
        } else if (configuration.isAppendClassConfidences() && !hasPossibleValues) {
            // can't add confidence columns (possible values unknown)
            m_predictionRearranger = null;
        } else {
            m_predictionRearranger = new ColumnRearranger(dataSpec);
            TreeEnsembleClassificationPredictorCellFactory cellFac =
                    TreeEnsembleClassificationPredictorCellFactory.createFactory(this);
            m_predictionRearranger.append(cellFac);
        }
    }

    /**
     * Sets the out of bag filter. The arguments must not be null.
     *
     * @param modelRowSamples
     * @param targetColumnData
     */
    public void setOutofBagFilter(final RowSample[] modelRowSamples, final TreeTargetColumnData targetColumnData) {
        if (modelRowSamples == null || targetColumnData == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        final int nrRows = targetColumnData.getNrRows();
        Map<RowKey, Integer> learnItemMap = new HashMap<RowKey, Integer>((int)(nrRows / 0.75 + 1));
        for (int i = 0; i < nrRows; i++) {
            RowKey key = targetColumnData.getRowKeyFor(i);
            learnItemMap.put(key, i);
        }
        m_modelLearnRowSamples = modelRowSamples;
        m_rowKeyToLearnIndex = learnItemMap;
    }

    /**
     * @return the rearranger for the appended columns or null if the out spec can't be determined (during configure with
     *         an unknown number of targets).
     */
    public ColumnRearranger getPredictionRearranger() {
        return m_predictionRearranger;
    }

    /** @return the configuration */
    public TreeEnsemblePredictorConfiguration getConfiguration() {
        return m_configuration;
    }

    /** @return the data */
    public DataTableSpec getDataSpec() {
        return m_dataSpec;
    }

    /** @return the modelObject */
    public TreeEnsembleModelPortObject getModelObject() {
        return m_modelObject;
    }

    /** @return the modelSpec */
    public TreeEnsembleModelPortObjectSpec getModelSpec() {
        return m_modelSpec;
    }

    /**
     * @return true if <b>this<b> has an out of bag filter
     */
    public boolean hasOutOfBagFilter() {
        return m_modelLearnRowSamples != null;
    }

    /**
     * @param key
     * @param modelIndex
     * @return true if the row with rowkey <b>key</b> in model with index <b>modelIndex</b>
     * is part of the training data for this model
     */
    public boolean isRowPartOfTrainingData(final RowKey key, final int modelIndex) {
        assert m_modelLearnRowSamples != null : "no out of bag filter set";
        Integer indexInteger = m_rowKeyToLearnIndex.get(key);
        if (indexInteger == null) {
            return false;
        }
        int index = indexInteger;
        return m_modelLearnRowSamples[modelIndex].getCountFor(index) > 0;
    }

}
