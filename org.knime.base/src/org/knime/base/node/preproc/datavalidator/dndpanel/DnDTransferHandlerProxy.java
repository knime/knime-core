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
 *   17.10.2014 (Marcel Hanser): created
 */
package org.knime.base.node.preproc.datavalidator.dndpanel;

import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * Simplifies the work with the drag and drop API of Swing. Usually transfer hander implementations just want to modify
 * the creation of the {@link Transferable} object. But most functionality, like setting the "Not dropable" icon if the
 * target component is not suited for the transferable object, can be reused from the TransferHandler received by the
 * {@link JComponent#getTransferHandler()}. So this class delegates most methods to the proxied transfer handler, just
 * leaf the creation of the {@link Transferable} object to the subclasses. Also it notifies the given
 * {@link DnDStateListener} if the drag and drop procedure starts and stops. An example usage:
 *
 * <pre>
 * ...
 * JList jlist;
 * m_columnList.setDragEnabled(true);
 *    final TransferHandler handler = jlist.getTransferHandler();
 *    jlist.setTransferHandler(new DragAndDropTransferHandlerProxy(handler, dndStateListener) {
 *       protected Transferable getTransferable() {
 *          return new DragAndDropColumnSpecSelection(getSelectedColumns());
 *       }
 *    });
 * </pre>
 *
 * @author Marcel Hanser
 */
@SuppressWarnings("serial")
public abstract class DnDTransferHandlerProxy extends TransferHandler {
    private final TransferHandler m_t;

    private final DnDStateListener m_dsl;

    /**
     * Constructor.
     *
     * @param toBeProxied the transfer handler usually received by {@link JComponent#getTransferHandler()}.
     * @param dragAndDropStateListener listener to notify for drag and drop start/end events
     */
    public DnDTransferHandlerProxy(final TransferHandler toBeProxied,
        final DnDStateListener dragAndDropStateListener) {
        super();
        this.m_t = toBeProxied;
        m_dsl = dragAndDropStateListener;
    }

    @Override
    protected Transferable createTransferable(final JComponent c) {
        Point locationOnScreen = c.getLocationOnScreen();
        Point mousePosition = c.getMousePosition();
        if (mousePosition != null) {
            locationOnScreen.y += mousePosition.y;
        }
        Transferable transferable = getTransferable();
        m_dsl.dragStartedAt(locationOnScreen, transferable);
        return transferable;
    }

    /**
     * @return the object to be transfered
     */
    protected abstract Transferable getTransferable();

    @Override
    public int hashCode() {
        return m_t.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return m_t.equals(obj);
    }

    @Override
    public String toString() {
        return m_t.toString();
    }

    @Override
    public void setDragImage(final Image img) {
        m_t.setDragImage(img);
    }

    @Override
    public Image getDragImage() {
        return m_t.getDragImage();
    }

    @Override
    public void setDragImageOffset(final Point p) {
        m_t.setDragImageOffset(p);
    }

    @Override
    public Point getDragImageOffset() {
        return m_t.getDragImageOffset();
    }

    @Override
    public void exportAsDrag(final JComponent comp, final InputEvent e, final int action) {
        m_t.exportAsDrag(comp, e, action);
    }

    @Override
    public void exportToClipboard(final JComponent comp, final Clipboard clip, final int action)
        throws IllegalStateException {
        m_t.exportToClipboard(comp, clip, action);
    }

    @Override
    public boolean importData(final TransferSupport support) {
        return m_t.importData(support);
    }

    @Override
    public boolean importData(final JComponent comp, final Transferable t) {
        return m_t.importData(comp, t);
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        return m_t.canImport(support);
    }

    @Override
    public boolean canImport(final JComponent comp, final DataFlavor[] transferFlavors) {
        return m_t.canImport(comp, transferFlavors);
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return m_t.getSourceActions(c);
    }

    @Override
    public Icon getVisualRepresentation(final Transferable t) {
        return m_t.getVisualRepresentation(t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void exportDone(final JComponent source, final Transferable data, final int action) {
        m_dsl.dragStoppedAt();
    }
}
