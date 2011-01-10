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
package org.knime.base.node.image.readpng;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/** Dialog to Read PNG node. It has a column selector and few other controls.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ReadPNGFromURLNodeDialogPane extends NodeDialogPane {

    private final ColumnSelectionComboxBox m_columnPanel;
    private final JCheckBox m_failOnInvalidChecker;
    private final JRadioButton m_replaceColumnRadio;
    private final JRadioButton m_appendColumnRadio;
    private final JTextField m_appendColumnField;

    /** Create new dialog. */
    @SuppressWarnings("unchecked")
    ReadPNGFromURLNodeDialogPane() {
        m_columnPanel = new ColumnSelectionComboxBox(
                (Border)null, StringValue.class);
        m_failOnInvalidChecker = new JCheckBox("Fail on invalid input");
        ButtonGroup bg = new ButtonGroup();
        m_replaceColumnRadio = new JRadioButton("Replace input column");
        m_appendColumnRadio = new JRadioButton("Append new column");
        bg.add(m_replaceColumnRadio);
        bg.add(m_appendColumnRadio);
        m_appendColumnField = new JTextField(10);
        m_appendColumnRadio.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_appendColumnField.setEnabled(
                        m_appendColumnRadio.isSelected());
            }
        });
        m_replaceColumnRadio.doClick();
        initLayout();
    }

    /** Init layout. */
    private void initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        JPanel p = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        p.add(new JLabel("URL Column: "), gbc);
        gbc.gridx += 1;
        p.add(m_columnPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        p.add(m_failOnInvalidChecker, gbc);

        gbc.gridy += 1;
        p.add(new JLabel(), gbc);

        gbc.gridy += 1;
        p.add(m_replaceColumnRadio, gbc);
        gbc.gridy += 1;
        p.add(m_appendColumnRadio, gbc);
        gbc.gridx += 1;
        p.add(m_appendColumnField, gbc);
        addTab("PNG Read Config", p);
    }


    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        ReadPNGFromURLConfig config = new ReadPNGFromURLConfig();
        config.loadInDialog(settings, specs[0]);
        m_columnPanel.update(specs[0], config.getUrlColName());
        m_failOnInvalidChecker.setSelected(config.isFailOnInvalid());
        String newColName = config.getNewColumnName();
        if (newColName == null) {
            m_replaceColumnRadio.doClick();
        } else {
            m_appendColumnRadio.doClick();
            m_appendColumnField.setText(newColName);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        ReadPNGFromURLConfig config = new ReadPNGFromURLConfig();
        config.setUrlColName(m_columnPanel.getSelectedColumn());
        config.setFailOnInvalid(m_failOnInvalidChecker.isSelected());
        if (m_replaceColumnRadio.isSelected()) {
            config.setNewColumnName(null);
        } else {
            config.setNewColumnName(m_appendColumnField.getText());
        }
        config.save(settings);
    }

}
