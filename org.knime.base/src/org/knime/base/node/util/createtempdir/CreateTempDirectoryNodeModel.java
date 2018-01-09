/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.util.createtempdir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.knime.base.node.util.createtempdir.CreateTempDirectoryConfiguration.VarNameFileNamePair;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class CreateTempDirectoryNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CreateTempDirectoryNodeModel.class);

    private CreateTempDirectoryConfiguration m_configuration;
    private String m_id;

    CreateTempDirectoryNodeModel() {
        super(new PortType[] {}, new PortType[] {FlowVariablePortObject.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_configuration == null) {
            throw new InvalidSettingsException("No settings available");
        }
        File f;
        do {
            m_id = RandomStringUtils.randomAlphanumeric(12).toLowerCase();
            f = computeFileName(m_id);
        } while (f.exists());
        pushVariables(f);
        return new PortObjectSpec[] {FlowVariablePortObjectSpec.INSTANCE};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(
            final PortObject[] inObjects, final ExecutionContext exec)
            throws Exception {
        File f = computeFileName(m_id);
        if (f.isDirectory()) {
            LOGGER.debug("Skipping creation of directory \""
                    + f.getAbsolutePath() + "\" as it already exists");
        } else {
            f.mkdir();
        }
        return new PortObject[] {FlowVariablePortObject.INSTANCE};
    }

    private void pushVariables(final File f) {
        String path = f.getAbsolutePath();
        pushFlowVariableString(m_configuration.getVariableName(), path);
        for (VarNameFileNamePair p : m_configuration.getPairs()) {
            pushFlowVariableString(p.getVariableName(),
                    new File(f, p.getFileName()).getAbsolutePath());
        }
    }

    private File computeFileName(final String id) {
        File rootDir = null;
        // get the flow's tmp dir from its context
        NodeContext nodeContext = NodeContext.getContext();
        if (nodeContext != null) {
            WorkflowContext workflowContext = nodeContext.getWorkflowManager().getContext();
            if (workflowContext != null) {
                rootDir = workflowContext.getTempLocation();
            }
        }
        if (rootDir == null) {
            // use the standard tmp dir then.
            rootDir = new File(KNIMEConstants.getKNIMETempDir());
        }
        String baseName = m_configuration.getBaseName();
        return new File(rootDir, baseName + id);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return super.configure(inSpecs);
    }

    /** {@inheritDoc} */
    @Override
    protected void onDispose() {
        /* This node is not deleting the created dir after its disposal. The server requires the temp dir to stay when
         * the flow is swapped out (which disposes of this node). Trust that the workflow manager or server delete all
         * temporary directories and files.
        */
        super.onDispose();
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        if (m_id == null) {
            return;
        }
        File file = computeFileName(m_id);
        StringBuilder debug = new StringBuilder();
        if (m_configuration.isDeleteOnReset()) {
            if (FileUtil.deleteRecursively(file)) {
                debug.append("Deleted temp directory "
                        + file.getAbsolutePath());
            } else {
                debug.append("Did not delete temp directory \"");
                debug.append(file.getAbsolutePath());
                debug.append("\" ");
                if (file.exists()) {
                    debug.append("(file/directory exists)");
                } else {
                    debug.append(" as it does not exist");
                }
            }
        } else {
            debug.append("Not deleting temp directory \"");
            debug.append(file.getAbsolutePath());
            debug.append("\" according to user setting");
        }
        LOGGER.debug(debug);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new CreateTempDirectoryConfiguration().loadInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        CreateTempDirectoryConfiguration t =
            new CreateTempDirectoryConfiguration();
        t.loadInModel(settings);
        m_configuration = t;

    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.save(settings);
        }
    }

    private static String INTERNAL_FILE_NAME = "internals.xml";

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File internalFile = new File(nodeInternDir, INTERNAL_FILE_NAME);
        boolean issueWarning;
        if (internalFile.exists()) { // not present before 3.1
            // in most standard cases this isn't reasonable as the folder gets deleted when the flow is closed.
            // however, it's useful if the node is run in a temporary workflow that is part of the streaming executor
            try (InputStream in = new FileInputStream(internalFile)) {
                NodeSettingsRO s = NodeSettings.loadFromXML(in);
                m_id = CheckUtils.checkSettingNotNull(s.getString("temp-folder-id"), "id must not be null");
                issueWarning = !computeFileName(m_id).exists();
            } catch (InvalidSettingsException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            issueWarning = m_configuration.isDeleteOnReset();
        }
        if (issueWarning) {
            setWarningMessage("Did not restore content; consider to re-execute!");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        if (m_id != null) {
            // added in 3.1
            try (OutputStream w = new FileOutputStream(new File(nodeInternDir, INTERNAL_FILE_NAME))) {
                NodeSettings s = new NodeSettings("temp-dir-node");
                s.addString("temp-folder-id", m_id);
                s.saveToXML(w);
            }
        }
        // else this was a workflow saved in 2.x and then loaded and saved/converted in 3.1
    }

}
