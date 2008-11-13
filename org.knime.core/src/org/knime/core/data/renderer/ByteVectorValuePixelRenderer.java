/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sept 2, 2008 (ohl): created
 */
package org.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;

import org.knime.core.data.vector.bytevector.ByteVectorValue;

/**
 * Paints {@link ByteVectorValue} elements. Each count is represented by a
 * little bar, which is either painted with the corresponding gray value.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class ByteVectorValuePixelRenderer extends
        AbstractPainterDataValueRenderer {

    /**
     * Singleton instance to be used.
     */
    public static final ByteVectorValuePixelRenderer INSTANCE =
            new ByteVectorValuePixelRenderer();

    private ByteVectorValue m_byteVector;

    /** {@inheritDoc} */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof ByteVectorValue) {
            m_byteVector = (ByteVectorValue)value;
        } else {
            m_byteVector = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Byte Scratch";
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (m_byteVector == null) {
            return;
        }
        int length = (int)Math.min(Integer.MAX_VALUE, m_byteVector.length());
        Insets ins = getInsets();
        int xOff = ins.left;
        int size = getWidth() - ins.left - ins.right;
        int yBot = getHeight() - ins.bottom;
        if (size < length) {
            long bitsPerPixel = length * 256 / size;
            long missingBitCount = length - (bitsPerPixel * size);
            double missingBitCountRatio = missingBitCount / (double)size;
            long onCount = 0;
            int curIndex = 0;
            int pix = 0;
            long nextPixelBitCount = bitsPerPixel;
            int missingBitsProcessed = 0;
            for (int i = 0; i < length; i++) {
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
                onCount += m_byteVector.get(i);
                curIndex++;
            }
        } else {
            int pixelPerBit = size / length;
            int missingPixelCount = size - (pixelPerBit * length);
            double missingPixelRatio = missingPixelCount / (double)size;
            int missingPixelProcessed = 0;
            int pix = 0;
            for (int i = 0; i < length; i++) {
                int sizeInPix = pixelPerBit;
                if (pix > 0
                        && missingPixelProcessed / (double)pix < missingPixelRatio) {
                    sizeInPix += 1;
                    missingPixelProcessed += 1;
                }
                int val = m_byteVector.get(i);
                g.setColor(new Color(val, val, val));
                g.fillRect(xOff + pix, ins.top, sizeInPix, yBot - ins.top);
                pix += sizeInPix;
            }
        }
    }

}
