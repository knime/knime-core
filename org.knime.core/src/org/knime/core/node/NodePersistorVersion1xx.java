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
 *   Sep 11, 2007 (wiswedel): created
 */
package org.knime.core.node;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion1xx;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;

public class NodePersistorVersion1xx implements NodePersistor {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private SingleNodeContainerPersistorVersion1xx
        m_sncPersistor;

    private boolean m_isExecuted;

    private boolean m_hasContent;

    private boolean m_isInactive;

    private boolean m_isConfigured;

    private ReferencedFile m_nodeDirectory;

    private ReferencedFile m_nodeInternDirectory;

    private NodeSettingsRO m_modelSettings;

    private PortObject[] m_portObjects;

    private PortObjectSpec[] m_portObjectSpecs;

    private String[] m_portObjectSummaries;

    private BufferedDataTable[] m_internalHeldTables;

    private boolean m_needsResetAfterLoad;

    private boolean m_isDirtyAfterLoad;

    private String m_warningMessage;

    /** Load Version, see {@link #getLoadVersion()} for details. */
    private final LoadVersion m_loadVersion;

    /** List of factories (only the simple class name), which were
     * auto-executable in 1.3.x and need to be restored as configured only. */
    public static final List<String> OLD_AUTOEXECUTABLE_NODEFACTORIES =
        Arrays.asList("InteractivePieNodeFactory", "HistogramNodeFactory",
                "JmolViewerNodeFactory", "TableNodeFactory");

    static String createDataFileDirName(final int index) {
        return DATA_FILE_PREFIX + index;
    }

    static String createModelFileName(final int index) {
        return MODEL_FILE_PREFIX + index + ".pmml.gz";
    }

    /** Creates persistor for both load and save.
     * @param sncPersistor The corresponding SNC persistor.
     * @param version The version string, see
     * {@link #getLoadVersion()} for details
     */
    public NodePersistorVersion1xx(
            final SingleNodeContainerPersistorVersion1xx sncPersistor,
            final LoadVersion version) {
        m_sncPersistor = sncPersistor;
        m_loadVersion = version;
    }

    protected NodeLogger getLogger() {
        return m_logger;
    }

    /** Version being loaded. This is given by the SNC-Persistor. If this
     * persistor is used for saving, the value of this field is unimportant
     * (will be the latest version).
     * @return Version being loaded (or saved). Can also be null unless
     * enforced in constructor of subclass. */
    public LoadVersion getLoadVersion() {
        return m_loadVersion;
    }

    protected boolean loadIsExecuted(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getBoolean(CFG_ISEXECUTED);
    }

    protected boolean loadHasContent(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return settings.getBoolean(CFG_ISEXECUTED);
    }

    protected boolean loadIsInactive(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return false;
    }

    /** Sub class hook to read warning message.
     * @param settings Ignored
     * @return null
     * @throws InvalidSettingsException Not actually thrown
     */
    protected String loadWarningMessage(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return null;
    }

    protected boolean loadIsConfigured(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getBoolean(CFG_ISCONFIGURED);
    }

    static ReferencedFile getNodeInternDirectory(final ReferencedFile nodeDir) {
        return new ReferencedFile(nodeDir, INTERN_FILE_DIR);
    }

    /** Load internal directory.
     * @param settings Ignored (but allows sub-classing)
     * @param nodeDir Node directory.
     * @return Internal directory.
     * @throws InvalidSettingsException If that fails for any reason.
     */
    protected ReferencedFile loadNodeInternDirectory(final NodeSettingsRO settings,
            final ReferencedFile nodeDir) throws InvalidSettingsException {
        return getNodeInternDirectory(nodeDir);
    }

