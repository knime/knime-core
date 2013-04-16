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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   29.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.columnresorter;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JList;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * The cell renderer with place holder element to render.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class DataColumnSpecListDummyCellRenderer extends 
DataColumnSpecListCellRenderer {
    private static final long serialVersionUID = 1156595670217009312L;

    /**
     * The default place holder for any new previously unknown columns. 
     */
    public static final DataColumnSpec UNKNOWN_COL_DUMMY = 
        new DataColumnSpecCreator("<any unknown new column>", 
                StringCell.TYPE).createSpec();
    
    /**
     * {@inheritDoc}
     */
    public Component getListCellRendererComponent(final JList list, 
            final Object value, final int index, final boolean isSelected, 
            final boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
        assert (c == this);
        // return place holder component
        if (value == UNKNOWN_COL_DUMMY) {
            setForeground(Color.GRAY);
            setText(UNKNOWN_COL_DUMMY.getName());
            setIcon(DataValue.UTILITY.getIcon());
        }
        return this;
    }
    
//    private DataColumnSpecListCellRenderer m_renderer = 
//        new DataColumnSpecListCellRenderer();
    
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public Component getListCellRendererComponent(
//            final JList list, final Object value, final int index,
//            final boolean isSelected, final boolean cellHasFocus) {
//        Component c =  super.getListCellRendererComponent(list, value, index,
//                isSelected, cellHasFocus);
//        assert (c == this);
//        // return place holder component
//        if (value == UNKNOWN_COL_DUMMY) {
//            setForeground(Color.GRAY);
//            setText(UNKNOWN_COL_DUMMY.getName());
//            setIcon(DataValue.UTILITY.getIcon());
//            return this;
//            
//        // regular cell renderer takes over for regular cells
//        } else {
//            return m_renderer.getListCellRendererComponent(list, value, index, 
//                isSelected, cellHasFocus);
//        }
//    }
}
