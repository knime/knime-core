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
package org.knime.base.data.bitvector;

import java.awt.Dimension;
import java.util.BitSet;

import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @deprecated use the
 *             {@link org.knime.core.data.renderer.BitVectorValueStringRenderer}
 *             from the core plug-in instead.
 */
final class BitVectorValueStringRenderer extends DefaultDataValueRenderer {

    /** Possible types for string representation. */
    static enum Type {
        /** Hex string representation. */
        HEX,
        /** Bit string representation. */
        BIT
    };

    private StringBuilder m_stringBuilder;

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
                if (m_stringBuilder == null) {
                    m_stringBuilder = new StringBuilder(bv.getNumBits());
                }
                m_stringBuilder.setLength(0);
                BitSet s = bv.getBitSet();
                for (int i = 0; i < bv.getNumBits(); i++) {
                    m_stringBuilder.append(s.get(i) ? '1' : '0');
                }
                val = m_stringBuilder.toString();
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
