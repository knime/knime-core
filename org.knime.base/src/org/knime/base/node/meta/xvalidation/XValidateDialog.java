/* Created on Jun 12, 2006 11:03:30 AM by thor
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.meta.xvalidation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
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
 * This is the simple dialog for the cross validation node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateDialog extends NodeDialogPane {
    private final XValidateSettings m_settings = new XValidateSettings();

    private final JSpinner m_validations =
            new JSpinner(new SpinnerNumberModel(10, 2, 100, 1));

    private final JRadioButton m_linearSampling = new JRadioButton();

    private final JRadioButton m_randomSampling = new JRadioButton();

    private final JRadioButton m_leaveOneOut = new JRadioButton();

    private final JRadioButton m_stratifiedSampling = new JRadioButton();

    private final JLabel m_classColumnLabel = new JLabel("   Class column   ");

    private final ColumnSelectionComboxBox m_classColumn =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);

    private final JCheckBox m_useRandomSeed = new JCheckBox("Random seed   ");

    private final JTextField m_randomSeed = new JTextField(10);

    /**
     * Creates a new dialog for the cross validation settings.
     */
    public XValidateDialog() {
        super();

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 1, 2, 1);
        p.add(new JLabel("Number of validations   "), c);
        c.gridx = 1;
        p.add(m_validations, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Linear sampling   "), c);
        c.gridx = 1;
        p.add(m_linearSampling, c);
        m_linearSampling.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (m_linearSampling.isSelected()) {
                    m_useRandomSeed.setEnabled(false);
                    m_randomSeed.setEnabled(false);
                } else {
                    m_useRandomSeed.setEnabled(true);
                    m_randomSeed.setEnabled(m_useRandomSeed.isSelected());
                }
            }
        });

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Random sampling   "), c);
        c.gridx = 1;
        p.add(m_randomSampling, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Stratified sampling   "), c);
        c.gridx = 1;
        p.add(m_stratifiedSampling, c);
        m_stratifiedSampling.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                boolean b = m_stratifiedSampling.isSelected();
                m_classColumnLabel.setEnabled(b);
                m_classColumn.setEnabled(b);
            }
        });

        c.gridy++;
        c.gridx = 0;
        p.add(m_classColumnLabel, c);
        c.gridx = 1;
        p.add(m_classColumn, c);


        c.gridy++;
        c.gridx = 0;
        p.add(m_useRandomSeed, c);
        m_useRandomSeed.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_randomSeed.setEnabled(m_useRandomSeed.isSelected());
            }
        });
        c.gridx = 1;
        p.add(m_randomSeed, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Leave-one-out   "), c);
        c.gridx = 1;
        p.add(m_leaveOneOut, c);
        m_leaveOneOut.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_validations.setEnabled(!m_leaveOneOut.isSelected());
            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(m_linearSampling);
        bg.add(m_randomSampling);
        bg.add(m_stratifiedSampling);
        bg.add(m_leaveOneOut);

        addTab("Standard settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_validations.setValue(m_settings.validations());
        if (m_settings.randomSampling()) {
            m_randomSampling.setSelected(true);
        } else if (m_settings.stratifiedSampling()) {
            m_stratifiedSampling.setSelected(true);
        } else if (m_settings.leaveOneOut()) {
            m_leaveOneOut.setSelected(true);
            m_validations.setEnabled(false);
        } else {
            m_linearSampling.setSelected(true);
        }
        m_useRandomSeed.setSelected(m_settings.useRandomSeed());
        m_randomSeed.setText(Long.toString(m_settings.randomSeed()));

        m_classColumn.update(specs[0], m_settings.classColumn());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.validations((byte)Math.min(100, ((Number)m_validations
                .getValue()).intValue()));
        m_settings.randomSampling(m_randomSampling.isSelected());
        m_settings.stratifiedSampling(m_stratifiedSampling.isSelected());
        m_settings.leaveOneOut(m_leaveOneOut.isSelected());
        m_settings.classColumn(m_classColumn.getSelectedColumn());
        m_settings.useRandomSeed(m_useRandomSeed.isSelected());
        m_settings.randomSeed(Long.parseLong(m_randomSeed.getText()));
        m_settings.saveSettingsTo(settings);
    }
}
