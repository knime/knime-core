/*
 * ------------------------------------------------------------------------
 *
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
 *   27.04.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.knime.base.node.mine.regression.logistic.learner4.TrainingRow;

/**
 * Unit tests for EagerSgdUpdater
 *
 * @author Adrian Nembach, KNIME.com
 */
public class EagerSgdUpdaterTest {

    @Test
    public void testUpdate() throws Exception {
        EagerSgdUpdater<TrainingRow> updater = new EagerSgdUpdater.EagerSgdUpdaterFactory<>().create();
        MockClassificationTrainingRow[] mockRows = new MockClassificationTrainingRow[]{
            new MockClassificationTrainingRow(new double[]{1, 1}, 0, 0),
            new MockClassificationTrainingRow(new double[]{2, 3}, 1, 1),
            new MockClassificationTrainingRow(new double[]{4, 5}, 2, 0),
        };
        SimpleWeightVector<TrainingRow> beta =
                new SimpleWeightVector<TrainingRow>(3, 3, true);
        double[] gradient = new double[]{3, -2};

        updater.update(mockRows[0], gradient, beta, 1.0, 0);
        double[][] expectedBeta = new double[][]{
            {-3.0, -3.0, -3.0},
            {2.0, 2.0, 2.0}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());

        gradient = new double[]{1, 2};
        updater.update(mockRows[1], gradient, beta, 2.0, 1);
        expectedBeta = new double[][]{
            {-5.0, -7.0, -9.0},
            {-2.0, -6.0, -10.0}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());

        gradient = new double[]{0, 2};
        updater.update(mockRows[0], gradient, beta, 2.0, 2);
        expectedBeta = new double[][]{
            {-5.0, -7.0, -9.0},
            {-6.0, -10.0, -14.0}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());

        gradient = new double[]{-3.0, -1.0};
        updater.update(mockRows[2], gradient, beta, 3.0, 3);
        expectedBeta = new double[][]{
            {4.0, 29.0, 36.0},
            {-3.0, 2.0, 1.0}
        };
        assertArrayEquals(expectedBeta, beta.getWeightVector());
    }
}
