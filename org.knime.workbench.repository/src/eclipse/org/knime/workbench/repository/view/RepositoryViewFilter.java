/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

import org.eclipse.swt.widgets.Display;
import org.knime.workbench.repository.model.AbstractNodeTemplate;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.Root;

/**
 * Viewer Filter for the reprository view.
 *
 * @author Florian Georg, University of Konstanz
 */
public class RepositoryViewFilter extends TextualViewFilter {

    /**
     *  An element is selected if itself, a parent or a
     * child contains the query string in its name.
     * {@inheritDoc}
     */
    @Override
    protected boolean doSelect(final Object parentElement,
            final Object element, final boolean recurse) {
        boolean selectThis = false;
        // Node Template : Match against name
        if (element instanceof AbstractNodeTemplate) {

            // check against node name
            selectThis = match(((AbstractNodeTemplate)element).getName());
            if (element instanceof MetaNodeTemplate) {
                // with meta nodes also check the name of the workflow manager
                selectThis |= match(((MetaNodeTemplate)element).getManager().getName());
            }
            if (selectThis) {
                return true;
            }
            // we must also check towards root, as we want to include all
            // children of a selected category
            IRepositoryObject temp = (IRepositoryObject)parentElement;
            while (!(temp instanceof Root)) {

                // check parent category, but do *not* recurse !!!!
                if (doSelect(temp.getParent(), temp, false)) {
                    return true;
                }
                temp = temp.getParent();
            }
        } else
        // Category: Match against name and children
        if (element instanceof Category) {
            // check against node name
            selectThis = match(((Category)element).getName());
            if (selectThis) {
                return true;
            }

            // check recursively against children, if needed
            if (recurse) {
                Category category = (Category)element;
                IRepositoryObject[] children = category.getChildren();
                for (int i = 0; i < children.length; i++) {
                    // recursively check. return true on first matching child
                    if (doSelect(category, children[i], true)) {
                        return true;
                    }

                }
            }
        }

        return false;
    }
}
