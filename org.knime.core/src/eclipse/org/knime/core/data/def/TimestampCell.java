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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.core.data.def;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.TimestampValue;

/**
 * Implementation of a <code>DataCell</code> holding day/time
 * information as {@link Date}.
 * 
 * @author M. Berthold, University of Konstanz
 * @deprecated Date and time in KNIME is represented by 
 * {@link org.knime.core.data.date.DateAndTimeValue} and
 * {@link org.knime.core.data.date.DateAndTimeCell}. This class will be removed
 * in future versions of KNIME.
 */
@Deprecated
public class TimestampCell extends DataCell implements TimestampValue {

    /** Convenience access member for 
     * <code>DataType.getType(TimestampCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(TimestampCell.class);
    
    /** Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return TimestampValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return TimestampValue.class;
    }
    
    /** Internal representation as {@link java.lang.Date}. */
    private final Date m_date;

    
    /**
     * Creates a new <code>TimestampCell</code> based on the given value.
     *
     * @param d date to be stored.
     */
    public TimestampCell(final Date d) {
        m_date = d;
    }

    /** 
     * Parse string and create new date object. Use predefined format as
     * defined as <code>yyyy-MM-dd;HH:mm:ss.S</code>.
     * @param s the input string date
     * @throws ParseException if parsing failed
     */
    public TimestampCell(final String s) throws ParseException {
        // define format
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd;HH:mm:ss.S");
        // parse string
        m_date = df.parse(s);
    }
    
    /** Parse string and create new date object. Format is also provided.
     * 
     * @param s the input string
     * @param df the format specification for the date in s
     * @throws ParseException if parsing failed
     */
    public TimestampCell(final String s, final SimpleDateFormat df)
            throws ParseException {
        this(df.parse(s));
    }
    
    /** {@inheritDoc} */
    @Override
    public Date getDate() {
        return m_date;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_date.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return m_date.equals(((TimestampValue)dc).getDate());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_date.hashCode();
    }
    
    private static final TimestampCellSerializer SERIALIZER = 
        new TimestampCellSerializer();
    
    /** Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final TimestampCellSerializer getCellSerializer() {
        return SERIALIZER;
    }
    
    /** Factory for (de-)serializing a TimestampCell. */
    private static class TimestampCellSerializer 
            implements DataCellSerializer<TimestampCell> {
        /** {@inheritDoc} */
        @Override
        public void serialize(final TimestampCell cell, 
                final DataCellDataOutput output) throws IOException {
            output.writeLong(cell.getDate().getTime());

        }
        /** {@inheritDoc} */
        @Override
        public TimestampCell deserialize(
                final DataCellDataInput input) throws IOException {
            return new TimestampCell(new Date(input.readLong()));
        }
    }

}
