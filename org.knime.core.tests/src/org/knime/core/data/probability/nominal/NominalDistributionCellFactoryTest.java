/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Nov 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability.nominal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.knime.core.data.filestore.FileStoreFactory;

/**
 * Unit tests for {@link NominalDistributionCellFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class NominalDistributionCellFactoryTest {

    private static final String[] VALUES = new String[]{"A", "B", "C"};

    private static final NominalDistributionCellFactory TEST_INSTANCE =
        new NominalDistributionCellFactory(FileStoreFactory.createFileStoreFactory(null), VALUES);

    private static double[] d(final double... ds) {
        return ds;
    }

    /**
     * Verifies that the constructor fails if exec is null.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorFailsOnNullExec() {
        @SuppressWarnings("unused") // the constructor fails anyway
        final NominalDistributionCellFactory factory = new NominalDistributionCellFactory(null, VALUES);
    }

    /**
     * Tests {@link NominalDistributionCellFactory#createCell(double[], double)} including the epsilon functionality.
     */
    @Test
    public void testCreateCellFromDoubleArray() {
        testCreateCellFromDoubleArray(d(0.1, 0.2, 0.7), 1e-6);
        testCreateCellFromDoubleArray(d(0.100000001, 0.2, 0.7), 0.0001);
    }

    private static void testCreateCellFromDoubleArray(final double[] p, final double epsilon) {
        NominalDistributionCell c = TEST_INSTANCE.createCell(p, epsilon);
        for (int i = 0; i < VALUES.length; i++) {
            assertEquals(p[i], c.getProbability(VALUES[i]), epsilon);
        }
    }

    /**
     * Verifies that {@link NominalDistributionCellFactory#createCell(double[], double)} fails if probabilities is null.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateCellFromDoubleArrayFailsOnNullProbabilities() {
        TEST_INSTANCE.createCell(null);
    }

    /**
     * Verifies that {@link NominalDistributionCellFactory#createCell(double[], double)} fails if the wrong number of
     * probabilities is provided.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateCellFromDoubleArrayFailsOnWrongNumberOfProbabilities() {
        TEST_INSTANCE.createCell(d(0.5, 0.5), 0);
    }

    /**
     * Verifies that {@link NominalDistributionCellFactory#createCell(double[], double)} fails if epsilon is negative.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateCellFromDoubleArrayFailsOnNegativeEpsilon() {
        TEST_INSTANCE.createCell(d(0.1, 0.2, 0.7), -1);
    }

    /**
     * Verifies that {@link NominalDistributionCellFactory#createCell(double[], double)} fails if any probability is
     * negative.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateCellFromDoubleArrayFailsOnNegativeProbabilities() {
        TEST_INSTANCE.createCell(d(0.2, 0.9, -0.1), 0);
    }

    /**
     * Verifies {@link NominalDistributionCellFactory#createCell(double[], double)} fails if the probabilities don't sum
     * up to one.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateCellFromDoubleArrayFailsOnProbabilitiesNotSummingUpToOne() {
        TEST_INSTANCE.createCell(d(0.1, 0.2, 0.3), 0);
    }

    /**
     * Tests the {@link NominalDistributionCellFactory#createCell(String)} method.
     */
    @Test
    public void testCreateCellFromString() {
        for (String value : VALUES) {
            final NominalDistributionCell cell = TEST_INSTANCE.createCell(value);
            final double[] expectedProbabilities = createProbsFor(value);
            for (int i = 0; i < VALUES.length; i++) {
                assertEquals(expectedProbabilities[i], cell.getProbability(VALUES[i]), 0);
            }
        }
    }

    private static double[] createProbsFor(final String value) {
        final double[] probs = new double[VALUES.length];
        for (int i = 0; i < VALUES.length; i++) {
            if (value.equals(VALUES[i])) {
                probs[i] = 1;
                break;
            }
        }
        return probs;
    }

    /**
     * Verifies that {@link NominalDistributionCellFactory#createCell(String)} fails on unknown values.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateCellFromStringFailsOnUnknownValue() {
        TEST_INSTANCE.createCell("Z");
    }

    /**
     * Verifies that {@link NominalDistributionCellFactory#createCell(String)} fails if the value is null.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateCellFromStringFailsOnNullValue() {
        TEST_INSTANCE.createCell(null);
    }

}
