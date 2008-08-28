/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files are protected by
 * copyright law. All rights reserved. Copyright, 2003 - 2006 University of
 * Konstanz, Germany. Chair for Bioinformatics and Information Mining Prof. Dr.
 * Michael R. Berthold You may not modify, publish, transmit, transfer or sell,
 * reproduce, create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as otherwise
 * expressly permitted in writing by the copyright owner or as specified in the
 * license file distributed with this product. If you have any questions please
 * contact the copyright holder: website: www.knime.org email: contact@knime.org
 * -------------------------------------------------------------------
 * History
 * 27.08.2008 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.groupby.dialogutil;

import org.knime.core.data.DataColumnSpec;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * Renderer that checks if the value being renderer is of type
 * <code>DataColumnSpec</code> if so it will renderer the name of the column
 * spec and also the type's icon. If not, the passed value's toString() method
 * is used for rendering.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class DataColumnSpecTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = -3420798953925662402L;

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        // The super method will reset the icon if we call this method
        // last. So we let super do its job first and then we take care
        // that everything is properly set.
        final Component c =
            super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        assert (c == this);
        if (value instanceof DataColumnSpec) {
            setText(((DataColumnSpec)value).getName());
            setIcon(((DataColumnSpec)value).getType().getIcon());
        }
        return this;
    }
}
