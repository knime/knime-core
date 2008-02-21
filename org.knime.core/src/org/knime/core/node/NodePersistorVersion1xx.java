/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.Node.MemoryPolicy;
import org.knime.core.node.Node.SettingsLoaderAndWriter;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

public class NodePersistorVersion1xx implements NodePersistor {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodePersistorVersion1xx.class);

    private boolean m_isExecuted;
    
    private boolean m_hasContent;

    private boolean m_isConfigured;

    private NodeMessage m_nodeMessage;

    private File m_nodeDirectory;

    private File m_nodeInternDirectory;

    private NodeSettingsRO m_modelSettings;

    private PortObject[] m_portObjects;

    private PortObjectSpec[] m_portObjectSpecs;

    private MemoryPolicy m_memoryPolicy;
    
    private boolean m_needsResetAfterLoad;
    
    private final GenericNodeFactory<GenericNodeModel> m_class;
    
    static String createDataFileDirName(final int index) {
        return DATA_FILE_PREFIX + index;
    }

    static String createModelFileName(final int index) {
        return MODEL_FILE_PREFIX + index + ".pmml.gz";
    }

    /** Constructor being used when this class is used for loading a workflow.
     * It's then save to call the {@link #load(ExecutionMonitor, int, HashMap)}
     * method. 
     * @param cl The factory class for the node. The factory being used to
     * instantiate a node is retrieved using the 
     */
    public NodePersistorVersion1xx(
            final GenericNodeFactory<GenericNodeModel> cl) {
        m_class = cl;
    }
    
    /** Constructor that should be used when node is saved. It's not been used
     * for loading, i.e. all getXXX() methods will return invalid values.
     */
    public NodePersistorVersion1xx() {
        this(null);
    }
    
    protected boolean loadIsExecuted(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getBoolean(CFG_ISEXECUTED);
    }
    
    protected boolean loadHasContent(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return settings.getBoolean(CFG_ISEXECUTED);
    }
    

    protected boolean loadIsConfigured(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getBoolean(CFG_ISCONFIGURED);
    }

    protected NodeMessage loadNodeMessage(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return null;
    }
    
    static File getNodeInternDirectory(final File nodeDir) {
        return new File(nodeDir, INTERN_FILE_DIR);
    }

    protected File loadNodeInternDirectory(final NodeSettingsRO settings, 
            final File nodeDir) throws InvalidSettingsException {
        return getNodeInternDirectory(nodeDir);
    }
    
    protected GenericNodeFactory<GenericNodeModel> loadNodeFactoryClass(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return m_class;
    }
    
    protected void loadPorts(final Node node,
            final ExecutionMonitor execMon, final NodeSettingsRO settings,
            final int loadID, final HashMap<Integer, ContainerTable> tblRep)
            throws IOException, InvalidSettingsException,
            CanceledExecutionException {
        for (int i = 0; i < node.getNrOutPorts(); i++) {
            ExecutionMonitor execPort = execMon
                    .createSubProgress(1.0 / node.getNrOutPorts());
            PortType type = node.getOutPort(i).getPortType();
            boolean isDataPort = BufferedDataTable.class.isAssignableFrom(type
                    .getPortObjectClass());
            if (m_isConfigured) {
                PortObjectSpec spec = 
                    loadPortObjectSpec(node, settings, loadID, i);
                setPortObjectSpec(i, spec);
            }
            if (m_isExecuted) {
                PortObject object;
                if (isDataPort) {
                    object = loadBufferedDataTable(
                            node, settings, execPort, loadID, i, tblRep);
                } else {
                    object = loadModelContent(node, settings, execMon, i);
                }
                setPortObject(i, object);
            }
        }
    }
    
    private BufferedDataTable loadBufferedDataTable(final Node node,
            final NodeSettingsRO settings, final ExecutionMonitor execMon,
            final int loadID, final int index,
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
            // dataConfigFile = data.xml in node dir
            File dataConfigFile = new File(m_nodeDirectory, dataConfigFileName);
            NodeSettingsRO dataSettings = NodeSettings
                    .loadFromXML(new BufferedInputStream(new FileInputStream(
                            dataConfigFile)));
            String dataPath = dataSettings.getString(CFG_DATA_FILE_DIR);
            // dataDir = /data
            File dataDir = new File(m_nodeDirectory, dataPath);
            // note: we do not check for existence here - in some cases
            // this directory may not exist (when exported and empty
            // directories are pruned)
            NodeSettingsRO portSettings = dataSettings
                    .getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            // dir = /data/data_i
            File dir = new File(dataDir, dataName);
            BufferedDataTable t = BufferedDataTable.loadFromFile(
                    dir, portSettings, execSubData, loadID,
                    // we didn't have blobs in 1.1.x
                    new HashMap<Integer, ContainerTable>());
            t.setOwnerRecursively(node);
            return t;
        } else {
            NodeSettingsRO dataSettings = settings
                    .getNodeSettings(CFG_DATA_FILE);
            String dataDirStr = dataSettings.getString(CFG_DATA_FILE_DIR);
            File dataDir = new File(m_nodeDirectory, dataDirStr);
            NodeSettingsRO portSettings = dataSettings
                    .getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            File dir = new File(dataDir, dataName);
            if (!(dir.isDirectory() && dir.canRead())) {
                throw new IOException("Can not read directory "
                        + dir.getAbsolutePath());
            }
            BufferedDataTable t = BufferedDataTable.loadFromFile(dir,
            /* ignored in 1.2.0+ */
            null, execMon, loadID, tblRep);
            t.setOwnerRecursively(node);
            return t;
        }
    }

    private ModelContentRO loadModelContent(final Node node,
            final NodeSettingsRO settings, final ExecutionMonitor execMon,
            final int index) throws InvalidSettingsException, IOException,
            CanceledExecutionException {
        // load models
        int modelIndex = index - countDataOutPorts(node);
        NodeSettingsRO model = settings.getNodeSettings(CFG_MODEL_FILES);
        String modelName = model.getString(CFG_OUTPUT_PREFIX + modelIndex);
        File targetFile = new File(m_nodeDirectory, modelName);
    
        // in an earlier version the model settings were written
        // directly (without zipping); now the settings are
        // zipped (see save()); to be backward compatible
        // both ways are tried
        InputStream in = null;
        try {
            in = new GZIPInputStream(new BufferedInputStream(
                    new FileInputStream(targetFile)));
        } catch (IOException ioe) {
            // if a gz input stream could not be created
            // we use read directly from the file via the
            // previously created buffered input stream
            in = new BufferedInputStream(new FileInputStream(targetFile));
        }
        ModelContentRO pred = ModelContent.loadFromXML(in);
        return pred;
    }

    private PortObjectSpec loadPortObjectSpec(final Node node,
            final NodeSettingsRO settings, final int loadID, final int index)
            throws InvalidSettingsException, CanceledExecutionException,
            IOException {
        PortType type = node.getOutPort(index).getPortType();
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
            File targetFile = new File(m_nodeDirectory, specName);
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
            File dataDir = new File(m_nodeDirectory, dataDirStr);
            NodeSettingsRO portSettings = dataSettings
                    .getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            File dir = new File(dataDir, dataName);
            if (!(dir.isDirectory() && dir.canRead())) {
                throw new IOException("Can not read directory "
                        + dir.getAbsolutePath());
            }
            DataTableSpec outSpec = null;
            if (portSettings.getBoolean(CFG_HAS_SPEC_FILE, true)) {
                outSpec = BufferedDataTable.loadSpec(dir);
                if (portSettings.containsKey(CFG_HAS_SPEC_FILE)
                        && outSpec == null) {
                    throw new IOException("No spec file available for"
                            + " outport " + index + ".");
                }
            }
            return outSpec;
        }
    }

    /** Is configured according to the settings object. 
     * @return If node is saved in configured state. */
    public boolean isConfigured() {
        return m_isConfigured;
    }
    
    /** Is executed according to the settings object. 
     * @return If node is saved in executed state. */
    public boolean isExecuted() {
        return m_isExecuted;
    }
    
    /** {@inheritDoc} */
    public boolean hasContent() {
        return m_hasContent;
    }
    
    /** {@inheritDoc} */
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }
    
    /** Indicate an error and that this node should better be reset after load.
     */
    protected void setNeedsResetAfterLoad() {
        m_needsResetAfterLoad = true;
    }
    
    public LoadResult load(Node node,
            final File configFile, ExecutionMonitor execMon, int loadID,
            HashMap<Integer, ContainerTable> tblRep) 
            throws InvalidSettingsException, IOException, CanceledExecutionException {
        LoadResult result = new LoadResult();
        m_portObjects = new PortObject[node.getNrOutPorts()];
        m_portObjectSpecs = new PortObjectSpec[node.getNrOutPorts()];
        m_nodeDirectory = configFile.getParentFile();
        NodeSettingsRO settings;
        if (!configFile.isFile() || !configFile.canRead()) {
            String error = "Unable to load \"" + node.getName() + "\": "
                    + "Can't read config file \"" + configFile + "\"";
            result.addError(error);
            settings = new NodeSettings("empty");
            setNeedsResetAfterLoad();
        } else {
            settings = 
                NodeSettings.loadFromXML(new FileInputStream(configFile));
        }
    
        try {
            m_nodeMessage = loadNodeMessage(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load node message: " + ise.getMessage();
            result.addError(e);
            LOGGER.warn(e, ise);
        }
    
        try {
            SettingsLoaderAndWriter nodeSettings = 
                SettingsLoaderAndWriter.load(settings);
            m_memoryPolicy = nodeSettings.getMemoryPolicy();
            m_modelSettings = nodeSettings.getModelSettings();
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load node settings: " + ise.getMessage();
            result.addError(e);
            LOGGER.warn(e, ise);
        }
    
        try {
            m_hasContent = loadHasContent(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load hasContent flag: " + ise.getMessage();
            result.addError(e);
            LOGGER.warn(e, ise);
            setNeedsResetAfterLoad();
        }
        
        try {
            m_isExecuted = loadIsExecuted(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load execution flag: " + ise.getMessage();
            result.addError(e);
            LOGGER.warn(e, ise);
            setNeedsResetAfterLoad();
        }
    
        try {
            m_isConfigured = loadIsConfigured(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load configuration flag: " + ise.getMessage();
            result.addError(e);
            LOGGER.warn(e, ise);
            setNeedsResetAfterLoad();
        }
    
        // load internals
        if (m_isExecuted) {
            try {
                m_nodeInternDirectory = 
                    loadNodeInternDirectory(settings, m_nodeDirectory);
            } catch (InvalidSettingsException ise) {
                String e = "Unable to load internals directory";
                result.addError(e);
                LOGGER.warn(e, ise);
            }
        }
        try {
            loadPorts(node, execMon, settings, loadID, tblRep);
        } catch (Exception e) {
            if (!(e instanceof InvalidSettingsException)
                    && !(e instanceof IOException)) {
                LOGGER.error("Unexpected \"" + e.getClass().getSimpleName() 
                        + "\" encountered");
            }
            String err = "Unable to load content for node \"" + node.getName()
                + "\": " + e.getMessage();
            result.addError(err);
            LOGGER.warn(err, e);
            setNeedsResetAfterLoad();
        } finally {
            execMon.setProgress(1.0);
        }
        if (result.hasErrors()) {
            m_nodeMessage = new NodeMessage(Type.ERROR, result.getErrors());
        }

        execMon.setMessage("Loading settings into node instance");
        node.load(this, execMon.createSilentSubProgress(0.0));
        return result;
    }

    /** {@inheritDoc} */
    public MemoryPolicy getMemoryPolicy() {
        return m_memoryPolicy;
    }

    /** {@inheritDoc} */
    public File getNodeDirectory() {
        return m_nodeDirectory;
    }

    /** {@inheritDoc} */
    public File getNodeInternDirectory() {
        return m_nodeInternDirectory;
    }

    /** {@inheritDoc} */
    public NodeSettingsRO getNodeModelSettings() {
        return m_modelSettings;
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
    public NodeMessage getNodeMessage() {
        return m_nodeMessage;
    }
    
    private static int countDataOutPorts(final Node node) {
        int dataPortsCount = 0;
        for (int i = 0; i < node.getNrOutPorts(); i++) {
            PortType type = node.getOutPort(i).getPortType();
            if (BufferedDataTable.class.isAssignableFrom(
                    type.getPortObjectClass())) {
                dataPortsCount += 1;
            }
        }
        return dataPortsCount;
    }
    
}
