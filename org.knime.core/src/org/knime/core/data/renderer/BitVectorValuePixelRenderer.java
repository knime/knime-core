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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;

import org.knime.core.data.vector.bitvector.BitVectorValue;

/**
 * Paints {@link BitVectorValue} elements. Each bit is represented by a little
 * bar, which is either painted (bit = set) or not.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class BitVectorValuePixelRenderer extends
        AbstractPainterDataValueRenderer {

    private BitVectorValue m_bitVector;

    /** {@inheritDoc} */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof BitVectorValue) {
            m_bitVector = (BitVectorValue)value;
        } else {
            m_bitVector = null;
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
        if (m_bitVector == null) {
            return;
        }
        int length = (int)Math.min(Integer.MAX_VALUE, m_bitVector.length());
        if (length == 0) {
            return;
        }
        Insets ins = getInsets();
        int xOff = ins.left;
        int size = getWidth() - ins.left - ins.right;
        int yBot = getHeight() - ins.bottom;
        if (size < length) {
            int bitsPerPixel = length / size;
            int missingBitCount = length - (bitsPerPixel * size);
            double missingBitCountRatio = missingBitCount / (double)size;
            int onCount = 0;
            int curIndex = 0;
            int pix = 0;
            int nextPixelBitCount = bitsPerPixel;
            int missingBitsProcessed = 0;
            for (int i = length - 1; i >= 0; i--) {
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
                onCount += m_bitVector.get(i) ? 1 : 0;
                curIndex++;
            }
        } else {
            int pixelPerBit = size / length;
            int missingPixelCount = size - (pixelPerBit * length);
            double missingPixelRatio = missingPixelCount / (double)size;
            int missingPixelProcessed = 0;
            int pix = 0;
            for (int i = length - 1; i >= 0; i--) {
                int sizeInPix = pixelPerBit;
                if (pix > 0
                        && missingPixelProcessed / (double)pix < missingPixelRatio) {
                    sizeInPix += 1;
                    missingPixelProcessed += 1;
                }
                if (m_bitVector.get(i)) {
                    g.setColor(Color.BLACK);
                    g.fillRect(xOff + pix, ins.top, sizeInPix, yBot - ins.top);
                }
                pix += sizeInPix;
            }
        }
    }

}
