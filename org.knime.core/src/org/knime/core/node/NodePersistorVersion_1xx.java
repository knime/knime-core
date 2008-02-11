/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.Node.MemoryPolicy;
import org.knime.core.node.Node.SettingsLoaderAndWriter;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.util.FileUtil;

public class NodePersistorVersion_1xx implements NodePersistor {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodePersistorVersion_1xx.class);

    private boolean m_isExecuted;

    private boolean m_isConfigured;

    private NodeMessage m_nodeMessage;

    private File m_nodeDirectory;

    private File m_nodeInternDirectory;

    private NodeSettingsRO m_modelSettings;

    private PortObject[] m_portObjects;

    private PortObjectSpec[] m_portObjectSpecs;

    private MemoryPolicy m_memoryPolicy;
    
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
    public NodePersistorVersion_1xx(
            final GenericNodeFactory<GenericNodeModel> cl) {
        m_class = cl;
    }
    
    /** Constructor that should be used when node is saved. It's not been used
     * for loading, i.e. all getXXX() methods will return invalid values.
     */
    public NodePersistorVersion_1xx() {
        this(null);
    }
    
    protected PortObject loadPortObject(final Node node,
            final NodeSettingsRO settings, final ExecutionMonitor execMon,
            final int loadID, final int index,
            final HashMap<Integer, ContainerTable> tblRep)
            throws InvalidSettingsException, CanceledExecutionException,
            IOException {
        PortType type = node.getOutPort(index).getPortType();
        boolean isDataPort = BufferedDataTable.class.isAssignableFrom(type
                .getPortObjectClass());
        if (isDataPort) {
            return loadBufferedDataTable(node, settings, execMon, loadID,
                    index, tblRep);
        } else {
            return loadModelContent(node, settings, execMon, index);
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
            BufferedDataTable t = BufferedDataTable.loadFromFile(dir, portSettings,
                    execSubData, loadID,
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

    protected PortObjectSpec loadPortObjectSpec(final Node node,
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

    protected boolean loadIsExecuted(final NodeSettingsRO settings)
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
            final File nodeDir)
            throws InvalidSettingsException {
        return getNodeInternDirectory(nodeDir);
    }
    
    protected GenericNodeFactory<GenericNodeModel> loadNodeFactoryClass(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return m_class;
    }
    
    protected void loadOverride(final Node node, NodeSettingsRO settings)
            throws InvalidSettingsException {

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
    
    public Node load(final GenericNodeFactory<GenericNodeModel> factory, 
            final File configFile, ExecutionMonitor execMon, int loadID,
            HashMap<Integer, ContainerTable> tblRep) throws IOException, 
            InvalidSettingsException, CanceledExecutionException {
        NodeSettingsRO settings = NodeSettings.loadFromXML(new FileInputStream(
                configFile));
        Node node = new Node(factory);
        m_portObjects = new PortObject[node.getNrOutPorts()];
        m_portObjectSpecs = new PortObjectSpec[node.getNrOutPorts()];
        if (node == null || !configFile.isFile() || !configFile.canRead()) {
            m_isExecuted = false;
            m_isConfigured = false;
            String errorMessage = "Unable to load \"" + node.getName() + "\": "
            + SETTINGS_FILE_NAME + " can't be read: " + configFile;
            m_nodeMessage = new NodeMessage(Type.ERROR, errorMessage);
            throw new IOException(errorMessage);
        }
        m_nodeDirectory = configFile.getParentFile();
        try {
            loadOverride(node, settings);
        } catch (InvalidSettingsException ise) {
            String e = "Failed to call loadOverride: " + ise.getMessage();
            LOGGER.warn(e);
            m_nodeMessage = new NodeMessage(Type.ERROR, e);
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        } catch (Error e) {
            throw new InvalidSettingsException(e);
        }
    
        try {
            if (m_nodeMessage != null) {
                // load only if above calls went fine
                m_nodeMessage = loadNodeMessage(settings);
            }
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load node message: " + ise.getMessage();
            LOGGER.warn(e);
            m_nodeMessage = new NodeMessage(Type.ERROR, e);
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        } catch (Error e) {
            throw new InvalidSettingsException(e);
        }
    
        try {
            SettingsLoaderAndWriter nodeSettings = 
                SettingsLoaderAndWriter.load(settings);
            m_memoryPolicy = nodeSettings.getMemoryPolicy();
            m_modelSettings = nodeSettings.getModelSettings();
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load node settings: " + ise.getMessage();
            LOGGER.warn(e);
            m_nodeMessage = new NodeMessage(Type.ERROR, e);
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        } catch (Error e) {
            throw new InvalidSettingsException(e);
        }
    
        try {
            m_isExecuted = loadIsExecuted(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load execution flag: " + ise.getMessage();
            LOGGER.warn(e);
            m_nodeMessage = new NodeMessage(Type.ERROR, e);
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        } catch (Error e) {
            throw new InvalidSettingsException(e);
        }
    
        try {
            m_isConfigured = loadIsConfigured(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load configuration flag: " + ise.getMessage();
            LOGGER.warn(e);
            m_nodeMessage = new NodeMessage(Type.ERROR, e);
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        } catch (Error e) {
            throw new InvalidSettingsException(e);
        }
    
        // load internals
        if (m_isExecuted) {
            m_nodeInternDirectory = 
                loadNodeInternDirectory(settings, m_nodeDirectory);
        }
    
        for (int i = 0; i < node.getNrOutPorts(); i++) {
            ExecutionMonitor execPort = execMon
                    .createSubProgress(1.0 / node.getNrOutPorts());
            try {
                if (m_isConfigured) {
                    m_portObjectSpecs[i] = loadPortObjectSpec(
                            node, settings, loadID, i);
                }
                if (m_isExecuted) {
                    m_portObjects[i]= loadPortObject(node,
                            settings, execPort, loadID, i, tblRep);
                }
            } catch (InvalidSettingsException ise) {
                String e = "Unable to load content for output port " + i + ": "
                        + ise.getMessage();
                LOGGER.warn(e);
                m_nodeMessage = new NodeMessage(Type.ERROR, e);
            } catch (IOException ioe) {
                String e = "Unable to load content for output port " + i + ": "
                        + ioe.getMessage();
                LOGGER.warn(e);
                m_nodeMessage = new NodeMessage(Type.ERROR, e);
            } finally {
                execPort.setProgress(1.0);
            }
        }
        execMon.setMessage("Loading settings into node instance");
        node.load(this, execMon.createSilentSubProgress(0.0));
        return node;
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

    /** {@inheritDoc} */
    public PortObjectSpec getPortObjectSpec(final int outportIndex) {
        return m_portObjectSpecs[outportIndex];
    }

    /** {@inheritDoc} */
    public NodeMessage getNodeMessage() {
        return m_nodeMessage;
    }
    
    /**
     * Saves the node, node settings, and all internal structures, spec, data,
     * and models, to the given node directory (located at the node file).
     * 
     * @param nodeFile To write node settings to.
     * @param execMon Used to report progress during saving.
     * @throws IOException If the node file can't be found or read.
     * @throws CanceledExecutionException If the saving has been canceled.
     */
    public void save(final Node node, final File nodeFile, 
            final ExecutionMonitor execMon, final boolean isSaveData) 
        throws IOException, CanceledExecutionException {
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        final File nodeDir = nodeFile.getParentFile();
        saveOverride(node, settings);
        saveCustomName(node, settings);
        saveIsExecuted(node, settings, isSaveData);
        saveIsConfigured(node, settings);
        node.saveSettingsTo(settings);
        saveNodeMessage(node, settings);
        saveNodeInternDirectory(node, nodeDir, settings, execMon);
        savePortObjectsAndSpecs(node, nodeDir, settings, execMon, isSaveData);
        settings.saveToXML(new BufferedOutputStream(
                new FileOutputStream(nodeFile)));
    }

    protected void savePortObjectsAndSpecs(final Node node, final File nodeDir,
            final NodeSettingsWO settings, ExecutionMonitor exec, 
            final boolean saveData) 
        throws IOException, CanceledExecutionException {
        // delete previous directory no matter what.
        deleteDataDirectory(nodeDir);
        if (node.getNrOutPorts() == 0 
                || node.isAutoExecutable() || !node.isExecuted()) {
            return;
        }
        final int portCount = node.getNrOutPorts();
        int dataOutportCount = 0;
        int modelOutportCount = 0;
        for (int i = 0; i < portCount; i++) {
            PortType type = node.getOutPort(i).getPortType();
            if (BufferedDataTable.class.isAssignableFrom(
                    type.getPortObjectClass())) {
                dataOutportCount += 1;
            } else {
                modelOutportCount += 1;
            }
        }
        if (dataOutportCount > 0) {
            ExecutionMonitor subProgress = exec.createSubProgress(
                    dataOutportCount / (double)portCount);
            subProgress.setMessage("Saving output data");
            saveDataPorts(node, nodeDir, settings, subProgress, saveData);
            subProgress.setProgress(1.0);
        }
        if (modelOutportCount > 0) {
            ExecutionMonitor subProgress = exec.createSubProgress(
                    modelOutportCount / (double)portCount);
            subProgress.setMessage("Saving output models");
            saveModelPorts(node, nodeDir, settings, exec, saveData);
            subProgress.setProgress(1.0);
        }
    }
    
    protected void saveDataPorts(final Node node, final File nodeDir, 
            final NodeSettingsWO nodeSettings, ExecutionMonitor exec, 
            final boolean isSaveData) 
        throws IOException, CanceledExecutionException {
        // dataSettings = subtree in settings.xml
        NodeSettingsWO dataSettings =
            nodeSettings.addNodeSettings(CFG_DATA_FILE);
        dataSettings.addString(CFG_DATA_FILE_DIR, DATA_FILE_DIR);
        File dataDir = new File(nodeDir, DATA_FILE_DIR);
        dataDir.mkdir();
        if (!dataDir.isDirectory() || !dataDir.canWrite()) {
            throw new IOException("Can not write directory "
                    + dataDir.getAbsolutePath());
        }
        final int portCount = node.getNrOutPorts();
        for (int i = 0; i < portCount; i++) {
            PortType type = node.getOutPort(i).getPortType();
            if (BufferedDataTable.class.isAssignableFrom(
                    type.getPortObjectClass())) {
                exec.setMessage("Saving data output " + i);
                saveBufferedDataTable(node, dataDir, dataSettings, 
                        exec.createSubProgress(0.0), i, isSaveData);
            }
        }
        exec.setProgress(1.0);
    }
    
    protected void deleteDataDirectory(final File nodeDir) {
        // delete previous directory no matter what.
        File dataDir = new File(nodeDir, DATA_FILE_DIR);
        if (dataDir.exists()) {
            FileUtil.deleteRecursively(dataDir);
        }
    }
    
    protected void saveModelPorts(final Node node, final File nodeDir, 
            final NodeSettingsWO nodeSettings, ExecutionMonitor exec, 
            final boolean isSaveData) 
        throws IOException, CanceledExecutionException {
        if (!node.isExecuted() || !isSaveData) {
            return;
        }
        NodeSettingsWO models = nodeSettings.addNodeSettings(CFG_MODEL_FILES);
        final int portCount = node.getNrOutPorts();
        for (int i = 0; i < portCount; i++) {
            PortType type = node.getOutPort(i).getPortType();
            if (!BufferedDataTable.class.isAssignableFrom(
                    type.getPortObjectClass())) {
                exec.setMessage("Saving model output " + i);
                saveModelContent(node, nodeDir, models, 
                        exec.createSubProgress(0.0), i);
            }
        }
        exec.setProgress(1.0);
    }
    
    protected void saveBufferedDataTable(final Node node, final File dataDir,
            final NodeSettingsWO dataSettings, final ExecutionMonitor exec,
            final int pIndex, final boolean isSaveData) 
        throws IOException, CanceledExecutionException {
        NodeSettingsWO portSettings =
                dataSettings.addNodeSettings(CFG_OUTPUT_PREFIX + pIndex);
        String dataName = createDataFileDirName(pIndex);
        File dir = new File(dataDir, dataName);
        dir.mkdir();
        if (!(dir.isDirectory() && dir.canWrite())) {
            throw new IOException("Can not write directory "
                    + dir.getAbsolutePath());
        }
        portSettings.addString(CFG_DATA_FILE_DIR, dataName);
        if (node.isExecuted() && isSaveData) {
            BufferedDataTable table = 
                (BufferedDataTable)node.getOutPort(pIndex).getPortObject();
            table.save(dir, exec);
        } else {
            DataTableSpec spec = 
                (DataTableSpec)node.getOutPort(pIndex).getPortObjectSpec();
            if (spec == null) {
                portSettings.addBoolean(CFG_HAS_SPEC_FILE, false);
            } else {
                portSettings.addBoolean(CFG_HAS_SPEC_FILE, true);
                BufferedDataTable.saveSpec(spec, dir);
            }
        }
        exec.setProgress(1.0);
    }
    
    protected void saveModelContent(final Node node, final File m_nodeDir,
            final NodeSettingsWO settings, final ExecutionMonitor exec, 
            final int i) throws IOException, CanceledExecutionException {
        int modelIndex = i - countDataOutPorts(node);
        ModelContentRO pred = 
            (ModelContentRO)node.getOutPort(i).getPortObject();
        if (pred != null) {
            String modelFileName = createModelFileName(modelIndex);
            settings.addString(CFG_OUTPUT_PREFIX + modelIndex, modelFileName);
            File targetFile = new File(m_nodeDir, modelFileName);
            // reset from previous saving
            targetFile.delete();
            BufferedOutputStream out = new BufferedOutputStream(
                    new GZIPOutputStream(new FileOutputStream(targetFile)));
            pred.saveToXML(out);
        }
        exec.setProgress(1.0);
    }
    
    protected void saveIsExecuted(final Node node, 
            final NodeSettingsWO settings, boolean isSaveData) {
        settings.addBoolean(CFG_ISEXECUTED, node.isExecuted() && isSaveData);
    }
    
    protected void saveIsConfigured(
            final Node node, final NodeSettingsWO settings) {
        // write configured flag
        settings.addBoolean(CFG_ISCONFIGURED, node.isConfigured());
    }
    protected void saveNodeMessage(
            final Node node, final NodeSettingsWO settings) {
    }
    
    protected void saveNodeInternDirectory(final Node node, final File nodeDir,
            final NodeSettingsWO settings, final ExecutionMonitor exec) 
        throws CanceledExecutionException {
        if (!node.isAutoExecutable() && node.isExecuted()) {
            File nodeInternDir = getNodeInternDirectory(nodeDir);
            node.saveInternals(nodeInternDir, exec);
        }
    }
    
    protected void saveCustomName(
            final Node node, final NodeSettingsWO settings) {
        // write node name
        settings.addString(CFG_NAME, node.getName());
    }
    protected void saveOverride(
            final Node node, final NodeSettingsWO settings) {
        
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
