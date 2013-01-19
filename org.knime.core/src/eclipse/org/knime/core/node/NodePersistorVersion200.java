/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 13, 2008 (wiswedel): created
 */
package org.knime.core.node;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.EmptyFileStoreHandler;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.WorkflowFileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.WriteFileStoreHandler;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpec.PortObjectSpecSerializer;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion200;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;
import org.knime.core.util.FileUtil;

/**
 *
 * @author wiswedel, University of Konstanz
 * @noinstantiate
 * @noextend
 */
public class NodePersistorVersion200 extends NodePersistorVersion1xx {

    /** @noreference Not public API. */
    public static final String FILESTORE_FOLDER_PREFIX = "filestore";

    /** Prefix of associated port folders.
     * (Also used in export wizard, public declaration here.) */
    public static final String PORT_FOLDER_PREFIX = "port_";

    /** Prefix of associated port folders.
     * (Also used in export wizard, public declaration here.) */
    public static final String INTERNAL_TABLE_FOLDER_PREFIX = "internalTables";

    /** Invokes super constructor.
     * @param sncPersistor Forwarded.
     * @param loadVersion Version, must not be null.
     * @param configFileRef node.xml */
    public NodePersistorVersion200(
            final SingleNodeContainerPersistorVersion200 sncPersistor,
            final LoadVersion loadVersion, final ReferencedFile configFileRef) {
        super(sncPersistor, loadVersion, configFileRef);
        if (loadVersion == null) {
            throw new NullPointerException("Version arg must not be null.");
        }
    }

