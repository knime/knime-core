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
 * Created for each comment pattern of the <code>FileTokenizer</code> keeping
 * its specifics.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class Comment {
    private final String m_left;

    private final String m_right;

    private boolean m_return;

    private boolean m_include;

    /* keys used to store parameters in a config object */
    private static final String CFGKEY_BEGIN = "begin";

    private static final String CFGKEY_END = "end";

    private static final String CFGKEY_RETURN = "returnAsToken";

    private static final String CFGKEY_INCLUDE = "includeInToken";

    /**
     * Creates a new Comment object. Only created by the
     * <code>FileTokenizerSettings</code> class.
     * 
     * @see FileTokenizerSettings
     * @param left The comment start pattern.
     * @param right The comment end pattern.
     * @param returnAsToken boolean flag.
     * @param includeInToken boolean flag.
     */
    public Comment(final String left, final String right,
            final boolean returnAsToken, final boolean includeInToken) {

        m_return = returnAsToken;
        m_include = includeInToken;
        m_left = left;
        m_right = right;
    }

    /**
     * Creates a new <code>Comment</code> object and sets its parameters from
     * the <code>config</code> object. If config doesn't contain all necessary
     * parameters or contains inconsistent settings it will throw an
     * IllegalArguments exception
     * 
     * @param settings an object the parameters are read from.
     * @throws InvalidSettingsException if the config is not valid. Huh.
     */
    Comment(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings == null) {
            throw new NullPointerException("Can't initialize from a null "
                    + "object! Settings incomplete!!");
        }

        try {
            m_left = settings.getString(CFGKEY_BEGIN);
            m_right = settings.getString(CFGKEY_END);
        } catch (InvalidSettingsException ice) {
            throw new InvalidSettingsException("Illegal config object for "
                    + "comment! Settings incomplete!");
        }

        try {
            if (settings.containsKey(CFGKEY_INCLUDE)) {
                m_include = settings.getBoolean(CFGKEY_INCLUDE);
            } else {
                // the default value
                m_include = false;
            }
            if (settings.containsKey(CFGKEY_RETURN)) {
                m_return = settings.getBoolean(CFGKEY_RETURN);
            } else {
                // the default values
                m_return = false;
            }
        } catch (InvalidSettingsException ice) {
            assert false;
            m_include = false;
            m_return = false;
        }

    }

    /**
     * Writes the object into a <code>NodeSettings</code> object. If this config
     * object is then used to construct a new <code>Comment</code> this and
     * the new object should be identical.
     * 
     * @param cfg a config object the internal values of this object will be
     *            stored into.
     */
    void saveToConfig(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'comment' "
                    + "to null config!");
        }

        cfg.addString(CFGKEY_BEGIN, getBegin());
        cfg.addString(CFGKEY_END, getEnd());
        cfg.addBoolean(CFGKEY_INCLUDE, includeInToken());
        cfg.addBoolean(CFGKEY_RETURN, returnAsSeparateToken());

    }

    /**
     * @return The begin pattern of this comment.
     */
    public String getBegin() {
        return m_left;
    }

    /**
     * @return The end pattern of this comment.
     */
    public String getEnd() {
        return m_right;
    }

    /**
     * @return The first character of the begin pattern of this comment.
     */
    public char getFirstCharOfBegin() {
        return m_left.charAt(0);
    }

    /**
     * @return <code>true</code> if this comment will be included in the
     *         current token.
     */
    public boolean includeInToken() {
        return m_include;
    }

    /**
     * @return <code>true</code> if this comment will be returned as separate
     *         token.
     */
    public boolean returnAsSeparateToken() {
        return m_return;
    }

    /**
     * Returns "[begin]...[end], i, r" for block comments, or "[begin] SL, i, r"
     * for single line comments, with 'i' only printed when flag
     * "indcludeInToken" is set, and 'r' only printed when flag
     * "returnAsSeparateToken is set.
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        boolean closeParan = false;
        
        result.append(getBegin());
        if ((getEnd() != null) && (getEnd().equals(FileTokenizer.LF_STR))) {
            result.append(" (SL");
            closeParan = true;
        } else {
            result.append("...");
            result.append(getEnd());
        }

        if (includeInToken()) {
            if (closeParan) {
                result.append(",i");
            } else {
                result.append(" (i");
                closeParan = true;
            }
        }
        if (returnAsSeparateToken()) {
            if (closeParan) {
                result.append(",r");
            } else {
                result.append(" (r");
                closeParan = true;
            }
        }
        if (closeParan) {
            result.append(")");
        }
        return result.toString();
    }

} // Comment
