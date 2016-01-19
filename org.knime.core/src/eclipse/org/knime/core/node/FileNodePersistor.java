/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.EmptyFileStoreHandler;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.FileStoreKey;
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
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FileNativeNodeContainerPersistor;
import org.knime.core.node.workflow.FileWorkflowPersistor;
import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class FileNodePersistor implements NodePersistor {

    /* Contains all fully qualified path names of previously existing
     *  PMMLPortObjects that have been removed in version v2.4. This is
     *  necessary for being able to replace them with the general PMMLPortObject
     *  on loading. */
    private static final Set<String> PMML_PORTOBJECT_CLASSES;
    static {
        PMML_PORTOBJECT_CLASSES = new HashSet<String>();
        PMML_PORTOBJECT_CLASSES.add("org.knime.base.node.mine.cluster.PMMLClusterPortObject");
        PMML_PORTOBJECT_CLASSES.add("org.knime.base.node.mine.decisiontree2.PMMLDecisionTreePortObject");
        PMML_PORTOBJECT_CLASSES.add("org.knime.base.node.mine.neural.mlp.PMMLNeuralNetworkPortObject");
        PMML_PORTOBJECT_CLASSES.add("org.knime.base.node.mine.regression.PMMLRegressionPortObject");
        PMML_PORTOBJECT_CLASSES.add("org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionPortObject");
        PMML_PORTOBJECT_CLASSES.add("org.knime.base.node.mine.subgroupminer.PMMLAssociationRulePortObject");
        PMML_PORTOBJECT_CLASSES.add("org.knime.base.node.mine.svm.PMMLSVMPortObject");
    }

    /** @noreference Not public API. */
    public static final String FILESTORE_FOLDER_PREFIX = "filestore";

    /**
     * Prefix of associated port folders. (Also used in export wizard, public declaration here.)
     */
    public static final String PORT_FOLDER_PREFIX = "port_";

    /**
     * Prefix of associated port folders. (Also used in export wizard, public declaration here.)
     */
    public static final String INTERNAL_TABLE_FOLDER_PREFIX = "internalTables";

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final FileNativeNodeContainerPersistor m_nncPersistor;

    private final NodeSettingsRO m_settings;

    /** Load Version, see {@link #getLoadVersion()} for details. */
    private final FileWorkflowPersistor.LoadVersion m_loadVersion;

    private boolean m_isExecuted;

    private boolean m_hasContent;

    private boolean m_isInactive;

    private boolean m_isConfigured;

    private ReferencedFile m_nodeInternDirectory;

    private PortObject[] m_portObjects;

    private PortObjectSpec[] m_portObjectSpecs;

    private String[] m_portObjectSummaries;

    private PortObject[] m_internalHeldObjects;

    private IFileStoreHandler m_fileStoreHandler;

    private boolean m_needsResetAfterLoad;

    private boolean m_isDirtyAfterLoad;

    /**
     * List of factories (only the simple class name), which were auto-executable in 1.3.x and need to be restored as
     * configured only.
     */
    public static final List<String> OLD_AUTOEXECUTABLE_NODEFACTORIES = Arrays.asList("InteractivePieNodeFactory",
        "HistogramNodeFactory", "JmolViewerNodeFactory", "TableNodeFactory");

    static String createDataFileDirName(final int index) {
        return DATA_FILE_PREFIX + index;
    }

    static String createModelFileName(final int index) {
        return MODEL_FILE_PREFIX + index + ".pmml.gz";
    }

    /**
     * Creates persistor for both load and save.
     *
     * @param nncPersistor The corresponding node container persistor.
     * @param version The version string, see {@link #getLoadVersion()} for details
     * @param settings The settings from the settings file stored in the node folder (not null).
     */
    public FileNodePersistor(final FileNativeNodeContainerPersistor nncPersistor,
        final FileWorkflowPersistor.LoadVersion version, final NodeSettingsRO settings) {
        if (nncPersistor == null || settings == null) {
            throw new NullPointerException();
        }
        m_nncPersistor = nncPersistor;
        m_loadVersion = version;
        m_settings = settings;
    }

    protected NodeLogger getLogger() {
        return m_logger;
    }

    /**
     * Version being loaded. This is given by the SNC-Persistor.
     *
     * @return Version being loaded. Can also be null unless enforced in constructor of subclass.
     */
    public FileWorkflowPersistor.LoadVersion getLoadVersion() {
        return m_loadVersion;
    }

    boolean loadIsExecuted(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V200)) {
            return settings.getBoolean(CFG_ISEXECUTED);
        } else {
            return false; // no longer saved in node itself
        }
    }

    boolean loadHasContent(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V200)) {
            return settings.getBoolean(CFG_ISEXECUTED);
        } else {
            return settings.getBoolean("hasContent");
        }
    }

    boolean loadIsInactive(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V230)) {
            return false;
        } else {
            return settings.getBoolean("isInactive", false);
        }
    }

    /**
     * Sub class hook to read warning message.
     *
     * @param settings Ignored
     * @return null
     * @throws InvalidSettingsException Not actually thrown
     */
    String loadWarningMessage(final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }

    boolean loadIsConfigured(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V200)) {
            return settings.getBoolean(CFG_ISCONFIGURED);
        } else {
            return false; // no longer saved
        }
    }

    static ReferencedFile getNodeInternDirectory(final ReferencedFile nodeDir) {
        return new ReferencedFile(nodeDir, INTERN_FILE_DIR);
    }

    /**
     * Load internal directory.
     *
     * @param settings Ignored (but allows sub-classing)
     * @param nodeDir Node directory.
     * @return Internal directory.
     * @throws InvalidSettingsException If that fails for any reason.
     */
    ReferencedFile loadNodeInternDirectory(final NodeSettingsRO settings, final ReferencedFile nodeDir)
        throws InvalidSettingsException {
        return getNodeInternDirectory(nodeDir);
    }

    /**
     * @noreference
     * @nooverride
     */
    void loadPorts(final Node node, final ExecutionMonitor exec, final NodeSettingsRO settings,
        final Map<Integer, BufferedDataTable> loadTblRep, final HashMap<Integer, ContainerTable> tblRep,
        final FileStoreHandlerRepository fileStoreHandlerRepository) throws IOException, InvalidSettingsException,
        CanceledExecutionException {
        final int nrOutPorts = node.getNrOutPorts();
        if (getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V200)) {
            // skip flow variables port (introduced in v2.2)
            for (int i = 1; i < nrOutPorts; i++) {
                int oldIndex = getOldPortIndex(i);
                ExecutionMonitor execPort = exec.createSubProgress(1.0 / nrOutPorts);
                exec.setMessage("Port " + oldIndex);
                PortType type = node.getOutputType(i);
                boolean isDataPort = BufferedDataTable.class.isAssignableFrom(type.getPortObjectClass());
                if (m_isConfigured) {
                    PortObjectSpec spec = loadPortObjectSpec(node, settings, oldIndex);
                    setPortObjectSpec(i, spec);
                }
                if (m_isExecuted) {
                    PortObject object;
                    if (isDataPort) {
                        object =
                            loadBufferedDataTable(node, settings, execPort, loadTblRep, oldIndex, tblRep,
                                fileStoreHandlerRepository);
                    } else {
                        throw new IOException("Can't restore model ports of " + "old 1.x workflows. Execute node again.");
                    }
                    String summary = object != null ? object.getSummary() : null;
                    setPortObject(i, object);
                    setPortObjectSummary(i, summary);
                }
                execPort.setProgress(1.0);
            }
        } else {
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
                    ReferencedFile portDir = new ReferencedFile(getNodeDirectory(), portDirN);
                    subProgress.setMessage("Port " + index);
                    loadPort(node, portDir, singlePortSetting, subProgress, index, loadTblRep, tblRep,
                        fileStoreHandlerRepository);
                }
                subProgress.setProgress(1.0);
            }

        }
    }

    void loadPort(final Node node, final ReferencedFile portDir, final NodeSettingsRO settings,
        final ExecutionMonitor exec, final int portIdx, final Map<Integer, BufferedDataTable> loadTblRep,
        final HashMap<Integer, ContainerTable> tblRep, final FileStoreHandlerRepository fileStoreHandlerRepository)
        throws IOException, InvalidSettingsException, CanceledExecutionException {
        final String specClass = settings.getString("port_spec_class");
        final String objectClass = loadPortObjectClassName(settings);

        PortType designatedType = node.getOutputType(portIdx);
        PortObjectSpec spec = null;
        PortObject object = null;
        // this cannot be simplified as BDT must be loaded as BDT even if
        // the port type is not BDT (but general PortObject)
        boolean isBDT =
            (BufferedDataTable.TYPE.getPortObjectClass().getName().equals(objectClass) && BufferedDataTable.TYPE
                .getPortObjectSpecClass().getName().equals(specClass))
                || designatedType.equals(BufferedDataTable.TYPE);
        // an InactiveBranchPortObjectSpec can be put into any port!
        boolean isInactive = InactiveBranchPortObjectSpec.class.getName().equals(specClass);
        if (isBDT && !isInactive) {
            if (specClass != null && !specClass.equals(BufferedDataTable.TYPE.getPortObjectSpecClass().getName())) {
                throw new IOException("Actual spec class \"" + specClass + "\", expected \""
                    + BufferedDataTable.TYPE.getPortObjectSpecClass().getName() + "\"");
            }
            if (objectClass != null && !objectClass.equals(BufferedDataTable.TYPE.getPortObjectClass().getName())) {
                throw new IOException("Actual object class \"" + objectClass + "\", expected \""
                    + BufferedDataTable.TYPE.getPortObjectClass().getName() + "\"");
            }
            if (objectClass != null) {
                object = loadBufferedDataTable(portDir, exec, loadTblRep, tblRep, fileStoreHandlerRepository);
                ((BufferedDataTable)object).setOwnerRecursively(node);
                spec = ((BufferedDataTable)object).getDataTableSpec();
            } else if (specClass != null) {
                spec = BufferedDataTable.loadSpec(portDir);
            }
        } else {
            object = loadPortObject(portDir, settings, exec, fileStoreHandlerRepository).orElse(null);
            spec = object != null ? object.getSpec() : null;
        }
        if (spec != null) {
            if (!designatedType.getPortObjectSpecClass().isInstance(spec) && !isInactive) {
                throw new IOException("Actual port spec type (\"" + spec.getClass().getSimpleName()
                    + "\") does not match designated one (\"" + designatedType.getPortObjectSpecClass().getSimpleName()
                    + "\")");
            }
        }
        String summary = null;
        if (object != null) {
            if (!designatedType.getPortObjectClass().isInstance(object) && !isInactive) {
                throw new IOException("Actual port object type (\"" + object.getClass().getSimpleName()
                    + "\") does not match designated one (\"" + designatedType.getPortObjectClass().getSimpleName()
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
     * @param portDir
     * @param settings
     * @param exec
     * @param fileStoreHandlerRepository
     * @return
     * @throws IOException
     * @throws InvalidSettingsException
     * @throws FileNotFoundException
     * @throws CanceledExecutionException
     */
    private Optional<PortObject> loadPortObject(final ReferencedFile portDir, final NodeSettingsRO settings,
        final ExecutionMonitor exec, final FileStoreHandlerRepository fileStoreHandlerRepository)
            throws IOException, InvalidSettingsException, FileNotFoundException, CanceledExecutionException {
        exec.setMessage("Loading port object");
        final String specClass = settings.getString("port_spec_class");
        final String objectClass = loadPortObjectClassName(settings);
        PortObject object = null;
        PortObjectSpec spec = null;
        if (specClass != null) {
            Class<? extends PortObjectSpec> cl = PortTypeRegistry.getInstance().getSpecClass(specClass)
                    .orElseThrow(() ->  new IOException("Invalid spec class \"" + specClass + "\""));
            ReferencedFile specDirRef = new ReferencedFile(portDir, settings.getString("port_spec_location"));
            File specFile = specDirRef.getFile();
            if (!specFile.isFile()) {
                throw new IOException("Can't read spec file " + specFile.getAbsolutePath());
            }
            try (PortObjectSpecZipInputStream in = PortUtil.getPortObjectSpecZipInputStream(
                new BufferedInputStream(new FileInputStream(specFile)))) {
                PortObjectSpecSerializer<?> serializer = PortTypeRegistry.getInstance().getSpecSerializer(cl).get();
                spec = serializer.loadPortObjectSpec(in);
                if (spec == null) {
                    throw new IOException("Serializer \"" + serializer.getClass().getName()
                        + "\" restored null spec ");
                }
            }
        }
        if (spec != null && objectClass != null) {
            Class<? extends PortObject> cl = PortTypeRegistry.getInstance().getObjectClass(objectClass)
                    .orElseThrow(() -> new IOException("Invalid object class \"" + objectClass + "\""));
            ReferencedFile objectFileRef = new ReferencedFile(portDir, settings.getString("port_object_location"));
            File objectFile = objectFileRef.getFile();
            if (!objectFile.isFile()) {
                throw new IOException("Can't read file " + objectFile.getAbsolutePath());
            }
            // buffering both disc I/O and the gzip stream pays off
            try (PortObjectZipInputStream in = PortUtil.getPortObjectZipInputStream(
                new BufferedInputStream(new FileInputStream(objectFile)))) {
                PortObjectSerializer<?> serializer = PortTypeRegistry.getInstance().getObjectSerializer(cl).get();
                object = serializer.loadPortObject(in, spec, exec);
            }
            if (object instanceof FileStorePortObject) {
                File fileStoreXML = new File(objectFile.getParent(), "filestore.xml");
                final ModelContentRO fileStoreModelContent =
                        ModelContent.loadFromXML(new FileInputStream(fileStoreXML));
                List<FileStoreKey> fileStoreKeys = new ArrayList<FileStoreKey>();
                if (getLoadVersion().isOlderThan(LoadVersion.V2100)) {
                    // only one filestore in <2.10 (bug 5227)
                    FileStoreKey fileStoreKey = FileStoreKey.load(fileStoreModelContent);
                    fileStoreKeys.add(fileStoreKey);
                } else {
                    ModelContentRO keysContent = fileStoreModelContent.getModelContent("filestore_keys");
                    for (String id : keysContent.keySet()) {
                        ModelContentRO keyContent = keysContent.getModelContent(id);
                        fileStoreKeys.add(FileStoreKey.load(keyContent));
                    }
                }
                FileStoreUtil.retrieveFileStoreHandlerFrom(
                    (FileStorePortObject)object, fileStoreKeys, fileStoreHandlerRepository);
            }
        }
        return Optional.ofNullable(object);
    }

    private BufferedDataTable loadBufferedDataTable(final ReferencedFile objectDir, final ExecutionMonitor exec,
        final Map<Integer, BufferedDataTable> loadTblRep, final HashMap<Integer, ContainerTable> tblRep,
        final FileStoreHandlerRepository fileStoreHandlerRepository) throws CanceledExecutionException, IOException,
        InvalidSettingsException {
        return BufferedDataTable.loadFromFile(objectDir, /* ignored in 1.2+ */
            null, exec, loadTblRep, tblRep, fileStoreHandlerRepository);
    }

    /**
     * Called on "missing" nodes to guess their output port types (only possible for executed nodes). This
     * implementation returns null; subclasses overwrite it.
     * @param loadResult ...
     * @param nodeName ...
     *
     * @return ...
     * @throws InvalidSettingsException ...
     * @throws IOException ...
     */
    public final PortType[] guessOutputPortTypes(final LoadResult loadResult,
        final String nodeName) throws IOException, InvalidSettingsException {
        if (getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V200)) {
            return null;
        } else {
            if (!loadHasContent(m_settings)) {
                return null;
            }
            NodeSettingsRO portsSettings = loadPortsSettings(m_settings);
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
                        String msg = "Class \"" + cl.getSimpleName() + "\" is not a sub-class of \""
                                + PortObject.class.getSimpleName() + "\"";
                        loadResult.addError(msg);
                        cl = PortObject.class;
                    }
                    outTypes[index] = PortTypeRegistry.getInstance().getPortType(cl.asSubclass(PortObject.class));
                }
            }
            return outTypes;
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

    int loadPortIndex(final NodeSettingsRO singlePortSetting) throws InvalidSettingsException {
        int index = singlePortSetting.getInt("index");
        // KNIME v2.1 and before had no optional flow variable input port
        // port 0 in v2.1 is port 1 now.
        if (getLoadVersion().ordinal() < FileWorkflowPersistor.LoadVersion.V220.ordinal()) {
            index = index + 1;
        }
        return index;
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
        if (getLoadVersion().ordinal() <= FileWorkflowPersistor.LoadVersion.V230.ordinal()
                && PMML_PORTOBJECT_CLASSES.contains(objectClass)) {
            objectClass = PMMLPortObject.class.getName();
        }
        return objectClass;
    }

    /**
     * Subtracts one from the argument. As of v2.2 KNIME has an additional output port (index 0) carrying flow
     * variables.
     *
     * @param loaded Index of port in current version
     * @return Old port index (1 becomes 0, etc)
     */
    private int getOldPortIndex(final int loaded) {
        return loaded - 1;
    }

    /**
     * Adds one to the argument. As of v2.2 KNIME has an additional output port (index 0) carrying flow variables.
     *
     * @param loaded Index of port in version 1.x
     * @return New port index (0 becomes 1, etc)
     */
    private int getNewPortIndex(final int old) {
        return old + 1;
    }

    /** Reads internally held table in version {@link LoadVersion#V2100Pre} and before. Was replaced by
     * #loadInternalHeldObjects then on. */
    void loadInternalHeldTablesPre210(final Node node, final ExecutionMonitor execMon, final NodeSettingsRO settings,
        final Map<Integer, BufferedDataTable> loadTblRep, final HashMap<Integer, ContainerTable> tblRep,
        final FileStoreHandlerRepository fileStoreHandlerRepository) throws IOException, InvalidSettingsException,
        CanceledExecutionException {
        if (getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V200)) {
            return;
        }
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
            ExecutionMonitor subProgress = execMon.createSubProgress(1.0 / result.length);
            NodeSettingsRO singlePortSetting = portSettings.getNodeSettings(s);
            int index = singlePortSetting.getInt("index");
            if (index < 0 || index >= result.length) {
                throw new InvalidSettingsException("Invalid index: " + index);
            }
            String location = singlePortSetting.getString("table_dir_location");
            if (location == null) {
                result[index] = null;
            } else {
                ReferencedFile portDirRef = new ReferencedFile(subDirFile, location);
                readDirectory(portDirRef.getFile());
                portDirRef.getFile();
                BufferedDataTable t =
                    loadBufferedDataTable(portDirRef, subProgress, loadTblRep, tblRep, fileStoreHandlerRepository);
                t.setOwnerRecursively(node);
                result[index] = t;
            }
            subProgress.setProgress(1.0);
        }
        setInternalHeldPortObjects(result);
    }

    /** New with {@link LoadVersion#V2100}, supports {@link org.knime.core.node.port.PortObjectHolder}. */
    void loadInternalHeldObjects(final Node node, final ExecutionMonitor execMon, final NodeSettingsRO settings,
        final Map<Integer, BufferedDataTable> loadTblRep, final HashMap<Integer, ContainerTable> tblRep,
        final FileStoreHandlerRepository fileStoreHandlerRepository) throws IOException, InvalidSettingsException,
        CanceledExecutionException {
        assert !getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V2100);
        if (!settings.containsKey("internalObjects")) {
            return;
        }
        NodeSettingsRO subSettings = settings.getNodeSettings("internalObjects");
        String subDirName = subSettings.getString("location");
        ReferencedFile subDirFile = new ReferencedFile(getNodeDirectory(), subDirName);
        NodeSettingsRO portSettings = subSettings.getNodeSettings("content");
        Set<String> keySet = portSettings.keySet();
        PortObject[] result = new PortObject[keySet.size()];
        for (String s : keySet) {
            ExecutionMonitor subProgress = execMon.createSubProgress(1.0 / result.length);
            NodeSettingsRO singlePortSetting = portSettings.getNodeSettings(s);
            int index = singlePortSetting.getInt("index");
            if (index < 0 || index >= result.length) {
                throw new InvalidSettingsException("Invalid index: " + index);
            }
            String type = singlePortSetting.getString("type");
            PortObject object = null;
            if ("null".equals(type)) {
                // object stays null
            } else if ("table".equals(type)) {
                String location = singlePortSetting.getString("table_dir_location");
                if (location != null) {
                    ReferencedFile portDirRef = new ReferencedFile(subDirFile, location);
                    readDirectory(portDirRef.getFile());
                    BufferedDataTable t = loadBufferedDataTable(
                        portDirRef, subProgress, loadTblRep, tblRep, fileStoreHandlerRepository);
                    t.setOwnerRecursively(node);
                    object = t;
                }
            } else if ("referenced_output".equals(type)) {
                int outputPortIndex = singlePortSetting.getInt("outport");
                CheckUtils.checkSetting(outputPortIndex >= 0, "Port index must not < 0: $d", outputPortIndex);
                object = getPortObject(outputPortIndex);
            } else if ("non-table".equals(type)) {
                String location = singlePortSetting.getString("port_dir_location");
                ReferencedFile portDirRef = new ReferencedFile(subDirFile, location);
                readDirectory(portDirRef.getFile());
                object = loadPortObject(portDirRef, singlePortSetting, subProgress, fileStoreHandlerRepository)
                        // not sure when this can actually happen
                        .orElseThrow(() -> new IOException("Settings do not reference internal held port object"));

            } else {
                CheckUtils.checkSetting(false, "Unknown object reference %s", type);
            }
            result[index] = object;
            subProgress.setProgress(1.0);
        }
        setInternalHeldPortObjects(result);
    }

    IFileStoreHandler loadFileStoreHandler(final Node node, final ExecutionMonitor execMon,
        final NodeSettingsRO settings, final WorkflowFileStoreHandlerRepository fileStoreHandlerRepository)
        throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(FileWorkflowPersistor.LoadVersion.V260)) {
            return new EmptyFileStoreHandler(fileStoreHandlerRepository);
        }
        NodeSettingsRO fsSettings = settings.getNodeSettings("filestores");
        String dirNameInFlow = fsSettings.getString("file_store_location");
        if (dirNameInFlow == null) {
            return new EmptyFileStoreHandler(fileStoreHandlerRepository);
        } else {
            String uuidS = fsSettings.getString("file_store_id");
            UUID uuid = UUID.fromString(uuidS);
            ReferencedFile subDirFile = new ReferencedFile(getNodeDirectory(), dirNameInFlow);
            IFileStoreHandler fsh =
                WriteFileStoreHandler.restore(node.getName(), uuid, fileStoreHandlerRepository, subDirFile.getFile());
            return fsh;
        }
    }

    private BufferedDataTable loadBufferedDataTable(final Node node, final NodeSettingsRO settings,
        final ExecutionMonitor execMon, final Map<Integer, BufferedDataTable> loadTblRep, final int index,
        final HashMap<Integer, ContainerTable> tblRep, final FileStoreHandlerRepository fileStoreHandlerRepository)
        throws InvalidSettingsException, IOException, CanceledExecutionException {
        // in 1.1.x and before the settings.xml contained the location
        // of the data table specs file (spec_0.xml, e.g.). From 1.2.0 on,
        // the spec is saved in data/data_0/spec.xml
        boolean isVersion11x = settings.containsKey(CFG_SPEC_FILES);
        ExecutionMonitor execSubData = execMon.createSubProgress(0.25);
        ReferencedFile nodeDirectory = getNodeDirectory();
        if (isVersion11x) {
            /* In version 1.1.x the data was stored in a different way. The
             * data.xml that is now contained in the data/data_x/ directory was
             * aggregated in a data.xml file directly in the m_nodeDir. Also the
             * spec was located at a different location.
             */
            String dataConfigFileName = settings.getString(CFG_DATA_FILE);
            File nodeDir = nodeDirectory.getFile();
            // dataConfigFile = data.xml in node dir
            File dataConfigFile = new File(nodeDir, dataConfigFileName);
            NodeSettingsRO dataSettings =
                NodeSettings.loadFromXML(new BufferedInputStream(new FileInputStream(dataConfigFile)));
            String dataPath = dataSettings.getString(CFG_DATA_FILE_DIR);
            // dataDir = /data
            ReferencedFile dataDirRef = new ReferencedFile(nodeDirectory, dataPath);
            // note: we do not check for existence here - in some cases
            // this directory may not exist (when exported and empty
            // directories are pruned)
            NodeSettingsRO portSettings = dataSettings.getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            // dir = /data/data_i
            ReferencedFile dirRef = new ReferencedFile(dataDirRef, dataName);
            BufferedDataTable t = BufferedDataTable.loadFromFile(dirRef, portSettings, execSubData, loadTblRep,
            // no blobs or file stores in 1.1.x
                new HashMap<Integer, ContainerTable>(), new WorkflowFileStoreHandlerRepository());
            t.setOwnerRecursively(node);
            return t;
        } else {
            NodeSettingsRO dataSettings = settings.getNodeSettings(CFG_DATA_FILE);
            String dataDirStr = dataSettings.getString(CFG_DATA_FILE_DIR);
            ReferencedFile dataDirRef = new ReferencedFile(nodeDirectory, dataDirStr);
            NodeSettingsRO portSettings = dataSettings.getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            ReferencedFile dirRef = new ReferencedFile(dataDirRef, dataName);
            File dir = dirRef.getFile();
            readDirectory(dir);
            BufferedDataTable t = BufferedDataTable.loadFromFile(dirRef,
            /* ignored in 1.2.0+ */
            null, execMon, loadTblRep, tblRep, fileStoreHandlerRepository);
            t.setOwnerRecursively(node);
            return t;
        }
    }


    private PortObjectSpec loadPortObjectSpec(final Node node, final NodeSettingsRO settings, final int index)
        throws InvalidSettingsException, IOException {
        int newIndex = getNewPortIndex(index);
        PortType type = node.getOutputType(newIndex);
        boolean isDataPort = BufferedDataTable.class.isAssignableFrom(type.getPortObjectClass());
        if (!isDataPort) {
            // port is a model port, no spec available in 1.x.x
            return null;
        }
        // in 1.1.x and before the settings.xml contained the location
        // of the data table specs file (spec_0.xml, e.g.). From 1.2.0 on,
        // the spec is saved in data/data_0/spec.xml
        boolean isVersion11x = settings.containsKey(CFG_SPEC_FILES);
        ReferencedFile nodeDirectory = getNodeDirectory();
        if (isVersion11x) {
            NodeSettingsRO spec = settings.getNodeSettings(CFG_SPEC_FILES);
            String specName = spec.getString(CFG_OUTPUT_PREFIX + index);
            ReferencedFile targetFileRef = new ReferencedFile(nodeDirectory, specName);
            File targetFile = targetFileRef.getFile();
            DataTableSpec outSpec = null;
            if (targetFile.exists()) {
                NodeSettingsRO settingsSpec =
                    NodeSettings.loadFromXML(new BufferedInputStream(new FileInputStream(targetFile)));
                outSpec = DataTableSpec.load(settingsSpec);
            }
            return outSpec;
        } else {
            NodeSettingsRO dataSettings = settings.getNodeSettings(CFG_DATA_FILE);
            String dataDirStr = dataSettings.getString(CFG_DATA_FILE_DIR);
            ReferencedFile dataDirRef = new ReferencedFile(nodeDirectory, dataDirStr);
            NodeSettingsRO portSettings = dataSettings.getNodeSettings(CFG_OUTPUT_PREFIX + index);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            DataTableSpec outSpec = null;
            if (portSettings.getBoolean(CFG_HAS_SPEC_FILE, true)) {
                ReferencedFile dirRef = new ReferencedFile(dataDirRef, dataName);
                File dir = dirRef.getFile();
                readDirectory(dir);
                outSpec = BufferedDataTable.loadSpec(dirRef);
                if (portSettings.containsKey(CFG_HAS_SPEC_FILE) && outSpec == null) {
                    throw new IOException("No spec file available for" + " outport " + index + ".");
                }
            }
            return outSpec;
        }
    }

    /**
     * @return the singleNodeContainerPersistor
     */
    FileNativeNodeContainerPersistor getNativeNodeContainerPersistor() {
        return m_nncPersistor;
    }

    WorkflowLoadHelper getLoadHelper() {
        return m_nncPersistor.getLoadHelper();
    }

    /**
     * Is configured according to the settings object.
     *
     * @return If node is saved in configured state.
     */
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

    /**
     * Accessor for derived class, not part of interface!
     *
     * @return Inactive (dead IF branch)
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean isInactive() {
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
        return getNativeNodeContainerPersistor().mustWarnOnDataLoadError();
    }

    /**
     * Pre-load instance to fill fields that are used in calling class before final load is performed.
     *
     * @param node The target node, used only for meta information (factory class name).
     * @param loadResult where to add errors to
     * @noreference This method is not intended to be referenced by clients.
     */
    public final void preLoad(final Node node, final LoadResult loadResult) {
        try {
            m_hasContent = loadHasContent(m_settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load hasContent flag: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
            setNeedsResetAfterLoad(); // also implies dirty
        }

        try {
            m_isInactive = loadIsInactive(m_settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load isInactive flag: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
            setNeedsResetAfterLoad(); // also implies dirty
        }

        try {
            m_isExecuted = loadIsExecuted(m_settings);
            if (m_isExecuted && OLD_AUTOEXECUTABLE_NODEFACTORIES.contains(node.getFactory().getClass().getSimpleName())) {
                getLogger().debug(
                    "Setting executed flag of node \"" + node.getFactory().getClass().getSimpleName()
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
            m_isConfigured = loadIsConfigured(m_settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load configuration flag: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
            setNeedsResetAfterLoad();
        }
    }

    /**
     * Loads content into node instance.
     *
     * @param node The target node, used for meta info (#ports, e.g) and to invoke the
     *            {@link Node#load(NodePersistor, ExecutionMonitor, LoadResult)} on
     * @param parentPersistor workflow persistor for decryption
     * @param exec For progress/cancelation
     * @param loadTblRep The table repository used during load
     * @param tblRep The table repository for blob handling
     * @param fileStoreHandlerRepository ...
     * @param loadResult where to add errors to
     * @throws IOException If files can't be read
     * @throws CanceledExecutionException If canceled
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride
     */
    public final void load(final Node node, final WorkflowPersistor parentPersistor, final ExecutionMonitor exec,
        final Map<Integer, BufferedDataTable> loadTblRep, final HashMap<Integer, ContainerTable> tblRep,
        final WorkflowFileStoreHandlerRepository fileStoreHandlerRepository, final LoadResult loadResult)
        throws IOException, CanceledExecutionException {
        ExecutionMonitor loadExec = exec.createSilentSubProgress(0.6);
        ExecutionMonitor loadFileStoreExec = exec.createSilentSubProgress(0.2);
        ExecutionMonitor loadIntTblsExec = exec.createSilentSubProgress(0.1);
        ExecutionMonitor createExec = exec.createSilentSubProgress(0.1);
        exec.setMessage("settings");
        m_portObjects = new PortObject[node.getNrOutPorts()];
        m_portObjectSpecs = new PortObjectSpec[node.getNrOutPorts()];
        m_portObjectSummaries = new String[node.getNrOutPorts()];
        String nodeName = node.getName();

        // load internals
        if (m_hasContent) {
            try {
                m_nodeInternDirectory = loadNodeInternDirectory(m_settings, getNodeDirectory());
            } catch (InvalidSettingsException ise) {
                String e = "Unable to load internals directory";
                loadResult.addError(e);
                getLogger().warn(e, ise);
                setDirtyAfterLoad();
            }
        }
        WorkflowLoadHelper loadHelper = getLoadHelper();

        try {
            if (!loadHelper.isTemplateFlow()) {
                m_fileStoreHandler =
                    loadFileStoreHandler(node, loadFileStoreExec, m_settings, fileStoreHandlerRepository);
            }
        } catch (Exception e) {
            if (!(e instanceof InvalidSettingsException) && !(e instanceof IOException)) {
                getLogger().error("Unexpected \"" + e.getClass().getSimpleName() + "\" encountered");
            }
            String err = "Unable to load file store handler for node \"" + nodeName + "\": " + e.getMessage();
            loadResult.addError(err, true);
            if (mustWarnOnDataLoadError()) {
                getLogger().warn(err, e);
            } else {
                getLogger().debug(err);
            }
            setNeedsResetAfterLoad();
        }
        loadFileStoreExec.setProgress(1.0);
        exec.setMessage("ports");
        try {
            if (!loadHelper.isTemplateFlow()) {
                loadPorts(node, loadExec, m_settings, loadTblRep, tblRep, fileStoreHandlerRepository);
            }
        } catch (Exception e) {
            if (!(e instanceof InvalidSettingsException) && !(e instanceof IOException)) {
                getLogger().error("Unexpected \"" + e.getClass().getSimpleName() + "\" encountered");
            }
            String err = "Unable to load port content for node \"" + nodeName + "\": " + e.getMessage();
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
            if (!loadHelper.isTemplateFlow()) {
                if (getLoadVersion().isOlderThan(LoadVersion.V2100)) {
                    loadInternalHeldTablesPre210(node, loadIntTblsExec,
                        m_settings, loadTblRep, tblRep, fileStoreHandlerRepository);
                } else {
                    loadInternalHeldObjects(node, loadIntTblsExec,
                        m_settings, loadTblRep, tblRep, fileStoreHandlerRepository);
                }
            }
        } catch (Exception e) {
            if (!(e instanceof InvalidSettingsException) && !(e instanceof IOException)) {
                getLogger().error("Unexpected \"" + e.getClass().getSimpleName() + "\" encountered");
            }
            String err = "Unable to load internally held tables for node \"" + nodeName + "\": " + e.getMessage();
            loadResult.addError(err, true);
            if (mustWarnOnDataLoadError()) {
                getLogger().warn(err, e);
            } else {
                getLogger().debug(err);
            }
            setNeedsResetAfterLoad();
        }
        loadIntTblsExec.setProgress(1.0);
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

    protected final ReferencedFile getNodeDirectory() {
        return m_nncPersistor.getNodeContainerDirectory();
    }

    /** {@inheritDoc} */
    @Override
    public ReferencedFile getNodeInternDirectory() {
        return m_nodeInternDirectory;
    }

    /** {@inheritDoc} */
    @Override
    public String getWarningMessage() {
        return getNativeNodeContainerPersistor().getNodeMessage();
    }

    /** {@inheritDoc} */
    @Override
    public PortObject getPortObject(final int outportIndex) {
        if (outportIndex == 0) {
            if (!m_nncPersistor.hasConfiguredState()) {
                return null;
            } else if (m_isInactive) {
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
    void setPortObject(final int idx, final PortObject portObject) {
        checkPortIndexOnSet(idx);
        m_portObjects[idx] = portObject;
    }

    private void checkPortIndexOnSet(final int index) {
        CheckUtils.checkState(index > 0, "Must not set content of port 0; it's the framework port: " + index);
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec getPortObjectSpec(final int outportIndex) {
        if (outportIndex == 0) {
            if (!m_nncPersistor.hasConfiguredState()) {
                return null;
            } else if (m_isInactive) {
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
    void setPortObjectSpec(final int idx, final PortObjectSpec portObjectSpec) {
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
    void setPortObjectSummary(final int idx, final String portObjectSummary) {
        checkPortIndexOnSet(idx);
        m_portObjectSummaries[idx] = portObjectSummary;
    }

    /** {@inheritDoc} */
    @Override
    public PortObject[] getInternalHeldPortObjects() {
        return m_internalHeldObjects;
    }

    /**
     * @param internalHeldObjects the internalHeldTables to set
     */
    void setInternalHeldPortObjects(final PortObject[] internalHeldObjects) {
        m_internalHeldObjects = internalHeldObjects;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.6
     */
    @Override
    public IFileStoreHandler getFileStoreHandler() {
        return m_fileStoreHandler;
    }

    /**
     * Saves the node, node settings, and all internal structures, spec, data, and models, to the given node directory
     * (located at the node file).

     * @param nnc ...
     * @param settings ...
     * @param execMon Used to report progress during saving.
     * @param nodeDirRef Directory associated with node - will create internals folder in it
     * @param isSaveData  ...
     * @throws IOException If the node file can't be found or read.
     * @throws CanceledExecutionException If the saving has been canceled.
     * @since 2.9
     */
    public static void save(final NativeNodeContainer nnc, final NodeSettingsWO settings,
        final ExecutionMonitor execMon, final ReferencedFile nodeDirRef,
        final boolean isSaveData) throws IOException, CanceledExecutionException {
        final Node node = nnc.getNode();

        saveCustomName(node, settings);
        saveHasContent(node, settings);
        saveIsInactive(node, settings);
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
            isSaveInternals = node.isInactiveBranchConsumer() || !node.isInactive();
        }
        if (isSaveInternals) {
            NodeContext.pushContext(nnc);
            try {
                saveNodeInternDirectory(node, nodeInternDir, settings, internalMon);
            } finally {
                NodeContext.removeLastContext();
            }
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
        execMon.setProgress(1.0);
    }

    private static void savePorts(final Node node, final ReferencedFile nodeDirRef, final NodeSettingsWO settings,
        final Set<Integer> savedTableIDs, final ExecutionMonitor exec, final boolean saveData) throws IOException,
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
            ExecutionMonitor subProgress = exec.createSubProgress(1.0 / portCount);
            NodeSettingsWO singlePortSetting = portSettings.addNodeSettings(portName);
            singlePortSetting.addInt("index", i);
            PortObject object = node.getOutputObject(i);
            String portDirName;
            if (object != null && saveData) {
                portDirName = portName;
                ReferencedFile portDirRef = new ReferencedFile(nodeDirRef, portDirName);
                File portDir = portDirRef.getFile();
                subProgress.setMessage("Cleaning directory " + portDir.getAbsolutePath());
                FileUtil.deleteRecursively(portDir);
                if (!portDir.mkdir() && !portDir.isDirectory()) {
                    throw new IOException("Cannot create port directory " + portDir.getAbsolutePath() + " ("
                        + "exists: " + portDir.exists() + ", isDir: " + portDir.isDirectory() + ", "
                        + "parent permissions: " + (portDir.getParentFile().canRead() ? "r" : "-")
                        + (portDir.getParentFile().canWrite() ? "w" : "-")
                        + (portDir.getParentFile().canExecute() ? "x" : "-") + ")");
                }
                if (!portDir.canWrite()) {
                    throw new IOException("Cannot write to port directory " + portDir.getAbsolutePath());
                }
                savePort(node, portDir, singlePortSetting, savedTableIDs, subProgress, i, saveData);
            } else {
                portDirName = null;
            }
            singlePortSetting.addString("port_dir_location", portDirName);
            subProgress.setProgress(1.0);
        }
    }

    private static void saveInternalHeldTables(final Node node, final ReferencedFile nodeDirRef,
        final NodeSettingsWO settings, final Set<Integer> savedTableIDs, final ExecutionMonitor exec,
        final boolean saveData) throws IOException, CanceledExecutionException {
        PortObject[] internalObjects = node.getInternalHeldPortObjects();
        if (internalObjects == null || !saveData) {
            return;
        }
        final int internalTblsCount = internalObjects.length;
        NodeSettingsWO subSettings = settings.addNodeSettings("internalObjects");
        String subDirName = INTERNAL_TABLE_FOLDER_PREFIX;
        ReferencedFile subDirFile = new ReferencedFile(nodeDirRef, subDirName);
        subSettings.addString("location", subDirName);
        NodeSettingsWO portSettings = subSettings.addNodeSettings("content");
        FileUtil.deleteRecursively(subDirFile.getFile());
        subDirFile.getFile().mkdirs();

        exec.setMessage("Saving internally held objects");
        for (int i = 0; i < internalTblsCount; i++) {
            PortObject t = internalObjects[i];
            String objName = "object_" + i;
            ExecutionMonitor subProgress = exec.createSubProgress(1.0 / internalTblsCount);
            NodeSettingsWO singlePortSetting = portSettings.addNodeSettings(objName);
            ReferencedFile portDirRef = new ReferencedFile(subDirFile, objName);
            File portDir = portDirRef.getFile();
            singlePortSetting.addInt("index", i);
            if (t == null) {
                singlePortSetting.addString("type", "null");
            } else if (t instanceof BufferedDataTable) {
                BufferedDataTable table = (BufferedDataTable)t;
                saveBufferedDataTable(table, savedTableIDs, createDirectory(portDir), exec);
                singlePortSetting.addString("type", "table");
                singlePortSetting.addString("table_dir_location", objName);
            } else {
                // other type of port object - object might be in output
                int outputPortIndex = IntStream.range(0, node.getNrOutPorts())
                        .filter(p -> node.getOutputObject(p) == t).findFirst().orElse(-1);
                if (outputPortIndex >= 0) {
                    singlePortSetting.addString("type", "referenced_output");
                    singlePortSetting.addInt("outport", outputPortIndex);
                } else {
                    singlePortSetting.addString("type", "non-table");
                    singlePortSetting.addString("port_dir_location", objName);
                    savePortObject(t.getSpec(), t, createDirectory(portDir), singlePortSetting, exec);
                }
            }
            subProgress.setProgress(1.0);
        }
    }

    /** Check if argument is a directory and can be read, otherwise throws exception. */
    private static File readDirectory(final File dir) throws IOException {
        if (!(dir.isDirectory() && dir.canRead())) {
            throw new IOException("Cannot read directory " + dir.getAbsolutePath());
        }
        return dir;
    }

    /** Create a directory, make sure it exists and return it. */
    private static File createDirectory(final File portDir) throws IOException {
        portDir.mkdir();
        if (!portDir.isDirectory() || !portDir.canWrite()) {
            throw new IOException("Cannot write directory " + portDir.getAbsolutePath());
        }
        return portDir;
    }

    private static void savePort(final Node node, final File portDir, final NodeSettingsWO settings,
        final Set<Integer> savedTableIDs, final ExecutionMonitor exec, final int portIdx, final boolean saveData)
        throws IOException, CanceledExecutionException {
        PortObjectSpec spec = node.getOutputSpec(portIdx);
        PortObject object = node.getOutputObject(portIdx);
        String summary = node.getOutputObjectSummary(portIdx);

        settings.addString("port_spec_class", spec != null ? spec.getClass().getName() : null);
        boolean isSaveObject = saveData && object != null;
        settings.addString("port_object_class", isSaveObject ? object.getClass().getName() : null);
        if (saveData && object != null) {
            settings.addString("port_object_summary", summary);
        }
        boolean isBDT =
                object instanceof BufferedDataTable || node.getOutputType(portIdx).equals(BufferedDataTable.TYPE);
        boolean isInactive = spec instanceof InactiveBranchPortObjectSpec;
        if (isBDT && !isInactive) {
            assert object == null || object instanceof BufferedDataTable : "Expected BufferedDataTable, got "
                + object.getClass().getSimpleName();
            // executed and instructed to save data
            if (saveData && object != null) {
                saveBufferedDataTable((BufferedDataTable)object, savedTableIDs, portDir, exec);
            }
        } else {
            if (isSaveObject) {
                exec.setMessage("Saving object");
                assert spec != null : "Spec is null but port object is non-null (port " + portIdx + " of node "
                        + node.getName() + ")";
                savePortObject(spec, object, portDir, settings, exec);
            }
        }
    }

    private static void savePortObject(final PortObjectSpec spec, final PortObject object,
        final File portDir, final NodeSettingsWO settings, final ExecutionMonitor exec)
                throws IOException, FileNotFoundException, CanceledExecutionException {
        settings.addString("port_spec_class", spec.getClass().getName());
        settings.addString("port_object_class", object.getClass().getName());
        String specDirName = "spec";
        String specFileName = "spec.zip";
        String specPath = specDirName + "/" + specFileName;
        File specDir = createDirectory(new File(portDir, specDirName));

        File specFile = new File(specDir, specFileName);
        settings.addString("port_spec_location", specPath);
        try (PortObjectSpecZipOutputStream out = PortUtil.getPortObjectSpecZipOutputStream(
            new BufferedOutputStream(new FileOutputStream(specFile)))) {
            PortObjectSpecSerializer serializer =
                    PortTypeRegistry.getInstance().getSpecSerializer(spec.getClass()).get();
            serializer.savePortObjectSpec(spec, out);
        }

        String objectDirName = null;
        objectDirName = "object";
        File objectDir = createDirectory(new File(portDir, objectDirName));
        String objectPath;
        String objectFileName = "portobject.zip";

        objectPath = objectDirName + "/" + objectFileName;
        settings.addString("port_object_location", objectPath);
        File file = new File(objectDir, objectFileName);
        try (PortObjectZipOutputStream out =
            PortUtil.getPortObjectZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            PortObjectSerializer serializer =
                    PortTypeRegistry.getInstance().getObjectSerializer(object.getClass()).get();
            serializer.savePortObject(object, out, exec);
            if (object instanceof FileStorePortObject) {
                List<FileStoreKey> fileStoreKeys = FileStoreUtil.translateToLocal((FileStorePortObject)object);
                File fileStoreXML = new File(objectDir, "filestore.xml");
                final ModelContent fileStoreModelContent = new ModelContent("filestore");
                ModelContentWO keysContent = fileStoreModelContent.addModelContent("filestore_keys");
                for (int i = 0; i < fileStoreKeys.size(); i++) {
                    FileStoreKey key = fileStoreKeys.get(i);
                    ModelContentWO keyContent = keysContent.addModelContent("filestore_key_" + i);
                    key.save(keyContent);
                }
                fileStoreModelContent.saveToXML(new FileOutputStream(fileStoreXML));
            }
        }
    }

    /**
     * @param node
     * @param nodeDirRef
     * @param settings
     * @param fileStoreMon
     * @param isSaveData
     * @throws IOException
     */
    private static void saveFileStoreObjects(final Node node, final ReferencedFile nodeDirRef,
        final NodeSettingsWO settings, final ExecutionMonitor fileStoreMon, final boolean isSaveData) throws IOException {
        NodeSettingsWO fsSettings = settings.addNodeSettings("filestores");
        IFileStoreHandler fileStoreHandler = node.getFileStoreHandler();
        String uuidS;
        String dirNameInFlow;
        if (isSaveData && fileStoreHandler instanceof WriteFileStoreHandler) {
            final WriteFileStoreHandler defFileStoreHandler = (WriteFileStoreHandler)fileStoreHandler;
            File baseDir = defFileStoreHandler.getBaseDir();
            dirNameInFlow = baseDir == null ? null : FILESTORE_FOLDER_PREFIX;
            if (dirNameInFlow != null) {
                File saveLocation = new File(nodeDirRef.getFile(), dirNameInFlow);
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

    private static void saveBufferedDataTable(final BufferedDataTable table, final Set<Integer> savedTableIDs,
        final File directory, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
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

    /**
     * Sub-class hook to save location of internal directory.
     *
     * @param node Node
     * @param nodeInternDir Directory
     * @param settings Ignored (possibly not in sub-classes)
     * @param exec exec mon.
     * @throws CanceledExecutionException If canceled.
     */
    static void saveNodeInternDirectory(final Node node, final File nodeInternDir, final NodeSettingsWO settings,
        final ExecutionMonitor exec) throws CanceledExecutionException {
        node.saveInternals(nodeInternDir, exec);
    }

    static void saveCustomName(final Node node, final NodeSettingsWO settings) {
        settings.addString(CFG_NAME, node.getName());
    }

}
