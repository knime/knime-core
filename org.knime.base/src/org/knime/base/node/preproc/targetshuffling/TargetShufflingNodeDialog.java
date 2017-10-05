/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.targetshuffling;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This is the dialog for the y-scrambling node.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Tim-Oliver Buchholz, University of Konstanz
 */
class TargetShufflingNodeDialog extends NodeDialogPane {
    private final TargetShufflingSettings m_settings = new TargetShufflingSettings();

    private final ColumnSelectionComboxBox m_column = new ColumnSelectionComboxBox((Border)null, DataValue.class);

    private final JCheckBox m_useSeed = new JCheckBox("Use seed");

    private final JButton m_drawSeed = new JButton("Draw seed");

    private JFormattedTextField m_seed;

    /**
     * Creates a new dialog for the y-scrambling node.
     */
    public TargetShufflingNodeDialog() {

        m_seed = new JFormattedTextField(new AbstractFormatter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Object stringToValue(final String text) throws ParseException {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException nfe) {
                    throw new ParseException("Contains non-numeric chars", 0);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String valueToString(final Object value) throws ParseException {
                return value == null ? null : value.toString();
            }
        });
        m_seed.setColumns(16);
        m_seed.setPreferredSize(new Dimension(m_seed.getPreferredSize().width, m_drawSeed.getPreferredSize().height));

        m_drawSeed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                long l1 = Double.doubleToLongBits(Math.random());
                long l2 = Double.doubleToLongBits(Math.random());
                long l = ((0xFFFFFFFFL & l1) << 32) + (0xFFFFFFFFL & l2);
                m_seed.setText(Long.toString(l));
            }
        });

        m_useSeed.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                m_drawSeed.setEnabled(m_useSeed.isSelected());
                m_seed.setEnabled(m_useSeed.isSelected());
            }
        });

        m_drawSeed.setEnabled(m_useSeed.isSelected());
        m_seed.setEnabled(m_useSeed.isSelected());

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;
        p.add(new JLabel("Column name"), c);
        c.gridx++;
        c.anchor = GridBagConstraints.WEST;
        p.add(m_column, c);

        c.gridy++;
        c.gridx = 0;
        p.add(m_useSeed, c);

        c.gridx++;
        p.add(m_seed, c);

        c.gridx++;
        p.add(m_drawSeed, c);

        addTab("Standard settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        try {
            m_settings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // ignore it and use default settings
        }

        m_column.update(specs[0], m_settings.columnName());

        m_seed.setText(Long.toString(m_settings.getSeed()));
        m_useSeed.setSelected(m_settings.getUseSeed());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.columnName(m_column.getSelectedColumn());

        try {
            String t = m_seed.getText();

            m_settings.setSeed(Long.parseLong(t));
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("Can't parse seed as number.");
        }

        m_settings.setUseSeed(m_useSeed.isSelected());
        m_settings.saveSettingsTo(settings);
    }
}
