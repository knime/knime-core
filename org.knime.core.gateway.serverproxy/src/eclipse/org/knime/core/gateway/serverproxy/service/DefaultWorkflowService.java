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

import static org.knime.core.gateway.entities.EntityBuilderManager.builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.api.node.workflow.project.WorkflowGroup;
import org.knime.core.api.node.workflow.project.WorkflowProject;
import org.knime.core.api.node.workflow.project.WorkflowProjectManager;
import org.knime.core.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.core.gateway.v0.workflow.entity.EntityID;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.core.gateway.v0.workflow.entity.XYEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.ConnectionEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeMessageEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.XYEntBuilder;
import org.knime.core.gateway.v0.workflow.service.WorkflowService;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultWorkflowService implements WorkflowService {

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
    public WorkflowEnt getWorkflow(final EntityID id) {
        //TODO somehow get the right IWorkflowManager for the given id and create a WorkflowEnt from it
        IWorkflowManager wfm = WorkflowProjectManager.openProject(new WorkflowProject() {

            @Override
            public String getName() {
                return id.getID();
            }

            @Override
            public WorkflowProjectType getType() {
                return null;
            }

        });
        Collection<INodeContainer> nodeContainers = wfm.getAllNodeContainers();
        builder(NodeEntBuilder.class).setIsDeletable(false).setBounds(null).build();
        List<NodeEnt> nodes = nodeContainers.stream().map(nc -> {
            int[] bounds = nc.getUIInformation().getBounds();
            return builder(NodeEntBuilder.class)
                    .setName(nc.getName())
                    .setNodeID(nc.getID().toString())
                    .setNodeMessage(builder(NodeMessageEntBuilder.class).setMessage(nc.getNodeMessage().getMessage()).setType(nc.getNodeMessage().getMessageType().toString()).build())
                    .setNodeType(nc.getType().toString())
                    .setBounds(builder(BoundsEntBuilder.class).setX(bounds[0]).setY(bounds[1]).setWidth(bounds[2]).setHeight(bounds[3]).build())
                    .setIsDeletable(nc.isDeletable())
                    .build();
        }).collect(Collectors.toList());
        Collection<IConnectionContainer> connectionContainers = wfm.getConnectionContainers();
        List<ConnectionEnt> connections = connectionContainers.stream().map(cc -> {
            int[][] allBendpoints = cc.getUIInfo().getAllBendpoints();
            List<XYEnt> bendpoints = Arrays.stream(allBendpoints).map(a -> {
               return builder(XYEntBuilder.class).setX(a[0]).setY(a[1]).build();
            }).collect(Collectors.toList());
            return builder(ConnectionEntBuilder.class)
                   .setDest(cc.getDest().toString())
                   .setDestPort(cc.getDestPort())
                   .setSource(cc.getSource().toString())
                   .setSourcePort(cc.getSourcePort())
                   .setIsDeleteable(cc.isDeletable())
                   .setType(cc.getType().toString())
                   .setBendPoints(bendpoints)
                   .build();
        }).collect(Collectors.toList());
        return builder(WorkflowEntBuilder.class).setNodes(nodes).setConnections(connections).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EntityID> getAllWorkflows() {
        WorkflowGroup rootWorkflowGroup = WorkflowProjectManager.getRootWorkflowGroup();
        //TODO traverse and get all workflow projects (possibly only the local ones)
        return null;
    }

}
