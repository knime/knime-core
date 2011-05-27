/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   Sep 18, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodePersistor.LoadNodeModelSettingsFailPolicy;
import org.knime.core.node.NodePersistorVersion1xx;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class SingleNodeContainerPersistorVersion1xx
    implements SingleNodeContainerPersistor {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final String m_versionString;

    private Node m_node;

    /** Meta persistor, only set when used to load a workflow. */
    private final NodeContainerMetaPersistorVersion1xx m_metaPersistor;
    /** WFM persistor, only set when used to load a workflow. */
    private final WorkflowPersistorVersion1xx m_wfmPersistor;

    private NodeSettingsRO m_nodeSettings;
    private SingleNodeContainerSettings m_sncSettings;
    private boolean m_needsResetAfterLoad;
    private boolean m_isDirtyAfterLoad;
    private List<FlowObject> m_flowObjects;
    private LoadNodeModelSettingsFailPolicy m_settingsFailPolicy;

    /** Load persistor. */
    SingleNodeContainerPersistorVersion1xx(
            final WorkflowPersistorVersion1xx workflowPersistor,
            final ReferencedFile nodeSettingsFile,
            final WorkflowLoadHelper loadHelper,
            final String versionString) {
        this(workflowPersistor, new NodeContainerMetaPersistorVersion1xx(
                nodeSettingsFile, loadHelper), versionString);
    }

    /** Constructor used internally, not used outside this class or its
     * derivates.
     * @param versionString
     * @param metaPersistor
     * @param wfmPersistor
     */
    SingleNodeContainerPersistorVersion1xx(
            final WorkflowPersistorVersion1xx wfmPersistor,
            final NodeContainerMetaPersistorVersion1xx metaPersistor,
            final String versionString) {
        if (versionString == null || wfmPersistor == null) {
            throw new NullPointerException();
        }
        m_versionString = versionString;
        m_metaPersistor = metaPersistor;
        m_wfmPersistor = wfmPersistor;
    }

    protected final String getVersionString() {
        return m_versionString;
    }

    protected NodeLogger getLogger() {
        return m_logger;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    /** Indicate that node should be reset after load (due to load problems). */
    public void setNeedsResetAfterLoad() {
        m_needsResetAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return m_isDirtyAfterLoad;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return true;
    }

    /** Mark as dirty. */
    protected void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }

    @Override
    public NodeContainerMetaPersistorVersion1xx getMetaPersistor() {
        return m_metaPersistor;
    }

    public WorkflowLoadHelper getLoadHelper() {
        return m_metaPersistor.getLoadHelper();
    }

    /** {@inheritDoc} */
    @Override
    public Node getNode() {
        return m_node;
    }

    /** {@inheritDoc} */
    @Override
    public SingleNodeContainerSettings getSNCSettings() {
        return m_sncSettings;
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowObject> getFlowObjects() {
        return m_flowObjects;
    }

    /** {@inheritDoc} */
    @Override
    public SingleNodeContainer getNodeContainer(
            final WorkflowManager wm, final NodeID id) {
        return new SingleNodeContainer(wm, id, this);
    }

    protected NodePersistorVersion1xx createNodePersistor() {
        return new NodePersistorVersion1xx(this, null);
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final NodeSettingsRO parentSettings,
            final LoadResult result)
    throws InvalidSettingsException, IOException {
        NodeContainerMetaPersistorVersion1xx meta = getMetaPersistor();
        final ReferencedFile settingsFileRef = meta.getNodeSettingsFile();
        File settingsFile = settingsFileRef.getFile();
        String error;
        if (!settingsFile.isFile()) {
            setDirtyAfterLoad();
            throw new IOException("Can't read node file \""
                    + settingsFile.getAbsolutePath() + "\"");
        }
        NodeSettingsRO settings;
        try {
            settings = NodeSettings.loadFromXML(
                    new BufferedInputStream(new FileInputStream(settingsFile)));
        } catch (IOException ioe) {
            setDirtyAfterLoad();
            throw ioe;
        }
        String nodeFactoryClassName;
        try {
            nodeFactoryClassName = loadNodeFactoryClassName(
                    parentSettings, settings);
        } catch (InvalidSettingsException e) {
            if (settingsFile.getName().equals(
                    WorkflowPersistor.WORKFLOW_FILE)) {
                error = "Can't load meta flows in this version";
            } else {
                error = "Can't load node factory class name";
            }
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        NodeFactory<NodeModel> nodeFactory;

        try {
            nodeFactory = loadNodeFactory(nodeFactoryClassName);
        } catch (Throwable e) {
            error =  "Unable to load factory class \""
                + nodeFactoryClassName + "\"";
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        m_node = new Node(nodeFactory);
        boolean resetRequired = meta.load(settings, parentSettings, result);
        m_nodeSettings = settings;
        if (resetRequired) {
            setNeedsResetAfterLoad();
            setDirtyAfterLoad();
        }
        if (meta.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult result)
    throws InvalidSettingsException, CanceledExecutionException, IOException {
        String nodeFileName;
        try {
            nodeFileName = loadNodeFile(m_nodeSettings);
        } catch (InvalidSettingsException e) {
            String error =
                "Unable to load node settings file for node with ID suffix "
                    + m_metaPersistor.getNodeIDSuffix() + " (node \""
                    + m_node.getName() + "\"): " + e.getMessage();
            result.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            return;
        }
        ReferencedFile nodeDir = getMetaPersistor().getNodeContainerDirectory();
        ReferencedFile nodeFile = new ReferencedFile(nodeDir, nodeFileName);
        m_settingsFailPolicy =
            translateToFailPolicy(m_metaPersistor.getState());
        NodePersistorVersion1xx nodePersistor = createNodePersistor();
        try {
            HashMap<Integer, ContainerTable> globalTableRepository =
                getWorkflowManagerPersistor().getGlobalTableRepository();
            nodePersistor.load(m_node, nodeFile,
                    exec, tblRep, globalTableRepository, result);
        } catch (final Exception e) {
            String error = "Error loading node content: " + e.getMessage();
            getLogger().warn(error, e);
            needsResetAfterLoad();
            result.addError(error);
        }
        try {
            m_flowObjects = loadFlowObjects(m_nodeSettings);
        } catch (Exception e) {
            m_flowObjects = Collections.emptyList();
            String error = "Error loading flow variables: "
                + e.getMessage();
            getLogger().warn(error, e);
            result.addError(error);
            setDirtyAfterLoad();
            needsResetAfterLoad();
        }
        try {
            m_sncSettings = loadSNCSettings(m_nodeSettings, nodePersistor);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load SNC settings: " + e.getMessage();
            result.addError(error);
            getLogger().debug(error, e);
            m_sncSettings = new SingleNodeContainerSettings();
            setDirtyAfterLoad();
            return;
        }
        if (nodePersistor.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }
        if (nodePersistor.needsResetAfterLoad()) {
            setNeedsResetAfterLoad();
        }
        exec.setProgress(1.0);
    }

    /** Load factory name.
     * @param parentSettings settings of outer workflow (old style workflows
     *        have it in there)
     * @param settings settings of this node, ignored in this implementation
     * @return Name of factory
     * @throws InvalidSettingsException If that fails for any reason.
     */
    protected String loadNodeFactoryClassName(
            final NodeSettingsRO parentSettings, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String factoryName = parentSettings.getString(KEY_FACTORY_NAME);

        // This is a hack to load old J48 Nodes Model from pre-2.0 workflows
        if ("org.knime.ext.weka.j48_2.WEKAJ48NodeFactory2".equals(factoryName)
                || "org.knime.ext.weka.j48.WEKAJ48NodeFactory".equals(factoryName)) {
            return "org.knime.ext.weka.knimeJ48.KnimeJ48NodeFactory";
        } else {
            return factoryName;
        }
    }

    @SuppressWarnings("unchecked")
    protected NodeFactory<NodeModel> loadNodeFactory(
            final String factoryClassName) throws InvalidSettingsException,
            InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        // use global Class Creator utility for Eclipse "compatibility"
        try {
            NodeFactory<NodeModel> f = (NodeFactory<NodeModel>)((GlobalClassCreator
                    .createClass(factoryClassName)).newInstance());
            return f;
        } catch (ClassNotFoundException ex) {
            String[] x = factoryClassName.split("\\.");
            String simpleClassName = x[x.length - 1];

            for (String s : NodeFactory.getLoadedNodeFactories()) {
                if (s.endsWith("." + simpleClassName)) {
                    NodeFactory<NodeModel> f = (NodeFactory<NodeModel>)((GlobalClassCreator
                            .createClass(s)).newInstance());
                    getLogger().warn("Substituted '" + f.getClass().getName()
                            + "' for unknown factory '" + factoryClassName
                            + "'");
                    return f;
                }
            }
            throw ex;
        }
    }

    /** Load Name of file containing node settings.
     * @param settings to load from, used in sub-classes, ignored here.
     * @return "settings.xml"
     * @throws InvalidSettingsException If that fails for any reason.
     */
    protected String loadNodeFile(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return SETTINGS_FILE_NAME;
    }

    /** Load configuration of node.
     * @param settings to load from (used in sub-classes)
     * @param nodePersistor persistor to allow this implementation to load
     *        old-style
     * @return node config
     * @throws InvalidSettingsException if that fails for any reason.
     */
    protected SingleNodeContainerSettings loadSNCSettings(
            final NodeSettingsRO settings,
            final NodePersistorVersion1xx nodePersistor)
        throws InvalidSettingsException {
        NodeSettingsRO s = nodePersistor.getSettings();
        SingleNodeContainerSettings sncs = new SingleNodeContainerSettings();
        // in versions before KNIME 1.2.0, there were no misc settings
        // in the dialog, we must use caution here: if they are not present
        // we use the default, i.e. small data are kept in memory
        MemoryPolicy p;
        if (s.containsKey(Node.CFG_MISC_SETTINGS)
                && s.getNodeSettings(Node.CFG_MISC_SETTINGS).containsKey(
                        SingleNodeContainer.CFG_MEMORY_POLICY)) {
            NodeSettingsRO sub =
                    s.getNodeSettings(Node.CFG_MISC_SETTINGS);
            String memoryPolicy =
                    sub.getString(SingleNodeContainer.CFG_MEMORY_POLICY,
                            MemoryPolicy.CacheSmallInMemory.toString());
            if (memoryPolicy == null) {
                throw new InvalidSettingsException(
                        "Can't use null memory policy.");
            }
            try {
                p = MemoryPolicy.valueOf(memoryPolicy);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException(
                        "Invalid memory policy: " + memoryPolicy);
            }
        } else {
            p = MemoryPolicy.CacheSmallInMemory;
        }
        sncs.setMemoryPolicy(p);
        return sncs;
    }

    /** Load from variables.
     * @param settings to load from, ignored in this implementation
     *        (flow variables added in later versions)
     * @return an empty list.
     * @throws InvalidSettingsException if that fails for any reason.
     */
    protected List<FlowObject> loadFlowObjects(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return Collections.emptyList();
    }

    protected boolean shouldFixModelPortOrder() {
        return true;
    }

    WorkflowPersistorVersion1xx getWorkflowManagerPersistor() {
        return m_wfmPersistor;
    }

    public boolean mustWarnOnDataLoadError() {
        return getWorkflowManagerPersistor().mustWarnOnDataLoadError();
    }

    /**
     * @return the settingsFailPolicy
     */
    public LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy() {
        return m_settingsFailPolicy;
    }

    static final LoadNodeModelSettingsFailPolicy translateToFailPolicy(
            final State nodeState) {
        switch (nodeState) {
        case IDLE:
            return LoadNodeModelSettingsFailPolicy.IGNORE;
        case EXECUTED:
            return LoadNodeModelSettingsFailPolicy.WARN;
        default:
            return LoadNodeModelSettingsFailPolicy.FAIL;
        }
    }
}
