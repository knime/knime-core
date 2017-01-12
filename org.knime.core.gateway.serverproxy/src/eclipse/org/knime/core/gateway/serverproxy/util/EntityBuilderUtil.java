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
 *   Jan 10, 2017 (hornm): created
 */
package org.knime.core.gateway.serverproxy.util;

import static org.knime.core.gateway.entities.EntityBuilderManager.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.api.node.port.PortTypeUID;
import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.api.node.workflow.INodeAnnotation;
import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.api.node.workflow.INodeInPort;
import org.knime.core.api.node.workflow.INodeOutPort;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.api.node.workflow.JobManagerUID;
import org.knime.core.api.node.workflow.NodeUIInformation;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.MetaPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.PortTypeEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.core.gateway.v0.workflow.entity.XYEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.ConnectionEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.EntityIDBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.JobManagerEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.MetaPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeInPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeMessageEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeOutPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.PortTypeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowEntBuilder;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NativeNodeContainer;

/**
 * Collects helper methods to build entity instances basically from core.api-classes (e.g. IWorkflowManager etc.).
 *
 * @author Martin Horn, University of Konstanz
 */
public class EntityBuilderUtil {

    private EntityBuilderUtil() {
        //utility class
    }

    private static PortTypeEnt buildPortTypeEnt(final PortTypeUID portTypeUID) {
        return builder(PortTypeEntBuilder.class).setColor(portTypeUID.getColor()).setIsHidden(portTypeUID.isHidden())
            .setIsOptional(portTypeUID.isOptional()).setName(portTypeUID.getName())
            .setPortObjectClassName(portTypeUID.getPortObjectClassName()).build();
    }

    private static List<NodeInPortEnt> buildNodeInPortEnts(final INodeContainer nc) {
        List<NodeInPortEnt> inPorts = new ArrayList<>(nc.getNrInPorts());
        for (int i = 0; i < nc.getNrInPorts(); i++) {
            INodeInPort inPort = nc.getInPort(i);
            PortTypeEnt pType = buildPortTypeEnt(inPort.getPortTypeUID());
            inPorts.add(builder(NodeInPortEntBuilder.class).setPortIndex(i).setPortName(inPort.getPortName())
                .setPortType(pType).build());
        }
        return inPorts;
    }

    private static List<NodeOutPortEnt> buildNodeOutPortEnts(final INodeContainer nc) {
        List<NodeOutPortEnt> outPorts = new ArrayList<>(nc.getNrOutPorts());
        for (int i = 0; i < nc.getNrOutPorts(); i++) {
            INodeOutPort outPort = nc.getOutPort(i);
            PortTypeEnt pType = buildPortTypeEnt(outPort.getPortTypeUID());
            outPorts.add(builder(NodeOutPortEntBuilder.class).setPortIndex(i).setPortName(outPort.getPortName())
                .setPortType(pType).build());
        }
        return outPorts;
    }

    private static NodeAnnotationEnt buildNodeAnnotationEnt(final INodeContainer nc) {
        INodeAnnotation na = nc.getNodeAnnotation();
        return builder(NodeAnnotationEntBuilder.class).setBackgroundColor(na.getBgColor())
            .setBorderColor(na.getBorderColor()).setBorderSize(na.getBorderSize())
            .setDefaultFontSize(na.getDefaultFontSize()).setHeight(na.getHeight()).setNode("TODO").setText(na.getText())
            .setTextAlignment(na.getAlignment().toString()).setVersion(na.getVersion()).setWidth(na.getWidth())
            .setX(na.getX()).setY(na.getY()).build();
    }

    private static JobManagerEnt buildJobManagerEnt(final INodeContainer nc) {
        Optional<JobManagerUID> jobManagerUID = nc.getJobManagerUID();
        return builder(JobManagerEntBuilder.class).setJobManagerID(jobManagerUID.map(j -> j.getID()).orElse("Not set"))
            .setName(jobManagerUID.map(j -> j.getName()).orElse("Not set")).build();
    }

    private static BoundsEnt buildBoundsEnt(final NodeUIInformation ui) {
        int[] bounds;
        //ui information is not available in some cases
        if (ui != null) {
            bounds = ui.getBounds();
        } else {
            bounds = new int[4];
        }
        return builder(BoundsEntBuilder.class).setX(bounds[0]).setY(bounds[1]).setWidth(bounds[2]).setHeight(bounds[3])
            .build();
    }

