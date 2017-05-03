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

import org.knime.core.api.node.port.MetaPortInfo;
import org.knime.core.api.node.port.PortTypeUID;
import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.api.node.workflow.INodeAnnotation;
import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.api.node.workflow.INodeInPort;
import org.knime.core.api.node.workflow.INodeOutPort;
import org.knime.core.api.node.workflow.IWorkflowAnnotation;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.api.node.workflow.JobManagerUID;
import org.knime.core.api.node.workflow.NodeUIInformation;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.MetaPortInfoEnt;
import org.knime.core.gateway.v0.workflow.entity.NativeNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.PortTypeEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.XYEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.ConnectionEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.JobManagerEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.MetaPortInfoEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NativeNodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeFactoryIDEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeInPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeMessageEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeOutPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.PortTypeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowNodeEntBuilder;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.JobManagerUtil;
import org.knime.core.util.PortTypeUtil;

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
            inPorts.add(buildNodeInPortEnt(nc.getInPort(i)));
        }
        return inPorts;
    }

    private static List<NodeInPortEnt> buildWorkflowOutgoingPortEnts(final IWorkflowManager wm) {
        List<NodeInPortEnt> inPorts = new ArrayList<>(wm.getNrWorkflowOutgoingPorts());
        for (int i = 0; i < wm.getNrWorkflowOutgoingPorts(); i++) {
            inPorts.add(buildNodeInPortEnt(wm.getWorkflowOutgoingPort(i)));
        }
        return inPorts;
    }

    private static NodeInPortEnt buildNodeInPortEnt(final INodeInPort inPort) {
        PortTypeEnt pType = buildPortTypeEnt(inPort.getPortTypeUID());
        return builder(NodeInPortEntBuilder.class)
                .setPortIndex(inPort.getPortIndex())
                .setPortName(inPort.getPortName())
                .setPortType(pType).build();
    }

    private static List<NodeOutPortEnt> buildNodeOutPortEnts(final INodeContainer nc) {
        List<NodeOutPortEnt> outPorts = new ArrayList<>(nc.getNrOutPorts());
        for (int i = 0; i < nc.getNrOutPorts(); i++) {
            outPorts.add(buildNodeOutPortEnt(nc.getOutPort(i)));
        }
        return outPorts;
    }

    private static List<NodeOutPortEnt> buildWorkflowIncomingPortEnts(final IWorkflowManager wm) {
        List<NodeOutPortEnt> outPorts = new ArrayList<>(wm.getNrWorkflowIncomingPorts());
        for (int i = 0; i < wm.getNrWorkflowIncomingPorts(); i++) {
            outPorts.add(buildNodeOutPortEnt(wm.getWorkflowIncomingPort(i)));
        }
        return outPorts;
    }

    private static NodeOutPortEnt buildNodeOutPortEnt(final INodeOutPort outPort) {
        PortTypeEnt pType = buildPortTypeEnt(outPort.getPortTypeUID());
        return builder(NodeOutPortEntBuilder.class)
                .setPortIndex(outPort.getPortIndex())
                .setPortName(outPort.getPortName())
                .setPortType(pType).build();
    }

    private static NodeAnnotationEnt buildNodeAnnotationEnt(final INodeContainer nc) {
        INodeAnnotation na = nc.getNodeAnnotation();
        return builder(NodeAnnotationEntBuilder.class).setBackgroundColor(na.getBgColor())
            .setBorderColor(na.getBorderColor()).setBorderSize(na.getBorderSize())
            .setDefaultFontSize(na.getDefaultFontSize()).setHeight(na.getHeight()).setText(na.getText())
            .setTextAlignment(na.getAlignment().toString()).setVersion(na.getVersion()).setWidth(na.getWidth())
            .setX(na.getX()).setY(na.getY()).setIsDefault(na.getData().isDefault()).build();
    }

    private static Optional<JobManagerEnt> buildJobManagerEnt(final Optional<JobManagerUID> jobManagerUID) {
        return jobManagerUID.map(uid -> {
            return builder(JobManagerEntBuilder.class)
                .setJobManagerID(jobManagerUID.map(j -> j.getID()).orElse("Not set"))
                .setName(jobManagerUID.map(j -> j.getName()).orElse("Not set")).build();
        });
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

    //TODO!!
    private static MetaPortInfoEnt buildMetaPortInfoEnt(final MetaPortInfo info) {
        PortType pt = PortTypeUtil.getPortType(info.getTypeUID());
        PortTypeEnt portType = builder(PortTypeEntBuilder.class)
                .setColor(pt.getColor())
                .setIsHidden(pt.isHidden())
                .setIsOptional(pt.isOptional())
                .setName(pt.getName())
                .setPortObjectClassName(pt.getPortObjectClass().getCanonicalName()).build();
        return builder(MetaPortInfoEntBuilder.class)
            .setIsConnected(info.isConnected())
            .setMessage(info.getMessage())
            .setNewIndex(info.getNewIndex())
            .setOldIndex(info.getNewIndex())
            .setPortType(portType).build();
    }

    private static List<MetaPortInfoEnt> buildMetaInPortInfoEnts(final IWorkflowManager wm) {
        if (wm.getNrWorkflowIncomingPorts() == 0) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(wm.getParent().getMetanodeInputPortInfo(wm.getID())).map(i -> buildMetaPortInfoEnt(i))
                .collect(Collectors.toList());
        }
    }

    private static List<MetaPortInfoEnt> buildMetaOutPortInfoEnts(final IWorkflowManager wm) {
        if (wm.getNrWorkflowOutgoingPorts() == 0) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(wm.getParent().getMetanodeOutputPortInfo(wm.getID())).map(i -> buildMetaPortInfoEnt(i))
                .collect(Collectors.toList());
        }
    }

    private static NodeMessageEnt buildNodeMessageEnt(final INodeContainer nc) {
        return builder(NodeMessageEntBuilder.class).setMessage(nc.getNodeMessage().getMessage())
            .setType(nc.getNodeMessage().getMessageType().toString()).build();
    }

    /**
     * @param nc
     * @param rootWorkflowID must be present, if nc is of type {@link IWorkflowManager}
     * @return
     */
    public static NodeEnt buildNodeEnt(final INodeContainer nc, final String rootWorkflowID) {
        if(nc instanceof NativeNodeContainer) {
            return buildNativeNodeEnt((NativeNodeContainer) nc, rootWorkflowID);
        } else if(nc instanceof IWorkflowManager) {
            return buildWorkflowNodeEnt((IWorkflowManager) nc, rootWorkflowID);
        } else {
            throw new IllegalArgumentException("Node container " + nc.getClass().getCanonicalName() + " cannot be mapped to a node entity.");
        }
    }

    private static NativeNodeEnt buildNativeNodeEnt(final NativeNodeContainer nc, final String rootWorkflowID) {
        NodeFactory<NodeModel> factory = nc.getNode().getFactory();
        NodeFactoryIDEntBuilder nodeFactoryIDBuilder = builder(NodeFactoryIDEntBuilder.class)
                .setClassName(factory.getClass().getCanonicalName());
        //only set node name in case of a dynamic node factory
        if (DynamicNodeFactory.class.isAssignableFrom(factory.getClass())) {
            nodeFactoryIDBuilder.setNodeName(Optional.of(nc.getName()));
        }
        return builder(NativeNodeEntBuilder.class).setName(nc.getName()).setNodeID(nc.getID().toString())
            .setNodeMessage(buildNodeMessageEnt(nc)).setNodeType(nc.getType().toString())
            .setBounds(buildBoundsEnt(nc.getUIInformation())).setIsDeletable(nc.isDeletable())
            .setNodeState(nc.getNodeContainerState().toString()).setOutPorts(buildNodeOutPortEnts(nc))
            .setParentNodeID(Optional.ofNullable(nc.getParent()).map(p -> p.getID().toString()))
            .setRootWorkflowID(rootWorkflowID)
            .setJobManager(buildJobManagerEnt(nc.getJobManagerUID())).setNodeAnnotation(buildNodeAnnotationEnt(nc))
            .setInPorts(buildNodeInPortEnts(nc)).setHasDialog(nc.hasDialog())
            .setNodeFactoryID(nodeFactoryIDBuilder.build()).build();
    }

    public static WorkflowNodeEnt buildWorkflowNodeEnt(final IWorkflowManager wm, final String rootWorkflowID) {
        Optional<JobManagerUID> jobManagerUID;
        if (wm.getParent() == WorkflowManager.ROOT) {
            //TODO somehow get the default job manager from the workflow manager itself!!
            jobManagerUID =
                Optional.of(JobManagerUtil.getJobManagerUID(NodeExecutionJobManagerPool.getDefaultJobManagerFactory()));
        } else {
            jobManagerUID = wm.getJobManagerUID();
        }
        return builder(WorkflowNodeEntBuilder.class).setName(wm.getName()).setNodeID(wm.getID().toString())
                .setNodeMessage(buildNodeMessageEnt(wm)).setNodeType(wm.getType().toString())
                .setBounds(buildBoundsEnt(wm.getUIInformation())).setIsDeletable(wm.isDeletable())
                .setNodeState(wm.getNodeContainerState().toString()).setOutPorts(buildNodeOutPortEnts(wm))
                .setParentNodeID(wm.getParent() == WorkflowManager.ROOT ? Optional.empty() : Optional.of(wm.getParent().getID().toString()))
                .setJobManager(buildJobManagerEnt(jobManagerUID)).setNodeAnnotation(buildNodeAnnotationEnt(wm))
                .setInPorts(buildNodeInPortEnts(wm)).setHasDialog(wm.hasDialog())
                .setWorkflowIncomingPorts(buildWorkflowIncomingPortEnts(wm))
                .setWorkflowOutgoingPorts(buildWorkflowOutgoingPortEnts(wm))
                .setRootWorkflowID(rootWorkflowID).build();
    }

    private static ConnectionEnt buildContainerEnt(final IConnectionContainer cc) {
        //TODO
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

    private static WorkflowAnnotationEnt buildWorkflowAnnotationEnt(final IWorkflowAnnotation wa) {
        BoundsEnt bounds = builder(BoundsEntBuilder.class)
                .setX(wa.getX())
                .setY(wa.getY())
                .setWidth(wa.getWidth())
                .setHeight(wa.getHeight())
                .build();
        return builder(WorkflowAnnotationEntBuilder.class)
                .setAlignment(wa.getAlignment().toString())
                .setBgColor(wa.getBgColor())
                .setBorderColor(wa.getBorderColor())
                .setBorderSize(wa.getBorderSize())
                .setFontSize(wa.getDefaultFontSize())
                .setBounds(bounds)
                .setText(wa.getText())
                .build();
    }

    /**
     * @param wfm
     * @param rootWorkflowID the workflow ID of the root workflow
     * @return
     */
    public static WorkflowEnt buildWorkflowEnt(final IWorkflowManager wfm, final String rootWorkflowID) {
        Collection<INodeContainer> nodeContainers = wfm.getAllNodeContainers();
        Map<String, NodeEnt> nodes = nodeContainers.stream().map(nc -> {
            return buildNodeEnt(nc, rootWorkflowID);
        }).collect(Collectors.toMap(n -> n.getNodeID(), n -> n));
        Collection<IConnectionContainer> connectionContainers = wfm.getConnectionContainers();
        List<ConnectionEnt> connections = connectionContainers.stream().map(cc -> {
            return buildContainerEnt(cc);
        }).collect(Collectors.toList());
        return builder(WorkflowEntBuilder.class)
            .setNodes(nodes)
            .setConnections(connections)
            .setMetaInPortInfos(buildMetaInPortInfoEnts(wfm))
            .setMetaOutPortInfos(buildMetaOutPortInfoEnts(wfm))
            .setWorkflowAnnotations(wfm.getWorkflowAnnotations().stream().map(wa -> buildWorkflowAnnotationEnt(wa)).collect(Collectors.toList()))
            .build();
    }

}
