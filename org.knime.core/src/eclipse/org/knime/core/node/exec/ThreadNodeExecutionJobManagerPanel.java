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
 *   23 Jun 2022 (Dionysios Stolis): created
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
 *
 * @author Dionysios Stolis
 * @since 4.6
 */
public class ThreadNodeExecutionJobManagerPanel extends NodeExecutionJobManagerPanel {

    private final JCheckBox m_discardIntermediateData;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public ThreadNodeExecutionJobManagerPanel() {
        setLayout(new BorderLayout());
        m_discardIntermediateData = new JCheckBox("Discard Intermediate Data");
        add(m_discardIntermediateData, BorderLayout.NORTH);
        var descriptionText =
                "<html><body><p>Discards the intermediate data after Component's execution.</p><body></html>";
        var description = new JLabel(descriptionText);
        add(description, BorderLayout.CENTER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        var c = new ThreadNodeExecutionSettings();
        c.setDiscardIntermediateData(m_discardIntermediateData.isSelected());
        c.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) {
        var c = new ThreadNodeExecutionSettings();
        c.loadSettings(settings);
        m_discardIntermediateData.setSelected(c.isDiscardIntermediateData());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInputSpecs(final PortObjectSpec[] inSpecs) {
        // TODO Auto-generated method stub

    }

}
