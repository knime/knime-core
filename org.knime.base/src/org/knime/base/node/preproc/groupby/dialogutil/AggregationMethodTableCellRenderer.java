/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files are protected by
 * copyright law. All rights reserved. Copyright, 2003 - 2008 University of
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

import org.knime.base.node.preproc.groupby.aggregation.AggregationMethod;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * Table cell renderer that checks if the value being renderer is of type
 * <code>AggregationMethod</code> if so it will renderer the name of the method.
 * If not, the passed value's toString() method is used for rendering.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationMethodTableCellRenderer
    extends DefaultTableCellRenderer {
  private static final long serialVersionUID = -2935929914992836023L;

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        final Component c =
            super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        assert (c == this);
        if (value instanceof AggregationMethod) {
            setText(((AggregationMethod)value).getLabel());
//            setIcon(((DataColumnSpec)value).getType().getIcon());
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Left mouse click to change method. "
        + "Right mouse click for context menu.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent event) {
        return getToolTipText();
    }
}
