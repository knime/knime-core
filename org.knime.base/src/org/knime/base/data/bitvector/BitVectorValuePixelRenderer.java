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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.BitSet;

import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;

/**
 * Paints {@link BitVectorValue} elements. Each bit is represented by a little
 * bar, which is either painted (bit = set) or not.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @deprecated use the
 *             {@link org.knime.core.data.renderer.BitVectorValuePixelRenderer}
 *             from the core plug-in instead
 */
final class BitVectorValuePixelRenderer extends
        AbstractPainterDataValueRenderer {

    private BitSet m_bitSet;

    private int m_length;

    /** {@inheritDoc} */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof BitVectorValue) {
            m_bitSet = ((BitVectorValue)value).getBitSet();
            m_length = ((BitVectorValue)value).getNumBits();
        } else {
            m_bitSet = null;
            m_length = -1;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Bit Scratch";
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (m_bitSet == null) {
            return;
        }
        Insets ins = getInsets();
        int xOff = ins.left;
        int size = getWidth() - ins.left - ins.right;
        int yBot = getHeight() - ins.bottom;
        if (size < m_length) {
            int bitsPerPixel = m_length / size;
            int missingBitCount = m_length - (bitsPerPixel * size);
            double missingBitCountRatio = missingBitCount / (double)size;
            int onCount = 0;
            int curIndex = 0;
            int pix = 0;
            int nextPixelBitCount = bitsPerPixel;
            int missingBitsProcessed = 0;
            for (int i = 0; i < m_length; i++) {
                if (curIndex == nextPixelBitCount) {
                    float ratio = 1.0f - (onCount / (float)nextPixelBitCount);
                    g.setColor(new Color(ratio, ratio, ratio));
                    g.drawLine(xOff + pix, ins.top, xOff + pix, yBot);
                    pix++;
                    curIndex = 0;
                    onCount = 0;
                    nextPixelBitCount = bitsPerPixel;
                    double missRatio = missingBitsProcessed / (double)pix;
                    if (missRatio < missingBitCountRatio) {
                        missingBitsProcessed += 1;
                        nextPixelBitCount += 1;
                    }
                }
                onCount += m_bitSet.get(i) ? 1 : 0;
                curIndex++;
            }
        } else {
            int pixelPerBit = size / m_length;
            int missingPixelCount = size - (pixelPerBit * m_length);
            double missingPixelRatio = missingPixelCount / (double)size;
            int missingPixelProcessed = 0;
            int pix = 0;
            for (int i = 0; i < m_length; i++) {
                int sizeInPix = pixelPerBit;
                if (pix > 0
                        && missingPixelProcessed / (double)pix < missingPixelRatio) {
                    sizeInPix += 1;
                    missingPixelProcessed += 1;
                }
                if (m_bitSet.get(i)) {
                    g.setColor(Color.BLACK);
                    g.fillRect(xOff + pix, ins.top, sizeInPix, yBot - ins.top);
                }
                pix += sizeInPix;
            }
        }
    }

}
