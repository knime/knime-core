/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy.shrink;

import org.knime.base.node.mine.bfn.fuzzy.membership.MembershipFunction;

/**
 * ShrinkDistanceBased. calculates the volume loss from the left or right
 * support or core region border to the new value, divided by the support or
 * core spread <code>d1_i*Prod(i!=k)d2_k/Prod(k)d2_k = d1_i/d2_i</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class VolumeBorderBasedShrink implements Shrink {
    
    private static final Shrink SHRINK = new VolumeBorderBasedShrink();

    /*
     * Empty default constructor.
     */
    private VolumeBorderBasedShrink() {
        // empty
    }

    /**
     * @return a new instance of the ShrinkVolumeRuleBased object
     */
    public static Shrink getInstance() {
        // creates a new instance and returns it
        return SHRINK;
    }

    /**
     * leftSuppLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy membership function
     * @return loss in distance assessed to the left dimension of the support
     */
    public double leftSuppLoss(final double value,
            final MembershipFunction mem) {

        // value on the border
        if (mem.getMinSupport() >= value) {
            return Double.MIN_VALUE;
        }
        return (value - mem.getMinSupport())
                / (mem.getMinCore() - mem.getMinSupport());
    }

    /**
     * leftCoreLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy membership function
     * @return loss in distance assessed to the core region
     */
    public double leftCoreLoss(final double value, 
            final MembershipFunction mem) {

        // value on the border
        if (mem.getMinCore() >= value) {
            return Double.MIN_VALUE;
        }
        return ((value - mem.getMinCore()) / mem.getCore());
    }

    /**
     * rightSuppLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy membership function
     * @return loss in distance assessed to the right dimension of the support
     */
    public double rightSuppLoss(final double value, 
            final MembershipFunction mem) {
        // value on the border
        if (mem.getMaxSupport() <= value) {
            return Double.MIN_VALUE;
        }
        return (mem.getMaxSupport() - value)
                / (mem.getMaxSupport() - mem.getMaxCore());
    }

    /**
     * rightCoreLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return loss in distance assessed to the core region
     */
    public double rightCoreLoss(final double value, 
            final MembershipFunction mem) {

        // value on the border
        if (mem.getMaxCore() <= value) {
            return Double.MIN_VALUE;
        }
        return ((mem.getMaxCore() - value) / mem.getCore());
    }

    /**
     * Returns the string representation <b>VolumeBorderBased</b> for this
     * shrink function.
     * 
     * @return a name for this shrink
     */
    @Override
    public final String toString() {
        return "VolumeBorderBased";
    }
}
