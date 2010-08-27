/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 *   27.02.07 (po): implements ComplexNumberValue now
 */
package org.knime.core.data.def;

import java.io.IOException;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.ComplexNumberValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.FuzzyNumberValue;


/**
 * A data cell implementation holding a double value by storing this value in a
 * private <code>double</code> member. It provides a double value and a fuzzy
 * number value, as well as a fuzzy interval value.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class DoubleCell extends DataCell 
    implements DoubleValue, ComplexNumberValue, FuzzyNumberValue, 
    FuzzyIntervalValue, BoundedValue {
    
    /** Convenience access member for 
     * <code>DataType.getType(DoubleCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = 
        DataType.getType(DoubleCell.class);

    /** Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return DoubleValue.class
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return DoubleValue.class;
    }
    
    private static final DataCellSerializer<DoubleCell> SERIALIZER = 
        new DoubleSerializer();
    
    /** Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<DoubleCell> getCellSerializer() {
        return SERIALIZER;
    }
    
    private final double m_double;

    /**
     * Creates a new cell for a generic double value. Also acting as
     * FuzzyNumberCell and FuzzyIntervalCell.
     * 
     * @param d The double value.
     */
    public DoubleCell(final double d) {
        m_double = d;
    }

    /**
     * {@inheritDoc}
     */
    public double getDoubleValue() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getCore() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxSupport() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinSupport() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxCore() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinCore() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getCenterOfGravity() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getImaginaryValue() {
        return 0.0;
    }
    
    /**
     * {@inheritDoc}
     */
    public double getRealValue() {
        return m_double;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        double o = ((DoubleCell)dc).m_double;
        if (Double.isNaN(m_double) && Double.isNaN(o)) {
            // Double.NaN is not equal to Double.NaN
            return true;
        }
        return o == m_double;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(m_double);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Double.toString(m_double);
    }
    
    /** Factory for (de-)serializing a DoubleCell. */
    private static class DoubleSerializer 
        implements DataCellSerializer<DoubleCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(
                final DoubleCell cell, final DataCellDataOutput out) 
            throws IOException {
            out.writeDouble(cell.m_double);
        }
        
        /**
         * {@inheritDoc}
         */
        public DoubleCell deserialize(final DataCellDataInput input) 
            throws IOException {
            double d = input.readDouble();
            return new DoubleCell(d);
        }
    }
}
