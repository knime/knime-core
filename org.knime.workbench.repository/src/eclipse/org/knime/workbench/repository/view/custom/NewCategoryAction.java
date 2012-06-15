/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * ---------------------------------------------------------------------
 *
 * History
 *   01.06.2012 (meinl): created
 */
package org.knime.workbench.repository.view.custom;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.repository.RepositoryFactory;
import org.knime.workbench.repository.model.AbstractContainerObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.CustomRepositoryManager;
import org.knime.workbench.repository.model.Root;

/**
 * Action for creating a new custom category.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
class NewCategoryAction extends Action {
    private final TreeViewer m_viewer;

    NewCategoryAction(final TreeViewer viewer) {
        super("New category");
        m_viewer = viewer;
        ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
        setImageDescriptor(images
                .getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD));
        setDisabledImageDescriptor(images
                .getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD_DISABLED));
    }

    @Override
    public void run() {
        IStructuredSelection sel =
                (IStructuredSelection)m_viewer.getSelection();
        AbstractContainerObject parent;
        if (sel.isEmpty()) {
            parent = (Root)m_viewer.getInput();
        } else if (sel.getFirstElement() instanceof Category) {
            parent = (Category)sel.getFirstElement();
        } else {
            return;
        }
        Category newCategory =
                CustomRepositoryManager.createCustomCategory("New Category");
        String path =
                (parent instanceof Root) ? "/" : (((Category)parent).getPath()
                        + "/" + parent.getID());
        newCategory.setPath(path);

        parent.addChild(newCategory);
        newCategory.setParent(parent);
        newCategory.setIconDescriptor(RepositoryFactory.defaultIcon);
        newCategory.setIcon(RepositoryFactory.defaultIcon.createImage());

        m_viewer.refresh(parent);
    }

    public boolean canBeEnabled() {
        if (m_viewer.getSelection().isEmpty()) {
            return true;
        }
        IStructuredSelection sel =
                (IStructuredSelection)m_viewer.getSelection();
        if (sel.size() > 1) {
            return false;
        }

        Object selectedObject = sel.getFirstElement();

        if (!(selectedObject instanceof Category)) {
            return false;
        }
        return CustomRepositoryManager
                .isCustomCategory((Category)selectedObject);
    }
}
