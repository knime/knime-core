/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import org.knime.base.data.aggregation.AggregationMethod;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;


/**
 * Include missing cell table cell renderer that contains most
 * of the code from the {JTable$BooleanRenderer} class.
 * @author Tobias Koetter, University of Konstanz
 */
public class IncludeMissingCellRenderer extends JCheckBox
implements TableCellRenderer, UIResource {
    private static final long serialVersionUID = 4646190851811197484L;
    private static final Border NO_FOCUS_BORDER =
        new EmptyBorder(1, 1, 1, 1);
    private final TableModel m_model;

    /**Constructor for class IncludeMissingCellRenderer.
     * @param tableModel the table model with the values that should
     * be rendered
     */
    public IncludeMissingCellRenderer(final TableModel tableModel) {
        super();
        setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(true);
        if (tableModel == null) {
            throw new NullPointerException(
                    "Table model must not be null");
        }
        m_model = tableModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        setSelected((value != null && ((Boolean)value).booleanValue()));

        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        } else {
            setBorder(NO_FOCUS_BORDER);
        }

        final Object methodVal = m_model.getValueAt(row, 1);
        if (methodVal instanceof AggregationMethod) {
            final AggregationMethod method =
                (AggregationMethod)methodVal;
            setEnabled(method.supportsMissingValueOption());
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        final String superText = super.getToolTipText();
        if (superText != null) {
            return superText;
        }
        return "Tick to include missing cells";
    }
}
