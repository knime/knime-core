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
