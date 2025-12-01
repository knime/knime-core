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
 *   May 25, 2023 (wiswedel): created
 */
package org.knime.core.node.port.report;

import java.time.Duration;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.shared.workflow.def.ReportConfigurationDef;
import org.knime.shared.workflow.def.impl.ReportConfigurationDefBuilder;

/**
 * Describes report configuration, currently only if it's enabled. In future versions it will contain any configuration
 * set on a component, e.g. a custom report layout.
 *
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME
 *
 * @since 5.1
 */
public final class ReportConfiguration {

    private static final String DEFAULT_REPORT_GENERATION_TIMEOUT_SYS_PROP =
        "org.knime.reporting2.report_generation_timeout_seconds";

    private static final int DEFAULT_REPORT_GENERATION_TIMEOUT_IN_SECONDS = 600;

    // -- CONFIG KEYS --

    private static final String ENABLE_REPORT_KEY = "enableReport";

    private static final String FAIL_ON_REPORT_ERROR_KEY = "failOnReportError";

    // -- CONFIG VALUES --

    private final boolean m_failOnReportError;

    /**
     * The instance of the default report configuration.
     *
     * @since 5.2
     */
    public static final ReportConfiguration INSTANCE = new ReportConfiguration(true);

    private ReportConfiguration(final boolean failOnReportError) {
        m_failOnReportError = failOnReportError;
    }

    /**
     * Currently not an instance value, but set statically for all report configurations.
     * Simply calls {@link #getReportGenerationTimeout()}, might change in the future.
     *
     * @return the report timeout duration
     */
    @SuppressWarnings("static-method")
    public Duration getTimeoutDuration() {
        return Duration.ofSeconds(getReportGenerationTimeout());
    }

    /**
     * Whether to fail the node execution on any errors that occur
     * during the report generation.
     *
     * @return {@code true} if the node should fail, {@code false} otherwise
     */
    public boolean isFailOnReportError() {
        return m_failOnReportError;
    }

    /**
     * Save config to disc/workflow.
     *
     * @param settings To save to.
     */
    public void save(final NodeSettingsWO settings) { //NOSONAR (more generic arg possible)
        settings.addBoolean(ENABLE_REPORT_KEY, true);
        settings.addBoolean(FAIL_ON_REPORT_ERROR_KEY, m_failOnReportError);
    }

    /** Counterpart to {@link #save(NodeSettingsWO)}.
     * @param settings To load from.
     * @return A new instance
     * @throws InvalidSettingsException in case of errors
     * @since 5.2
     */
    public static Optional<ReportConfiguration> load(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (!settings.containsKey(ENABLE_REPORT_KEY) && settings.containsKey("pagesize")) {
            throw new InvalidSettingsException(
                "Component was created with KNIME 5.1 - enable report output (again) in layout editor");
        }
        if (!settings.getBoolean(ENABLE_REPORT_KEY, false)) {
            return Optional.empty();
        }
        if (settings.containsKey(FAIL_ON_REPORT_ERROR_KEY)) {
            return Optional.of(new ReportConfiguration(settings.getBoolean(FAIL_ON_REPORT_ERROR_KEY)));
        }
        // backwards compatibility - we introduced non-failing handling first
        return Optional.of(new ReportConfiguration(false));
    }

    /**
     * Convert to Def object for new workflow serialization.
     *
     * @return A def object.
     */
    @SuppressWarnings("static-method")
    public ReportConfigurationDef toDef() {
        return new ReportConfigurationDefBuilder() //
            .setEnabled(Boolean.TRUE) //
            .build();
    }

    /**
     * @return the report generation timeout in seconds
     * @since 5.4
     */
    public static int getReportGenerationTimeout() {
        return Integer.getInteger(getReportGenerationTimeoutSystemProperty(),
            DEFAULT_REPORT_GENERATION_TIMEOUT_IN_SECONDS);
    }

    /**
     * @return the report generation timeout system property
     * @since 5.10
     */
    public static String getReportGenerationTimeoutSystemProperty() {
        return DEFAULT_REPORT_GENERATION_TIMEOUT_SYS_PROP;
    }

    /**
     * Counterpart to {@link #toDef()}.
     *
     * @param def To read from.
     * @return A new optional report config, if def was non-null.
     */
    public static Optional<ReportConfiguration> fromDef(final ReportConfigurationDef def) {
        return (def != null && def.isEnabled()) ? Optional.of(INSTANCE) : Optional.empty();
    }

}
