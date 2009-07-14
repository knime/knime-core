/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   14.07.2009 (mb): created
 */
package org.knime.core.node;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



/** Button for a WorkflowVariableModel, launching a dialog which allows to
 * control the settings.
 *
 * This allows NodeDialogPane implementations to easily use Variables
 * for individual options.
 * 
 * @author Michael Berthold, University of Konstanz
 */
@SuppressWarnings("serial")
public class WorkflowVariableModelButton extends JButton
implements ChangeListener, ActionListener {

    /* remember underlying model (to track changes) */
    private WorkflowVariableModel m_model;

    /**
     * @param wvm the underlying model
     */
    public WorkflowVariableModelButton(
            final WorkflowVariableModel wvm) {
        m_model = wvm;
        // add us as listener for changes to the WorkflowVariableModel
        wvm.addChangeListener(this);
        // add us as listener for actions on the underlying JButton
        this.addActionListener(this);
        // and make sure we start with the right button layout.
        stateChanged(null);
    }

    /** React to state changes in the underlying WorkflowVariableModel
     * and set tool tip accordingly.
     * 
     * @param evt event
     */
    @Override
    public void stateChanged(final ChangeEvent evt) {
        boolean enabled = m_model.isVariableReplacementEnabled();
        this.setToolTipText(enabled ? m_model.getInputVariableName() : "N/A");
        try {
            // try to load icon(s)
            ImageIcon icon;
            ClassLoader loader = this.getClass().getClassLoader(); 
            String packagePath = 
                this.getClass().getPackage().getName().replace('.', '/');
            String correctedPath = "/icon/"
                + (enabled ? "variable_dialog_active.png"
                            : "variable_dialog_inactive.png");
            icon = new ImageIcon(
                    loader.getResource(packagePath + correctedPath));
            this.setText("");
            this.setBorder(new LineBorder(Color.gray, 0));
            this.setIcon(icon);
        } catch (Exception e) {
            this.setText(enabled ? "v!" : "v?");
            return;
        }
    }

    /** React to clicks on the underlying button: open dialog which enables
     * the user to change the underlying settings.
     * 
     * @param e event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        Object[] comps = new Object[4];
        comps[0] = new JLabel("Input");
        comps[1] = new JComboBox(m_model.getMatchingVariables());
        comps[2] = new JLabel("Output");
        comps[3] = new JTextField();
        JOptionPane.showInputDialog(this, comps);
        Object[] selObs = ((JComboBox)comps[1]).getSelectedObjects();
        m_model.setInputVariableName(selObs == null || selObs.length == 0
                ? null : (selObs[0] == null ? null : selObs[0].toString()));
        m_model.setOutputVariableName(((JTextField)comps[3]).getText());
    }
    

}
