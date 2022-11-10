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
 *   10 Nov 2022 (marcbux): created
 */
package org.knime.core.webui.node.dialog.impl;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The {@link NodeModel} for simple WebUI nodes, see {@link WebUINodeFactory}.
 *
 * @param <S> the type of model settings
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public abstract class WebUINodeModel<S extends DefaultNodeSettings> extends NodeModel {

    private DataTableSpec[] m_inSpecs;

    private S m_modelSettings;

    private final Class<S> m_modelSettingsClass;

    /**
     * @param configuration the {@link WebUINodeConfiguration} for this factory
     * @param modelSettingsClass the type of the model settings for this node
     */
    protected WebUINodeModel(final WebUINodeConfiguration configuration, final Class<S> modelSettingsClass) {
        super(configuration.getInPortDescriptions().length, configuration.getOutPortDescriptions().length);
        m_modelSettingsClass = modelSettingsClass;
    }

    @Override
    protected final DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        m_inSpecs = inSpecs;
        if (m_modelSettings == null) {
            m_modelSettings = DefaultNodeSettings.createSettings(m_modelSettingsClass, inSpecs);
        }
        return configure(inSpecs, m_modelSettings);
    }

    /**
     * @param inSpecs the input {@link DataTableSpec DataTableSpecs}
     * @param modelSettings the current model settings
     * @return the output {@link DataTableSpec DataTableSpecs}
     * @throws InvalidSettingsException if the settings are inconsistent with the input specs
     * @see NodeModel#configure(DataTableSpec[])
     */
    protected abstract DataTableSpec[] configure(final DataTableSpec[] inSpecs, S modelSettings)
        throws InvalidSettingsException;

    @Override
    protected final BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        return execute(inData, exec, m_modelSettings);
    }

    /**
     * @param inData the input {@link BufferedDataTable BufferedDataTables}
     * @param exec the current {@link ExecutionContext}
     * @param modelSettings the current model settings
     * @return the output {@link BufferedDataTable BufferedDataTables}
     * @throws Exception if anything goes wrong during the execution
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    protected abstract BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        S modelSettings) throws Exception;

    @Override
    protected final void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected final void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_modelSettings != null) {
            DefaultNodeSettings.saveSettings(m_modelSettingsClass, m_modelSettings, m_inSpecs, settings);
        }
    }

    @Override
    protected final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_modelSettings = DefaultNodeSettings.loadSettings(settings, m_modelSettingsClass);
    }

    @Override
    protected final void reset() {
    }

}
