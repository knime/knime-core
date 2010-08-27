/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
