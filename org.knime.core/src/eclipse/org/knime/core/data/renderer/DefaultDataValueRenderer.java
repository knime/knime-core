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
 *
 */
package org.knime.core.data.renderer;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import org.knime.core.data.DataColumnSpec;


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
public class DefaultDataValueRenderer
    extends DefaultTableCellRenderer implements DataValueRenderer {

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
        m_description = description == null ? "Default" : description;
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
    public Component getListCellRendererComponent(
            final JList list, final Object value, final int index,
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

        setValue(value);
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBorder((cellHasFocus)
                ? UIManager.getBorder("List.focusCellHighlightBorder")
                : noFocusBorder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getRendererComponent(final Object val) {
        setValue(val);
        return this;
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

}
