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
 */
package org.knime.base.node.io.filereader;

import java.awt.event.MouseEvent;

import javax.swing.table.JTableHeader;

import org.knime.core.node.tableview.TableContentView;


/**
 * Extension of an KNIME table view that reacts on mouse events in the header.
 * It will sent a property change event to anyone interested in whenever the
 * column header in the table is clicked on. It will sent the column index with
 * the event as "new value" (last argument), the "old value" (middle argument)
 * will always be <code>null</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PreviewTableContentView extends TableContentView {

    /** Property event ID when table spec has changed. */
    public static final String PROPERTY_SPEC_CHANGED = "TableSpecChanged";

    /**
     * Disallows reordering.
     * 
     * @see javax.swing.JTable#setTableHeader(javax.swing.table.JTableHeader)
     */
    @Override
    public void setTableHeader(final JTableHeader newTableHeader) {
        if (newTableHeader != null) {
            newTableHeader.setReorderingAllowed(false);
        }
        super.setTableHeader(newTableHeader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMouseClickInHeader(final MouseEvent e) {
        JTableHeader header = getTableHeader();
        // get column in which event occurred
        int column = header.columnAtPoint(e.getPoint());
        if (column < 0) {
            return;
        }
        int modelIndex = convertColumnIndexToModel(column);
        firePropertyChange(PROPERTY_SPEC_CHANGED, null, modelIndex);
    }
}
