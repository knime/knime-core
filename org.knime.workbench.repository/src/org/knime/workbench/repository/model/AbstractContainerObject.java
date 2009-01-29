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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract base implementation of a container object.
 * 
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class AbstractContainerObject extends AbstractRepositoryObject
        implements IContainerObject {
    
    private boolean m_sortChildren = true;
    
    public void setSortChildren(final boolean sort) {
        m_sortChildren = sort;
    }
    
    protected boolean sortChildren() {
        return m_sortChildren;
    }
    
    /**
     * The list of categories and nodes.
     */
    private ArrayList<AbstractRepositoryObject> m_children = new ArrayList<AbstractRepositoryObject>();

    /**
     * Contains a list of categories that could not inserted properly.
     */
    private ArrayList<Category> m_problemCategories = new ArrayList<Category>();

    /**
     * Return true, if there are children contained in this cotainer.
     * 
     * @see org.knime.workbench.repository.model.IContainerObject# hasChildren()
     */
    public final boolean hasChildren() {
        return this.getChildren().length > 0;
    }

    /**
     * Add a child to this container.
     * 
     * @see org.knime.workbench.repository.model.IContainerObject#
     *      addChild(org.knime.workbench.repository.model.
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
    
    public void removeAllChildren() {
        m_children.clear();
    }
    
    public void addAllChildren(Collection<? extends AbstractRepositoryObject> children) {
        m_children.addAll(children);
    }

    /**
     * Returns the children. The children are sorted according to the
     * after-relationship defined in the plugin-xml and lexicographically.
     * 
     * @return The children (category and nodes of current level)
     * @see org.knime.workbench.repository.model.IContainerObject# getChildren()
     */
    public synchronized IRepositoryObject[] getChildren() {

        // Collections.sort(m_children, m_comparator);
        if (m_sortChildren) {
            m_children = sortChildren(m_children);
        }
        return m_children.toArray(new IRepositoryObject[m_children.size()]);
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
    private ArrayList<AbstractRepositoryObject> sortChildren(
            final ArrayList<AbstractRepositoryObject> children) {

        // create two seperate lists of categories and nodes, as categories
        // are ordered according to the after-relationship (see plugin.xml)
        ArrayList<AbstractRepositoryObject> categoryChildren = new ArrayList<AbstractRepositoryObject>();
        ArrayList<AbstractRepositoryObject> nodeChildren = new ArrayList<AbstractRepositoryObject>();
        for (AbstractRepositoryObject object : children) {
            if (object instanceof Category) {
                categoryChildren.add((Category)object);
            } else if (object instanceof AbstractSimpleObject) {
                nodeChildren.add((AbstractSimpleObject)object);
            }
        }

        // the ordered result list
        ArrayList<AbstractRepositoryObject> result = new ArrayList<AbstractRepositoryObject>();

        // ---------- Category sorting -----------------------------------------
        // Create the root element of the after-relationship tree for the
        // categories
        TreeEntry root = new TreeEntry(null);

        // Recursively create the tree
        addSuccessors(root, categoryChildren);

        // traverse the tree depth first (apriori) and thus create the
        // sorted list
        createSortedList(root, result);

        // append all categories that have not been inserted due to wrong
        // or missing after-relationship information
        Collections.sort(categoryChildren);
        for (AbstractRepositoryObject category : categoryChildren) {
            m_problemCategories.add((Category)category);
            result.add(category);
        }

        // ---------- Node sorting ---------------------------------------------
        // Create the root element of the after-relationship tree for the nodes
        root = new TreeEntry(null);

        // Recursively create the tree
        addSuccessors(root, nodeChildren);

        // traverse the tree depth first (apriori) and thus create the
        // sorted list
        createSortedList(root, result);

        // Finally append all nodes in lexicographically order
        Collections.sort(nodeChildren);
        for (AbstractRepositoryObject node : nodeChildren) {
            result.add(node);
        }

        return result;
    }

    /**
     * Traverses a tree in depth first (apriori) order to create a sorted list.
     * 
     * @param entry the current entry of the tree
     * @param result the list the visited nodes are entered
     * @see AbstractContainerObject#sortChildren(ArrayList)
     */
    private void createSortedList(final TreeEntry entry,
            final ArrayList<AbstractRepositoryObject> result) {
        for (TreeEntry treeEntry : entry.getChildren()) {
            result.add(treeEntry.m_repositoryObject);
            createSortedList(treeEntry, result);
        }
    }

    /**
     * Recursive method to add all categories to the given parent entry from the
     * given list of categories (children).
     * 
     * @param parent the parent node to which the after-relationship related
     *            categories are added
     * @param children the list of all categories
     */
    private void addSuccessors(final TreeEntry parent,
            final ArrayList<AbstractRepositoryObject> children) {

        // add all children with an after id equal to the parents id
        Iterator<AbstractRepositoryObject> childIter = children.iterator();
        while (childIter.hasNext()) {
            AbstractRepositoryObject child = childIter.next();

            if (child.getAfterID().equals(parent.getId())) {
                parent.addChildCategory(new TreeEntry(child));
                childIter.remove();
            }
        }

        // sort the child entries
        parent.sort();

        // then invoke this method recursively for all children
        // this is the recursion termination in case there is no category
        // with an after id corresponding to the parents id
        for (TreeEntry childCategory : parent.getChildren()) {
            addSuccessors(childCategory, children);
        }
    }

    /**
     * An entry of a tree holding a category and all its direct successors
     * according to the after-relationship defined in the plugin.xml.
     * 
     * @author Christoph Sieb, University of Konstanz
     */
    private class TreeEntry implements Comparable<TreeEntry> {
        private AbstractRepositoryObject m_repositoryObject;

        private List<TreeEntry> m_treeChildren;

        private String m_id;

        /**
         * Constructs a new GraphEntry.
         * 
         * @param repositoryObject the category representing the parent
         */
        public TreeEntry(final AbstractRepositoryObject repositoryObject) {
            m_repositoryObject = repositoryObject;

            if (m_repositoryObject != null) {
                m_id = m_repositoryObject.getID();
            } else {
                m_id = "";
            }
            m_treeChildren = new ArrayList<TreeEntry>();
        }

        /**
         * Sorts the children lexicographically.
         */
        public void sort() {
            Collections.sort(m_treeChildren);
        }

        /**
         * Adds a category as child to this tree entry.
         * 
         * @param category the category to add.
         */
        public void addChildCategory(final TreeEntry category) {
            m_treeChildren.add(category);
        }

        /**
         * @return returns the after relationship id
         */
        public String getId() {
            return m_id;
        }

        /**
         * @return the child categories of this tree entry.
         */
        public List<TreeEntry> getChildren() {
            return m_treeChildren;
        }

        public int compareTo(final TreeEntry o) {

            return m_repositoryObject.compareTo(o.m_repositoryObject);
        }
    }

    /**
     * Removes a child.
     * 
     * @param child The child to remove
     * @see org.knime.workbench.repository.model.IContainerObject#
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
     * @see org.knime.workbench.repository.model.IRepositoryObject#
     *      move(org.knime.workbench.repository.model.IContainerObject)
     * @param newParent The container to move this object to
     */
    @Override
    public void move(final IContainerObject newParent) {
        this.getParent().removeChild(this);
        this.setParent(newParent);
        this.getParent().addChild(this);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized IRepositoryObject getChildByID(final String id, final boolean rec) {
        // The slash and the empty string represent 'this'
        if ("/".equals(id) || "".equals(id.trim())) {
            return this;
        }
        for (Iterator it = m_children.iterator(); it.hasNext();) {
            IRepositoryObject o = (IRepositoryObject)it.next();

            if (o.getID().equals(id)) {
                return o;
            }

            // if it is a container, recursivly dive inside it !
            if (rec) {
                if (o instanceof IContainerObject) {
                    IRepositoryObject result = ((IContainerObject)o)
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
                NodeTemplate t = (NodeTemplate)c[i];
                if (t.getFactory().getName().equals(factory)) {
                    return t;
                }
            } else if (c[i] instanceof AbstractContainerObject) {
                NodeTemplate t = ((AbstractContainerObject)c[i])
                        .findTemplateByFactory(factory);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Appends all categories to the passed list, that have wrong
     * after-relationship information.
     * 
     * @param problemList the list to which the problem categories are appended
     */
    protected void appendProblemCategories(final List<Category> problemList) {

        problemList.addAll(m_problemCategories);

        for (AbstractRepositoryObject repositoryObject : m_children) {

            if (repositoryObject instanceof AbstractContainerObject) {

                ((AbstractContainerObject)repositoryObject)
                        .appendProblemCategories(problemList);
            }
        }
    }
}
