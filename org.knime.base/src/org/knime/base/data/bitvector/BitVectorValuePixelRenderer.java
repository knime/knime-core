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
 *   Apr 4, 2007 (wiswedel): created
 */
package org.knime.base.data.bitvector;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.BitSet;

import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;

/**
 * Paints {@link BitVectorValue} elements. Each bit is represented by a 
 * little bar, which is either painted (bit = set) or not.
 * @author Bernd Wiswedel, University of Konstanz
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
                if (pix > 0 && missingPixelProcessed / (double)pix 
                        < missingPixelRatio) {
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
