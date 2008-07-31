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
 * Interface that is used be FuzzyRuleBasisFunction's to shrink membership
 * functions by various measurements. The shrink action can be differed between
 * left and right region referring to the anchor (initial) value, and if the
 * shrink is made in the support or core region. Note, support region here means
 * only the "ramp" of the membership function.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface Shrink {
    /** Selected shrink procedure. */
    public static final String SHRINK_KEY = "shrink";

    /** Number of possible shrink procedure. */
    public static final Shrink[] SHRINKS = new Shrink[]{
            VolumeBorderBasedShrink.getInstance(),
            VolumeAnchorBasedShrink.getInstance(),
            VolumeRuleBasedShrink.getInstance()};

    /**
     * leftSuppLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return calculated fuzzy membership loss on the anchor left side in the
     *         support region
     */
    double leftSuppLoss(double value, MembershipFunction mem);

    /**
     * leftCoreLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return calculated fuzzy membership loss on the anchor left side in the
     *         core region
     */
    double leftCoreLoss(double value, MembershipFunction mem);

    /**
     * rightSuppLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return calculated fuzzy membership loss on the anchor right side in the
     *         support region
     */
    double rightSuppLoss(double value, MembershipFunction mem);

    /**
     * rightCoreLoss(.).
     * 
     * @param value current value
     * @param mem holds parameter of fuzzy trapezoid membership function
     * @return calculated fuzzy membership loss on the anchor right side in the
     *         core region
     */
    double rightCoreLoss(double value, MembershipFunction mem);
}
