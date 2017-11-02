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
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.knime.core.node.BufferedDataContainer;
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
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.LoopEndNode;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FeatureSelectionLoopEndNodeModel extends NodeModel implements LoopEndNode {

    private FeatureSelector m_featureSelector;

    private FeatureSelectionLoopEndSettings m_settings = new FeatureSelectionLoopEndSettings();

    private int m_iteration = 0;

    private BufferedDataContainer m_resultTable;

    /**
     * Constructor
     */
    public FeatureSelectionLoopEndNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE, FeatureSelectionModel.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final String scoreVariableName = m_settings.getScoreVariableName();
        final Map<String, FlowVariable> fvs = getAvailableInputFlowVariables();
        boolean compatibleFVExists = false;
        for (final FlowVariable fv : fvs.values()) {
            if (fv.getType() == FlowVariable.Type.DOUBLE) {
                compatibleFVExists = true;
            }
        }
        if (!compatibleFVExists) {
            throw new InvalidSettingsException("There is no compatible flow variable (Double) at the inport.");
        }
        if (!fvs.containsKey(scoreVariableName)) {
            throw new InvalidSettingsException("The defined score variable \"" + scoreVariableName
                + "\" is not contained in the input flow variables.");
        }
        final FlowVariable scoreVariable = getAvailableInputFlowVariables().get(scoreVariableName);
        if (scoreVariable.getType() != FlowVariable.Type.DOUBLE) {
            throw new InvalidSettingsException("The score variable must be of type Double.");
        }

        // first configure
        if (m_iteration == 0) {
//            m_selectionStrategy = ((FeatureSelectionLoopStartNodeModel)getLoopStartNode()).getSelectionStrategy();
//            m_selectionStrategy.setIsMinimize(m_settings.isMinimize());
//            m_selectionStrategy.setScoreName(scoreVariableName);
            FeatureSelectionLoopStartNodeModel startNode = (FeatureSelectionLoopStartNodeModel)getLoopStartNode();
            m_featureSelector = startNode.getFeatureSelector();
            if (m_featureSelector == null) {
                return null;
            }
            m_featureSelector.setIsMinimize(m_settings.isMinimize());
            m_featureSelector.setScoreName(scoreVariableName);
        }

        // the second outSpec is null because before the search is finished, we don't know which feature levels there are
        return new PortObjectSpec[] {m_featureSelector.getSpecForResultTable(), null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        final FlowVariable scoreVariable = getAvailableInputFlowVariables().get(m_settings.getScoreVariableName());
        final double score = scoreVariable.getDoubleValue();
        if (m_resultTable == null) {
            m_resultTable = exec.createDataContainer(m_featureSelector.getSpecForResultTable());
            m_featureSelector.setResultTableContainer(m_resultTable);
        }

        m_featureSelector.addScore(score);


        m_iteration++;
        if (m_featureSelector.continueLoop()) {
            continueLoop();
            return null;
        }
        m_resultTable.close();
        return new PortObject[]{m_resultTable.getTable(), m_featureSelector.getFeatureSelectionModel()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_iteration = 0;
        m_resultTable = null;
        m_featureSelector = null;
    }

}
