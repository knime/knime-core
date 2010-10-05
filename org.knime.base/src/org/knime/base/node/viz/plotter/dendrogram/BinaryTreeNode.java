/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
