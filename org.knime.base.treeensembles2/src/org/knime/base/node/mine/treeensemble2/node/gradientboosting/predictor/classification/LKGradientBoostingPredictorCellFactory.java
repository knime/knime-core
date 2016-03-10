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
 *   14.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor.classification;

import java.util.ArrayList;
import java.util.Arrays;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostingModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.MultiClassGradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor.GradientBoostingPredictor;
import org.knime.base.node.mine.treeensemble2.node.predictor.TreeEnsemblePredictorConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;

/**
 *
 * @author Adrian Nembach
 */
public class LKGradientBoostingPredictorCellFactory extends AbstractCellFactory {

    private final GradientBoostingModelPortObject m_modelPO;

    private final DataTableSpec m_learnSpec;

    private final int[] m_learnColumnInRealDataIndices;

    private final TreeEnsemblePredictorConfiguration m_config;

    /**
     * @param newColSpec
     */
    private LKGradientBoostingPredictorCellFactory(final DataColumnSpec[] newColSpecs,
        final GradientBoostingModelPortObject modelPO, final DataTableSpec learnSpec,
        final int[] learnColumnInRealDataIndices, final TreeEnsemblePredictorConfiguration config) {
        super(newColSpecs);
        m_modelPO = modelPO;
        m_learnSpec = learnSpec;
        m_learnColumnInRealDataIndices = learnColumnInRealDataIndices;
        m_config = config;
        setParallelProcessing(true);
    }

    public static LKGradientBoostingPredictorCellFactory createFactory(final GradientBoostingPredictor predictor) throws InvalidSettingsException {
        TreeEnsemblePredictorConfiguration config = predictor.getConfig();
        DataTableSpec testSpec = predictor.getDataSpec();
        TreeEnsembleModelPortObjectSpec modelSpec = predictor.getModelSpec();
        ArrayList<DataColumnSpec> newColSpecs = new ArrayList<DataColumnSpec>();
        UniqueNameGenerator nameGen = new UniqueNameGenerator(testSpec);
        newColSpecs.add(nameGen.newColumn(config.getPredictionColumnName(), StringCell.TYPE));
        if (config.isAppendPredictionConfidence()) {
            newColSpecs.add(nameGen.newColumn("Confidence", DoubleCell.TYPE));
        }
        if (config.isAppendClassConfidences()) {
            final String targetColName = modelSpec.getTargetColumn().getName();
            final String suffix = config.getSuffixForClassProbabilities();
            for (String val : modelSpec.getTargetColumnPossibleValueMap().keySet()) {
                String colName = "P(" + targetColName + "=" + val + ")" + suffix;
                newColSpecs.add(nameGen.newColumn(colName, DoubleCell.TYPE));
            }
        }

        return new LKGradientBoostingPredictorCellFactory(newColSpecs.toArray(new DataColumnSpec[newColSpecs.size()]),
            predictor.getModelPO(), modelSpec.getLearnTableSpec(), modelSpec.calculateFilterIndices(testSpec), config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        MultiClassGradientBoostedTreesModel model = (MultiClassGradientBoostedTreesModel)m_modelPO.getEnsembleModel();
        DataRow filterRow = new FilterColumnRow(row, m_learnColumnInRealDataIndices);
        int nrClasses = model.getNrClasses();
        int nrLevels = model.getNrLevels();
        PredictorRecord record = model.createPredictorRecord(filterRow, m_learnSpec);
        double[] classFunctionPredictions = new double[nrClasses];
        Arrays.fill(classFunctionPredictions, model.getInitialValue());
        for (int i = 0; i < nrLevels; i++) {
            for (int j = 0; j < nrClasses; j++) {
                classFunctionPredictions[j] +=
                    model.getCoefficientMap(i, j).get(model.getModel(i, j).findMatchingNode(record).getSignature());
            }
        }
        double[] classProbabilities = new double[nrClasses];
        double expSum = 0;
        for (int i = 0; i < nrClasses; i++) {
            classProbabilities[i] = Math.exp(classFunctionPredictions[i]);
            expSum += classProbabilities[i];
        }
        int classIdx = -1;
        double classProb = -1;
        for (int i = 0; i < nrClasses; i++) {
            classProbabilities[i] /=  expSum;
            if (classProbabilities[i] > classProb) {
                classIdx = i;
                classProb = classProbabilities[i];
            }
        }

        ArrayList<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(model.getClassLabel(classIdx)));

        if (m_config.isAppendPredictionConfidence()) {
            cells.add(new DoubleCell(classProb));
        }
        if (m_config.isAppendClassConfidences()) {
            for (int i = 0; i < classProbabilities.length; i++) {
                cells.add(new DoubleCell(classProbabilities[i]));
            }
        }

        return cells.toArray(new DataCell[cells.size()]);
    }

}
