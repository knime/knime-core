/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 20, 2015 (hornm): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.knime.workbench.repository.model.AbstractNodeTemplate;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.Root;

/**
 * Filters nodes whose additional information given by a certain key is available (indicated by their additional information - {@link AbstractRepositoryObject#getAdditionalInfo(String)}).
 * It helps, e.g., to filter out nodes that are streamable.
 *
 * @author Martin Horn, University of Konstanz
 */
class AdditionalInfoViewFilter extends ViewerFilter{

    private boolean m_doFilter;
    private TextualViewFilter m_delegate;
    private String[] m_additionalInfoKeys;

    /**
     * @param delegate another filter that is to be used before
     * @param additionalInfoKeys the keys to be checked
     *
     */
    public AdditionalInfoViewFilter(final TextualViewFilter delegate, final String... additionalInfoKeys) {
        m_delegate = delegate;
        m_additionalInfoKeys = additionalInfoKeys;
        m_doFilter = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
        if (m_doFilter) {
            if (element instanceof AbstractNodeTemplate) {
                AbstractNodeTemplate nT = (AbstractNodeTemplate)element;
                for (String key : m_additionalInfoKeys) {
                    if (nT.getAdditionalInfo(key) == null) {
                        return false;
                    }
                }

                //additional infos are present, check the textual delegate filter
                if (m_delegate.hasNonEmptyQuery()) {
                    return m_delegate.match(nT.getName());
                } else {
                    return true;
                }

            } else
            // Category: check children
            if (element instanceof Category) {
                // check recursively against children, if needed
                Category category = (Category)element;
                IRepositoryObject[] children = category.getChildren();
                for (int i = 0; i < children.length; i++) {
                    if (select(viewer, element, children[i])) {
                        //return true if first matching child is found
                        return true;
                    }
                }
                return false;
            } else if(element instanceof Root){
                return true;
            } else {
                //e.g. in the case of an meta node
                return false;
            }

        } else {
            return m_delegate.select(viewer, parentElement, element);
        }
    }

    public void setDoFilter(final boolean doFilter) {
        m_doFilter = doFilter;
    }

    /**
     * @return the underlying filter used additional to the info filtering
     */
    TextualViewFilter getDelegateFilter() {
        return m_delegate;
    }

}
