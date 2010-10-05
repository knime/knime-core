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
 * ---------------------------------------------------------------------
 *
 * History
 *   24.02.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import org.knime.core.node.InvalidSettingsException;
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

    private boolean m_integerLoop;

    /**
     * Returns if the loop should iterate over integer and not doubles.
     *
     * @return <code>true</code> if the loop is over integers
     */
    public boolean integerLoop() {
        return m_integerLoop;
    }

    /**
     * Sets if the loop should iterate over integer and not doubles.
     *
     * @param il <code>true</code> if the loop is over integers
     */
    public void integerLoop(final boolean il) {
        m_integerLoop = il;
    }

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
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_from = settings.getDouble("from", 0);
        m_to = settings.getDouble("to", 1);
        m_step = settings.getDouble("step", 0.01);
        m_integerLoop = settings.getBoolean("integerLoop", false);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_from = settings.getDouble("from");
        m_to = settings.getDouble("to");
        m_step = settings.getDouble("step");
        m_integerLoop = settings.getBoolean("integerLoop");
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
        settings.addBoolean("integerLoop", m_integerLoop);
    }
}
