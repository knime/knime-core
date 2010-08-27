/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * -------------------------------------------------------------------
 * 
 * History
 *   23.10.2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * Represents a discretization scheme. Therefore, an ordered set of boundaries
 * is hold.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class DiscretizationScheme {

    private static final String CONFIG_KEY_INTERVAL_PREFIX = "Interval_";

    /**
     * The sorted vector of intervals.
     */
    private Vector<Interval> m_intervals = new Vector<Interval>();

    /**
     * Creates a discretization scheme from an initial interval.
     * 
     * @param initialInterval the initial interval
     */
    public DiscretizationScheme(final Interval initialInterval) {
        m_intervals.add(initialInterval);
    }

    /**
     * Creates a discretization scheme from a given one. Creates a deep copy
     * except the intervals which are imutable.
     * 
     * @param dScheme the discretization scheme to copy
     */
    public DiscretizationScheme(final DiscretizationScheme dScheme) {

        m_intervals.addAll(dScheme.m_intervals);
    }

    /**
     * Creates a <code>DiscretizationScheme</code> from a {@link Config}
     * object.
     * 
     * @param content the content object to restore the model from
     * @throws InvalidSettingsException thrown if the settings are invalid
     */
    public DiscretizationScheme(final Config content)
            throws InvalidSettingsException {

        m_intervals = new Vector<Interval>();
        int i = 0;
        Enumeration<TreeNode> schemeConfigEnum = content.children();
        while (schemeConfigEnum.hasMoreElements()) {

            Config schemeConfig = (Config)schemeConfigEnum.nextElement();
            m_intervals.add(new Interval(schemeConfig));

            i++;
        }
    }

    /**
     * Returns the index of the interval the given value is contained in.
     * 
     * @param value the value to find the interval index for
     * @return the index of the interval the value is contained in; -1 if not
     *         contained in any interval
     */
    public int getIntervalIndexOf(final double value) {

        boolean contained = false;
        int i = 0;
        for (; i < m_intervals.size(); i++) {

            if (m_intervals.get(i).contains(value)) {

                contained = true;
                break;
            }
        }

        if (contained) {
            return i;
        } else {
            return -1;
        }
    }

    /**
     * Determins the interval of the value and returns a discrete value for
     * that.
     * 
     * @param value the value to discretize
     * @return the discretized value
     */
    public String getDiscreteValue(final double value) {
        int index = getIntervalIndexOf(value);

        return CONFIG_KEY_INTERVAL_PREFIX + index;
    }

    /**
     * Inserts a new bound into an interval. This results in one additional
     * interval. The new bound must be located within an interval.
     * 
     * @param newBound the new bound to insert
     */
    public void insertBound(final double newBound) {
        // search the interval the bound is located in
        int i = 0;
        for (; i < m_intervals.size(); i++) {
            Interval interval = m_intervals.get(i);

            if (interval.contains(newBound)) {
                break;
            }
        }

        // if the index i is bigger than the biggest possible index
        // there is now inteval containing the bound
        // thus return
        if (i >= m_intervals.size()) {
            return;
        }

        // if the new bound is located on the border of the interval do nothing
        if (m_intervals.get(i).getLeftBound() == newBound
                && m_intervals.get(i).isIncludeLeft()
                || m_intervals.get(i).getRightBound() == newBound
                && m_intervals.get(i).isIncludeRight()) {

            return;
        }

        // create two intervals from the one the new bound is contained in
        // the left interval includes the new bound
        Interval newLeftInterval =
                new Interval(m_intervals.get(i).getLeftBound(), newBound,
                        m_intervals.get(i).isIncludeLeft(), true);
        Interval newRightInterval =
                new Interval(newBound, m_intervals.get(i).getRightBound(),
                        false, m_intervals.get(i).isIncludeRight());

        // remove the old interval
        m_intervals.remove(i);
        m_intervals.add(i, newRightInterval);
        m_intervals.add(i, newLeftInterval);
    }

    /**
     * @return the number of intervals of this discretization scheme
     */
    public int getNumIntervals() {
        return m_intervals.size();
    }

    // /**
    // * TODO: STILL BUGGY !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // Adds
    // * an interval to the given discretization scheme. If the interval
    // conflicts
    // * with the current intervals they are adapted accordingly.
    // *
    // * @param interval the new interval to add
    // */
    // public void addInterval(final Interval interval) {
    //
    // throw new RuntimeException("Buggy at the moment. Can not be used.");
    //
    // // if (m_intervals.size() == 0) {
    // // m_intervals.add(interval);
    // // return;
    // // }
    // //
    // // // at the beginning the insertion before index is 0
    // // int indexOfLeftInterval = 0;
    // //
    // // // first find the interval where the left bound of the interval to
    // // // insert is located
    // // for (int i = 0; i < m_intervals.size(); i++) {
    // //
    // // Interval currInterval = m_intervals.get(i);
    // //
    // // if (interval.compareLeftBoundToLeft(currInterval) <= 0) {
    // // // if the interval to insert starts before the current one
    // // // or is equal break
    // // break;
    // // } else if (interval.compareLeftBoundToRight(currInterval) <= 0) {
    // // // if the interval starts within the current interval
    // // // break
    // // break;
    // // }
    // //
    // // // else increment the interval counter and proceed with the next
    // // // interval
    // // indexOfLeftInterval++;
    // // }
    // //
    // // int indexOfRightInterval = indexOfLeftInterval;
    // //
    // // // now find the interval where the right bound of the interval
    // // // to insert is falling in
    // // for (int i = indexOfLeftInterval; i < m_intervals.size(); i++) {
    // //
    // // Interval currInterval = m_intervals.get(i);
    // //
    // // if (interval.compareRightBoundToLeft(currInterval) <= 0) {
    // // // if the interval to insert starts before the current one
    // // // or is equal break
    // // break;
    // // } else if (interval.compareRightBoundToRight(currInterval) <= 0) {
    // // // if the interval starts within the current interval
    // // // break
    // // break;
    // // }
    // //
    // // // else increment the interval counter and proceed with the next
    // // // interval
    // // indexOfRightInterval++;
    // // }
    // //
    // // Interval leftShrinkedInterval = null;
    // // Interval rightShrinkedInterval = null;
    // // if (indexOfLeftInterval < m_intervals.size()) {
    // // if (interval.compareLeftBoundToLeft(m_intervals
    // // .get(indexOfLeftInterval)) <= 0) {
    // // // if the interval is before or equal to the indexed one
    // // // do nothing with the left bound of the indexed interval
    // // } else {
    // // // shrink the indexed interval to the left bound of the interval
    // // // to insert
    // //
    // // double leftBound =
    // // m_intervals.get(indexOfLeftInterval).getLeftBound();
    // // double rightBound = interval.getLeftBound();
    // // boolean includeLeft =
    // // m_intervals.get(indexOfLeftInterval).isIncludeLeft();
    // // boolean includeRight = !interval.isIncludeLeft();
    // //
    // // leftShrinkedInterval =
    // // new Interval(leftBound, rightBound, includeLeft,
    // // includeRight);
    // // }
    // //
    // // if (indexOfRightInterval < m_intervals.size()) {
    // //
    // // if (interval.compareRightBoundToLeft(m_intervals
    // // .get(indexOfRightInterval)) < 0) {
    // // // if the interval ends before the indexed one
    // // // do nothing with the left bound of the indexed interval
    // // } else {
    // // // shrink the indexed interval to the right bound of the
    // // // interval
    // // // to insert
    // //
    // // double leftBound = interval.getRightBound();
    // // double rightBound =
    // // m_intervals.get(indexOfRightInterval)
    // // .getRightBound();
    // // boolean includeLeft = !interval.isIncludeRight();
    // //
    // // boolean includeRight =
    // // m_intervals.get(indexOfRightInterval)
    // // .isIncludeRight();
    // //
    // // rightShrinkedInterval =
    // // new Interval(leftBound, rightBound, includeLeft,
    // // includeRight);
    // // }
    // // }
    // // }
    // //
    // // // change the intervals if they were shrinked
    // // // and remove all intermediate intervals covered by the interval
    // // // to insert
    // // for (int i = indexOfRightInterval; i >= 0; i--) {
    // //
    // // if (i == indexOfLeftInterval || i == indexOfRightInterval) {
    // // if (i == indexOfRightInterval) {
    // // if (rightShrinkedInterval != null) {
    // // m_intervals.remove(i);
    // // m_intervals.add(i, rightShrinkedInterval);
    // // }
    // // }
    // // if (i == indexOfLeftInterval) {
    // // if (leftShrinkedInterval != null) {
    // // if (i < m_intervals.size()
    // // && rightShrinkedInterval == null) {
    // // m_intervals.remove(i);
    // // }
    // // m_intervals.add(i, leftShrinkedInterval);
    // // m_intervals.add(i + 1, interval);
    // //
    // // // finished
    // // break;
    // // } else {
    // // // remove the left one
    // // if (i < m_intervals.size()) {
    // // m_intervals.remove(i);
    // // }
    // // m_intervals.add(i, interval);
    // //
    // // // finished
    // // break;
    // // }
    // // }
    // // } else {
    // // // remove all intermediate intervals
    // // if (i < m_intervals.size()) {
    // // m_intervals.remove(i);
    // // }
    // // }
    // // }
    // }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Interval interval : m_intervals) {
            sb.append(interval.toString());
        }

        return sb.toString();
    }

    /**
     * Saves this scheme to a {@link org.knime.core.node.ModelContentWO} object.
     * 
     * @param modelContent the {@link Config} object to store the
     *            {@link DiscretizationScheme} to
     */
    public void saveToModelContent(final Config modelContent) {

        int i = 0;
        for (Interval interval : m_intervals) {
            Config intervalConfig =
                    modelContent.addConfig(CONFIG_KEY_INTERVAL_PREFIX + i);
            interval.saveToModelContent(intervalConfig);
            i++;
        }
    }

    /**
     * Returns the bounds of the intervals. It is assumed that the bounderies
     * are touching each other.
     * 
     * @return the boundary values
     */
    public double[] getBounds() {
        double[] result = new double[m_intervals.size() + 1];

        // insert the first bound
        result[0] = m_intervals.get(0).getLeftBound();

        // now add all right bounds
        int counter = 1;
        for (Interval interval : m_intervals) {
            result[counter] = interval.getRightBound();
            counter++;
        }

        return result;
    }
}
