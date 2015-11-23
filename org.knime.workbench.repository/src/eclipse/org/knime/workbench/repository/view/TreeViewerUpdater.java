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
 *   Nov 19, 2015 (hornm): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Little helper class to allow different classes to trigger an update of the TreeViewer in the node repository.
 * Updating the TreeViewer also implies (re)applying all filters that are set on the given TreeViewer. Main purpose is
 * to avoid code duplication.
 *
 * @author Martin Horn, University of Konstanz
 */
class TreeViewerUpdater {

    private static final boolean IS_OS_WINDOWS = Platform.OS_WIN32.equals(Platform.getOS());

    private TreeViewerUpdater() {
        //static utility class
    }

    /**
     * Collapses all branches.
     */
    static void collapseAll(final TreeViewer viewer) {
        Point backup = null;

        if (IS_OS_WINDOWS) {
            Rectangle bounds = viewer.getTree().getParent().getShell().getBounds();
            // Bug 2809 -
            // on windows the search is much slower if the cursor is within the KNIME window.
            // so we just set it somewhere outside and restore it afterwards
            backup = Display.getCurrent().getCursorLocation();
            Display.getCurrent().setCursorLocation(new Point(bounds.x - 2, bounds.y - 2));
        }

        try {
            viewer.collapseAll();
        } finally {
            if (backup != null) {
                Display.getCurrent().setCursorLocation(backup);
            }

            viewer.getControl().setRedraw(true);
        }

    }

    /** Updates and filters the tree
     *
     * @param shouldExpand whether the tree should be expanded
     */
    static void update(final TreeViewer viewer, final boolean shouldExpand) {

        Point backup = null;

        if (IS_OS_WINDOWS) {
            Rectangle bounds = viewer.getTree().getParent().getShell().getBounds();
            // Bug 2809 -
            // on windows the search is much slower if the cursor is within the KNIME window.
            // so we just set it somewhere outside and restore it afterwards
            backup = Display.getCurrent().getCursorLocation();
            Display.getCurrent().setCursorLocation(new Point(bounds.x - 2, bounds.y - 2));
        }
        viewer.getControl().setRedraw(false);
        try {
            viewer.refresh();
            if (shouldExpand) {
                viewer.expandAll();
            }
            //scroll to root
            if (viewer.getTree().getItemCount() > 0) {
                TreeItem item = viewer.getTree().getItem(0);
                viewer.getTree().showItem(item);
            }
        } finally {
            if (backup != null) {
                Display.getCurrent().setCursorLocation(backup);
            }

            viewer.getControl().setRedraw(true);
        }
    }

}
