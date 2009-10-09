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
 * @deprecated use org.knime.core.util.tokenizer.Quote instead
 */
@Deprecated
public class Quote {
    private final String m_left;

    private final String m_right;

    private final char m_escape;

    private final boolean m_hasEscape;

    private final boolean m_dontRemove;

    /* keys used to store parameters in a config object */
    private static final String CFGKEY_LEFT = "left";

    private static final String CFGKEY_RIGHT = "right";

    private static final String CFGKEY_ESC = "EscChar";

    private static final String CFGKEY_DONTREM = "DontRem";

    /**
     * Creates a new Quote object. Only constructed by the
     * <code>FileTokenizerSettings</code> class.
     *
     * @see FileTokenizerSettings
     * @param left the left quote pattern
     * @param right the right quote pattern
     * @param escape the escape character for these quotes.
     * @param dontRemove if set true the quote patterns will not be removed
     *            from, but returned in the token.
     */
    public Quote(final String left, final String right, final char escape,
            final boolean dontRemove) {
        m_left = left;
        m_right = right;
        m_escape = escape;
        m_hasEscape = true;
        m_dontRemove = dontRemove;
    }
    /**
     * Creates a new Quote object. The quotes will be removed from the token.
     * Only constructed by the <code>FileTokenizerSettings</code> class.
     *
     * @see FileTokenizerSettings
     * @param left the left quote pattern
     * @param right the right quote pattern
     * @param escape the escape character for these quotes.
     */
    public Quote(final String left, final String right, final char escape) {
        this(left, right, escape, false);
    }

    /**
     * Creates a new Quote object, without escape character, quotes being
     * removed from the token.
     *
     * @param left The left quote pattern.
     * @param right The right quote pattern.
     * @param dontRemove if set true quote patterns don't get removed but be
     *            returned in the token.
     */
    public Quote(final String left, final String right,
            final boolean dontRemove) {
        m_left = left;
        m_right = right;
        m_escape = '\0';
        m_hasEscape = false;
        m_dontRemove = dontRemove;
    }
    /**
     * Creates a new Quote object, without escape character, quotes being
     * removed from the token.
     *
     * @param left The left quote pattern.
     * @param right The right quote pattern.
     */
    public Quote(final String left, final String right) {
        this(left, right, false);
    }

    /**
     * Creates a new <code>Quote</code> object and sets its parameters from
     * the <code>config</code> object. If config doesn't contain all necessary
     * parameters or contains inconsistent settings it will throw an
     * IllegalArgument exception
     *
     * @param settings an object the parameters are read from.
     * @throws InvalidSettingsException when the config stinks.
     */
    Quote(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings == null) {
            throw new NullPointerException("Can't initialize from a null "
                    + "object! Settings incomplete!!");
        }
        try {
            m_left = settings.getString(CFGKEY_LEFT);
            m_right = settings.getString(CFGKEY_RIGHT);
        } catch (InvalidSettingsException ice) {
            throw new InvalidSettingsException("Illegal config object for "
                    + "quote (missing key)! Settings incomplete!");
        }
        try {
            if (settings.containsKey(CFGKEY_ESC)) {
                m_hasEscape = true;
                m_escape = settings.getChar(CFGKEY_ESC);
            } else {
                // the default value
                m_hasEscape = false;
                m_escape = ' ';
            }
        } catch (InvalidSettingsException ice) {
            throw new InvalidSettingsException("Illegal config object for "
                    + "quote (must not specify mult char string as escape"
                    + "character)! Settings incomplete!");
        }

        // optional for backward compatibility
        m_dontRemove = settings.getBoolean(CFGKEY_DONTREM, false);
    }

    /**
     * Writes the object into a <code>NodeSettings</code> object. If this
     * config object is then used to construct a new <code>Delimiter</code>
     * this and the new object should be identical.
     *
     * @param cfg a config object the internal values of this object will be
     *            stored into.
     */
    void saveToConfig(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'quote' "
                    + "to null config!");
        }

        cfg.addString(CFGKEY_LEFT, getLeft());
        cfg.addString(CFGKEY_RIGHT, getRight());
        if (hasEscapeChar()) {
            cfg.addChar(CFGKEY_ESC, getEscape());
        }

        cfg.addBoolean(CFGKEY_DONTREM, m_dontRemove);
    }

    /**
     * @return The left quote pattern.
     */
    public String getLeft() {
        return m_left;
    }

    /**
     * @return The right quote pattern.
     */
    public String getRight() {
        return m_right;
    }

    /**
     * @return <code>true</code> if an escape character is defined for this
     *         quote pair.
     */
    public boolean hasEscapeChar() {
        return m_hasEscape;
    }

    /**
     * @return If an escape character is defined it will return it, otherwise
     *         the return value is undefined.
     */
    public char getEscape() {
        return m_escape;
    }

    /**
     * @return true if the quote patterns will remain in the token, false, if
     *         they get removed and discarded when read.
     */
    public boolean getDontRemoveFlag() {
        return m_dontRemove;
    }

    /**
     * @return The first character of the left quote pattern.
     */
    public char getFirstCharOfLeft() {
        return m_left.charAt(0);
    }

    /**
     * Returns "[left]...[right], '[esc]'", with ", [esc]" only printed when an
     * escape char is defined.
     *
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getLeft());
        result.append("...");
        result.append(getRight());
        if (hasEscapeChar()) {
            result.append(", '");
            result.append(getEscape());
            result.append("'");
        }
        if (m_dontRemove) {
            result.append("(remains)");
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Quote) {
            Quote q = (Quote)obj;
            if (q.m_dontRemove != this.m_dontRemove) {
                return false;
            }
            if (q.m_escape != this.m_escape) {
                return false;
            }
            if (q.m_hasEscape != this.m_hasEscape) {
                return false;
            }
            if (!q.m_left.equals(this.m_left)) {
                return false;
            }
            if (!q.m_right.equals(this.m_right)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getLeft().hashCode() ^ getRight().hashCode();
    }

}
