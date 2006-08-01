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
 * Interface for repository objects that act as a container of other objects.
 * 
 * @author Florian Georg, University of Konstanz
 */
public interface IContainerObject extends IRepositoryObject {
    /**
     * Constant indicating that the finder method should look at "infinite"
     * depth.
     */
    public static final int DEPTH_INFINITE = -1;

    /**
     * Returns wheter this container conatains cildren.
     * 
     * @return <code>true</code>, if this container has children
     * 
     */
    public boolean hasChildren();

    /**
     * Returns the children.
     * 
     * @return Array containing the children
     */
    public IRepositoryObject[] getChildren();

    /**
     * Adds a child, should throw an excpetion if invalid.
     * 
     * @param child The child to add
     */
    public void addChild(AbstractRepositoryObject child);

    /**
     * Removes a child, should throw an exception if invalid.
     * 
     * @param child The child to remove
     */
    public void removeChild(AbstractRepositoryObject child);

    /**
     * Looks up a child, given by id.
     * 
     * @param id The (level) id
     * @param recurse wheter to dive into sub-containers
     * 
     * @return The child, or <code>null</code>
     */
    public IRepositoryObject getChildByID(String id, boolean recurse);
}
