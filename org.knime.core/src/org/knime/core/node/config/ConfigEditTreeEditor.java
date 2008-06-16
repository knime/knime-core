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
import java.util.EventObject;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;

import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
import org.knime.core.node.workflow.ScopeObjectStack;

/** Editor component for {@link ConfigEditJTree} implementation.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ConfigEditTreeEditor extends DefaultTreeCellEditor {

    /** Constructs new tree editor.
     * @param myTree associated tree.
     * @param myRenderer associated renderer.
     * @see DefaultTreeCellEditor#DefaultTreeCellEditor(
     * JTree, DefaultTreeCellRenderer)
     */
    public ConfigEditTreeEditor(final ConfigEditJTree myTree, 
            final ConfigEditTreeRenderer myRenderer) {
        super(myTree, myRenderer);
    }
    
    /** {@inheritDoc} */
    @Override
    public Component getTreeCellEditorComponent(final JTree myTree, 
            final Object value, final boolean isSelected, 
            final boolean expanded, final boolean leaf, final int row) {
        ((ConfigEditTreeRenderer)super.renderer).setValue(myTree, value);
        return super.getTreeCellEditorComponent(
                myTree, value, isSelected, expanded, leaf, row);
    }
    
    /** {@inheritDoc} */
    @Override
    protected TreeCellEditor createTreeCellEditor() {
        return new ComponentCreator();
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean canEditImmediately(final EventObject event) {
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isCellEditable(final EventObject event) {
        return true;
    }
    
    /** Factory editor to create the {@link ConfigEditTreeNodePanel}. See
     * class description of {@link DefaultTreeCellEditor} for details. */
    private final class ComponentCreator extends DefaultTreeCellEditor {
        
        private final ConfigEditTreeNodePanel m_panelPlain;
        private final ConfigEditTreeNodePanel m_panelFull;
        private ConfigEditTreeNodePanel m_active;
        
        private ComponentCreator() {
            super(new JTree(), new DefaultTreeCellRenderer());
            m_panelPlain = new ConfigEditTreeNodePanel(false);
            m_panelFull = new ConfigEditTreeNodePanel(true);
            m_active = m_panelPlain;
        }

        /** {@inheritDoc} */
        @Override
        public Component getTreeCellEditorComponent(final JTree myTree, 
                final Object value, final boolean isSelected, 
                final boolean expanded, final boolean leaf, final int row) {
            if (value instanceof ConfigEditTreeNode) {
                ConfigEditTreeNode node = (ConfigEditTreeNode)value;
                m_active = node.isLeaf() ? m_panelFull : m_panelPlain;
                ScopeObjectStack stack = null;
                JTree outerTree = ConfigEditTreeEditor.this.tree;
                if (outerTree instanceof ConfigEditJTree) {
                    stack = ((ConfigEditJTree)outerTree).getScopeStack();
                }
                m_active.setScopeStack(stack);
                m_active.setTreeNode(node);
                return m_active;
            } else {
                m_active = m_panelPlain; 
                m_active.setTreeNode(null);
                return m_active;
            }
        }
        
        /** {@inheritDoc} */
        @Override
        public void cancelCellEditing() {
            m_active.commit();
            super.cancelCellEditing();
        }
        
        /** {@inheritDoc} */
        @Override
        public boolean stopCellEditing() {
            m_active.commit();
            return super.stopCellEditing();
        }
        
    }
    
}
