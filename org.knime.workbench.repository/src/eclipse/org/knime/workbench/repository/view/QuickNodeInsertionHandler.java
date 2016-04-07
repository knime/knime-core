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
 *   29.07.2014 (Marcel Hanser): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.knime.core.node.NodeLogger;

/**
 * Used to open the Quick Node Insertion dialog. Now, however, it just puts the focus to the node repository view that
 * offers the same functionality that the quick node insertion dialog offered (i.e. fuzzy search and node insertion via
 * arrow-keys and enter).
 *
 * @author Marcel Hanser
 * @author Martin Horn, University of Konstanz
 */
public class QuickNodeInsertionHandler extends AbstractHandler {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(QuickNodeInsertionHandler.class);

    /**
     * The eclipse command id.
     */
    public static final String COMMAND_ID = "org.knime.workbench.repository.view.QuickNodeInsertionHandler";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        // fixes bug 6268: CTRL+Space opens Quick Node Insertion instead of autocomplete for node dialogs
        // that use the RSyntaxTextArea autocomplete feature

        // according the extension point definition of org.eclipse.ui.contexts you can also define deletion markers
        // but I did not get this to work - the quick node insertion always popped up in the dialog of a java snippet
        // until we added this
        IContextService ctxService = PlatformUI.getWorkbench().getService(IContextService.class);
        if (ctxService != null && ctxService.getActiveContextIds().contains(IContextService.CONTEXT_ID_DIALOG)) {
            return false;
        }
        return true;
    }

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        try {
            PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage().showView(DefaultRepositoryView.ID);
        } catch (PartInitException e) {
            LOGGER.warn("Can't give focus to the node repository view.", e);
        }
        return null;
    }
}
