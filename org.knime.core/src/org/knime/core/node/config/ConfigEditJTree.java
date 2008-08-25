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
 *   Mar 29, 2008 (wiswedel): created
 */
package org.knime.core.node.config;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.ScopeObjectStack;

/**
 * A tree implementation that allows one to overwrite certain node settings 
 * using flow variables (called scope variables).
 * 
 * <p>This class is not meant for public use.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ConfigEditJTree extends JTree {
    
    /** Fallback model. */
    private static final ConfigEditTreeModel EMPTY_MODEL =
        ConfigEditTreeModel.create(new NodeSettings("empty"));
    
    /** To get the available variables from. */
    private ScopeObjectStack m_scopeStack;
    
    /** Constructor for empty tree. */
    public ConfigEditJTree() {
        this(EMPTY_MODEL);
    }
    
    /** Shows given tree model.
     * @param model The model to show. */
    public ConfigEditJTree(final ConfigEditTreeModel model) {
        super(model);
        setRootVisible(false);
        setShowsRootHandles(true);
        ConfigEditTreeRenderer renderer = new ConfigEditTreeRenderer();
        setCellRenderer(renderer);
        setCellEditor(new ConfigEditTreeEditor(this, renderer));
        setRowHeight(renderer.getPreferredSize().height);
        setEditable(true);
        setToolTipText("config tree"); // enable tooltip
    }
    
    /** Overwritten to fail on model implementations which are not of class
     * {@link ConfigEditTreeModel}.
     * {@inheritDoc} */
    @Override
    public void setModel(final TreeModel newModel) {
        if (!(newModel instanceof ConfigEditTreeModel)) {
            throw new IllegalArgumentException("Argument must be of class "
                    + ConfigEditTreeModel.class.getSimpleName());
        }
        super.setModel(newModel);
        expandAll();
    }
    
    /** Expand the tree. */
    public void expandAll() {
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public ConfigEditTreeModel getModel() {
        return (ConfigEditTreeModel)super.getModel();
    }
    
    /** @param scopeStack the scopeStack to set */
    public void setScopeStack(final ScopeObjectStack scopeStack) {
        m_scopeStack = scopeStack;
    }
    
    /** @return the scopeStack */
    public ScopeObjectStack getScopeStack() {
        return m_scopeStack;
    }
    
    /** Public testing method that displays a simple tree with no scope 
     * variable stack, though.
     * @param args command line args, ignored here. */
    public static void main(final String[] args) {
        NodeSettings settings = new NodeSettings("Demo");
        settings.addString("String_Demo", "This is a demo string");
        settings.addInt("Int_Demo", 32);
        settings.addDoubleArray("DoubleArray_Demo", new double[]{3.2, 97.4});
        NodeSettingsWO sub = settings.addNodeSettings("SubElement_Demo");
        sub.addString("String_Demo", "Yet another string");
        sub.addString("String_Demo2", "One more");
        JFrame frame = new JFrame("Tree View Demo");
        Container content = frame.getContentPane();
        ConfigEditTreeModel treeModel = ConfigEditTreeModel.create(settings);
        ConfigEditJTree tree = new ConfigEditJTree(treeModel);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(tree), BorderLayout.CENTER);
        content.add(p);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
