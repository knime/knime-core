/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ShrinkVolumeAnchorBased. calculates the volume loss from the left or right
 * support or core region border to the new value, divided by the anchor to the
 * border spread <code>d1_i*Prod(i!=k)d2_k/Prod(k)d2_k = d1_i/d2_i</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class VolumeAnchorBasedShrink implements Shrink {
    
    private static final Shrink SHRINK = new VolumeAnchorBasedShrink();

    /**
     * Empty default constructor.
     */
    private VolumeAnchorBasedShrink() {
        // empty
    }

    /**
     * @return a new instance of the ShrinkVolumeRuleBased object
     */
    public static Shrink getInstance() {
        return SHRINK;
    }

    /**
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return loss normalized loss in volume in the left support region
     */
    public double leftSuppLoss(final double value, 
            final MembershipFunction mem) {

        // value on the border
        if (mem.getMinSupport() >= value) {
            return Double.MIN_VALUE;
        }
        // normalized volume loss
        return (value - mem.getMinSupport())
                / (mem.getAnchor() - mem.getMinSupport());
    }

    /**
     * leftCoreLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return loss normalized loss in volume in the left core region
     */
    public double leftCoreLoss(final double value, 
            final MembershipFunction mem) {

        // value on the border
        if (mem.getMinCore() >= value) {
            return Double.MIN_VALUE;
        }
        // normalized volume loss
        return (value - mem.getMinCore())
                / (mem.getAnchor() - mem.getMinCore());
    }

    /**
     * rightSuppLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return loss normalized loss in volume in the right support region
     */
    public double rightSuppLoss(final double value, 
            final MembershipFunction mem) {

        // value on the border
        if (mem.getMaxSupport() <= value) {
            return Double.MIN_VALUE;
        }
        // normalized volume loss
        return (mem.getMaxSupport() - value)
                / (mem.getMaxSupport() - mem.getAnchor());
    }

    /**
     * rightCoreLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return loss normalized loss in volume in the right core region
     */
    public double rightCoreLoss(final double value, 
            final MembershipFunction mem) {

        // value on the border
        if (mem.getMaxCore() <= value) {
            return Double.MIN_VALUE;
        }
        // normalized volume loss
        return (mem.getMaxCore() - value)
                / (mem.getMaxCore() - mem.getAnchor());
    }

    /**
     * Returns the string representation <b>VolumeAnchorBased</b> for this
     * shrink function.
     * 
     * @return a name for this shrink
     */
    @Override
    public final String toString() {
        return "VolumeAnchorBased";
    }
}
