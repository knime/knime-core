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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 27, 2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.WorkflowFileStoreHandlerRepository;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class ObsoleteMetaNodeWorkflowPersistorVersion1xx extends
        WorkflowPersistorVersion1xx {

    private enum MetaNodeType {
        ORDINARY,
        CROSSVALIDATION,
        LOOPER
    };

    public static final List<String> OLD_META_NODES =
        Collections.unmodifiableList(Arrays.asList(new String[]{
            "org.knime.base.node.meta.MetaNodeFactory01",
            "org.knime.base.node.meta.MetaNodeFactory11",
            "org.knime.base.node.meta.MetaNodeFactory21",
            "org.knime.base.node.meta.MetaNodeFactory12",
            "org.knime.base.node.meta.MetaNodeFactory22",
            "org.knime.base.node.meta.xvalidation.XValidateNodeFactory",
            "org.knime.base.node.meta.looper.LooperFactory",
            "de.unikn.knime.core.node.meta.MetaNodeFactory01",
            "de.unikn.knime.core.node.meta.MetaNodeFactory11",
            "de.unikn.knime.core.node.meta.MetaNodeFactory21",
            "de.unikn.knime.core.node.meta.MetaNodeFactory12",
            "de.unikn.knime.core.node.meta.MetaNodeFactory22",
            "de.unikn.knime.dev.node.xvalidation.XValidateNodeFactory",
            "de.unikn.knime.dev.node.looper.LooperFactory"
        }));

    private int[] m_dataInNodeIDs = new int[0];
    private int[] m_dataOutNodeIDs = new int[0];

    private MetaNodeType m_metaNodeType = MetaNodeType.ORDINARY;

    ObsoleteMetaNodeWorkflowPersistorVersion1xx(
            final HashMap<Integer, ContainerTable> globalRep,
            final WorkflowFileStoreHandlerRepository fileStoreHandlerRepository,
            final ReferencedFile workflowKNIMEFile,
            final WorkflowLoadHelper loadHelper,
            final LoadVersion version) {
        super(globalRep, fileStoreHandlerRepository,
                workflowKNIMEFile, loadHelper, version, false);
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor,
            final NodeSettingsRO parentSettings, final LoadResult result)
    throws IOException, InvalidSettingsException {
        ReferencedFile workflowKNIMEFile = super.getWorkflowKNIMEFile();
        File setFile = workflowKNIMEFile.getFile();
        if (!setFile.getName().equals("settings.xml")) {
            String warn = "Settings file of obsolete meta node is not "
                    + "named settings.xml: " + setFile.getName();
            getLogger().warn(warn);
            result.addError(warn);
        }
        ReferencedFile parent = workflowKNIMEFile.getParent();
        if (parent == null) {
            throw new IOException("Parent directory not represented by class "
                    + ReferencedFile.class);
        }
        ReferencedFile workflowKnimeRef =
            new ReferencedFile(parent, "workflow.knime");
        File workflowKnime = workflowKnimeRef.getFile();
        if (!workflowKnime.isFile()) {
            throw new IOException(
                    "Can't find file " + workflowKnime.getAbsolutePath());
        }
        String factory = parentSettings.getString("factory");
        if ("org.knime.base.node.meta.xvalidation.XValidateNodeFactory".equals(
                factory)) {
            m_metaNodeType = MetaNodeType.CROSSVALIDATION;
        } else if ("org.knime.base.node.meta.looper.LooperFactory".equals(
                factory)) {
            m_metaNodeType = MetaNodeType.LOOPER;
        } else {
            m_metaNodeType = MetaNodeType.ORDINARY;
        }
        NodeSettingsRO settings = NodeSettings.loadFromXML(
                new BufferedInputStream(new FileInputStream(setFile)));
        NodeSettingsRO modelSet = settings.getNodeSettings("model");
        m_dataInNodeIDs = modelSet.getIntArray("dataInContainerIDs");
        m_dataOutNodeIDs = modelSet.getIntArray("dataOutContainerIDs");
        super.preLoadNodeContainer(parentPersistor, parentSettings, result);
        String name = "Looper";
        switch (m_metaNodeType) {
        case CROSSVALIDATION:
            name = "Cross Validation";
        case LOOPER:
            result.addError("Workflow contains obsolete \"" + name
                    + "\" meta node implementation, not all settings could "
                    + "be restored, please re-configure and execute again.");
            setNeedsResetAfterLoad();
        default:
        }
    }

    /** {@inheritDoc} */
    @Override
    protected ReferencedFile getWorkflowKNIMEFile() {
        // config file as passed from parent wfm is "settings.xml"
        ReferencedFile settingsXML = super.getWorkflowKNIMEFile();
        return new ReferencedFile(settingsXML.getParent(),
                WorkflowPersistor.WORKFLOW_FILE);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean shouldSkipThisNode(final NodeSettingsRO settings) {
        switch (m_metaNodeType) {
        case LOOPER:
            return false;
        default:
        }
        try {
            int idx = loadNodeIDSuffix(settings);
            return doesAnyArrayContain(idx, m_dataInNodeIDs, m_dataOutNodeIDs);
        } catch (InvalidSettingsException e) {
            return false; // will fail at later stage
        }
    }

    /** {@inheritDoc} */
    @Override
    protected SingleNodeContainerPersistorVersion1xx
        createSingleNodeContainerPersistorLoad(final ReferencedFile nodeFile) {
        return new ObsoleteSpecialNodeSingleNodeContainerPersistorVersion1xx(
                this, nodeFile, getLoadHelper(), getLoadVersion());
    }

    /** {@inheritDoc} */
    @Override
    protected String loadWorkflowName(final NodeSettingsRO set)
            throws InvalidSettingsException {
        switch (m_metaNodeType) {
        case LOOPER:
            return "Looper";
        case CROSSVALIDATION:
            return "Cross Validation";
        default:
            StringBuilder b = new StringBuilder("Meta Node ");
            b.append(m_dataInNodeIDs.length);
            b.append(" : ");
            b.append(m_dataOutNodeIDs.length);
            return b.toString();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO readParentSettings() throws IOException {
        File f = getWorkflowKNIMEFile().getParent().getFile();
        File oldSettingsFile = new File(f, "settings.xml");
        if (!oldSettingsFile.isFile()) {
            throw new IOException("No such settings file: "
                    + oldSettingsFile.getAbsolutePath());
        }
        return NodeSettings.loadFromXML(new BufferedInputStream(
                new FileInputStream(oldSettingsFile)));
    }

    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadOutPortsSetting(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
        NodeSettings template = new NodeSettings("outports");
        for (int i = 0; i < m_dataOutNodeIDs.length; i++) {
            NodeSettingsWO sub = template.addNodeSettings("data_" + i);
            sub.addInt("index", i);
            sub.addInt("node", m_dataOutNodeIDs[i]);
            sub.addString("type", "data");
        }
        return template;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadOutPortsSettingsEnum(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings;
    }

    /** {@inheritDoc} */
    @Override
    protected WorkflowPortTemplate loadOutPortTemplate(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        int index = settings.getInt("index");
        String stype = settings.getString("type");
        PortType type;
        if ("data".equals(stype)) {
            type = BufferedDataTable.TYPE;
        } else if ("model".equals(stype)) {
            throw new InvalidSettingsException("1.x model ports no longer "
                    + "supported: " + stype);
        } else {
            throw new InvalidSettingsException("Unknown port: " + stype);
        }
        return new WorkflowPortTemplate(index, type);
    }

    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadInPortsSetting(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        NodeSettings template = new NodeSettings("inports");
        for (int i = 0; i < m_dataInNodeIDs.length; i++) {
            NodeSettingsWO sub = template.addNodeSettings("data_" + i);
            sub.addInt("index", i);
            sub.addInt("node", m_dataInNodeIDs[i]);
            sub.addString("type", "data");
        }
        return template;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadInPortsSettingsEnum(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings;
    }

    /** {@inheritDoc} */
    @Override
    protected WorkflowPortTemplate loadInPortTemplate(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return loadOutPortTemplate(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadSettingsForConnections(
            final NodeSettingsRO set) throws InvalidSettingsException {
        NodeSettingsRO original = super.loadSettingsForConnections(set);
        if (!m_metaNodeType.equals(MetaNodeType.LOOPER)) {
            return original;
        }
        // looper nodes have two more connections (meta-in and out), faking them
        NodeSettings fake = new NodeSettings(original.getKey());
        original.copyTo(fake);
        String wfmInKey;
        int i = 0;
        do {
            wfmInKey = "workflow_connection" + i;
        } while (fake.containsKey(wfmInKey));
        NodeSettingsWO wfmIn = fake.addNodeSettings(wfmInKey);
        // ID is not read anyway...
        wfmIn.addInt("ID", Integer.MAX_VALUE - 1);
        wfmIn.addInt("sourceID", -1);
        wfmIn.addInt("sourcePort", 0);
        wfmIn.addInt("targetID", m_dataInNodeIDs[0]);
        wfmIn.addInt("targetPort", 0);
        String wfmOutKey;
        do {
            wfmOutKey = "workflow_connection" + i++;
        } while (fake.containsKey(wfmOutKey));
        NodeSettingsWO wfmOut = fake.addNodeSettings(wfmOutKey);
        // ID is not read ...
        wfmOut.addInt("ID", Integer.MAX_VALUE - 2);
        wfmOut.addInt("sourceID", m_dataOutNodeIDs[0]);
        wfmOut.addInt("sourcePort", 0);
        wfmOut.addInt("targetID", -1);
        wfmOut.addInt("targetPort", 0);
        return fake;
    }

    /** {@inheritDoc} */
    @Override
    protected ConnectionContainerTemplate loadConnection(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // in the looper node the obsolete data-in and -out nodes are
        // replaced by the loop head and end nodes (which were not present
        // in 1.x.x)
        if (m_metaNodeType.equals(MetaNodeType.LOOPER)) {
            return super.loadConnection(settings);
        }
        int sourceID = settings.getInt("sourceID");
        int destID = loadConnectionDestID(settings);
        int sourcePort = settings.getInt("sourcePort");
        int destPort = loadConnectionDestPort(settings);
        if (doesAnyArrayContain(sourceID, m_dataInNodeIDs)) {
            int i = 0;
            for (; i < m_dataInNodeIDs.length; i++) {
                if (m_dataInNodeIDs[i] == sourceID) {
                    sourcePort = i;
                }
            }
            sourceID = -1;
        }
        if (doesAnyArrayContain(destID, m_dataOutNodeIDs)) {
            int i = 0;
            for (; i < m_dataOutNodeIDs.length; i++) {
                if (m_dataOutNodeIDs[i] == destID) {
                    destPort = i;
                }
            }
            destID = -1;
        }
        if (sourceID != -1 && sourceID == destID) {
            throw new InvalidSettingsException("Source and Destination must "
                    + "not be equal, id is " + sourceID);
        }
        ConnectionUIInformation connUIInfo = null;
        try {
            String uiInfoClass = loadUIInfoClassName(settings);
            UIInformation uiInfo = loadUIInfoInstance(uiInfoClass);
            if (uiInfo != null) {
                if (!(uiInfo instanceof ConnectionUIInformation)) {
                    getLogger().debug("Could not load UI information for "
                            + "connection between nodes " + sourceID + " and "
                            + destID + ": expected "
                            + ConnectionUIInformation.class.getName()
                            + " but got " + uiInfoClass.getClass().getName());
                } else {
                    loadUIInfoSettings(uiInfo, settings);
                    connUIInfo = (ConnectionUIInformation)uiInfo;
                }
            }
        } catch (InvalidSettingsException ise) {
            getLogger().debug("Could not load UI information for connection "
                    + "between nodes " + sourceID + " and " + destID);
        } catch (Throwable t) {
            getLogger().warn("Exception while loading connection UI "
                    + "information between nodes " + sourceID + " and "
            		+ destID, t);
        }
        return new ConnectionContainerTemplate(sourceID, sourcePort, destID,
                destPort, /*isDeletable*/true, connUIInfo);
    }

    private static boolean doesAnyArrayContain(final int value,
            final int[]... arrays) {
        for (int[] array : arrays) {
            for (int i : array) {
                if (i == value) {
                    return true;
                }
            }
        }
        return false;
    }

    private class ObsoleteSpecialNodeSingleNodeContainerPersistorVersion1xx
        extends SingleNodeContainerPersistorVersion1xx {

        /**
         *
         */
        public ObsoleteSpecialNodeSingleNodeContainerPersistorVersion1xx(
                final WorkflowPersistorVersion1xx workflowPersistor,
                final ReferencedFile nodeSettingsFile,
                final WorkflowLoadHelper loadHelper,
                final LoadVersion version) {
            super(workflowPersistor, nodeSettingsFile, loadHelper, version);
        }

        /** {@inheritDoc} */
        @Override
        NodeAndBundleInformation loadNodeFactoryInfo(
                final NodeSettingsRO parentSettings,
                final NodeSettingsRO settings) throws InvalidSettingsException {
            NodeAndBundleInformation f = super.loadNodeFactoryInfo(parentSettings, settings);
            switch (m_metaNodeType) {
            case LOOPER:
                String in = "org.knime.core.node.meta.DataInputNodeFactory";
                String out = "org.knime.core.node.meta.DataOutputNodeFactory";
                String newClass = f.getFactoryClass();
                if (in.equals(newClass)) {
                    newClass =
                        "org.knime.base.node.meta.looper.ForLoopHeadNodeFactory";
                } else if (out.equals(newClass)) {
                    newClass =
                        "org.knime.base.node.meta.looper.ForLoopTailNodeFactory";
                }
                f = new NodeAndBundleInformation(newClass, null, null, null, null);
                break;
            case CROSSVALIDATION:
                // the x-partitioner and aggregator node have the same name
                // in 1.x and 2.0 - which is good
            default:
            }
            return f;
        }

    }

}
