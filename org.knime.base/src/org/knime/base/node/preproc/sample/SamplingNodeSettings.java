/* Created on Jun 9, 2006 4:57:25 PM by thor
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
 */
package org.knime.base.node.preproc.sample;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the sampling and the partioning node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class SamplingNodeSettings {
    /** NodeSettings key: Which method to use (relative or absolute). */
    private static final String CFG_COUNT_METHOD = "method";

    private static final String CFG_SAMPLING_METHOD = "samplingMethod";

    /** NodeSettings key: Fraction to use (relative method). */
    private static final String CFG_FRACTION = "fraction";

    /** NodeSettings key: Count to use (absolute method). */
    private static final String CFG_COUNT = "count";

    /** NodeSettings key: If to choose randomly or from top of table. */
    private static final String CFG_RANDOM = "random";

    /** NodeSettings key: The random seed. */
    private static final String CFG_RANDOM_SEED = "random_seed";

    private static final String CFG_STRATIFIED = "stratified_sampling";

    private static final String CFG_CLASS_COLUMN = "class_column";

    /**
     * Enum for the two methods for setting the number of rows in the output
     * table.
     */
    public enum CountMethods {
        /** Relative fraction. */
        Relative,
        /** Absolute number. */
        Absolute
    }

    /**
     * Enum for the four different sampling methods.
     */
    public enum SamplingMethods {
        /** Selects the first <em>x</em> rows. */
        First,
        /** Selects rows randomly. */
        Random,
        /** Select rows randomly but maintain the class distribution. */
        Stratified,
        /** Select the rows linearly over the whole table. */
        Linear
    }

    /** The method to use, METHOD_RELATIVE or METHOD_ABSOLUTE. */
    private CountMethods m_countMethod;

    /** Fraction to use (if relative sampling). */
    private double m_fraction;

    /** Count of samples to choose (if absolute sampling). */
    private int m_count;

    /**
     * The seed to use for random initialization. Will be null if no
     * deterministic sampling is required as from the dialog.
     */
    private Long m_seed;

    private SamplingMethods m_samplingMethod;

    private String m_classColumnName;

    /**
     * Saves the settings to the given object.
     *
     * @param settings the node settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_countMethod != null) {
            settings.addString(CFG_COUNT_METHOD, m_countMethod.toString());
            settings
                    .addString(CFG_SAMPLING_METHOD, m_samplingMethod.toString());
            settings.addDouble(CFG_FRACTION, m_fraction);
            settings.addInt(CFG_COUNT, m_count);
            // write null here if no deterministic behaviour required.
            settings.addString(CFG_RANDOM_SEED, m_seed != null ? Long
                    .toString(m_seed) : null);
            settings.addString(CFG_CLASS_COLUMN, m_classColumnName);
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
            String method =
                    settings.getString(CFG_COUNT_METHOD, CountMethods.Absolute
                            .toString());
            if (method == null) {
                method = CountMethods.Absolute.toString();
            }
            try {
                m_countMethod = CountMethods.valueOf(method);
            } catch (IllegalArgumentException iae) {
                m_countMethod = CountMethods.Absolute;
            }

            String samplingMethod =
                    settings.getString(CFG_SAMPLING_METHOD,
                            SamplingMethods.Random.toString());
            if (samplingMethod == null) {
                samplingMethod = SamplingMethods.Random.toString();
            }

            try {
                m_samplingMethod = SamplingMethods.valueOf(samplingMethod);
            } catch (IllegalArgumentException iae) {
                m_samplingMethod = SamplingMethods.Random;
            }

            seed = settings.getString(CFG_RANDOM_SEED, null);
            m_fraction = settings.getDouble(CFG_FRACTION, 0.1);
            m_count = settings.getInt(CFG_COUNT, 100);
        } else {
            String method = settings.getString(CFG_COUNT_METHOD);
            if (method == null) {
                throw new InvalidSettingsException("Method must not be null.");
            }
            try {
                m_countMethod = CountMethods.valueOf(method);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid sampling method: "
                        + method);
            }

            String samplingMethod =
                    settings.getString(CFG_SAMPLING_METHOD, null);
            if (samplingMethod == null) {
                try {
                    boolean random = settings.getBoolean(CFG_RANDOM);
                    boolean stratified =
                            settings.getBoolean(CFG_STRATIFIED, false);

                    if (stratified) {
                        m_samplingMethod = SamplingMethods.Stratified;
                    } else if (random) {
                        m_samplingMethod = SamplingMethods.Random;
                    } else {
                        m_samplingMethod = SamplingMethods.First;
                    }
                } catch (InvalidSettingsException ex) {
                    throw new InvalidSettingsException(
                            "No valid sampling method selected");
                }
            } else {
                try {
                    m_samplingMethod = SamplingMethods.valueOf(samplingMethod);
                } catch (IllegalArgumentException iae) {
                    throw new InvalidSettingsException(iae);
                }
            }

            seed = settings.getString(CFG_RANDOM_SEED);
            m_fraction = settings.getDouble(CFG_FRACTION);
            m_count = settings.getInt(CFG_COUNT);
        }
        m_classColumnName = settings.getString(CFG_CLASS_COLUMN, null);
        if (seed != null) {
            try {
                m_seed = Long.parseLong(seed);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Unable to parse seed "
                        + "string \"" + seed + "\", not a number");
            }
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
    public CountMethods countMethod() {
        return m_countMethod;
    }

    /**
     * Sets the method use for sampling the rows.
     *
     * @param method the sampling method
     */
    public void countMethod(final CountMethods method) {
        m_countMethod = method;
    }


    /**
     * Returns the sampling method.
     *
     * @return the sampling method
     */
    public SamplingMethods samplingMethod() {
        return m_samplingMethod;
    }


    /**
     * Sets the sampling method.
     *
     * @param method the sampling method
     */
    public void samplingMethod(final SamplingMethods method) {
        m_samplingMethod = method;
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

    /**
     * Sets the class column whose distribution should be retained when using
     * stratified sampling.
     *
     * @param columnName the name of the class column
     */
    public void classColumn(final String columnName) {
        m_classColumnName = columnName;
    }

    /**
     * Returns the class column whose distribution should be retained when using
     * stratified sampling.
     *
     * @return the name of the class column
     */
    public String classColumn() {
        return m_classColumnName;
    }
}
