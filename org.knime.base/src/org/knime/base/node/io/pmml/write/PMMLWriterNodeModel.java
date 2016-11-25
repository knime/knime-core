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
package org.knime.base.node.io.pmml.write;

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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLWriterNodeModel extends NodeModel {


    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLWriterNodeModel.class);

    private final SettingsModelString m_outfile
        = PMMLWriterNodeDialog.createFileModel();

    private final SettingsModelBoolean m_overwriteOK
            = PMMLWriterNodeDialog.createOverwriteOKModel();

    private final SettingsModelBoolean m_validatePMML
            = PMMLWriterNodeDialog.createValidateModel();

    /**
     *
     */
    public PMMLWriterNodeModel() {
        super(new PortType[] {PMMLPortObject.TYPE}, new PortType[] {});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        String warning = CheckUtils.checkDestinationFile(m_outfile.getStringValue(), m_overwriteOK.getBooleanValue());
        if (warning != null) {
            setWarningMessage(warning);
        }
        return new PortObjectSpec[] {};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec)
            throws Exception {
        CheckUtils.checkDestinationFile(m_outfile.getStringValue(), m_overwriteOK.getBooleanValue());

        URL url = FileUtil.toURL(m_outfile.getStringValue());
        Path localPath = FileUtil.resolveToPath(url);
        PMMLPortObject pmml = (PMMLPortObject)inData[0];
        if (m_validatePMML.getBooleanValue()) {
            pmml.validate();
        }

        try (OutputStream os = openOutputStream(localPath, url)) {
            pmml.save(os);
        }
        return new PortObject[] {};
    }

    private static OutputStream openOutputStream(final Path localPath, final URL url) throws IOException {
        if (localPath != null) {
            return Files.newOutputStream(localPath);
        } else {
            return FileUtil.openOutputConnection(url, "PUT").getOutputStream();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_outfile.loadSettingsFrom(settings);
        try {
            // property added in v2.1 -- if missing (old flow), set it to true
            m_overwriteOK.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_overwriteOK.setBooleanValue(true);
        }
        try {
            // property added in v2.4 -- if missing (old flow), set it to true
            m_validatePMML.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_validatePMML.setBooleanValue(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // ignore -> no view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_outfile.saveSettingsTo(settings);
        m_overwriteOK.saveSettingsTo(settings);
        m_validatePMML.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_outfile.validateSettings(settings);
        // overwriteOk added in v2.1 - can't validate
    }

}
