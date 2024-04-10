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
 *   Sep 29, 2020 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.Optional;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.knime.core.data.TableBackend;
import org.knime.core.data.TableBackendRegistry;
import org.knime.core.data.container.storage.TableStoreFormatInformation;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.osgi.framework.Bundle;

/**
 * Represents the configuration as to which {@link TableBackend} is used in a workflow project
 * (workflow level configuration).
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public final class WorkflowTableBackendSettings {

    private static final String CFG_TABLE_BACKEND = "tableBackend";
    private static final String CFG_TABLE_BACKEND_CLASS = "class";
    private static final String CFG_TABLE_BACKEND_BUNDLE = "bundle";
    private static final String CFG_TABLE_BACKEND_FEATURE = "feature";
    private static final String CFG_TABLE_BACKEND_SHORTNAME = "shortname";

    private final TableBackend m_tableBackend;

    WorkflowTableBackendSettings() {
        this(TableBackendRegistry.getInstance().getDefaultBackendForNewWorkflows());
    }

    WorkflowTableBackendSettings(final TableBackend tableBackend) {
        m_tableBackend = CheckUtils.checkArgumentNotNull(tableBackend);
    }

    /**
     * @return the tableBackend
     */
    TableBackend getTableBackend() {
        return m_tableBackend;
    }

    static WorkflowTableBackendSettings loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        TableBackend tableBackend;
        final TableBackendRegistry registry = TableBackendRegistry.getInstance();
        if (registry.isForceDefaultBackendOnOldWorkflows()) {
            tableBackend = registry.getDefaultBackendForNewWorkflows();
        } else if (settings.containsKey(CFG_TABLE_BACKEND)) {
            NodeSettingsRO tableBackendSettings = settings.getNodeSettings(CFG_TABLE_BACKEND);
            String className = CheckUtils.checkSettingNotNull(tableBackendSettings.getString(CFG_TABLE_BACKEND_CLASS),
                "Table Backend Class must not be null");
            try {
                tableBackend = registry.getTableBackend(className);
            } catch (IllegalArgumentException ex) {
                String bundle = tableBackendSettings.getString(CFG_TABLE_BACKEND_BUNDLE, null);
                String feature = tableBackendSettings.getString(CFG_TABLE_BACKEND_FEATURE, null);
                String shortname = tableBackendSettings.getString(CFG_TABLE_BACKEND_SHORTNAME, className);
                throw new TableBackendUnknownException("Table backend implementation not found: " + ex.getMessage(),
                    TableStoreFormatInformation.forTableBackend(bundle, feature, shortname), ex);
            }
        } else {
            tableBackend = registry.getPre43TableBackend();
        }
        return new WorkflowTableBackendSettings(tableBackend);
    }

    static WorkflowTableBackendSettings loadSettingsInDialog(final NodeSettingsRO settings) {
        try {
            return loadSettingsInModel(settings);
        } catch (InvalidSettingsException ex) { // NOSONAR
            return new WorkflowTableBackendSettings();
        }
    }

    void saveSettingsTo(final NodeSettingsWO settings) {
        if (!m_tableBackend.equals(TableBackendRegistry.getInstance().getPre43TableBackend())) {
            NodeSettingsWO tableBackendSettings = settings.addNodeSettings(CFG_TABLE_BACKEND);
            tableBackendSettings.addString(CFG_TABLE_BACKEND_CLASS, m_tableBackend.getClass().getName());
            Bundle bundle = OSGIHelper.getBundle(m_tableBackend.getClass());
            Optional<IInstallableUnit> feature = OSGIHelper.getFeature(bundle);
            tableBackendSettings.addString(CFG_TABLE_BACKEND_BUNDLE, bundle != null ? bundle.getSymbolicName() : null);
            tableBackendSettings.addString(CFG_TABLE_BACKEND_FEATURE, feature.map(IInstallableUnit::getId).orElse(null));
            tableBackendSettings.addString(CFG_TABLE_BACKEND_SHORTNAME, m_tableBackend.getShortName());
        }
    }

    /** Get table backend selected for the current thread / executing workflow. Method resolved corresponding workflow
     * via {@link NodeContext}. If none is detected, the default backend is returned.
     * @return The backend for the current workflow, not null.
     */
    public static TableBackend getTableBackendForCurrentContext() {
        final var  context = NodeContext.getContext();
        if (context != null) {
            final var optSettings = getSettings(context.getWorkflowManager());
            if (optSettings.isPresent()) {
                return optSettings.get().getTableBackend();
            }
        }
        return TableBackendRegistry.getInstance().getDefaultBackendForNewWorkflows();
    }

    /**
     * @param workflowManager
     * @return
     * @since 5.3
     */
    public static Optional<WorkflowTableBackendSettings> getSettings(final WorkflowManager workflowManager) {
        for (var wfm = workflowManager; wfm != null; wfm = wfm.getParent()) {
            final var optSettings = wfm.getTableBackendSettings();
            if (optSettings.isPresent()) {
                return optSettings;
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return String.format("backend: %s", m_tableBackend.getClass().getSimpleName());
    }

    @SuppressWarnings("serial")
    static final class TableBackendUnknownException extends InvalidSettingsException {

        private final TableStoreFormatInformation m_formatInfo; // NOSONAR

        TableBackendUnknownException(final String msg, final TableStoreFormatInformation formatInfo,
            final Throwable cause) {
            super(msg, cause);
            m_formatInfo = formatInfo;
        }

        TableStoreFormatInformation getFormatInfo() {
            return m_formatInfo;
        }

    }

}
