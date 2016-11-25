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
 * -------------------------------------------------------------------
 *
 * History
 *   29.10.2005 (mb): created
 */
package org.knime.base.node.io.portobject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;


/**
 * Write ModelContent object into file.
 *
 * @author M. Berthold, University of Konstanz
 */
class PortObjectWriterNodeModel extends NodeModel {

    /** key for filename entry in config object. */
    static final String FILENAME = "filename";

    /** Config identifier for overwrite OK. */
    static final String CFG_OVERWRITE_OK = "overwriteOK";

    private final SettingsModelString m_fileName =
        new SettingsModelString(FILENAME, null);

    private final SettingsModelBoolean m_overwriteOK =
        new SettingsModelBoolean(CFG_OVERWRITE_OK, false);

    /**
     * Constructor: Create new NodeModel with only one Model Input Port.
     * @param writeType The type of the input port.
     */
    PortObjectWriterNodeModel(final PortType writeType) {
        super(new PortType[]{writeType}, new PortType[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileName.saveSettingsTo(settings);
        m_overwriteOK.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.validateSettings(settings);
        // must not verify overwriteOK (added in v2.1)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.loadSettingsFrom(settings);
        try {
            // property added in v2.1 -- if missing (old flow), set it to true
            m_overwriteOK.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_overwriteOK.setBooleanValue(true);
        }
    }

    /**
     * Writes model as ModelContent to file.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] portObject,
            final ExecutionContext exec) throws Exception {
        CheckUtils.checkDestinationFile(m_fileName.getStringValue(), m_overwriteOK.getBooleanValue());

        URL url = FileUtil.toURL(m_fileName.getStringValue());
        Path localPath = FileUtil.resolveToPath(url);

        if (localPath != null) {
            try {
                PortUtil.writeObjectToFile(portObject[0], localPath.toFile(), exec);
            } catch (Exception e) {
                Files.deleteIfExists(localPath);
                throw e;
            }
        } else {
            try (OutputStream os = FileUtil.openOutputConnection(url, "PUT").getOutputStream()) {
                PortUtil.writeObjectToStream(portObject[0], os, exec);
            }
        }
        return new PortObject[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        String warning = CheckUtils.checkDestinationFile(m_fileName.getStringValue(), m_overwriteOK.getBooleanValue());
        if (warning != null) {
            setWarningMessage(warning);
        }
        return new PortObjectSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

}
