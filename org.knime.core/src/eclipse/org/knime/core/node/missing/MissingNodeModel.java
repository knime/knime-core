/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * Created on Oct 26, 2012 by wiswedel
 */
package org.knime.core.node.missing;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;

/**
 * No API - Model to missing node placeholder node. It fails on configure and execute but provides data etc.
 * if the node was loaded in an executed state.
 * @author wiswedel
 */
public final class MissingNodeModel extends NodeModel {

    private NodeAndBundleInformation m_nodeAndBundleInformation;
    private NodeSettings m_settings;

    /** Copy the content of the internal directory to temp space in order to be save when the workflow is saved.
     * Note, loaded executed nodes are not saved unless there is a version hope in the workflow format.
     * (And this field is only for that special case!)
     */
    private File m_nodeInternDir;

    /** See {@link MissingNodeFactory#setCopyInternDirForWorkflowVersionChange(boolean)}. */
    private final boolean m_copyInternDirForWorkflowVersionChange;

    /**
     * @param nodeInfo  ...
     * @param inPortTypes ...
     * @param outPortTypes ...
     * @param copyInternDirForWorkflowVersionChange ...
     */
    MissingNodeModel(final NodeAndBundleInformation nodeInfo, final PortType[] inPortTypes,
            final PortType[] outPortTypes, final boolean copyInternDirForWorkflowVersionChange) {
        super(inPortTypes, outPortTypes);
        m_nodeAndBundleInformation = nodeInfo;
        m_copyInternDirForWorkflowVersionChange = copyInternDirForWorkflowVersionChange;
    }

    /**
     * @return the nodeAndBundleInformation
     */
    public NodeAndBundleInformation getNodeAndBundleInformation() {
        return m_nodeAndBundleInformation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        throw new InvalidSettingsException(getErrorMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        throw new InvalidSettingsException(getErrorMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_nodeInternDir != null) {
            FileUtil.deleteRecursively(m_nodeInternDir);
        }
        m_nodeInternDir = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDispose() {
        super.onDispose();
        reset();
    }

    private String getErrorMessage() {
        return "Node can't be executed - " + m_nodeAndBundleInformation.getErrorMessageWhenNodeIsMissing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_settings != null) {
            m_settings.copyTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings = new NodeSettings("copy");
        settings.copyTo(m_settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_copyInternDirForWorkflowVersionChange && nodeInternDir.exists() && nodeInternDir.list().length > 0) {
            String nodeName = m_nodeAndBundleInformation.getNodeNameNotNull();
            nodeName = nodeName.replaceAll("[^\\w-]", "");
            m_nodeInternDir = FileUtil.createTempDir("knime_internal_to_" + nodeName + "_");
            FileUtil.copyDir(nodeInternDir, m_nodeInternDir);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_nodeInternDir != null) {
            FileUtil.copyDir(m_nodeInternDir, nodeInternDir);
        }
    }

}
