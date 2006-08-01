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

import org.eclipse.core.runtime.IAdaptable;

/**
 * Base interface for objects in the repository.
 * 
 * @author Florian Georg, University of Konstanz
 */
public interface IRepositoryObject extends IAdaptable {
    /**
     * Returns an ID for this object.The semantics may differ in the concrete
     * implementations.
     * 
     * @return A (semantically) id for this object
     */
    public String getID();

    /**
     * Returns the parent object. May be <code>null</code> if this is a root
     * object, or detached from the model tree.
     * 
     * @return The parent, or <code>null</code>
     */
    public IContainerObject getParent();

    /**
     * Moves this object to a new parent object.
     * 
     * @param newParent The new parent.
     */
    public void move(IContainerObject newParent);
}
