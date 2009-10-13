/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   29.11.2004 (ohl): created
 */
package org.knime.base.node.io.filetokenizer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @deprecated use {@link org.knime.core.util.tokenizer.Delimiter} instead. Will
 *             be removed in Ver3.0.
 */
@Deprecated
public class Delimiter {
    private final String m_delim;

    private boolean m_combineMultiple;

    private boolean m_return;

    private boolean m_include;

    /* keys used to store parameters in a config object */
    private static final String CFGKEY_DELIM = "pattern";

    private static final String CFGKEY_COMBINE = "combineMultiple";

    private static final String CFGKEY_RETURN = "returnAsToken";

    private static final String CFGKEY_INCLUDE = "includeInToken";

    /**
     * Creates a new delimiter object. Only constructed by the
     * <code>FileTokenizerSettings</code> class.
     *
     * @see FileTokenizerSettings
     * @param pattern the delimiter patter
     * @param combineConsecutive boolean flag
     * @param returnAsSeparateToken boolean flag
     * @param includeInToken boolean flag
     */
    public Delimiter(final String pattern, final boolean combineConsecutive,
            final boolean returnAsSeparateToken, final boolean includeInToken) {

        m_delim = pattern;
        m_combineMultiple = combineConsecutive;
        m_return = returnAsSeparateToken;
        m_include = includeInToken;
    }

    /**
     * Creates a new <code>Delimiter</code> object and sets its parameters from
     * the <code>config</code> object. If config doesn't contain all necessary
     * parameters or contains inconsistent settings it will throw an
     * IllegalArguments exception
     *
     * @param settings an object the parameters are read from.
     * @throws InvalidSettingsException if the config is invalid. Right?
     */
    Delimiter(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings == null) {
            throw new NullPointerException("Can't initialize from a null "
                    + "object! Settings incomplete!!");
        }

        try {
            m_delim = settings.getString(CFGKEY_DELIM);
        } catch (InvalidSettingsException ice) {
            throw new InvalidSettingsException("Illegal config object for "
                    + "delimiter (missing key)! Settings incomplete!");

        }

        /*
         * if settings doesn't contain the key it will return the passed default
         * value
         */
        try {
            if (settings.containsKey(CFGKEY_COMBINE)) {
                m_combineMultiple = settings.getBoolean(CFGKEY_COMBINE);
            } else {
                m_combineMultiple = false;
            }
            if (settings.containsKey(CFGKEY_RETURN)) {
                m_return = settings.getBoolean(CFGKEY_RETURN);
            } else {
                m_return = false;
            }
            if (settings.containsKey(CFGKEY_INCLUDE)) {
                m_include = settings.getBoolean(CFGKEY_INCLUDE);
            } else {
                m_include = false;
            }
        } catch (InvalidSettingsException ice) {
            assert false;
            m_combineMultiple = false;
            m_return = false;
            m_include = false;
        }
    }

    /**
     * Writes the object into a <code>NodeSettings</code> object. If this config
     * object is then used to construct a new <code>Delimiter</code> this and
     * the new object should be identical.
     *
     * @param cfg a config object the internal values of this object will be
     *            stored into.
     */
    void saveToConfig(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'delimiter' "
                    + "to null config!");
        }

        cfg.addString(CFGKEY_DELIM, getDelimiter());
        cfg.addBoolean(CFGKEY_COMBINE, combineConsecutiveDelims());
        cfg.addBoolean(CFGKEY_INCLUDE, includeInToken());
        cfg.addBoolean(CFGKEY_RETURN, returnAsToken());

    }

    /**
     * @return The delimiter pattern.
     */
    public String getDelimiter() {
        return m_delim;
    }

    /**
     * @return <code>true</code> if consecutive appearances of this delimiter
     *         should be combined.
     */
    public boolean combineConsecutiveDelims() {
        return m_combineMultiple;
    }

    /**
     * @return <code>true</code> if this delimiter should be returned as
     *         separate token.
     */
    public boolean returnAsToken() {
        return m_return;
    }

    /**
     * @return <code>true</code> if this delimiter should be included in the
     *         prev. token.
     */
    public boolean includeInToken() {
        return m_include;
    }

    /**
     * @return The first character of this delimiter pattern.
     */
    public char getFirstChar() {
        return m_delim.charAt(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return FileTokenizerSettings.printableStr(getDelimiter());
    }

    /*
     * --- the equal and hash functions only look at the delimier. The don't
     * compare the value of any flag. --------------
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Delimiter)) {
            return false;
        }
        return this.getDelimiter().equals(((Delimiter)obj).getDelimiter());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getDelimiter().hashCode();
    }

} // Delimiter
