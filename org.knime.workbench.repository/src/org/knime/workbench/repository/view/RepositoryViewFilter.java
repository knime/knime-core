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
