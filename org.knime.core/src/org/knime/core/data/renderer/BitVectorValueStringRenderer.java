/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 4, 2007 (wiswedel): created
 */
package org.knime.core.data.renderer;

import java.awt.Dimension;

import org.knime.core.data.vector.bitvector.BitVectorValue;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class BitVectorValueStringRenderer extends DefaultDataValueRenderer {

    /**
     * Singleton for a hex string renderer of bit vector values.
     */
    public static final BitVectorValueStringRenderer HEX_RENDERER =
            new BitVectorValueStringRenderer(Type.HEX);

    /**
     * Singleton for a binary (0/1) string renderer of bit vector values.
     */
    public static final BitVectorValueStringRenderer BIN_RENDERER =
        new BitVectorValueStringRenderer(Type.BIT);

    /** Possible types for string representation. */
    static enum Type {
        /** Hex string representation. */
        HEX,
        /** Bit string representation. */
        BIT
    };

    private final Type m_type;

    /**
     * Constructs new renderer.
     *
     * @param type The type to use.
     */
    BitVectorValueStringRenderer(final Type type) {
        if (type == null) {
            throw new NullPointerException("Type must not be null.");
        }
        m_type = type;
    }

    /** {@inheritDoc} */
    @Override
    protected void setValue(final Object value) {
        Object val;
        if (value instanceof BitVectorValue) {
            BitVectorValue bv = (BitVectorValue)value;
            switch (m_type) {
            case HEX:
                val = bv.toHexString();
                break;
            case BIT:
                val = bv.toBinaryString();
                break;
            default:
                throw new InternalError("Unknown type: " + m_type);
            }
        } else {
            val = value;
        }
        super.setValue(val);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        switch (m_type) {
        case HEX:
            return "Hex String";
        case BIT:
            return "Bit String";
        default:
            throw new InternalError("Unknown type: " + m_type);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (d.width > 150) {
            d = new Dimension(150, d.height);
        }
        return d;
    }

}
