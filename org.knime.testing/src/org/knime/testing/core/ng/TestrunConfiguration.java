/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   02.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.File;

/**
 * Common configuration for all testflows that are executed during the current run.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class TestrunConfiguration {
    /**
     * The default maximum runtime for a single testcase in seconds (currently {@value} ). After the timeout the
     * workflow will be canceled.
     */
    public static final int DEFAULT_TIMEOUT = 300;

    private boolean m_testDialogs;

    private boolean m_testViews;

    private boolean m_checkForLoadWarnings;

    private boolean m_reportDeprecatedNodes;

    private boolean m_checkLogMessages;

    private File m_saveLocation;

    private int m_timeout = DEFAULT_TIMEOUT;

    /**
     * Sets if dialogs for all nodes in the workflow should be tested, i.e. load settings and save settings after the
     * workflow has been executed.
     *
     * @param b <code>true</code> if dialogs should be tested, <code>false</code> otherwise
     */
    public void setTestDialogs(final boolean b) {
        m_testDialogs = b;
    }

    /**
     * Sets if all views should be opened prior to running the workflow.
     *
     * @param b <code>true</code> if views should be opened, <code>false</code> otherwise
     */
    public void setTestViews(final boolean b) {
        m_testViews = b;
    }

    /**
     * Sets the timeout for this workflow in seconds. After the timeout the workflow will be canceled if it is still
     * running. Note that the timeout set here is overridden by the timeout specified via the testflow configuration
     * node.
     *
     * @param seconds the timeout
     */
    public void setTimeout(final int seconds) {
        m_timeout = seconds;
    }

    /**
     * Returns if dialogs for all nodes in the workflow should be tested, i.e. load settings and save settings.
     *
     * @return <code>true</code> if dialogs should be tested, <code>false</code> otherwise
     */
    public boolean isTestDialogs() {
        return m_testDialogs;
    }

    /**
     * Returns if all views should be opened prior to running the workflow.
     *
     * @return <code>true</code> if views should be opened, <code>false</code> otherwise
     */
    public boolean isTestViews() {
        return m_testViews;
    }

    /**
     * Returns the timeout for this workflow in seconds. After the timeout the workflow will be canceled if it is still
     * running.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return m_timeout;
    }

    /**
     * Sets whether warnings during load should be reported as failures or not.
     *
     * @param b <code>true</code> if warnings are treated as failures, <code>false</code> otherwise
     */
    public void setCheckForLoadWarnings(final boolean b) {
        m_checkForLoadWarnings = b;
    }

    /**
     * Returns whether warnings during load should be reported as failures or not.
     *
     * @return <code>true</code> if warnings are treated as failures, <code>false</code> otherwise
     */
    public boolean isCheckForLoadWarnings() {
        return m_checkForLoadWarnings;
    }

    /**
     * Returns the location where executed testflows should be saved to. If <code>null</code> the workflows should not
     * be saved.
     *
     * @return the destination directory for saved workflows
     */
    public File getSaveLocation() {
        return m_saveLocation;
    }

    /**
     * Sets the location where executed testflows should be saved to. If <code>null</code> the workflows should not
     * be saved.
     *
     * @param saveLocation the destination directory for saved workflows
     */
    public void setSaveLocation(final File saveLocation) {
        m_saveLocation = saveLocation;
    }

    /**
     * Returns whether deprecated nodes in workflows should be reported as failures.
     *
     * @return <code>true</code> if deprecated nodes result in failures, <code>false</code> otherwise
     */
    public boolean isReportDeprecatedNodes() {
        return m_reportDeprecatedNodes;
    }

    /**
     * Sets whether deprecated nodes in workflows should be reported as failures.
     *
     * @param reportDeprecatedNodes <code>true</code> if deprecated nodes result in failures, <code>false</code> otherwise
     */
    public void setReportDeprecatedNodes(final boolean reportDeprecatedNodes) {
        m_reportDeprecatedNodes = reportDeprecatedNodes;
    }

    /**
     * Returns whether all log messages should be checked for required or unexpected messages.
     *
     * @return <code>true</code> when log messages should be checked, <code>false</code> otherwise
     */
    public boolean isCheckLogMessages() {
        return m_checkLogMessages;
    }

    /**
     * Sets whether all log messages should be checked for required or unexpected messages.
     *
     * @param checkLogMessages <code>true</code> when log messages should be checked, <code>false</code> otherwise
     */
    public void setCheckLogMessages(final boolean checkLogMessages) {
        m_checkLogMessages = checkLogMessages;
    }
}
