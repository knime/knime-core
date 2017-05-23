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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.api.node.workflow.project.ProjectTreeNode;
import org.knime.core.api.node.workflow.project.WorkflowProject;
import org.knime.core.api.node.workflow.project.WorkflowProjectManager;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.core.gateway.v0.workflow.service.WorkflowService;
import org.knime.core.internal.KNIMEPath;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.util.LockFailedException;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultWorkflowService implements WorkflowService {

    private ProjectTreeNode m_root = new MyProjectTreeNode(KNIMEPath.getWorkspaceDirPath().toPath());

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateWorkflow(final WorkflowEnt wf) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowEnt getWorkflow(final String rootWorkflowID, final Optional<String> nodeID) {
        //get the right IWorkflowManager for the given id and create a WorkflowEnt from it
        try {
            if (nodeID.isPresent()) {
                INodeContainer metaNode = getWorkflowProjectForID(rootWorkflowID, m_root).openProject()
                    .getNodeContainer(NodeID.fromString(nodeID.get()));
                assert metaNode instanceof IWorkflowManager;
                IWorkflowManager wfm = (IWorkflowManager)metaNode;
                if(wfm.isEncrypted()) {
                    throw new IllegalStateException("Workflow is encrypted and cannot be accessed.");
                }
                return buildWorkflowEnt(wfm, rootWorkflowID);
            } else {
                IWorkflowManager wfm = getWorkflowProjectForID(rootWorkflowID, m_root).openProject();
                if(wfm.isEncrypted()) {
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
                INodeContainer node = getWorkflowProjectForID(rootWorkflowID, m_root).openProject()
                    .getNodeContainer(NodeID.fromString(nodeID.get()));
                return buildNodeEnt(node, rootWorkflowID);
            } else {
                return buildNodeEnt(getWorkflowProjectForID(rootWorkflowID, m_root).openProject(), rootWorkflowID);
            }
        } catch (Exception ex) {
            // TODO better exception handling
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<String> getWorkflowIDs(final String workflowGroupId) {
        ProjectTreeNode n;
        if (workflowGroupId.equals("root")) {
            n = m_root;
        } else {
            n = getProjectTreeNodeForID(workflowGroupId, m_root);
        }
        if (n != null) {
            return n.getChildrenProjects().stream().map(wp -> wp.getID()).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getWorkflowGroupIDs(final String workflowGroupId) {
        ProjectTreeNode n;
        if (workflowGroupId.equals("root")) {
            n = m_root;
        } else {
            n = getProjectTreeNodeForID(workflowGroupId, m_root);
        }
        if (n != null) {
            return n.getChildren().stream().map(tn -> tn.getID()).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllWorkflows() {
        return WorkflowProjectManager.getWorkflowProjectsMap().values().stream().map((wp) -> {
            return wp.getName();
        }).collect(Collectors.toList());
    }

    /**
     * Traverse the project tree in order to find a node with the given id
     *
     * @param id tree node id
     * @return the tree node, <code>null</code> if none found with the id
     */
    private ProjectTreeNode getProjectTreeNodeForID(final String id, final ProjectTreeNode node) {
        for (ProjectTreeNode n : node.getChildren()) {
            if (n.getID().equals(id)) {
                return n;
            } else {
                ProjectTreeNode found = getProjectTreeNodeForID(id, n);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Recursively traverse the project tree in order to find the workflow with the given id TODO: speed up by keeping a
     * workflow-id map
     *
     * @param id
     * @param node
     * @return the workflow project for the given id, or <code>null</code> if not found
     */
    private WorkflowProject getWorkflowProjectForID(final String id, final ProjectTreeNode node) {
        for (WorkflowProject wp : node.getChildrenProjects()) {
            if (wp.getID().equals(id)) {
                return wp;
            }
        }
        for (ProjectTreeNode n : node.getChildren()) {
            WorkflowProject found = getWorkflowProjectForID(id, n);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private class MyProjectTreeNode implements ProjectTreeNode {

        private Path m_path;

        private List<ProjectTreeNode> m_children = null;

        private List<WorkflowProject> m_wfChildren = null;

        /**
         *
         */
        public MyProjectTreeNode(final Path path) {
            m_path = path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_path.getFileName().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getID() {
            return m_path.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<ProjectTreeNode> getChildren() {
            resolveChildren();
            return m_children;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<WorkflowProject> getChildrenProjects() {
            resolveChildren();
            return m_wfChildren;
        }

        private void resolveChildren() {
            if (m_children == null) {
                //not resolved, yet
                m_children = new ArrayList<ProjectTreeNode>();
                m_wfChildren = new ArrayList<WorkflowProject>();
                try {
                    Files.list(m_path).forEach((p) -> {
                        if (Files.isDirectory(p)) {
                            if (Files.exists(p.resolve("workflow.knime"))) {
                                m_wfChildren.add(new MyWorkflowProject(p));
                            } else {
                                m_children.add(new MyProjectTreeNode(p));
                            }
                        }
                    });
                } catch (IOException ex) {
                    // TODO better exception handling
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private class MyWorkflowProject implements WorkflowProject {

        private Path m_path;

        private IWorkflowManager m_cachedWFM;

        /**
         *
         */
        public MyWorkflowProject(final Path path) {
            m_path = path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_path.getFileName().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getID() {
            return m_path.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WorkflowProjectType getType() {
            return WorkflowProjectType.LOCAL;
        }

        /**
         * {@inheritDoc}
         *
         * @throws LockFailedException
         * @throws UnsupportedWorkflowVersionException
         * @throws CanceledExecutionException
         * @throws InvalidSettingsException
         * @throws IOException
         */
        @Override
        public IWorkflowManager openProject() throws IOException, InvalidSettingsException, CanceledExecutionException,
            UnsupportedWorkflowVersionException, LockFailedException {
            if (m_cachedWFM == null) {
                m_cachedWFM = org.knime.core.node.workflow.WorkflowManager
                    .loadProject(m_path.toFile(), new ExecutionMonitor(), new WorkflowLoadHelper())
                    .getWorkflowManager();
            }
            return m_cachedWFM;
        }

        @Override
        public Optional<IWorkflowManager> getProject() {
            return Optional.ofNullable(m_cachedWFM);
        }

    }

}
