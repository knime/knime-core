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
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.base.node.mine.treeensemble2.node.shrinker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.xmlbeans.XmlException;
import org.dmg.pmml.PMMLDocument;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
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
import org.knime.ensembles.pmml.ModelMismatchException;
import org.knime.ensembles.pmml.combine.PMMLEnsembleUtilities;

/**
 * @author Patrick Winter, University of Konstanz
 */
class TreeEnsembleShrinkerNodeModel extends NodeModel {

    private TreeEnsembleShrinkerNodeConfig m_config = new TreeEnsembleShrinkerNodeConfig();

    protected TreeEnsembleShrinkerNodeModel() {
        super(new PortType[]{TreeEnsembleModelPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{PMMLPortObject.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        TreeEnsembleModel treeEnsemble = ((TreeEnsembleModelPortObject)inObjects[0]).getEnsembleModel();
        TreeEnsembleModelPortObject resultEnsemble;
        int resultSize = m_config.getResultSize(treeEnsemble.getNrModels());
        boolean shrink = true;
        if (!m_config.isResultSizeAutomatic()) {
            // Check if result size is valid
            if (resultSize < 1) {
                // Result size is to small, use 1
                setWarningMessage("The configured result size is smaller than 1, defaulting to 1");
                resultSize = 1;
            } else if (resultSize > treeEnsemble.getNrModels()) {
                // Result size is to big, just keep current ensemble
                setWarningMessage(
                    "The configured result size is bigger than the size of the input ensemble, defaulting to the input ensembles size");
                shrink = false;
            } else if (resultSize == treeEnsemble.getNrModels()) {
                // Result size is ensemble size -> we don't need to shrink
                shrink = false;
            }
        }
        // If our result size is not smaller than the current ensemble we don't have to do the following and therefore can save time
        if (shrink) {
            BufferedDataTable inData = (BufferedDataTable)inObjects[1];
            // Create shrinker
            TreeEnsembleShrinker shrinker =
                new TreeEnsembleShrinker(treeEnsemble, inData, m_config.getTargetColumn(), exec);
            // Shrink ensemble
            if (m_config.isResultSizeAutomatic()) {
                shrinker.autoShrink();
            } else {
                shrinker.shrinkTo(resultSize);
            }
            // Get shrunk ensemble
            TreeEnsembleModel newEnsemble = shrinker.getModel();
            // Push flow variable with archived accuracy
            pushFlowVariableDouble("Tree Ensemble Shrinker Prediction Accuracy", shrinker.getAccuracy());
            // Create port object for tree ensemble
            resultEnsemble =
                TreeEnsembleModelPortObject.createPortObject(((TreeEnsembleModelPortObject)inObjects[0]).getSpec(),
                    newEnsemble, exec.createFileStore(UUID.randomUUID().toString()));
        } else {
            // We did not need to shrink just use input tree ensemble port object
            resultEnsemble = (TreeEnsembleModelPortObject)inObjects[0];
        }
        // Convert tree ensemble port object to PMML
        PMMLPortObject pmmlEnsemble = convertToPmmlEnsemble(resultEnsemble, exec);
        return new PortObject[]{pmmlEnsemble};
    }

    /**
     * Converts the given TreeEnsembleModelPortObject to a PMMLPortObject.
     *
     * @param treeEnsemble The tree ensemble to convert
     * @param exec The execution context
     * @return PMML representation of the tree ensemble
     * @throws XmlException If something went wrong
     * @throws CanceledExecutionException If the operation got canceled
     * @throws ModelMismatchException If something went wrong
     */
    private PMMLPortObject convertToPmmlEnsemble(final TreeEnsembleModelPortObject treeEnsemble,
        final ExecutionContext exec) throws XmlException, CanceledExecutionException, ModelMismatchException {
        // This code is based on org.knime.ensembles.pmml.combine.PMMLEnsembleNodeModel.execute()
        List<PMMLDocument> documents = new ArrayList<PMMLDocument>();
        for (int i = 0; i < treeEnsemble.getEnsembleModel().getNrModels(); i++) {
            documents.add(PMMLDocument.Factory
                .parse(treeEnsemble.createDecisionTreePMMLPortObject(i).getPMMLValue().getDocument()));
        }
        return PMMLEnsembleUtilities.convertToPmmlEnsemble(documents, null,
            org.dmg.pmml.MULTIPLEMODELMETHOD.MAJORITY_VOTE, exec);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        TreeEnsembleModelPortObjectSpec modelSpec = (TreeEnsembleModelPortObjectSpec)inSpecs[0];
        modelSpec.assertTargetTypeMatches(false);
        DataTableSpec tableSpec = (DataTableSpec)inSpecs[1];
        int targetColumnIndex = tableSpec.findColumnIndex(m_config.getTargetColumn());
        if (targetColumnIndex < 0
            || !tableSpec.getColumnSpec(targetColumnIndex).getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("No valid target column selected");
        }
        return new PortObjectSpec[]{null};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.save(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        TreeEnsembleShrinkerNodeConfig config = new TreeEnsembleShrinkerNodeConfig();
        config.load(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        TreeEnsembleShrinkerNodeConfig config = new TreeEnsembleShrinkerNodeConfig();
        config.load(settings);
        m_config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

}