    protected void loadPorts(final Node node,
            final ExecutionMonitor execMon, final NodeSettingsRO settings,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep)
            throws IOException, InvalidSettingsException,
            CanceledExecutionException {
        // skip flow variables port (introduced in v2.2)
        for (int i = 1; i < node.getNrOutPorts(); i++) {
            int oldIndex = getOldPortIndex(i);
            ExecutionMonitor execPort = execMon
                    .createSubProgress(1.0 / node.getNrOutPorts());
            execMon.setMessage("Port " + oldIndex);
            PortType type = node.getOutputType(i);
            boolean isDataPort = BufferedDataTable.class.isAssignableFrom(type
                    .getPortObjectClass());
            if (m_isConfigured) {
                PortObjectSpec spec =
                    loadPortObjectSpec(node, settings, oldIndex);
                setPortObjectSpec(i, spec);
            }
            if (m_isExecuted) {
                PortObject object;
                if (isDataPort) {
                    object = loadBufferedDataTable(node, settings, execPort,
                            loadTblRep, oldIndex, tblRep);
                } else {
                    throw new IOException("Can't restore model ports of "
                            + "old 1.x workflows. Execute node again.");
                }
                String summary = object != null ? object.getSummary() : null;
                setPortObject(i, object);
                setPortObjectSummary(i, summary);
            }
            execPort.setProgress(1.0);
        }
    }

    /** Subtracts one from the argument. As of v2.2 KNIME has an additional
     * output port (index 0) carrying flow variables.
     * @param loaded Index of port in current version
     * @return Old port index (1 becomes 0, etc)
     */
    private int getOldPortIndex(final int loaded) {
        return loaded - 1;
    }

    /** Adds one to the argument. As of v2.2 KNIME has an additional
     * output port (index 0) carrying flow variables.
     * @param loaded Index of port in version 1.x
     * @return New port index (0 becomes 1, etc)
     */
    private int getNewPortIndex(final int old) {
        return old + 1;
    }

    /** Sub class hook to read internal tables.
     * @param node Ignored.
     * @param execMon Ignored.
     * @param settings Ignored.
     * @param loadTblRep Ignored.
     * @param tblRep Ignored.
     * @throws IOException Not actually thrown.
     * @throws InvalidSettingsException Not actually thrown.
     * @throws CanceledExecutionException Not actually thrown.
     */
    protected void loadInternalHeldTables(final Node node,
            final ExecutionMonitor execMon,
            final NodeSettingsRO settings,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep)
    throws IOException, InvalidSettingsException, CanceledExecutionException {
        // sub class hook
    }

    private BufferedDataTable loadBufferedDataTable(final Node node,
            final NodeSettingsRO settings, final ExecutionMonitor execMon,
            final Map<Integer, BufferedDataTable> loadTblRep, final int index,
            final HashMap<Integer, ContainerTable> tblRep)
            throws InvalidSettingsException, IOException,
            CanceledExecutionException {
        // in 1.1.x and before the settings.xml contained the location
        // of the data table specs file (spec_0.xml, e.g.). From 1.2.0 on,
        // the spec is saved in data/data_0/spec.xml
        boolean isVersion11x = settings.containsKey(CFG_SPEC_FILES);
        ExecutionMonitor execSubData = execMon.createSubProgress(0.25);
        if (isVersion11x) {
            /* In version 1.1.x the data was stored in a different way. The
             * data.xml that is now contained in the data/data_x/ directory was
             * aggregated in a data.xml file directly in the m_nodeDir. Also the
             * spec was located at a different location.
             */
            String dataConfigFileName = settings.getString(CFG_DATA_FILE);
            File nodeDir = m_nodeDirectory.getFile();
            // dataConfigFile = data.xml in node dir
            File dataConfigFile = new File(nodeDir, dataConfigFileName);
            NodeSettingsRO dataSettings = NodeSettings
                    .loadFromXML(new BufferedInputStream(new FileInputStream(
                            dataConfigFile)));
            String dataPath = dataSettings.getString(CFG_DATA_FILE_DIR);
            // dataDir = /data
            ReferencedFile dataDirRef =
                new ReferencedFile(m_nodeDirectory, dataPath);
            // note: we do not check for existence here - in some cases
            // this directory may not exist (when exported and empty
            // directories are pruned)
            NodeSettingsRO portSettings = dataSettings
                    .getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            // dir = /data/data_i
            ReferencedFile dirRef = new ReferencedFile(dataDirRef, dataName);
            BufferedDataTable t = BufferedDataTable.loadFromFile(
                    dirRef, portSettings, execSubData, loadTblRep,
                    // we didn't have blobs in 1.1.x
                    new HashMap<Integer, ContainerTable>());
            t.setOwnerRecursively(node);
            return t;
        } else {
            NodeSettingsRO dataSettings = settings
                    .getNodeSettings(CFG_DATA_FILE);
            String dataDirStr = dataSettings.getString(CFG_DATA_FILE_DIR);
            ReferencedFile dataDirRef =
                new ReferencedFile(m_nodeDirectory, dataDirStr);
            NodeSettingsRO portSettings = dataSettings
                    .getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            ReferencedFile dirRef = new ReferencedFile(dataDirRef, dataName);
            File dir = dirRef.getFile();
            if (!(dir.isDirectory() && dir.canRead())) {
                throw new IOException("Can not read directory "
                        + dir.getAbsolutePath());
            }
            BufferedDataTable t = BufferedDataTable.loadFromFile(dirRef,
                /* ignored in 1.2.0+ */
                null, execMon, loadTblRep, tblRep);
            t.setOwnerRecursively(node);
            return t;
        }
    }

