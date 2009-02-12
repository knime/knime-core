/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 17, 2005 (wiswedel): created
 * 2006-06-08 (tm): reviewed   
 */
package org.knime.core.node.tableview;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

/**
 * A mouse listener and mouse motion listener that is registered to
 * a {@link javax.swing.JTable} to change row height as the user resizes a row. 
 * This {@link javax.swing.JTable} represents the row header view in a
 * scrollpane (it has one column).
 *  
 * @author Bernd Wiswedel, University of Konstanz
 */
class RowHeaderHeightMouseListener extends MouseInputAdapter {

    /** Cursor to be used when resizting is possible. */
    private static final Cursor RESIZE_CURSOR = Cursor
            .getPredefinedCursor(Cursor.N_RESIZE_CURSOR);

    /** The standard cursor. */
    private static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();

    /** Distance in pixels from the rows (horizontal) edge within which the
     * "you can resize" cursor is shown.
     */
    private static final int PIXELS = 2;

    /** The underlying table. */
    private final JTable m_table;

    /** Memorize the current action. */
    private boolean m_isDragging;

    /** Row being resized (if any, otherwise -1). */
    private int m_resizingRow;

    /** Helper that remembers the last y coordinate. The last event was fired 
     * with the cursor at this position.
     */
    private int m_yOld;

    /** Always the cursor that is not shown. */
    private Cursor m_tmpCursor = RESIZE_CURSOR;

    /** Creates a new listener and also registers this listener to the
     * argument table.
     * 
     * @param table The table to observe.
     * @throws NullPointerException If the argument is null.
     */
    public RowHeaderHeightMouseListener(final JTable table) {
        // that's some sort of a hack: I make sure that this object is 
        // notified first when it comes to events. Some events (those which
        // trigger row resizing are e.consume()'d. The selection listeners
        // respects this and will ignore the selection event, bug fix#654
        MouseListener[] mouseListener = table.getMouseListeners();
        MouseMotionListener[] motionListener = table.getMouseMotionListeners();
        for (MouseListener m : mouseListener) {
            table.removeMouseListener(m);
        }
        for (MouseMotionListener m : motionListener) {
            table.removeMouseMotionListener(m);
        }
        table.addMouseListener(this);
        table.addMouseMotionListener(this);
        for (MouseListener m : mouseListener) {
            table.addMouseListener(m);
        }
        for (MouseMotionListener m : motionListener) {
            table.addMouseMotionListener(m);
        }
        m_table = table;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(final MouseEvent e) {
        // figure out which row we were pointing at
        Point point = e.getPoint();
        int rowIndex = m_table.rowAtPoint(point);
        if (m_table.getCursor().equals(RESIZE_CURSOR)) {
            // remember current row
            m_resizingRow = rowIndex;
            // and also current Y-coordinate of mouse
            m_yOld = point.y;
            // and remember that we are now dragging something
            m_isDragging = true;
            // must consume here, flag for the selection mouse listener
            e.consume();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(final MouseEvent e) {
        Point point = e.getPoint();
        int colIndex = m_table.columnAtPoint(point);
        int rowIndex = m_table.rowAtPoint(point);
        // check if row height of this row is adjustable
        Rectangle rect = m_table.getCellRect(rowIndex, colIndex, false);
        Cursor cursor = m_table.getCursor();
        int distFromLowerBorder = rect.height + rect.y - point.y;
        // if we are close enough to border (within PIXELS), change cursor
        if (distFromLowerBorder <= PIXELS) {
            if (!cursor.equals(RESIZE_CURSOR)) {
                swapCursor();
            }
        } else {
            // otherwise change cursor back (only if it was changed before, of
            // course)
            if (!cursor.equals(DEFAULT_CURSOR)) {
                swapCursor();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(final MouseEvent e) {
        if (m_isDragging) {
            // only if we are dragging a row height, adjust it
            int mouseY = e.getY();
            int mouseDiffY = mouseY - m_yOld;
            // figure out which row we were pointing at
            int newHeight = m_table.getRowHeight(m_resizingRow) + mouseDiffY;
            if (newHeight < 5) {
                return; // don't do anything drastic!
            }
            m_table.setRowHeight(m_resizingRow, newHeight);
            // and remember where mouse is now
            m_yOld = mouseY;
            // must consume here, flag for the selection mouse listener
            e.consume();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(final MouseEvent e) {
        if (e.isShiftDown()) {
            int height = m_table.getRowHeight(m_resizingRow);
            m_table.setRowHeight(height);
        }
        if (m_isDragging) {
            m_isDragging = false;
            e.consume();
        }
    }

    /** Swaps the current cursor and the temp cursor. */
    private void swapCursor() {
        Cursor tmp = m_table.getCursor();
        m_table.setCursor(m_tmpCursor);
        m_tmpCursor = tmp;
    }
}
