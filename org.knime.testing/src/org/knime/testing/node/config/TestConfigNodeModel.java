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
 * ---------------------------------------------------------------------
 *
 * History
 *   07.09.2011 (meinl): created
 */
package org.knime.testing.node.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContext;
import org.knime.testing.core.TestrunJanitor;

import junit.framework.AssertionFailedError;

/**
 * This is the node model for the testflow configuration node. The model
 * essentially does nothing except for checking if the owner's mail address has
 * been provided.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TestConfigNodeModel extends NodeModel {
    private final TestConfigSettings m_settings = new TestConfigSettings();

    private final List<TestrunJanitor> m_janitors = new ArrayList<>();

    /**
     * Creates a new node model.
     */
    public TestConfigNodeModel() {
        super(0, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        TestConfigSettings s = new TestConfigSettings();
        s.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if ((m_settings.owner() == null)
                || (m_settings.owner().trim().length() < 1)) {
            throw new InvalidSettingsException("No workflow owner set");
        }

        Map<String, TestrunJanitor> janitors = new HashMap<>();
        for (TestrunJanitor j : TestrunJanitor.getJanitors()) {
            janitors.put(j.getID(), j);
        }

        for (String jid : m_settings.usedJanitors()) {
            TestrunJanitor j = janitors.get(jid);
            if (j != null) {
                m_janitors.add(j);
            } else {
                throw new InvalidSettingsException("Configured testrun janitor '" + jid + "' not found");
            }
        }

        for (TestrunJanitor j : m_janitors) {
            pushFlowVariables(j.getFlowVariables());
        }

        return new DataTableSpec[0];
    }

    private void pushFlowVariables(final Collection<FlowVariable> flowVariables) throws InvalidSettingsException {
        for (FlowVariable fv : flowVariables) {
            if (fv.getType() == FlowVariable.Type.DOUBLE) {
                pushFlowVariableDouble(fv.getName(), fv.getDoubleValue());
            } else if (fv.getType() == FlowVariable.Type.INTEGER) {
                pushFlowVariableInt(fv.getName(), fv.getIntValue());
            } else if (fv.getType() == FlowVariable.Type.STRING) {
                pushFlowVariableString(fv.getName(), fv.getStringValue());
            } else {
                throw new InvalidSettingsException("Unsupported flow variable type for '" + fv.getName() + "': "
                        + fv.getType());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (m_settings.requiredLoadVersion() != null) {
            LoadVersion loadVersion = NodeContext.getContext().getWorkflowManager().getLoadVersion();
            if (m_settings.requiredLoadVersion().isOlderThan(loadVersion)) {
                throw new AssertionFailedError(String.format("Workflow was required to stay in an older version than it"
                    + " is now (required: %s, actual: %s). It may have been accidentally saved with a newer version.",
                    m_settings.requiredLoadVersion(), loadVersion));
            }
        }

        final double max = m_janitors.size();
        int i = 0;
        for (TestrunJanitor j : m_janitors) {
            exec.checkCanceled();
            exec.setProgress((i++ / max), "Executing janitor " + j.getName());
            j.before();
            pushFlowVariables(j.getFlowVariables());
        }

        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        runAfterJanitors();
        m_janitors.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDispose() {
        runAfterJanitors();
        super.onDispose();
    }

    private void runAfterJanitors() {
        for (TestrunJanitor j : m_janitors) {
            try {
                j.after();
            } catch (Exception ex) {
                getLogger().error("Error while executing testrun janitor '" + j.getID() + "': " + ex.getMessage(), ex);
            }
        }
    }
}
