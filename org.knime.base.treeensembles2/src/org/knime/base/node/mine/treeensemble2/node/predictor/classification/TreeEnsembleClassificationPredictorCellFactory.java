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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 10, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.node.predictor.classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.TreeModelClassification;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;
import org.knime.base.node.mine.treeensemble2.node.predictor.TreeEnsemblePredictor;
import org.knime.base.node.mine.treeensemble2.node.predictor.TreeEnsemblePredictorConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class TreeEnsembleClassificationPredictorCellFactory extends AbstractCellFactory {

    private final TreeEnsemblePredictor m_predictor;

    private final DataTableSpec m_learnSpec;

    private final int[] m_learnColumnInRealDataIndices;

    private final Map<String, DataCell> m_targetValueMap;

    private final VotingFactory m_votingFactory;

    private TreeEnsembleClassificationPredictorCellFactory(final TreeEnsemblePredictor predictor,
        final Map<String, DataCell> targetValueMap, final DataColumnSpec[] appendSpecs,
        final int[] learnColumnInRealDataIndices, final VotingFactory votingFactory) {
        super(appendSpecs);
        setParallelProcessing(true);
        m_targetValueMap = targetValueMap;
        m_predictor = predictor;
        m_learnSpec = predictor.getModelSpec().getLearnTableSpec();
        m_learnColumnInRealDataIndices = learnColumnInRealDataIndices;
        m_votingFactory = votingFactory;
    }

    /**
     * Creates a TreeEnsembleClassificationPredictorCellFactory from the provided <b>predictor</b>
     * @param predictor
     * @return an instance of TreeEnsembleClassificationPredictorCellFactory configured according to the settings of the provided
     * <b>predictor<b>
     * @throws InvalidSettingsException
     *  */
    public static TreeEnsembleClassificationPredictorCellFactory createFactory(final TreeEnsemblePredictor predictor)
        throws InvalidSettingsException {
        DataTableSpec testDataSpec = predictor.getDataSpec();
        TreeEnsembleModelPortObjectSpec modelSpec = predictor.getModelSpec();
        TreeEnsembleModelPortObject modelObject = predictor.getModelObject();
        TreeEnsemblePredictorConfiguration configuration = predictor.getConfiguration();
        UniqueNameGenerator nameGen = new UniqueNameGenerator(testDataSpec);
        Map<String, DataCell> targetValueMap = modelSpec.getTargetColumnPossibleValueMap();
        List<DataColumnSpec> newColsList = new ArrayList<DataColumnSpec>();
        DataType targetColType = modelSpec.getTargetColumn().getType();
        String predictionColName = configuration.getPredictionColumnName();
        DataColumnSpec targetCol = nameGen.newColumn(predictionColName, targetColType);
        newColsList.add(targetCol);
        if (configuration.isAppendPredictionConfidence()) {
            newColsList.add(nameGen.newColumn(targetCol.getName() + " (Confidence)", DoubleCell.TYPE));
        }
        if (configuration.isAppendClassConfidences()) {
            final String targetColName = modelSpec.getTargetColumn().getName();
            final String suffix = configuration.getSuffixForClassProbabilities();
            // the model needs to have valid possible values if confidence
            // values should be added (otherwise no output can be set upfront
            // and this class is not called)
            assert targetValueMap != null : "Target column has no possible values";
            for (String v : targetValueMap.keySet()) {
                final String colName = "P(" + targetColName + "=" + v + ")" + suffix;
                newColsList.add(nameGen.newColumn(colName, DoubleCell.TYPE));
            }
        }
        if (configuration.isAppendModelCount()) {
            newColsList.add(nameGen.newColumn("model count", IntCell.TYPE));
        }
        // if the model is non-null (during execute) the target values must be
        // assigned
        assert modelObject == null || targetValueMap != null : "Target values must be known during execution";
        DataColumnSpec[] newCols = newColsList.toArray(new DataColumnSpec[newColsList.size()]);
        int[] learnColumnInRealDataIndices = modelSpec.calculateFilterIndices(testDataSpec);
        final Map<String, Integer> targetValueToIndexMap = new HashMap<String, Integer>(targetValueMap.size());
        Iterator<String> targetValIterator = targetValueMap.keySet().iterator();
        for (int i = 0; i < targetValueMap.size(); i++) {
            targetValueToIndexMap.put(targetValIterator.next(), i);
        }
        final VotingFactory votingFactory;
        if (configuration.isUseSoftVoting()) {
            votingFactory = new SoftVotingFactory(targetValueToIndexMap);
        } else {
            votingFactory = new HardVotingFactory(targetValueToIndexMap);
        }
        return new TreeEnsembleClassificationPredictorCellFactory(predictor, targetValueMap, newCols,
            learnColumnInRealDataIndices, votingFactory);
    }

    /** {@inheritDoc} */
    @Override
    public DataCell[] getCells(final DataRow row) {
        TreeEnsembleModelPortObject modelObject = m_predictor.getModelObject();
        TreeEnsemblePredictorConfiguration cfg = m_predictor.getConfiguration();
        final TreeEnsembleModel ensembleModel = modelObject.getEnsembleModel();
        int size = 1;
        final boolean appendConfidence = cfg.isAppendPredictionConfidence();
        if (appendConfidence) {
            size += 1;
        }
        final boolean appendClassConfidences = cfg.isAppendClassConfidences();
        if (appendClassConfidences) {
            size += m_targetValueMap.size();
        }
        final boolean appendModelCount = cfg.isAppendModelCount();
        if (appendModelCount) {
            size += 1;
        }
        final boolean hasOutOfBagFilter = m_predictor.hasOutOfBagFilter();
        DataCell[] result = new DataCell[size];
        DataRow filterRow = new FilterColumnRow(row, m_learnColumnInRealDataIndices);
        PredictorRecord record = ensembleModel.createPredictorRecord(filterRow, m_learnSpec);
        if (record == null) { // missing value
            Arrays.fill(result, DataType.getMissingCell());
            return result;
        }
        final Voting voting = m_votingFactory.createVoting();
        final int nrModels = ensembleModel.getNrModels();
        int nrValidModels = 0;
        for (int i = 0; i < nrModels; i++) {
            if (hasOutOfBagFilter && m_predictor.isRowPartOfTrainingData(row.getKey(), i)) {
                // ignore, row was used to train the model
            } else {
                TreeModelClassification m = ensembleModel.getTreeModelClassification(i);
                TreeNodeClassification match = m.findMatchingNode(record);
                voting.addVote(match);
                nrValidModels += 1;
            }
        }
        final NominalValueRepresentation[] targetVals = ((TreeTargetNominalColumnMetaData)ensembleModel.getMetaData().getTargetMetaData()).getValues();
        String majorityClass = voting.getMajorityClass();
        int index = 0;
        if (majorityClass == null) {
            assert nrValidModels == 0;
            Arrays.fill(result, DataType.getMissingCell());
            index = size - 1;
        } else {
            result[index++] = m_targetValueMap.get(majorityClass);
//            final float[] distribution = voting.getClassProbabilities();
            if (appendConfidence) {
                result[index++] = new DoubleCell(voting.getClassProbabilityForClass(majorityClass));
            }
            if (appendClassConfidences) {
                for (String targetValue : m_targetValueMap.keySet()) {
                    result[index++] = new DoubleCell(voting.getClassProbabilityForClass(targetValue));
                }
            }
        }
        if (appendModelCount) {
            result[index++] = new IntCell(voting.getNrVotes());
        }
        return result;
    }

}
