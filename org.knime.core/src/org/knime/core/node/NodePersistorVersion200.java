/*
 * ------------------------------------------------------------------ *
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec.PortObjectSpecSerializer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion200;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.util.FileUtil;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class NodePersistorVersion200 extends NodePersistorVersion1xx {
    
    /** Prefix of associated port folders.
     * (Also used in export wizard, public declaration here.) */
    public static final String PORT_FOLDER_PREFIX = "port_";

    /** Prefix of associated port folders.
     * (Also used in export wizard, public declaration here.) */
    public static final String INTERNAL_TABLE_FOLDER_PREFIX = "internalTables";
    
    /** Invokes super constructor. 
     * @param modelSettingsFailPolicy Forwared.*/
    public NodePersistorVersion200(
            final SingleNodeContainerPersistorVersion200 sncPersistor) {
        super(sncPersistor);
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
    public void save(final Node node, final ReferencedFile nodeFile,
            final ExecutionMonitor execMon, final boolean isSaveData)
            throws IOException, CanceledExecutionException {
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
        saveNodeMessage(node, settings);
        ReferencedFile nodeInternDirRef = getNodeInternDirectory(nodeDirRef);
        File nodeInternDir = nodeInternDirRef.getFile();
        if (nodeInternDir.exists()) {
            FileUtil.deleteRecursively(nodeInternDir);
        }
        ExecutionMonitor internalMon = execMon.createSilentSubProgress(0.2);
        ExecutionMonitor portMon = execMon.createSilentSubProgress(0.6);
        ExecutionMonitor intTblsMon = execMon.createSilentSubProgress(0.1);
        execMon.setMessage("Internals");
        if (isSaveData) {
            saveNodeInternDirectory(node, nodeInternDir, settings, internalMon);
        }
        internalMon.setProgress(1.0);
        execMon.setMessage("Ports");
        savePorts(node, nodeDirRef, settings, portMon, isSaveData);
        portMon.setProgress(1.0);
        execMon.setMessage("Internal Tables");
        saveInternalHeldTables(node, nodeDirRef, settings, portMon, isSaveData);
        intTblsMon.setProgress(1.0);
        settings.saveToXML(new BufferedOutputStream(new FileOutputStream(
                nodeFile.getFile())));
        execMon.setProgress(1.0);
    }

    protected void savePorts(final Node node, final ReferencedFile nodeDirRef,
            final NodeSettingsWO settings, final ExecutionMonitor exec,
            final boolean saveData) throws IOException,
            CanceledExecutionException {
        if (node.getNrOutPorts() == 0) {
            return;
        }
        final int portCount = node.getNrOutPorts();
        NodeSettingsWO portSettings = settings.addNodeSettings("ports");
        exec.setMessage("Saving outport data");
        for (int i = 0; i < portCount; i++) {
            String portName = PORT_FOLDER_PREFIX + i;
            ExecutionMonitor subProgress =
                    exec.createSubProgress(1.0 / portCount);
            NodeSettingsWO singlePortSetting =
                    portSettings.addNodeSettings(portName);
            singlePortSetting.addInt("index", i);
            PortObjectSpec spec = node.getOutputSpec(i);
            PortObject object = node.getOutputObject(i);
            String portDirName;
            if (spec != null || object != null) {
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
                savePort(node, portDir, singlePortSetting, subProgress, i,
                        saveData);
            } else {
                portDirName = null;
            }
            singlePortSetting.addString("port_dir_location", portDirName);
            subProgress.setProgress(1.0);
        }
    }

    protected void saveInternalHeldTables(final Node node,
            final ReferencedFile nodeDirRef, final NodeSettingsWO settings,
            final ExecutionMonitor exec, final boolean saveData)
            throws IOException, CanceledExecutionException {
        BufferedDataTable[] internalTbls = node.getInternalHeldTables();
        if (internalTbls == null) {
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
                t.save(portDir, exec);
            } else {
                tblDirName = null;
            }
            singlePortSetting.addString("table_dir_location", tblDirName);
            subProgress.setProgress(1.0);
        }
    }

    protected void savePort(final Node node, final File portDir,
            final NodeSettingsWO settings, final ExecutionMonitor exec,
            final int portIdx, final boolean saveData) throws IOException,
            CanceledExecutionException {
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
        if (node.getOutputType(portIdx).equals(BufferedDataTable.TYPE)) {
            assert object == null || object instanceof BufferedDataTable 
                : "Expected BufferedDataTable, got " 
                    + object.getClass().getSimpleName();
            // executed and instructed to save data
            if (saveData && object != null) {
                ((BufferedDataTable)object).save(portDir, exec);
            } else if (spec != null) {
                BufferedDataTable.saveSpec((DataTableSpec)spec, portDir);
            }
        } else {
            exec.setMessage("Saving specification");
            if (spec != null) {
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
            if (isSaveObject) {
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
                    saveBufferedDataTable((BufferedDataTable)object, objectDir,
                            exec);
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

    private void saveBufferedDataTable(final BufferedDataTable table,
            final File directory, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        table.save(directory, exec);
    }

    protected void saveHasContent(final Node node, final NodeSettingsWO settings) {
        boolean hasContent = node.hasContent();
        settings.addBoolean("hasContent", hasContent);
    }

    protected void saveNodeMessage(final Node node,
            final NodeSettingsWO settings) {
        NodeMessage message = node.getNodeMessage();
        if (message != null) {
            NodeSettingsWO sub = settings.addNodeSettings("node_message");
            sub.addString("type", message.getMessageType().name());
            sub.addString("message", message.getMessage());
        }
    }

    protected void saveNodeInternDirectory(final Node node,
            final File nodeInternDir, final NodeSettingsWO settings,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        node.saveInternals(nodeInternDir, exec);
    }

    protected void saveCustomName(final Node node, final NodeSettingsWO settings) {
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
    protected boolean shouldLoadAsNotExecuted(Node node) {
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    public LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy() {
        LoadNodeModelSettingsFailPolicy result = 
            getSingleNodeContainerPersistor().getModelSettingsFailPolicy();
        assert result != null : "fail policy is null";
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean loadHasContent(NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getBoolean("hasContent");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean loadIsConfigured(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeMessage loadNodeMessage(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.containsKey("node_message")) {
            NodeSettingsRO sub = settings.getNodeSettings("node_message");
            String typeS = sub.getString("type");
            if (typeS == null) {
                throw new InvalidSettingsException(
                        "Message type must not be null");
            }
            Type type;
            try {
                type = Type.valueOf(typeS);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid message type: "
                        + typeS, iae);
            }
            String message = sub.getString("message");
            return new NodeMessage(type, message);
        }
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void loadInternalHeldTables(final Node node, ExecutionMonitor execMon,
            NodeSettingsRO settings,
            Map<Integer, BufferedDataTable> loadTblRep,
            HashMap<Integer, ContainerTable> tblRep) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        if (!settings.containsKey("internalTables")) {
            return;
        }
        NodeSettingsRO subSettings = settings.getNodeSettings("internalTables");
        String subDirName = subSettings.getString("location");
        ReferencedFile subDirFile = 
            new ReferencedFile(getNodeDirectory(), subDirName);
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
                BufferedDataTable t = loadBufferedDataTable(
                        portDirRef, subProgress, loadTblRep, tblRep);
                result[index] = t;
            }
            subProgress.setProgress(1.0);
        }
        setInternalHeldTables(result);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadPorts(final Node node, final ExecutionMonitor exec,
            final NodeSettingsRO settings,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        if (node.getNrOutPorts() == 0) {
            return;
        }
        final int portCount = node.getNrOutPorts();
        NodeSettingsRO portSettings = settings.getNodeSettings("ports");
        exec.setMessage("Reading outport data");
        for (String key : portSettings.keySet()) {
            NodeSettingsRO singlePortSetting =
                    portSettings.getNodeSettings(key);
            ExecutionMonitor subProgress =
                    exec.createSubProgress(1 / (double)portCount);
            int index = singlePortSetting.getInt("index");
            if (index < 0 || index >= node.getNrOutPorts()) {
                throw new InvalidSettingsException(
                        "Invalid outport index in settings: " + index);
            }
            String portDirN = singlePortSetting.getString("port_dir_location");
            if (portDirN != null) {
                ReferencedFile portDir =
                        new ReferencedFile(getNodeDirectory(), portDirN);
                subProgress.setMessage("Port " + index);
                loadPort(node, portDir, singlePortSetting, subProgress, index,
                        loadTblRep, tblRep);
            }
            subProgress.setProgress(1.0);
        }
    }

    protected void loadPort(final Node node, final ReferencedFile portDir,
            final NodeSettingsRO settings, final ExecutionMonitor exec,
            final int portIdx,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        String specClass = settings.getString("port_spec_class");
        String objectClass = settings.getString("port_object_class");
        PortType designatedType = node.getOutputType(portIdx);
        PortObjectSpec spec = null;
        PortObject object = null;
        if (node.getOutputType(portIdx).equals(BufferedDataTable.TYPE)) {
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
                object = loadBufferedDataTable(
                        portDir, exec, loadTblRep, tblRep);
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
                    cl = GlobalClassCreator.createClass(specClass);
                } catch (ClassNotFoundException e) {
                    throw new IOException("Can't load class \"" + specClass
                            + "\"", e);
                }
                if (!PortObjectSpec.class.isAssignableFrom(cl)) {
                    throw new IOException("Class \"" + cl.getSimpleName()
                            + "\" does not a sub-class \""
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
                    cl = GlobalClassCreator.createClass(objectClass);
                } catch (ClassNotFoundException e) {
                    throw new IOException("Can't load port object class \""
                            + objectClass + "\"", e);
                }
                if (!PortObject.class.isAssignableFrom(cl)) {
                    throw new IOException("Class \"" + cl.getSimpleName()
                            + "\" does not a sub-class \""
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
                                    loadTblRep, tblRep);
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
            if (!designatedType.getPortObjectSpecClass().isInstance(spec)) {
                throw new IOException("Actual port spec type (\""
                        + spec.getClass().getSimpleName()
                        + "\") does not match designated one (\""
                        + designatedType.getPortObjectSpecClass()
                                .getSimpleName() + "\")");
            }
        }
        String summary = null;
        if (object != null) {
            if (!designatedType.getPortObjectClass().isInstance(object)) {
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

    private BufferedDataTable loadBufferedDataTable(
            final ReferencedFile objectDir, final ExecutionMonitor exec,
            final Map<Integer, BufferedDataTable> loadTblRep,
            final HashMap<Integer, ContainerTable> tblRep)
            throws CanceledExecutionException, IOException,
            InvalidSettingsException {
        return BufferedDataTable.loadFromFile(objectDir, /* ignored in 1.2+ */
        null, exec, loadTblRep, tblRep);
    }

}
