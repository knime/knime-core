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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.util.ScopeVariableListCellRenderer;
import org.knime.core.node.workflow.ScopeVariable;



/** Button for a @link ScopeVariableModel, launching a dialog which allows to
 * control the settings.
 *
 * This allows NodeDialogPane implementations to easily use Variables
 * for individual options.
 * 
 * @author Michael Berthold, University of Konstanz
 */
@SuppressWarnings("serial")
public class ScopeVariableModelButton extends JButton
implements ChangeListener, ActionListener {

    /* remember underlying model (to track changes) */
    private ScopeVariableModel m_model;

    /**
     * @param wvm the underlying model
     */
    public ScopeVariableModelButton(
            final ScopeVariableModel wvm) {
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
        ScopeVarEditDialog ved = new ScopeVarEditDialog(parentFrame);

        if (m_model.getInputVariableName() != null) {
            ved.setInputVariableName(m_model.getInputVariableName());
        }
        if (m_model.getOutputVariableName() != null) {
            ved.setOutputVariableName(m_model.getOutputVariableName());
        }
        ved.setLocationRelativeTo(this);
        ved.setVisible(true);
    }

    private class ScopeVarEditDialog extends JDialog {
        
        ScopeVarEditDialog(final Frame f) {
            // set title and make dialog modal
            super(f, "Variable Settings", true);
            // set icon of dialog frame
            ClassLoader loader = this.getClass().getClassLoader(); 
            String packagePath = 
                this.getClass().getPackage().getName().replace('.', '/');
            String correctedPath = "/icon/variable_dialog_active.png";
            ImageIcon icon = new ImageIcon(
                    loader.getResource(packagePath + correctedPath));
            this.setIconImage(icon.getImage());
            // finalize setup
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
            Container cont = this.getContentPane();
            cont.setLayout(new BorderLayout());
            JPanel cp = new JPanel();
            cont.add(cp, BorderLayout.NORTH);
            cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
            cp.setAlignmentY(TOP_ALIGNMENT);
            // top part to use variable for specific settings
            JPanel panelTop = new JPanel();
            panelTop.setAlignmentX(Component.CENTER_ALIGNMENT);
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
            m_inputVar.setRenderer(new ScopeVariableListCellRenderer());
            m_inputVar.setEnabled(false);
            panelTop.add(m_inputVar);
            cp.add(panelTop);
            // middle part to create new variable based on specific settings
            JPanel panelMiddle = new JPanel();
            panelMiddle.setAlignmentX(Component.CENTER_ALIGNMENT);
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
            panelBottom.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.X_AXIS));
            panelBottom.add(Box.createHorizontalGlue());
            m_ok = new JButton("OK");
            m_ok.setAlignmentY(Component.BOTTOM_ALIGNMENT);
            m_ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    // write values back to model
                    if (m_enableInputVar.isSelected()) {
                        m_model.setInputVariableName(
                       ((ScopeVariable)m_inputVar.getSelectedItem()).getName());
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
            panelBottom.add(Box.createRigidArea(new Dimension(10, 0)));
            m_cancel = new JButton("Cancel");
            m_cancel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
            m_cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    // do nothing here!
                    setVisible(false);
                }
            });
            panelBottom.add(m_cancel);
            cp.add(Box.createRigidArea(new Dimension(0, 5)));
            cp.add(panelBottom);
            cp.add(Box.createVerticalGlue());
        }
        
        void setInputVariableName(final String s) {
            m_enableInputVar.setSelected(true);
            m_inputVar.setEnabled(true);
            // try to find variable with corresponding name (and type):
            ScopeVariable var = null;
            for (ScopeVariable v : m_model.getMatchingVariables()) {
                if (v.getName().equals(s)) {
                    var = v;
                    break;
                }
            }
            if (var != null) {
                // found it: we can select it. Everything fine.
                m_inputVar.setSelectedItem(var);
            } else {
                // could not find it: add a non-selectable entry to
                // the list so the user knows what's wrong. KNIME will
                // complain during configure anyway and let her know
                // that the variable does not exist (anymore).
                m_inputVar.addItem(s);
                m_inputVar.setRenderer(new CustomListCellRenderer(s));
                m_inputVar.setSelectedItem(s);
                m_inputVar.addActionListener(new ActionListener() {
                    private Object m_oldSelection = null;
                    public void actionPerformed(final ActionEvent evt) {
                        Object o = m_inputVar.getSelectedItem();
                        if (o.equals(s)) {
                            if (!o.equals(m_oldSelection)) {
                                // only try to select the previous (hopefully
                                // legal) selection if it was different!
                                m_inputVar.setSelectedItem(m_oldSelection);
                            }
                        } else {
                            m_oldSelection = o;
                        }
                    }
                });
            }
        }

        void setOutputVariableName(final String s) {
            m_enableOutputVar.setSelected(true);
            m_outputVar.setEnabled(true);
            m_outputVar.setText(s);
        }
        
    }

    /** Helper class to allow also the display of disabled list elements. */
    class CustomListCellRenderer extends ScopeVariableListCellRenderer {
        private String m_toDisable;
        
        /** Create new render which disables given string.
         * 
         * @param s string to disable
         */
        CustomListCellRenderer(final String s) {
            m_toDisable = s;
        }

        /** {@inheritDoc} */
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            Component comp = super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
            if (value.toString().equals(m_toDisable)) {
                comp.setFocusable(false);
                comp.setEnabled(false);
            }
            return comp;
        }
    }
}
