/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   16.08.2013 (thor): created
 */
package org.knime.testing.internal.diffcheckers;

import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.knime.core.data.image.png.PNGImageValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.testing.core.AbstractDifferenceChecker;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;

/**
 * Checker for PNGs that uses the pHash algorithm. See
 *
 * http://www.hackerfactor.com/blog/index.php?/archives/432-Looks-Like-It.html and http://www.phash.org/
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class PngPHashChecker extends AbstractDifferenceChecker<PNGImageValue> {
    /**
     * Factory for {@link PngPHashChecker}.
     */
    public static class Factory implements DifferenceCheckerFactory<PNGImageValue> {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<PNGImageValue> getType() {
            return PNGImageValue.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DifferenceChecker<PNGImageValue> newChecker() {
            return new PngPHashChecker();
        }
    }

    private static final String DESCRIPTION = "PNG images (pHash)";

    private final SettingsModelDoubleBounded m_allowedDifference = new SettingsModelDoubleBounded("allowedDifference",
            5, 0, 100);

    private DialogComponentNumber m_allowedDifferenceComponent;

    private final SettingsModelIntegerBounded m_sampleSize = new SettingsModelIntegerBounded("sampleSize", 32, 8, 64);

    private DialogComponentNumber m_sampleSizeComponent;

    private final SettingsModelIntegerBounded m_dctSize = new SettingsModelIntegerBounded("dctSize", 8, 8, 64);

    private DialogComponentNumber m_dctSizeComponent;

    private double[] m_dctCoefficients;

    private final ColorConvertOp m_colorConvert = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    /**
     * {@inheritDoc}
     */
    @Override
    public Result check(final PNGImageValue valueA, final PNGImageValue valueB) {
        initCoefficients();
        Image pngA = valueA.getImageContent().getImage();
        Image pngB = valueB.getImageContent().getImage();

        BitSet hashA = getHash(ImageUtil.getBufferedImage(pngA));
        BitSet hashB = getHash(ImageUtil.getBufferedImage(pngB));

        hashA.xor(hashB);
        int diff = hashA.cardinality();

        double relativeDiff = 100 * diff / (8.0 * 8.0);
        if (relativeDiff <= m_allowedDifference.getDoubleValue()) {
            return OK;
        } else {
            return new Result("image difference " + relativeDiff + "% is greater than "
                    + m_allowedDifference.getDoubleValue() + "%");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettings(settings);
        m_allowedDifference.loadSettingsFrom(settings);
        m_sampleSize.loadSettingsFrom(settings);
        m_dctSize.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        super.loadSettingsForDialog(settings);
        try {
            m_allowedDifference.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_allowedDifference.setDoubleValue(1);
        }
        try {
            m_sampleSize.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_sampleSize.setIntValue(8);
        }
        try {
            m_dctSize.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_dctSize.setIntValue(8);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        m_allowedDifference.saveSettingsTo(settings);
        m_sampleSize.saveSettingsTo(settings);
        m_dctSize.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_allowedDifference.validateSettings(settings);
        m_sampleSize.validateSettings(settings);
        m_dctSize.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends DialogComponent> getDialogComponents() {
        if (m_allowedDifferenceComponent == null) {
            m_allowedDifferenceComponent = new DialogComponentNumber(m_allowedDifference, "Allowed difference in %", 1);
        }
        if (m_sampleSizeComponent == null) {
            m_sampleSizeComponent = new DialogComponentNumber(m_sampleSize, "Sample size", 1);
        }
        if (m_dctSizeComponent == null) {
            m_dctSizeComponent = new DialogComponentNumber(m_dctSize, "DCT sample size", 1);
        }

        List<DialogComponent> l = new ArrayList<DialogComponent>(super.getDialogComponents());
        l.add(m_allowedDifferenceComponent);
        l.add(m_sampleSizeComponent);
        l.add(m_dctSizeComponent);
        return l;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    private BufferedImage grayscale(final BufferedImage img) {
        return m_colorConvert.filter(img, null);
    }

    private void initCoefficients() {
        m_dctCoefficients = new double[m_sampleSize.getIntValue()];
        m_dctCoefficients[0] = 1 / Math.sqrt(2.0);
        for (int i = 1; i < m_sampleSize.getIntValue(); i++) {
            m_dctCoefficients[i] = 1;
        }
    }

    private double[][] applyDCT(final double[][] f) {
        int n = m_sampleSize.getIntValue();

        double[][] result = new double[n][n];
        for (int u = 0; u < n; u++) {
            for (int v = 0; v < n; v++) {
                double sum = 0.0;
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        sum +=
                                Math.cos(((2 * i + 1) / (2.0 * n)) * u * Math.PI)
                                        * Math.cos(((2 * j + 1) / (2.0 * n)) * v * Math.PI) * (f[i][j]);
                    }
                }
                sum *= ((m_dctCoefficients[u] * m_dctCoefficients[v]) / 4.0);
                result[u][v] = sum;
            }
        }
        return result;
    }

    private BitSet getHash(BufferedImage img) {
        /* 1. Reduce size.
         * Like Average Hash, pHash starts with a small image.
         * However, the image is larger than 8x8; 32x32 is a good size.
         * This is really done to simplify the DCT computation and not
         * because it is needed to reduce the high frequencies.
         */
        img = ImageUtil.resize(img, m_sampleSize.getIntValue(), m_sampleSize.getIntValue());

        /* 2. Reduce color.
         * The image is reduced to a grayscale just to further simplify
         * the number of computations.
         */
        img = grayscale(img);

        double[][] vals = new double[m_sampleSize.getIntValue()][m_sampleSize.getIntValue()];

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                vals[x][y] = ImageUtil.getBlue(img, x, y);
            }
        }

        /* 3. Compute the DCT.
         * The DCT separates the image into a collection of frequencies
         * and scalars. While JPEG uses an 8x8 DCT, this algorithm uses
         * a 32x32 DCT.
         */
        double[][] dctVals = applyDCT(vals);

        /* 4. Reduce the DCT.
         * This is the magic step. While the DCT is 32x32, just keep the
         * top-left 8x8. Those represent the lowest frequencies in the
         * picture.
         */
        /* 5. Compute the average value.
         * Like the Average Hash, compute the mean DCT value (using only
         * the 8x8 DCT low-frequency values and excluding the first term
         * since the DC coefficient can be significantly different from
         * the other values and will throw off the average).
         */
        double total = 0;

        for (int x = 0; x < m_dctSize.getIntValue(); x++) {
            for (int y = 0; y < m_dctSize.getIntValue(); y++) {
                total += dctVals[x][y];
            }
        }
        total -= dctVals[0][0];

        double avg = total / ((m_dctSize.getIntValue() * m_dctSize.getIntValue()) - 1);

        /* 6. Further reduce the DCT.
         * This is the magic step. Set the 64 hash bits to 0 or 1
         * depending on whether each of the 64 DCT values is above or
         * below the average value. The result doesn't tell us the
         * actual low frequencies; it just tells us the very-rough
         * relative scale of the frequencies to the mean. The result
         * will not vary as long as the overall structure of the image
         * remains the same; this can survive gamma and color histogram
         * adjustments without a problem.
         */
        BitSet hash = new BitSet(m_dctSize.getIntValue() * m_dctSize.getIntValue());

        int index = 0;
        for (int x = 0; x < m_dctSize.getIntValue(); x++) {
            for (int y = 0; y < m_dctSize.getIntValue(); y++) {
                if (x != 0 && y != 0) {
                    hash.set(index++, dctVals[x][y] > avg);
                }
            }
        }

        return hash;
    }
}
