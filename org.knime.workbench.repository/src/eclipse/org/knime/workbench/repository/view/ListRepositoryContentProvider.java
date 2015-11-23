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
package org.knime.workbench.repository.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.Root;

/**
 * List ContentProvider for the quick node insertion dialog. It returns all nodes ignoring the categories.
 *
 * @author Marcel Hanser, University of Konstanz
 * @author Martin Horn, University of Konstanz
 */
public class ListRepositoryContentProvider implements ITreeContentProvider {
    private Object m_root;
    private Object[] m_allElements;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        ArrayList<Object> list = new ArrayList<>();
        getObjects(inputElement, list);
        m_root = inputElement;
        m_allElements = list.toArray();
        return m_allElements;
    }

    /**
     * Aggregates all objects.
     *
     * @param inputElement
     * @param toAddTo
     */
    private void getObjects(final Object inputElement, final List<Object> toAddTo) {
        if (inputElement instanceof IContainerObject) {
            IRepositoryObject[] children = ((IContainerObject)inputElement).getChildren();
            for (IRepositoryObject child : children) {
                getObjects(child, toAddTo);
            }
        } else {
            toAddTo.add(inputElement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof Root) {
            return m_allElements;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParent(final Object element) {
        if(element instanceof Root) {
            return null;
        } else {
            return m_root;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        if(element instanceof Root) {
            return true;
        } else {
            return false;
        }
    }
}
