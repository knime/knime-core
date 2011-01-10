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
 * History
 *   23.03.2006 (cebron): created
 */
package org.knime.core.data.renderer;

import java.text.NumberFormat;
import java.util.Locale;

import org.knime.core.data.ComplexNumberValue;


/**
 * Render to display a complex number value using a given
 * <code>NumberFormat</code>.
 * 
 * @see java.text.NumberFormat
 * @author ciobaca, University of Konstanz
 */
public class ComplexNumberValueRenderer extends DefaultDataValueRenderer {

    /**
     * Singleton for ordinary representation.
     */
    public static final DataValueRenderer STANDARD_RENDERER = 
            new ComplexNumberValueRenderer(
                    NumberFormat.getNumberInstance(Locale.US), 
                    "Standard Complex Number");

    /** disable grouping in renderer */
    static {
        NumberFormat.getNumberInstance(Locale.US).setGroupingUsed(false);
    }

    /** Used to get a string representation of the complex number value. */
    private final NumberFormat m_format;

    /** Description to the renderer. */
    private final String m_desc;

    /**
     * Instantiates a new object using a given format.
     * 
     * @param format To be used to render this object.
     * @param desc The description to the renderer
     * @throws NullPointerException If argument is <code>null</code>.
     */
    public ComplexNumberValueRenderer(final NumberFormat format,
            final String desc) {
        if (format == null || desc == null) {
            throw new NullPointerException(
                    "Format/Description must not be null.");
        }
        m_format = format;
        m_desc = desc;
    }

    /**
     * Formats the object. If <code>value</code> is instance of
     * <code>ComplexNumberValue</code>, the renderer's formatter is used to get
     * a string from the complex number value of the cell. Otherwise the
     * <code>value</code>'s <code>toString()</code> method is used.
     * 
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    @Override
    protected void setValue(final Object value) {
        Object newValue;
        if (value instanceof ComplexNumberValue) {
            ComplexNumberValue cell = (ComplexNumberValue)value;
            double real = cell.getRealValue();
            double imag = cell.getImaginaryValue();
            if (Double.isNaN(real) || Double.isNaN(imag)) {
                newValue = "NaN";
            } else {
                newValue = m_format.format(real);
                if (imag < 0) {
                    newValue = newValue + " - i*" + Math.abs(imag);
                } else {
                    newValue = newValue + " + i*" + imag;
                }
            }
        } else {
            // missing data cells will also end up here
            newValue = value;
        }
        super.setValue(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return m_desc;
    }
}
