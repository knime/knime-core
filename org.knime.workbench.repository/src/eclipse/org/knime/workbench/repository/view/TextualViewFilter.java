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
 * ------------------------------------------------------------------------
 */

package org.knime.workbench.repository.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class TextualViewFilter extends ViewerFilter {
    private String m_query;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean select(final Viewer viewer, final Object parentElement,
            final Object element) {

        // this means that the filter has been cleared
        if (!hasNonEmptyQuery()) {
            return true;
        }
        // call helper method
        return doSelect(parentElement, element, true);
    }

    /**
     * Determines if an element shall be selected or not.
     *
     * @param parentElement the parent element
     * @param element the element to check for selection
     * @param recursive whether to recurse into elements or not
     * @return <code>true</code> if the element should be selected
     */
    protected abstract boolean doSelect(Object parentElement,
            Object element, boolean recursive) ;

    /**
     *
     * @param test String to test
     * @return <code>true</code> if the test is contained in the m_query
     *         String (ignoring case)
     */
    protected boolean match(final String test) {
        if (test == null) {
            return false;
        }
        return test.toUpperCase().contains(m_query);
    }

    /**
     * Set the query String that is responsible for selecting nodes/categories.
     *
     * @param query The query string
     */
    public void setQueryString(final String query) {
        m_query = query.toUpperCase();
    }

    /**
     * Returns is this filter has a non-empty query, i.e. if item should be
     * filtered out.
     *
     * @return <code>true</code> if a non-empty query exists, <code>false</code>
     *         otherwise
     */
    public boolean hasNonEmptyQuery() {
        return (m_query != null) && (m_query.length() > 0);
    }
}
