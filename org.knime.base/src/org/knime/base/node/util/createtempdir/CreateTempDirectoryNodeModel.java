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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.util.createtempdir;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.knime.base.node.util.createtempdir.CreateTempDirectoryConfiguration.VarNameFileNamePair;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class CreateTempDirectoryNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(CreateTempDirectoryNodeModel.class);

    private CreateTempDirectoryConfiguration m_configuration;
    private UUID m_id;

    /**
     *
     */
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
            m_id = UUID.randomUUID();
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

    private File computeFileName(final UUID id) {
        String baseName = m_configuration.getBaseName();
        return new File(new File(System.getProperty("java.io.tmpdir")),
                baseName + id.toString());
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
        super.onDispose();
        reset();
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

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        if (m_configuration.isDeleteOnReset()) {
            setWarningMessage(
                    "Did not restore content; consider to re-execute!");
        }
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals, really
    }

}
