/* 
 * -------------------------------------------------------------------
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
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

/**
 * Abstract base implementation of a generic repository object.
 * 
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractRepositoryObject implements IRepositoryObject,
        Comparable<AbstractRepositoryObject> {
    private IContainerObject m_parent;

    private String m_name;

    private String m_id;
    
    private String m_afterID;

    private boolean m_isExpertNode;
    
    /**
     * Default implementation, provides no adapters.
     * 
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(final Class adapter) {
        return null;
    }

    /**
     * Sets the parent. Make sure to remove childn references on old parent as
     * well !
     * 
     * @param parent The parent
     */
    public void setParent(final IContainerObject parent) {
        if (parent == this) {
            throw new IllegalArgumentException("can't set parent to 'this'");
        }
        if (parent == null) {
            throw new IllegalArgumentException("can't set parent to 'null'");
        }

        m_parent = parent;
    }

    /**
     * @return returns the parent object
     * 
     * @see org.knime.workbench.repository.model.IRepositoryObject#
     *      getParent()
     */
    public IContainerObject getParent() {
        return m_parent;
    }

    /**
     * Moves this object to another parent.
     * 
     * @see org.knime.workbench.repository.model.IRepositoryObject#
     *      move(org.knime.workbench.repository.model.IContainerObject)
     */
    public void move(final IContainerObject newParent) {
        this.getParent().removeChild(this);
        newParent.addChild(this);
    }

    /**
     * internal, sets parent to null.
     * 
     */
    protected void detach() {
        m_parent = null;
    }

    /**
     * @return Returns the id.
     */
    public String getID() {
        return m_id;
    }

    /**
     * Set the id.
     * 
     * @param id the id
     */
    protected void setID(final String id) {
        m_id = id;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return m_name;
    }
    
    /**
     * @return Returns the afterID.
     */
    public String getAfterID() {
        return m_afterID;
    }

    /**
     * @param name The name to set.
     */
    public void setName(final String name) {
        m_name = name;
    }
    
    /**
     * @param id the id to set
     */
    public void setAfterID(final String id) {
        m_afterID = id;
    }

    /**
     * @return the isExpertNode
     */
    public boolean isExpertNode() {
        return m_isExpertNode;
    }

    /**
     * @param isExpertNode the isExpertNode to set
     */
    public void setExpertNode(final boolean isExpertNode) {
        m_isExpertNode = isExpertNode;
    }

    /**
     * Compares two repository objects lexicographically accordint to their
     * name.
     * 
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(final AbstractRepositoryObject o) {
        return m_name.compareTo(o.m_name);
    }
}
