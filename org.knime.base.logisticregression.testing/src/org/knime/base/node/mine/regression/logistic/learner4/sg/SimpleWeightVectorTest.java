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
 *   20.04.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for the SimpleWeightVector.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class SimpleWeightVectorTest extends AbstractWeightVectorTest {

    @Override
    protected SimpleWeightMatrix<MockClassificationTrainingRow> createTestVec(
        final boolean fitIntercept, final int nRows, final int nCols) {
        SimpleWeightMatrix<MockClassificationTrainingRow> vec = new SimpleWeightMatrix<>(nCols, nRows, fitIntercept);
        double[][] data = vec.m_data;
        assertEquals(data.length, nRows);
        for (int i = 0; i < data.length; i++) {
            assertEquals(nCols, data[i].length);
        }
        return vec;
    }

    @Override
    @Test
    public void testScale() throws Exception {
        SimpleWeightMatrix<MockClassificationTrainingRow> vec = createTestVec(false, 3, 3);
        double[][] data = vec.m_data;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = 1.0;
            }
        }

        vec.scale(3.0);
        assertEquals(vec.getScale(), 1.0, EPSILON);
        double[][] beta = vec.getWeightVector();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (j == 0) {
                    assertEquals(data[i][j], 1.0, EPSILON);
                    assertEquals(beta[i][j], 1.0, EPSILON);
                } else {
                    assertEquals(data[i][j], 3.0, EPSILON);
                    assertEquals(beta[i][j], 3.0, EPSILON);
                }
            }
        }
    }

}