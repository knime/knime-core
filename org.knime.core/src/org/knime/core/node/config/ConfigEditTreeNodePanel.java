/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
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
    
    private static final Icon ICON_STRING = StringValue.UTILITY.getIcon();
    private static final Icon ICON_INT = IntValue.UTILITY.getIcon();
    private static final Icon ICON_DOUBLE = DoubleValue.UTILITY.getIcon();
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
        m_valueField.setPrototypeDisplayValue("xxxxxxxxxxxxxxxx");
        FocusListener l = new FocusAdapter() {
            /** {@inheritDoc} */
            @Override
            public void focusLost(final FocusEvent e) {
                commit();
            } 
        };
        m_valueField.addFocusListener(l);
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
            usedVariable = m_treeNode.getUseVariableName();
            String exposeVariable = m_treeNode.getExposeVariableName();
            m_exposeAsVariableField.setText(exposeVariable);
        } else {
            selType = Type.STRING;
            m_keyLabel.setText("");
            m_keyIcon = ICON_UNKNOWN;
            m_exposeAsVariableField.setText("");
            usedVariable = null;
        }

        setToolTipText(m_keyLabel.getText());
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
        ScopeVariable match = null;
        for (ScopeVariable v : allVars) {
            boolean isOk = false;
            switch (selType) {
            // case order is important here:
            // string accepts also double and integer,
            // double accepts integer, integer only accepts integer
            case STRING:
                isOk = true;
            case DOUBLE:
                isOk |= v.getType().equals(Type.DOUBLE);
            case INTEGER:
                isOk |= v.getType().equals(Type.INTEGER);
            }
            if (isOk) {
                model.addElement(v);
                if (v.getName().equals(usedVariable)) {
                    match = v; 
                }
            }
        }
        if (match != null) {
            m_valueField.setSelectedItem(match);
        } 
        m_valueField.setEnabled(model.getSize() > 1);
    }
    
    /** Write the currently edited values to the underlying model. */
    public void commit() {
        if (m_treeNode == null) {
            return;
        }
        String v = null;
        Object selVar = m_valueField.getSelectedItem();
        if (selVar instanceof ScopeVariable) {
            v = ((ScopeVariable)selVar).getName();
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
    
    /** Renderer for the combo box. */
    private static final class ComboBoxRenderer 
        extends DefaultListCellRenderer {
        
        /** Instance to be used. */
        static final ComboBoxRenderer INSTANCE = new ComboBoxRenderer();
        
        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list, 
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof ScopeVariable) {
                ScopeVariable v = (ScopeVariable)value;
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
                setToolTipText(v.getName() + " (currently \"" 
                        + curValue + "\")");
            } else {
                setToolTipText(null);
            }
            return c;
        }
    }
    
}
