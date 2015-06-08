/*
 * ------------------------------------------------------------------------
 *
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
 *   Mar 13, 2015 (wiswedel): created
 */
package org.knime.testing.node.failing;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ViewUtils;



/**
 *
 * @author wiswedel
 */
final class FailingNodeDialogPane extends NodeDialogPane {

    private final JSpinner m_failAtRowIndexSpinner;
    private final JCheckBox m_failAtRowIndexChecker;

    FailingNodeDialogPane() {
        m_failAtRowIndexSpinner = new JSpinner(new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 1));
        m_failAtRowIndexChecker = new JCheckBox("Fail at row index: ");
        m_failAtRowIndexChecker.addActionListener((ae) ->
            {m_failAtRowIndexSpinner.setEnabled(m_failAtRowIndexChecker.isSelected());});
        m_failAtRowIndexChecker.doClick();
        addTab("Main", ViewUtils.getInFlowLayout(m_failAtRowIndexChecker, m_failAtRowIndexSpinner));
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        int count = m_failAtRowIndexChecker.isSelected() ? (Integer)m_failAtRowIndexSpinner.getValue() : -1;
        FailingNodeConfiguration configuration = new FailingNodeConfiguration();
        configuration.setFailAtIndex(count);
        configuration.saveSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
                throws NotConfigurableException {
        FailingNodeConfiguration configuration = new FailingNodeConfiguration();
        configuration.loadSettingsInDialog(settings);
        int count = configuration.getFailAtIndex();
        if (count >= 0 != m_failAtRowIndexChecker.isSelected()) {
            m_failAtRowIndexChecker.doClick();
        }
        m_failAtRowIndexSpinner.setValue(count >= 0 ? count : 100);
    }

}
