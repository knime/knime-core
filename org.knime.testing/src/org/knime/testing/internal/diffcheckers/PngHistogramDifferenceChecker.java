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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
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
 * Image checker that computes color histograms for two images and compares the normalized histograms.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class PngHistogramDifferenceChecker extends AbstractDifferenceChecker<PNGImageValue> {
    /**
     * Factory for {@link PngHistogramDifferenceChecker}.
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
            return new PngHistogramDifferenceChecker();
        }
    }

    private static final String DESCRIPTION = "PNG images (histogram)";

    private final SettingsModelDoubleBounded m_allowedDifference = new SettingsModelDoubleBounded("allowedDifference",
            5, 0, 100);

    private DialogComponentNumber m_allowedDifferenceDialogComponent;

    private final SettingsModelIntegerBounded m_numberOfBins = new SettingsModelIntegerBounded("numberOfBins", 5, 1,
            255);

    private DialogComponentNumber m_binsDialogComponent;

    /**
     * {@inheritDoc}
     */
    @Override
    public Result check(final PNGImageValue expected, final PNGImageValue got) {
        BufferedImage expectedImage = ImageUtil.getBufferedImage(expected.getImageContent().getImage());
        BufferedImage actualImage = ImageUtil.getBufferedImage(got.getImageContent().getImage());

        double[][] referenceHisto = computeHistograms(expectedImage);
        double[][] testHisto = computeHistograms(actualImage);

        double diff = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < m_numberOfBins.getIntValue(); j++) {
                diff += Math.abs(referenceHisto[i][j] - testHisto[i][j]);
            }
        }

        double relativeDiff = 100 * diff / (3 * m_numberOfBins.getIntValue());
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
        m_numberOfBins.loadSettingsFrom(settings);
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
            m_numberOfBins.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_numberOfBins.setIntValue(8);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        m_allowedDifference.saveSettingsTo(settings);
        m_numberOfBins.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_allowedDifference.validateSettings(settings);
        m_numberOfBins.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends DialogComponent> getDialogComponents() {
        if (m_allowedDifferenceDialogComponent == null) {
            m_allowedDifferenceDialogComponent =
                    new DialogComponentNumber(m_allowedDifference, "Allowed difference in %", 1);
        }
        if (m_binsDialogComponent == null) {
            m_binsDialogComponent = new DialogComponentNumber(m_numberOfBins, "Bins per color", 1);
        }

        List<DialogComponent> l = new ArrayList<DialogComponent>(super.getDialogComponents());
        l.add(m_allowedDifferenceDialogComponent);
        l.add(m_binsDialogComponent);
        return l;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    private double[][] computeHistograms(final BufferedImage img) {
        double[][] histo = new double[3][m_numberOfBins.getIntValue()];

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int redBin = (int)Math.floor(ImageUtil.getRed(img, x, y) / 256.0 * m_numberOfBins.getIntValue());
                int greenBin = (int)Math.floor(ImageUtil.getGreen(img, x, y) / 256.0 * m_numberOfBins.getIntValue());
                int blueBin = (int)Math.floor(ImageUtil.getBlue(img, x, y) / 256.0 * m_numberOfBins.getIntValue());

                histo[0][redBin]++;
                histo[1][greenBin]++;
                histo[2][blueBin]++;
            }
        }

        final int totalPixels = img.getWidth() * img.getHeight();
        for (int j = 0; j < m_numberOfBins.getIntValue(); j++) {
            for (int i = 0; i < 3; i++) {
                histo[i][j] /= totalPixels;
            }
        }

        return histo;
    }
}
