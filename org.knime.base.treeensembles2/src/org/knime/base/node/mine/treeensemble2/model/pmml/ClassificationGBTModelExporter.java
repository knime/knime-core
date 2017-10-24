/*
 * ------------------------------------------------------------------------
 *
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
 *   10.10.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.FIELDUSAGETYPE;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.MININGFUNCTION.Enum;
import org.dmg.pmml.MULTIPLEMODELMETHOD;
import org.dmg.pmml.MiningFieldDocument.MiningField;
import org.dmg.pmml.MiningModelDocument.MiningModel;
import org.dmg.pmml.MiningSchemaDocument.MiningSchema;
import org.dmg.pmml.NumericPredictorDocument.NumericPredictor;
import org.dmg.pmml.OPTYPE;
import org.dmg.pmml.OutputDocument.Output;
import org.dmg.pmml.OutputFieldDocument.OutputField;
import org.dmg.pmml.REGRESSIONNORMALIZATIONMETHOD;
import org.dmg.pmml.RESULTFEATURE;
import org.dmg.pmml.RegressionModelDocument.RegressionModel;
import org.dmg.pmml.RegressionTableDocument.RegressionTable;
import org.dmg.pmml.SegmentDocument.Segment;
import org.dmg.pmml.SegmentationDocument.Segmentation;
import org.dmg.pmml.TargetDocument.Target;
import org.dmg.pmml.TargetsDocument.Targets;
import org.knime.base.node.mine.treeensemble2.model.MultiClassGradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;

/**
 * Handles the export of {@link MultiClassGradientBoostedTreesModel}s.
 *
 * @author Adrian Nembach, KNIME
 */
final class ClassificationGBTModelExporter extends AbstractGBTModelExporter<MultiClassGradientBoostedTreesModel> {

    /**
     * @param gbtModel
     */
    public ClassificationGBTModelExporter(final MultiClassGradientBoostedTreesModel gbtModel) {
        super(gbtModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Enum getMiningFunction() {
        return MININGFUNCTION.CLASSIFICATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doWrite(final MiningModel model) {
        Segmentation modelChain = model.addNewSegmentation();
        modelChain.setMultipleModelMethod(MULTIPLEMODELMETHOD.MODEL_CHAIN);
        MultiClassGradientBoostedTreesModel gbt = getGBTModel();
        // write one segment per class
        for (int i = 0; i < gbt.getNrClasses(); i++) {
            addClassSegment(modelChain, i);
        }
        // combine class predictions
        addAggregationSegment(modelChain);
    }

    private void addAggregationSegment(final Segmentation modelChain) {
        Segment seg = modelChain.addNewSegment();
        seg.setId(Integer.toString(getGBTModel().getNrClasses() + 1));
        seg.addNewTrue();
        addSoftmaxRegression(seg);
    }

    private void addSoftmaxRegression(final Segment segment) {
        RegressionModel reg = segment.addNewRegressionModel();
        reg.setFunctionName(MININGFUNCTION.CLASSIFICATION);
        reg.setNormalizationMethod(REGRESSIONNORMALIZATIONMETHOD.SOFTMAX);
        addAggregationMiningScheme(reg);
        addRegressionOutputs(reg);
        addRegressionTables(reg);
    }

    private void addRegressionTables(final RegressionModel regression) {
        for (int i = 0; i < getGBTModel().getNrClasses(); i++) {
            RegressionTable regTable = regression.addNewRegressionTable();
            regTable.setIntercept(0);
            regTable.setTargetCategory(getClassLabel(i));
            NumericPredictor np = regTable.addNewNumericPredictor();
            np.setName(logitName(i));
            np.setCoefficient(1d);
        }
    }

    private void addRegressionOutputs(final RegressionModel regression) {
        Output output = regression.addNewOutput();
        for (int i = 0; i < getGBTModel().getNrClasses(); i++) {
            OutputField p = output.addNewOutputField();
            p.setName(probabilityName(i));
            p.setOptype(OPTYPE.CONTINUOUS);
            p.setDataType(DATATYPE.DOUBLE);
            p.setFeature(RESULTFEATURE.PROBABILITY);
            p.setValue(getClassLabel(i));
        }
    }

    private String getClassLabel(final int classIdx) {
        return getGBTModel().getClassLabel(classIdx);
    }

    private String probabilityName(final int classIdx) {
        return "P (" + getClassLabel(classIdx) + ")";
    }

    private void addAggregationMiningScheme(final RegressionModel regression) {
        MiningSchema miningSchema = regression.addNewMiningSchema();
        // add target field
        MiningField targetField = miningSchema.addNewMiningField();
        targetField.setName(getGBTModel().getMetaData().getTargetMetaData().getAttributeName());
        targetField.setUsageType(FIELDUSAGETYPE.TARGET);
        // add class logits
        for (int i = 0; i < getGBTModel().getNrClasses(); i++) {
            MiningField logit = miningSchema.addNewMiningField();
            logit.setName(logitName(i));
        }
    }

    private String logitName(final int classIdx) {
        return "gbtValue(" + getClassLabel(classIdx) + ")";
    }


    private void addClassSegment(final Segmentation modelChain, final int classIdx) {
        Segment cs = modelChain.addNewSegment();
        cs.setId(Integer.toString(classIdx + 1));
        cs.addNewTrue();
        MiningModel cm = cs.addNewMiningModel();
        cm.setFunctionName(MININGFUNCTION.REGRESSION);
        // write mining schema
        PMMLMiningSchemaTranslator.writeMiningSchema(getPMMLSpec(), cm);
        addOutput(cm, classIdx);
        addTarget(cm);
        addSegmentation(cm, classIdx);
    }


    private void addSegmentation(final MiningModel miningModel, final int c) {
        Segmentation seg = miningModel.addNewSegmentation();
        MultiClassGradientBoostedTreesModel gbt = getGBTModel();
        Collection<TreeModelRegression> trees = IntStream.range(0, gbt.getNrLevels())
                .mapToObj(i -> gbt.getModel(i, c))
                .collect(Collectors.toList());
        Collection<Map<TreeNodeSignature, Double>> coefficientMaps = IntStream.range(0, gbt.getNrLevels())
                .mapToObj(i -> gbt.getCoefficientMap(i, c))
                .collect(Collectors.toList());
        writeSumSegmentation(seg, trees, coefficientMaps);
    }

    private void addTarget(final MiningModel miningModel) {
        Targets targets = miningModel.addNewTargets();
        Target target = targets.addNewTarget();
        target.setRescaleConstant(getGBTModel().getInitialValue());
        target.setField(getGBTModel().getMetaData().getTargetMetaData().getAttributeName());
    }

    private void addOutput(final MiningModel miningModel, final int idx) {
        Output output = miningModel.addNewOutput();
        OutputField f = output.addNewOutputField();
        f.setName(logitName(idx));
        f.setOptype(OPTYPE.CONTINUOUS);
        f.setDataType(DATATYPE.DOUBLE);
        f.setFeature(RESULTFEATURE.PREDICTED_VALUE);
        // in 4.3 isFinalResult should be set to false
    }

}
