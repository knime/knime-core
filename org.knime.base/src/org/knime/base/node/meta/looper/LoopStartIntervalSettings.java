/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   24.02.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the loop interval start node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopStartIntervalSettings {
    private double m_from = 0;

    private double m_to = 1;

    private double m_step = 0.01;

    /**
     * Returns the interval's start value.
     *
     * @return the interval's start value
     */
    public double from() {
        return m_from;
    }

    /**
     * Sets the interval's start value.
     *
     * @param f the interval's start value
     */
    public void from(final double f) {
        m_from = f;
    }

    /**
     * Returns the interval's end value.
     *
     * @return the interval's end value
     */
    public double to() {
        return m_to;
    }

    /**
     * Sets the interval's end value.
     *
     * @param t the interval's end value
     */
    public void to(final double t) {
        m_to = t;
    }

    /**
     * Returns the loop's step size.
     *
     * @return the interval's end value
     */
    public double step() {
        return m_step;
    }

    /**
     * Sets the loop's step size.
     *
     * @param s the interval's end value
     */
    public void step(final double s) {
        m_step = s;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) {
        m_from = settings.getDouble("from", 0);
        m_to = settings.getDouble("to", 1);
        m_step = settings.getDouble("step", 0.01);
    }

    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addDouble("from", m_from);
        settings.addDouble("to", m_to);
        settings.addDouble("step", m_step);
    }
}
