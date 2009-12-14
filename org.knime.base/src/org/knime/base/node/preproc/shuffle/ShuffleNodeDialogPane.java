/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *   Nov 8, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.shuffle;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JFormattedTextField.AbstractFormatter;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Dialog to enter a seed.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ShuffleNodeDialogPane extends NodeDialogPane {
    
    private final JFormattedTextField m_seedField;
    private final JButton m_drawNewSeedButton;
    private final JCheckBox m_enableStaticSeedChecker;
    
    /** Inits GUI. */
    public ShuffleNodeDialogPane() {
        m_seedField = new JFormattedTextField(new AbstractFormatter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Object stringToValue(
                    final String text) throws ParseException {
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
            public String valueToString(
                    final Object value) throws ParseException {
                return value == null ? null : value.toString();
            }
        });
        m_seedField.setColumns(16);
        
        m_drawNewSeedButton = new JButton("Draw new seed");
        m_drawNewSeedButton.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
               long l1 = Double.doubleToLongBits(Math.random());
               long l2 = Double.doubleToLongBits(Math.random());
               long l = ((0xFFFFFFFFL & l1) << 32)
                   + (0xFFFFFFFFL & l2);
               m_seedField.setText(Long.toString(l));
            } 
        });
        
        m_enableStaticSeedChecker = new JCheckBox("Use seed");
        m_enableStaticSeedChecker.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                checkEnableState();
            }
        });
        JPanel p = new JPanel(new GridLayout(0, 1));
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEADING));
        flow.add(m_enableStaticSeedChecker);
        p.add(flow);
        flow = new JPanel(new FlowLayout(FlowLayout.LEADING));
        flow.add(m_seedField);
        flow.add(m_drawNewSeedButton);
        p.add(flow);
        addTab("Seed", p);
    }
    
    private void checkEnableState() {
        boolean enabled = m_enableStaticSeedChecker.isSelected();
        m_drawNewSeedButton.setEnabled(enabled);
        m_seedField.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        String seed = settings.getString(ShuffleNodeModel.CFG_SEED, null);
        Long lSeed = null;
        if (seed != null) {
            try {
                lSeed = Long.parseLong(seed);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        m_enableStaticSeedChecker.setSelected(lSeed != null);
        if (lSeed != null) {
            m_seedField.setText(Long.toString(lSeed));
        }
        checkEnableState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String seed = null;
        if (m_enableStaticSeedChecker.isSelected()) {
            try {
                String t = m_seedField.getText();
                Long.parseLong(t);
                seed = t;
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException(
                        "Can't parse seed as number.");
            }
        }
        settings.addString(ShuffleNodeModel.CFG_SEED, seed);
    }

}
