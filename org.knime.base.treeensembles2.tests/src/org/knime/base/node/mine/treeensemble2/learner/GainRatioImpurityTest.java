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
 *   15.04.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Contains unit tests for the class {@link GainRatioImpurity}. <br>
 * This class implements only the method {@link GainRatioImpurity#getGain(double, double, double[], double)}.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class GainRatioImpurityTest {

    final static double TOLERANCE = 1e-1;

    /**
     * Tests the method {@link GainRatioImpurity#getGain(double, double, double[], double)}.
     *
     * @throws Exception
     */
    @Test
    public void testGetGain() throws Exception {
        final GainRatioImpurity gainRatioImpurity = GainRatioImpurity.INSTANCE;
        double priorImpurity = 0.5;
        double postSplitImpurity = 0.48;
        double[] partitionWeights = new double[]{6, 4};
        double totalWeight = 10;

        assertEquals("Gain was incorrect.", 0.02,
            gainRatioImpurity.getGain(priorImpurity, postSplitImpurity, partitionWeights, totalWeight), TOLERANCE);

        postSplitImpurity = 0.0;
        assertEquals("Gain was incorrect.", 0.5,
            gainRatioImpurity.getGain(priorImpurity, postSplitImpurity, partitionWeights, totalWeight), TOLERANCE);

        priorImpurity = 0.37;
        postSplitImpurity = 0.33;
        assertEquals("Gain was incorrect.", 0.04,
            gainRatioImpurity.getGain(priorImpurity, postSplitImpurity, partitionWeights, totalWeight), TOLERANCE);

        partitionWeights = new double[]{6, 4, 3, 7};
        totalWeight = 20;
        assertEquals("Gain was incorrect.", 0.04 / 1.9261207468426806,
            gainRatioImpurity.getGain(priorImpurity, postSplitImpurity, partitionWeights, totalWeight), TOLERANCE);
    }
}