    private PortObjectSpec loadPortObjectSpec(final Node node,
            final NodeSettingsRO settings, final int index)
            throws InvalidSettingsException, IOException {
        int newIndex = getNewPortIndex(index);
        PortType type = node.getOutputType(newIndex);
        boolean isDataPort = BufferedDataTable.class.isAssignableFrom(type
                .getPortObjectClass());
        if (!isDataPort) {
            // port is a model port, no spec available in 1.x.x
            return null;
        }
        // in 1.1.x and before the settings.xml contained the location
        // of the data table specs file (spec_0.xml, e.g.). From 1.2.0 on,
        // the spec is saved in data/data_0/spec.xml
        boolean isVersion11x = settings.containsKey(CFG_SPEC_FILES);
        if (isVersion11x) {
            NodeSettingsRO spec = settings.getNodeSettings(CFG_SPEC_FILES);
            String specName = spec.getString(CFG_OUTPUT_PREFIX + index);
            ReferencedFile targetFileRef =
                new ReferencedFile(m_nodeDirectory, specName);
            File targetFile = targetFileRef.getFile();
            DataTableSpec outSpec = null;
            if (targetFile.exists()) {
                NodeSettingsRO settingsSpec = NodeSettings
                        .loadFromXML(new BufferedInputStream(
                                new FileInputStream(targetFile)));
                outSpec = DataTableSpec.load(settingsSpec);
            }
            return outSpec;
        } else {
            NodeSettingsRO dataSettings = settings
                    .getNodeSettings(CFG_DATA_FILE);
            String dataDirStr = dataSettings.getString(CFG_DATA_FILE_DIR);
            ReferencedFile dataDirRef =
                new ReferencedFile(m_nodeDirectory, dataDirStr);
            NodeSettingsRO portSettings = dataSettings
                    .getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            DataTableSpec outSpec = null;
            if (portSettings.getBoolean(CFG_HAS_SPEC_FILE, true)) {
                ReferencedFile dirRef =
                    new ReferencedFile(dataDirRef, dataName);
                File dir = dirRef.getFile();
                if (!(dir.isDirectory() && dir.canRead())) {
                    throw new IOException("Can not read directory "
                            + dir.getAbsolutePath());
                }
                outSpec = BufferedDataTable.loadSpec(dirRef);
                if (portSettings.containsKey(CFG_HAS_SPEC_FILE)
                        && outSpec == null) {
                    throw new IOException("No spec file available for"
                            + " outport " + index + ".");
                }
            }
            return outSpec;
        }
    }

    /**
     * @return the singleNodeContainerPersistor
     */
    public SingleNodeContainerPersistorVersion1xx
        getSingleNodeContainerPersistor() {
        return m_sncPersistor;
    }

