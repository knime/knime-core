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
 *   May 20, 2021 (hornm): created
 */
package org.knime.core.node.workflow;

import static org.knime.core.node.workflow.def.DefToCoreUtil.toNodeSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.ConfigPasswordEntry;
import org.knime.core.node.workflow.FlowLoopContext.RestoredFlowLoopContext;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.workflow.def.SingleNodeDef;

/**
 *
 * @author hornm
 */
public abstract class DefSingleNodeContainerPersistor implements SingleNodeContainerPersistor {

    private SingleNodeDef m_def;

    private SingleNodeContainerSettings m_sncSettings;

    private List<FlowObject> m_flowObjects;

    private DefNodeContainerMetaPersistor m_metaPersistor;

    /**
     * @param def
     */
    public DefSingleNodeContainerPersistor(final SingleNodeDef def) {
        m_def = def;
        m_metaPersistor = new DefNodeContainerMetaPersistor(m_def);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingleNodeContainerSettings getSNCSettings() {
        return m_sncSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FlowObject> getFlowObjects() {
        return m_flowObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec,
        final LoadResult result) throws InvalidSettingsException, CanceledExecutionException, IOException {
        // TODO duplicated code!!!
        m_sncSettings = new SingleNodeContainerSettings();
        exec.checkCanceled();
        try {
            m_sncSettings.setMemoryPolicy(loadMemoryPolicySettings(toNodeSettings(m_def.getInternalNodeSubSettings())));
        } catch (InvalidSettingsException e) {
            String error = "Unable to load SNC settings: " + e.getMessage();
            result.addError(error);
            //            getLogger().debug(error, e);
            //            setDirtyAfterLoad();
            return;
        }
        NodeSettingsRO modelSettings = null;
        //        try {
        modelSettings = toNodeSettings(m_def.getModelSettings());
        //        } catch (InvalidSettingsException ise) {
        //            String error = "Unable to load model settings: " + ise.getMessage();
        //            result.addError(error);
        //            getLogger().debug(error, ise);
        //            setDirtyAfterLoad();
        //        }
        if (Node.DISALLOW_WEAK_PASSWORDS_IN_NODE_CONFIGURATION && modelSettings != null
            && ConfigPasswordEntry.containsPassword((NodeSettings)modelSettings, false)) {
            result.addWarning(String.format(
                "Node stores passwords in its configuration. These will be lost when saving "
                    + "the workflow in this installation (as per \"%s\" system property)",
                KNIMEConstants.PROPERTY_WEAK_PASSWORDS_IN_SETTINGS_FORBIDDEN));
        }
        //        try {
        //            modelSettings = loadNCAndWashModelSettings(settingsForNode, modelSettings, tblRep, exec, result);
        //        } catch (InvalidSettingsException ise) {
        //            String error = "Unable to load node container and wash settings: " + ise.getMessage();
        //            result.addError(error);
        //            getLogger().debug(error, ise);
        //            setDirtyAfterLoad();
        //        }
        m_sncSettings.setModelSettings(modelSettings);
        //        try {
        m_sncSettings.setVariablesSettings(toNodeSettings(m_def.getVariableSettings()));
        //        } catch (InvalidSettingsException e) {
        //            String msg = "Could load variable settings: " + e.getMessage();
        //            result.addError(msg);
        //            setDirtyAfterLoad();
        //            setNeedsResetAfterLoad();
        //        }
        try {
            m_flowObjects = loadFlowObjects(toNodeSettings(m_def.getFlowStack()));
        } catch (Exception e) {
            //            m_flowObjects = Collections.emptyList();
            String error = "Error loading flow variables: " + e.getMessage();
            //            getLogger().warn(error, e);
            result.addError(error);
            //            setDirtyAfterLoad();
            //            setNeedsResetAfterLoad();
        }
        exec.setProgress(1.0);
    }

    private List<FlowObject> loadFlowObjects(final NodeSettingsRO flowStack) throws InvalidSettingsException {
        // TODO duplicated code!!!
        List<FlowObject> result = new ArrayList<>();
        for (String key : flowStack.keySet()) {
            NodeSettingsRO sub = flowStack.getNodeSettings(key);
            String type = sub.getString("type");
            if ("variable".equals(type)) {
                FlowVariable v = FlowVariable.load(sub);
                result.add(v);
            } else if ("loopcontext".equals(type)) {
                result.add(new RestoredFlowLoopContext());
                //                int tailID = sub.getInt("tailID");
            } else if ("loopcontext_execute".equals(type)) {
                result.add(new InnerFlowLoopContext());
            } else if ("loopcontext_inactive".equals(type)) {
                FlowLoopContext flc = new FlowLoopContext();
                flc.inactiveScope(true);
                result.add(flc);
            } else if ("flowcapturecontext".equals(type)) {
                result.add(new FlowCaptureContext());
            } else if ("flowcapturecontext_inactive".equals(type)) {
                FlowScopeContext slc = new FlowCaptureContext();
                slc.inactiveScope(true);
                result.add(slc);
            } else if ("scopecontext".equals(type)) {
                result.add(new FlowScopeContext());
            } else if ("scopecontext_inactive".equals(type)) {
                FlowScopeContext slc = new FlowScopeContext();
                slc.inactiveScope(true);
                result.add(slc);
            } else {
                throw new InvalidSettingsException("Unknown flow object type: " + type);
            }
        }
        return result;
    }

    private MemoryPolicy loadMemoryPolicySettings(final NodeSettingsRO internalNodeSubSettings)
        throws InvalidSettingsException {
        // TODO duplicated code!!!
        //        if (getLoadVersion().isOlderThan(LoadVersion.V210_Pre)) {
        //            // in versions before KNIME 1.2.0, there were no misc settings
        //            // in the dialog, we must use caution here: if they are not present
        //            // we use the default, i.e. small data are kept in memory
        //            if (nodeSettings.containsKey(Node.CFG_MISC_SETTINGS)
        //                && nodeSettings.getNodeSettings(Node.CFG_MISC_SETTINGS).containsKey(SingleNodeContainer.CFG_MEMORY_POLICY)) {
        //                NodeSettingsRO sub = nodeSettings.getNodeSettings(Node.CFG_MISC_SETTINGS);
        //                String memoryPolicy =
        //                    sub.getString(SingleNodeContainer.CFG_MEMORY_POLICY, MemoryPolicy.CacheSmallInMemory.toString());
        //                if (memoryPolicy == null) {
        //                    throw new InvalidSettingsException("Can't use null memory policy.");
        //                }
        //                try {
        //                    return MemoryPolicy.valueOf(memoryPolicy);
        //                } catch (IllegalArgumentException iae) {
        //                    throw new InvalidSettingsException("Invalid memory policy: " + memoryPolicy);
        //                }
        //            } else {
        //                return MemoryPolicy.CacheSmallInMemory;
        //            }
        //        } else {
        // any version after 2.0 saves the snc settings in the settings.xml
        // (previously these settings were saves as part of the node.xml)
        String memoryPolicy = internalNodeSubSettings.getString(SingleNodeContainer.CFG_MEMORY_POLICY,
            MemoryPolicy.CacheSmallInMemory.toString());
        if (memoryPolicy == null) {
            throw new InvalidSettingsException("Can't use null memory policy.");
        }
        try {
            return MemoryPolicy.valueOf(memoryPolicy);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Invalid memory policy: " + memoryPolicy);
        }
        //        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needsResetAfterLoad() {
        // TODO
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirtyAfterLoad() {
        // TODO
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return true;
    }

}
