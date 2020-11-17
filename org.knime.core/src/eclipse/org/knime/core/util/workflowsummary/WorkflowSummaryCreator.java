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
import static org.knime.core.util.workflowsummary.WorkflowSummaryUtil.copy;

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
        // creates and materializes the 'hollow' workflow summary object into a 'solid' instance
        return copy(new WorkflowSummaryImpl(wfm, nodesToIgnore), includeExecutionInfo);
    }

    private static class WorkflowSummaryImpl implements WorkflowSummary {

        private WorkflowManager m_wfm;

        private List<NodeID> m_nodesToIgnore;

        WorkflowSummaryImpl(final WorkflowManager wfm, final List<NodeID> nodesToIgnore) {
            m_wfm = wfm;
            m_nodesToIgnore = nodesToIgnore;
        }

        @Override
        public String getVersion() {
            return V_1_0_0;
        }

        @Override
        public String getSummaryCreationDateTime() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            return formatter.format(ZonedDateTime.now());
        }

        @Override
        public Environment getEnvironment() {
            return createEnvironment();
        }

        @Override
        public Workflow getWorkflow() {
            try (WorkflowLock lock = m_wfm.lock()) {
                return createWorkflow(m_wfm, m_nodesToIgnore);
            }
        }

        private static Integer getGraphDepth(final NodeContainer nc) {
            Set<NodeGraphAnnotation> nga = nc.getParent().getNodeGraphAnnotation(nc.getID());
            if (!nga.isEmpty()) {
                return nga.iterator().next().getDepth();
            }
            return null;
        }

        private static Environment createEnvironment() {
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
                    return createInstallation();
                }

                @Override
                public Map<String, String> getSystemProperties() {
                    Properties sysProps = System.getProperties();
                    Map<String, String> res = new HashMap<>();
                    for (Entry<?, ?> entry : sysProps.entrySet()) {
                        res.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                    return res;
                }
            };
        }

        private static Installation createInstallation() {
            return new Installation() {

                @Override
                public List<Plugin> getPlugins() {
                    return createPlugins();
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

        private static Workflow createWorkflow(final WorkflowManager wfm, final List<NodeID> nodesToIgnore) {
            return new Workflow() {

                @Override
                public String getName() {
                    return wfm.getName();
                }

                @Override
                public List<Node> getNodes() {
                    Stream<NodeContainer> stream = wfm.getNodeContainers().stream();
                    if (nodesToIgnore != null && !nodesToIgnore.isEmpty()) {
                        stream = stream.filter(nc -> !nodesToIgnore.contains(nc.getID()));
                    }
                    Comparator<NodeContainer> comp = Comparator.comparing((final NodeContainer n) -> {
                        Integer d = getGraphDepth(n);
                        // graph depth is null sometimes (don't know why)
                        return d == null ? -1 : d;
                    }).thenComparing(NodeContainer::getID);
                    return stream.sorted(comp::compare).map(nc -> createNode(nc, nodesToIgnore)).collect(Collectors.toList());
                }

                @Override
                public List<String> getAnnotations() {
                    return wfm.getWorkflowAnnotations().stream().map(wa -> wa.getData().getText()).collect(toList());
                }

                @Override
                public WorkflowMetadata getMetadata() {
                    return createWorkflowMetadata(wfm);
                }

            };
        }

        private static Node createNode(final NodeContainer nc, final List<NodeID> nodesToIgnore) {
            return new Node() {

                @Override
                public String getId() {
                    //remove project wfm id
                    WorkflowManager projectWFM = nc.getParent().getProjectWFM();
                    return NodeIDSuffix.create(projectWFM.getID(), nc.getID()).toString();
                }

                @Override
                public String getName() {
                    return nc.getName();
                }

                @Override
                public String getType() {
                    return nc.getType().toString();
                }

                @Override
                public NodeFactoryKey getFactoryKey() {
                    if (nc instanceof NativeNodeContainer) {
                        return createNodeFactoryKey(((NativeNodeContainer)nc).getNode().getFactory());
                    }
                    return null;
                }

                @Override
                public Boolean isMetanode() {
                    if (nc instanceof WorkflowManager) {
                        return true;
                    }
                    return null;
                }

                @Override
                public Boolean isComponent() {
                    if (nc instanceof SubNodeContainer) {
                        return true;
                    }
                    return null;
                }

                @Override
                public String getState() {
                    return nc.getNodeContainerState().toString();
                }

                @Override
                public Integer getGraphDepth() {
                    return WorkflowSummaryImpl.getGraphDepth(nc);
                }

                @Override
                public List<Setting> getSettings() {
                    if (nc instanceof SingleNodeContainer) {
                        SingleNodeContainer snc = (SingleNodeContainer)nc;
                        if (nc.getNodeContainerState().isExecuted()) {
                            try {
                                return createSettings(snc.getModelSettingsUsingFlowObjectStack());
                            } catch (InvalidSettingsException ex) {
                                throw new IllegalStateException(
                                    "Problem extracting settings of node '" + snc.getNameWithID() + "'", ex);
                            }
                        } else {
                            NodeSettings nodeSettings = snc.getNodeSettings();
                            if (nodeSettings.containsKey("model")) {
                                try {
                                    return createSettings(nodeSettings.getConfig("model"));
                                } catch (InvalidSettingsException ex) {
                                    //can never happen - checked before
                                }
                            }
                        }
                    }
                    return null;
                }

                @Override
                public Workflow getSubWorkflow() {
                    if (nc instanceof WorkflowManager) {
                        return createWorkflow((WorkflowManager)nc, nodesToIgnore);
                    } else if (nc instanceof SubNodeContainer) {
                        return createWorkflow(((SubNodeContainer)nc).getWorkflowManager(), nodesToIgnore);
                    } else {
                        return null;
                    }
                }

                @Override
                public List<OutputPort> getOutputs() {
                    return IntStream.range(0, nc.getNrOutPorts()).mapToObj(i -> {
                        return createOutputPort(i, nc.getOutPort(i), nodesToIgnore);
                    }).collect(toList());
                }

                @Override
                public String getAnnotation() {
                    if (!nc.getNodeAnnotation().getData().isDefault()) {
                        return nc.getNodeAnnotation().getText();
                    }
                    return null;
                }

                @Override
                public ExecutionStatistics getExecutionStatistics() {
                    if (nc instanceof NativeNodeContainer) {
                        return createExecutionStatistics(nc.getNodeTimer());
                    }
                    return null;
                }

                @Override
                public JobManager getJobManager() {
                    return createJobManager(nc.getJobManager());
                }

                @Override
                public NodeMessage getNodeMessage() {
                    return createNodeMessage(nc.getNodeMessage());
                }

                @Override
                public Boolean isDeprecated() {
                    if (nc instanceof NativeNodeContainer
                        && ((NativeNodeContainer)nc).getNode().getFactory().isDeprecated()) {
                        return true;
                    } else {
                        return null;
                    }
                }

                @Override
                public String getParentId() {
                    if (nc.getParent().isProject()) {
                        return null;
                    } else {
                        //remove project wfm id
                        WorkflowManager projectWFM = nc.getParent().getProjectWFM();
                        return NodeIDSuffix.create(projectWFM.getID(), nc.getParent().getID()).toString();
                    }
                }

                @Override
                public LinkInfo getLinkInfo() {
                    if (nc instanceof NodeContainerTemplate) {
                        return createLinkInfo(((NodeContainerTemplate)nc).getTemplateInformation());
                    } else {
                        return null;
                    }
                }

                @Override
                public List<FlowVariable> getFlowVariables() {
                    if (nc instanceof SingleNodeContainer) {
                        // the outgoing stack of a SingleNodeContainer only contains variables owned by that node
                        FlowObjectStack fos = ((SingleNodeContainer)nc).getOutgoingFlowObjectStack();
                        if (fos != null) {
                            List<FlowVariable> vars = fos.getAllAvailableFlowVariables().values().stream()//
                                .filter(f -> f.getScope() == Scope.Flow)//
                                .map(WorkflowSummaryImpl::createFlowVariable).collect(Collectors.toList());
                            if (!vars.isEmpty()) {
                                return vars;
                            }
                        }
                    }
                    return null;
                }

            };
        }

        private static NodeFactoryKey createNodeFactoryKey(final NodeFactory<NodeModel> factory) {
            return new NodeFactoryKey() {

                @Override
                public String getSettings() {
                    if (factory instanceof DynamicNodeFactory) {
                        NodeSettings settings = new NodeSettings("settings");
                        factory.saveAdditionalFactorySettings(settings);
                        return JSONConfig.toJSONString(settings, WriterConfig.DEFAULT);

                    }
                    return null;
                }

                @Override
                public String getClassName() {
                    return factory.getClass().getCanonicalName();
                }
            };

        }

        private static NodeMessage createNodeMessage(final org.knime.core.node.workflow.NodeMessage msg) {
            if (msg == org.knime.core.node.workflow.NodeMessage.NONE) {
                return null;
            }
            return new NodeMessage() {

                @Override
                public String getType() {
                    return msg.getMessageType().toString();
                }

                @Override
                public String getMessage() {
                    return msg.getMessage();
                }

            };
        }

        private static ExecutionStatistics createExecutionStatistics(final NodeTimer nt) {
            if (nt.getStartTime() < 0) {
                return null;
            } else {
                return new ExecutionStatistics() {

                    @Override
                    public String getLastExecutionStartTime() {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
                        return formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(nt.getStartTime()),
                            Clock.systemDefaultZone().getZone()));
                    }

                    @Override
                    public long getLastExecutionDuration() {
                        return nt.getLastExecutionDuration();
                    }

                    @Override
                    public long getExecutionDurationSinceReset() {
                        return nt.getExecutionDurationSinceReset();
                    }

                    @Override
                    public long getExecutionDurationSinceStart() {
                        return nt.getExecutionDurationSinceStart();
                    }

                    @Override
                    public int getExecutionCountSinceReset() {
                        return nt.getNrExecsSinceReset();
                    }

                    @Override
                    public int getExecutionCountSinceStart() {
                        return nt.getNrExecsSinceStart();
                    }

                };
            }
        }

        private static JobManager createJobManager(final NodeExecutionJobManager mgr) {
            if (mgr == null) {
                return null;
            }
            return new JobManager() {

                @Override
                public String getId() {
                    return mgr.getID();
                }

                @Override
                public String getName() {
                    return NodeExecutionJobManagerPool.getJobManagerFactory(mgr.getID()).getLabel();
                }

                @Override
                public List<Setting> getSettings() {
                    NodeSettings ns = new NodeSettings("job_manager_settings");
                    mgr.save(ns);
                    return createSettings(ns);
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
            final List<NodeID> nodesToIgnore) {
            return new OutputPort() {

                @Override
                public String getType() {
                    if (p.getPortType().equals(BufferedDataTable.TYPE)) {
                        return "table";
                    } else if (p.getPortType().equals(FlowVariablePortObject.TYPE)) {
                        return "flowvariable port";
                    }
                    return p.getPortType().toString();
                }

                @Override
                public TableSpec getTableSpec() {
                    if (p.getPortObjectSpec() instanceof DataTableSpec) {
                        return createTableSpec((DataTableSpec)p.getPortObjectSpec());
                    } else {
                        return null;
                    }
                }

                @Override
                public int getIndex() {
                    return index;
                }

                @Override
                public String getDataSummary() {
                    if (p.getPortObject() != null) {
                        return p.getPortObject().getSummary();
                    } else {
                        return null;
                    }
                }

                @Override
                public List<Successor> getSuccessors() {
                    SingleNodeContainer nc = p.getConnectedNodeContainer();
                    if (nc != null) {
                        List<Successor> res = nc.getParent().getOutgoingConnectionsFor(nc.getID(), index).stream()
                            .filter(
                                cc -> nodesToIgnore == null || !nodesToIgnore.contains(cc.getDest()))
                            .map(cc -> createSuccessor(cc, nc.getParent().getProjectWFM())).sorted() //for deterministic results for testing
                            .collect(toList());
                        return res.isEmpty() ? null : res;
                    }
                    return null;
                }

                @Override
                public Boolean isInactive() {
                    if (p.getPortObjectSpec() instanceof InactiveBranchPortObjectSpec) {
                        return true;
                    }
                    return null;
                }
            };
        }

        private static Successor createSuccessor(final ConnectionContainer cc, final WorkflowManager projectWfm) {
            final String id = NodeIDSuffix.create(projectWfm.getID(), cc.getDest()).toString();
            return new Successor() {

                @Override
                public String getId() {
                    return id;
                }

                @Override
                public int getPortIndex() {
                    return cc.getDestPort();
                }

                @Override
                public int compareTo(final Successor o) {
                    //required to get deterministic results for testing
                    return getId().compareTo(o.getId()) * 10 + getPortIndex() - o.getPortIndex();
                }

            };
        }

        private static TableSpec createTableSpec(final DataTableSpec spec) {
            return new TableSpec() {

                @Override
                public List<Column> getColumns() {
                    return IntStream.range(0, spec.getNumColumns()).mapToObj(i -> {
                        return createColumn(i, spec.getColumnSpec(i));
                    }).collect(toList());
                }
            };
        }

        private static Column createColumn(final int index, final DataColumnSpec colSpec) {
            return new Column() {

                @Override
                public String getName() {
                    return colSpec.getName();
                }

                @Override
                public int getIndex() {
                    return index;
                }

                @Override
                public List<ColumnProperty> getColumnProperties() {
                    return createColumnProperties(colSpec.getProperties());
                }

                @Override
                public ColumnDomain getColumnDomain() {
                    return createColumnDomain(colSpec.getDomain());
                }

                @Override
                public String getType() {
                    return colSpec.getType().toString();
                }
            };
        }

        private static ColumnDomain createColumnDomain(final DataColumnDomain domain) {
            return new ColumnDomain() {

                @Override
                public List<String> getValues() {
                    if (domain.hasValues()) {
                        return domain.getValues().stream().map(DataCell::toString).collect(toList());
                    }
                    return null;
                }

                @Override
                public String getUpperBound() {
                    if (domain.hasUpperBound()) {
                        return domain.getUpperBound().toString();
                    }
                    return null;
                }

                @Override
                public String getLowerBound() {
                    if (domain.hasLowerBound()) {
                        return domain.getLowerBound().toString();
                    }
                    return null;
                }
            };
        }

        private static List<ColumnProperty> createColumnProperties(final DataColumnProperties props) {
            Enumeration<String> enumeration = props.properties();
            List<ColumnProperty> res = new ArrayList<>();
            while (enumeration.hasMoreElements()) {
                String key = enumeration.nextElement();
                res.add(new ColumnProperty() {

                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return props.getProperty(key);
                    }

                });
            }
            return res;
        }

        private static FlowVariable createFlowVariable(final org.knime.core.node.workflow.FlowVariable var) {
            return new FlowVariable() {

                @Override
                public String getName() {
                    return var.getName();
                }

                @Override
                public String getType() {
                    return var.getVariableType().getIdentifier();
                }

                @Override
                public String getValue() {
                    return var.getValueAsString();
                }

            };
        }

        private static List<Setting> createSettings(final Config config) {
            Iterator<String> iterator = config.iterator();
            List<Setting> res = new ArrayList<>();
            while (iterator.hasNext()) {
                String key = iterator.next();
                AbstractConfigEntry entry = config.getEntry(key);
                res.add(new Setting() {

                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        if (entry.getType() != ConfigEntries.config) {
                            return entry.toStringValue();
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public String getType() {
                        return entry.getType().toString();
                    }

                    @Override
                    public List<Setting> getSettings() {
                        if (entry.getType() == ConfigEntries.config) {
                            return createSettings((Config)entry);
                        } else {
                            return null;
                        }
                    }

                });
            }
            return res;
        }

        private static LinkInfo createLinkInfo(final MetaNodeTemplateInformation info) {
            info.getSourceURI();
            if (info.getRole() != Role.None) {
                return new LinkInfo() {

                    @Override
                    public String getSourceURI() {
                        return info.getSourceURI() != null ? info.getSourceURI().toString() : null;
                    }

                    @Override
                    public String getTimeStamp() {
                        return info.getTimeStampString();
                    }

                    @Override
                    public String getUpdateStatus() {
                        return info.getUpdateStatus() != null ? info.getUpdateStatus().name() : null;
                    }
                };
            } else {
                return null;
            }
        }
    }

}
