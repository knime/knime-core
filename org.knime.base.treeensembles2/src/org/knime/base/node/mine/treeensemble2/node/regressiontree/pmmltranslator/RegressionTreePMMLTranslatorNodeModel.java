/*
 * ------------------------------------------------------------------------
 *
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
package org.knime.base.node.mine.treeensemble2.node.regressiontree.pmmltranslator;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModel;
import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.pmml.RegressionTreeModelPMMLTranslator;
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
 * @author Adrian Nembach, KNIME.com
 */
public class RegressionTreePMMLTranslatorNodeModel extends NodeModel {

    /**
     */
    protected RegressionTreePMMLTranslatorNodeModel() {
        super(new PortType[]{RegressionTreeModelPortObject.TYPE}, new PortType[]{PMMLPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final RegressionTreeModelPortObject treePO = (RegressionTreeModelPortObject)inObjects[0];
        final RegressionTreeModel model = treePO.getModel();
        final RegressionTreeModelPortObjectSpec treeSpec = treePO.getSpec();
        PMMLPortObjectSpec pmmlSpec = createPMMLSpec(treeSpec, model);
        PMMLPortObject portObject = new PMMLPortObject(pmmlSpec);
        final TreeModelRegression tree = model.getTreeModel();
        final RegressionTreeModelPMMLTranslator translator = new RegressionTreeModelPMMLTranslator(tree, model.getMetaData());
        portObject.addModelTranslater(translator);
        if (translator.hasWarning()) {
            setWarningMessage(translator.getWarning());
        }
        return new PortObject[]{portObject};
    }

    private PMMLPortObjectSpec createPMMLSpec(final RegressionTreeModelPortObjectSpec treeSpec,
        final RegressionTreeModel model) {
        DataColumnSpec targetSpec = treeSpec.getTargetColumn();
        DataTableSpec learnFeatureSpec = treeSpec.getLearnTableSpec();
        if (model == null && containsVector(learnFeatureSpec)) {
            setWarningMessage("The model was learned on a vector column. It's possible to export the model "
                + "to PMML but it won't be possible to import it from the exported PMML.");
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
        RegressionTreeModelPortObjectSpec treeSpec = (RegressionTreeModelPortObjectSpec)inSpecs[0];
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
        // not settings to validate
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
        // no settings to reset
    }

}
