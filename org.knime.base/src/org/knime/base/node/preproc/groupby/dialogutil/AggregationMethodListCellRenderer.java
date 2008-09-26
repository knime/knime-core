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

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;


/**
 * List cell renderer that checks if the value being renderer is of type
 * <code>AggregationMethod</code> if so it will renderer the name of the method.
 * If not, the passed value's toString() method is used for rendering.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationMethodListCellRenderer
    extends DefaultListCellRenderer {

    private static final long serialVersionUID = -5113725870350491440L;

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        final Component c =
            super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);
        assert (c == this);
        if (value instanceof AggregationMethod) {
            setText(((AggregationMethod)value).getLabel());
//            setIcon(((DataColumnSpec)value).getType().getIcon());
        }
        return this;
    }

}
