/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;

/**
 * A data cell implementation holding a string value by storing this value in a
 * private {@link String} member.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class StringCell extends DataCell 
implements StringValue, NominalValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(StringCell.class)</code>.
     * 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(StringCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     * 
     * @return StringValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return StringValue.class;
    }

    private static final StringSerializer SERIALIZER = new StringSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     * 
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final StringSerializer getCellSerializer() {
        return SERIALIZER;
    }

    private final String m_string;

    /**
     * Creates a new String Cell based on the given String value.
     * 
     * @param str The String value to store.
     * @throws NullPointerException If the given String value is
     *             <code>null</code>.
     */
    public StringCell(final String str) {
        if (str == null) {
            throw new NullPointerException("String value can't be null.");
        }
        m_string = str;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringValue() {
        return m_string;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return m_string.equals(((StringCell)dc).m_string);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_string.hashCode();
    }

    /** Factory for (de-)serializing a StringCell. */
    private static class StringSerializer implements
            DataCellSerializer<StringCell> {
        /**
         * {@inheritDoc}
         */
        public void serialize(final StringCell cell, 
                final DataCellDataOutput output) throws IOException {
            output.writeUTF(cell.getStringValue());
        }

        /**
         * {@inheritDoc}
         */
        public StringCell deserialize(
                final DataCellDataInput input) throws IOException {
            String s = input.readUTF();
            return new StringCell(s);
        }

    }

}
