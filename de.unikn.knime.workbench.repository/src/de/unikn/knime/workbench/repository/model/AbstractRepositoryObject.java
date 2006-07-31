/* 
 * -------------------------------------------------------------------
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
 *   16.03.2005 (georg): created
 */
package de.unikn.knime.workbench.repository.model;

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
     * @see de.unikn.knime.workbench.repository.model.IRepositoryObject#
     *      getParent()
     */
    public IContainerObject getParent() {
        return m_parent;
    }

    /**
     * Moves this object to another parent.
     * 
     * @see de.unikn.knime.workbench.repository.model.IRepositoryObject#
     *      move(de.unikn.knime.workbench.repository.model.IContainerObject)
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
     * @param name The name to set.
     */
    public void setName(final String name) {
        m_name = name;
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
