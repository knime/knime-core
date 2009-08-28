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
 *   Apr 4, 2008 (wiswedel): created
 */
package org.knime.core.node.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.core.data.DataValue;
import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.ScopeVariableListCellRenderer;
import org.knime.core.node.workflow.ScopeObjectStack;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.node.workflow.ScopeVariable.Type;

/**
 * Panel that displays a single line/element of a {@link ConfigEditJTree}.
 * It is composed of a label showing the property name, a combo box to select
 * the overwriting variable from and a textfield to enter a new variable name
 * in case the property should be exposed.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ConfigEditTreeNodePanel extends JPanel {
    
    private static final Icon ICON_STRING = 
        ScopeVariableListCellRenderer.SCOPE_VAR_STRING_ICON;
    private static final Icon ICON_INT = 
        ScopeVariableListCellRenderer.SCOPE_VAR_INT_ICON;
    private static final Icon ICON_DOUBLE = 
        ScopeVariableListCellRenderer.SCOPE_VAR_DOUBLE_ICON;
    private static final Icon ICON_UNKNOWN = DataValue.UTILITY.getIcon();
    
    private static final Dimension LABEL_DIMENSION = new Dimension(100, 20);
    private final JLabel m_keyLabel;
    private Icon m_keyIcon;
    private final JComboBox m_valueField;
    private ScopeObjectStack m_scopeObjectStack;
    private final JTextField m_exposeAsVariableField;
    private ConfigEditTreeNode m_treeNode;
    
    /** Constructs new panel.
     * @param isForConfig if true, the combo box and the textfield are not
     * shown (configs can't be overwritten, nor be exported as variable).
     */
    public ConfigEditTreeNodePanel(final boolean isForConfig) {
        super(new FlowLayout());
        m_keyLabel = new JLabel();
        m_keyLabel.setMinimumSize(LABEL_DIMENSION);
        m_keyLabel.setMaximumSize(LABEL_DIMENSION);
        m_keyLabel.setPreferredSize(LABEL_DIMENSION);
        m_keyLabel.setSize(LABEL_DIMENSION);
        m_valueField = new JComboBox(new DefaultComboBoxModel());
        m_valueField.setToolTipText(" "); // enable tooltip;
        m_valueField.setRenderer(ComboBoxRenderer.INSTANCE);
        m_valueField.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxx");
        FocusListener l = new FocusAdapter() {
            /** {@inheritDoc} */
            @Override
            public void focusLost(final FocusEvent e) {
                commit();
            } 
        };
        m_valueField.addFocusListener(l);
        m_valueField.addItemListener(new ItemListener() {
            /** {@inheritDoc} */
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    onSelectedItemChange(e.getItem());
                }
            }
        });
        m_exposeAsVariableField = new JTextField(8);
        m_exposeAsVariableField.addFocusListener(l);
        add(m_keyLabel);
        if (isForConfig) {
            add(m_valueField);
            add(m_exposeAsVariableField);
        }
    }
    
    /** Set a new tree node to display.
     * @param treeNode the new node to represent (may be null).
     */
    public void setTreeNode(final ConfigEditTreeNode treeNode) {
        m_treeNode = treeNode;
        boolean isEditable = m_treeNode != null && m_treeNode.isLeaf();
        Type selType;
        String usedVariable;
        m_valueField.setEnabled(isEditable);
        if (m_treeNode != null) {
            AbstractConfigEntry entry = treeNode.getConfigEntry();
            switch (entry.getType()) {
            case xbyte:
            case xlong:
            case xshort:
            case xint:
                selType = Type.INTEGER;
                break;
            case xdouble:
            case xfloat:
                selType = Type.DOUBLE;
                break;
            default:
                selType = Type.STRING;
            }
            Icon icon;
            switch (entry.getType()) {
            case xstring:
                icon = ICON_STRING;
                break;
            case xdouble:
                icon = ICON_DOUBLE;
                break;
            case xint:
                icon = ICON_INT;
                break;
            default:
                icon = ICON_UNKNOWN;
            }
            m_keyIcon = icon;
            m_keyLabel.setText(entry.getKey());
            m_keyLabel.setToolTipText(entry.getKey());
            usedVariable = m_treeNode.getUseVariableName();
            String exposeVariable = m_treeNode.getExposeVariableName();
            m_exposeAsVariableField.setText(exposeVariable);
        } else {
            selType = Type.STRING;
            m_keyLabel.setText("");
            m_keyLabel.setToolTipText(null);
            m_keyIcon = ICON_UNKNOWN;
            m_exposeAsVariableField.setText("");
            usedVariable = null;
        }

        m_keyLabel.setMinimumSize(LABEL_DIMENSION);
        m_keyLabel.setMaximumSize(LABEL_DIMENSION);
        m_keyLabel.setPreferredSize(LABEL_DIMENSION);
        m_keyLabel.setSize(LABEL_DIMENSION);
        DefaultComboBoxModel model = 
            (DefaultComboBoxModel)m_valueField.getModel();
        model.removeAllElements();
        model.addElement(" ");
        @SuppressWarnings("unchecked")
        Collection<ScopeVariable> allVars = getScopeStack() != null
            ? getScopeStack().getAvailableVariables().values() 
            : (Collection<ScopeVariable>)Collections.EMPTY_LIST;
        ComboBoxElement match = null;
        for (ScopeVariable v : allVars) {
            boolean isOk = ConfigEditTreeModel.doesTypeAccept(
                    selType, v.getType());
            if (isOk) {
                ComboBoxElement cbe = new ComboBoxElement(v);
                model.addElement(cbe);
                if (v.getName().equals(usedVariable)) {
                    match = cbe;
                }
            } else if (v.getName().equals(usedVariable)) {
                String error = "Variable \"" + usedVariable 
                + "\" has wrong type (" + v.getType() 
                + "), expected " + selType;
                ComboBoxElement cbe = new ComboBoxElement(v, error);
                model.addElement(cbe);
                match = cbe;
            }
        }
        if (match != null) {
            m_valueField.setSelectedItem(match);
        } else if (usedVariable != null) {
            String error = "Invalid variable \"" + usedVariable + "\"";
            ScopeVariable virtualVar;
            switch (selType) {
            case DOUBLE: 
                virtualVar = new ScopeVariable(usedVariable, 0.0);
                break;
            case INTEGER: 
                virtualVar = new ScopeVariable(usedVariable, 0);
                break;
            default:
                virtualVar = new ScopeVariable(usedVariable, "");
                break;
            }
            ComboBoxElement cbe = new ComboBoxElement(virtualVar, error);
            model.addElement(cbe);
            m_valueField.setSelectedItem(cbe);
        }
        m_valueField.setEnabled(model.getSize() > 1);
    }
    
    private void onSelectedItemChange(final Object newItem) {
        String newToolTip;
        if (newItem instanceof ComboBoxElement) {
            ComboBoxElement cbe = (ComboBoxElement)newItem;
            if (cbe.m_errorString != null) {
                newToolTip = cbe.m_errorString;
            } else {
                newToolTip = m_keyLabel.getText();
            }
        } else {
            newToolTip = m_keyLabel.getText();
        }
        String oldToolTip = getToolTipText();
        if (!ConvenienceMethods.areEqual(oldToolTip, newToolTip)) {
            setToolTipText(newToolTip);
        }
    }
    
    /** Write the currently edited values to the underlying model. */
    public void commit() {
        if (m_treeNode == null) {
            return;
        }
        String v = null;
        Object selVar = m_valueField.getSelectedItem();
        if (selVar instanceof ComboBoxElement) {
            ComboBoxElement cbe = (ComboBoxElement)selVar;
            if (cbe.m_errorString == null) {
                v = cbe.m_variable.getName();
            }
        }
        m_treeNode.setUseVariableName(v != null && v.length() > 0 ? v : null);
        v = m_exposeAsVariableField.getText();
        m_treeNode.setExposeVariableName(
                v != null && v.length() > 0 ? v : null);
    }
    
    /** Get icon to this property (string, double, int, unknown).
     * @return representative icon.
     */
    public Icon getIcon() {
        return m_keyIcon;
    }
    
    /**
     * @param scopeObjectStack the variableStack to set
     */
    public void setScopeStack(final ScopeObjectStack scopeObjectStack) {
        m_scopeObjectStack = scopeObjectStack;
    }
    
    /**
     * @return the variableStack
     */
    public ScopeObjectStack getScopeStack() {
        return m_scopeObjectStack;
    }
    
    /** Elements in the combo box. Used to also indicate errors with the 
     * current selection. */
    private static final class ComboBoxElement {
        private final ScopeVariable m_variable;
        private final String m_errorString;
        
        /** Create ordinary element, without error. */
        private ComboBoxElement(final ScopeVariable v) {
            this(v, null);
        }
        
        /** Creator error element. */
        private ComboBoxElement(final ScopeVariable v, final String error) {
            m_variable = v;
            m_errorString = error;
        }
        
    }
    
    /** Renderer for the combo box. */
    private static final class ComboBoxRenderer 
        extends DefaultListCellRenderer {
        
        /** Instance to be used. */
        static final ComboBoxRenderer INSTANCE = new ComboBoxRenderer();
        
        private final Border m_errBorder = 
            BorderFactory.createLineBorder(Color.RED);
        private final Border m_okBorder =
            BorderFactory.createEmptyBorder(1, 1, 1, 1);
        
        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list, 
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            ((JComponent)c).setBorder(m_okBorder);
            if (value instanceof ComboBoxElement) {
                ComboBoxElement cbe = (ComboBoxElement)value;
                ScopeVariable v = cbe.m_variable;
                Icon icon;
                String curValue;
                switch (v.getType()) {
                case DOUBLE:
                    icon = ICON_DOUBLE;
                    curValue = Double.toString(v.getDoubleValue());
                    break;
                case STRING:
                    icon = ICON_STRING;
                    curValue = v.getStringValue();
                    break;
                case INTEGER:
                    icon = ICON_INT;
                    curValue = Integer.toString(v.getIntValue());
                    break;
                default:
                    assert false : "Unknown type " + v.getType();
                    curValue = "?";
                    icon = ICON_UNKNOWN;
                }
                setIcon(icon);
                setText(v.getName());
                setToolTipText(v.getName() + " (" 
                        + (v.getName().startsWith("knime.") ? "constant " 
                                : "currently ") + "\"" + curValue + "\")");
                if (cbe.m_errorString != null) {
                    ((JComponent)c).setBorder(m_errBorder);
                    setToolTipText(cbe.m_errorString);
                }
            } else {
                setToolTipText(null);
            }
            return c;
        }
    }
    
}
