/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion1xx;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

public class NodePersistorVersion1xx implements NodePersistor {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private SingleNodeContainerPersistorVersion1xx 
        m_sncPersistor;
    
    private boolean m_isExecuted;
    
    private boolean m_hasContent;

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

    /** Constructor that should be used when node is saved. It's not been used
     * for loading, i.e. all getXXX() methods will return invalid values.
     */
    public NodePersistorVersion1xx(
            final SingleNodeContainerPersistorVersion1xx sncPersistor) {
        m_sncPersistor = sncPersistor;
    }
    
    protected NodeLogger getLogger() {
        return m_logger;
    }
    
    protected boolean loadIsExecuted(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getBoolean(CFG_ISEXECUTED);
    }
    
    protected boolean loadHasContent(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return settings.getBoolean(CFG_ISEXECUTED);
    }
    
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

    protected ReferencedFile loadNodeInternDirectory(final NodeSettingsRO settings, 
            final ReferencedFile nodeDir) throws InvalidSettingsException {
        return getNodeInternDirectory(nodeDir);
    }
    
    protected void loadPorts(final Node node,
            final ExecutionMonitor execMon, final NodeSettingsRO settings,
            final Map<Integer, BufferedDataTable> loadTblRep, final HashMap<Integer, ContainerTable> tblRep)
            throws IOException, InvalidSettingsException,
            CanceledExecutionException {
        for (int i = 0; i < node.getNrOutPorts(); i++) {
            ExecutionMonitor execPort = execMon
                    .createSubProgress(1.0 / node.getNrOutPorts());
            execMon.setMessage("Port " + i);
            PortType type = node.getOutputType(i);
            boolean isDataPort = BufferedDataTable.class.isAssignableFrom(type
                    .getPortObjectClass());
            if (m_isConfigured) {
                PortObjectSpec spec = 
                    loadPortObjectSpec(node, settings, i);
                setPortObjectSpec(i, spec);
            }
            if (m_isExecuted) {
                PortObject object;
                if (isDataPort) {
                    object = loadBufferedDataTable(
                            node, settings, execPort, loadTblRep, i, tblRep);
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
    
    protected void loadInternalHeldTables(final Node node, 
            final ExecutionMonitor execMon, 
            final NodeSettingsRO settings, 
            final Map<Integer, BufferedDataTable> loadTblRep, 
            final HashMap<Integer, ContainerTable> tblRep)
    throws IOException, InvalidSettingsException, CanceledExecutionException {
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
        PortType type = node.getOutputType(index);
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
    
    /** {@inheritDoc} */
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
    
    /** {@inheritDoc} */
    @Override
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
    public ReferencedFile getNodeInternDirectory() {
        return m_nodeInternDirectory;
    }

    /** {@inheritDoc} */
    public NodeSettingsRO getSettings() {
        return m_modelSettings;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getWarningMessage() {
        return m_warningMessage;
    }

    /** {@inheritDoc} */
    public PortObject getPortObject(final int outportIndex) {
        return m_portObjects[outportIndex];
    }
    
    /**
     * @param idx The outport index.
     * @param portObject the portObjects to set
     */
    public void setPortObject(final int idx, final PortObject portObject) {
        m_portObjects[idx] = portObject;
    }
    
    /** {@inheritDoc} */
    public PortObjectSpec getPortObjectSpec(final int outportIndex) {
        return m_portObjectSpecs[outportIndex];
    }

    /**
     * @param idx The outport index.
     * @param portObjectSpec the portObjects to set
     */
    public void setPortObjectSpec(
            final int idx, final PortObjectSpec portObjectSpec) {
        m_portObjectSpecs[idx] = portObjectSpec;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getPortObjectSummary(final int outportIndex) {
        return m_portObjectSummaries[outportIndex];
    }
    
    /**
     * @param idx port for which to set summary
     * @param portObjectSummary the portObjectSummary to set
     */
    public void setPortObjectSummary(final int idx, 
            final String portObjectSummary) {
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
