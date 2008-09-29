/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   11.01.2006 (Florian Georg): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;

/**
 * Viewer Filter for the reprository view.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class RepositoryViewFilter extends ViewerFilter {
    private String m_query;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean select(final Viewer viewer, final Object parentElement,
            final Object element) {

        // this means that the filter has been cleared
        if ((m_query == null) || (m_query.equals(""))) {
            return true;
        }

        // call helper method
        return doSelect((IRepositoryObject)parentElement,
                (IRepositoryObject)element, true);

    }

    /**
     * Actual select method. An element is selected if itself, a parent or a
     * child contains the query string in its name.
     * 
     * @param parentElement
     * @param element
     * @param recurse whether to recurse into categories or not
     * @return <code>true</code> if the element should be selected
     */
    private boolean doSelect(final IRepositoryObject parentElement,
            final IRepositoryObject element, final boolean recurse) {

        boolean selectThis = false;

        // Node Template : Match against name
        if (element instanceof NodeTemplate) {

            // check against node name
            selectThis = match(((NodeTemplate)element).getName());
            if (selectThis) {
                return true;
            }
            // we must also check towards root, as we want to include all
            // children of a selected category
            IRepositoryObject temp = parentElement;
            while (!(temp instanceof Root)) {

                // check parent category, but do *not* recurse !!!!
                if (doSelect(temp.getParent(), temp, false)) {
                    return true;
                }
                temp = temp.getParent();
            }
        } else 
        // MetaNodeTemplate: check agains name and names of contained nodes
        if (element instanceof MetaNodeTemplate) {
            selectThis = match(((MetaNodeTemplate)element).getName())
                || match(((MetaNodeTemplate)element).getManager().getName());
            if (selectThis) {
                return true;
            } 
            /*
             * enable if advanced search in NodeRepository is available
            for (NodeContainer cont : ((MetaNodeTemplate)element).getManager()
                    .getNodeContainers()) {
                if (match(cont.getName())) {
                    return true;
                }
            }
            */
        } else
        // Category: Match against name and children
        if (element instanceof Category) {
            // check against node name
            selectThis = match(((Category)element).getName());
            if (selectThis) {
                return true;
            }

            // check recursivly against children, if needed
            if (recurse) {
                Category category = (Category)element;
                IRepositoryObject[] children = category.getChildren();
                for (int i = 0; i < children.length; i++) {
                    // recursivly check. return true on first matching child
                    if (doSelect(category, children[i], true)) {
                        return true;
                    }

                }
            }
        }

        return false;
    }

    /**
     * 
     * @param test String to test
     * @return <code>true</code> if the test is contained in the m_query
     *         String (ignoring case)
     */
    private boolean match(final String test) {
        if (test == null) {
            return false;
        }
        return test.toUpperCase().contains(m_query.toUpperCase());
    }

    /**
     * Set the query String that is responsible for selecting nodes/categories.
     * 
     * @param query The query string
     */
    public void setQueryString(final String query) {
        m_query = query;
    }
}
