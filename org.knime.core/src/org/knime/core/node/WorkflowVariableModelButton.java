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
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
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
        // make sure dialog is modal with respect to the "nearest" frame
        Container c = SwingUtilities.getAncestorOfClass(Frame.class, this);
        Frame parentFrame = (Frame)c;
        VarEditDialog ved = new VarEditDialog(parentFrame);

        if (m_model.getInputVariableName() != null) {
            ved.setInputVariableName(m_model.getInputVariableName());
        }
        if (m_model.getOutputVariableName() != null) {
            ved.setOutputVariableName(m_model.getOutputVariableName());
        }
        ved.setLocationRelativeTo(this);
        ved.setVisible(true);
    }

    private class VarEditDialog extends JDialog {
        
        VarEditDialog(final Frame f) {
            super(f, "Variable Settings", true);
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            initComponents();
            pack();
        }
        
        private JCheckBox m_enableInputVar;
        private JComboBox m_inputVar;
        private JCheckBox m_enableOutputVar;
        private JTextField m_outputVar;
        private JButton m_cancel;
        private JButton m_ok;
        
        private void initComponents() {
            Container cp = this.getContentPane();
            cp.setLayout(new GridLayout(3, 1));
            // top part to use variable for specific settings
            JPanel panelTop = new JPanel();
            panelTop.setBorder(new TitledBorder("Use Variable:"));
            panelTop.setLayout(new GridLayout(1, 2));
            m_enableInputVar = new JCheckBox();
            m_enableInputVar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent evt) {
                    m_inputVar.setEnabled(m_enableInputVar.isSelected());
                }
            });
            panelTop.add(m_enableInputVar);
            m_inputVar = new JComboBox(m_model.getMatchingVariables());
            m_inputVar.setEnabled(false);
            panelTop.add(m_inputVar);
            cp.add(panelTop);
            // middle part to create new variable based on specific settings
            JPanel panelMiddle = new JPanel();
            panelMiddle.setBorder(new TitledBorder("Create Variable:"));
            panelMiddle.setLayout(new GridLayout(1, 2));
            m_enableOutputVar = new JCheckBox();
            m_enableOutputVar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent evt) {
                    m_outputVar.setEnabled(m_enableOutputVar.isSelected());
                }
            });
            panelMiddle.add(m_enableOutputVar);
            m_outputVar = new JTextField();
            m_outputVar.setEnabled(false);
            panelMiddle.add(m_outputVar);
            cp.add(panelMiddle);
            // pane for buttons
            JPanel panelBottom = new JPanel();
            panelBottom.setLayout(new GridLayout(1, 2));
            m_cancel = new JButton("Cancel");
            m_cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    // do nothing here!
                    setVisible(false);
                }
            });
            panelBottom.add(m_cancel);
            m_ok = new JButton("OK");
            m_ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    // write values back to model
                    if (m_enableInputVar.isSelected()) {
                        m_model.setInputVariableName(
                                m_inputVar.getSelectedItem().toString());
                    } else {
                        m_model.setInputVariableName(null);
                    }
                    if (m_enableOutputVar.isSelected()) {
                        m_model.setOutputVariableName(
                                m_outputVar.getText());
                    } else {
                        m_model.setOutputVariableName(null);
                    }
                    setVisible(false);
                }
            });
            panelBottom.add(m_ok);
            cp.add(panelBottom);
        }
        
        void setInputVariableName(final String s) {
            m_enableInputVar.setSelected(true);
            m_inputVar.setEnabled(true);
            m_inputVar.setSelectedItem(s);
        }

        void setOutputVariableName(final String s) {
            m_enableOutputVar.setSelected(true);
            m_outputVar.setEnabled(true);
            m_outputVar.setText(s);
        }
        
    }
    
}
