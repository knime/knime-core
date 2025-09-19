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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.08.2013 (thor): created
 */
package org.knime.testing.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.knime.core.node.workflow.FlowVariable;

/**
 * Common configuration for all testflows that are executed during the current run.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class TestrunConfiguration {
    /**
     * The default maximum runtime for a single testcase in seconds (currently {@value} ). After the timeout the
     * workflow will be canceled.
     */
    public static final int DEFAULT_TIMEOUT = 300;

    /**
     * The default maximum allowed memory increase per testflow (currently {@value} bytes).
     */
    public static final int DEFAULT_ALLOWED_MEMORY_INCREASE = 1024 * 1024;

    private boolean m_testDialogs;

    private boolean m_testMUIDialogs;

    private boolean m_testViews = true;

    private boolean m_checkForLoadWarnings;

    private boolean m_reportDeprecatedNodes;

    private boolean m_checkLogMessages = true;

    private boolean m_loadSaveLoad;

    private File m_saveLocation;

    private boolean m_closeWorkflowAfterTest = true;

    private int m_timeout = DEFAULT_TIMEOUT;

    private boolean m_checkMemoryLeaks = false;

    private int m_allowedMemoryIncrease = DEFAULT_ALLOWED_MEMORY_INCREASE;

    private boolean m_stacktraceOnTimeout;

    private boolean m_checkNodeMessages = true;

    private boolean m_enableStreamingMode = true;

    private final Collection<FlowVariable> m_flowVariables = new ArrayList<>();

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
     * Set if all modern UI ("MUI") dialogs backed by NodeParameters should be tested, i.e. load settings from
     * the workflow, then load&apply via the dialog's NodeParameters class.
     *
     * @param b the value to set
     * @since 5.8
     */
    public void setTestMUIDialogs(final boolean b) {
        m_testMUIDialogs = b;
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
     * Returns if dialogs for all nodes in the workflow should be tested, i.e. load settings and save settings. The
     * default is <code>false</code>.
     *
     * @return <code>true</code> if dialogs should be tested, <code>false</code> otherwise
     */
    public boolean isTestDialogs() {
        return m_testDialogs;
    }

    /**
     * Getter for {@link #setTestMUIDialogs(boolean)}.
     * @return the value of {@link #setTestMUIDialogs(boolean)}
     */
    public boolean isTestMUIDialogs() {
        return m_testMUIDialogs;
    }

    /**
     * Returns if all views should be opened prior to running the workflow. The default is <code>true</code>.
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
     * Returns whether warnings during load should be reported as failures or not. The default is <code>false</code>.
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
     * Sets the location where executed testflows should be saved to. If <code>null</code> the workflows should not be
     * saved.
     *
     * @param saveLocation the destination directory for saved workflows
     */
    public void setSaveLocation(final File saveLocation) {
        m_saveLocation = saveLocation;
    }

    /**
     * Returns whether deprecated nodes in workflows should be reported as failures. The default is <code>false</code>.
     *
     * @return <code>true</code> if deprecated nodes result in failures, <code>false</code> otherwise
     */
    public boolean isReportDeprecatedNodes() {
        return m_reportDeprecatedNodes;
    }

    /**
     * Sets whether deprecated nodes in workflows should be reported as failures.
     *
     * @param reportDeprecatedNodes <code>true</code> if deprecated nodes result in failures, <code>false</code>
     *            otherwise
     */
    public void setReportDeprecatedNodes(final boolean reportDeprecatedNodes) {
        m_reportDeprecatedNodes = reportDeprecatedNodes;
    }

    /**
     * Returns whether all log messages should be checked for required or unexpected messages. The default is
     * <code>true</code>.
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

    /**
     * Sets whether the workflow should be closed as part of the tests. Sometimes the workflow should not be closed,
     * e.g. when the test is run in the KNIME GUI.
     *
     * @param close <code>true</code> when the workflow should be closed, <code>false</code> otherwise
     */
    public void setCloseWorkflowAfterTest(final boolean close) {
        m_closeWorkflowAfterTest = close;
    }

    /**
     * Returns whether the workflow should be closed as part of the tests. Sometimes the workflow should not be closed,
     * e.g. when the test is run in the KNIME GUI. The default is <code>true</code>.
     *
     * @return <code>true</code> when the workflow should be closed, <code>false</code> otherwise
     */
    public boolean isCloseWorkflowAfterTest() {
        return m_closeWorkflowAfterTest;
    }

    /**
     * Sets whether the workflows should be loaded, saved into a temporary directory, and loaded from there again before
     * it is executed. This is useful in order to check whether format conversion are done properly. If set to
     * <code>false</code> the workflow is only loaded once from the original location.
     *
     * @param loadSaveLoad <code>true</code> when the workflows should be loaded, saved, and loaded again,
     *            <code>false</code> otherwise
     */
    public void setLoadSaveLoad(final boolean loadSaveLoad) {
        m_loadSaveLoad = loadSaveLoad;
    }

    /**
     * Returns whether the workflows should be loaded, saved into a temporary directory, and loaded from there again
     * before it is executed. This is useful in order to check whether format conversion are done properly. If set to
     * <code>false</code> the workflow is only loaded once from the original location. The default is <code>false</code>
     * .
     *
     * @return <code>true</code> when the workflows should be loaded, saved, and loaded again, <code>false</code>
     *         otherwise
     */
    public boolean isLoadSaveLoad() {
        return m_loadSaveLoad;
    }

    /**
     * Sets whether the memory usage before and after the execution of a testflow should be tested.
     *
     * @param checkMemoryLeaks <code>true</code> if a check for memory leaks should be performed, <code>false</code>
     *            otherwise.
     */
    public void setCheckMemoryLeaks(final boolean checkMemoryLeaks) {
        m_checkMemoryLeaks = checkMemoryLeaks;
    }

    /**
     * Returns whether the memory usage before and after the execution of a testflow should be tested. The default is to
     * not check for memory leaks.
     *
     * @return <code>true</code> if a check for memory leaks should be performed, <code>false</code> otherwise.
     */
    public boolean isCheckMemoryLeaks() {
        return m_checkMemoryLeaks;
    }

    /**
     * Returns the maximum allowed increase in heap usage after each testflow. If the increase is greater than this
     * threshold a failure will be reported (if the memory leaks are tested).
     *
     * @return the maximum allowed memory increase in bytes
     */
    public int getAllowedMemoryIncrease() {
        return m_allowedMemoryIncrease;
    }

    /**
     * Sets the maximum allowed increase in heap usage after each testflow. If the increase is greater than this
     * threshold a failure will be reported (if the memory leaks are tested).
     *
     * @param allowedMemoryIncrease the maximum allowed memory increase in bytes
     */
    public void setAllowedMemoryIncrease(final int allowedMemoryIncrease) {
        m_allowedMemoryIncrease = allowedMemoryIncrease;
    }


    /**
     * Sets whether a full stack trace dump should be output if a workflow times out. The stack trace will be part of
     * the test result message.
     *
     * @param stacktraceOnTimeout <code>true</code> if a stack trace dump should be output, <code>false</code> otherwise
     */
    public void setStacktraceOnTimeout(final boolean stacktraceOnTimeout) {
        m_stacktraceOnTimeout = stacktraceOnTimeout;
    }


    /**
     * Returns whether a full stack trace dump should be output if a workflow times out. The stack trace will be part of
     * the test result message.
     *
     * @return <code>true</code> if a stack trace dump should be output, <code>false</code> otherwise
     */
    public boolean isStacktraceOnTimeout() {
        return m_stacktraceOnTimeout;
    }

    /**
     * Sets whether all node messages should be checked for required or unexpected messages.
     *
     * @param checkNodeMessages <code>true</code> when node messages should be checked, <code>false</code> otherwise
     */
    public void setCheckNodeMessages(final boolean checkNodeMessages) {
        m_checkNodeMessages = checkNodeMessages;
    }


    /**
     * Returns whether all node messages should be checked for required or unexpected messages.
     *
     * @return <code>true</code> when node messages should be checked, <code>false</code> otherwise
     */
    public boolean isCheckNodeMessages() {
        return m_checkNodeMessages;
    }

    /**
     * Sets whether all nodes should be run in streaming mode using the streaming test executor in order to test the
     * streaming API. The workflow needs to be configured to actually be executed in streaming mode.
     *
     * @param runInStreamingMode <code>true</code> if nodes should be run in streaming mode
     */
    public void setEnableStreamingMode(final boolean runInStreamingMode) {
        m_enableStreamingMode = runInStreamingMode;
    }

    /**
     * Returns whether the nodes are to be executed in streaming mode, i.e. by using the streaming API-methods. The
     * workflow needs to be configured to actually be executed in streaming mode.
     *
     * @return <code>true</code> if the nodes should be executed in streaming mode
     */
    public boolean isEnableStreamingTests() {
        return m_enableStreamingMode;
    }

    /**
     * Adds a new flow variable to this configuration.
     *
     * @param var a flow variable
     */
    public void addFlowVariable(final FlowVariable var) {
        m_flowVariables.add(var);
    }

    /**
     * Returns an array with all configured flow variables.
     *
     * @return an array with flow variables
     */
    public FlowVariable[] getFlowVariables() {
        return m_flowVariables.toArray(new FlowVariable[m_flowVariables.size()]);
    }
}
