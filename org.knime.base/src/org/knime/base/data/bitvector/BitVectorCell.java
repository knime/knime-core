/* 
 * 
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
 *   01.12.2004 (berthold): created
 */
package org.knime.base.data.bitvector;

import java.util.BitSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;


/**
 * An implementation of a cell holding an entire vector of bits.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class BitVectorCell extends DataCell implements BitVectorValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(BitVectorCell.class)</code>.
     * 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(BitVectorCell.class);

    /**
     * Preferred value class of a BitVectorCell is BitVectorValue. This method
     * is called per reflection.
     * 
     * @return BitVectorValue.class
     * @see DataCell
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return BitVectorValue.class;
    }

    private final BitSet m_bits; // store all those bits in java's

    // BitSet.

    private final int m_nrBits; // remember this as well


    // Note: BitSet.length() doesn't work! (returns length only up to last '1')

    /**
     * Create new BitVectorCell with a predefined value, read from a string
     * which holds a hexadecimal representation. Resulting bit vector assumes
     * lowest-to-highest order in blocks of 32 bits.
     * 
     * @param hex the hexadecimal representation of the bit vector's value
     * @throws NumberFormatException if that fails
     */
    public BitVectorCell(final String hex) {
        String hexString = hex;
        int rest = hexString.length() % 8;
        if (rest > 0) {
            // fill the hexString with zeros to length that is a multiple of 8
            StringBuffer prefix = new StringBuffer(rest);
            for (int i = 0; i < 8 - rest; i++) {
                prefix.append('0');
            }
            hexString = prefix.toString().concat(hexString);
        }
        m_nrBits = 4 * hexString.length();
        m_bits = new BitSet(m_nrBits);
        m_bits.clear();
        for (int bitIndex = 0; bitIndex < m_nrBits; bitIndex += 4) {
            // which block of 8 characters does this bit come from?
            int blockIndex = bitIndex / 32;
            // which character represents the next 4 bits
            int charIndex = 7 - (bitIndex - blockIndex * 32) / 4;
            char hexChar = hexString.charAt(blockIndex * 8 + charIndex);
            int hexAsNumber = 0;
            if (('0' <= hexChar) && (hexChar <= '9')) {
                hexAsNumber = hexChar - '0';
            } else if (('A' <= hexChar) && (hexChar <= 'F')) {
                hexAsNumber = 10 + hexChar - 'A';
            } else if (('a' <= hexChar) && (hexChar <= 'f')) {
                hexAsNumber = 10 + hexChar - 'a';
            } else {
                throw new NumberFormatException("not a hex-character");
            }
            if (hexAsNumber >= 8) {
                m_bits.set(bitIndex + 3);
                hexAsNumber = hexAsNumber - 8;
            } else {
                m_bits.clear(bitIndex + 3);
            }
            if (hexAsNumber >= 4) {
                m_bits.set(bitIndex + 2);
                hexAsNumber = hexAsNumber - 4;
            } else {
                m_bits.clear(bitIndex + 2);
            }
            if (hexAsNumber >= 2) {
                m_bits.set(bitIndex + 1);
                hexAsNumber = hexAsNumber - 2;
            } else {
                m_bits.clear(bitIndex + 1);
            }
            if (hexAsNumber >= 1) {
                m_bits.set(bitIndex + 0);
                hexAsNumber = hexAsNumber - 1;
            } else {
                m_bits.clear(bitIndex + 0);
            }
            if (hexAsNumber != 0) {
                throw new NumberFormatException(
                        "DataElement HexChar wasn't converted correctly.");
            }
        }
    } // BitVectorCell(String)

    /**
     * Creates a new BitVectorCell based on a BitSet and a fixed length for the
     * bits.
     * 
     * @param bits the bit set to be put in the cell
     * @param nrOfBits the number for the fixed length of the cell
     */
    public BitVectorCell(final BitSet bits, final int nrOfBits) {
        if (bits != null) {
            assert bits.length() <= nrOfBits;
        }
        m_nrBits = nrOfBits;
        m_bits = bits;
    }

    /**
     * Provide inverse routing, converting a bit vector into the corresponding
     * hexadecimal representation.
     * 
     * @return hex representation
     */
    public String toHexString() {
        final BitSet set = getBitSet();
        // compute number of hex characters, which come in blocks of 4!
        final int nrHexChars = ((getNumBits() / 4 + 1) / 8 + 1) * 8;
        assert (nrHexChars % 8 == 0);
        assert (nrHexChars > (getNumBits() / 4 + 1));
        // reserve space for resulting string
        final StringBuffer buf = new StringBuffer(nrHexChars);
        for (int b = 0; b < getNumBits(); b += 32) {
            // process bits in chunks of 32 (= 8 chars)
            for (int blockId = 7; blockId >= 0; blockId--) {
                // go through the 8 blocks backwards
                // convert block of 4 bits to one hex character
                int i = 0;
                for (int k = 0; k < 4; k++) {
                    int bitIndex = b + k + (blockId * 4);
                    if (bitIndex < getNumBits()) {
                        i += (1 << k) * (set.get(bitIndex) ? 1 : 0);
                    }
                }
                assert (i >= 0 && i < 16);
                int charI = i + '0';
                if (charI > '9') {
                    charI += ('A' - ('9' + 1));
                }
                // add character to string
                buf.append((char)(charI));
            }
        }
        // done, return hex representation
        return buf.toString();
    }

    /**
     * The old way of constructing the bitvector cell's bitvector. Create new
     * <code>BitVectorCell</code> with a predefined value, read from a string
     * which holds a hexadecimal representation. Resulting bit vector assumes
     * highest-to-lowest order, that is, the bits for the 2^3 will be left-most
     * in each group of 4 bits. Other than that the 4-bit groups will follow the
     * order of hex characters.
     * 
     * @param hex the hexadecimal representation of the bit vector's value
     * @return the corresponding bit set
     */
    static BitSet oldWay(final String hex) {
        // convert hex string to upper case
        final String uhex = hex.toUpperCase();
        int nrBits = 4 * uhex.length();
        BitSet bits = new BitSet(nrBits);
        bits.clear();
        for (int i = 0; i < uhex.length(); i++) { // iterate over all
                                                    // hex-chars
            char c = uhex.charAt(i);
            // convert hex-value into integer - hopefully in [0,15]
            int val = c - '0';
            if (val >= 10) {
                val = val - ('A' - '0') + 10; // TODO 'a' vs. 'A'
            }
            // handle non-meaningful cell values:
            if (!((0 <= val) && (val <= 15))) {
                throw new NumberFormatException("not a hex-character at index "
                        + i + ": " + uhex.charAt(i));
            }
            // set appropriate bits for 8, 4, 2, 1 position.
            if (val >= 8) {
                bits.set(i * 4 + 0);
                val = val - 8;
            }
            if (val >= 4) {
                bits.set(i * 4 + 1);
                val = val - 4;
            }
            if (val >= 2) {
                bits.set(i * 4 + 2);
                val = val - 2;
            }
            if (val >= 1) {
                bits.set(i * 4 + 3);
            }
        }
        return bits;
    }

    /**
     * @return number of bits actually used
     */
    public int getNumBits() {
        return m_nrBits;
    }

    /**
     * @return the underlying bit set
     * @see java.util.BitSet
     */
    public BitSet getBitSet() {
        return m_bits;
    }

    /**
     * @return a bit string
     */
    @Override
    public String toString() {
        StringBuffer res = new StringBuffer(m_nrBits);
        for (int i = 0; i < m_nrBits; i++) {
            res.append(m_bits.get(i) ? 1 : 0);
        }
        return res.toString();
    }

    /**
     * Returns cardinality of this bit vector.
     * 
     * @see org.knime.core.data.IntValue#getIntValue()
     * @return cardinality of this bit vector
     */
    public int getIntValue() {
        return m_bits.cardinality();
    }

    /**
     * Check if two BitVectorCells are equal.
     * 
     * @param o the other object to check
     * @return <code>true</code> if this instance and the given object are
     *         instances of the same class and their string representations are
     *         equal
     * 
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equalsDataCell(final DataCell o) {
        // true of called on the same objects
        if (this == o) {
            return true;
        }
        // check for null pointer
        if (o == null) {
            return false;
        }
        // only do a real check if objects are of same class
        if (this.getClass() == o.getClass()) {
            // if both cells are missing they are equal
            if (this.isMissing() && o.isMissing()) {
                return true;
            }
            // if only one of both cells is missing they can not be equal
            if (this.isMissing() || o.isMissing()) {
                return false;
            }
            // check if string representations are equal.
            return m_bits.equals(((BitVectorCell)o).m_bits);
        }
        // no, they are not equal
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // return the bitsets hashcode
        return m_bits.hashCode();
    }

    private static final String KEY = "bitvector";

    /**
     * Saves this cell to the config.
     * 
     * @param config to write into it
     * 
     * @see #toString()
     */
    public void save(final Config config) {
        config.addString(KEY, this.toString());
    }

    /**
     * Loads a new cell using the properties from the given config.
     * 
     * @param config the config to get String from and inits this cell
     * @return a new bit vector cell
     * @throws InvalidSettingsException if the value is not available
     */
    public static DataCell load(final Config config)
            throws InvalidSettingsException {
        final String s = config.getString(KEY);
        if (s != null) {
            return new BitVectorCell(s);
        }
        return null;
    }
}
