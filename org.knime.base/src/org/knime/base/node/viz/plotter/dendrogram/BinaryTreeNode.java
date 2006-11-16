/* -------------------------------------------------------------------
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
 *   12.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.dendrogram;


/**
 * @param <T> the type of the nodes content.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BinaryTreeNode<T> {
    


    private final T m_content;
    
    private BinaryTreeNode<T> m_parent;
    
    private BinaryTreeNode<T> m_leftChild;
    
    private BinaryTreeNode<T> m_rightChild;
    

    /**
     * 
     * @param content the content of the node.
     */
    public BinaryTreeNode(final T content) {
        m_content = content;
    }
    
    /**
     * 
     * @return the node's content.
     */
    public T getContent() {
        return m_content;
    }
    
    /**
     * 
     * @return the parent of the node.
     */
    public BinaryTreeNode<T> getParent() {
        return m_parent;
    }
    
    /**
     * 
     * @param parent the parent of this node.
     */
    public void setParent(final BinaryTreeNode<T> parent) {
        if (m_parent != null) {
            throw new IllegalArgumentException("Parent node is already set: "
                    + m_parent);
        }
        m_parent = parent;
    }
    
    /**
     * 
     * @return the left child of this node.
     */
    public BinaryTreeNode<T> getLeftChild() {
        return m_leftChild;
    }
    
    /**
     * 
     * @param leftChild the left child of this node.
     */
    public void setLeftChild(final BinaryTreeNode<T> leftChild) {
        if (m_leftChild != null) {
            throw new IllegalArgumentException(
                    "left child node is already set: " + m_leftChild);
        }
        m_leftChild = leftChild;
    }
    
    /**
     * 
     * @return the right child of this node.
     */
    public BinaryTreeNode<T> getRightChild() {
        return m_rightChild;
    }
    
    /**
     * 
     * @param rightChild the right child.
     */
    public void setRightChild(final BinaryTreeNode<T> rightChild) {
        if (m_rightChild != null) {
            throw new IllegalArgumentException(
                    "Right child node is already set: " + m_rightChild);
        }
        m_rightChild = rightChild;
    }
    
    /**
     * 
     * @return true if this node is a leaf node.
     */
    public boolean isLeaf() {
        return getLeftChild() == null && getRightChild() == null;
    }
    
    /**
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "content: " + m_content.toString()
        + " left: " + m_leftChild + " right: " + m_rightChild;
    }
}
