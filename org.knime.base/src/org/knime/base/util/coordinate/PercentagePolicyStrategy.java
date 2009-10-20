/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   Aug 12, 2008 (sellien): created
 */
package org.knime.base.util.coordinate;

/**
 * Class for percentage tick policy.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public class PercentagePolicyStrategy extends
        AscendingNumericTickPolicyStrategy {

    /**
     * ID for a percentage tick policy.
     */
    @SuppressWarnings("hiding")
    public static final String ID = "Percentage";

    /**
     * Constructor.
     */
    public PercentagePolicyStrategy() {
        super(ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoordinateMapping[] getTickPositions(final double absoluteLength,
            final double minDomainValue, final double maxDomainValue,
            final double tickDistance) {
        int nrTicks = (int)(absoluteLength / tickDistance);
        if (nrTicks > 100) {
            nrTicks = 100; // 1 % steps
        } else if (nrTicks > 40) {
            nrTicks = 40; // 2.5 %
        } else if (nrTicks > 20) {
            nrTicks = 20; // 5 %
        } else if (nrTicks > 10) {
            nrTicks = 10; // 10 %
        } else if (nrTicks > 4) {
            nrTicks = 4; // 25%
        } else if (nrTicks > 2) {
            nrTicks = 2; // 50%
        } else {
            nrTicks = 1; // 100% step
        }
        CoordinateMapping[] coordMap = new CoordinateMapping[nrTicks + 1];
        double step = 100.0 / nrTicks;
        double dist = absoluteLength / nrTicks;
        for (int i = 0; i <= nrTicks; i++) {
            coordMap[i] =
                    new DoubleCoordinateMapping((step * i) + " %", step * i,
                            dist * i);
        }
        return coordMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoordinateMapping[] getTickPositions(final int absoluteLength,
            final int minDomainValue, final int maxDomainValue,
            final int tickDistance) {
        return getTickPositions((double)absoluteLength, (double)minDomainValue,
                (double)maxDomainValue, (double)tickDistance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMappingAllowed() {
        return false;
    }

}
