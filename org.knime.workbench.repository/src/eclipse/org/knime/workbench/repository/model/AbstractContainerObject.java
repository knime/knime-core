/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Abstract base implementation of a container object.
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class AbstractContainerObject extends AbstractRepositoryObject
        implements IContainerObject {

    private boolean m_sortChildren = true;

    private boolean m_isLocked = true;

    /**
     * Creates a new locked container object.
     *
     * @param id the object's unique id
     * @param name the object's display name
     * @param contributingPlugin the id of the plug-in which contributed this object
     */
    protected AbstractContainerObject(final String id, final String name, final String contributingPlugin) {
        super(id, name, contributingPlugin);
    }


    /**
     * Creates a new container object.
     *
     * @param id the object's unique id
     * @param name the object's display name
     * @param contributingPlugin the id of the plug-in which contributed this object
     * @param locked <code>true</code> if this container is locked, <code>false</code> otherwise
     */
    protected AbstractContainerObject(final String id, final String name, final String contributingPlugin,
                                      final boolean locked) {
        this(id, name, contributingPlugin);
        m_isLocked = locked;
    }

    /**
     * Creates a copy of the given object.
     *
     * @param copy the object that should be copied
     */
    protected AbstractContainerObject(final AbstractContainerObject copy) {
        super(copy);
        this.m_sortChildren = copy.m_sortChildren;
        for (AbstractRepositoryObject child : copy.m_children) {
            AbstractRepositoryObject childCopy =
                    (AbstractRepositoryObject)child.deepCopy();
            childCopy.setParent(this);
            this.m_children.add(childCopy);
        }
    }

    /**
     * Sets if children should be sorted according to the "after" declarations
     * and the node names.
     *
     * @param sort <code>true</code> if the nodes should be sorted,
     *            <code>false</code> if the order in which they have been added
     *            should be retained
     * @see #setAfterID(String)
     */
    public void setSortChildren(final boolean sort) {
        m_sortChildren = sort;
    }

    /**
     * Returns if children should be sorted according to the "after"
     * declarations and the node names.
     *
     * @return <code>true</code> if the nodes should be sorted,
     *         <code>false</code> if the order in which they have been added
     *         should be retained
     * @see #setAfterID(String)
     */
    protected boolean sortChildren() {
        return m_sortChildren;
    }

    /**
     * The list of categories and nodes.
     */
    private final List<AbstractRepositoryObject> m_children =
            new ArrayList<AbstractRepositoryObject>();

    private AbstractRepositoryObject[] m_sortedChildren = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean hasChildren() {
        return !m_children.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addChild(final AbstractRepositoryObject child) {
        if (m_children.contains(child)) {
            return false;
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
        m_sortedChildren = null;
        return true;
    }

    /**
     * Removes all children from this container.
     */
    public void removeAllChildren() {
        m_children.clear();
        m_sortedChildren = null;
    }

    /**
     * Adds all repository objects from the given list as children to this
     * container.
     *
     * @param children a collection of repository objects
     * @see #addChild(AbstractRepositoryObject)
     */
    public void addAllChildren(
            final Collection<? extends AbstractRepositoryObject> children) {
        for (AbstractRepositoryObject aro : children) {
            addChild(aro);
        }
        m_sortedChildren = null;
    }

    /**
     * Returns the children. The children are sorted according to the
     * after-relationship defined in the plugin-xml and lexicographically.
     *
     * @return The children (category and nodes of current level)
     * @see org.knime.workbench.repository.model.IContainerObject# getChildren()
     */
    @Override
    public synchronized IRepositoryObject[] getChildren() {
        if (m_sortChildren) {
            if (m_sortedChildren == null) {
                List<AbstractRepositoryObject> sc = sortChildren(m_children);
                m_sortedChildren = sc.toArray(new AbstractRepositoryObject[sc.size()]);
            }
            return m_sortedChildren;
        } else {
            return m_children.toArray(new IRepositoryObject[m_children.size()]);
        }
    }

    /**
     * Sorts the children (categories and nodes) of this level. Categories are
     * placed before nodes and are sorted according to the after-relationship
     * defined in the plugin.xml. Nodes are sorted lexicographically and are
     * appended at the end of the list.
     *
     * @param children the children (categories and nodes)
     *
     * @return the sorted list
     */
    private List<AbstractRepositoryObject> sortChildren(
            final List<AbstractRepositoryObject> children) {
        CategorySorter sorter = new CategorySorter();
        return sorter.sortCategory(children);
    }

    /**
     * Removes a child.
     *
     * @param child The child to remove
     * @see org.knime.workbench.repository.model.IContainerObject#
     *      removeChild(AbstractRepositoryObject)
     */
    @Override
    public void removeChild(final AbstractRepositoryObject child) {
        if (!m_children.contains(child)) {
            throw new IllegalArgumentException(
                    "Can't remove child more, object not found");
        }
        m_children.remove(child);
        child.detach();
        m_sortedChildren = null;
    }

    /**
     * Moves this object to a different parent.
     *
     * @see org.knime.workbench.repository.model.IRepositoryObject#
     *      move(org.knime.workbench.repository.model.IContainerObject)
     * @param newParent The container to move this object to
     */
    @Override
    public void move(final IContainerObject newParent) {
        this.getParent().removeChild(this);
        this.setParent(newParent);
        this.getParent().addChild(this);
        m_sortedChildren = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized IRepositoryObject getChildByID(final String id,
            final boolean rec) {
        // The slash and the empty string represent 'this'
        if ("/".equals(id) || "".equals(id.trim())) {
            return this;
        }
        for (Iterator<AbstractRepositoryObject> it = m_children.iterator(); it
                .hasNext();) {
            IRepositoryObject o = it.next();

            if (o.getID().equals(id)) {
                return o;
            }

            // if it is a container, recursivly dive inside it !
            if (rec) {
                if (o instanceof IContainerObject) {
                    IRepositoryObject result =
                            ((IContainerObject)o).getChildByID(id, rec);
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
                NodeTemplate t = (NodeTemplate)c[i];
                if (t.getFactory().getName().equals(factory)) {
                    return t;
                }
            } else if (c[i] instanceof AbstractContainerObject) {
                NodeTemplate t =
                        ((AbstractContainerObject)c[i])
                                .findTemplateByFactory(factory);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addChildAfter(final AbstractRepositoryObject child,
            final AbstractRepositoryObject before) {
        if (m_children.contains(child)) {
            return false;
        }

        ListIterator<AbstractRepositoryObject> it = m_children.listIterator();
        while (it.hasNext()) {
            if (it.next() == before) {
                it.add(child);
                m_sortedChildren = null;
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addChildBefore(final AbstractRepositoryObject child,
            final AbstractRepositoryObject after) {
        if (m_children.contains(child)) {
            return false;
        }
        ListIterator<AbstractRepositoryObject> it = m_children.listIterator();
        while (it.hasNext()) {
            if (it.next() == after) {
                it.previous();
                it.add(child);
                m_sortedChildren = null;
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(final IRepositoryObject child) {
        return m_children.contains(child);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocked() {
        return m_isLocked;
    }
}
