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
 * Created on Oct 1, 2013 by Berthold
 */
package org.knime.core.node.workflow;

import static java.util.stream.Collectors.toList;
import static org.knime.core.node.util.CheckUtils.checkState;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeConfigureHelper;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDescription27Proxy;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.node.dialog.EnabledDialogNodeModelFilter;
import org.knime.core.node.dialog.SubNodeDescriptionProvider;
import org.knime.core.node.dialog.util.ConfigurationLayoutUtil;
import org.knime.core.node.exec.ThreadNodeExecutionJobManagerFactory;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.ViewContent;
import org.knime.core.node.message.Message;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.port.report.IReportPortObject;
import org.knime.core.node.port.report.ReportConfiguration;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.wizard.CSSModifiable;
import org.knime.core.node.wizard.ViewHideable;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.ComponentMetadata.ComponentOptionalsBuilder;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.node.workflow.MetaNodeDialogPane.MetaNodeDialogType;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.TemplateType;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.NodePropertyChangedEvent.NodeProperty;
import org.knime.core.node.workflow.TemplateUpdateUtil.TemplateUpdateCheckResult;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;
import org.knime.core.node.workflow.WorkflowPersistor.ConnectionContainerTemplate;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.NodeContainerTemplateLinkUpdateResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult.Builder;
import org.knime.core.node.workflow.def.DefToCoreUtil;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
import org.knime.core.node.workflow.execresult.SubnodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.WorkflowExecutionResult;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeExchange;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeModel;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeOutputNodeFactory;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeOutputNodeModel;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.LockFailedException;
import org.knime.core.util.ThreadPool;
import org.knime.shared.workflow.def.BaseNodeDef;
import org.knime.shared.workflow.def.ComponentNodeDef;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Implementation of a {@link NodeContainer} holding a set of nodes via a {@link WorkflowManager}
 * and acting like a {@link NativeNodeContainer} to the outside.
 *
 * @noreference Not to be used by clients.
 * @author M. Berthold &amp; B. Wiswedel
 * @since 2.9
 */