    //TODO
    private static List<MetaPortEnt> buildDummyMetaPortEnt() {
        PortTypeEnt portType = builder(PortTypeEntBuilder.class).setColor(0).setIsHidden(false).setIsOptional(true).setName("name").setPortObjectClassName("bliblablub").build();
        return Arrays.asList(builder(MetaPortEntBuilder.class).setIsConnected(false).setMessage("test").setNewIndex(0).setOldIndex(1).setPortType(portType).build());
    }

    private static NodeMessageEnt buildNodeMessageEnt(final INodeContainer nc) {
        return builder(NodeMessageEntBuilder.class).setMessage(nc.getNodeMessage().getMessage())
            .setType(nc.getNodeMessage().getMessageType().toString()).build();
    }

    private static NodeEnt buildNodeEnt(final INodeContainer nc) {

        String nodeTypeID = "";
        if(nc instanceof NativeNodeContainer) {
            NodeFactory<NodeModel> factory = ((NativeNodeContainer)nc).getNode().getFactory();
            if(factory instanceof DynamicNodeFactory) {
                nodeTypeID = factory.getClass().getCanonicalName() + "#" + nc.getName();
            } else {
                nodeTypeID = factory.getClass().getCanonicalName();
            }
        }
        return builder(NodeEntBuilder.class).setName(nc.getName()).setNodeID(nc.getID().toString())
            .setNodeMessage(buildNodeMessageEnt(nc)).setNodeType(nc.getType().toString())
            .setBounds(buildBoundsEnt(nc.getUIInformation())).setIsDeletable(nc.isDeletable())
            .setNodeState(nc.getNodeContainerState().toString()).setOutPorts(buildNodeOutPortEnts(nc))
            .setParent(builder(EntityIDBuilder.class).setID("TODO").setType("WorkflowEnt").build())
            .setJobManager(buildJobManagerEnt(nc)).setNodeAnnotation(buildNodeAnnotationEnt(nc))
            .setInPorts(buildNodeInPortEnts(nc)).setHasDialog(nc.hasDialog())
            .setNodeTypeID(nodeTypeID).build();
    }

    private static ConnectionEnt buildContainerEnt(final IConnectionContainer cc) {
        //cc.getUIInfo() gives null!
        //      int[][] allBendpoints = cc.getUIInfo().getAllBendpoints();
        //      List<XYEnt> bendpoints = Arrays.stream(allBendpoints).map(a -> {
        //         return builder(XYEntBuilder.class).setX(a[0]).setY(a[1]).build();
        //      }).collect(Collectors.toList());
        List<XYEnt> bendpoints = Collections.emptyList();
        return builder(ConnectionEntBuilder.class).setDest(cc.getDest().toString()).setDestPort(cc.getDestPort())
            .setSource(cc.getSource().toString()).setSourcePort(cc.getSourcePort()).setIsDeleteable(cc.isDeletable())
            .setType(cc.getType().toString()).setBendPoints(bendpoints).build();
    }

    public static WorkflowEnt buildWorkflowEnt(final IWorkflowManager wfm) {
        Collection<INodeContainer> nodeContainers = wfm.getAllNodeContainers();
        Map<String, NodeEnt> nodes = nodeContainers.stream().map(nc -> {
            return buildNodeEnt(nc);
        }).collect(Collectors.toMap(n -> n.getNodeID(), n -> n));
        Collection<IConnectionContainer> connectionContainers = wfm.getConnectionContainers();
        List<ConnectionEnt> connections = connectionContainers.stream().map(cc -> {
            return buildContainerEnt(cc);
        }).collect(Collectors.toList());
        return builder(WorkflowEntBuilder.class).setNodes(nodes).setConnections(connections)
            .setBounds(buildBoundsEnt(wfm.getUIInformation())).setHasDialog(wfm.hasDialog())
            .setInPorts(buildNodeInPortEnts(wfm)).setIsDeletable(wfm.isDeletable())
            .setJobManager(buildJobManagerEnt(wfm)).setName(wfm.getName())
            .setNodeAnnotation(buildNodeAnnotationEnt(wfm)).setNodeID(wfm.getID().toString())
            .setNodeMessage(buildNodeMessageEnt(wfm)).setNodeState("TODO").setNodeType(wfm.getType().toString())
            .setOutPorts(buildNodeOutPortEnts(wfm))
            .setParent(builder(EntityIDBuilder.class).setID("TODO").setType("WorkflowEnt").build())
            .setMetaInPorts(buildDummyMetaPortEnt())
            .setMetaOutPorts(buildDummyMetaPortEnt()).build();
    }

}
