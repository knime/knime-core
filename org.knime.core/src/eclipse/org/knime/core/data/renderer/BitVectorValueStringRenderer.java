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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 4, 2007 (wiswedel): created
 */
package org.knime.core.data.renderer;

import java.awt.Dimension;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.vector.bitvector.BitVectorValue;

/**
 * Renderer for bit vector values.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class BitVectorValueStringRenderer extends DefaultDataValueRenderer {
    /**
     * Renderer that shows bit vectors as hex strings.
     *
     * @since 2.8
     */
    public static final class HexRendererFactory extends AbstractDataValueRendererFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return HexRenderer.DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new HexRenderer();
        }
    }


    /**
     * Renderer that shows bit vectors as binary strings.
     *
     * @since 2.8
     */
    public static final class BinRendererFactory extends AbstractDataValueRendererFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return BinRenderer.DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new BinRenderer();
        }
    }


    /**
     * Renderer that shows the set bits and the length.
     *
     * @since 2.8
     */
    public static final class SetBitsRendererFactory extends AbstractDataValueRendererFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return SetBitsRenderer.DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new SetBitsRenderer();
        }
    }


    private static final class HexRenderer extends BitVectorValueStringRenderer {
        static final String DESCRIPTION = "Hex String";

        HexRenderer() {
            super(DESCRIPTION);
        }

        @Override
        protected void setValue(final Object value) {
            if (value instanceof BitVectorValue) {
                super.setValue(((BitVectorValue)value).toHexString());
            } else {
                super.setValue(value);
            }
        }
    }

    private static final class BinRenderer extends BitVectorValueStringRenderer {
        static final String DESCRIPTION = "Bit String";

        BinRenderer() {
            super(DESCRIPTION);
        }

        @Override
        protected void setValue(final Object value) {
            if (value instanceof BitVectorValue) {
                super.setValue(((BitVectorValue)value).toBinaryString());
            } else {
                super.setValue(value);
            }
        }
    }


    private static final class SetBitsRenderer extends BitVectorValueStringRenderer {
        static final String DESCRIPTION = "Set bits";

        SetBitsRenderer() {
            super(DESCRIPTION);
        }

        @Override
        protected void setValue(final Object value) {
            if (value instanceof BitVectorValue) {
                BitVectorValue bv = (BitVectorValue)value;

                StringBuilder result = new StringBuilder(BitVectorValue.MAX_DISPLAY_BITS * 5);
                long length = bv.length();
                result.append("{length=").append(length).append(", set bits=");
                int setBits = 0;
                for (long i = 0; (i < length) && (setBits < BitVectorValue.MAX_DISPLAY_BITS); i++) {
                    if (bv.get(i)) {
                        result.append(i).append(", ");
                        setBits++;
                    }
                }
                if (setBits < bv.cardinality()) {
                    result.append("... ");
                } else if (result.length() > 2) {
                    result.delete(result.length() - 2, result.length());
                }
                result.append('}');
                super.setValue(result.toString());
            } else {
                super.setValue(value);
            }
        }
    }

    /**
     * Singleton for a hex string renderer of bit vector values.
     * @deprecated Do not use this singleton instance, renderers are not thread-safe!
     */
    @Deprecated
    public static final BitVectorValueStringRenderer HEX_RENDERER = new HexRenderer();

    /**
     * Singleton for a binary (0/1) string renderer of bit vector values.
     * @deprecated Do not use this singleton instance, renderers are not thread-safe!
     */
    @Deprecated
    public static final BitVectorValueStringRenderer BIN_RENDERER = new BinRenderer();


    /**
     * Constructs new renderer.
     *
     * @param description a description for the renderer
     */
    BitVectorValueStringRenderer(final String description) {
        super(description);
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
