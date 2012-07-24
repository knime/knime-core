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
package org.knime.base.node.preproc.equalsizesampling;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.base.node.preproc.equalsizesampling.EqualSizeSamplingConfiguration.SamplingMethod;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class EqualSizeSamplingNodeDialogPane extends NodeDialogPane {
    private final JTextField m_seedField;
    private final JCheckBox m_seedEnableChecker;
    private final JRadioButton m_exactSamplingChecker;
    private final JRadioButton m_approximateSamplingChecker;
    private final ColumnSelectionComboxBox m_classColumnBox;
    private final JButton m_newSeedButton;
    private final Random m_random = new Random();

    /**  */
    public EqualSizeSamplingNodeDialogPane() {
        m_classColumnBox = new ColumnSelectionComboxBox(
                (Border)null, NominalValue.class);
        m_seedField = new JTextField(20);
        m_newSeedButton = new JButton("New Seed");
        m_newSeedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_seedField.setText(Long.toString(m_random.nextLong()));
            }
        });
        m_seedEnableChecker = new JCheckBox("Enable static seed");
        m_seedEnableChecker.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean selected = m_seedEnableChecker.isSelected();
                m_newSeedButton.setEnabled(selected);
                m_seedField.setEnabled(selected);
            }
        });
        m_seedEnableChecker.doClick();
        m_exactSamplingChecker = new JRadioButton("Use exact sampling");
        m_exactSamplingChecker.setToolTipText("memory expensive for larger "
                + "data with many classes)");
        m_approximateSamplingChecker = new JRadioButton(
                "Use approximate sampling");
        m_approximateSamplingChecker.setToolTipText(
                "not exact but less memory expensive");
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_exactSamplingChecker);
        bg.add(m_approximateSamplingChecker);
        m_exactSamplingChecker.doClick();
        addTab("Configuration", initLayout());
    }

    /**
     * @return */
    private JPanel initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Nominal column"), gbc);
        gbc.gridx += 1;
        gbc.gridwidth = 2;
        panel.add(m_classColumnBox, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 3;
        panel.add(m_exactSamplingChecker, gbc);
        gbc.gridy += 1;
        panel.add(m_approximateSamplingChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 1;
        panel.add(m_seedEnableChecker, gbc);
        gbc.gridx += 1;
        panel.add(m_seedField, gbc);
        gbc.gridx += 1;
        panel.add(m_newSeedButton, gbc);
        return panel;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        EqualSizeSamplingConfiguration c = new EqualSizeSamplingConfiguration();
        c.loadConfigurationInDialog(settings, specs[0]);
        m_classColumnBox.update(specs[0], c.getClassColumn());
        Long seed = c.getSeed();
        if (seed != null) {
            if (!m_seedEnableChecker.isSelected()) {
                m_seedEnableChecker.doClick();
            }
            m_seedField.setText(Long.toString(seed));
        } else {
            if (m_seedEnableChecker.isSelected()) {
                m_seedEnableChecker.doClick();
            }
            m_seedField.setText("");
        }
        switch (c.getSamplingMethod()) {
        case Approximate:
            m_approximateSamplingChecker.doClick();
            break;
        default:
            m_exactSamplingChecker.doClick();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        EqualSizeSamplingConfiguration c = new EqualSizeSamplingConfiguration();
        c.setClassColumn(m_classColumnBox.getSelectedColumn());
        if (m_seedEnableChecker.isSelected()) {
            String seedS = m_seedField.getText();
            try {
                c.setSeed(Long.parseLong(seedS));
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Can't parse seed \""
                        + seedS + "\", it's not a number", nfe);
            }
        } else {
            c.setSeed(null);
        }
        SamplingMethod m = SamplingMethod.Exact;
        if (m_approximateSamplingChecker.isSelected()) {
            m = SamplingMethod.Approximate;
        }
        c.setSamplingMethod(m);
        c.saveConfiguration(settings);
    }

}
