/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.columnrenameregex;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/** Dialog to node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnRenameRegexNodeDialogPane extends NodeDialogPane {

    private final JTextField m_searchField;
    private final JTextField m_replaceField;
    private final JCheckBox m_caseInsensitiveChecker;
    private final JCheckBox m_literalChecker;

    /** Create new dialog. */
    public ColumnRenameRegexNodeDialogPane() {
        final int textFieldColCount = 20;
        m_searchField = new JTextField(textFieldColCount);
        m_replaceField = new JTextField(textFieldColCount);
        m_caseInsensitiveChecker = new JCheckBox("Case Insensitive");
        m_literalChecker = new JCheckBox("Literal");
        addTab("Main Configuration", createPanel());
    }

    private JPanel createPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel p = new JPanel(new GridBagLayout());
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        p.add(new JLabel("Search String (regexp): "), gbc);
        gbc.gridx += 1;
        gbc.gridwidth = 2;
        p.add(m_searchField, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        p.add(new JLabel("Replacement: "), gbc);
        gbc.gridx += 1;
        gbc.gridwidth = 2;
        p.add(m_replaceField, gbc);

        gbc.gridy += 1;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        p.add(m_caseInsensitiveChecker, gbc);
        gbc.gridx += 1;
        p.add(m_literalChecker, gbc);

        return p;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        ColumnRenameRegexConfiguration config =
            new ColumnRenameRegexConfiguration();
        config.loadSettingsInDialog(settings);
        m_searchField.setText(config.getSearchString());
        m_replaceField.setText(config.getReplaceString());
        m_caseInsensitiveChecker.setSelected(config.isCaseInsensitive());
        m_literalChecker.setSelected(config.isLiteral());
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        ColumnRenameRegexConfiguration config =
            new ColumnRenameRegexConfiguration();
        config.setSearchString(m_searchField.getText());
        config.setReplaceString(m_replaceField.getText());
        config.setCaseInsensitive(m_caseInsensitiveChecker.isSelected());
        config.setLiteral(m_literalChecker.isSelected());
        config.saveConfiguration(settings);
    }

}
