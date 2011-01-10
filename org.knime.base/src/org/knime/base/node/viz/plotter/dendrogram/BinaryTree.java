/*
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
 * History
 *   12.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.dendrogram;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <T> the type of the nodes content.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BinaryTree<T> {
    
    /**
     * Tree traversal methods.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public enum Traversal {
        /** Constant for preorder traversal. */
        PRE,
        /** Constant for postorder traversal. */
        POST,
        /** Constant for inorder traversal. */
        IN,
        /** Constant for levelorder traversal. */
        //LEVEL
    }
    
    
    private final BinaryTreeNode<T> m_root;
    
    /**
     * 
     * @param root the root node of this tree.
     */
    public BinaryTree(final BinaryTreeNode<T> root) {
        m_root = root;
    }
    
    
    /**
     * Returns the nodes of this tree as a ordered list, where the order 
     * depends on the traversal type defined in the enum {@link Traversal}.
     * @param traversal the traversal method to use.
     * @return an ordered list of the nodes of this tree
     */
    public List<BinaryTreeNode<T>> getNodes(final Traversal traversal) {
        List<BinaryTreeNode<T>> list = new ArrayList<BinaryTreeNode<T>>();
        if (traversal.equals(Traversal.PRE)) {
            preorder(list, m_root);
        } else if (traversal.equals(Traversal.POST)) {
            postorder(list, m_root);
        } else if (traversal.equals(Traversal.IN)) {
            inorder(list, m_root);
        }
        return list;
    }
    
    /**
     * Adds a node to this tree. The tree is build up level by level from 
     * left to right.
     * 
     * @param newNode the node to add.
     */
    public void addNode(final BinaryTreeNode<T> newNode) {
        List<BinaryTreeNode<T>> nodes = new ArrayList<BinaryTreeNode<T>>(); 
            preorder(nodes, m_root);
            for (BinaryTreeNode<T> node : nodes) {
//                System.out.println("node: " + node);
                if (addNode(node, newNode)) {
//                    System.out.println("added: " + newNode);
//                    System.out.println("looks like: " + node);
                    break;
                }
            }
    }
    
    /**
     * Adds the passed new node to the node as a left child 
     * (if this position is free) or as the right child if this position is 
     * free. Returns true if a node can be added, false otherwise. 
     * 
     * @param node the current node to test.
     * @param newNode the node to add
     * @return true if the node could be added, false otherwise.
     */
    private boolean addNode(final BinaryTreeNode<T> node,
            final BinaryTreeNode<T>newNode) {
        if (node.getLeftChild() != null && node.getRightChild() == null) {
            newNode.setParent(node);
            node.setRightChild(newNode);
            return true;  
        }
        if (node.getLeftChild() == null) {
            newNode.setParent(node);
            node.setLeftChild(newNode);
            return true;
        } 
        return false;
    }
    
    
    private void preorder(final List<BinaryTreeNode<T>> list, 
            final BinaryTreeNode<T> node) {
        if (node == null) {
            return;
        }
        list.add(node);
        preorder(list, node.getLeftChild());
        preorder(list, node.getRightChild());
    }

    private void postorder(final List<BinaryTreeNode<T>> list, 
            final BinaryTreeNode<T> node) {
        if (node == null) {
            return;
        }
        postorder(list, node.getLeftChild());
        postorder(list, node.getRightChild());
        list.add(node);
    }
    
    
    private void inorder(final List<BinaryTreeNode<T>> list, 
            final BinaryTreeNode<T> node) {
        if (node == null) {
            return;
        }
        inorder(list, node.getLeftChild());
        list.add(node);
        inorder(list, node.getRightChild());
    }
   
}
