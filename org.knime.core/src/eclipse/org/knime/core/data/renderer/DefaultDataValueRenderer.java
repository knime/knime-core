/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingValue;
import org.knime.core.node.NodeLogger;


/**
 * Default implementation for a renderer for
 * {@link org.knime.core.data.DataValue} objects. This class should be used
 * (better: derived from) when the rendering is only a string representation
 * of the <code>DataValue</code> object. It's recommended to derive this class
 * and overwrite the {@link DefaultTableCellRenderer#setValue(Object)} and
 * the {@link #getDescription()} methods. A correct implementation of
 * <code>setValue(Object)</code> will test if the argument object is of the
 * expected <code>DataValue</code> class and call
 * <code>super.setValue(Object)</code> with the desired string representation.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class DefaultDataValueRenderer extends DefaultTableCellRenderer implements DataValueRenderer {
    /**
     * Factory for a default (fallback) renderer that uses the string representation of a cell.
     *
     * @since 2.8
     */
    public static final class Factory extends AbstractDataValueRendererFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return DEFAULT_DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new DefaultDataValueRenderer(colSpec, DEFAULT_DESCRIPTION);
        }
    }

    private static final String DEFAULT_DESCRIPTION = "Default";

    /** The spec to the column for which this renderer is being used. */
    private final DataColumnSpec m_colSpec;
    /** Returned in {@link #getDescription()}, not null. */
    private final String m_description;

    /** Creates new instance given a null column spec. and a default description. */
    public DefaultDataValueRenderer() {
        this((String)null);
    }

    /** Creates instance with null column spec and the given description.
     * @param description The value returned in {@link #getDescription()} (if null a default is used).
     * @since 2.7
     */
    public DefaultDataValueRenderer(final String description) {
        this(null, description);
    }

    /**
     * Creates new renderer and memorizes the column spec. The argument may
     * be, however, null.
     * @param spec The column spec of the column for which this renderer is
     * used.
     */
    public DefaultDataValueRenderer(final DataColumnSpec spec) {
        this(spec, null);
    }

    /** Create new instance with given arguments. Both arguments may be null.
     * Creates new renderer and keeps the column spec.
     * @param spec The column spec of the column for which this renderer is
     * used.
     * @param description The description shown in {@link #getDescription()} (if null a default is used).
     * @since 2.7
     */
    public DefaultDataValueRenderer(final DataColumnSpec spec, final String description) {
        m_colSpec = spec;
        m_description = description == null ? DEFAULT_DESCRIPTION : description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return m_description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index,
        final boolean isSelected, final boolean cellHasFocus) {
        if (isMissingValue(value)) {
            return getListMissingValueRendererComponent(list, value, index, isSelected, cellHasFocus);
        } else {
            resetMissingValueRenderer();
            return getCustomListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    private Component getCustomListCellRendererComponent(final JList list, final Object value, final int index,
        final boolean isSelected, final boolean cellHasFocus) {
        /* Copied almost all code from DefaultListCellRenderer */
        setComponentOrientation(list.getComponentOrientation());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setValueCatchingException(value);
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
        final boolean hasFocus, final int row, final int column) {
        if (isMissingValue(value)) {
            return getTableMissingValueRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            resetMissingValueRenderer();
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getRendererComponent(final Object val) {
        if (isMissingValue(val)) {
            return getMissingValueRendererComponent(val);
        } else {
            resetMissingValueRenderer();
            setValueCatchingException(val);
            return this;
        }
    }

    private void setValueCatchingException(final Object value) {
        try {
            setValue(value);
        } catch (Exception e) {
            String valS = value != null ? value.toString() : "<null>";
            if (valS.length() > 20) {
                valS = valS.substring(0, 20).concat("...");
            }
            StringBuilder b = new StringBuilder("Failed setting new value (\"");
            b.append(valS).append("\"");
            if (value != null) {
                b.append(" - class ").append(value.getClass().getSimpleName());
            }
            b.append(")");
            NodeLogger.getLogger(getClass()).coding(b.toString(), e);
            setValue(DataType.getMissingCell());
        }
    }

    /**
     * Get reference to the constructor's argument. The return value may be
     * null (in particular if the empty constructor has been used).
     * @return The column spec for this renderer.
     */
    protected DataColumnSpec getColSpec() {
        return m_colSpec;
    }

    /**
     * Returns always <code>true</code>.
     * {@inheritDoc}
     */
    @Override
    public boolean accepts(final DataColumnSpec spec) {
        return true;
    }


    /* ----- Methods for missing value rendering, might be overridden by implementing subclasses in order to change the behavior ------ */

    /**
     * @param value the value to check whether it's a missing value
     * @return <code>true</code> if its regarded as a missing value
     * @since 3.3
     */
    protected boolean isMissingValue(final Object value) {
        return value instanceof MissingValue;
    }

    /**
     * Returns the render component for a table if the respective value is a missing value.
     *
     * @param table
     * @param value
     * @param isSelected
     * @param hasFocus
     * @param row
     * @param column
     * @return returns <code>this</code> since {@link DefaultTableCellRenderer} is a {@link Component} by itself
     * @since 3.3
     */
    protected Component getTableMissingValueRendererComponent(final JTable table, final Object value, final boolean isSelected,
        final boolean hasFocus, final int row, final int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        return getMissingValueRendererComponent(value);
    }

    /**
     * Returns the render component for a list if the respective value is a missing value.
     *
     * @param list
     * @param value
     * @param index
     * @param isSelected
     * @param cellHasFocus
     * @return returns <code>this</code> since {@link DefaultListCellRenderer} is a {@link Component} by itself
     * @since 3.3
     */
    protected Component getListMissingValueRendererComponent(
            final JList list, final Object value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {
        getCustomListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        return getMissingValueRendererComponent(value);
    }

    /**
     * Manly to avoid code duplication. Method used by
     * {@link #getTableMissingValueRendererComponent(JTable, Object, boolean, boolean, int, int)} and
     * {@link #getListMissingValueRendererComponent(JList, Object, int, boolean, boolean)}.
     *
     * @param value
     * @return returns <code>this</code> since the {@link DefaultDataValueRenderer} is a {@link Component} by itself
     * @since 3.3
     */
    protected Component getMissingValueRendererComponent(final Object value) {
        setForeground(Color.RED);
        MissingValue val = (MissingValue)value;
        setToolTipText("Missing Value" + (val.getError() != null ? " (" + val.getError() + ")" : ""));
        setValue("?");
        return this;
    }

    /**
     * Since all the methods {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}
     * {@link #getTableMissingValueRendererComponent(JTable, Object, boolean, boolean, int, int)} etc. return
     * <code>this</code> as the component to be rendered, the state of this object is important.
     *
     * This method allows one to reset the state when <code>this</code> value renderer has been used as a missing value
     * renderer before (e.g. resetting the foreground color or the tooltip text).
     * @since 3.3
     *
     */
    protected void resetMissingValueRenderer() {
        setToolTipText(null);
        setForeground(null);
    }

}
