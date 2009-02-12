/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
