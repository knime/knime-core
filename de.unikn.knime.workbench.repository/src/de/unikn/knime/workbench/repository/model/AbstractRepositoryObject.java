/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
public abstract class AbstractRepositoryObject implements IRepositoryObject {

    private IContainerObject m_parent;

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
}
