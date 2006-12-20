/* Created on Jun 9, 2006 4:57:25 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.preproc.sample;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class SamplingNodeSettings {
    /** NodeSettings key: Which method to use (relative or absolute). */
    private static final String CFG_METHOD = "method";

    /** NodeSettings key: Fraction to use (relative method). */
    private static final String CFG_FRACTION = "fraction";

    /** NodeSettings key: Count to use (absolute method). */
    private static final String CFG_COUNT = "count";

    /** NodeSettings key: If to choose randomly or from top of table. */
    private static final String CFG_RANDOM = "random";

    /** NodeSettings key: The random seed. */
    private static final String CFG_RANDOM_SEED = "random_seed";

    /**
     * Enum for the two methods for setting the number of rows in the output
     * table.
     */
    public enum Methods {
        /** Relative fraction. */
        Relative,
        /** Absolute number. */
        Absolute
    }

    /** The method to use, METHOD_RELATIVE or METHOD_ABSOLUTE. */
    private Methods m_method;

    /** Fraction to use (if relative sampling). */
    private double m_fraction;

    /** Count of samples to choose (if absolute sampling). */
    private int m_count;

    /** If to choose samples randomly, if false take from beginning. */
    private boolean m_random;

    /**
     * The seed to use for random initialization. Will be null if no
     * deterministic sampling is required as from the dialog.
     */
    private Long m_seed;

    /**
     * Saves the settings to the given object.
     * 
     * @param settings the node settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_method != null) {
            settings.addString(CFG_METHOD, m_method.toString());
            settings.addBoolean(CFG_RANDOM, m_random);
            settings.addDouble(CFG_FRACTION, m_fraction);
            settings.addInt(CFG_COUNT, m_count);
            // write null here if no deterministic behaviour required.
            settings.addString(CFG_RANDOM_SEED, m_seed != null ? Long
                    .toString(m_seed) : null);
        }
    }

    /**
     * Loads the setting from the given object.
     * 
     * @param settings the settings
     * @param guessValues If <code>true</code>, default values are used in
     *            case the settings are incomplete, <code>false</code> will
     *            throw an exception. <code>true</code> should be used when
     *            called from the dialog, <code>false</code> when called from
     *            the model.
     * @throws InvalidSettingsException if settings incomplete and
     *             <code>guessValues</code> is <code>false</code>
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final boolean guessValues) throws InvalidSettingsException {
        String seed;
        if (guessValues) {
            String method = settings.getString(CFG_METHOD, Methods.Absolute
                    .toString());
            if (method == null) {
                method = Methods.Absolute.toString();
            }
            try {
                m_method = Methods.valueOf(method);
            } catch (IllegalArgumentException iae) {
                m_method = Methods.Absolute;
            }
            m_random = settings.getBoolean(CFG_RANDOM, true);
            seed = settings.getString(CFG_RANDOM_SEED, null);
            m_fraction = settings.getDouble(CFG_FRACTION, 0.1);
            m_count = settings.getInt(CFG_COUNT, 100);
        } else {
            String method = settings.getString(CFG_METHOD);
            if (method == null) {
                throw new InvalidSettingsException("Method must not be null.");
            }
            try {
                m_method = Methods.valueOf(method);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid sampling method: "
                        + method);
            }
            m_random = settings.getBoolean(CFG_RANDOM);
            seed = settings.getString(CFG_RANDOM_SEED);
            m_fraction = settings.getDouble(CFG_FRACTION);
            m_count = settings.getInt(CFG_COUNT);
        }
        if (seed != null) {
            m_seed = Long.parseLong(seed);
        } else {
            m_seed = null;
        }
    }

    /**
     * Returns the absolute number of rows in the output table.
     * 
     * @return the absolute number of rows
     */
    public int count() {
        return m_count;
    }

    /**
     * Sets the absolute number of rows in the output table.
     * 
     * @param count the number of rows
     */
    public void count(final int count) {
        m_count = count;
    }

    /**
     * Returns the relative number of rows in the output table (in relation to
     * the number of input rows).
     * 
     * @return the relative number of rows (a value between 0 and 1)
     */
    public double fraction() {
        return m_fraction;
    }

    /**
     * Sets the relative number of rows in the output table (in relation to the
     * number of input rows).
     * 
     * @param fraction the relative number of rows, a value between 0 and 1
     */
    public void fraction(final double fraction) {
        m_fraction = fraction;
    }

    /**
     * Returns the method use for sampling the rows.
     * 
     * @return the sampling method
     */
    public Methods method() {
        return m_method;
    }

    /**
     * Sets the method use for sampling the rows.
     * 
     * @param method the sampling method
     */
    public void method(final Methods method) {
        m_method = method;
    }

    /**
     * Returns if the rows should be sampled randomly.
     * 
     * @return <code>true</code> if the rows should be sampled randomly,
     *         <code>false</code> otherwise
     */
    public boolean random() {
        return m_random;
    }

    /**
     * Sets if the rows should be sampled randomly.
     * 
     * @param random <code>true</code> if the rows should be sampled randomly,
     *            <code>false</code> otherwise
     */
    public void random(final boolean random) {
        m_random = random;
    }

    /**
     * Returns the optional random seed.
     * 
     * @return the random seed or <code>null</code> if none is specified
     */
    public Long seed() {
        return m_seed;
    }

    /**
     * Sets the seed for the random number generator.
     * 
     * @param seed a seed or <code>null</code> if none is set
     */
    public void seed(final Long seed) {
        m_seed = seed;
    }
}
