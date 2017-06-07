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
 *   Nov 11, 2016 (hornm): created
 */
package org.knime.core.gateway.serverproxy.service;

import static org.knime.core.gateway.serverproxy.util.EntityBuilderUtil.buildNodeEnt;
import static org.knime.core.gateway.serverproxy.util.EntityBuilderUtil.buildWorkflowEnt;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.api.node.workflow.project.WorkflowProjectManager;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.core.gateway.v0.workflow.service.WorkflowService;
import org.knime.core.node.workflow.NodeID;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultWorkflowService implements WorkflowService {

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowEnt getWorkflow(final String rootWorkflowID, final Optional<String> nodeID) {
        //get the right IWorkflowManager for the given id and create a WorkflowEnt from it
        try {
            if (nodeID.isPresent()) {
                INodeContainer metaNode = WorkflowProjectManager.getInstance().openAndCacheWorkflow(rootWorkflowID)
                    .orElseThrow(() -> new NoSuchElementException(
                        "Workflow project for ID \"" + rootWorkflowID + "\" not found."))
                    .getNodeContainer(NodeID.fromString(nodeID.get()));
                assert metaNode instanceof IWorkflowManager;
                IWorkflowManager wfm = (IWorkflowManager)metaNode;
                if (wfm.isEncrypted()) {
                    throw new IllegalStateException("Workflow is encrypted and cannot be accessed.");
                }
                return buildWorkflowEnt(wfm, rootWorkflowID);
            } else {
                IWorkflowManager wfm = WorkflowProjectManager.getInstance().openAndCacheWorkflow(rootWorkflowID)
                    .orElseThrow(() -> new NoSuchElementException(
                        "Workflow project for ID \"" + rootWorkflowID + "\" not found."));
                if (wfm.isEncrypted()) {
                    throw new IllegalStateException("Workflow is encrypted and cannot be accessed.");
                }
                return buildWorkflowEnt(wfm, rootWorkflowID);
            }
        } catch (Exception ex) {
            // TODO better exception handling
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeEnt getNode(final String rootWorkflowID, final Optional<String> nodeID) {
        //get the right IWorkflowManager for the given id and create a WorkflowEnt from it
        try {
            if (nodeID.isPresent()) {
                INodeContainer node = WorkflowProjectManager.getInstance().openAndCacheWorkflow(rootWorkflowID).orElseThrow(() -> new NoSuchElementException(
                    "Workflow project for ID \"" + rootWorkflowID + "\" not found."))
                    .getNodeContainer(NodeID.fromString(nodeID.get()));
                return buildNodeEnt(node, rootWorkflowID);
            } else {
                return buildNodeEnt(WorkflowProjectManager.getInstance().openAndCacheWorkflow(rootWorkflowID)
                    .orElseThrow(() -> new NoSuchElementException(
                        "Workflow project for ID \"" + rootWorkflowID + "\" not found.")),
                    rootWorkflowID);
            }
        } catch (Exception ex) {
            // TODO better exception handling
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllWorkflows() {
        return WorkflowProjectManager.getInstance().getWorkflowProjects().stream().map((wp) -> {
            return wp.getID();
        }).collect(Collectors.toList());
    }
}
