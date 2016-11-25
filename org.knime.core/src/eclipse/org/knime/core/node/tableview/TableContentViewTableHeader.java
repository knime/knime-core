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
 *   Aug 16, 2014 (wiswedel): created
 */
package org.knime.core.node.tableview;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.knime.core.node.util.CheckUtils;

/**
 * Custom table header that mostly handles column width events and proper initialization.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 3.3
 */
@SuppressWarnings("serial")
public class TableContentViewTableHeader extends JTableHeader {

    private final TableContentView m_contentView;

    /** @param contentView The associated table view
     * @param cm forwarded to super constructor. */
    protected TableContentViewTableHeader(final TableContentView contentView, final TableColumnModel cm) {
        super(cm);
        m_contentView = CheckUtils.checkNotNull(contentView);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("null")
    @Override
    protected void processMouseEvent(final MouseEvent e) {
        TableColumn resizingColumn2 = getResizingColumn();
        super.processMouseEvent(e);
        boolean applyToAll = e.getID() == MouseEvent.MOUSE_RELEASED && e.isShiftDown() && resizingColumn2 != null;
        if (applyToAll) {
            int columnIndexAtX = columnAtPoint(e.getPoint());
            Rectangle tableVisibleRect = m_contentView.getVisibleRect();
            // oldColumnRect is already with new width (this method is called after the resizing column was resized)
            // usually oldColumnRect.width == newColumnRect.width but I found (but could not explain) difference when
            // resizing was done outside the viewport
            Rectangle oldColumnRect = getHeaderRect(columnIndexAtX);
            int spaceLeft = oldColumnRect.x - tableVisibleRect.x;
            int spaceRight = tableVisibleRect.x + tableVisibleRect.width - oldColumnRect.x - oldColumnRect.width;
            m_contentView.setColumnWidth(resizingColumn2.getWidth());
            Rectangle newColumnRect = getHeaderRect(columnIndexAtX);
            final int scrollX = newColumnRect.x - spaceLeft;
            final int scrollWidth = spaceLeft + newColumnRect.width + spaceRight;
            Rectangle newVisibleRect = new Rectangle(scrollX, tableVisibleRect.y, scrollWidth, tableVisibleRect.height);
            m_contentView.scrollRectToVisible(newVisibleRect);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Dimension getPreferredSize() {
        // get preferred width for all columns and if there is any one that needs more than allocated
        // space add at most two more "rows" to the column header
        Dimension d = super.getPreferredSize();
        if (isPreferredSizeSet()) {
            return d;
        }
        TableCellRenderer r = getDefaultRenderer();
        TableColumnModel cM = getColumnModel();
        int prefHeight = d.height;
        if (r instanceof ColumnHeaderRenderer && ((ColumnHeaderRenderer)r).isWrapHeader()) {
            ColumnHeaderRenderer chr = (ColumnHeaderRenderer)r;
            for (Enumeration<TableColumn> enu = cM.getColumns(); enu.hasMoreElements();) {
                TableColumn tc = enu.nextElement();
                int tcPreferredWidth = tc.getWidth(); // includes icon
                // this is what tc.sizeWidthToFit() does, too
                int col = m_contentView.convertColumnIndexToView(tc.getModelIndex());
                Component c = chr.getTableCellRendererComponent(m_contentView,
                    tc.getHeaderValue(), false, false, 0, col);
                Dimension prefSize = c.getPreferredSize();
                int prefTextWidth = prefSize.width; // includes icon
                if (c == chr) { // almost surely, unless overwritten
                    int prefTextWidth2 = chr.getPreferredTextWidth();
                    if (prefTextWidth2 > 0) {       // correct by icon space
                        int iconWidth = prefSize.width - prefTextWidth2;
                        prefTextWidth = prefTextWidth2;
                        tcPreferredWidth -= iconWidth;
                    }
                }
                int tcBestHeight;
                if (prefTextWidth > 2 * tcPreferredWidth) {
                    tcBestHeight = 3 * d.height;
                } else if (prefTextWidth > tcPreferredWidth) {
                    tcBestHeight = 2 * d.height;
                } else {
                    tcBestHeight = d.height;
                }
                prefHeight = Math.max(prefHeight, Math.min(80, tcBestHeight));
            }
        }
        if (prefHeight != d.height) {
            return new Dimension(d.width, prefHeight);
        }
        return d;
    }
}