    /* Contains all fully qualified path names of previously existing
     *  PMMLPortObjects that have been removed in version v2.4. This is
     *  necessary for being able to replace them with the general PMMLPortObject
     *  on loading. */
    private static final Set<String> PMML_PORTOBJECT_CLASSES;
    static {
        PMML_PORTOBJECT_CLASSES = new HashSet<String>();
        PMML_PORTOBJECT_CLASSES.add(
            "org.knime.base.node.mine.cluster.PMMLClusterPortObject");
        PMML_PORTOBJECT_CLASSES.add("org.knime.base.node.mine.decisiontree2"
                + ".PMMLDecisionTreePortObject");
        PMML_PORTOBJECT_CLASSES.add(
            "org.knime.base.node.mine.neural.mlp.PMMLNeuralNetworkPortObject");
        PMML_PORTOBJECT_CLASSES.add(
            "org.knime.base.node.mine.regression.PMMLRegressionPortObject");
        PMML_PORTOBJECT_CLASSES.add(
            "org.knime.base.node.mine.regression.pmmlgreg"
                + ".PMMLGeneralRegressionPortObject");
        PMML_PORTOBJECT_CLASSES.add(
            "org.knime.base.node.mine.subgroupminer"
                + ".PMMLAssociationRulePortObject");
        PMML_PORTOBJECT_CLASSES.add(
            "org.knime.base.node.mine.svm.PMMLSVMPortObject");
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
    public static void save(final SingleNodeContainer snc,
            final ReferencedFile nodeFile, final ExecutionMonitor execMon,
            final boolean isSaveData)
    throws IOException, CanceledExecutionException {
        final Node node = snc.getNode();
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        final ReferencedFile nodeDirRef = nodeFile.getParent();
        if (nodeDirRef == null) {
            throw new IOException("parent file of file \"" + nodeFile
                    + "\" is not represented as object of class "
                    + ReferencedFile.class.getSimpleName());
        }
        saveCustomName(node, settings);
        node.saveSettingsTo(settings);
        saveHasContent(node, settings);
        saveIsInactive(node, settings);
        saveWarningMessage(node, settings);
        ReferencedFile nodeInternDirRef = getNodeInternDirectory(nodeDirRef);
        File nodeInternDir = nodeInternDirRef.getFile();
        if (nodeInternDir.exists()) {
            FileUtil.deleteRecursively(nodeInternDir);
        }
        ExecutionMonitor internalMon = execMon.createSilentSubProgress(0.2);
        ExecutionMonitor portMon = execMon.createSilentSubProgress(0.5);
        ExecutionMonitor intTblsMon = execMon.createSilentSubProgress(0.1);
        ExecutionMonitor fileStoreMon = execMon.createSilentSubProgress(0.1);
        execMon.setMessage("Internals");
        boolean isSaveInternals = isSaveData;
        if (isSaveInternals) {
            // do not save data for inactive nodes
            isSaveInternals =
                node.isInactiveBranchConsumer() || !node.isInactive();
        }
        if (isSaveInternals) {
            saveNodeInternDirectory(node, nodeInternDir, settings, internalMon);
        }
        internalMon.setProgress(1.0);
        /* A hash set of all tables that originate from the corresponding node
         * (owner ID of the table equals NodeID), which have already been saved.
         * It is used to avoid multiple saves of the same table, e.g. when one
         * table is returned in multiple outputs or if an output table is used
         * as "internal" held table. See bug 2117. */
        final Set<Integer> savedTableIDs = new HashSet<Integer>();
        execMon.setMessage("Ports");
        savePorts(node, nodeDirRef, settings, savedTableIDs, portMon, isSaveData);
        portMon.setProgress(1.0);
        execMon.setMessage("Internal Tables");
        saveInternalHeldTables(node, nodeDirRef, settings, savedTableIDs, internalMon, isSaveData);
        intTblsMon.setProgress(1.0);
        // save them last as now all tables have been saved (all cells ran through persistor) and all
        // FileStore#getFile() have been called and saved
        execMon.setMessage("File Store Objects");
        saveFileStoreObjects(node, nodeDirRef, settings, fileStoreMon, isSaveData);
        fileStoreMon.setProgress(1.0);
        // file name has already correct ending
        OutputStream os = new FileOutputStream(nodeFile.getFile());
        if (snc.getParent().isEncrypted()) {
            os = snc.getParent().cipherOutput(os);
        }
        settings.saveToXML(new BufferedOutputStream(os));
        execMon.setProgress(1.0);
    }

    private static void savePorts(final Node node, final ReferencedFile nodeDirRef,
            final NodeSettingsWO settings, final Set<Integer> savedTableIDs,
            final ExecutionMonitor exec, final boolean saveData) throws IOException,
            CanceledExecutionException {
        if (node.getNrOutPorts() == 0) {
            return;
        }
        final int portCount = node.getNrOutPorts();
        NodeSettingsWO portSettings = settings.addNodeSettings("ports");
        exec.setMessage("Saving outport data");
        // starting at port 1 (ignore default flow variable output)
        for (int i = 1; i < portCount; i++) {
            String portName = PORT_FOLDER_PREFIX + i;
            ExecutionMonitor subProgress =
                    exec.createSubProgress(1.0 / portCount);
            NodeSettingsWO singlePortSetting =
                    portSettings.addNodeSettings(portName);
            singlePortSetting.addInt("index", i);
            PortObject object = node.getOutputObject(i);
            String portDirName;
            if (object != null && saveData) {
                portDirName = portName;
                ReferencedFile portDirRef =
                        new ReferencedFile(nodeDirRef, portDirName);
                File portDir = portDirRef.getFile();
                subProgress.setMessage("Cleaning directory "
                        + portDir.getAbsolutePath());
                FileUtil.deleteRecursively(portDir);
                portDir.mkdir();
                if (!portDir.isDirectory() || !portDir.canWrite()) {
                    throw new IOException("Can not write port directory "
                            + portDir.getAbsolutePath());
                }
                savePort(node, portDir, singlePortSetting, savedTableIDs,
                        subProgress, i, saveData);
            } else {
                portDirName = null;
            }
            singlePortSetting.addString("port_dir_location", portDirName);
            subProgress.setProgress(1.0);
        }
    }

    private static void saveInternalHeldTables(final Node node,
            final ReferencedFile nodeDirRef, final NodeSettingsWO settings,
            final Set<Integer> savedTableIDs, final ExecutionMonitor exec,
            final boolean saveData)
            throws IOException, CanceledExecutionException {
        BufferedDataTable[] internalTbls = node.getInternalHeldTables();
        if (internalTbls == null || !saveData) {
            return;
        }
        final int internalTblsCount = internalTbls.length;
        NodeSettingsWO subSettings = settings.addNodeSettings("internalTables");
        String subDirName = INTERNAL_TABLE_FOLDER_PREFIX;
        ReferencedFile subDirFile = new ReferencedFile(nodeDirRef, subDirName);
        subSettings.addString("location", subDirName);
        NodeSettingsWO portSettings = subSettings.addNodeSettings("content");
        FileUtil.deleteRecursively(subDirFile.getFile());
        subDirFile.getFile().mkdirs();

        exec.setMessage("Saving internally held data");
        for (int i = 0; i < internalTblsCount; i++) {
            BufferedDataTable t = internalTbls[i];
            String tblName = "table_" + i;
            ExecutionMonitor subProgress =
                    exec.createSubProgress(1.0 / internalTblsCount);
            NodeSettingsWO singlePortSetting =
                    portSettings.addNodeSettings(tblName);
            singlePortSetting.addInt("index", i);
            String tblDirName;
            if (t != null) {
                tblDirName = tblName;
                ReferencedFile portDirRef =
                        new ReferencedFile(subDirFile, tblDirName);
                File portDir = portDirRef.getFile();
                portDir.mkdir();
                if (!portDir.isDirectory() || !portDir.canWrite()) {
                    throw new IOException("Can not write table directory "
                            + portDir.getAbsolutePath());
                }
                saveBufferedDataTable(t, savedTableIDs, portDir, exec);
            } else {
                tblDirName = null;
            }
            singlePortSetting.addString("table_dir_location", tblDirName);
            subProgress.setProgress(1.0);
        }
    }

    private static void savePort(final Node node, final File portDir,
            final NodeSettingsWO settings, final Set<Integer> savedTableIDs,
            final ExecutionMonitor exec, final int portIdx,
            final boolean saveData) throws IOException, CanceledExecutionException {
        PortObjectSpec spec = node.getOutputSpec(portIdx);
        settings.addString("port_spec_class", spec != null ? spec.getClass()
                .getName() : null);
        PortObject object = node.getOutputObject(portIdx);
        String summary = node.getOutputObjectSummary(portIdx);
        boolean isSaveObject = saveData && object != null;
        settings.addString("port_object_class", isSaveObject ? object
                .getClass().getName() : null);
        if (saveData && object != null) {
            settings.addString("port_object_summary", summary);
        }
        boolean isBDT = object instanceof BufferedDataTable
            || node.getOutputType(portIdx).equals(BufferedDataTable.TYPE);
        boolean isInactive = spec instanceof InactiveBranchPortObjectSpec;
        if (isBDT && !isInactive) {
            assert object == null || object instanceof BufferedDataTable
                : "Expected BufferedDataTable, got "
                    + object.getClass().getSimpleName();
            // executed and instructed to save data
            if (saveData && object != null) {
                saveBufferedDataTable((BufferedDataTable)object,
                        savedTableIDs, portDir, exec);
            }
        } else {
            exec.setMessage("Saving specification");
            if (isSaveObject) {
                assert spec != null
                : "Spec is null but port object is non-null (port "
                    + portIdx + " of node " + node.getName() + ")";
                if (!(object instanceof BufferedDataTable)) {
                    String specDirName = "spec";
                    String specFileName = "spec.zip";
                    String specPath = specDirName + "/" + specFileName;
                    File specDir = new File(portDir, specDirName);
                    specDir.mkdir();
                    if (!specDir.isDirectory() || !specDir.canWrite()) {
                        throw new IOException("Can't create directory "
                                + specDir.getAbsolutePath());
                    }

                    File specFile = new File(specDir, specFileName);
                    PortObjectSpecZipOutputStream out =
                        PortUtil.getPortObjectSpecZipOutputStream(
                                new BufferedOutputStream(
                                        new FileOutputStream(specFile)));
                    settings.addString("port_spec_location", specPath);
                    PortObjectSpecSerializer serializer =
                        PortUtil.getPortObjectSpecSerializer(spec.getClass());
                    serializer.savePortObjectSpec(spec, out);
                    out.close();
                }
                String objectDirName = null;
                objectDirName = "object";
                File objectDir = new File(portDir, objectDirName);
                objectDir.mkdir();
                if (!objectDir.isDirectory() || !objectDir.canWrite()) {
                    throw new IOException("Can't create directory "
                            + objectDir.getAbsolutePath());
                }
                String objectPath;
                // object is BDT, but port type is not BDT.TYPE - still though..
                if (object instanceof BufferedDataTable) {
                    objectPath = objectDirName;
                    saveBufferedDataTable((BufferedDataTable)object,
                            savedTableIDs, objectDir, exec);
                } else {
                    String objectFileName = "portobject.zip";
                    objectPath = objectDirName + "/" + objectFileName;
                    File file = new File(objectDir, objectFileName);
                    PortObjectZipOutputStream out =
                        PortUtil.getPortObjectZipOutputStream(
                                new BufferedOutputStream(
                                        new FileOutputStream(file)));
                    PortObjectSerializer serializer =
                            PortUtil.getPortObjectSerializer(object.getClass());
                    serializer.savePortObject(object, out, exec);
                    out.close();
                }
                settings.addString("port_object_location", objectPath);
            }
        }
    }

    /**
     * @param node
     * @param nodeDirRef
     * @param settings
     * @param fileStoreMon
     * @param isSaveData
     * @throws IOException */
    private static void saveFileStoreObjects(final Node node,
            final ReferencedFile nodeDirRef, final NodeSettings settings,
            final ExecutionMonitor fileStoreMon, final boolean isSaveData)
        throws IOException {
        NodeSettingsWO fsSettings = settings.addNodeSettings("filestores");
        IFileStoreHandler fileStoreHandler = node.getFileStoreHandler();
        String uuidS;
        String dirNameInFlow;
        if (isSaveData && fileStoreHandler instanceof WriteFileStoreHandler) {
            final WriteFileStoreHandler defFileStoreHandler =
                (WriteFileStoreHandler)fileStoreHandler;
            File baseDir = defFileStoreHandler.getBaseDir();
            dirNameInFlow = baseDir == null ? null : FILESTORE_FOLDER_PREFIX;
            if (dirNameInFlow != null) {
                File saveLocation = new File(
                        nodeDirRef.getFile(), dirNameInFlow);
                FileUtil.copyDir(baseDir, saveLocation);
            }
            uuidS = defFileStoreHandler.getStoreUUID().toString();
        } else {
            uuidS = null;
            dirNameInFlow = null;
        }
        fsSettings.addString("file_store_location", dirNameInFlow);
        fsSettings.addString("file_store_id", uuidS);
    }

    private static void saveBufferedDataTable(final BufferedDataTable table,
            final Set<Integer> savedTableIDs, final File directory,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        table.save(directory, savedTableIDs, exec);
    }

    private static void saveHasContent(final Node node, final NodeSettingsWO settings) {
        boolean hasContent = node.hasContent();
        settings.addBoolean("hasContent", hasContent);
    }

    private static void saveIsInactive(final Node node, final NodeSettingsWO settings) {
        boolean isInactive = node.isInactive();
        settings.addBoolean("isInactive", isInactive);
    }

    private static void saveWarningMessage(
            final Node node, final NodeSettingsWO settings) {
        String warnMessage = node.getWarningMessageFromModel();
        if (warnMessage != null) {
            settings.addString(CFG_NODE_MESSAGE, warnMessage);
        }
    }

    /** Sub-class hook to save location of internal directory.
     * @param node Node
     * @param nodeInternDir Directory
     * @param settings Ignored (possibly not in sub-classes)
     * @param exec exec mon.
     * @throws CanceledExecutionException If canceled.
     */
    static void saveNodeInternDirectory(final Node node,
            final File nodeInternDir, final NodeSettingsWO settings,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        node.saveInternals(nodeInternDir, exec);
    }

    static void saveCustomName(final Node node, final NodeSettingsWO settings) {
        settings.addString(CFG_NAME, node.getName());
    }

    /** {@inheritDoc} */
    @Override
    protected boolean loadIsExecuted(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected String loadWarningMessage(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getString(CFG_NODE_MESSAGE, null);
    }

    /** {@inheritDoc} */
    @Override
    public LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy() {
        LoadNodeModelSettingsFailPolicy result =
            getSingleNodeContainerPersistor().getModelSettingsFailPolicy();
        if (isInactive()) {
            // silently ignore invalid settings for dead branches
            // (nodes may be saved as EXECUTED but they were never actually
            // executed but only set to be inactive)
            return LoadNodeModelSettingsFailPolicy.IGNORE;
        }
        assert result != null : "fail policy is null";
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public PortType[] guessOutputPortTypes(final WorkflowPersistor parentPersistor,
               final LoadResult loadResult, final String nodeName) throws IOException, InvalidSettingsException {
        NodeSettingsRO settings = loadSettingsFromConfigFile(parentPersistor, loadResult, nodeName);
        if (!loadHasContent(settings)) {
            return null;
        }
        NodeSettingsRO portsSettings = loadPortsSettings(settings);
        Set<String> keySet = portsSettings.keySet();
        PortType[] outTypes = new PortType[keySet.size()];
        for (String key : keySet) {
            NodeSettingsRO singlePortSetting = portsSettings.getNodeSettings(key);
            // the 0 index is omitted in settings (flow variable port)
            int index = loadPortIndex(singlePortSetting) - 1;
            String portObjectClass = loadPortObjectClassName(singlePortSetting);
            if (BufferedDataTable.TYPE.getPortObjectClass().getName().equals(portObjectClass)) {
                outTypes[index] = BufferedDataTable.TYPE;
            } else {
                Class<?> cl;
                try {
                    cl = Class.forName(portObjectClass);
                } catch (ClassNotFoundException e) {
                    String msg = "Can't load port object class \"" + portObjectClass + "\"";
                    loadResult.addError(msg);
                    getLogger().debug(msg, e);
                    cl = PortObject.class;
                }
                if (!PortObject.class.isAssignableFrom(cl)) {
                    String msg = "Class \"" + cl.getSimpleName() + "\" is not a sub-class \""
                            + PortObject.class.getSimpleName() + "\"";
                    loadResult.addError(msg);
                    cl = PortObject.class;
                }
                outTypes[index] = new PortType(cl.asSubclass(PortObject.class));
            }
        }
        return outTypes;
    }


    /** {@inheritDoc} */
    @Override
    boolean loadHasContent(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getBoolean("hasContent");
    }

    /** {@inheritDoc} */
    @Override
    boolean loadIsInactive(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (getLoadVersion().ordinal() >= LoadVersion.V230.ordinal()) {
            return settings.getBoolean("isInactive", false);
        } else {
            return super.loadIsInactive(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    boolean loadIsConfigured(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    void loadInternalHeldTables(final Node node, final ExecutionMonitor execMon,
            final NodeSettingsRO settings,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep,
            final FileStoreHandlerRepository fileStoreHandlerRepository)
    throws IOException, InvalidSettingsException, CanceledExecutionException {
        if (!settings.containsKey("internalTables")) {
            return;
        }
        NodeSettingsRO subSettings = settings.getNodeSettings("internalTables");
        String subDirName = subSettings.getString("location");
        ReferencedFile subDirFile = new ReferencedFile(getNodeDirectory(), subDirName);
        NodeSettingsRO portSettings = subSettings.getNodeSettings("content");
        Set<String> keySet = portSettings.keySet();
        BufferedDataTable[] result = new BufferedDataTable[keySet.size()];
        for (String s : keySet) {
            ExecutionMonitor subProgress =
                execMon.createSubProgress(1.0 / result.length);
            NodeSettingsRO singlePortSetting =
                portSettings.getNodeSettings(s);
            int index = singlePortSetting.getInt("index");
            if (index < 0 || index >= result.length) {
                throw new InvalidSettingsException("Invalid index: " + index);
            }
            String location = singlePortSetting.getString("table_dir_location");
            if (location == null) {
                result[index] = null;
            } else {
                ReferencedFile portDirRef =
                    new ReferencedFile(subDirFile, location);
                File portDir = portDirRef.getFile();
                if (!portDir.isDirectory() || !portDir.canRead()) {
                    throw new IOException("Can not read table directory "
                            + portDir.getAbsolutePath());
                }
                BufferedDataTable t = loadBufferedDataTable(portDirRef,
                        subProgress, loadTblRep, tblRep,
                        fileStoreHandlerRepository);
                result[index] = t;
            }
            subProgress.setProgress(1.0);
        }
        setInternalHeldTables(result);
    }

    /** {@inheritDoc} */
    @Override
    IFileStoreHandler loadFileStoreHandler(final Node node,
            final ExecutionMonitor execMon, final NodeSettingsRO settings,
            final WorkflowFileStoreHandlerRepository fileStoreHandlerRepository)
    throws InvalidSettingsException {
        if (getLoadVersion().ordinal() < LoadVersion.V260.ordinal()) {
            return super.loadFileStoreHandler(node, execMon,
                    settings, fileStoreHandlerRepository);
        }
        NodeSettingsRO fsSettings = settings.getNodeSettings("filestores");
        String dirNameInFlow = fsSettings.getString("file_store_location");
        if (dirNameInFlow == null) {
            return new EmptyFileStoreHandler(fileStoreHandlerRepository);
        } else {
            String uuidS = fsSettings.getString("file_store_id");
            UUID uuid = UUID.fromString(uuidS);
            ReferencedFile subDirFile =
                new ReferencedFile(getNodeDirectory(), dirNameInFlow);
            IFileStoreHandler fsh = WriteFileStoreHandler.restore(node.getName(), uuid,
                    fileStoreHandlerRepository, subDirFile.getFile());
            return fsh;
        }
    }

    /** {@inheritDoc} */
    @Override
    void loadPorts(final Node node, final ExecutionMonitor exec,
            final NodeSettingsRO settings,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep,
            final FileStoreHandlerRepository fileStoreHandlerRepository)
    throws IOException, InvalidSettingsException, CanceledExecutionException {
        final int nrOutPorts = node.getNrOutPorts();
        if (nrOutPorts == 1) {
            // only the mandatory flow variable port
            return;
        }
        NodeSettingsRO portsSettings = loadPortsSettings(settings);
        exec.setMessage("Reading outport data");
        for (String key : portsSettings.keySet()) {
            NodeSettingsRO singlePortSetting = portsSettings.getNodeSettings(key);
            ExecutionMonitor subProgress = exec.createSubProgress(1 / (double)nrOutPorts);
            int index = loadPortIndex(singlePortSetting);
            if (index < 0 || index >= nrOutPorts) {
                throw new InvalidSettingsException("Invalid outport index in settings: " + index);
            }
            String portDirN = singlePortSetting.getString("port_dir_location");
            if (portDirN != null) {
                ReferencedFile portDir =
                        new ReferencedFile(getNodeDirectory(), portDirN);
                subProgress.setMessage("Port " + index);
                loadPort(node, portDir, singlePortSetting, subProgress, index,
                        loadTblRep, tblRep, fileStoreHandlerRepository);
            }
            subProgress.setProgress(1.0);
        }
    }

    /**
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    NodeSettingsRO loadPortsSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings.getNodeSettings("ports");
    }

    int loadPortIndex(final NodeSettingsRO singlePortSetting)
        throws InvalidSettingsException {
        int index = singlePortSetting.getInt("index");
        // KNIME v2.1 and before had no optional flow variable input port
        // port 0 in v2.1 is port 1 now.
        if (getLoadVersion().ordinal() < LoadVersion.V220.ordinal()) {
            index = index + 1;
        }
        return index;
    }


    void loadPort(final Node node, final ReferencedFile portDir,
            final NodeSettingsRO settings, final ExecutionMonitor exec,
            final int portIdx,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep,
            final FileStoreHandlerRepository fileStoreHandlerRepository)
    throws IOException, InvalidSettingsException, CanceledExecutionException {
        String specClass = settings.getString("port_spec_class");
        String objectClass = loadPortObjectClassName(settings);

        PortType designatedType = node.getOutputType(portIdx);
        PortObjectSpec spec = null;
        PortObject object = null;
        // this can not be simplified as BDT must be loaded as BDT even if
        // the port type is not BDT (but general PortObject)
        boolean isBDT =
                (BufferedDataTable.TYPE.getPortObjectClass().getName().equals(
                        objectClass) && BufferedDataTable.TYPE
                        .getPortObjectSpecClass().getName().equals(specClass))
                        || designatedType.equals(BufferedDataTable.TYPE);
        // an InactiveBranchPortObjectSpec can be put into any port!
        boolean isInactive =
            InactiveBranchPortObjectSpec.class.getName().equals(specClass);
        if (isBDT && !isInactive) {
            if (specClass != null
                    && !specClass.equals(BufferedDataTable.TYPE
                            .getPortObjectSpecClass().getName())) {
                throw new IOException("Actual spec class \""
                        + specClass
                        + "\", expected \""
                        + BufferedDataTable.TYPE.getPortObjectSpecClass()
                                .getName() + "\"");
            }
            if (objectClass != null
                    && !objectClass.equals(BufferedDataTable.TYPE
                            .getPortObjectClass().getName())) {
                throw new IOException("Actual object class \"" + objectClass
                        + "\", expected \""
                        + BufferedDataTable.TYPE.getPortObjectClass().getName()
                        + "\"");
            }
            if (objectClass != null) {
                object = loadBufferedDataTable(portDir, exec,
                        loadTblRep, tblRep, fileStoreHandlerRepository);
                ((BufferedDataTable)object).setOwnerRecursively(node);
                spec = ((BufferedDataTable)object).getDataTableSpec();
            } else if (specClass != null) {
                spec = BufferedDataTable.loadSpec(portDir);
            }
        } else {
            exec.setMessage("Loading specification");
            if (specClass != null) {
                Class<?> cl;
                try {
                    cl = Class.forName(specClass);
                } catch (ClassNotFoundException e) {
                    throw new IOException("Can't load class \"" + specClass
                            + "\"", e);
                }
                if (!PortObjectSpec.class.isAssignableFrom(cl)) {
                    throw new IOException("Class \"" + cl.getSimpleName()
                            + "\" is not a sub-class \""
                            + PortObjectSpec.class.getSimpleName() + "\"");
                }
                ReferencedFile specDirRef =
                        new ReferencedFile(portDir, settings
                                .getString("port_spec_location"));
                File specFile = specDirRef.getFile();
                if (!specFile.isFile()) {
                    throw new IOException("Can't read spec file "
                            + specFile.getAbsolutePath());
                }
                PortObjectSpecZipInputStream in =
                    PortUtil.getPortObjectSpecZipInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(specFile)));
                PortObjectSpecSerializer<?> serializer =
                        PortUtil.getPortObjectSpecSerializer(cl
                                .asSubclass(PortObjectSpec.class));
                spec = serializer.loadPortObjectSpec(in);
                in.close();
                if (spec == null) {
                    throw new IOException("Serializer \""
                            + serializer.getClass().getName()
                            + "\" restored null spec ");
                }
            }
            if (spec != null && objectClass != null) {
                Class<?> cl;
                try {
                    cl = Class.forName(objectClass);
                } catch (ClassNotFoundException e) {
                    throw new IOException("Can't load port object class \""
                            + objectClass + "\"", e);
                }
                if (!PortObject.class.isAssignableFrom(cl)) {
                    throw new IOException("Class \"" + cl.getSimpleName()
                            + "\" is not a sub-class \""
                            + PortObject.class.getSimpleName() + "\"");
                }
                ReferencedFile objectFileRef =
                        new ReferencedFile(portDir, settings
                                .getString("port_object_location"));
                if (BufferedDataTable.class.equals(cl)) {
                    File objectDir = objectFileRef.getFile();
                    if (!objectDir.isDirectory()) {
                        throw new IOException("Can't read directory "
                                + objectDir.getAbsolutePath());
                    }
                    // can't be true, however as BDT can only be saved
                    // for adequate port types (handled above)
                    // we leave the code here for future versions..
                    object = loadBufferedDataTable(objectFileRef, exec,
                            loadTblRep, tblRep, fileStoreHandlerRepository);
                    ((BufferedDataTable)object).setOwnerRecursively(node);
                } else {
                    File objectFile = objectFileRef.getFile();
                    if (!objectFile.isFile()) {
                        throw new IOException("Can't read file "
                                + objectFile.getAbsolutePath());
                    }
                    // buffering both disc I/O and the gzip stream pays off
                    PortObjectZipInputStream in =
                        PortUtil.getPortObjectZipInputStream(
                                new BufferedInputStream(
                                        new FileInputStream(objectFile)));
                    PortObjectSerializer<?> serializer =
                            PortUtil.getPortObjectSerializer(cl
                                    .asSubclass(PortObject.class));
                    object = serializer.loadPortObject(in, spec, exec);
                    in.close();
                }
            }
        }
        if (spec != null) {
            if (!designatedType.getPortObjectSpecClass().isInstance(spec)
                    && !isInactive) {
                throw new IOException("Actual port spec type (\""
                        + spec.getClass().getSimpleName()
                        + "\") does not match designated one (\""
                        + designatedType.getPortObjectSpecClass()
                                .getSimpleName() + "\")");
            }
        }
        String summary = null;
        if (object != null) {
            if (!designatedType.getPortObjectClass().isInstance(object)
                    && !isInactive) {
                throw new IOException("Actual port object type (\""
                        + object.getClass().getSimpleName()
                        + "\") does not match designated one (\""
                        + designatedType.getPortObjectClass().getSimpleName()
                        + "\")");
            }
            summary = settings.getString("port_object_summary", null);
            if (summary == null) {
                summary = object.getSummary();
            }
        }
        setPortObjectSpec(portIdx, spec);
        setPortObject(portIdx, object);
        setPortObjectSummary(portIdx, summary);
    }

    /**
     * @param singlePortSettings ...
     * @return ...
     * @throws InvalidSettingsException ...
     */
    String loadPortObjectClassName(final NodeSettingsRO singlePortSettings) throws InvalidSettingsException {
        String objectClass = singlePortSettings.getString("port_object_class");

        /* In versions < V230 different PMMLPortObject classes existed which
         * got all replaced by a general PMMLPortObject. To stay backward
         * compatible we have to load the new object for them. */
        if (getLoadVersion().ordinal() <= LoadVersion.V230.ordinal()
                && PMML_PORTOBJECT_CLASSES.contains(objectClass)) {
            objectClass = PMMLPortObject.class.getName();
        }
        return objectClass;
    }

    private BufferedDataTable loadBufferedDataTable(
            final ReferencedFile objectDir, final ExecutionMonitor exec,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep,
            final FileStoreHandlerRepository fileStoreHandlerRepository)
            throws CanceledExecutionException, IOException,
            InvalidSettingsException {
        return BufferedDataTable.loadFromFile(objectDir, /* ignored in 1.2+ */
        null, exec, loadTblRep, tblRep, fileStoreHandlerRepository);
    }
}
