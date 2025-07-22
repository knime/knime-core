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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 5, 2013 by wiswedel
 */
package org.knime.core.node.workflow;

import java.util.Map;
import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CopyNodePersistor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.util.Version;

/**
 *
 * @author wiswedel
 */
public class CopyNativeNodeContainerPersistor extends CopySingleNodeContainerPersistor implements
    NativeNodeContainerPersistor {

    private final NodeFactory<NodeModel> m_nodeFactory;
    private final CopyNodePersistor m_nodePersistor;

    /** The node instance that was created last. It fixes bug 4404 (comes up when a node is pasted
     * multiple times). The usual pattern is:
     * 1) User hits Ctrl-C: and object of this class is instantiated
     * 2) User hits Ctrl-V:
     * 2.1) Paste routine in WFM calls {@link #getNodeContainer(WorkflowManager, NodeID)}
     *      (also assigns m_lastCreatedNode)
     * 2.2) Paste routine in WFM calls {@link #loadNodeContainer(Map, ExecutionMonitor, LoadResult)}
     *      (fills m_lastCreatedNode)
     * 2.3) Node is inserted and has all members initialized
     * 3) User hits Ctrl-V again:
     * 3.1) See 2.1)
     * 3.2) See 2.2)
     *
     * Alternatively, we could also do the stuff from 2.2 (loading the configuration into the node)
     * in 2.1 but that feels wrong as it's done directly from the constructor of SingleNodeContainer
     * (for instance no context is available but we call client code already).
     */
    private Node m_lastCreatedNode;

    private final ModifiableNodeCreationConfiguration m_creationConfig;

    private Version m_nodeSettingsBundleVersion;

    /**
     * @param original
     * @param preserveDeletableFlag
     * @param isUndoableDeleteCommand
     */
    public CopyNativeNodeContainerPersistor(final NativeNodeContainer original, final boolean preserveDeletableFlag,
        final boolean isUndoableDeleteCommand) {
        super(original, preserveDeletableFlag, isUndoableDeleteCommand);
        Node originalNode = original.getNode();
        m_nodeFactory = originalNode.getFactory();
        m_creationConfig = originalNode.getCopyOfCreationConfig().orElse(null);
        m_nodePersistor = originalNode.createCopyPersistor();
        m_nodeSettingsBundleVersion = original.getSettingsBundleVersion();
    }

    /** {@inheritDoc} */
    @Override
    public Node getNode() {
        Node node = new Node(m_nodeFactory, m_creationConfig);
        // we don't load any settings into the node instance here as this method is called
        // from the constructor of SingleNodeContainer - it doesn't have a context set and therefore
        // cannot resolve URLs etc (knime://knime.workflow/some-path)
        // Settings are loaded in loadNodeContainer
        m_lastCreatedNode = node;
        return node;
    }

    /** {@inheritDoc} */
    @Override
    public NodeAndBundleInformationPersistor getNodeAndBundleInformation() {
        return null; // copy & paste only for idle/configured nodes - no need to keep bundle version
    }

    /**
     * @since 5.6
     */
    @Override
    public Optional<Version> getNodeSettingsBundleVersion() {
        return Optional.ofNullable(m_nodeSettingsBundleVersion);
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent, final NodeID id) {
        return new NativeNodeContainer(parent, id, this);
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(
            final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult) {
        try {
            final NodeSettingsRO modelSettings = getSNCSettings().getModelSettings();
            if (modelSettings != null) {
                m_lastCreatedNode.validateModelSettings(modelSettings);
                m_lastCreatedNode.loadModelSettingsFrom(modelSettings);
            }
        } catch (InvalidSettingsException e) {
            NodeLogger.getLogger(CopyNativeNodeContainerPersistor.class).debug(
                "Failed to copy settings into node target: " + e.getMessage(), e);
        }
        m_nodePersistor.loadInto(m_lastCreatedNode);
    }

}