public final class SubNodeContainer extends SingleNodeContainer
    implements NodeContainerParent, NodeContainerTemplate, CSSModifiable, ViewHideable {

    /** Shown in help description when nothing is set in input/output node. */
    private static final String NO_DESCRIPTION_SET = "<no description set>";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SubNodeContainer.class);

    /** Keeps outgoing information (specs, objects, HiLiteHandlers...). */
    private static final class Output {
        private final PortType m_type;
        private PortObjectSpec m_spec;
        private PortObject m_object;
        private HiLiteHandler m_hiliteHdl;
        private String m_summary = "no summary";

        /** @param type Non null port type. */
        Output(final PortType type) {
            m_type = type;
        }
        boolean setSpec(final PortObjectSpec spec) {
            boolean differ = ObjectUtils.notEqual(m_spec, spec);
            m_spec = spec;
            return differ;
        }
        boolean setObject(final PortObject object) {
            boolean differ = ObjectUtils.notEqual(m_object, object);
            String summary = object != null ? object.getSummary() : null;
            differ = differ || ObjectUtils.notEqual(m_summary, summary);
            m_object = object;
            m_summary = summary;
            return differ;
        }
        boolean setHiliteHdl(final HiLiteHandler hiliteHdl) {
            boolean differ = ObjectUtils.notEqual(m_hiliteHdl, hiliteHdl);
            m_hiliteHdl = hiliteHdl;
            return differ;
        }
        PortType getType() {
            return m_type;
        }
        PortObjectSpec getSpec() {
            return m_spec;
        }
        PortObject getObject() {
            return m_object;
        }
        HiLiteHandler getHiliteHdl() {
            return m_hiliteHdl;
        }
        String getSummary() {
            return m_summary;
        }
        void clean() {
            m_spec = null;
            m_object = null;
            m_hiliteHdl = null;
            m_summary = null;
        }
        static PortType[] getPortTypesNoFlowVariablePort(final Output[] outputs) {
            PortType[] result = new PortType[outputs.length - 1];
            for (int i = 1; i < outputs.length; i++) {
                result[i - 1] = outputs[i].getType();
            }
            return result;
        }

    }

    private final WorkflowManager m_wfm;

    private NodeStateChangeListener m_wfmStateChangeListener;
    private WorkflowListener m_wfmListener; // to reset this node's NodeMessage on change

    private NodeInPort[] m_inports;
    private HiLiteHandler[] m_inHiliteHandler;
    private NodeContainerOutPort[] m_outports;
    private Output[] m_outputs;

    private int m_virtualInNodeIDSuffix;
    private int m_virtualOutNodeIDSuffix;

    private FlowObjectStack m_incomingStack;
    private FlowObjectStack m_outgoingStack;

    private final FlowSubnodeScopeContext m_subnodeScopeContext;

    /** Helper flag to avoid state transitions as part callbacks from the inner wfm. These are ignored when execution
     * is triggered via {@link #performExecuteNode(PortObject[])} or reset via {@link #performReset()}. */
    private boolean m_isPerformingActionCalledFromParent;

    /** JSON layout info provider for wizard nodes. */
    private SubnodeContainerLayoutStringProvider m_subnodeLayoutStringProvider;

    /** JSON configuration layout info provider for dialog nodes. */
    private SubnodeContainerConfigurationStringProvider m_subnodeConfigurationStringProvider;

    private boolean m_hideInWizard;
    private String m_customCSS;

    private ComponentMetadata m_metadata;

    private MetaNodeTemplateInformation m_templateInformation;

    /** report config as set by layout editor. */
    private ReportConfiguration m_reportConfiguration;

    /** Caches the example input data spec */
    private PortObjectSpec[] m_exampleInputDataSpec;

    /** Load workflow from persistor.
     *
     * @param parent ...
     * @param id ...
     * @param persistor ...
     */
    SubNodeContainer(final WorkflowManager parent, final NodeID id, final SubNodeContainerPersistor persistor) {
        super(parent, id, persistor.getMetaPersistor());
        m_subnodeScopeContext = new FlowSubnodeScopeContext(this);
        WorkflowPersistor workflowPersistor = persistor.getWorkflowPersistor();
        m_wfm = new WorkflowManager(this, null, new NodeID(id, 0), workflowPersistor,
            parent.getWorkflowDataRepository());
        m_wfm.setJobManager(null);
        WorkflowPortTemplate[] inPortTemplates = persistor.getInPortTemplates();
        WorkflowPortTemplate[] outPortTemplates = persistor.getOutPortTemplates();
        m_reportConfiguration = persistor.getReportConfiguration().orElse(null);
        final int outputLength = outPortTemplates.length + (m_reportConfiguration != null ? 1 : 0);
        m_outports = new NodeContainerOutPort[outputLength];
        m_outputs = new Output[outputLength];
        for (int i = 0; i < outPortTemplates.length; i++) {
            WorkflowPortTemplate t = outPortTemplates[i];
            m_outputs[i] = new Output(t.getPortType());
            m_outports[i] = new NodeContainerOutPort(this, t.getPortType(), t.getPortIndex());
            m_outports[i].setPortName(t.getPortName());
        }
        if (m_reportConfiguration != null) {
            final var lastPortIndex = m_outports.length - 1;
            m_outports[lastPortIndex] = createReportOutputPort(lastPortIndex);
            m_outputs[lastPortIndex] = new Output(m_outports[lastPortIndex].getPortType());
        }
        m_virtualInNodeIDSuffix = persistor.getVirtualInNodeIDSuffix();
        m_virtualOutNodeIDSuffix = persistor.getVirtualOutNodeIDSuffix();
        m_subnodeLayoutStringProvider = persistor.getSubnodeLayoutStringProvider();
        m_subnodeConfigurationStringProvider = persistor.getSubnodeConfigurationStringProvider();
        m_hideInWizard = persistor.isHideInWizard();
        m_customCSS = persistor.getCssStyles();

        final int inputLength = inPortTemplates.length + (m_reportConfiguration != null ? 1 : 0);
        m_inports = new NodeInPort[inputLength];
        m_inHiliteHandler = new HiLiteHandler[inPortTemplates.length - 1];
        for (int i = 0; i < inPortTemplates.length; i++) {
            m_inports[i] = new NodeInPort(i, inPortTemplates[i].getPortType());
            if (i > 0) {
                // ignore optional variable input port
                m_inHiliteHandler[i - 1] = new HiLiteHandler();
            }
        }
        if (m_reportConfiguration != null) {
            final int lastPortIndex = m_inports.length - 1;
            m_inports[lastPortIndex] = createReportInputPort(lastPortIndex);
        }
        m_metadata = persistor.getMetadata();
        m_templateInformation = persistor.getTemplateInformation();
    }

    /** Load workflow from ComponentDef.
    *
    * @param parent the parent {@link WorkflowManager}
    * @param id ...
    * @param persistor ...
    */
    SubNodeContainer(final WorkflowManager parent, final NodeID id, final ComponentNodeDef def) {
        super(parent, id, def);
        m_subnodeScopeContext = new FlowSubnodeScopeContext(this);
        m_wfm = WorkflowManager.newComponentWorkflowManagerInstance(this, new NodeID(id, 0), def);
        m_wfm.setJobManager(null);
        var inports = def.getInPorts();
        var outports = def.getOutPorts();
        m_reportConfiguration = ReportConfiguration.fromDef(def.getReportConfiguration()).orElse(null);
        final int outputLength = outports.size() + (m_reportConfiguration != null ? 1 : 0);
        m_outports = new NodeContainerOutPort[outputLength];
        m_outputs = new Output[outputLength];
        for (var i = 0; i < outports.size(); i++) {
            var portType = DefToCoreUtil.toPortType(outports.get(i).getPortType());
            m_outputs[i] = new Output(portType);
            m_outports[i] = new NodeContainerOutPort(this, portType, outports.get(i).getIndex());
            m_outports[i].setPortName(portType.getName());
        }
        if (m_reportConfiguration != null) {
            final var lastPortIndex = m_outports.length - 1;
            m_outports[lastPortIndex] = createReportOutputPort(lastPortIndex);
            m_outputs[lastPortIndex] = new Output(m_outports[lastPortIndex].getPortType());
        }
        m_inports = new NodeInPort[inports.size()];
        m_inHiliteHandler = new HiLiteHandler[inports.size()- 1];
        m_virtualInNodeIDSuffix = def.getVirtualInNodeId();
        m_virtualOutNodeIDSuffix = def.getVirtualOutNodeId();
        var dialogSettings = def.getDialogSettings();
        m_subnodeLayoutStringProvider = new SubnodeContainerLayoutStringProvider(dialogSettings.getLayoutJSON());
        m_subnodeConfigurationStringProvider =
                new SubnodeContainerConfigurationStringProvider(dialogSettings.getConfigurationLayoutJSON());
        m_hideInWizard = dialogSettings.isHideInWizard();
        m_customCSS = dialogSettings.getCssStyles();

        final int inputLength = inports.size() + (m_reportConfiguration != null ? 1 : 0);
        m_inports = new NodeInPort[inputLength];
        m_inHiliteHandler = new HiLiteHandler[inports.size() - 1];
        for (int i = 0; i < inports.size(); i++) {
            m_inports[i] = new NodeInPort(i, DefToCoreUtil.toPortType(inports.get(i).getPortType()));
            if (i > 0) {
                // ignore optional variable input port
                m_inHiliteHandler[i - 1] = new HiLiteHandler();
            }
        }
        if (m_reportConfiguration != null) {
            final int lastPortIndex = m_inports.length - 1;
            m_inports[lastPortIndex] = createReportInputPort(lastPortIndex);
        }

        m_metadata = DefToCoreUtil.toComponentMetadata(def.getMetadata());
        m_templateInformation =
                MetaNodeTemplateInformation.createNewTemplate(def.getTemplateInfo(), TemplateType.SubNode);
    }

    /**
     * Create new SubNode from existing Metanode (=WorkflowManager).
     *
     * @param parent hosting parent WFM
     * @param id The ID of this node
     * @param content The metanode instance
     * @param name The name of the sub node
     */
    static SubNodeContainer newSubNodeContainerFromMetaNodeContent(final WorkflowManager parent, final NodeID id,
        final WorkflowManager content, final String name) {

        final PortType[] inTypes = IntStream.range(0, content.getNrInPorts()) //
                .mapToObj(content::getInPort) //
                .map(WorkflowInPort::getPortType) //
                .toArray(PortType[]::new);
        final PortType[] outTypes = IntStream.range(0, content.getNrOutPorts()) //
                .mapToObj(content::getOutPort) //
                .map(WorkflowOutPort::getPortType) //
                .toArray(PortType[]::new);

        final SubNodeContainer snc = new SubNodeContainer(parent, id, inTypes, outTypes, name);
        snc.getNodeAnnotation().copyFrom(content.getNodeAnnotation().getData(), true);


        // and copy content
        final WorkflowCopyContent c = WorkflowCopyContent.builder() //
                .setAnnotationIDs(content.getWorkflowAnnotationIDs().toArray(WorkflowAnnotationID[]::new)) //
                .setNodeIDs(content.getWorkflow().getNodeIDs().toArray(NodeID[]::new)) //
                .setIncludeInOutConnections(false) //
                .build();
        final WorkflowPersistor wp = content.copy(c);
        final WorkflowManager wfm = snc.m_wfm;
        final WorkflowCopyContent wcc = wfm.paste(wp);
        // create map of NodeIDs for quick lookup/search
        final Collection<NodeContainer> ncs = content.getNodeContainers();
        final NodeID[] orgIDs = ncs.stream().map(NodeContainer::getID).toArray(NodeID[]::new);
        final NodeID[] newIDs = wcc.getNodeIDs();
        final Map<NodeID, NodeID> oldIDsHash = new HashMap<>();
        for (int j = 0; j < orgIDs.length; j++) {
            oldIDsHash.put(orgIDs[j], newIDs[j]);
        }

        final NodeID inNodeID = snc.getVirtualInNodeID();
        final NodeID outNodeID = snc.getVirtualOutNodeID();

        final MinMaxCoordinates minMaxCoordinates = snc.getMinMaxCoordinates();

        for (ConnectionContainer cc : content.getWorkflow().getConnectionsBySource(content.getID())) {
            if (cc.getType() == ConnectionType.WFMTHROUGH) {
                wfm.addConnection(inNodeID, cc.getSourcePort() + 1, outNodeID, cc.getDestPort() + 1);
            } else {
                wfm.addConnection(inNodeID, cc.getSourcePort() + 1, oldIDsHash.get(cc.getDest()), cc.getDestPort());
            }
        }
        for (ConnectionContainer cc : content.getWorkflow().getConnectionsByDest(content.getID())) {
            if (cc.getType() == ConnectionType.WFMTHROUGH) {
                // wfm-through-connections have already been added above
                continue;
            }
            wfm.addConnection(oldIDsHash.get(cc.getSource()), cc.getSourcePort(), outNodeID, cc.getDestPort() + 1);
        }

        snc.placeVirtualInNode(minMaxCoordinates);
        snc.placeVirtualOutNode(minMaxCoordinates);
        return snc;
    }

    /**
     * Create new SubNode from existing Metanode (=WorkflowManager), only used via
     * {@link #newSubNodeContainerFromMetaNodeContent(WorkflowManager, NodeID, WorkflowManager, String)}.
     *
     * @param parent ...
     * @param id ...
     * @param inTypes Types of input ports (excl. flow var & report)
     * @param outTypes Types of output ports (excl. flow var & report)
     * @param name The name of the sub node
     */
    private SubNodeContainer(final WorkflowManager parent, final NodeID id, final PortType[] inTypes,
        final PortType[] outTypes, final String name) {
        super(parent, id);
        // Create new, internal workflow manager:
        m_wfm = new WorkflowManager(this, null, new NodeID(id, 0), new PortType[]{}, new PortType[]{}, false,
                parent.getContextV2(), name,
                Optional.of(parent.getWorkflowDataRepository()), Optional.empty());
        m_wfm.setJobManager(null);
        m_subnodeScopeContext = new FlowSubnodeScopeContext(this);
        // initialize NodeContainer inports
        // (metanodes don't have hidden variable port 0, SingleNodeContainers do!)
        m_inports = new NodeInPort[inTypes.length + 1];
        // but for hilite handler we still ignore optional variable port 0
        m_inHiliteHandler = new HiLiteHandler[inTypes.length];
        for (int i = 0; i < inTypes.length; i++) {
            m_inports[i + 1] = new NodeInPort(i + 1, inTypes[i]);
            m_inHiliteHandler[i] = new HiLiteHandler();
        }
        m_inports[0] = new NodeInPort(0, FlowVariablePortObject.TYPE_OPTIONAL);
        // initialize NodeContainer outports
        // (metanodes don't have hidden variable port 0, SingleNodeContainers do!)
        m_outports = new NodeContainerOutPort[outTypes.length + 1];
        m_outputs = new Output[outTypes.length + 1];
        for (int i = 0; i < outTypes.length; i++) {
            m_outputs[i + 1] = new Output(outTypes[i]);
            m_outports[i + 1] = new NodeContainerOutPort(this, outTypes[i], i + 1);
        }
        m_outputs[0] = new Output(FlowVariablePortObject.TYPE_OPTIONAL);
        m_outports[0] = new NodeContainerOutPort(this, FlowVariablePortObject.TYPE_OPTIONAL, 0);
        // add virtual in/out nodes and connect them
        m_virtualInNodeIDSuffix = addVirtualInNode(inTypes).getID().getIndex();
        m_virtualOutNodeIDSuffix = addVirtualOutNode(outTypes).getID().getIndex();
        m_wfmStateChangeListener = createAndAddStateListener();
        m_wfmListener = createAndAddWorkflowListener();
        setInternalState(m_wfm.getInternalState());
        m_templateInformation = MetaNodeTemplateInformation.NONE;
        m_metadata = ComponentMetadata.NONE;

        m_subnodeLayoutStringProvider = new SubnodeContainerLayoutStringProvider();
        m_subnodeConfigurationStringProvider = new SubnodeContainerConfigurationStringProvider();
        postLoadWFM();

    }

    /** Adds new/empty instance of a virtual input node and returns its ID. */
    private NodeContainer addVirtualInNode(final PortType[] inTypes) {
        final NodeID inNodeID = m_wfm.createAndAddNode(new VirtualSubNodeInputNodeFactory(this, inTypes));
        final NodeContainer inNodeNC = m_wfm.getNodeContainer(inNodeID);
        inNodeNC.setDeletable(false);
        return inNodeNC;
    }

    /** Moves the input node to the left-x, center-y of all nodes defined by the argument. */
    private void placeVirtualInNode(final MinMaxCoordinates minMaxCoordinates) {
        final int x = minMaxCoordinates.minX() - 100;
        final int y = minMaxCoordinates.centerY();
        getVirtualInNode().setUIInformation(NodeUIInformation.builder().setNodeLocation(x, y, 0, 0).build());
    }

    /** Adds new/empty instance of a virtual output node and returns its ID. */
    private NodeContainer addVirtualOutNode(final PortType[] outTypes) {
        final NodeID outNodeID = m_wfm.createAndAddNode(new VirtualSubNodeOutputNodeFactory(this, outTypes));
        final NodeContainer outNodeNC = m_wfm.getNodeContainer(outNodeID);
        outNodeNC.setDeletable(false);
        return outNodeNC;
    }

    /** Moves the output node to the right-x, center-y of all nodes defined by the argument. */
    private void placeVirtualOutNode(final MinMaxCoordinates minMaxCoordinates) {
        final int x = minMaxCoordinates.maxX() + 100;
        final int y = minMaxCoordinates.centerY();
        getVirtualOutNode().setUIInformation(NodeUIInformation.builder().setNodeLocation(x, y, 0, 0).build());
    }

    /** Return type of {@link #getMinMaxCoordinates()} - workflow bounding box. */
    private record MinMaxCoordinates(int minX, int minY, int maxX, int maxY) {
        /** @return the center position in the y direction. */
        int centerY() {
            return (minY + maxY) / 2;
        }
    }

    /** Iterates all nodes and determines min and max x,y coordinates. Used to place virtual in & output nodes.
     * @return the bounding box as {@link MinMaxCoordinates} */
    private MinMaxCoordinates getMinMaxCoordinates() {
        final Collection<NodeContainer> nodeContainers = m_wfm.getNodeContainers();
        final int nodeCount = nodeContainers.size();
        int xmin = nodeCount > 0 ? Integer.MAX_VALUE : 0;
        int ymin = nodeCount > 0 ? Integer.MAX_VALUE : 50;
        int xmax = nodeCount > 0 ? Integer.MIN_VALUE : 100;
        int ymax = nodeCount > 0 ? Integer.MIN_VALUE : 50;
        for (NodeContainer nc : nodeContainers) {
            NodeUIInformation uii = nc.getUIInformation();
            if (uii != null) {
                int[] bounds = uii.getBounds();
                if (bounds.length >= 2) {
                    xmin = Math.min(bounds[0], xmin);
                    ymin = Math.min(bounds[1], ymin);
                    xmax = Math.max(bounds[0], xmax);
                    ymax = Math.max(bounds[1], ymax);
                }
            }
        }
        return new MinMaxCoordinates(xmin, ymin, xmax, ymax);
    }

    /** Creates listener, adds it to m_wfm and sets the class field. */
    private WorkflowListener createAndAddWorkflowListener() {
        WorkflowListener listener = e -> onWFMStructureChange(e);
        m_wfm.addListener(listener, false);
        return listener;
    }

    /** Called by listener on inner workflow; will unset the {@link NodeMessage} when the inner workflow changes. */
    private void onWFMStructureChange(final WorkflowEvent state) {
        if (!m_isPerformingActionCalledFromParent) {
            switch (state.getType()) {
                case CONNECTION_ADDED:
                case CONNECTION_REMOVED:
                case NODE_ADDED:
                case NODE_REMOVED:
                case NODE_SETTINGS_CHANGED:
                    final NodeMessage msg = m_wfm.getNodeErrorSummary() //
                        .or(m_wfm::getNodeWarningSummary) //
                        .map(m -> m.toNodeMessage(Type.WARNING)) //
                        .orElse(NodeMessage.NONE);
                    setNodeMessage(msg);
                    final var virtualOutNode = getVirtualOutNode();
                    if (virtualOutNode.getInternalState().isExecuted()) {
                        m_wfm.resetAndConfigureNode(virtualOutNode.getID());
                    }
                    break;
                default:
                    // annotation change, dirty state change
            }
        }
    }

    /** Creates listener, adds it to m_wfm and sets the class field. */
    private NodeStateChangeListener createAndAddStateListener() {
        NodeStateChangeListener listener = e -> onWFMStateChange(e);
        m_wfm.addNodeStateChangeListener(listener);
        return listener;
    }

    /** Called by listener on inner workflow. */
    private void onWFMStateChange(final NodeStateEvent state) {
        if (!m_isPerformingActionCalledFromParent) {
            setNodeMessage(NodeMessage.NONE);
            final WorkflowManager parent = getParent();
            assert parent.isLockedByCurrentThread() : "Unsynchronized workflow state event";
            final InternalNodeContainerState oldState = getInternalState();
            final InternalNodeContainerState newState = state.getInternalNCState();
            final boolean gotReset = oldState.isExecuted() && !newState.isExecuted();
            if (gotReset) {
                // a contained node was reset -- trigger reset downstream
                parent.resetSuccessors(getID(), -1, true);
            }
            boolean outputChanged = setVirtualOutputIntoOutport(newState);
            setInternalState(newState);
            if (outputChanged) {
                parent.resetAndConfigureNodeAndSuccessors(getID(), false, false);
            } else {
                try (WorkflowLock parentLock = parent.lock()) {
                    parentLock.queueCheckForNodeStateChangeNotification(true);
                }
            }
        }
    }

    /* -------------------- Virtual node callbacks -------------- */

    /**
     * Called from virtual input node when it's executing. It's not triggering an upstream execute (different to 5.4).
     *
     * @return the subnode data input (incl. mandatory flow var port object).
     */
    public PortObject[] fetchInputDataFromParent() {
        final WorkflowManager parent = getParent();

        // might be not yet or no longer in workflow (e.g. part of construction)
        if (parent.containsNodeContainer(getID())) {
            PortObject[] results = new PortObject[getNrInPorts()];
            if (parent.assembleInputData(getID(), results)) {
                return results;
            }
        }
        return null;
    }

    /**
     * Called from the virtual input node when executed to get the actual input data. When this component represents a
     * component project, it will load and return the example input data. If this component is a regular "in workflow"
     * component, it will just retrieve the input data (no execution is triggered).
     *
     * @param exec for reading example data in
     *
     * @return the component data input (incl. mandatory flow var port object).
     */
    public PortObject[] fetchInputData(final ExecutionContext exec) {
        if (exec != null) {
            PortObject[] exampleInputData = fetchExampleInputData(exec);
            if (exampleInputData != null) {
                return exampleInputData;
            }
        }
        return fetchInputDataFromParent();
    }

    /** Fetches input specs of subnode, including mandatory flow var port. Used by virtual sub node input during
     * configuration.
     * @return input specs from subnode (as available from connected outports).
     */
    private PortObjectSpec[] fetchInputSpecFromParent() {
        PortObjectSpec[] results = new PortObjectSpec[getNrInPorts()];
        final WorkflowManager parent = getParent();
        // might be not yet or no longer in workflow (e.g. part of construction)
        if (parent.containsNodeContainer(getID())) {
            parent.assembleInputSpecs(getID(), results);
        }
        return results;
    }

    /**
     * Fetches input specs of a component, including mandatory flow var port. Used by virtual component input during
     * configuration.
     *
     * @return input specs from component (as available from connected outports).
     */
    public PortObjectSpec[] fetchInputSpec() {
        PortObjectSpec[] exampleInputSpec = fetchExampleInputSpec();
        if (exampleInputSpec != null) {
            return exampleInputSpec;
        }
        return fetchInputSpecFromParent();
    }

    private PortObject[] fetchExampleInputData(final ExecutionContext exec) {
        if (hasExampleInputData() && getNodeContainerDirectory() != null) {
            try {
                return FileSubNodeContainerPersistor.loadExampleInputData(
                    getTemplateInformation().getExampleInputDataInfo().get(),
                    getArtifactsDir(getNodeContainerDirectory()), exec);
            } catch (IOException | InvalidSettingsException | CanceledExecutionException ex) {
                //can happen in rare cases
                //e.g. if a data value implementation is not available (missing plugin)
                throw new IllegalStateException("Example input data cannot be read. See log for details.", ex);
            }
        } else {
            return null;
        }
    }

    private PortObjectSpec[] fetchExampleInputSpec() {
        if (hasExampleInputData() && getNodeContainerDirectory() != null) {
            if (m_exampleInputDataSpec == null) {
                try {
                    m_exampleInputDataSpec = FileSubNodeContainerPersistor.loadExampleInputSpecs(
                        getTemplateInformation().getExampleInputDataInfo().get(),
                        getArtifactsDir(getNodeContainerDirectory()));
                } catch (IOException | InvalidSettingsException ex) {
                    //can happen in rare cases
                    //e.g. if a data value implementation is not available (missing plugin)
                    throw new IllegalStateException(
                        "Example input data specification cannot be read. See log for details.", ex);
                }
            }
            return m_exampleInputDataSpec;
        } else {
            return null;
        }
    }

    /* -------------------- Subnode specific -------------- */

    /** @return the inportNodeModel */
    NativeNodeContainer getVirtualInNode() {
        return (NativeNodeContainer)m_wfm.getNodeContainer(getVirtualInNodeID());
    }

    /** @return the inportNodeModel */
    VirtualSubNodeInputNodeModel getVirtualInNodeModel() {
        return (VirtualSubNodeInputNodeModel)getVirtualInNode().getNodeModel();
    }

    /** @return the outportNodeModel */
    NativeNodeContainer getVirtualOutNode() {
        return (NativeNodeContainer)m_wfm.getNodeContainer(getVirtualOutNodeID());
    }

    /** @return the outportNodeModel */
    VirtualSubNodeOutputNodeModel getVirtualOutNodeModel() {
        return (VirtualSubNodeOutputNodeModel)getVirtualOutNode().getNodeModel();
    }

    /**
     * Iterate the "incoming" stack of the output node and throw an exception if a possible unclosed loop is detected.
     * (Prohibited for workflows created with 5.1.1+, see AP-20483.)
     *
     * @throws InvalidSettingsException if unclosed loops are detected
     * @noreference This method is not intended to be referenced by clients.
     * @since 5.2
     */
    public void checkForUnclosedLoopAtOutputNode() throws InvalidSettingsException {
        // the input stack of the output node - traverse it until we find subnode context. No open loop context
        // is expected (ideally also no other context but see details below)
        final var unclosedContextOptional = //
            StreamSupport.stream(getVirtualOutNode().getFlowObjectStack().spliterator(), false) //
                .takeWhile(Predicate.not(FlowSubnodeScopeContext.class::isInstance)) //
                .filter(FlowLoopContext.class::isInstance) //
                .map(FlowLoopContext.class::cast) //
                .findFirst();

        // ideally we would disallow any context (except for subnode context) but that seems overly
        // restrictive since:
        //    - try-catch constructs are often unclosed.
        //    - in version of KNIME before 5.1.1 (and also now) a component output could always be in any scope
        //      (whereby only loop scopes are plain wrong) and this would cause a lot of potential backward comp. issues
        if (unclosedContextOptional.isPresent()) {
            final var wfm = getWorkflowManager();
            final var startNodeID = unclosedContextOptional.get().getHeadNode();
            final var startNodeName = wfm.getNodeContainer(startNodeID).getNameWithID(wfm.getID());
            throw Message.builder() //
                .withSummary("Output node must not be part of a loop construct.") //
                .addTextIssue("Node is part of loop body (started by %s).".formatted(startNodeName)) //
                .addResolutions( //
                    "Move this node downstream of the loop end node.", "Move loop outside the component.") //
                .build().orElseThrow() //
                .toInvalidSettingsException();
        }
    }

    /** Static utility node to retrieve the nodemodel of the virtual output node. It does the type casts etc.
     * @param wfm The wfm to query (inner wfm)
     * @param nodeIDSuffix the id of the output node (must exist)
     * @return the node model of the output node.
     */
    static VirtualSubNodeOutputNodeModel getVirtualOutputNodeModel(final WorkflowManager wfm, final int nodeIDSuffix) {
        NativeNodeContainer nnc = (NativeNodeContainer)wfm.getNodeContainer(new NodeID(wfm.getID(), nodeIDSuffix));
        return (VirtualSubNodeOutputNodeModel)nnc.getNodeModel();
    }

    /** Static utility node to retrieve the nodemodel of the virtual input node. It does the type casts etc.
     * @param wfm The wfm to query (inner wfm)
     * @param nodeIDSuffix the id of the input node (must exist)
     * @return the node model of the input node.
     */
    static VirtualSubNodeInputNodeModel getVirtualInputNodeModel(final WorkflowManager wfm, final int nodeIDSuffix) {
        NativeNodeContainer nnc = (NativeNodeContainer)wfm.getNodeContainer(new NodeID(wfm.getID(), nodeIDSuffix));
        return (VirtualSubNodeInputNodeModel)nnc.getNodeModel();
    }

    /** @return the inNodeID */
    public NodeID getVirtualInNodeID() {
        return new NodeID(m_wfm.getID(), m_virtualInNodeIDSuffix);
    }

    /** @return the outNodeID */
    public NodeID getVirtualOutNodeID() {
        return new NodeID(m_wfm.getID(), m_virtualOutNodeIDSuffix);
    }

    /** Used by test framework to enforce a programmatically collapsed subnode to export all flow variables. */
    void updateOutputConfigurationToIncludeAllFlowVariables() {
        try (WorkflowLock lock = lock()) {
            getVirtualOutNodeModel().updateConfigIncludeAllFlowVariables();
            getWorkflowManager().saveNodeSettingsToDefault(getVirtualOutNodeID());
        }
    }

    /**
     * @return a DOM which describes only the port connections.
     */
    public Element getXMLDescriptionForPorts() {
        try {
            // Document
            final Document doc =
                NodeDescription.getDocumentBuilderFactory().newDocumentBuilder().getDOMImplementation()
                      .createDocument("http://knime.org/node2012", "knimeNode", null);

            addPortDescriptionToElement(doc, getMetadata(), getNrInPorts(), getNrOutPorts());

            // we avoid validating the document since we don't include certain elements like 'name'
            return (new NodeDescription27Proxy(doc, false)).getXMLDescription();
        } catch (ParserConfigurationException | DOMException | XmlException e) {
            LOGGER.warn("Could not generate ports description", e);
        }

        return null;
    }

    private static void addPortDescriptionToElement(final Document doc, final ComponentMetadata metadata,
        final int nrInPorts, final int nrOutPorts) throws DOMException, ParserConfigurationException, XmlException {
        final String[] inPortNames = metadata.getInPortNames().orElse(new String[nrInPorts - 1]);
        final String[] inPortDescriptions = metadata.getInPortDescriptions().orElse(new String[nrInPorts - 1]);
        final String[] outPortNames = metadata.getOutPortNames().orElse(new String[nrOutPorts - 1]);
        final String[] outPortDescriptions = metadata.getOutPortDescriptions().orElse(new String[nrOutPorts - 1]);

        final Element knimeNode = doc.getDocumentElement();
        final Element ports = doc.createElement("ports");
        knimeNode.appendChild(ports);
        // inPort
        for (int i = 0; i < inPortNames.length; i++) {
            final Element inPort = doc.createElement("inPort");
            ports.appendChild(inPort);
            inPort.setAttribute("index", "" + i);
            inPort.setAttribute("name", inPortNames[i]);
            String defaultText = NO_DESCRIPTION_SET;
            if (i == 0) {
                defaultText += "\nChange this label by opening the Component "
                        + "and editing its metadata.";
            }
            addText(inPort, inPortDescriptions[i], defaultText);
        }
        // outPort
        for (int i = 0; i < outPortNames.length; i++) {
            final Element outPort = doc.createElement("outPort");
            ports.appendChild(outPort);
            outPort.setAttribute("index", "" + i);
            outPort.setAttribute("name", outPortNames[i]);
            String defaultText = NO_DESCRIPTION_SET;
            if (i == 0) {
                defaultText += "\nChange this label by opening the Component "
                        + "and editing its metadata.";
            }
            addText(outPort, outPortDescriptions[i], defaultText);
        }
    }

    /* -------------------- NodeContainer info properties -------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getXMLDescription() {
        final String description = getMetadata().getDescription().orElse("");
        String sDescription;
        if (StringUtils.isEmpty(description)) {
            sDescription = "";
        } else {
            sDescription = StringUtils.split(description, ".\n")[0];
            sDescription = StringUtils.abbreviate(sDescription, 200);
        }

        final List<String> optionNames = new ArrayList<>();
        final List<String> optionDescriptions = new ArrayList<>();
        for (SubNodeDescriptionProvider<? extends DialogNodeValue> desc : getDialogDescriptions()) {
            optionNames.add(desc.getLabel());
            optionDescriptions.add(desc.getDescription());
        }
        try {
            // Document
            final Document doc =
                NodeDescription.getDocumentBuilderFactory().newDocumentBuilder().getDOMImplementation()
                      .createDocument("http://knime.org/node2012", "knimeNode", null);
            // knimeNode
            final Element knimeNode = doc.getDocumentElement();
            knimeNode.setAttribute("type", "Unknown");
            knimeNode.setAttribute("icon", "subnode.png");
            // name
            final Element name = doc.createElement("name");
            knimeNode.appendChild(name);
            name.appendChild(doc.createTextNode(getName()));
            // shortDescription
            final Element shortDescription = doc.createElement("shortDescription");
            knimeNode.appendChild(shortDescription);
            addText(shortDescription, sDescription, NO_DESCRIPTION_SET);
            // fullDescription
            final Element fullDescription = doc.createElement("fullDescription");
            knimeNode.appendChild(fullDescription);
            // intro
            final Element intro = doc.createElement("intro");
            fullDescription.appendChild(intro);
            addText(intro, description, NO_DESCRIPTION_SET + "\nIn order to set a description browse the input node "
                    + "contained in the Component and change its configuration.");
            // option
            for (int i = 0; i < optionNames.size(); i++) {
                final Element option = doc.createElement("option");
                fullDescription.appendChild(option);
                option.setAttribute("name", optionNames.get(i));
                addText(option, optionDescriptions.get(i), "");
            }

            addPortDescriptionToElement(doc, getMetadata(), getNrInPorts(), getNrOutPorts());

            return new NodeDescription27Proxy(doc).getXMLDescription();
        } catch (ParserConfigurationException | DOMException | XmlException e) {
            LOGGER.warn("Could not generate Component description", e);
        }
        return null;
    }

    /**
     * @return a list of descriptions for all the visible dialog options
     * @since 4.3
     */
    @SuppressWarnings({"java:S1452", "java:S3740", "rawtypes"}) // raw types
    public List<SubNodeDescriptionProvider<? extends DialogNodeValue>> getDialogDescriptions() {

        Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, new EnabledDialogNodeModelFilter(), false);

        List<Integer> order = ConfigurationLayoutUtil.getConfigurationOrder(
            m_subnodeConfigurationStringProvider, nodes, m_wfm);

        // Will contain the nodes in the ordering given by `order`.
        // Nodes not mentioned in `order` will be placed at the end in arbitrary order.
        TreeMap<Integer, DialogNode> orderedNodes = new TreeMap<>();
        List<DialogNode> unorderedNodes = new ArrayList<>(nodes.size());
        nodes.forEach((nodeId, metaNodeDialogNode) -> {
            int targetIndex = order.indexOf(nodeId.getIndex());
            if (targetIndex == -1) {
                unorderedNodes.add(metaNodeDialogNode);
            } else {
                orderedNodes.put(targetIndex, metaNodeDialogNode);
            }
        });
        List<DialogNode> res = new ArrayList<>();
        res.addAll(orderedNodes.values()); // `values` is ordered
        res.addAll(unorderedNodes);

        return res.stream().map(DialogNode::getDialogRepresentation)
            .filter(r -> r instanceof SubNodeDescriptionProvider).map(p -> (SubNodeDescriptionProvider)p)
            .collect(toList());
    }

    private void refreshPortNames() {
        String[] inPortNames;
        String[] outPortNames;

        final var numInPorts = getNrInPorts() - 1;
        final var numOutPorts = getNrOutPorts() - 1;
        if (m_metadata == null) {
            inPortNames = new String[numInPorts];
            outPortNames = new String[numOutPorts];
        } else {
            /* sync number of port names/descriptions with the actual ports number -> fill or cut-off */
            m_metadata = m_metadata.withNumberOfPorts(numInPorts, numOutPorts);
            inPortNames = m_metadata.getInPortNames().orElse(new String[numInPorts]);
            outPortNames = m_metadata.getOutPortNames().orElse(new String[numOutPorts]);
        }
        for (var i = 0; i < inPortNames.length; i++) {
            getInPort(i + 1).setPortName(inPortNames[i]);
        }
        for (var i = 0; i < outPortNames.length; i++) {
            getOutPort(i + 1).setPortName(outPortNames[i]);
        }
        notifyUIListeners(null);
    }

    private class RefreshPortNamesListener implements NodeStateChangeListener {
        @Override
        public void stateChanged(final NodeStateEvent state) {
            refreshPortNames();
        }
    }

    /**
     * Puts the text into the element while replacing new lines with &lt;br /> elements.
     *
     * @param element The element to add to
     * @param text The text to add
     * @param defaultTextIfEmpty Text to show if <code>text</code> is empty.
     */
    private static void addText(final Element element, final String text, final String defaultTextIfEmpty) {
        Document doc = element.getOwnerDocument();
        String[] splitText = (StringUtils.isEmpty(text) ? defaultTextIfEmpty : text).split("\n");
        for (int i = 0; i < splitText.length; i++) {
            element.appendChild(doc.createTextNode(splitText[i]));
            if (i + 1 < splitText.length) {
                element.appendChild(doc.createElement("br"));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getIcon() {
        if (m_metadata.getIcon().isPresent()) {
            return null;
        } else {
            return SubNodeContainer.class.getResource("virtual/subnode/empty.png");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getIconAsStream() {
        return m_metadata.getIcon().map(ByteArrayInputStream::new).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getType() {
        return m_metadata.getNodeType().map(t -> t.getType()).orElse(NodeType.Subnode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_wfm.getName();
    }

    /**
     * @param name The new name
     * @since 2.10
     */
    public void setName(final String name) {
        m_wfm.setName(name);
        setDirty();
        notifyNodePropertyChangedListener(NodeProperty.Name);
    }

    /* ------------------- Specific Interna ------------------- */

    /**
     * @return underlying workflow.
     */
    public WorkflowManager getWorkflowManager() {
        return m_wfm;
    }

    /* -------------------- Dialog Handling ------------------ */

    private NodeDialogPane m_nodeDialogPane;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        int c = NodeExecutionJobManagerPool.getNumberOfJobManagersFactories();
        return c > 1 || m_wfm.findNodes(DialogNode.class, true).size() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    NodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs, final PortObject[] inData)
        throws NotConfigurableException {
        NodeDialogPane dialogPane = getDialogPane();
        // find all dialog nodes and update subnode dialog
        Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, new NodeModelFilter<DialogNode>() { // NOSONAR
            @Override
            public boolean include(final DialogNode nodeModel) {
                return !nodeModel.isHideInDialog();
            }
        }, false);

        List<Integer> order = ConfigurationLayoutUtil.getConfigurationOrder(m_subnodeConfigurationStringProvider, nodes, m_wfm);
        ((MetaNodeDialogPane)dialogPane).setQuickformNodes(nodes, order);
        NodeSettings settings = new NodeSettings("subnode_settings");
        saveSettings(settings);
        // remove the flow variable port from the specs and data
        PortObjectSpec[] correctedInSpecs = ArrayUtils.remove(inSpecs, 0);
        PortObject[] correctedInData = ArrayUtils.remove(inData, 0);
        // the next call will call dialogPane.internalLoadSettingsFrom()
        // dialogPane is a MetaNodeDialogPane and does not handle the flow variable port correctly
        // this is why we remove it first
        Node.invokeDialogInternalLoad(dialogPane, settings, correctedInSpecs, correctedInData,
            getFlowObjectStack(), new CredentialsProvider(this, m_wfm.getCredentialsStore()),
            getParent().isWriteProtected());
        return dialogPane;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NodeDialogPane getDialogPane() {
        if (m_nodeDialogPane == null) {
            if (hasDialog()) {
                // create sub node dialog with dialog nodes
                m_nodeDialogPane = new MetaNodeDialogPane(MetaNodeDialogType.SUBNODE);
                // job managers tab
                if (NodeExecutionJobManagerPool.getNumberOfJobManagersFactories() > 1) {
                    // TODO: set the SplitType depending on the nodemodel
                    SplitType splitType = SplitType.USER;
                    m_nodeDialogPane.addJobMgrTab(splitType);
                }
                Node.addMiscTab(m_nodeDialogPane);
            } else {
                throw new IllegalStateException("Workflow has no dialog");
            }
        }
        return m_nodeDialogPane;
    }

    /* -------------------- Views ------------------ */

    // TODO: enable view handling!
    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeViewName(final int i) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractNodeView<NodeModel> getNodeView(final int i) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInteractiveView() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInteractiveViewName() {
        //TODO: custom view name?
        return getCustomName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V extends AbstractNodeView<?> & InteractiveView<?, ? extends ViewContent, ? extends ViewContent>> V
        getInteractiveView() {
        return null;
    }

    /** A {@link InteractiveWebViewsResult} for a subnode containing the set of all wizard views in it (no recursion).
     * {@inheritDoc} */
    @Override
    public InteractiveWebViewsResult getInteractiveWebViews() {
        return getInteractiveWebViews(false);
    }

    /**
     * A {@link InteractiveWebViewsResult} for a subnode containing either a single page view on all appropriate wizard views or a set of all individual wizard views (no recursion).
     *
     * @param combinedView
     * @return
     *  */
    public InteractiveWebViewsResult getInteractiveWebViews(final boolean combinedView) {
        try (WorkflowLock lock = m_wfm.lock()) {
            Builder builder = InteractiveWebViewsResult.newBuilder();
            if (combinedView) {

            } else {
                // collect all the nodes first, then do make names unique (in case there are 2+ scatterplot)
                NativeNodeContainer[] nodesWithViews = m_wfm.getWorkflow().getNodeValues().stream()
                        .filter(n -> n instanceof NativeNodeContainer)
                        .filter(n -> n.getInteractiveWebViews().size() > 0)
                        .map(n -> (NativeNodeContainer)n)
                        .toArray(NativeNodeContainer[]::new);

                // count how often certain names are in use
                Map<String, Long> uniqueNameBag = Arrays.stream(nodesWithViews)
                        .map(n -> n.getInteractiveViewName())
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                for (NativeNodeContainer n : nodesWithViews) {
                    String name = n.getInteractiveViewName();
                    if (uniqueNameBag.get(name) >= 2L) {
                        name = name.concat(" (ID " + n.getID().getIndex() + ")");
                    }
                    builder.add(n, name);
                }
            }
            return builder.build();
        }
    }

    /** This is ambiguous. A sub node currently doesn't support ExecutionEnvironments (in fact NativeNodeContainer
     * doesn't either). This method is overridden to avoid null checks in the super class.
     * {@inheritDoc} */
    @Override
    protected void setExecutionEnvironment(final ExecutionEnvironment exEnv) {
        // Deliberately empty
        // Super class implementation checks null assignment of the field variable - but a sub node can make many
        // state transitions from configured -> configured  -> ... which conflicts with the state checks in the super
        // implementation
    }

    /* -------------- Configuration/Execution ------------------- */


    /** {@inheritDoc} */
    @Override
    void performReset() {
        setNodeMessage(NodeMessage.NONE);
        runParentAction(() -> {
            m_wfm.resetAllNodesInWFM();
            setVirtualOutputIntoOutport(m_wfm.getInternalState());
        });
    }

    /** {@inheritDoc} */
    @Override
    boolean performConfigure(final PortObjectSpec[] rawInSpecs, final NodeConfigureHelper nch,
        final boolean keepNodeMessage) {
        assert rawInSpecs.length == m_inports.length;
        final boolean isInInactiveScope = getFlowObjectStack().peekScopeContext(FlowScopeContext.class, true) != null;
        m_subnodeScopeContext.inactiveScope(isInInactiveScope || Node.containsInactiveSpecs(rawInSpecs));
        final NodeMessage oldMessage;
        if (keepNodeMessage) {
            oldMessage = getNodeMessage();
        } else {
            setNodeMessage(null);
            oldMessage = NodeMessage.NONE;
        }

        m_isPerformingActionCalledFromParent = true;
        try {
            if (nch != null && !m_subnodeScopeContext.isInactiveScope()) {
                try {
                    nch.preConfigure();
                } catch (InvalidSettingsException ise) {
                    LOGGER.warn(ise.getMessage(), ise);
                    NodeMessage nodeMsg = NodeMessage.newWarning(ise.getMessage());
                    setNodeMessage(keepNodeMessage ? NodeMessage.merge(oldMessage, nodeMsg) : nodeMsg);
                    return false;
                }
            }
            // and launch a configure on entire sub workflow
            InternalNodeContainerState oldState = getInternalState();
            m_wfm.reconfigureAllNodesOnlyInThisWFM(keepNodeMessage);
            final InternalNodeContainerState internalState = m_wfm.getInternalState();
            InternalNodeContainerState newState;
            switch (internalState) {
                case IDLE:
                    newState = oldState.isExecutionInProgress()
                        ? InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC : InternalNodeContainerState.IDLE;
                    break;
                case CONFIGURED:
                    newState = oldState.isExecutionInProgress()
                        ? InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC : InternalNodeContainerState.CONFIGURED;
                    break;
                default:
                    newState = internalState;
            }
            m_wfm.getNodeErrorSummary() //
                .or(m_wfm::getNodeWarningSummary) //
                .map(m -> m.toNodeMessage(Type.WARNING)) //
                .ifPresent(m -> setNodeMessage(
                    keepNodeMessage ? mergeNodeMessagesAndRespectExecuteFailedPrefix(oldMessage, m) : m));
            setVirtualOutputIntoOutport(newState);
            setInternalState(newState);
            if (nch != null && !m_subnodeScopeContext.isInactiveScope()) {
                try {
                    nch.postConfigure(rawInSpecs, null);
                } catch (InvalidSettingsException ise) {
                    LOGGER.warn(ise.getMessage(), ise);
                    NodeMessage nodeMsg = NodeMessage.newWarning(ise.getMessage());
                    setNodeMessage(keepNodeMessage ? NodeMessage.merge(oldMessage, nodeMsg) : nodeMsg);
                    return false;
                }
            }
            return internalState.isConfigured();
        } finally {
            m_isPerformingActionCalledFromParent = false;
        }
    }


    /**
     * Merges two nodes messages but ignores the {@link Node#EXECUTE_FAILED_PREFIX} (+ '\n') in m1 for message
     * comparison and adds the prefix back again (if it was present) to the start of the merged message.
     *
     * If the message types differ, the more severe message (error) is returned only.
     *
     * The prefix for SNC-node messages is set in {@link #performExecuteNode(PortObject[])}.
     *
     * TODO: a message prefix could eventually be added to {@link NodeMessage} itself - but a bit of an overkill for the
     * time being with possible unknown side-effects
     */
    private static NodeMessage mergeNodeMessagesAndRespectExecuteFailedPrefix(final NodeMessage m1, final NodeMessage m2) {
        if (m1.getMessageType() != m2.getMessageType()) {
            if (m1.getMessageType() == Type.ERROR) {
                return m1;
            } else {
                return m2;
            }
        }
        NodeMessage m1WithoutPrefix;
        if (m1.getMessage().startsWith(Node.EXECUTE_FAILED_PREFIX + "\n")) {
            m1WithoutPrefix = new NodeMessage(m1.getMessageType(),
                StringUtils.removeStart(m1.getMessage(), Node.EXECUTE_FAILED_PREFIX + "\n"));
        } else {
            m1WithoutPrefix = m1;
        }

        NodeMessage res = NodeMessage.merge(m1WithoutPrefix, m2);
        //add prefix back again
        if (m1WithoutPrefix != m1) {
            return new NodeMessage(res.getMessageType(), Node.EXECUTE_FAILED_PREFIX + "\n" + res.getMessage());
        } else {
            return res;
        }
    }

    /** {@inheritDoc} */
    @Override
    boolean performStateTransitionQUEUED() {
        // theoretically we can come from any state into queued state, e.g. this node can be marked for
        // execution (which is the most likely state when run from the outer workflow) and then something is done
        // internally that causes an internal checkForNodeStateChanges.
        setInternalState(InternalNodeContainerState.CONFIGURED_QUEUED);
        return true;
    }

    private void runIfInExternalExecutor(final Runnable r) {
        if (!m_wfm.isLocalWFM() && !m_subnodeScopeContext.isInactiveScope()) {
            runParentAction(r);
        }
    }

    /** {@inheritDoc} */
    @Override
    boolean performStateTransitionPREEXECUTE() {
        synchronized (m_nodeMutex) {
            getProgressMonitor().reset();
            switch (getInternalState()) {
            case EXECUTED_QUEUED:
            case CONFIGURED_QUEUED:
                runIfInExternalExecutor(() -> m_wfm.mimicRemotePreExecute());
                setInternalState(InternalNodeContainerState.PREEXECUTE);
                return true;
            default:
                // ignore any other state: other states indicate that the node
                // was canceled before it is actually run
                // (this method is called from a worker thread, whereas cancel
                // typically from the UI thread)
                if (!Thread.currentThread().isInterrupted()) {
                    LOGGER.debug("Execution of node " + getNameWithID()
                            + " was probably canceled (node is " + getInternalState()
                            + " during 'preexecute') but calling thread is not"
                            + " interrupted");
                }
                return false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionEXECUTING() {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
                case PREEXECUTE:
                    if (NodeExecutionJobManagerPool.isThreaded(findJobManager())) {
                        setInternalState(InternalNodeContainerState.EXECUTING);
                    } else {
                        runIfInExternalExecutor(() -> m_wfm.mimicRemoteExecuting());
                        setInternalState(InternalNodeContainerState.EXECUTINGREMOTELY);
                    }
                    break;
                default:
                    throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionPOSTEXECUTE() {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case PREEXECUTE: // in case of errors, e.g. flow stack problems
                             // encountered during doBeforeExecution
            case EXECUTING:
            case EXECUTINGREMOTELY:
                runIfInExternalExecutor(() -> m_wfm.mimicRemotePostExecute());
                setInternalState(InternalNodeContainerState.POSTEXECUTE);
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionEXECUTED(
            final NodeContainerExecutionStatus status) {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case POSTEXECUTE:
                NodeContainerExecutionStatus wfmStatus = status instanceof SubnodeContainerExecutionResult ?
                    ((SubnodeContainerExecutionResult)status).getWorkflowExecutionResult() : status;
                runIfInExternalExecutor(() -> m_wfm.mimicRemoteExecuted(wfmStatus));
                InternalNodeContainerState newState = status.isSuccess() ?
                    InternalNodeContainerState.EXECUTED : m_wfm.getInternalState();
                setVirtualOutputIntoOutport(newState);
                setInternalState(newState);
                // don't reset and configure (like a NativeNodeContainer) for easier error inspection in case of failure
                setExecutionJob(null);
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerExecutionStatus performExecuteNode(final PortObject[] rawInObjects) {
        setNodeMessage(NodeMessage.NONE);
        assert rawInObjects.length == m_inports.length;
        FlowScopeContext peekfsc = getFlowObjectStack().peekScopeContext(FlowScopeContext.class, true);
        if (m_subnodeScopeContext.isInactiveScope() || peekfsc != null) {
            // subnode itself is inactive (received inactive inputs) or is contained in an inactive scope (AP-19271)
            setInactive();
            return NodeContainerExecutionStatus.SUCCESS;
        }
        m_isPerformingActionCalledFromParent = true;
        try {
            // launch execute on entire sub workflow and then wait for inner workflow to finish -
            // mark this thread as idle to avoid deadlock situation
            m_wfm.executeAll();
            boolean isCanceled;
            try {
                isCanceled = ThreadPool.currentPool().runInvisible(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Boolean result = Boolean.FALSE;
                        while (true) { // don't finish execution if there are nodes inside still executing (AP-11710)
                            try {
                                m_wfm.waitWhileInExecution(-1, TimeUnit.SECONDS);
                                return result;
                            } catch (InterruptedException e) { // NOSONAR interrupt is handled by WFM cancellation
                                m_wfm.cancelExecution();
                                result = Boolean.TRUE;
                            }
                        }
                    }
                });
            } catch (ExecutionException ee) {
                isCanceled = false;
                LOGGER.error(ee.getCause().getClass().getSimpleName() + " while waiting for inner workflow to complete",
                    ee);
            } catch (final InterruptedException e) { // NOSONAR interrupt is handled by WFM cancellation
                isCanceled = true;
            }
            final InternalNodeContainerState internalState = m_wfm.getInternalState();
            boolean allExecuted = internalState.isExecuted();
            if (allExecuted) {
                setVirtualOutputIntoOutport(internalState);
            } else if (isCanceled) {
                setNodeMessage(NodeMessage.newWarning("Execution canceled"));
            } else {
                final var nodeMessage = m_wfm.getNodeErrorSummary() //
                        .or(m_wfm::getNodeWarningSummary) //
                        .map(m -> m.toNodeMessage(Type.ERROR))
                        .orElse(NodeMessage.newError("<reason unknown>"));
                setNodeMessage(nodeMessage);
                return NodeContainerExecutionStatus.newFailure(nodeMessage.getMessage());
            }
            return allExecuted ? NodeContainerExecutionStatus.SUCCESS : NodeContainerExecutionStatus.FAILURE;
        } finally {
            m_isPerformingActionCalledFromParent = false;
        }
    }

    @Override
    void markForExecution(final boolean flag) {
        super.markForExecution(flag);
        runIfInExternalExecutor(() -> m_wfm.markForExecution(flag));
    }

    @Override
    void mimicRemotePreExecute() {
        super.mimicRemotePreExecute();
        runParentAction(() -> m_wfm.mimicRemotePreExecute());
    }

    @Override
    void mimicRemotePostExecute() {
        super.mimicRemotePostExecute();
        runParentAction(() -> m_wfm.mimicRemotePostExecute());
    }


    @Override
    void mimicRemoteExecuting() {
        super.mimicRemoteExecuting();
        runParentAction(() -> m_wfm.mimicRemoteExecuting());
    }


    @Override
    void mimicRemoteExecuted(final NodeContainerExecutionStatus status) {
        super.mimicRemoteExecuted(status);
        runParentAction(() -> m_wfm.mimicRemoteExecuted(status.getChildStatus(0)));
    }

    /** Copies data from virtual output node into m_outputs, notifies state listeners if not
     * processing call from parent.
     * @param newState State of the internal WFM to decide whether to publish ports and/or specs. */
    @SuppressWarnings("null")
    private boolean setVirtualOutputIntoOutport(final InternalNodeContainerState newState) {
        // retrieve results and copy to outports
        final VirtualSubNodeOutputNodeModel virtualOutNodeModel = getVirtualOutNodeModel();
        final boolean isInactive = getVirtualOutNode().isInactive();
        final VirtualSubNodeExchange outputExchange = virtualOutNodeModel.getOutputExchange();

        // put objects into output if state of WFM is executed
        boolean publishObjects = newState.isExecuted();
        // publishObjects implies that output node has data or is inactive
        assert !publishObjects || (isInactive || (outputExchange != null && outputExchange.getPortObjects() != null)) :
            String.format("output node must have data or be inactive, status: %s, inactive: %b, exhange is null: %b, "
                + "exchange content is null: %s", newState, isInactive, outputExchange == null,
                outputExchange == null ? "<invalid>" : String.valueOf(outputExchange.getPortObjects() == null));

        boolean publishSpecs = (isInactive || outputExchange != null)
                && (newState.isConfigured() || newState.isExecuted() || newState.isExecutionInProgress());

        boolean changed = false;
        for (int i = 1; i < m_outputs.length; i++) {
            // not publish spec:     null output
            // inactive output node: inactive branch port object
            // otherwise:            use data from output node
            final PortObjectSpec spec = publishSpecs ? (isInactive ? InactiveBranchPortObjectSpec.INSTANCE
                : outputExchange.getPortSpecs()[i - 1]) : null;
            changed = m_outputs[i].setSpec(spec) || changed;

            final PortObject object = publishObjects ? (isInactive ? InactiveBranchPortObject.INSTANCE
                : outputExchange.getPortObjects()[i - 1]) : null;
            changed = m_outputs[i].setObject(object) || changed;
        }
        final PortObjectSpec spec = publishSpecs ? (isInactive ? InactiveBranchPortObjectSpec.INSTANCE
            : FlowVariablePortObjectSpec.INSTANCE) : null;
        changed = m_outputs[0].setSpec(spec) || changed;

        final PortObject object = publishObjects ? (isInactive ? InactiveBranchPortObject.INSTANCE
            : FlowVariablePortObject.INSTANCE) : null;
        changed = m_outputs[0].setObject(object) || changed;

        final FlowObjectStack outgoingFlowObjectStack = getOutgoingFlowObjectStack();
        // only send flow variables downstream if the subnode is executed - otherwise there may be inconsistencies
        // between a previous #configure and the subsequent #execute (nodes turn inactive and don't publish variables
        // anymore -- check test case org.knime.core.node.workflow.TestSubnode_VariableScopeDuringExecution)
        // this can be fixed by...
        // TODO API to remove variables from stack, then remove variables no longer in output node and update "changed"
        if (publishObjects && !isInactive) {
            for (FlowVariable f : outputExchange.getFlowVariables()) {
                outgoingFlowObjectStack.push(f.cloneAndUnsetOwner());
            }
        } else {
            // outgoing stack may be null if reset is called twice in a row (or once but no configure was called)
            if (outgoingFlowObjectStack != null) {
                while (!outgoingFlowObjectStack.isEmpty()) {
                    outgoingFlowObjectStack.pop(FlowObject.class);
                }
            }
        }
        if (changed && !m_isPerformingActionCalledFromParent) {
            notifyStateChangeListeners(new NodeStateEvent(this)); // updates port views
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void cleanup() {
        super.cleanup();
        getVirtualInNodeModel().setSubNodeContainer(null);
        getVirtualOutNodeModel().setSubNodeContainer(null);
        m_wfm.removeNodeStateChangeListener(m_wfmStateChangeListener);
        m_wfm.removeListener(m_wfmListener);
        m_wfm.cleanup();
        m_subnodeScopeContext.cleanup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionContext createExecutionContext() {
        return getVirtualOutNode().createExecutionContext();
    }

    /**
     * {@inheritDoc}
     * @since 2.12
     */
    @Override
    public SubnodeContainerExecutionResult createExecutionResult(final ExecutionMonitor exec)
            throws CanceledExecutionException {
        try (WorkflowLock lock = lock()) {
            SubnodeContainerExecutionResult result = new SubnodeContainerExecutionResult(getID());
            super.saveExecutionResult(result);
            WorkflowExecutionResult innerResult = m_wfm.createExecutionResult(exec);
            if (innerResult.needsResetAfterLoad()) {
                result.setNeedsResetAfterLoad();
            }
            // innerResult is success as soon as one of the nodes is a success - be more strict here
            result.setSuccess(innerResult.isSuccess() && getInternalState().equals(EXECUTED));
            result.setWorkflowExecutionResult(innerResult);
            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void loadExecutionResult(final NodeContainerExecutionResult result, final ExecutionMonitor exec, final LoadResult loadResult) {
        CheckUtils.checkArgument(result instanceof SubnodeContainerExecutionResult,
            "Argument must be instance of \"%s\": %s", SubnodeContainerExecutionResult.class.getSimpleName(),
            result == null ? "null" : result.getClass().getSimpleName());
        SubnodeContainerExecutionResult r = (SubnodeContainerExecutionResult)result;
        try (WorkflowLock lock = lock()) {
            super.loadExecutionResult(result, exec, loadResult);
            WorkflowExecutionResult innerExecResult = r.getWorkflowExecutionResult();
            runParentAction(() -> getWorkflowManager().loadExecutionResult(innerExecResult, exec, loadResult));
            // After loading the execution result of the workflow manager we need to set the real output of the subnode
            if (r.isSuccess()) {
                setVirtualOutputIntoOutport(EXECUTED);
            }
        }
    }

    private void runParentAction(final Runnable r) {
        boolean wasFlagSet = m_isPerformingActionCalledFromParent;
        m_isPerformingActionCalledFromParent = true;
        try {
            r.run();
        } finally {
            m_isPerformingActionCalledFromParent = wasFlagSet;
        }
    }

    /**
     * Inspector of a {@link NodeStateEvent} that checks if the event will cause the entire component to fail. Added as
     * part of AP-13583.
     *
     * @param e The event to investigate
     * @return {@code true}, if the component execution will fail after this event, {@code false otherwise}
     */
    public boolean isThisEventFatal(final NodeStateEvent e) {
        var node = m_wfm.getNodeContainer(e.getSource());
        var isNodeMessageError = node.getNodeMessage().getMessageType() == Type.ERROR;
        var isNodeExecuting = e.getInternalNCState().isExecutionInProgress();
        var areErrorsCaught = node.getFlowObjectStack().peek(FlowTryCatchContext.class) != null;
        return !isNodeExecuting && isNodeMessageError && !areErrorsCaught;
    }

    /* ------------- Ports and related stuff --------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrInPorts() {
        return m_inports.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeInPort getInPort(final int index) {
        return m_inports[index];
    }

    /**
     * @param portTypes Types of the new ports (excludes both the mickey mouse port and the report port)
     * @param enableReportOutput true to enable report type, otherwise false.
     */
    void setInPorts(final PortType[] portTypes, final boolean enableReportOutput) {
        final int nrIns = portTypes.length + /* flow var */ 1 + (enableReportOutput ? 1 : 0);
        m_inports = new NodeInPort[nrIns];
        m_inHiliteHandler = new HiLiteHandler[portTypes.length];
        m_inports[0] = new NodeInPort(0, FlowVariablePortObject.TYPE_OPTIONAL);
        for (int i = 0; i < portTypes.length; i++) {
            m_inports[i + 1] = new NodeInPort(i + 1, portTypes[i]);
            m_inHiliteHandler[i] = new HiLiteHandler();
        }
        if (enableReportOutput) {
            final int lastPortIndex = m_inports.length - 1;
            m_inports[lastPortIndex] = createReportInputPort(lastPortIndex);
        }

        NodeContainer oldVNode = m_wfm.getNodeContainer(getVirtualInNodeID());
        final PortType[] oldInNodePortTypes = IntStream.range(1, oldVNode.getNrOutPorts()) //
                .mapToObj(oldVNode::getOutPort).map(NodeOutPort::getPortType).toArray(PortType[]::new);
        // don't replace output node if its ports have changed (e.g. don't if only report is enabled now)
        if (!Arrays.equals(portTypes, oldInNodePortTypes)) {
            NodeSettings settings = new NodeSettings("node settings");
            m_wfm.saveNodeSettings(oldVNode.getID(), settings);
            m_virtualInNodeIDSuffix = m_wfm.createAndAddNode(
                new VirtualSubNodeInputNodeFactory(this, portTypes)).getIndex();
            NodeContainer newVNode = m_wfm.getNodeContainer(getVirtualInNodeID());
            newVNode.setUIInformation(oldVNode.getUIInformation());
            newVNode.setDeletable(false);
            // copy settings from old to new node
            try {
                m_wfm.loadNodeSettings(newVNode.getID(), settings);
            } catch (InvalidSettingsException e) {
                // ignore
            }
            oldVNode.setDeletable(true);
            m_wfm.removeNode(oldVNode.getID());
            getInPort(0).setPortName("Variable Inport");
            newVNode.addNodeStateChangeListener(new RefreshPortNamesListener());
            refreshPortNames();
        }
        m_wfm.setDirty();
        setDirty();
        notifyNodePropertyChangedListener(NodeProperty.MetaNodePorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrOutPorts() {
        return m_outports.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeOutPort getOutPort(final int index) {
        return m_outports[index];
    }

    /**
     * True if the last port is a report output (not represented "inside").
     * @return That property.
     * @since 5.1
     */
    public boolean hasReportOutput() {
        return getNrOutPorts() > getVirtualOutNode().getNrInPorts();
    }

    /**
     * The report configuration for this subnode/component, if report generation is set on it. Otherwise an empty
     * optional.
     *
     * @return the report configuration.
     * @since 5.1
     */
    public Optional<ReportConfiguration> getReportConfiguration() {
        return Optional.ofNullable(m_reportConfiguration);
    }

    /**
     * Sets a report configuration on the node, possibly showing/hiding report port type.
     *
     * @param reportConfiguration the new report config (or null to disable report).
     * @return true if the ports have changed (now present but previously not, or vice versa)
     * @since 5.1
     */
    boolean setReportConfiguration(final ReportConfiguration reportConfiguration) {
        try (var lock = lock()) {
            resetOutputNodeForLayoutChanges();
            final var hasReportBefore = m_reportConfiguration != null;
            final var hasReportNow = reportConfiguration != null;
            final var havePortsChanged = hasReportBefore != hasReportNow;
            if (havePortsChanged) {
                final var portTypes = Stream.of(getOutputPortInfo()) //
                        .skip(1) // flow variable port
                        .map(MetaPortInfo::getType).toArray(PortType[]::new);
                setOutPorts(portTypes, hasReportNow);
                final var inPortTypes = Stream.of(getInputPortInfo()) //
                        .skip(1) // flow variable port
                        .map(MetaPortInfo::getType).toArray(PortType[]::new);
                setInPorts(inPortTypes, hasReportNow);
            }
            m_reportConfiguration = reportConfiguration;
            setDirty();
            return havePortsChanged;
        }
    }

    /**
     * Get the report object set on this subnode, or an empty optional if this subnode is not
     * building a report or the input node is not executed.
     * @return The spec of the report object, or an empty optional as described above.
     * @since 5.2
     */
    public Optional<IReportPortObject> getReportObjectFromInput() {
        return getVirtualInNodeModel().getReportObjectFromInput();
    }

    private NodeContainerOutPort createReportOutputPort(final int index) {
        final var port = new NodeContainerOutPort(this, IReportPortObject.TYPE , index);
        port.setPortName("Component Report");
        return port;
    }

    private static NodeInPort createReportInputPort(final int index) {
        final var port = new NodeInPort(index, IReportPortObject.TYPE);
        port.setPortName("Report");
        return port;
    }

    /**
     * When new layout changes are applied (e.g. the report output is enabled/disabled) the output node needs
     * to be reset. Also assert that nothing is currently in execution (exception thrown).
     */
    private void resetOutputNodeForLayoutChanges() {
        var stateOfOutnode = getVirtualOutNode().getInternalState();
        // the UI code will have tested this also - so merely an assertion
        checkState(!stateOfOutnode.isExecutionInProgress(), "Can't apply settings, node is currently in execution");
        checkState(!getInternalState().isExecuted() || canResetContainedNodes(),
            "Can't apply settings, unable to reset content (possibly executing downstream nodes)");
        if (stateOfOutnode.isExecuted()) {
            getWorkflowManager().resetAndConfigureNode(getVirtualOutNodeID());
        }
    }

    /**
     * @param outNodePortTypes Types of the new ports (excludes both the mickey mouse port and the report port)
     * @param enableReportOutput true to enable report type, otherwise false.
     */
    void setOutPorts(final PortType[] outNodePortTypes, final boolean enableReportOutput) {
        assert isLockedByCurrentThread();
        m_wfm.resetAndConfigureNode(getVirtualOutNodeID()); // unset outputs (before we change its length)
        final int nrOuts = outNodePortTypes.length + /* flowvar */ 1 + (enableReportOutput ? 1 : 0);
        m_outputs = new Output[nrOuts];
        m_outports = new NodeContainerOutPort[nrOuts];
        m_outputs[0] = new Output(FlowVariablePortObject.TYPE);
        m_outports[0] = new NodeContainerOutPort(this, FlowVariablePortObject.TYPE, 0);
        for (int i = 0; i < outNodePortTypes.length; i++) {
            m_outputs[i + 1] = new Output(outNodePortTypes[i]);
            m_outports[i + 1] = new NodeContainerOutPort(this, outNodePortTypes[i], i + 1);
        }
        if (enableReportOutput) {
            final int index = m_outputs.length - 1;
            m_outputs[index] = new Output(IReportPortObject.TYPE);
            m_outports[index] = createReportOutputPort(index);
        }

        NodeContainer oldVNode = m_wfm.getNodeContainer(getVirtualOutNodeID());
        final PortType[] oldOutNodePortTypes = IntStream.range(1, oldVNode.getNrInPorts()) //
            .mapToObj(oldVNode::getInPort).map(NodeInPort::getPortType).toArray(PortType[]::new);
        // don't replace output node if its ports have changed (e.g. don't if only report is enabled now)
        if (!Arrays.equals(outNodePortTypes, oldOutNodePortTypes)) {
            NodeSettings settings = new NodeSettings("node settings");
            m_wfm.saveNodeSettings(oldVNode.getID(), settings);

            m_virtualOutNodeIDSuffix =
                    m_wfm.createAndAddNode(new VirtualSubNodeOutputNodeFactory(this, outNodePortTypes)).getIndex();
            NodeContainer newVNode = m_wfm.getNodeContainer(getVirtualOutNodeID());
            newVNode.setUIInformation(oldVNode.getUIInformation());
            newVNode.setDeletable(false);
            // copy settings from old to new node
            try {
                m_wfm.loadNodeSettings(newVNode.getID(), settings);
            } catch (InvalidSettingsException e) {
                // ignore
            }
            oldVNode.setDeletable(true);
            m_wfm.removeNode(oldVNode.getID());
            getOutPort(0).setPortName("Variable Outport");
            newVNode.addNodeStateChangeListener(new RefreshPortNamesListener());
            refreshPortNames();
        }
        m_wfm.setDirty();
        setDirty();
        notifyNodePropertyChangedListener(NodeProperty.MetaNodePorts);
    }

    /**
     * Used by the UI to determine whether the given port represents the report port (last port, if enabled).
     *
     * @param portIndex port in question.
     * @param isInput true when refering to input port, false for output port.
     * @return Whether it's the report port.
     * @since 5.2
     */
    public boolean isReportPort(final int portIndex, final boolean isInput) {
        return getReportConfiguration().isPresent()
                && portIndex == (isInput ? getNrInPorts() : getNrOutPorts()) - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void cleanOutPorts(final boolean isLoopRestart) {
        for (Output o : m_outputs) {
            o.clean();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortType getOutputType(final int portIndex) {
        return m_outputs[portIndex].getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getOutputSpec(final int portIndex) {
        return m_outputs[portIndex].getSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject getOutputObject(final int portIndex) {
        return m_outputs[portIndex].getObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputObjectSummary(final int portIndex) {
        return m_outputs[portIndex].getSummary();
    }

    /* ------------- HiLite Support ---------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    void setInHiLiteHandler(final int index, final HiLiteHandler hdl) {
        assert 0 <= index && index < getNrInPorts();
        final int validStart = m_reportConfiguration != null ? 2 : 1;
        if (index >= validStart) {
            // ignore HiLiteHandler on optional variable input port
            m_inHiliteHandler[index - validStart] = hdl;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getOutputHiLiteHandler(final int portIndex) {
        if (hasReportOutput() && portIndex == getNrOutPorts() - 1) {
            return null;
        }
        return getVirtualOutNode().getNode().getInHiLiteHandler(portIndex);
    }

    /**
     * @param portIndex
     * @return the inHiliteHandler
     * @since 3.7
     */
    public HiLiteHandler getInHiliteHandler(final int portIndex) {
        assert 0 <= portIndex && portIndex < getNrInPorts();
        return m_inHiliteHandler[portIndex];
    }

    /* ------------------ Load&Save ------------------------- */

    /**
     * The default parameter name for dialog nodes. The parameter name is used as identifier in batch or web service
     * execution and is derived from the class name.
     * @param cl the class of the dialog node - only using its name
     * @return default parameter name, e.g. for StringInputQuickFormNodeModel -&gt; string-input
     * @since 2.12
     */
    public static final String getDialogNodeParameterNameDefault(final Class<?> cl) {
        CheckUtils.checkArgumentNotNull(cl, "Must not be null");
        // from IntInputQuickFormNodeModel -> IntInput
        final String[] hiddenSuffixes = new String[]{"configurationnodemodel", "quickformnodemodel", "nodemodel",
            "quickformconfig", "config", "dialognode", "node", "widget"};
        String truncated = cl.getSimpleName();
        for (String suffix : hiddenSuffixes) {
            truncated = StringUtils.removeEndIgnoreCase(truncated, suffix);
        }
        String[] segments = StringUtils.splitByCharacterTypeCamelCase(truncated);
        String result = StringUtils.lowerCase(StringUtils.join(segments, '-'));
        assert DialogNode.PARAMETER_NAME_PATTERN.matcher(result).matches() : "Doesn't match: " + cl.getName();
        return result;
    }

    /** The parameter name under which the subnode's flow variables tab, the batch executor or a web service caller
     * will find a dialog node. It's part of the node's configuration.
     * @param node The node itself - for querying its {@link DialogNode#getParameterName() parameter name}.
     * @param id The id of the node, not null - the suffix is always appended to avoid duplicates
     * @return The parameter name, not null.
     * @since 5.5
     */
    public static String getDialogNodeParameterName(final DialogNode<?, ?> node, final NodeID id) {
        final String parameterName = node.getParameterName();
        return (!StringUtils.isEmpty(parameterName) ? (parameterName + "-") : "") + id.getIndex();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    void performValidateSettings(final NodeSettingsRO modelSettings) throws InvalidSettingsException {
        Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, false);
        for (Map.Entry<NodeID, DialogNode> entry : nodes.entrySet()) {
            NodeID id = entry.getKey();
            DialogNode node = entry.getValue();
            String parameterName = getDialogNodeParameterName(node, id);
            if (modelSettings.containsKey(parameterName)) {
                NodeSettingsRO conf = modelSettings.getNodeSettings(parameterName);
                NodeContext.pushContext(m_wfm.getNodeContainer(id));
                try {
                    final DialogNodeValue validationDialogValue = node.createEmptyDialogValue();
                    validationDialogValue.loadFromNodeSettings(conf);
                    node.validateDialogValue(validationDialogValue);
                } catch (InvalidSettingsException ise) {
                    throw ise;
                } catch (Throwable e) {
                    LOGGER.coding("Settings validation threw \"" + e.getClass().getSimpleName()
                        + "\": " + e.getMessage(), e);
                    throw new InvalidSettingsException(e.getMessage(), e);
                } finally {
                    NodeContext.removeLastContext();
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void performLoadModelSettingsFrom(final NodeSettingsRO modelSettings) throws InvalidSettingsException {
        loadModelSettingsIntoDialogNodes(modelSettings, false);
    }

    /** Applies the "modelSettings" (stored in the {@link SingleNodeContainerSettings} into the {@link DialogNode}
     * contained in this workflow.
     * @param modelSettings The new model settings.
     * @param performReset true when called via dialog, false when called during load.
     * @throws InvalidSettingsException ...
     */
    @SuppressWarnings("rawtypes")
    private void loadModelSettingsIntoDialogNodes(final NodeSettingsRO modelSettings,
        final boolean performReset) throws InvalidSettingsException {
        assert isLockedByCurrentThread();
        synchronized (m_nodeMutex) {
            // check state of contained WFM as state of this Subnode may already be "MARKED".
            if (m_wfm.getInternalState().isExecutionInProgress()) {
                throw new IllegalStateException("Cannot load settings as the Component is currently executing");
            }
            Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, false);
            // contains all nodes that have new value (different to previous value, even if null now).
            Map<NodeID, DialogNodeValue> newDialogValueMap = new HashMap<>();

            // iterate all dialog nodes and determine those that have a new dialog value. Just load the value
            // but do not set it yet in order to verify/load all settings before applying them
            for (Map.Entry<NodeID, DialogNode> entry : nodes.entrySet()) {
                final NodeID id = entry.getKey();
                final DialogNode node = entry.getValue();
                final String parameterName = getDialogNodeParameterName(node, id);

                // the old/previously set value in the node
                final DialogNodeValue oldDialogValue = node.getDialogValue();
                final NodeSettings oldDialogValueSettings;
                if (oldDialogValue != null) {
                    oldDialogValueSettings = new NodeSettings(parameterName);
                    oldDialogValue.saveToNodeSettings(oldDialogValueSettings);
                } else {
                    oldDialogValueSettings = null;
                }
                final NodeSettingsRO newDialogValueSettings = modelSettings.containsKey(parameterName)
                        ? modelSettings.getNodeSettings(parameterName) : null;

                // only apply if different to previous value. Fall back to equality on settings object ass
                // #equals on DialogNodeValue might not be implemented.
                if (ObjectUtils.notEqual(newDialogValueSettings, oldDialogValueSettings)) {
                    final DialogNodeValue newDialogValue;
                    if (newDialogValueSettings != null) {
                        newDialogValue = node.createEmptyDialogValue();
                        try {
                            newDialogValue.loadFromNodeSettings(newDialogValueSettings);
                        } catch (InvalidSettingsException e) {
                            throw new InvalidSettingsException(String.format(
                                "Cannot load dialog value for node \"%s\": %s",
                                m_wfm.getNodeContainer(id).getNameWithID(), e.getMessage()), e);
                        }
                    } else {
                        newDialogValue = null;
                    }
                    newDialogValueMap.put(id, newDialogValue);
                }
            }

            // apply all new dialog values and reset/configure those nodes with modified config.
            for (Map.Entry<NodeID, DialogNodeValue> modifiedNodesEntry : newDialogValueMap.entrySet()) {
                final DialogNode node = nodes.get(modifiedNodesEntry.getKey());
                node.setDialogValue(modifiedNodesEntry.getValue());
                if (performReset) {
                    m_wfm.resetAndConfigureNode(modifiedNodesEntry.getKey());
                }
            }

        }
    }

    /** Callback from persistor. Link the virtual input/output nodes to the component that contains them. */
     void postLoadWFM() {
         getVirtualInNodeModel().setSubNodeContainer(this);
         getVirtualOutNodeModel().setSubNodeContainer(this);
    }

    /** {@inheritDoc} */
    @Override
    WorkflowCopyContent performLoadContent(final SingleNodeContainerPersistor nodePersistor,
        final Map<Integer, BufferedDataTable> tblRep, final FlowObjectStack inStack, final ExecutionMonitor exec,
        final LoadResult loadResult, final boolean preserveNodeMessage) throws CanceledExecutionException {
        SubNodeContainerPersistor subNodePersistor = (SubNodeContainerPersistor)nodePersistor;
        WorkflowPersistor workflowPersistor = subNodePersistor.getWorkflowPersistor();
        // TODO pass in a filter input stack
        try (WorkflowLock lock = m_wfm.lock()) {
            m_wfm.loadContent(workflowPersistor, tblRep, inStack, exec, loadResult, preserveNodeMessage);
        }
        if (workflowPersistor.isDirtyAfterLoad() || m_wfm.isDirty()) {
            setDirty();
        }
        InternalNodeContainerState loadState = nodePersistor.getMetaPersistor().getState();
        if (!m_wfm.getInternalState().equals(loadState)) {
            // can happen for workflows that were exported without data;
            // the same check is done by the caller (WorkflowManager#postLoad) and handled appropriately
            setInternalState(m_wfm.getInternalState(), false);
        }

        NodeSettingsRO modelSettings = subNodePersistor.getSNCSettings().getModelSettings();
        if (modelSettings != null) {
            try {
                loadModelSettingsIntoDialogNodes(modelSettings, false);
            } catch (InvalidSettingsException e) {
                final String msg = "Could not load Component configuration into dialog-nodes: " + e.getMessage();
                LOGGER.error(msg, e);
                loadResult.addError(msg);
                setDirty();
            }
        }
        checkInOutNodesAfterLoad(loadResult);
        loadLegacyPortNamesAndDescriptionsFromInOutNodes();
        // put data input output node if it was executed;
        final NativeNodeContainer virtualOutNode = getVirtualOutNode();
        LoadVersion l = nodePersistor instanceof FileSingleNodeContainerPersistor
                ? ((FileSingleNodeContainerPersistor)nodePersistor).getLoadVersion() : LoadVersion.V3010;
        if (l.isOlderThan(LoadVersion.V3010) && virtualOutNode.getInternalState().isExecuted()) {
            VirtualSubNodeOutputNodeModel outNodeModel = getVirtualOutNodeModel();
            PortObject[] outputData = new PortObject[virtualOutNode.getNrInPorts()];
            m_wfm.assembleInputData(getVirtualOutNodeID(), outputData);
            outNodeModel.postLoadExecute(ArrayUtils.removeAll(outputData, 0));
            // allow node to receive the internal held objects so that the next save operation also persists the
            // array of internal held objects - otherwise we get strange errors with nodes saved in 2.x, then loaded
            // and saved in 3.1+ (and converted ... although unmodified)
            getVirtualOutNode().getNode().assignInternalHeldObjects(outputData, null,
                getVirtualOutNode().createExecutionContext(), new PortObject[0]);
        }
        setVirtualOutputIntoOutport(m_wfm.getInternalState());
        m_wfmStateChangeListener = createAndAddStateListener();
        m_wfmListener = createAndAddWorkflowListener();
        getInPort(0).setPortName("Variable Inport");
        getOutPort(0).setPortName("Variable Outport");
        getVirtualInNode().addNodeStateChangeListener(new RefreshPortNamesListener());
        getVirtualOutNode().addNodeStateChangeListener(new RefreshPortNamesListener());
        refreshPortNames();
        return null;
    }

    @SuppressWarnings("deprecation")
    private void loadLegacyPortNamesAndDescriptionsFromInOutNodes() {
        final var loadVersion = getWorkflowManager().getLoadVersion();
        if ((m_metadata == null || m_metadata == ComponentMetadata.NONE) && loadVersion != null
            && loadVersion.isOlderThan(LoadVersion.V4010)) {
            //take node description from virtual input node
            ComponentOptionalsBuilder builder = ComponentMetadata.fluentBuilder();

            String[] inPortNames;
            String[] inPortDescriptions;
            String[] outPortNames;
            String[] outPortDescriptions;

            //take port descriptions from virtual input node
            inPortNames = getVirtualInNodeModel().getPortNames();
            inPortDescriptions = getVirtualInNodeModel().getPortDescriptions();
            for (var i = 0; i < inPortNames.length; i++) {
                builder.withInPort(inPortNames[i], inPortDescriptions[i]);
            }

            //take port descriptions from virtual output node
            outPortNames = getVirtualOutNodeModel().getPortNames();
            outPortDescriptions = getVirtualOutNodeModel().getPortDescriptions();
            for (var i = 0; i < outPortNames.length; i++) {
                builder.withOutPort(outPortNames[i], outPortDescriptions[i]);
            }
            m_metadata = builder //
                    .withPlainContent() //
                    .withLastModifiedNow() //
                    .withDescription(getVirtualInNodeModel().getSubNodeDescription()) //
                    .build();
        }
    }

    /** Fixes in- and output nodes after loading (in case they don't exist or have errors). */
    private void checkInOutNodesAfterLoad(final LoadResult loadResult) {
        /* Fix output node */
        NodeID virtualOutID = getVirtualOutNodeID();
        String error = null;                  // non null in case not is not present of of wrong type
        NodeSettings outputSettings = null;   // settings of previous node if present or null
        MinMaxCoordinates minMaxCoordinates; // assigned with node insertion, used for node placement
        if (m_wfm.containsNodeContainer(virtualOutID)) {
            NodeContainer virtualOutNC = m_wfm.getNodeContainer(virtualOutID);
            if (virtualOutNC instanceof NativeNodeContainer) {
                NodeModel virtualOutModel = ((NativeNodeContainer)virtualOutNC).getNodeModel();
                if (!(virtualOutModel instanceof VirtualSubNodeOutputNodeModel)) {
                    // this is very likely a missing node (placeholder)
                    error = String.format("Virtual output node is not of expected type (expected %s, actual %s)",
                        VirtualSubNodeOutputNodeModel.class.getName(), virtualOutModel.getClass().getName());
                    NodeSettings temp = new NodeSettings("temp");
                    m_wfm.saveNodeSettings(virtualOutID, temp);
                    outputSettings = temp;
                }
            } else {
                error = String.format("Virtual output node with ID %s is not a native node", virtualOutID);
            }
        } else {
            error = String.format("Virtual output node with ID %s does not exist", virtualOutID);
        }

        if (error != null) {
            minMaxCoordinates = getMinMaxCoordinates();
            m_virtualOutNodeIDSuffix =
                addVirtualOutNode(Output.getPortTypesNoFlowVariablePort(m_outputs)).getID().getIndex();
            placeVirtualOutNode(minMaxCoordinates);
            error = error.concat(String.format(" - creating new instance (ID %s)", m_virtualOutNodeIDSuffix));
            loadResult.addError(error);
            if (outputSettings != null) {
                try {
                    m_wfm.loadNodeSettings(getVirtualOutNodeID(), outputSettings);
                } catch (InvalidSettingsException e) {
                    // again, ignore as the node was missing, which is much more critical
                }
            }
        }

        /* Fix input node */
        NodeID virtualInID = getVirtualInNodeID();
        error = null;                         // non null in case not is not present of of wrong type
        NodeSettings inputSettings = null;    // settings of previous node if present or null
        if (m_wfm.containsNodeContainer(virtualInID)) {
            NodeContainer virtualInNC = m_wfm.getNodeContainer(virtualInID);
            if (virtualInNC instanceof NativeNodeContainer) {
                NodeModel virtualInModel = ((NativeNodeContainer)virtualInNC).getNodeModel();
                if (!(virtualInModel instanceof VirtualSubNodeInputNodeModel)) {
                    // this is very likely a missing node (placeholder)
                    error = String.format("Virtual input node is not of expected type (expected %s, actual %s)",
                        VirtualSubNodeInputNodeModel.class.getName(), virtualInModel.getClass().getName());
                    NodeSettings temp = new NodeSettings("temp");
                    m_wfm.saveNodeSettings(virtualInID, temp);
                    inputSettings = temp;
                }
            } else {
                error = String.format("Virtual input node with ID %s is not a native node", virtualInID);
            }
        } else {
            error = String.format("Virtual input node with ID %s does not exist", virtualInID);
        }

        if (error != null) {
            minMaxCoordinates = getMinMaxCoordinates();
            PortType[] inportTypes = new PortType[getNrInPorts() - 1]; // skip flow var port
            for (int i = 1; i < getNrInPorts(); i++) {
                inportTypes[i - 1] = getInPort(i).getPortType();
            }
            m_virtualInNodeIDSuffix = addVirtualInNode(inportTypes).getID().getIndex();
            placeVirtualInNode(minMaxCoordinates);
            error = error.concat(String.format(" - creating new instance (ID %s)", m_virtualInNodeIDSuffix));
            loadResult.addError(error);
            if (inputSettings != null) {
                try {
                    m_wfm.loadNodeSettings(getVirtualInNodeID(), inputSettings);
                } catch (InvalidSettingsException e) {
                    // again, ignore as the node was missing, which is much more critical
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    void performSaveModelSettingsTo(final NodeSettings modelSettings) {
        Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, false);
        for (Map.Entry<NodeID, DialogNode> entry : nodes.entrySet()) {
            final DialogNode dialogNode = entry.getValue();
            final String parameterName = getDialogNodeParameterName(dialogNode, entry.getKey());
            final DialogNodeValue dialogValue = dialogNode.getDialogValue();
            if (dialogValue != null) {
                NodeSettingsWO subSettings = modelSettings.addNodeSettings(parameterName);
                dialogValue.saveToNodeSettings(subSettings);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void performSaveDefaultViewSettingsTo(final NodeSettings viewSettings) {
        // components don't have view settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeContainerPersistor getCopyPersistor(final boolean preserveDeletableFlags,
        final boolean isUndoableDeleteCommand) {
        return new CopySubNodeContainerPersistor(this, preserveDeletableFlags, isUndoableDeleteCommand);
    }

    /** @return a persistor containing all but the virtual nodes and that is also fixing the in/out connections
     * once the node is unwrapped to a metanode. */
    WorkflowPersistor getConvertToMetaNodeCopyPersistor() {
        assert isLockedByCurrentThread();
        Collection<WorkflowAnnotationID> workflowAnnotations = m_wfm.getWorkflowAnnotationIDs();
        // all but virtual in and output node
        NodeID[] nodes = m_wfm.getNodeContainers().stream().map(nc -> nc.getID())
                .filter(id -> id.getIndex() != m_virtualInNodeIDSuffix)
                .filter(id -> id.getIndex() != m_virtualOutNodeIDSuffix)
                .toArray(NodeID[]::new);
        WorkflowCopyContent.Builder cnt = WorkflowCopyContent.builder();
        cnt.setNodeIDs(nodes);
        cnt.setAnnotationIDs(workflowAnnotations.toArray(new WorkflowAnnotationID[workflowAnnotations.size()]));
        cnt.setIncludeInOutConnections(true);
        WorkflowPersistor persistor = m_wfm.copy(true, cnt.build());
        final Set<ConnectionContainerTemplate> additionalConnectionSet = persistor.getAdditionalConnectionSet();
        for (Iterator<ConnectionContainerTemplate> it = additionalConnectionSet.iterator(); it.hasNext();) {
            ConnectionContainerTemplate c = it.next();
            if (c.getSourceSuffix() == m_virtualInNodeIDSuffix) {
                if (c.getSourcePort() == 0) {
                    it.remove();
                    continue;
                }
                c.setSourceSuffix(-1);
                c.setSourcePort(c.getSourcePort() - 1);
            }

            if (c.getDestSuffix() == m_virtualOutNodeIDSuffix) {
                if (c.getDestPort() == 0) {
                    it.remove();
                    continue;
                }
                c.setDestSuffix(-1);
                c.setDestPort(c.getDestPort() - 1);
            }
        }
        return persistor;
    }

    /* -------------------- Credentials/Stacks ------------------ */

    /**
     * {@inheritDoc}
     */
    @Override
    void performSetCredentialsProvider(final CredentialsProvider cp) {
        // TODO needed once we want to support workflow credentials
    }

    /**
     * {@inheritDoc}
     */
    @Override
    CredentialsProvider getCredentialsProvider() {
        // TODO remove the whole method as soon as we have introduced quickform credentials
        return new CredentialsProvider(this, new CredentialsStore(m_wfm));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setFlowObjectStack(final FlowObjectStack st, final FlowObjectStack outgoingStack) {
        if (hasExampleInputData()) {
            // push flow variables that are persisted with a component project onto the stack, too
            // (create a new stack for that purpose to not modify the passed one)
            m_incomingStack = new FlowObjectStack(getID(), st);
            for (FlowVariable fv : getTemplateInformation().getIncomingFlowVariables()) {
                m_incomingStack.push(fv);
            }
        } else {
            m_incomingStack = st;
        }
        m_outgoingStack = outgoingStack;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowObjectStack getFlowObjectStack() {
        return m_incomingStack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowObjectStack getOutgoingFlowObjectStack() {
        return m_outgoingStack;
    }

    /* -------------- Layouting --------- */

    /**
     * @return the SubnodeContainerLayoutStringProvider
     * @since 4.2
     */
    public SubnodeContainerLayoutStringProvider getSubnodeLayoutStringProvider() {
        return m_subnodeLayoutStringProvider.copy();
    }

    /**
     * @return the SubnodeContainerConfigurationStringProvider
     * @since 4.3
     */
    public SubnodeContainerConfigurationStringProvider getSubnodeConfigurationLayoutStringProvider() {
        return m_subnodeConfigurationStringProvider.copy();
    }

    /**
     * @param layoutStringProvider the SubnodeContainerLayoutStringProvider to set
     * @since 4.2
     */
    public void setSubnodeLayoutStringProvider(final SubnodeContainerLayoutStringProvider layoutStringProvider) {
        try (var lock = lock()) {
            if (!m_subnodeLayoutStringProvider.equals(layoutStringProvider)) {
                resetOutputNodeForLayoutChanges();
                m_subnodeLayoutStringProvider =
                        new SubnodeContainerLayoutStringProvider(layoutStringProvider.getLayoutString());
                setDirtyAfterLayoutChanges();
            }
        }
    }

    /**
     * @param configurationStringProvider the SubnodeContainerConfigurationStringProvider to set
     * @since 4.3
     */
    public void setSubnodeConfigurationStringProvider(
        final SubnodeContainerConfigurationStringProvider configurationStringProvider) {
        try (var lock = lock()) {
            if (!m_subnodeConfigurationStringProvider.equals(configurationStringProvider)) {
                m_subnodeConfigurationStringProvider = new SubnodeContainerConfigurationStringProvider(
                    configurationStringProvider.getConfigurationLayoutString());
                setDirtyAfterLayoutChanges();
            }
        }
    }

    private void setDirtyAfterLayoutChanges() {
        if (isProject()) {
            //differently handled if this is a component project
            //otherwise the setDirty event will just be past to the parent (which is ROOT)
            getChangesTracker().ifPresent(ct -> ct.otherChange());
            //for consistency
            if (!getWorkflowManager().isDirty()) {
                getWorkflowManager().setDirty();
            }
        } else {
            setDirty();
        }
    }

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public boolean isHideInWizard() {
        return m_hideInWizard;
    }

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public void setHideInWizard(final boolean hide) {
        if (hide != m_hideInWizard) {
            m_hideInWizard = hide;
            setDirty();
        }
    }

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public String getCssStyles() {
        if (m_customCSS == null) {
            m_customCSS = "";
        }
        return m_customCSS;
    }

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public void setCssStyles(final String styles) {
        if (!StringUtils.equals(m_customCSS, styles)) {
            m_customCSS = styles;
            setDirty();
        }
    }

    /**
     * Sets a flag on a given {@link WizardNode} of nested subnode, whether or not it is hidden from wizard execution
     * @param id the node to set the flag on
     * @param hide true if the node is supposed to be hidden from WebPortal or wizard execution, false otherwise
     * @since 3.5
     * @noreference This method is not intended to be referenced by clients.
     */
    public void setHideNodeFromWizard(final NodeID id, final boolean hide) {
        try (WorkflowLock lock = lock()) {
            NodeContainer container = m_wfm.getNodeContainer(id, NodeContainer.class, true);
            ViewHideable vh = null;
            NativeNodeContainer nnc = null;
            if (container instanceof SubNodeContainer) {
                vh = (SubNodeContainer)container;
            } else if (container instanceof NativeNodeContainer) {
                nnc = (NativeNodeContainer)container;
                NodeModel model = nnc.getNodeModel();
                CheckUtils.checkArgument(model instanceof WizardNode,
                    "Can't set hide in wizard flag on non-wizard nodes.");
                vh = (WizardNode<?, ?>)model;
            } else {
                throw new IllegalArgumentException(
                    "Node with id " + id + " needs to be a native node or a subnode container!");
            }
            if (vh != null) {
                if (hide != vh.isHideInWizard()) {
                    vh.setHideInWizard(hide);
                    if (nnc != null) {
                        nnc.saveModelSettingsToDefault();
                        nnc.setDirty();
                    }
                    resetOutputNodeForLayoutChanges();
                }
            }
        }
    }

    /** Sets a flag on a given {@link DialogNode}, whether or not it is hidden from a containing metanode dialog
     * @param id the node to set the flag on
     * @param hide true if the node is supposed to be hidden from a containing metanode dialog, false otherwise
     * @since 3.5
     * @noreference This method is not intended to be referenced by clients.
     */
    public void setHideNodeFromDialog(final NodeID id, final boolean hide) {
        try (WorkflowLock lock = lock()) {
            NativeNodeContainer nnc = m_wfm.getNodeContainer(id, NativeNodeContainer.class, true);
            NodeModel model = nnc.getNodeModel();
            CheckUtils.checkArgument(model instanceof DialogNode, "Can't set hide in dialog flag on non-dialog nodes.");
            DialogNode<?,?> dn = (DialogNode<?,?>)model;
            if (hide != dn.isHideInDialog()) {
                dn.setHideInDialog(hide);
                nnc.saveModelSettingsToDefault();
                nnc.setDirty();
            }
        }
    }

    /* -------------- SingleNodeContainer methods without meaningful equivalent --------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModelCompatibleTo(final Class<?> nodeModelClass) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInactive() {
        // temporary solution to work around a problem surfaced as part of AP-21327 and to be fixed with AP-22241:
        // this method might be called immediately post construction before the in/out nodes are assigned
        final var inNNC = getWorkflowManager().getNodeContainer(getVirtualInNodeID(), NativeNodeContainer.class, false);
        return inNNC != null && inNNC.isInactive();
    }

    @Override
    boolean setInactive() {
        LOGGER.assertLog(!m_wfm.isLockedByCurrentThread(), "Must not be executed inactively while locked");
        final Runnable inBackgroundCallable = this::executeAndWaitInactively;
        final ThreadPool currentPool = ThreadPool.currentPool();
        if (currentPool != null) {
            // ordinary workflow execution
            try {
                currentPool.runInvisible(Executors.callable(inBackgroundCallable, null));
            } catch (ExecutionException ee) {
                LOGGER.error(ee.getCause().getClass().getSimpleName()
                        + " while waiting for inner workflow to complete", ee);
            } catch (final InterruptedException e) { // NOSONAR interrupted handled by workflow cancellation
                m_wfm.cancelExecution();
            }
        } else {
            // streaming execution
            inBackgroundCallable.run();
        }
        // might not be executed (even when inactive) if there are unconnected nodes
        return m_wfm.getInternalState().isExecuted();
    }

    /** Called by {@link #setInactive()} to run all nodes in the contained workflow. */
    private void executeAndWaitInactively() {
        //collect all inner error node messages to set them again later after reset
        Map<NodeID, NodeMessage> innerNodeMessages = m_wfm.getNodeContainers().stream()
                .filter(nc -> nc.getNodeMessage().getMessageType() == NodeMessage.Type.ERROR)
                .collect(Collectors.toMap(nc -> nc.getID(), nc -> nc.getNodeMessage()));
        m_isPerformingActionCalledFromParent = true;
        try (WorkflowLock lock = m_wfm.lock()) {
            m_subnodeScopeContext.inactiveScope(true);
            m_wfm.cancelExecution();
            //'execute' inner workflow manager locally to propagate inactive port states

            // temporarily apply standard job manager
            NodeExecutionJobManager jobManager = m_wfm.getJobManager();
            m_wfm.setJobManager(ThreadNodeExecutionJobManagerFactory.INSTANCE.getInstance());
            m_wfm.resetAndConfigureAll();

            try {
                m_wfm.executeAllAndWaitUntilDoneInterruptibly();
            } catch (InterruptedException e) { // NOSONAR interrupted handled by workflow cancellation
                m_wfm.cancelExecution();
            }

            if (!m_wfm.getInternalState().isExecuted()) {
                cancelExecution();
                m_wfm.resetAndConfigureAll();
            }
            setVirtualOutputIntoOutport(m_wfm.getInternalState());
            // restore job manager
            m_wfm.setJobManager(jobManager);

            //set inner node error messages
            innerNodeMessages.entrySet().stream()
                .forEach(e -> m_wfm.getNodeContainer(e.getKey()).setNodeMessage(e.getValue()));
        } finally {
            m_isPerformingActionCalledFromParent = false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInactiveBranchConsumer() {
        return false;
    }

    /** Implementation of {@link WorkflowManager#getSubnodeInputPortInfo(NodeID)}.
     * @return ...
     */
    MetaPortInfo[] getInputPortInfo() {
        Workflow wfmFlow = m_wfm.getWorkflow();
        NodeContainer inNode = m_wfm.getNodeContainer(getVirtualInNodeID());

        List<MetaPortInfo> result = new ArrayList<MetaPortInfo>(inNode.getNrOutPorts());
        for (int i = 0; i < inNode.getNrOutPorts(); i++) {
            int insideCount = 0;
            for (ConnectionContainer cc : wfmFlow.getConnectionsBySource(
                    getVirtualInNodeID())) {
                if (cc.getSourcePort() == i) {
                    // could also be a through connection
                    insideCount += 1;
                }
            }
            boolean hasOutsideConnection = false;
            for (ConnectionContainer outCC : getParent().getWorkflow().getConnectionsByDest(getID())) {
                if (outCC.getDestPort() == i) {
                    hasOutsideConnection = true;
                    break;
                }
            }
            String message;
            boolean isConnected;
            PortType portType = inNode.getOutPort(i).getPortType();
            if (hasOutsideConnection || insideCount > 0) {
                isConnected = true;
                if (hasOutsideConnection && insideCount > 0) {
                    message = "Connected to one upstream node and "
                        + insideCount + " downstream node(s)";
                } else if (hasOutsideConnection) {
                    message = "Connected to one upstream node";
                } else {
                    message = "Connected to " + insideCount + " downstream node(s)";
                }
            } else {
                isConnected = false;
                message = null;
            }
            result.add(MetaPortInfo.builder()
                .setPortType(portType)
                .setIsConnected(isConnected)
                .setMessage(message)
                .setOldIndex(i).build());
        }
        return result.toArray(new MetaPortInfo[result.size()]);
    }

    /** Implementation of {@link WorkflowManager#getSubnodeOutputPortInfo(NodeID)}. Excludes possible report output!
     * @return ...
     */
    MetaPortInfo[] getOutputPortInfo() {
        Workflow wfmFlow = m_wfm.getWorkflow();
        NodeContainer outNode = m_wfm.getNodeContainer(getVirtualOutNodeID());

        List<MetaPortInfo> result = new ArrayList<MetaPortInfo>(outNode.getNrInPorts());
        for (int i = 0; i < outNode.getNrInPorts(); i++) {
            boolean hasInsideConnection = false;
            for (ConnectionContainer cc : wfmFlow.getConnectionsByDest(getVirtualOutNodeID())) {
                if (cc.getDestPort() == i) {
                    hasInsideConnection = true;
                    break;
                }
            }
            int outsideCount = 0;
            for (ConnectionContainer outCC : getParent().getWorkflow().getConnectionsBySource(getID())) {
                if (outCC.getSourcePort() == i) {
                    outsideCount += 1;
                }
            }
            String message;
            boolean isConnected;
            PortType portType = outNode.getInPort(i).getPortType();
            if (hasInsideConnection || outsideCount > 0) {
                isConnected = true;
                if (hasInsideConnection && outsideCount > 0) {
                    message = "Connected to one upstream node and " + outsideCount + " downstream node(s)";
                } else if (hasInsideConnection) {
                    // could also be a through conn but we ignore here
                    message = "Connected to one upstream node";
                } else {
                    message = "Connected to " + outsideCount + " downstream node(s)";
                }
            } else {
                isConnected = false;
                message = null;
            }
            result.add(MetaPortInfo.builder()
                .setPortType(portType)
                .setIsConnected(isConnected)
                .setMessage(message)
                .setOldIndex(i).build());
        }
        return result.toArray(new MetaPortInfo[result.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean internalIsDirty() {
        return m_wfm.isDirty();
    }

    /* -------------- NodeContainerParent methods ---------------- */

    /** {@inheritDoc} */
    @Override
    public WorkflowLock lock() {
        return getParent().lock();
    }

    /** {@inheritDoc} */
    @Override
    public ReentrantLock getReentrantLockInstance() {
        return getParent().getReentrantLockInstance();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLockedByCurrentThread() {
        return getParent().isLockedByCurrentThread();
    }

    /** {@inheritDoc} */
    @Override
    public boolean canConfigureNodes() {
        final NodeContainerParent directNCParent = getDirectNCParent();
        try (WorkflowLock lock = directNCParent.lock()) {
            final WorkflowManager parent = getParent();
            if (m_subnodeScopeContext.isInactiveScope()) {
                return true;
            }
            if (!parent.containsNodeContainer(getID())) { // called during set-up (loading via constructor)
                return false;
            }
            if (isProject() && hasExampleInputData()) {
                return true;
            }
            if (!parent.assembleInputData(getID(), new PortObject[getNrInPorts()])) {
                return false;
            }
            return directNCParent.canConfigureNodes();
        }
    }

    private boolean hasExampleInputData() {
        return getTemplateInformation().getExampleInputDataInfo().isPresent();
    }

    @Override
    public boolean canResetContainedNodes() {
        // see AP-6886 -- if executed then there must be no downstream node in execution
        return !getInternalState().isExecuted() || getParent().canResetSuccessors(getID());
    }

    @Override
    public boolean canModifyStructure() {
        final InternalNodeContainerState state = getInternalState();
        if (state.isIdle() || state.isConfigured()) {
            return true; // if this subnode is non-executing/executed their downstream nodes can't be executing
        }
        // if this is executing then it can be still modified (e.g. nodes be added) but once it's green it's green.
        if (state.isExecuted() && getParent().hasSuccessorInProgress(getID())) {
            return false;
        }
        return getDirectNCParent().canModifyStructure();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWriteProtected() {
        if (getParent().isWriteProtected()) {
            return true;
        } else if (isProject()) {
            return false;
        } else {
            return Role.Link.equals(getTemplateInformation().getRole());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void pushWorkflowVariablesOnStack(final FlowObjectStack sos) {
        sos.pushWithOwner(m_subnodeScopeContext);
    }

    /**
     * Removes messages from the output node iff there are other messages in the map. This is because the output node
     * only mirrors errors from contained nodes, so it should not show up in the summary.
     */
    @Override
    public void postProcessNodeErrors(final Map<NodeContainer, String> messageMap) {
        if (messageMap.size() > 1) {
            messageMap.remove(getVirtualOutNode());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getCipherFileName(final String fileName) {
        return WorkflowCipher.getCipherFileName(this, fileName);
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowCipher getWorkflowCipher() {
        return getDirectNCParent().getWorkflowCipher();
    }

    /** {@inheritDoc} */
    @Override
    public OutputStream cipherOutput(final OutputStream out) throws IOException {
        return getDirectNCParent().cipherOutput(out);
    }

    /**
     * {@inheritDoc}
     * @since 2.10
     */
    @Override
    public WorkflowManager getProjectWFM() {
        return getDirectNCParent().getProjectWFM();
    }

    /**
     * Sets (and overrides) the component's metadata and notifies possible change listeners.
     *
     * @param metadata
     */
    public void setMetadata(final ComponentMetadata metadata) {
        m_metadata = metadata;
        notifyNodePropertyChangedListener(NodeProperty.ComponentMetadata);
        refreshPortNames();
        setDirty();
        getWorkflowManager().notifyWorkflowListeners(
            new WorkflowEvent(WorkflowEvent.Type.WORKFLOW_METADATA_CHANGED, getID(), null, null));
    }

    @Override
    public void setContainerMetadata(final NodeContainerMetadata updatedMetadata) {
        setMetadata((ComponentMetadata) updatedMetadata);
    }

    /**
     * Returns the metadata stored with the component.
     *
     * The port names and descriptions are adopted to the number of ports (i.e. either cut-off or filled with empty
     * values).
     *
     * Legacy: if workflow load version older than {@link LoadVersion#V4010}, component description, port names and port
     * descriptions are taken from the component input/output nodes, if available.
     *
     * @return the component metadata
     */
    @Override
    public ComponentMetadata getMetadata() {
        /* sync number of port names/descriptions with the actual ports number -> fill or cut-off */
        m_metadata = m_metadata.withNumberOfPorts(getNrInPorts() - 1, getNrOutPorts() - 1);

        return m_metadata;
    }



    /* -------- template handling ----- */

    /** @return the templateInformation
     * @since 2.10*/
    @Override
    public MetaNodeTemplateInformation getTemplateInformation() {
        return m_templateInformation;
    }

    /** {@inheritDoc} */
    @Override
    public void setTemplateInformation(final MetaNodeTemplateInformation tI) {
        CheckUtils.checkArgumentNotNull(tI, "Argument must not be null.");
        CheckUtils.checkArgument(!Role.Template.equals(tI.getRole())
            || TemplateType.SubNode.equals(tI.getNodeContainerTemplateType()),
            "Template type expected to be subnode: %s", tI.getNodeContainerTemplateType());
        m_templateInformation = tI;
        notifyNodePropertyChangedListener(NodeProperty.TemplateConnection);
        setDirty();
    }

    /**
     * Saves the component in place (as opposed to {@link #saveAsTemplate(File, ExecutionMonitor)} where a copy is saved
     * to a new directory).
     *
     * @param exec execution monitor
     * @return information about the component template
     * @throws IOException If an IO error occurs
     * @throws CanceledExecutionException If execution is canceled during the operation
     * @throws LockFailedException If locking failed
     * @throws InvalidSettingsException if defaults can't be set (meta node settings to be reverted in template)
     * @since 5.2
     */
    public MetaNodeTemplateInformation saveTemplate(final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, LockFailedException, InvalidSettingsException {
        return saveInternal(getNodeContainerDirectory().getFile(), exec, null, false);
    }

    /** {@inheritDoc} */
    @Override
    public MetaNodeTemplateInformation saveAsTemplate(final File directory, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, LockFailedException, InvalidSettingsException {
        return saveAsTemplate(directory, exec, null);
    }

    /** {@inheritDoc} */
    @Override
    public MetaNodeTemplateInformation saveAsTemplate(final File directory, final ExecutionMonitor exec,
        final PortObject[] exampleInputData)
        throws IOException, CanceledExecutionException, LockFailedException, InvalidSettingsException {
        return saveInternal(directory, exec, exampleInputData, true);
    }

    private MetaNodeTemplateInformation saveInternal(final File directory, final ExecutionMonitor exec,
        final PortObject[] exampleInputData, final boolean createCopy)
        throws IOException, CanceledExecutionException, LockFailedException, InvalidSettingsException {
        WorkflowManager tempParent = null;
        SubNodeContainer copyOrThis = null;
        ReferencedFile workflowDirRef = new ReferencedFile(directory);
        directory.mkdir();
        workflowDirRef.lock();
        ReferencedFile artifactsDirRef = getArtifactsDir(workflowDirRef);
        try {
            if (createCopy) {
                tempParent = WorkflowManager.lazyInitTemplateWorkflowRoot();
                WorkflowCopyContent.Builder cntBuilder = WorkflowCopyContent.builder();
                cntBuilder.setNodeIDs(getID());
                WorkflowCopyContent cnt;
                synchronized (m_nodeMutex) {
                    cnt = tempParent.copyFromAndPasteHere(getParent(), cntBuilder.build());
                }
                NodeID cID = cnt.getNodeIDs()[0];
                copyOrThis = ((SubNodeContainer)tempParent.getNodeContainer(cID));
            } else {
                copyOrThis = this;
            }
            try (WorkflowLock copyLock = copyOrThis.lock()) {
                if (createCopy) {
                    SingleNodeContainerSettings sncSettings = copyOrThis.getSingleNodeContainerSettings().clone();
                    sncSettings.setModelSettings(new NodeSettings("empty model"));
                    sncSettings.setVariablesSettings(new NodeSettings("empty variables setting"));
                    NodeSettings newSettings = new NodeSettings("new settings");
                    sncSettings.save(newSettings);
                    copyOrThis.loadSettings(newSettings);
                }
                synchronized (copyOrThis.m_nodeMutex) {
                    NodeSettingsRO exampleInputDataInfo = null;
                    List<FlowVariable> incomingFlowVariables = Collections.emptyList();
                    if (!isProject() && exampleInputData != null) {
                        //if this component is embedded in a workflow
                        //and example data needs to be saved with it
                        exampleInputDataInfo = FileSubNodeContainerPersistor.saveExampleInputData(exampleInputData,
                            artifactsDirRef.getFile(), exec);
                        incomingFlowVariables = new ArrayList<>(
                            Node.invokeGetAvailableFlowVariables(getVirtualInNodeModel(), FlowVariable.Type.values())
                                .values());
                    } else if (isProject() && copyOrThis.hasExampleInputData()) {
                        //save a component that is not embedded in a workflow
                        //and has example data
                        exampleInputDataInfo = copyOrThis.getTemplateInformation().getExampleInputDataInfo().get();
                        incomingFlowVariables = copyOrThis.getTemplateInformation().getIncomingFlowVariables();
                        if (!directory.equals(getNodeContainerDirectory().getFile())) {
                            //~save as -> copy example data over
                            FileSubNodeContainerPersistor.copyExampleInputData(
                                getArtifactsDir(getNodeContainerDirectory()).getFile(), exampleInputDataInfo,
                                getArtifactsDir(workflowDirRef).getFile());
                        }
                    }
                    if (isProject()) {
                        // makes sure that all contained nodes are reset (and configured, if possible)
                        copyOrThis.getWorkflowManager().resetAndConfigureAll();
                        // (for non-project templates this is done during copy above)
                        unsetDirty(); // hack only required for the classic UI
                    }
                    copyOrThis.setName(null);

                    MetaNodeTemplateInformation template =
                        MetaNodeTemplateInformation.createNewTemplate(exampleInputDataInfo, incomingFlowVariables);
                    copyOrThis.setTemplateInformation(template);
                    NodeSettings templateSettings =
                        MetaNodeTemplateInformation.createNodeSettingsForTemplate(copyOrThis);
                    templateSettings.saveToXML(
                        new FileOutputStream(new File(workflowDirRef.getFile(), WorkflowPersistor.TEMPLATE_FILE)));

                    FileSingleNodeContainerPersistor.save(copyOrThis, workflowDirRef, exec,
                        new WorkflowSaveHelper(true, false));

                    return template;
                }
            }
        } finally {
            if (copyOrThis != null && tempParent != null) {
                tempParent.removeNode(copyOrThis.getID());
            }
            workflowDirRef.unlock();
        }
    }

    private static ReferencedFile getArtifactsDir(final ReferencedFile workflowDirRef) {
        return new ReferencedFile(workflowDirRef, ".artifacts");
    }

    /**
     * @return <code>true</code> if the component is directly loaded as a project, <code>false</code> if component is
     *         part of/embedded in a workflow
     */
    @Override
    public boolean isProject() {
        return getParent() == WorkflowManager.ROOT;
    }

    /**
     * {@inheritDoc}
     * @since 2.10
     */
    @Override
    public void notifyTemplateConnectionChangedListener() {
        notifyNodePropertyChangedListener(NodeProperty.TemplateConnection);
    }

    /** {@inheritDoc} */
    @Override
    protected void notifyNodePropertyChangedListener(final NodeProperty property) {
        super.notifyNodePropertyChangedListener(property);
        switch (property) {
            case JobManager:
            case TemplateConnection:
                m_wfm.notifyNodePropertyChangedListener(property);
                break;
            default:
                // ignore children notification
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMetaNodeLinkInternalRecursively(final ExecutionMonitor exec, final WorkflowLoadHelper loadHelper,
        final Map<URI, TemplateUpdateCheckResult> visitedTemplateMap,
        final NodeContainerTemplateLinkUpdateResult loadRes) throws Exception {
        m_wfm.updateMetaNodeLinkInternalRecursively(exec, loadHelper, visitedTemplateMap, loadRes);
    }

    /** {@inheritDoc} */
    @Override
    public Map<NodeID, NodeContainerTemplate> fillLinkedTemplateNodesList(
        final Map<NodeID, NodeContainerTemplate> mapToFill, final boolean recurse,
        final boolean stopRecursionAtLinkedMetaNodes) {
        return m_wfm.fillLinkedTemplateNodesList(mapToFill, recurse, stopRecursionAtLinkedMetaNodes);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsExecutedNode() {
        return m_wfm.containsExecutedNode();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<NodeContainer> getNodeContainers() {
        return m_wfm.getNodeContainers();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isResetable() {
        return super.isResetable() || getWorkflowManager().isResetable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInWizardExecution() {
        //can never be determined here, must always ask its parent
        return getDirectNCParent().isInWizardExecution();
    }

    /** {@inheritDoc} */
    @Override
    boolean canPerformReset() {
        if (getNodeLocks().hasResetLock()) {
            return false;
        } else {
            return getWorkflowManager().canPerformReset();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void loadContent(final BaseNodeDef nodeDef, final ExecutionMonitor exec, final LoadResult loadResult)
        throws CanceledExecutionException {
        // Set single node def properties
        super.loadContent(nodeDef, exec, loadResult);

        var componentNodeDef = (ComponentNodeDef) nodeDef;
        NodeContext.pushContext(this);
        try (WorkflowLock lock = m_wfm.lock()) { // locking added as part of AP-19761
            m_wfm.loadContent(componentNodeDef.getWorkflow(), exec, loadResult);
        } finally {
            NodeContext.removeLastContext();
        }
        if (!m_wfm.getInternalState().equals(InternalNodeContainerState.IDLE)) {
            // can happen for workflows that were exported without data;
            // the same check is done by the caller (WorkflowManager#postLoad) and handled appropriately
            setInternalState(m_wfm.getInternalState(), false);
        }

        NodeSettingsRO modelSettings = DefToCoreUtil.toNodeSettings(componentNodeDef.getModelSettings());
        if (modelSettings != null) {
            try {
                loadModelSettingsIntoDialogNodes(modelSettings, false);
            } catch (InvalidSettingsException e) {
                final String msg = "Could not load Component configuration into dialog-nodes: " + e.getMessage();
                LOGGER.error(msg, e);
                loadResult.addError(msg);
                setDirty();
            }
        }
        // add virtual input/ouput nodes.
        checkInOutNodesAfterLoad(loadResult);
        loadLegacyPortNamesAndDescriptionsFromInOutNodes();

        // put data input output node if it was executed;
        setVirtualOutputIntoOutport(m_wfm.getInternalState());
        m_wfmStateChangeListener = createAndAddStateListener();
        m_wfmListener = createAndAddWorkflowListener();
        getInPort(0).setPortName("Variable Inport");
        getOutPort(0).setPortName("Variable Outport");
        getVirtualInNode().addNodeStateChangeListener(new RefreshPortNamesListener());
        getVirtualOutNode().addNodeStateChangeListener(new RefreshPortNamesListener());
        refreshPortNames();
    }
}