    /** Is configured according to the settings object.
     * @return If node is saved in configured state. */
    @Override
    public boolean isConfigured() {
        return m_isConfigured;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExecuted() {
        return m_isExecuted;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasContent() {
        return m_hasContent;
    }

    /** Accessor for derived class, not part of interface!
     * @return Inactive (dead IF branch) */
    protected boolean isInactive() {
        return m_isInactive;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    /** {@inheritDoc} */
    @Override
    public void setNeedsResetAfterLoad() {
        m_needsResetAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return m_isDirtyAfterLoad;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return getSingleNodeContainerPersistor().mustWarnOnDataLoadError();
    }

    /** Loads content into node instance.
     * @param node The target node, used for meta info (#ports, e.g) and to
     *  invoke the
     *  {@link Node#load(NodePersistor, ExecutionMonitor, LoadResult)} on
     * @param configFileRef The configuration file for the node.
     * @param exec For progress/cancelation
     * @param loadTblRep The table repository used during load
     * @param tblRep The table repository for blob handling
     * @param loadResult where to add errors to
     * @throws IOException If files can't be read
     * @throws CanceledExecutionException If canceled
     */
    public void load(final Node node, final ReferencedFile configFileRef,
            final ExecutionMonitor exec,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep,
            final LoadResult loadResult)
            throws IOException, CanceledExecutionException {
        ExecutionMonitor settingsExec = exec.createSilentSubProgress(0.1);
        ExecutionMonitor loadExec = exec.createSilentSubProgress(0.7);
        ExecutionMonitor loadIntTblsExec = exec.createSilentSubProgress(0.1);
        ExecutionMonitor createExec = exec.createSilentSubProgress(0.1);
        exec.setMessage("settings");
        m_portObjects = new PortObject[node.getNrOutPorts()];
        m_portObjectSpecs = new PortObjectSpec[node.getNrOutPorts()];
        m_portObjectSummaries = new String[node.getNrOutPorts()];
        m_nodeDirectory = configFileRef.getParent();
        if (m_nodeDirectory == null) {
            throw new IOException("parent of config file \"" + configFileRef
                    + "\" is not represented as an object of class "
                    + ReferencedFile.class.getSimpleName());
        }
        File configFile = configFileRef.getFile();
        NodeSettingsRO settings;
        if (!configFile.isFile() || !configFile.canRead()) {
            String error = "Unable to load \"" + node.getName() + "\": "
                    + "Can't read config file \"" + configFile + "\"";
            loadResult.addError(error);
            settings = new NodeSettings("empty");
            setNeedsResetAfterLoad(); // also implies dirty
        } else {
            settings =
                NodeSettings.loadFromXML(new FileInputStream(configFile));
        }

        m_modelSettings = settings;

        try {
            m_hasContent = loadHasContent(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load hasContent flag: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
            setNeedsResetAfterLoad(); // also implies dirty
        }

        try {
            m_isInactive = loadIsInactive(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load isInactive flag: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
            setNeedsResetAfterLoad(); // also implies dirty
        }

        try {
            m_warningMessage = loadWarningMessage(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load (old) warning message: "
                + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
            setDirtyAfterLoad();
        }

        try {
            m_isExecuted = loadIsExecuted(settings);
            if (m_isExecuted && OLD_AUTOEXECUTABLE_NODEFACTORIES.contains(
                    node.getFactory().getClass().getSimpleName())) {
                getLogger().debug("Setting executed flag of node \""
                        + node.getFactory().getClass().getSimpleName()
                        + "\" to false due to version bump (loaded as true)");
                m_isExecuted = false;
                setNeedsResetAfterLoad();
            }
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load execution flag: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
            setNeedsResetAfterLoad();
        }

        try {
            m_isConfigured = loadIsConfigured(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load configuration flag: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
            setNeedsResetAfterLoad();
        }

        // load internals
        if (m_hasContent) {
            try {
                m_nodeInternDirectory =
                    loadNodeInternDirectory(settings, m_nodeDirectory);
            } catch (InvalidSettingsException ise) {
                String e = "Unable to load internals directory";
                loadResult.addError(e);
                getLogger().warn(e, ise);
                setDirtyAfterLoad();
            }
        }
        settingsExec.setProgress(1.0);
        exec.setMessage("ports");
        try {
            loadPorts(node, loadExec, settings, loadTblRep, tblRep);
        } catch (Exception e) {
            if (!(e instanceof InvalidSettingsException)
                    && !(e instanceof IOException)) {
                getLogger().error("Unexpected \"" + e.getClass().getSimpleName()
                        + "\" encountered");
            }
            String err = "Unable to load port content for node \""
                + node.getName() + "\": " + e.getMessage();
            loadResult.addError(err, true);
            if (mustWarnOnDataLoadError()) {
                getLogger().warn(err, e);
            } else {
                getLogger().debug(err);
            }
            setNeedsResetAfterLoad();
        }
        loadExec.setProgress(1.0);
        try {
            loadInternalHeldTables(
                    node, loadIntTblsExec, settings, loadTblRep, tblRep);
        } catch (Exception e) {
            if (!(e instanceof InvalidSettingsException)
                    && !(e instanceof IOException)) {
                getLogger().error("Unexpected \"" + e.getClass().getSimpleName()
                        + "\" encountered");
            }
            String err = "Unable to load internally held tables for node \""
                + node.getName() + "\": " + e.getMessage();
            loadResult.addError(err, true);
            if (mustWarnOnDataLoadError()) {
                getLogger().warn(err, e);
            } else {
                getLogger().debug(err);
            }
            setNeedsResetAfterLoad();
        }
        loadIntTblsExec.setProgress(1.0);
        exec.setMessage("creating instance");

        exec.setMessage("Loading settings into node instance");
        node.load(this, createExec, loadResult);
        String status;
        switch (loadResult.getType()) {
        case Ok:
            status = " without errors";
            break;
        case DataLoadError:
            status = " with data errors";
            break;
        case Error:
            status = " with errors";
            break;
        case Warning:
            status = " with warnings";
            break;
        default:
            status = " with " + loadResult.getType();
        }
        String message = "Loaded node " + node + status;
        exec.setProgress(1.0, message);
    }

    /** {@inheritDoc} */
    @Override
    public LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy() {
        // we explicitly return null here as the node decides on how
        // to behave (in workflows 1.x.x it's not known what is the correct
        // state of the node at this point)
        return null;
    }

    public ReferencedFile getNodeDirectory() {
        return m_nodeDirectory;
    }

    /** {@inheritDoc} */
    @Override
    public ReferencedFile getNodeInternDirectory() {
        return m_nodeInternDirectory;
    }

    /** {@inheritDoc} */
    @Override
    public NodeSettingsRO getSettings() {
        return m_modelSettings;
    }

    /** {@inheritDoc} */
    @Override
    public String getWarningMessage() {
        return m_warningMessage;
    }

    /** {@inheritDoc} */
    @Override
    public PortObject getPortObject(final int outportIndex) {
        if (outportIndex == 0) {
            if (m_isInactive) {
                return InactiveBranchPortObject.INSTANCE;
            } else {
                return FlowVariablePortObject.INSTANCE;
            }
        }
        return m_portObjects[outportIndex];
    }

    /**
     * @param idx The outport index.
     * @param portObject the portObjects to set
     */
    public void setPortObject(final int idx, final PortObject portObject) {
        checkPortIndexOnSet(idx);
        m_portObjects[idx] = portObject;
    }

    private void checkPortIndexOnSet(final int index) {
        if (index == 0) {
            throw new IllegalStateException("Must not set content of port 0;"
                    + "it's the framework port");
        }
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec getPortObjectSpec(final int outportIndex) {
        if (outportIndex == 0) {
            if (m_isInactive) {
                return InactiveBranchPortObjectSpec.INSTANCE;
            } else {
                return FlowVariablePortObjectSpec.INSTANCE;
            }
        }
        return m_portObjectSpecs[outportIndex];
    }

    /**
     * @param idx The outport index.
     * @param portObjectSpec the portObjects to set
     */
    public void setPortObjectSpec(
            final int idx, final PortObjectSpec portObjectSpec) {
        checkPortIndexOnSet(idx);
        m_portObjectSpecs[idx] = portObjectSpec;
    }

    /** {@inheritDoc} */
    @Override
    public String getPortObjectSummary(final int outportIndex) {
        if (outportIndex == 0) {
            return FlowVariablePortObject.INSTANCE.getSummary();
        }
        return m_portObjectSummaries[outportIndex];
    }

    /**
     * @param idx port for which to set summary
     * @param portObjectSummary the portObjectSummary to set
     */
    public void setPortObjectSummary(final int idx,
            final String portObjectSummary) {
        checkPortIndexOnSet(idx);
        m_portObjectSummaries[idx] = portObjectSummary;
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalHeldTables() {
        return m_internalHeldTables;
    }

    /**
     * @param internalHeldTables the internalHeldTables to set
     */
    public void setInternalHeldTables(
            final BufferedDataTable[] internalHeldTables) {
        m_internalHeldTables = internalHeldTables;
    }

}
