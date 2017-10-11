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
 *   09.05.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.pmml.exporter;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostingModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.MultiClassGradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.pmml.AbstractGBTModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.model.pmml.ClassificationGBTModelPMMLTranslator;
import org.knime.base.node.mine.treeensemble2.model.pmml.RegressionGBTModelPMMLTranslator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 *
 * @author Adrian Nembach, KNIME
 */
class GBTPMMLExporterNodeModel extends NodeModel {

    /**
     */
    protected GBTPMMLExporterNodeModel() {
        super(new PortType[]{GradientBoostingModelPortObject.TYPE}, new PortType[]{PMMLPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        GradientBoostingModelPortObject gbtPO = (GradientBoostingModelPortObject)inObjects[0];
        AbstractGBTModelPMMLTranslator<?> translator;
        AbstractGradientBoostingModel gbtModel = gbtPO.getEnsembleModel();
        if (gbtModel instanceof GradientBoostedTreesModel) {
            translator = new RegressionGBTModelPMMLTranslator((GradientBoostedTreesModel)gbtModel);
        } else if (gbtModel instanceof MultiClassGradientBoostedTreesModel){
            translator = new ClassificationGBTModelPMMLTranslator((MultiClassGradientBoostedTreesModel)gbtModel);
        } else {
            throw new IllegalArgumentException("Unknown gradient boosted trees model type '" +
                    gbtModel.getClass().getSimpleName() + "'.");
        }
        PMMLPortObjectSpec pmmlSpec = createPMMLSpec(gbtPO.getSpec(), gbtModel);
        PMMLPortObject pmmlPO = new PMMLPortObject(pmmlSpec);
        pmmlPO.addModelTranslater(translator);
        return new PortObject[]{pmmlPO};
    }

    private PMMLPortObjectSpec createPMMLSpec(final TreeEnsembleModelPortObjectSpec spec,
        final AbstractGradientBoostingModel model) {
        DataColumnSpec targetSpec = spec.getTargetColumn();
        DataTableSpec learnFeatureSpec = spec.getLearnTableSpec();
        if (containsVector(learnFeatureSpec)) {
            setWarningMessage("The model was learned on a vector column. It's possible to export the model "
                    + "to PMML but it won't be possible to import it from the exported PMML.");
        }
        if (model == null && containsVector(learnFeatureSpec)) {
        	// at this point we don't know how long the vector column is
            return null;
        } else if (model != null) {
            // possibly expand vectors with model
            learnFeatureSpec = model.getLearnAttributeSpec(learnFeatureSpec);
        }
        DataTableSpec completeLearnSpec = new DataTableSpec(learnFeatureSpec, new DataTableSpec(targetSpec));
        PMMLPortObjectSpecCreator pmmlSpecCreator =
                new PMMLPortObjectSpecCreator(completeLearnSpec);
        try {
            pmmlSpecCreator.setLearningCols(learnFeatureSpec);
        } catch (InvalidSettingsException e) {
            // this exception is not actually thrown in the code
            // (as of KNIME v2.5.1)
            throw new IllegalStateException(e);
        }
        pmmlSpecCreator.setTargetCol(targetSpec);
        return pmmlSpecCreator.createSpec();
    }

    private static boolean containsVector(final DataTableSpec learnFeatureSpec) {
        for (DataColumnSpec colSpec : learnFeatureSpec) {
            DataType type = colSpec.getType();
            boolean isVector = type.isCompatible(BitVectorValue.class) ||
                    type.isCompatible(DoubleVectorValue.class) ||
                    type.isCompatible(ByteVectorValue.class);
            if (isVector) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        TreeEnsembleModelPortObjectSpec treeSpec = (TreeEnsembleModelPortObjectSpec)inSpecs[0];
        return new PortObjectSpec[] {createPMMLSpec(treeSpec, null)};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals to load

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals to save

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // no settings to save

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings to validate

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings to load

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset

    }

}
