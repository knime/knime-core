/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
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
             * @see AbstractFormatter#stringToValue(String)
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
             * @see AbstractFormatter#valueToString(Object)
             */
            @Override
            public String valueToString(
                    final Object value) throws ParseException {
                return value == null ? null : value.toString();
            }
        });
        m_seedField.setColumns(8);
        
        m_drawNewSeedButton = new JButton("Draw new seed");
        m_drawNewSeedButton.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
               long l = Double.doubleToLongBits(Math.random());
               m_seedField.setText(Long.toString(l));
            } 
        });
        
        m_enableStaticSeedChecker = new JCheckBox("Enable static seed");
        m_enableStaticSeedChecker.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                checkEnableState();
            }
        });
        JPanel p = new JPanel(new GridLayout(0, 1));
        JPanel flow = new JPanel(new FlowLayout());
        flow.add(m_enableStaticSeedChecker);
        p.add(flow);
        flow = new JPanel(new FlowLayout());
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
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
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
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
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
