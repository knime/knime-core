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
 *   11.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.draganddroppanel.transferhandler;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * A transfer handler which transfers strings from one component to another.
 * This handler can be used to transfer list items from one list to another.
 *
 * @author tibuch
 */
public abstract class StringTransferHandler extends TransferHandler {

    /**
     * Exports a string from a component.
     *
     * @param c the transfer start component
     * @return the string to export
     */
    protected abstract String exportString(JComponent c);

    /**
     * Gets a string and adds this string to the component.
     *
     * @param c the transfer target component
     * @param str string to import
     */
    protected abstract void importString(JComponent c, String str);

    /**
     * If a move operation took place, the source item has to be removed from the source list.
     * If a copy operation took place, nothing has to be cleaned up.
     *
     * @param c the transfer start component
     * @param remove the exported string
     */
    protected abstract void cleanup(JComponent c, boolean remove);

    @Override
    protected Transferable createTransferable(final JComponent c) {
        return new StringSelection(exportString(c));
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return MOVE;
    }

    @Override
    public boolean importData(final JComponent c, final Transferable t) {
        if (canImport(c, t.getTransferDataFlavors())) {
            try {
                String str = (String)t.getTransferData(DataFlavor.stringFlavor);
                importString(c, str);
                return true;
            } catch (UnsupportedFlavorException | IOException e) {
                // nothing
            }
        }
        return false;
    }

    @Override
    protected void exportDone(final JComponent c, final Transferable data, final int action) {
        cleanup(c, action == MOVE);
    }

    @Override
    public boolean canImport(final JComponent c, final DataFlavor[] flavors) {
        for (DataFlavor dataFlavor : flavors) {
            if (dataFlavor.equals(DataFlavor.stringFlavor)) {
                return true;
            }
        }
        return false;
    }
}
