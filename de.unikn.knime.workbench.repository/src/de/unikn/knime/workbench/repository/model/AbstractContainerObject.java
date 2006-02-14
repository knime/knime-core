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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Abstract base implementation of a container object.
 * 
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractContainerObject extends AbstractRepositoryObject
        implements IContainerObject {

    // ordered set using default comparator
    private ArrayList m_children = new ArrayList();

    private Comparator m_comparator = new DefaultRepositoryComparator();

    /**
     * Return true, if there are children contained in this cotainer.
     * 
     * @see de.unikn.knime.workbench.repository.model.IContainerObject#
     *      hasChildren()
     */
    public final boolean hasChildren() {
        return this.getChildren().length > 0;
    }

    /**
     * Add a child to this container.
     * 
     * @see de.unikn.knime.workbench.repository.model.IContainerObject#
     *      addChild(de.unikn.knime.workbench.repository.model.
     *      AbstractRepositoryObject)
     */
    public void addChild(final AbstractRepositoryObject child) {
        if (m_children.contains(child)) {
            throw new IllegalArgumentException(
                    "Can't add child, already contained");
        }
        if (child instanceof Root) {
            throw new IllegalArgumentException(
                    "Can't add root object as a child");
        }
        if (child == this) {
            throw new IllegalArgumentException("Can't add 'this' as a child");
        }

        m_children.add(child);

        child.setParent(this);

    }

    /**
     * Returns the children.
     * 
     * @return The children
     * @see de.unikn.knime.workbench.repository.model.IContainerObject#
     *      getChildren()
     */
    public IRepositoryObject[] getChildren() {
        Collections.sort(m_children, m_comparator);

        return (IRepositoryObject[]) m_children
                .toArray(new IRepositoryObject[m_children.size()]);
    }

    /**
     * Removes a child.
     * 
     * @param child The child to remove
     * @see de.unikn.knime.workbench.repository.model.IContainerObject#
     *      removeChild(AbstractRepositoryObject)
     */
    public void removeChild(final AbstractRepositoryObject child) {
        if (!m_children.contains(child)) {
            throw new IllegalArgumentException(
                    "Can't remove child more, object not found");
        }
        m_children.remove(child);

        child.detach();

    }

    /**
     * Moves this object to a different parent.
     * 
     * @see de.unikn.knime.workbench.repository.model.IRepositoryObject#
     *      move(de.unikn.knime.workbench.repository.model.IContainerObject)
     * @param newParent The container to move this object to
     */
    public void move(final IContainerObject newParent) {
        this.getParent().removeChild(this);
        this.setParent(newParent);
        this.getParent().addChild(this);
    }

    /**
     * @see de.unikn.knime.workbench.repository.model.IContainerObject#
     *      getChildByID(String, boolean)
     */
    public IRepositoryObject getChildByID(final String id, final boolean rec) {
        // The slash and the empty string represent 'this'
        if ("/".equals(id) || "".equals(id.trim())) {
            return this;
        }
        for (Iterator it = m_children.iterator(); it.hasNext();) {
            IRepositoryObject o = (IRepositoryObject) it.next();

            if (o.getID().equals(id)) {
                return o;
            }

            // if it is a container, recursivly dive inside it !
            if (rec) {
                if (o instanceof IContainerObject) {
                    IRepositoryObject result = ((IContainerObject) o)
                            .getChildByID(id, rec);
                    if (result != null) {
                        return result;
                    }
                }
            }

        }
        return null;
    }

    /**
     * Looks up a <code>NodeTemplate</code> for a given factory.
     * 
     * @param factory The factory name for which the template should be found
     * @return The template or <code>null</code>
     */
    public NodeTemplate findTemplateByFactory(final String factory) {
        IRepositoryObject[] c = getChildren();
        for (int i = 0; i < c.length; i++) {
            if (c[i] instanceof NodeTemplate) {
                NodeTemplate t = (NodeTemplate) c[i];
                if (t.getFactory().getName().equals(factory)) {
                    return t;
                }
            } else if (c[i] instanceof AbstractContainerObject) {
                NodeTemplate t = ((AbstractContainerObject) c[i])
                        .findTemplateByFactory(factory);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * 
     * @see IContainerObject#setComparator(Comparator)
     */
    public void setComparator(final Comparator comp) {
        m_comparator = comp;
        Collections.sort(m_children, m_comparator);
    }

}
