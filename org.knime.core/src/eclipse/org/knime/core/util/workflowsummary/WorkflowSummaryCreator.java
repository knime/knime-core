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
 * History
 *   Mar 4, 2020 (hornm): created
 */
package org.knime.core.util.workflowsummary;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeGraphAnnotation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.metadata.MetaInfoFile;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Helper class to generate a summary from a workflow.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class WorkflowSummaryCreator {

    private static final String V_1_0_0 = "1.0.0";

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private WorkflowSummaryCreator() {
        // utility class
    }

    /**
     * Creates the summary.
     *
     * @param wfm the workflow manager to generate the summary for
     * @param includeExecutionInfo whether to include execution information in the summary, such as execution
     *            environment info (plugins, system variables etc.), port object summaries or execution statistics
     * @return the new workflow summary instance
     */
    public static WorkflowSummary create(final WorkflowManager wfm, final boolean includeExecutionInfo) {
        return create(wfm, includeExecutionInfo, null);
    }

    /**
     * Creates the summary.
     *
     * @param wfm the workflow manager to generate the summary for
     * @param includeExecutionInfo whether to include execution information in the summary, such as execution
     *            environment info (plugins, system variables etc.), port object summaries or execution statistics
     * @param nodesToIgnore list of nodes to ignore in the summary
     * @return the new workflow summary instance
     */
    public static WorkflowSummary create(final WorkflowManager wfm, final boolean includeExecutionInfo,
        final List<NodeID> nodesToIgnore) {
        return new WorkflowSummaryImpl(wfm, nodesToIgnore, includeExecutionInfo);
    }

    private static class WorkflowSummaryImpl implements WorkflowSummary {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

        private final Environment m_environment;

        private final Workflow m_workflow;

        private final String m_summaryCreationDateTime;

        WorkflowSummaryImpl(final WorkflowManager wfm, final List<NodeID> nodesToIgnore,
            final boolean includeExecutionInfo) {

            m_environment = includeExecutionInfo ? createEnvironment() : null;
            m_workflow = createWorkflow(wfm, nodesToIgnore, includeExecutionInfo);

            m_summaryCreationDateTime = FORMATTER.format(ZonedDateTime.now());
        }

        @Override
        public String getVersion() {
            return V_1_0_0;
        }

        @Override
        public String getSummaryCreationDateTime() {
            return m_summaryCreationDateTime;
        }

        @Override
        public Environment getEnvironment() {
            return m_environment;
        }

        @Override
        public Workflow getWorkflow() {
            return m_workflow;
        }

        private static Integer getGraphDepth(final NodeContainer nc) {
            Set<NodeGraphAnnotation> nga = nc.getParent().getNodeGraphAnnotation(nc.getID());
            if (!nga.isEmpty()) {
                return nga.iterator().next().getDepth();
            }
            return null;
        }

        private static Environment createEnvironment() {
            final Installation installation = createInstallation();
            Properties sysProps = System.getProperties();
            Map<String, String> sysPropsMap = new HashMap<>();
            for (Entry<?, ?> entry : sysProps.entrySet()) {
                sysPropsMap.put(entry.getKey().toString(), entry.getValue().toString());
            }
            return new Environment() {

                @Override
                public String getOS() {
                    return KNIMEConstants.getOSVariant();
                }

                @Override
                public String getKnimeVersion() {
                    return KNIMEConstants.VERSION;
                }

                @Override
                public Installation getInstallation() {
                    return installation;
                }

                @Override
                public Map<String, String> getSystemProperties() {
                    return sysPropsMap;
                }
            };
        }

        private static Installation createInstallation() {
            final List<Plugin> plugins = createPlugins();
            return new Installation() {

                @Override
                public List<Plugin> getPlugins() {
                    return plugins;
                }

            };
        }

        private static List<Plugin> createPlugins() {
            BundleContext bundleContext = FrameworkUtil.getBundle(WorkflowSummaryCreator.class).getBundleContext();
            //this plugin needs to be 'activated', bundle context is null otherwise
            Bundle[] bundles = bundleContext.getBundles();
            return Arrays.stream(bundles).map(b -> {
                return new Plugin() {
                    @Override
                    public String getName() {
                        return b.getSymbolicName();
                    }

                    @Override
                    public String getVersion() {
                        return b.getVersion().toString();
                    }
                };
            }).collect(toList());
        }

        private static Workflow createWorkflow(final WorkflowManager wfm, final List<NodeID> nodesToIgnore,
            final boolean includeExecutionInfo) {
            try (WorkflowLock lock = wfm.lock()) {
                final String name = wfm.getName();

                // Nodes
                Stream<NodeContainer> stream = wfm.getNodeContainers().stream();
                if (nodesToIgnore != null && !nodesToIgnore.isEmpty()) {
                    stream = stream.filter(nc -> !nodesToIgnore.contains(nc.getID()));
                }
                Comparator<NodeContainer> comp = Comparator.comparing((final NodeContainer n) -> {
                    Integer d = getGraphDepth(n);
                    // graph depth is null sometimes (don't know why)
                    return d == null ? -1 : d;
                }).thenComparing(NodeContainer::getID);

                final List<Node> nodes = stream.sorted(comp::compare)
                    .map(nc -> createNode(nc, nodesToIgnore, includeExecutionInfo)).collect(Collectors.toList());
                final List<String> annotations =
                    wfm.getWorkflowAnnotations().stream().map(wa -> wa.getData().getText()).collect(toList());
                final WorkflowMetadata metadata = createWorkflowMetadata(wfm);

                return new Workflow() {

                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    public List<Node> getNodes() {
                        return nodes;
                    }

                    @Override
                    public List<String> getAnnotations() {
                        return annotations;
                    }

                    @Override
                    public WorkflowMetadata getMetadata() {
                        return metadata;
                    }

                };
            }
        }

        private static Node createNode(final NodeContainer nc, final List<NodeID> nodesToIgnore,
            final boolean includeExecutionInfo) {
            //remove project wfm id
            final WorkflowManager projectWFM = nc.getParent().getProjectWFM();
            final String id = NodeIDSuffix.create(projectWFM.getID(), nc.getID()).toString();
            final String name = nc.getName();
            final String type = nc.getType().toString();
            final NodeFactoryKey factoryKey = nc instanceof NativeNodeContainer
                ? createNodeFactoryKey(((NativeNodeContainer)nc).getNode().getFactory()) : null;
            final Boolean isMetanode = nc instanceof WorkflowManager ? Boolean.TRUE : null;
            final Boolean isComponent = nc instanceof SubNodeContainer ? Boolean.TRUE : null;
            final String state = nc.getNodeContainerState().toString();
            final Integer graphDepth = WorkflowSummaryImpl.getGraphDepth(nc);
            List<Setting> settings = null;

            if (nc instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer)nc;
                if (nc.getNodeContainerState().isExecuted() && !snc.isInactive()) {
                    try {
                        settings = createSettings(snc.getModelSettingsUsingFlowObjectStack());
                    } catch (InvalidSettingsException ex) {
                        throw new IllegalStateException(
                            "Problem extracting settings of node '" + snc.getNameWithID() + "'", ex);
                    }
                } else {
                    NodeSettings nodeSettings = snc.getNodeSettings();
                    if (nodeSettings.containsKey("model")) {
                        try {
                            settings = createSettings(nodeSettings.getConfig("model"));
                        } catch (InvalidSettingsException ex) {
                            //can never happen - checked before
                        }
                    }
                }
            }

            final List<Setting> settingsList = settings;

            final Workflow subWorkflow;
            if (nc instanceof WorkflowManager) {
                subWorkflow = createWorkflow((WorkflowManager)nc, nodesToIgnore, includeExecutionInfo);
            } else if (nc instanceof SubNodeContainer) {
                subWorkflow =
                    createWorkflow(((SubNodeContainer)nc).getWorkflowManager(), nodesToIgnore, includeExecutionInfo);
            } else {
                subWorkflow = null;
            }

            final List<OutputPort> outputPorts = IntStream.range(0, nc.getNrOutPorts()).mapToObj(i -> {
                return createOutputPort(i, nc.getOutPort(i), nodesToIgnore, includeExecutionInfo);
            }).collect(toList());
            final String annotations =
                !nc.getNodeAnnotation().getData().isDefault() ? nc.getNodeAnnotation().getText() : null;
            final ExecutionStatistics executionStatistics = includeExecutionInfo && nc instanceof NativeNodeContainer
                ? createExecutionStatistics(nc.getNodeTimer()) : null;
            final Boolean isDeprecated =
                nc instanceof NativeNodeContainer && ((NativeNodeContainer)nc).getNode().getFactory().isDeprecated()
                    ? Boolean.TRUE : null;

            final String parentId;
            if (nc.getParent().isProject()) {
                parentId = null;
            } else {
                //remove project wfm id
                parentId = NodeIDSuffix.create(projectWFM.getID(), nc.getParent().getID()).toString();
            }

            final LinkInfo linkInfo = nc instanceof NodeContainerTemplate
                ? createLinkInfo(((NodeContainerTemplate)nc).getTemplateInformation()) : null;

            List<FlowVariable> variables = null;
            if (nc instanceof SingleNodeContainer) {
                // the outgoing stack of a SingleNodeContainer only contains variables owned by that node
                FlowObjectStack fos = ((SingleNodeContainer)nc).getOutgoingFlowObjectStack();
                if (fos != null) {
                    List<FlowVariable> vars = fos.getAllAvailableFlowVariables().values().stream()//
                        .filter(f -> f.getScope() == Scope.Flow)//
                        .map(WorkflowSummaryImpl::createFlowVariable).collect(Collectors.toList());
                    if (!vars.isEmpty()) {
                        variables = vars;
                    }
                }
            }

            final List<FlowVariable> flowVariables = variables;
            final JobManager jobManager = createJobManager(nc.getJobManager());
            final NodeMessage nodeMessage = createNodeMessage(nc.getNodeMessage());

            return new Node() {

                @Override
                public String getId() {
                    return id;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getType() {
                    return type;
                }

                @Override
                public NodeFactoryKey getFactoryKey() {
                    return factoryKey;
                }

                @Override
                public Boolean isMetanode() {
                    return isMetanode;
                }

                @Override
                public Boolean isComponent() {
                    return isComponent;
                }

                @Override
                public String getState() {
                    return state;
                }

                @Override
                public Integer getGraphDepth() {
                    return graphDepth;
                }

                @Override
                public List<Setting> getSettings() {
                    return settingsList;
                }

                @Override
                public Workflow getSubWorkflow() {
                    return subWorkflow;
                }

                @Override
                public List<OutputPort> getOutputs() {
                    return outputPorts;
                }

                @Override
                public String getAnnotation() {
                    return annotations;
                }

                @Override
                public ExecutionStatistics getExecutionStatistics() {
                    return executionStatistics;
                }

                @Override
                public JobManager getJobManager() {
                    return jobManager;
                }

                @Override
                public NodeMessage getNodeMessage() {
                    return nodeMessage;
                }

                @Override
                public Boolean isDeprecated() {
                    return isDeprecated;
                }

                @Override
                public String getParentId() {
                    return parentId;
                }

                @Override
                public LinkInfo getLinkInfo() {
                    return linkInfo;
                }

                @Override
                public List<FlowVariable> getFlowVariables() {
                    return flowVariables;
                }

            };
        }

        private static NodeFactoryKey createNodeFactoryKey(final NodeFactory<NodeModel> factory) {
            final String settingsString;
            if (factory instanceof DynamicNodeFactory) {
                final NodeSettings settings = new NodeSettings("settings");
                factory.saveAdditionalFactorySettings(settings);
                settingsString = JSONConfig.toJSONString(settings, WriterConfig.DEFAULT);

            } else {
                settingsString = null;
            }

            final String className = factory.getClass().getCanonicalName();

            return new NodeFactoryKey() {

                @Override
                public String getSettings() {
                    return settingsString;
                }

                @Override
                public String getClassName() {
                    return className;
                }
            };

        }

        private static NodeMessage createNodeMessage(final org.knime.core.node.workflow.NodeMessage msg) {
            if (msg == org.knime.core.node.workflow.NodeMessage.NONE) {
                return null;
            }

            final String type = msg.getMessageType().toString();
            final String message = msg.getMessage();

            return new NodeMessage() {

                @Override
                public String getType() {
                    return type;
                }

                @Override
                public String getMessage() {
                    return message;
                }

            };
        }

        private static ExecutionStatistics createExecutionStatistics(final NodeTimer nt) {
            if (nt.getStartTime() < 0) {
                return null;
            } else {
                final long startTime = nt.getStartTime();
                final long lastExecutionDuration = nt.getLastExecutionDuration();
                final long executionDurationSinceReset = nt.getExecutionDurationSinceReset();
                final long executionDurationSinceStart = nt.getExecutionDurationSinceStart();
                final int executionCountSinceReset = nt.getNrExecsSinceReset();
                final int executionCountSinceStart = nt.getNrExecsSinceStart();

                return new ExecutionStatistics() {

                    @Override
                    public String getLastExecutionStartTime() {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
                        return formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTime),
                            Clock.systemDefaultZone().getZone()));
                    }

                    @Override
                    public long getLastExecutionDuration() {
                        return lastExecutionDuration;
                    }

                    @Override
                    public long getExecutionDurationSinceReset() {
                        return executionDurationSinceReset;
                    }

                    @Override
                    public long getExecutionDurationSinceStart() {
                        return executionDurationSinceStart;
                    }

                    @Override
                    public int getExecutionCountSinceReset() {
                        return executionCountSinceReset;
                    }

                    @Override
                    public int getExecutionCountSinceStart() {
                        return executionCountSinceStart;
                    }

                };
            }
        }

        private static JobManager createJobManager(final NodeExecutionJobManager mgr) {
            if (mgr == null) {
                return null;
            }

            final String id = mgr.getID();
            final String name = NodeExecutionJobManagerPool.getJobManagerFactory(id).getLabel();

            final NodeSettings ns = new NodeSettings("job_manager_settings");
            mgr.save(ns);
            final List<Setting> settings = createSettings(ns);

            return new JobManager() {

                @Override
                public String getId() {
                    return id;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public List<Setting> getSettings() {
                    return settings;
                }

            };
        }

        private static WorkflowMetadata createWorkflowMetadata(final WorkflowManager wfm) {
            if (!wfm.isProject()) {
                return null;
            }
            final ReferencedFile rf = wfm.getProjectWFM().getWorkingDir();
            File metadataFile = new File(rf.getFile(), WorkflowPersistor.METAINFO_FILE);
            if (!metadataFile.exists()) {
                return null;
            }
            String[] meta = new String[5];
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                docFactory.setNamespaceAware(true);
                Document doc = docFactory.newDocumentBuilder().parse(metadataFile);
                doc.normalize();
                org.w3c.dom.Node root = doc.getChildNodes().item(0);
                //once a metadata version is present, we need to parse it differently here!
                //this should remind us
                assert root.getAttributes().getNamedItem(
                    MetadataXML.METADATA_VERSION) == null : "Implementation problem: metadata version not supported, yet";
                String prefix = root.getPrefix() == null ? "" : root.getPrefix();
                NodeList elements = doc.getElementsByTagName(prefix + ":" + MetadataXML.ATOM_ELEMENT);
                for (int i = 0; i < elements.getLength(); i++) {
                    org.w3c.dom.Node item = elements.item(i);
                    String name = item.getAttributes().getNamedItem(MetadataXML.NAME).getTextContent();
                    if (MetadataXML.AUTHOR_LABEL.equals(name)) {
                        meta[0] = item.getTextContent();
                    } else if (MetadataXML.CREATION_DATE_LABEL.equals(name)) {
                        meta[1] = item.getTextContent();
                    } else if (MetadataXML.DESCRIPTION_LABEL.equals(name)) {
                        meta[2] = item.getTextContent();
                    } else if ("Last Uploaded".equals(name)) {
                        meta[3] = item.getTextContent();
                    } else if ("Last Edited".equals(name)) {
                        meta[4] = item.getTextContent();
                    }
                }
            } catch (SAXException | IOException | ParserConfigurationException e) {
                throw new IllegalStateException(
                    "Something went wrong while extracting the workflow metadata from '" + metadataFile + "'", e);
            }
            return new WorkflowMetadata() {

                @Override
                public String getAuthor() {
                    return meta[0];
                }

                @Override
                public String getCreationDate() {
                    Calendar cal = MetaInfoFile.calendarFromDateString(meta[1]);
                    if (cal != null) {
                        //Only the date is provided here.
                        //The time is meaningless because it is attached to the date as constant value,
                        //see org.knime.workbench.descriptionview.metadata.atoms.DateMetaInfoAtom.TIME_OF_DAY_SUFFIX
                        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                        return formatter.format(cal.getTime());
                    }
                    return null;
                }

                @Override
                public String getDescription() {
                    return meta[2];
                }

                @Override
                public String getLastUploaded() {
                    return meta[3];
                }

                @Override
                public String getLastEdited() {
                    return meta[4];
                }

            };
        }

        private static OutputPort createOutputPort(final int index, final NodeOutPort p,
            final List<NodeID> nodesToIgnore, final boolean includeExecutionInfo) {
            final String type;
            if (p.getPortType().equals(BufferedDataTable.TYPE)) {
                type = "table";
            } else if (p.getPortType().equals(FlowVariablePortObject.TYPE)) {
                type = "flowvariable port";
            } else {
                type = p.getPortType().toString();
            }

            final TableSpec tableSpec = p.getPortObjectSpec() instanceof DataTableSpec
                ? createTableSpec((DataTableSpec)p.getPortObjectSpec()) : null;
            final String dataSummary =
                includeExecutionInfo && p.getPortObject() != null ? p.getPortObject().getSummary() : null;

            final SingleNodeContainer nc = p.getConnectedNodeContainer();
            final List<Successor> successors;
            if (nc != null) {
                List<Successor> res = nc.getParent().getOutgoingConnectionsFor(nc.getID(), index).stream()
                    .filter(cc -> nodesToIgnore == null || !nodesToIgnore.contains(cc.getDest()))
                    .map(cc -> createSuccessor(cc, nc.getParent().getProjectWFM())).sorted() //for deterministic results for testing
                    .collect(toList());
                successors = res.isEmpty() ? null : res;
            } else {
                successors = null;
            }

            final Boolean isInactive =
                p.getPortObjectSpec() instanceof InactiveBranchPortObjectSpec ? Boolean.TRUE : null;

            return new OutputPort() {

                @Override
                public String getType() {
                    return type;
                }

                @Override
                public TableSpec getTableSpec() {
                    return tableSpec;
                }

                @Override
                public int getIndex() {
                    return index;
                }

                @Override
                public String getDataSummary() {
                    return dataSummary;
                }

                @Override
                public List<Successor> getSuccessors() {
                    return successors;
                }

                @Override
                public Boolean isInactive() {
                    return isInactive;
                }
            };
        }

        private static Successor createSuccessor(final ConnectionContainer cc, final WorkflowManager projectWfm) {
            final String id = NodeIDSuffix.create(projectWfm.getID(), cc.getDest()).toString();
            final int portIndex = cc.getDestPort();

            return new Successor() {

                @Override
                public String getId() {
                    return id;
                }

                @Override
                public int getPortIndex() {
                    return portIndex;
                }

                @Override
                public int compareTo(final Successor o) {
                    //required to get deterministic results for testing
                    return id.compareTo(o.getId()) * 10 + portIndex - o.getPortIndex();
                }

            };
        }

        private static TableSpec createTableSpec(final DataTableSpec spec) {
            final List<Column> columns = IntStream.range(0, spec.getNumColumns()).mapToObj(i -> {
                return createColumn(i, spec.getColumnSpec(i));
            }).collect(toList());

            return new TableSpec() {

                @Override
                public List<Column> getColumns() {
                    return columns;
                }
            };
        }

        private static Column createColumn(final int index, final DataColumnSpec colSpec) {
            final String name = colSpec.getName();
            final List<ColumnProperty> columnProperties = createColumnProperties(colSpec.getProperties());
            final ColumnDomain culomnDomain = createColumnDomain(colSpec.getDomain());
            final String type = colSpec.getType().toString();

            return new Column() {

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public int getIndex() {
                    return index;
                }

                @Override
                public List<ColumnProperty> getColumnProperties() {
                    return columnProperties;
                }

                @Override
                public ColumnDomain getColumnDomain() {
                    return culomnDomain;
                }

                @Override
                public String getType() {
                    return type;
                }
            };
        }

        private static ColumnDomain createColumnDomain(final DataColumnDomain domain) {
            final List<String> values =
                domain.hasValues() ? domain.getValues().stream().map(DataCell::toString).collect(toList()) : null;
            final String upperBound = domain.hasUpperBound() ? domain.getUpperBound().toString() : null;
            final String lowerBound = domain.hasLowerBound() ? domain.getLowerBound().toString() : null;

            return new ColumnDomain() {

                @Override
                public List<String> getValues() {
                    return values;
                }

                @Override
                public String getUpperBound() {
                    return upperBound;
                }

                @Override
                public String getLowerBound() {
                    return lowerBound;
                }
            };
        }

        private static List<ColumnProperty> createColumnProperties(final DataColumnProperties props) {
            Enumeration<String> enumeration = props.properties();
            List<ColumnProperty> res = new ArrayList<>();
            while (enumeration.hasMoreElements()) {
                final String key = enumeration.nextElement();
                final String value = props.getProperty(key);
                res.add(new ColumnProperty() {

                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                });
            }
            return res;
        }

        private static FlowVariable createFlowVariable(final org.knime.core.node.workflow.FlowVariable var) {
            final String name = var.getName();
            final String type = var.getVariableType().getIdentifier();
            final String value = var.getValueAsString();

            return new FlowVariable() {

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getType() {
                    return type;
                }

                @Override
                public String getValue() {
                    return value;
                }

            };
        }

        private static List<Setting> createSettings(final Config config) {
            Iterator<String> iterator = config.iterator();
            List<Setting> res = new ArrayList<>();
            while (iterator.hasNext()) {
                final String key = iterator.next();
                final AbstractConfigEntry entry = config.getEntry(key);
                final ConfigEntries type = entry.getType();
                final String value = type != ConfigEntries.config ? entry.toStringValue() : null;
                final List<Setting> settings = type == ConfigEntries.config ? createSettings((Config)entry) : null;

                res.add(new Setting() {

                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                    @Override
                    public String getType() {
                        return type.toString();
                    }

                    @Override
                    public List<Setting> getSettings() {
                        return settings;
                    }

                });
            }
            return res;
        }

        private static LinkInfo createLinkInfo(final MetaNodeTemplateInformation info) {
            if (info.getRole() != Role.None) {
                final String sourceURI = info.getSourceURI() != null ? info.getSourceURI().toString() : null;
                final String timeStamp = info.getTimeStampString();
                final String updateStatus = info.getUpdateStatus() != null ? info.getUpdateStatus().name() : null;

                return new LinkInfo() {

                    @Override
                    public String getSourceURI() {
                        return sourceURI;
                    }

                    @Override
                    public String getTimeStamp() {
                        return timeStamp;
                    }

                    @Override
                    public String getUpdateStatus() {
                        return updateStatus;
                    }
                };
            } else {
                return null;
            }
        }
    }

}
