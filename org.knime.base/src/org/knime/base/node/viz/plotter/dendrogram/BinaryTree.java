/*
 * ------------------------------------------------------------------
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
