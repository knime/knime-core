/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   12.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.dendrogram;


/**
 * A generic tree with a content, a reference to the parent node and with a left
 * child and a right child. Everything might be <code>null</code> except of the
 * content, i.e. the children and the parent might be added later but only 
 * once. 
 * 
 * Nodes without children are considered ot be leaf nodes, i.e. 
 * {@link org.knime.base.node.viz.plotter.dendrogram.BinaryTreeNode#isLeaf()} 
 * returns <code>true</code>.
 * 
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
     * The content of a <code>BinaryTreeNode</code> is final and is set only 
     * once.
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
     * The parent of the node can be set only once, if the parent is already set
     * an {@link java.lang.IllegalArgumentException} is thrown.
     *  
     * @param parent the parent of this node.
     * @throws IllegalArgumentException if the parent is already set.
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
     * The parent of the node can be set only once, if the left child is 
     * already set an {@link java.lang.IllegalArgumentException} is thrown.
     * 
     * @param leftChild the left child of this node
     * @throws IllegalArgumentException if the left child is already set.
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
     * * The parent of the node can be set only once, if the right child is 
     * already set an {@link java.lang.IllegalArgumentException} is thrown.
     * 
     * @param rightChild the right child.
     * @throws IllegalArgumentException if the right child is already set.
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "content: " + m_content.toString()
        + " left: " + m_leftChild + " right: " + m_rightChild;
    }
}
