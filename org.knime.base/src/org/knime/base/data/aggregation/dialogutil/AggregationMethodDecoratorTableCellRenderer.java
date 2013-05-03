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
 */
package org.knime.base.data.aggregation.dialogutil;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.knime.base.data.aggregation.AggregationMethodDecorator;

/**
 * Table cell renderer that checks if the value being renderer is of type <code>AggregationMethodDecorator</code>
 * if so it uses the given {@link ValueRenderer} implementation to render a specific value.
 * If not, the passed value's toString() method is used for rendering. The renderer also handles
 * the special rendering of invalid {@link AggregationMethodDecorator}s.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.8
 */
public class AggregationMethodDecoratorTableCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    private final ValueRenderer m_renderer;

    private final boolean m_checkValidFlag;

    /** Used to render a specific value of an {@link AggregationMethodDecorator}. */
    public interface ValueRenderer {
        /**
         * @param c the {@link DefaultTableCellRenderer} to display the value
         * @param method the {@link AggregationMethodDecorator} to display the value for
         */
        public void renderComponent(final DefaultTableCellRenderer c, final AggregationMethodDecorator method);
    }

    /**
     * @param valueRenderer the {@link ValueRenderer} to use
     */
    public AggregationMethodDecoratorTableCellRenderer(final ValueRenderer valueRenderer) {
        this(valueRenderer, true);
    }

    /**
     * @param valueRenderer the {@link ValueRenderer} to use
     * @param checkValidFlag <code>true</code> if the valid flag of the {@link AggregationMethodDecorator}
     * should be checked
     */
    public AggregationMethodDecoratorTableCellRenderer(final ValueRenderer valueRenderer,
                                                       final boolean checkValidFlag) {
        if (valueRenderer == null) {
            throw new NullPointerException("renderer must not be null");
        }
        m_renderer = valueRenderer;
        m_checkValidFlag = checkValidFlag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
        final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        assert (c == this);
        if (value instanceof AggregationMethodDecorator) {
            final AggregationMethodDecorator method = (AggregationMethodDecorator)value;
            m_renderer.renderComponent(this, method);
            if (m_checkValidFlag && !method.isValid()) {
                //set a red border for invalid methods
                setBorder(BorderFactory.createLineBorder(Color.RED));
            } else {
                setBorder(null);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Left mouse click to change method. " + "Right mouse click for context menu.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent event) {
        return getToolTipText();
    }
}
