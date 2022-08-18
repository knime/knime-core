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
 *   30 Jun 2022 (jasper): created
 */
package org.knime.core.node.exec;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeExecutionJobManagerPanel;

/**
 * This panel provides the UI to configure an instance of {@link ThreadComponentExecutionJobManagerSettings}.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz
 */
public final class ThreadComponentExecutionJobManagerPanel extends NodeExecutionJobManagerPanel {

    private static final long serialVersionUID = -3844665009400635814L;

    private static final String CANCEL_ON_FAILURE_TITLE = "Cancel on failure";

    private static final String CANCEL_ON_FAILURE_DESC =
        "If enabled, a failing node whose error is not handled by a Try node<br/>"
        + "will cause the cancellation of all running nodes in the component.";

    private final JCheckBox m_cancelOnFailure;

    /**
     * Create a new instance of the configuration panel
     */
    public ThreadComponentExecutionJobManagerPanel() {
        setLayout(new BorderLayout());
        m_cancelOnFailure = new JCheckBox(CANCEL_ON_FAILURE_TITLE);
        add(m_cancelOnFailure, BorderLayout.NORTH);
        var descriptionText = "<html><body><p>" + CANCEL_ON_FAILURE_DESC + "</p><body></html>";
        var description = new JLabel(descriptionText);
        add(description, BorderLayout.CENTER);
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        var s = new ThreadComponentExecutionJobManagerSettings();
        s.setCancelOnFailure(m_cancelOnFailure.isSelected());
        s.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettings(final NodeSettingsRO settings) {
        var s = new ThreadComponentExecutionJobManagerSettings();
        s.loadSettingsFrom(settings);
        m_cancelOnFailure.setSelected(s.isCancelOnFailure());
    }

    /** {@inheritDoc} */
    @Override
    public void updateInputSpecs(final PortObjectSpec[] inSpecs) {
        // nothing to do
    }

}
