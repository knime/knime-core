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

import java.util.Iterator;
import java.util.Vector;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import org.knime.base.node.io.filereader.SettingsStatus;

/**
 * Defines the object holding the configuration for the FileTokenizer. <br>
 * Use an instance of this class to set all parameters and pass it to a
 * <code>FileTokenizer</code>. This object is used as a transport vehicle to
 * first try setting new user configurations and, if everything went fine (i.e.
 * without any exception), transporting them into the file tokenizer. This class
 * is used in both directions - to get current tokenizer settings, and to set a
 * new configuration in the tokenizer. The methods with default permissions are
 * only used by the file tokenizer to set its current settings in this object -
 * any object user outside the package will retrieve them then through the
 * get-methods. While new user settings will be implanted from the
 * out-of-package world with the set-methods.
 * 
 * @see FileTokenizer
 * @author ohl, University of Konstanz
 */
public class FileTokenizerSettings {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(FileTokenizerSettings.class);

    /* the column delimiters we handle */
    private final Vector<Delimiter> m_delimPatterns;

    /* the quotes for strings */
    private final Vector<Quote> m_quotePatterns;

    /* the patterns marking comments */
    private final Vector<Comment> m_commentPatterns;

    /* the user defined whitespaces (strings) */
    private final Vector<String> m_whitespaces;

    private String m_lineContChar;

    private boolean m_combineMultiple;

    /* keys used to store parameters in a config object */
    private static final String CFGKEY_DELIMS = "Delimiters";

    private static final String CFGKEY_QUOTES = "Quotes";

    private static final String CFGKEY_COMMENTS = "Comments";

    private static final String CFGKEY_WHITES = "WhiteSpaces";

    private static final String CFGKEY_LINECONT = "LineContChar";

    private static final String CFGKEY_DELIMCFG = "Delim";

    private static final String CFGKEY_QUOTECFG = "Quote";

    private static final String CFGKEY_COMMNTCFG = "Comment";

    private static final String CFGKEY_WSPACECFG = "WhiteSpace";

    private static final String CFGKEY_COMBMULTI = "CombineMultDelims";

    /**
     * Creates a new Settings for FileTokenizer object with default settings.
     * 
     * @see FileTokenizer#resetToDefault() for description of default settings.
     */
    public FileTokenizerSettings() {

        m_delimPatterns = new Vector<Delimiter>();
        m_quotePatterns = new Vector<Quote>();
        m_commentPatterns = new Vector<Comment>();
        m_whitespaces = new Vector<String>();

        m_lineContChar = null;
        m_combineMultiple = false;
    }

    /**
     * Creates a clone of the passed object.
     * 
     * @param clonee the object to read the settings from.
     */
    public FileTokenizerSettings(final FileTokenizerSettings clonee) {
        m_delimPatterns = new Vector<Delimiter>(clonee.m_delimPatterns);
        m_quotePatterns = new Vector<Quote>(clonee.m_quotePatterns);
        m_commentPatterns = new Vector<Comment>(clonee.m_commentPatterns);
        m_whitespaces = new Vector<String>(clonee.m_whitespaces);

        m_lineContChar = clonee.m_lineContChar;
        m_combineMultiple = clonee.m_combineMultiple;

    }

    /**
     * Creates a new <code>FileTokenizerSettings</code> object and sets its
     * parameters from the <code>config</code> object. If config doesn't
     * contain all necessary parameters or contains inconsistent settings it
     * will throw an InvalidArguments exception
     * 
     * @param settings an object the parameters are read from, if null default
     *            settings will be created.
     * @throws InvalidSettingsException if the config is not valid
     */
    public FileTokenizerSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        this();

        if (settings != null) {
            try {
                // get the configuration that holds all delimiters
                NodeSettingsRO delims = settings.getNodeSettings(CFGKEY_DELIMS);
                addDelimitersFromConfiguration(delims);
                // get the configuration that holds all quotes
                NodeSettingsRO quotes = settings.getNodeSettings(CFGKEY_QUOTES);
                addQuotesFromConfiguration(quotes);
                // get the configuration that holds all comments
                NodeSettingsRO comments =
                        settings.getNodeSettings(CFGKEY_COMMENTS);
                addCommentsFromConfiguration(comments);
                // get the config holding white spaces
                NodeSettingsRO wspaces =
                        settings.getNodeSettings(CFGKEY_WHITES);
                addWhitesFromConfiguration(wspaces);

            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for "
                        + "file tokenizer settings (missing key)! "
                        + "Settings incomplete!");
            }

            if (settings.containsKey(CFGKEY_LINECONT)) {
                try {
                    setLineContinuationCharacter(settings
                            .getChar(CFGKEY_LINECONT));
                } catch (InvalidSettingsException ice) {
                    throw new InvalidSettingsException(
                            "Illegal config object for "
                                    + "file tokenizer settings (Must not "
                                    + "specify multi char string for a char)! "
                                    + "Settings incomplete!");
                }
            }

            setCombineMultipleDelimiters(settings.getBoolean(CFGKEY_COMBMULTI,
                    false));

        } // if (settings != null)
    }

    /**
     * Saves all settings into a <code>NodeSettings</code> object. Using the
     * cfg object to construct a new FileTokenizerSettings object should lead to
     * an object identical to this.
     * 
     * @param cfg the config object the settings are stored into.
     */
    public void saveToConfiguration(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'file "
                    + "tokenizer settings' to null config!");
        }

        // save delimiters first
        NodeSettingsWO subCfg = cfg.addNodeSettings(CFGKEY_DELIMS);
        saveDelimitersToConfiguration(subCfg);

        // save quotes next
        subCfg = cfg.addNodeSettings(CFGKEY_QUOTES);
        saveQuotesToConfiguration(subCfg);

        // also, save comments
        subCfg = cfg.addNodeSettings(CFGKEY_COMMENTS);
        saveCommentsToConfiguration(subCfg);

        // do the whitespaces
        subCfg = cfg.addNodeSettings(CFGKEY_WHITES);
        saveWhitesToConfiguration(subCfg);

        // add the linecontinuatino character if defined
        String lineCont = getLineContinuationCharacter();
        if (lineCont != null) {
            assert lineCont.length() == 1;
            cfg.addChar(CFGKEY_LINECONT, lineCont.charAt(0));
        }

        // add the flag for combining multiple delimiters
        cfg.addBoolean(CFGKEY_COMBMULTI, getCombineMultipleDelimiters());
    }

    /*
     * trys to add all delimiters defined in the passed configuration object. It
     * expects the config to contain delimiters only. if anything illegal is
     * defined in there, it is going to print an error message and is ignoring
     * it.
     */
    private void addDelimitersFromConfiguration(
            final NodeSettingsRO allDelims) {
        for (String delimKey : allDelims.keySet()) {
            // they should all start with "Delim"...
            if (delimKey.indexOf(CFGKEY_DELIMCFG) != 0) {
                LOGGER.warn("Illegal delimiter configuration '" + delimKey
                        + "' (wrong prefix). Ignoring it!");
                continue;
            }

            NodeSettingsRO delimSettings;
            try {
                delimSettings = allDelims.getNodeSettings(delimKey);
            } catch (InvalidSettingsException ice) {
                assert false; // we've checked the type before...
                LOGGER.warn("Illegal delimiter configuration '" + delimKey
                        + "'. Ignoring it!");
                continue;
            }
            try {
                // try constructing and adding it
                Delimiter delim = new Delimiter(delimSettings);
                addDelimiterPattern(delim);

            } catch (InvalidSettingsException ice) {
                LOGGER.warn(ice.getMessage() + "Ignoring '" + delimKey + "'!");
                continue;
            } catch (IllegalArgumentException iae) {
                LOGGER.error("Error delimiter configuration '" + delimKey
                        + "'.");
                LOGGER.error(iae.getMessage());
                LOGGER.error("Ignoring delimiter!");
                continue;
            }
        }
    }

    /*
     * saves the settings of all delimiters defined by adding a configuration
     * object for each delimiter to the passed config.
     */
    private void saveDelimitersToConfiguration(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'delimiters' "
                    + "to null config!");
        }
        for (int d = 0; d < m_delimPatterns.size(); d++) {
            NodeSettingsWO delimConf = cfg.addNodeSettings(CFGKEY_DELIMCFG + d);
            Delimiter delim = m_delimPatterns.get(d);
            delim.saveToConfig(delimConf);
        }

    }

    /*
     * trys to add all quotes defined in the passed configuration object. It
     * expects the config to contain quotes only. if anything illegal is defined
     * in there, it is going to print an error message and is ignoring it.
     */
    private void addQuotesFromConfiguration(final NodeSettingsRO allQuotes) {
        for (String quoteKey : allQuotes.keySet()) {
            // they should all start with "Quote"...
            if (quoteKey.indexOf(CFGKEY_QUOTECFG) != 0) {
                LOGGER.warn("Illegal quote configuration '" + quoteKey
                        + "'. Ignoring it!");
                continue;
            }

            NodeSettingsRO quoteSettings;
            try {
                quoteSettings = allQuotes.getNodeSettings(quoteKey);
            } catch (InvalidSettingsException ice) {
                assert false; // just tested it ...
                LOGGER.warn("Illegal quote configuration '" + quoteKey
                        + "'. Ignoring it!");
                continue;
            }
            try {
                // try constructing and adding it
                Quote quote = new Quote(quoteSettings);
                addQuotePattern(quote);
            } catch (InvalidSettingsException ice) {
                LOGGER.warn(ice.getMessage() + "'. Ignoring '" + quoteKey
                        + "'!");
            } catch (IllegalArgumentException iae) {
                LOGGER.error("ERROR: in quote configuration '" + quoteKey
                        + "'.");
                LOGGER.error(iae.getMessage());
                LOGGER.error("Ignoring quote!");
            }
        }
    }

    /*
     * saves the settings of all quotes defined by adding a configuration object
     * for each quote to the passed config.
     */
    private void saveQuotesToConfiguration(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'quotes' "
                    + "to null config!");
        }
        for (int q = 0; q < m_quotePatterns.size(); q++) {
            NodeSettingsWO quoteConf = cfg.addNodeSettings(CFGKEY_QUOTECFG + q);
            Quote quote = m_quotePatterns.get(q);
            quote.saveToConfig(quoteConf);
        }

    }

    /*
     * trys to add all comments defined in the passed configuration object. It
     * expects the config to contain comments only. if anything illegal is
     * defined in there, it is going to print an error message and is ignoring
     * it.
     */
    private void addCommentsFromConfiguration(
            final NodeSettingsRO allComments) {
        for (String commentKey : allComments.keySet()) {
            // they should all start with "Comment"...
            if (commentKey.indexOf(CFGKEY_COMMNTCFG) != 0) {
                LOGGER.warn("Illegal comment configuration '" + commentKey
                        + "'. Ignoring it!");
                continue;
            }

            NodeSettingsRO commentSettings;

            try {
                commentSettings = allComments.getNodeSettings(commentKey);
            } catch (InvalidSettingsException ice) {
                assert false; // we've just checked the type...
                LOGGER.warn("Illegal comment configuration '" + commentKey
                        + "'. Ignoring it!");
                continue;
            }

            try {
                // try constructing and adding it
                Comment comment = new Comment(commentSettings);
                addCommentPattern(comment);
            } catch (InvalidSettingsException ice) {
                LOGGER.warn("Illegal comment configuration '" + commentKey
                        + "'. Ignoring it!");
                continue;
            } catch (IllegalArgumentException iae) {
                LOGGER.error("ERROR: in comment configuration '" + commentKey
                        + "'.");
                LOGGER.error(iae.getMessage());
                LOGGER.error("Ignoring comment!");
            }
        }
    }

    private void addWhitesFromConfiguration(final NodeSettingsRO allWhites) {
        for (String whitesKey : allWhites.keySet()) {
            // this must not go wrong.
            String ws = allWhites.getString(whitesKey, null);
            try {
                addWhiteSpaceCharacter(ws);
            } catch (IllegalArgumentException iae) {
                LOGGER.error("Invalid whitespace definition in tokenizer "
                        + "configuration (ignoring '" + ws + "')");
            }
        }
    }

    /*
     * saves the settings of all Comments defined by adding a configuration
     * object for each comment to the passed config.
     */
    private void saveCommentsToConfiguration(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'comments' "
                    + "to null config!");
        }
        for (int c = 0; c < m_commentPatterns.size(); c++) {
            NodeSettingsWO commentConf =
                    cfg.addNodeSettings(CFGKEY_COMMNTCFG + c);
            Comment comment = m_commentPatterns.get(c);
            comment.saveToConfig(commentConf);
        }

    }

    /*
     * saves user defined whitepsaces into the passed configuration
     */
    private void saveWhitesToConfiguration(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'whitespaces' "
                    + "to null config!");
        }
        for (int w = 0; w < m_whitespaces.size(); w++) {
            cfg.addString(CFGKEY_WSPACECFG + w, m_whitespaces.get(w));
        }

    }

    /*
     * ------ out-of-package user functions -----------------------------
     */
    /**
     * Adds support for the specified quote patterns and escape character. The
     * tokenizer will treat any string within the specified
     * <code>leftQuote</code> and <code>rightQuote</code> as quoted string,
     * i.e. any token delimiter will not end the token but will be included in
     * the string and no comment will be recognized inside a quoted string. With
     * the escape character it is possible to include special characters (like
     * new line e.g.) or even the right quote pattern in the string. The esc
     * character and the immediate next char will be translated into one new
     * character: %EscChar%+'t' becomes '\t' (Tab), +'n' translates int '\n'
     * (Newline), EscChar+any other char becomes this other character. The
     * escape character cannot be part of the end pattern. A typical call to
     * this function adding support for single quotes would be
     * addQuotePattern("'", "'", "\\"); - support for double quotes
     * addQuotePattern("\"", "\"", "\\"); - both calls also add support for the
     * escape character '\'. If you don't want an escape character, use the next
     * function. The Quote patterns get removed from the token by default. There
     * are methods that take a flag, if you want them to remain in the token.
     * 
     * @param leftQuote A string containing the left quote pattern.
     * @param rightQuote A string containing the right quote pattern.
     * @param escapeChar The escape character.
     */
    public void addQuotePattern(final String leftQuote,
            final String rightQuote, final char escapeChar) {

        addQuotePattern(new Quote(leftQuote, rightQuote, escapeChar));
    }

    /**
     * @param leftQuote the left quote pattern
     * @param rightQuote the right quote pattern
     * @param escapeChar the escape character inside a quoted text
     * @param dontRemoveQuotes true if quote patterns should stay in the token,
     *            false, if they should be removed from the returned token.
     */
    public void addQuotePattern(final String leftQuote,
            final String rightQuote, final char escapeChar,
            final boolean dontRemoveQuotes) {
        addQuotePattern(new Quote(leftQuote, rightQuote, escapeChar,
                dontRemoveQuotes));
    }

    /**
     * @param leftQuote The left quot char.
     * @param rightQuote The right quot char.
     * 
     * @see #addQuotePattern(String, String, char)
     */
    public void addQuotePattern(
            final String leftQuote, final String rightQuote) {

        addQuotePattern(new Quote(leftQuote, rightQuote));
    }

    /**
     * @param leftQuote the left quote pattern
     * @param rightQuote the right quote pattern
     * @param dontRemoveQuotes true if quote patterns should stay in the token,
     *            false, if they should be removed from the returned token.
     */
    public void addQuotePattern(final String leftQuote,
            final String rightQuote, final boolean dontRemoveQuotes) {
        addQuotePattern(new Quote(leftQuote, rightQuote, dontRemoveQuotes));
    }

    /*
     * adds a new Quotepattern expecting a Quote object. Does all kinds of
     * checking and throws IllegalArgument exceptions
     */
    private void addQuotePattern(final Quote quote) {

        assert quote != null;

        try {
            // throws an IllegalArgumentException is something is wrong in there
            checkQuotePattern(quote);
        } catch (IllegalArgumentException iae) {
            String msg = "Add Quote: \n" + iae.getMessage();
            throw new IllegalArgumentException(msg);
        }

        m_quotePatterns.add(quote);

    }

    /*
     * throws an IllegalArgumentException if this quote conflicts with any other
     * already defined quote, or has some invalid settings.
     */
    private void checkQuotePattern(final Quote quote) {

        String errMsg = "";

        if (quote.hasEscapeChar()
                && (quote.getRight().indexOf(quote.getEscape()) != -1)) {
            errMsg +=
                    "The escape character must not occure in the right "
                            + "quote pattern.\n";
        }

        if ((quote.getRight() == null) || (quote.getRight().length() < 1)) {
            errMsg += "Right quote pattern must be a non-empty string\n";
        }
        if ((quote.getLeft() == null) || (quote.getLeft().length() < 1)) {
            errMsg += "Left quote pattern must be a non-empty string\n";
        } else {
            if (quote.getLeft().charAt(0) > FileTokenizer.MAX_CHAR) {
                errMsg +=
                        "The left quote must begin with a plain ASCII "
                                + "character (ascii code < 127) \n";
            }
        }
        if (!errMsg.equals("")) {
            throw new IllegalArgumentException(errMsg);
        }
        // make sure no other comment/quote begin pattern is a prefix to
        // this quote - and vice versa.
        errMsg = checkPrefixing(quote.getLeft(), "left quote", quote);
        if (!errMsg.equals("")) {
            throw new IllegalArgumentException(errMsg);
        }

    }

    /**
     * Removes the Quote object with the specified patterns from the list of
     * defined quotes. Returns the removed quote object, if it existed or null
     * if the patterns didn't match any.
     * 
     * @param begin the quote begin pattern to match
     * @param end the quote end pattern to match
     * @return the quote object removed, or null if no quote with the specified
     *         begin and end pattern existed.
     */
    public Quote removeQuotePattern(final String begin, final String end) {
        for (int i = 0; i < m_quotePatterns.size(); i++) {
            if (m_quotePatterns.get(i).getLeft().equals(begin)
                    && m_quotePatterns.get(i).getRight().equals(end)) {
                return m_quotePatterns.remove(i);
            }
        }
        return null;
    }

    /**
     * Removes all (!) quotes from the file reader settings. Not a single quote
     * will be defined after a call to this method.
     */
    public void removeAllQuotes() {
        m_quotePatterns.clear();
    }

    /**
     * Adds a delimiter to the tokenizer. If a delimiter is read - outside any
     * comment block or quoted string - the characters read before will be
     * returned as token. Depending on the parameters, the delimiter just read
     * will be either appended to the current token (
     * <code>includeInToken</code> set <code>true</code>), returned in a
     * separate token (<code> returnAsSeparateToken</code> set <code>true
     * </code>)
     * or discarded (both set <code>false</code>). If you set both parameters
     * <code>true</code>, it will throw an <code>
     * IllegalArgumentException</code>.
     * Another parameter (<code>
     * combineConsecutiveDelimis</code>) will
     * determine whether delimiters of the same kind immediately following will
     * be ignored (set to <code>true
     * </code>) or will cause empty tokens to be
     * returned (set <code>false
     * </code>). The delimiter specified must not
     * prefix any existing delimiter, left quote or comment begin pattern.
     * 
     * @param delimiter A string containing the delimiter.
     * @param combineConsecutiveDelims Pass in <code>true</code>, if you want
     *            multiple consecutive delimiters to be treated as one, or
     *            <code>false</code> if empty tokens should be returned
     *            between them.
     * @param returnAsSeparateToken Set to <code>true</code> to get delimiters
     *            returned as tokens, or <code>false</code> if they should be
     *            discarded (or included in the tokens - see next parameter).
     *            Mutually exclusive with <code>includeInToken</code>.
     * @param includeInToken Set to <code>true</code> if you want the
     *            delimiter returned at the end of the token. Otherwise it will
     *            be discarded (or returned as separate token, see parameter
     *            above). Mutually exclusive with <code>returnAsSeparateToken
     *            </code>.
     */
    public void addDelimiterPattern(final String delimiter,
            final boolean combineConsecutiveDelims,
            final boolean returnAsSeparateToken, final boolean includeInToken) {

        addDelimiterPattern(new Delimiter(delimiter, combineConsecutiveDelims,
                returnAsSeparateToken, includeInToken));
    }

    /**
     * Adds a new delimiter pattern expecting a Delimiter object. Does all kinds
     * of checkings and throws IllegalArgument exceptions.
     * 
     * @param delimiter the delimiter to add.
     */
    protected void addDelimiterPattern(final Delimiter delimiter) {

        assert delimiter != null;

        try {
            // throws an IllegalArgumentException if something is wrong
            // with the delimiter.
            checkDelimiterPattern(delimiter);
        } catch (IllegalArgumentException iae) {
            String msg = "Add Delimiter: \n" + iae.getMessage();
            throw new IllegalArgumentException(msg);
        }

        m_delimPatterns.add(delimiter);

    } // addDelimiterPatter(Delimiter)

    /*
     * throws an IllegalArgumentException if this delimiter conflicts with any
     * other already defined one, or has some invalid settings.
     */
    private void checkDelimiterPattern(final Delimiter delimiter) {

        String errMsg = "";

        assert delimiter != null;

        if ((delimiter.getDelimiter() == null)
                || (delimiter.getDelimiter().length() < 1)) {
            errMsg += "Delimiter pattern must be a non-empty string\n";
        } else {
            if (delimiter.getDelimiter().charAt(0) > FileTokenizer.MAX_CHAR) {
                errMsg +=
                        "The delimiter must begin with a plain ASCII "
                                + "character (ascii code < 127) \n";
            }
        }
        if (delimiter.returnAsToken() && delimiter.includeInToken()) {
            errMsg +=
                    "Cannot set 'returnAsSeparateToken' "
                            + "AND 'includeInToken'. "
                            + "They are mutually exclusive!\n";
        }

        if (!errMsg.equals("")) {
            throw new IllegalArgumentException(errMsg);
        }

        // make sure no other delim/comment/quote begin pattern is a prefix to
        // this delimiter - and vice versa.
        errMsg =
                checkPrefixing(delimiter.getDelimiter(), "delimiter", 
                        delimiter);
        if (!errMsg.equals("")) {
            throw new IllegalArgumentException(errMsg);
        }

    }

    /**
     * Replaces the delimiter with the same delimiter pattern overriding the
     * values for <code>combineConsecutiveDelims</code>,
     * <code>returnAsSeparateToken</code>, and <code>includeInToken</code>.
     * It will return <code>true</code>, if everything works fine -
     * <code>false</code>, if it couldn't find a matching delimiter to
     * replace.
     * 
     * @param delimiter The pattern matching the delimiter to replace.
     * @param combineConsecutiveDelims New value for this parameter.
     * @param returnAsSeparateToken New value for this parameter.
     * @param includeInToken New value for this parameter.
     * @return <code>true</code> if it replaced the delimiter or false if it
     *         was added.
     */
    public boolean addOrReplaceDelimiterPattern(final String delimiter,
            final boolean combineConsecutiveDelims,
            final boolean returnAsSeparateToken, final boolean includeInToken) {

        String errMsg = "";
        if ((delimiter == null) || (delimiter.length() < 1)) {
            errMsg +=
                    "You must pass a non-empty string "
                            + "as delimiter pattern\n";
        }
        if (returnAsSeparateToken && includeInToken) {
            errMsg +=
                    "You cannot specify 'returnAsSeparateToken' AND"
                            + "'includeInToken'. "
                            + "They are mutually exclusive!\n";
        }
        if (delimiter.charAt(0) > FileTokenizer.MAX_CHAR) {
            errMsg +=
                    "The delimiter must begin with a plain ASCII "
                            + "character (ascii code < 127) \n";
        }
        if (!errMsg.equals("")) {
            errMsg = "replaceDelimiterPattern:\n" + errMsg;
            throw new IllegalArgumentException(errMsg);
        }

        Delimiter newDelim =
                new Delimiter(delimiter, combineConsecutiveDelims,
                        returnAsSeparateToken, includeInToken);

        int i;
        for (i = 0; i < m_delimPatterns.size(); i++) {
            if (m_delimPatterns.get(i).getDelimiter().equals(delimiter)) {
                break;
            }
        }
        if (i == m_delimPatterns.size()) {
            // it's not there. Add it.
            addDelimiterPattern(newDelim);
            return false;
        } else {
            // now replace the existing delimiter with a new one
            m_delimPatterns.set(i, newDelim);
            return true;
        }

    } // addOrReplaceDelimiterPattern(String,boolean,boolean,boolean)

    /**
     * Returns the Delimiter object stored for the delimiter with the pattern
     * specified.
     * 
     * @param delimPattern the string pattern of the delimiter to look for.
     * @return the Delimiter object of the specified delimiter pattern, if
     *         defined, otherwise null.
     */
    public Delimiter getDelimiterPattern(final String delimPattern) {
        for (Delimiter delim : m_delimPatterns) {
            if (delim.getDelimiter().equals(delimPattern)) {
                return delim;
            }
        }
        return null;

    }

    /**
     * Removes the Delimiter object with the specified pattern from the list of
     * defined delimiters. Returns the removed delimiter object, if it existed
     * or null if the pattern didn't exist.
     * 
     * @param pattern the delimiter to remove
     * @return the delimiter object removed, or null if no delimiter with the
     *         specified pattern existed.
     */
    public Delimiter removeDelimiterPattern(final String pattern) {
        for (int i = 0; i < m_delimPatterns.size(); i++) {
            if (m_delimPatterns.get(i).getDelimiter().equals(pattern)) {
                return m_delimPatterns.remove(i);
            }
        }
        return null;
    }

    /**
     * Removes all (!) delimiters from the file reader settings. Not a single
     * delimiter will be defined after a call to this method.
     */
    public void removeAllDelimiters() {
        m_delimPatterns.clear();
    }

    /**
     * Checks if the newPattern prefixes or is prefixed by any existing comment
     * begin, left quote, or delimiter pattern. If so, it returns a message
     * telling which pattern prefixes the other. If not, it returns an empty
     * string.
     * 
     * @param newPattern String to check against all existing patterns.
     * @param patternName Name that will be printed in the errormessage to name
     *            the new pattern.
     * @param skipObject the object NOT to check prefixing for. As we use this
     *            method to check all defined patterns against each other we
     *            need to skip the one currently under test. Can be null.
     * @return An error message, or an empty string.
     */
    private String checkPrefixing(final String newPattern,
            final String patternName, final Object skipObject) {
        String errMsg = "";

        // check existing delimiters
        for (Delimiter d : m_delimPatterns) {
            if (d == skipObject) {
                continue;
            }
            String delim = d.getDelimiter();
            if (newPattern.indexOf(delim) == 0) {
                errMsg +=
                        "The already defined delimiter '" + delim
                                + "' prefixes the " + patternName
                                + " pattern.\n";
            } else if (delim.indexOf(newPattern) == 0) {
                errMsg +=
                        "The " + patternName + " pattern prefixes the "
                                + "already defined delimiter '" + delim
                                + "'.\n";
            }
        }
        // check defined quotes - and there the left quote only
        for (Quote q : m_quotePatterns) {
            if (q == skipObject) {
                continue;
            }
            String quote = q.getLeft();
            if (newPattern.indexOf(quote) == 0) {
                errMsg +=
                        "The already defined quote pattern '" + quote
                                + "' prefixes the " + patternName
                                + " pattern.\n";
            } else if (quote.indexOf(newPattern) == 0) {
                errMsg +=
                        "The " + patternName + " pattern prefixes the already "
                                + "defined quote '" + quote + "'.\n";
            }
        }
        // and now for the comment patterns - that is the comment begin
        for (Comment c : m_commentPatterns) {
            if (c == skipObject) {
                continue;
            }
            String comment = c.getBegin();
            if (newPattern.indexOf(comment) == 0) {
                errMsg +=
                        "The already defined comment begin pattern '" + comment
                                + "' prefixes the " + patternName
                                + " pattern.\n";
            } else if (comment.indexOf(newPattern) == 0) {
                errMsg +=
                        "The " + patternName + " pattern prefixes the already"
                                + "defined comment pattern '" + comment
                                + "'.\n";
            }
        }

        return errMsg;
    } // checkPrefixing(String,String)

    /**
     * Adds support for block comment to the tokenizer. Everything between the
     * comment begin pattern and the comment end pattern will be ignored, and
     * either returned as separate token (if <code>returnAsSeparateToken</code>
     * is set <code>true</code>), included in the token (if
     * <code>includeInToken</code> is <code>true</code>), or discarded (if
     * both parameters are set <code>false</code>). (If you specify both
     * parameters <code>true</code> it will throw an
     * <code>IllegalArgumentException</code>.)
     * 
     * @param commentBegin The string containing a pattern that starts a
     *            comment.
     * @param commentEnd The string containing the end pattern of the comment.
     *            (Must not be a LF ("\n"). Use the next function for line
     *            comment.)
     * @param returnAsSeparateToken Set to <code>true</code> if the comment
     *            should be returned in a separate token, or <code>false</code>
     *            if it should be discarded, or included in the token (see the
     *            following parameter).
     * @param includeInToken Set <code>true</code> if a comment should be
     *            returned within the token (at the place where it occured in
     *            the stream), of false, if it should be discarded or returned
     *            as separate token (depending on the parameter above).
     */
    public void addBlockCommentPattern(final String commentBegin,
            final String commentEnd, final boolean returnAsSeparateToken,
            final boolean includeInToken) {

        addCommentPattern(new Comment(commentBegin, commentEnd,
                returnAsSeparateToken, includeInToken));
    }

    /**
     * Adds support for single line comment to the tokenizer. Everything between
     * the comment begin pattern and the next line feed will be ignored, and
     * either returned as separate token (if <code>returnAsSeparateToken</code>
     * is set <code>true</code>), included in the token (if
     * <code>includeInToken</code> is <code>true</code>), or discarded (if
     * both parameters are set <code>false</code>). (If you specify both
     * parameters <code>true</code> it will throw an
     * <code>IllegalArgumentException</code>.)
     * 
     * @param commentBegin The string containing a pattern that starts a single
     *            line comment.
     * @param returnAsSeparateToken Set to <code>true</code> if the comment
     *            should be returned in a separate token, or <code>false</code>
     *            if it should be discarded, or included in the token (see the
     *            following parameter).
     * @param includeInToken Set <code>true</code> if a comment should be
     *            returned within the token (at the place where it occured in
     *            the stream), of false, if it should be discarded or returned
     *            as separate token (depending on the parameter above).
     */
    public void addSingleLineCommentPattern(final String commentBegin,
            final boolean returnAsSeparateToken, final boolean includeInToken) {

        /*
         * Single line comment is not really different from block comment, just
         * with "\n" as end pattern. But - the end pattern is not part of the
         * comment! Which is taken care of, when we read the comment from the
         * stream.
         */

        addCommentPattern(new Comment(commentBegin, FileTokenizer.LF_STR,
                returnAsSeparateToken, includeInToken));
    }

    /*
     * adds a comment pattern expecting a Comment object. It does all kinds of
     * checking and throws IllegalArgument exceptions.
     */
    private void addCommentPattern(final Comment comment) {

        assert comment != null;

        try {
            // throws an illegal arg exception if something is wring with
            // the comment definition
            checkCommentPattern(comment);
        } catch (IllegalArgumentException iae) {
            String msg = "Add Comment: \n" + iae.getMessage();
            throw new IllegalArgumentException(msg);
        }

        m_commentPatterns.add(comment);

    } // addCommentPattern(Comment)

    /*
     * throws an IllegalArgumentException if this delimiter conflicts with any
     * other already defined one, or has some invalid settings.
     */
    private void checkCommentPattern(final Comment comment) {
        assert comment != null;

        String errMsg = "";
        if ((comment.getBegin() == null) || (comment.getBegin().length() < 1)) {
            errMsg += "The comment begin pattern must be a non-empty string.\n";
        } else {
            if (comment.getBegin().charAt(0) > FileTokenizer.MAX_CHAR) {
                errMsg +=
                        "The comment pattern must begin with a plain ASCII "
                                + "character (ascii code < 127) \n";
            }
        }
        if ((comment.getEnd() == null) || (comment.getEnd().length() < 1)) {
            errMsg += "The comment end pattern must be a non-empty string.\n";
        }
        if (comment.returnAsSeparateToken() && comment.includeInToken()) {
            errMsg +=
                    "Cannot specify 'returnAsSeparateToken' AND"
                            + "'includeInToken'. "
                            + "They are mutually exclusive!\n";
        }

        if (!errMsg.equals("")) {
            throw new IllegalArgumentException(errMsg);
        }

        // make sure no other delim/comment/quote begin pattern is a prefix to
        // this delimiter - and vice versa.
        errMsg = checkPrefixing(comment.getBegin(), "comment begin", comment);
        if (!errMsg.equals("")) {
            throw new IllegalArgumentException(errMsg);
        }

    }

    /**
     * Removes all (!) comments from the tokenier settings. Not a single comment
     * will be defined after a call to this method.
     */
    public void removeAllComments() {
        m_commentPatterns.clear();
    }

    /**
     * Defines a new character to be handled as a whitespace character.
     * Whitespaces will be ignored when they appear in the file (except when
     * inside quotes or defined as delimiter/quote/comment pattern). Any other
     * definition of the same character overrides the whitespace definition,
     * i.e., e.g. if the same character is defined as linecontinuation char it
     * will be treated as such, the whitespace definition of this char will be
     * (silently) ignored.
     * 
     * @param ws a one character string containing the new whitespace character
     */
    public void addWhiteSpaceCharacter(final String ws) {
        String errMsg = "";
        if (ws == null) {
            errMsg += "Please specify a non-null whitespace character.\n";
        }
        if (ws.length() != 1) {
            errMsg += "Please specify a one-character string as whitespace.\n";
        }
        if (ws.charAt(0) > FileTokenizer.MAX_CHAR) {
            errMsg +=
                    "The whitespace must begin with a plain ASCII "
                            + "character (ascii code < 127) \n";
        }
        if (!errMsg.equals("")) {
            throw new IllegalArgumentException("Add whitespace: " + errMsg);
        }
        if (!m_whitespaces.contains(ws)) {
            m_whitespaces.add(ws);
        }
    }

    /**
     * This is a convenience method. Whitespace characters are handled as
     * one-character strings.
     * 
     * @see #addWhiteSpaceCharacter(String)
     * 
     * @param w character containing the new whitespace character
     */
    public void addWhiteSpaceCharacter(final char w) {
        addWhiteSpaceCharacter(new String(new char[]{w}));
    }

    /**
     * removes all user defined whitespaces. No whitespaces will be ignored
     * after that.
     */
    public void removeAllWhiteSpaces() {
        m_whitespaces.clear();
    }

    /**
     * Adds support for line continuation in tokens and quoted strings. The
     * tokenizer ignores a new line, if the last character in a line was the
     * specified character <i>c </i> (No trailing spaces allowed!) and it will
     * also ignore any space or tab character at the beginning of the new line
     * then.
     * <p>
     * <b>The following two quoted strings are equivalent if '\' is set as line
     * cont. char: "this is \<br>
     * considered one line" and "this is considered one line". </b>
     * 
     * @param c The new line continuation character.
     */
    public void setLineContinuationCharacter(final char c) {
        m_lineContChar = new String(new char[]{c});
    }

    /**
     * Returns a string with one character containing the line continuation
     * character that is currently set - or null if none is set.
     * 
     * @return A one-char long string containing the line cont. char, or
     *         <code>null</code> if none is set.
     */
    public String getLineContinuationCharacter() {
        return m_lineContChar;
    }

    /**
     * if set true multiple different (but consecutive) delimiters are combined,
     * that is ignored (unless they are supposed to be returned).
     * 
     * @param value set true to combine multiple different consecutive
     *            delimiters, of false to handle each as seperate delimiter.
     */
    public void setCombineMultipleDelimiters(final boolean value) {
        m_combineMultiple = value;
    }

    /**
     * @return true if multiple consecutive (but different) delimiters are
     *         combined as one - or treated each as separate delimiter.
     */
    public boolean getCombineMultipleDelimiters() {
        return m_combineMultiple;
    }

    /**
     * @return a new vector, with items of type <code>Comment</code>,
     *         containing all currently defined comment patterns. Could be
     *         emtpy, but never null. The vector is your's if you want it to
     *         change.
     * @see Comment
     */
    public Vector<Comment> getAllComments() {
        return new Vector<Comment>(m_commentPatterns);
    }

    /**
     * @return a new vector, with items of type <code>Quote</code>,
     *         containing all currently defined quote patterns. Could be emtpy,
     *         but never null. The vector is your's if you want it to change.
     * @see Quote
     */
    public Vector<Quote> getAllQuotes() {
        return new Vector<Quote>(m_quotePatterns);
    }

    /**
     * @return a new vector, with items of type <code>Delimiter</code>,
     *         containing all currently defined delimiter patterns. Could be
     *         emtpy, but never null. The vector is your's if you want it to
     *         change.
     * @see Delimiter
     */
    public Vector<Delimiter> getAllDelimiters() {
        return new Vector<Delimiter>(m_delimPatterns);
    }

    /**
     * @return a new vector of strings, all of length one, containing the
     *         characters handled and ignored as whitespaces. The vector is
     *         your's.
     */
    public Vector<String> getAllWhiteSpaces() {
        return new Vector<String>(m_whitespaces);
    }

    /*
     * ----- package internal function for the file tokenizer
     */

    /**
     * sets comment objects to the settings structure. No consistency checks
     * will be performed.
     * 
     * @param comments a Vector of Comment objects to add. Must not be null.
     */
    void setComments(final Vector<Comment> comments) {
        assert comments != null;
        m_commentPatterns.clear();
        m_commentPatterns.addAll(comments);
    }

    /**
     * sets quote objects to the settings structure. No consistency checks will
     * be performed.
     * 
     * @param quotes a vector of Quote objects to add. Must not be null.
     */
    void setQuotes(final Vector<Quote> quotes) {
        assert quotes != null;
        m_quotePatterns.clear();
        m_quotePatterns.addAll(quotes);
    }

    /**
     * sets delimiter objects to the settings structure. No consitency checks
     * will be performed.
     * 
     * @param delimiters a Vector of delimiter objects to add. Must not be null.
     */
    void setDelimiters(final Vector<Delimiter> delimiters) {
        assert delimiters != null;
        m_delimPatterns.clear();
        m_delimPatterns.addAll(delimiters);
    }

    /**
     * sets whitespaces to the settings structure. No consistency checks will be
     * performed. Existing whitespaces will be cleared before.
     * 
     * @param whites a Vector of one-character strings to set. Must not be null.
     */
    void setWhiteSpaces(final Vector<String> whites) {
        assert whites != null;
        m_whitespaces.clear();
        m_whitespaces.addAll(whites);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        // print the delimiters
        result.append("Delimiters:\n");
        for (Iterator<Delimiter> dIter = getAllDelimiters().iterator(); dIter
                .hasNext();) {
            Delimiter delim = dIter.next();
            assert delim != null;
            result.append("    " + delim.toString());
            result.append("\n");
        }
        // append the comment pattern definitions
        result.append("Comments:\n");
        for (Iterator<Comment> cIter = getAllComments().iterator(); cIter
                .hasNext();) {
            Comment comment = cIter.next();
            assert comment != null;
            result.append("    " + comment.toString());
            result.append("\n");
        }
        // at last add the quote definitions
        result.append("Quotes:\n");
        for (Iterator<Quote> qIter = getAllQuotes().iterator(); 
                qIter.hasNext();) {
            Quote quote = qIter.next();
            assert quote != null;
            result.append("    " + quote.toString());
            result.append("\n");
        }
        // append the whitespace characters
        if (m_whitespaces.size() > 0) {
            for (String ws : getAllWhiteSpaces()) {
                result.append("Whitespace: '");
                result.append(printableStr(ws));
                result.append("'\n");
            }
        } else {
            result.append("Whitespaces won't be ignored.\n");
        }

        // add the line continuation character - if set.
        String lcc = getLineContinuationCharacter();
        if (lcc != null) {
            result.append("LineContChar: '");
            result.append(lcc);
            result.append("'\n");
        }
        // and the combine mult delims flag
        result.append("Multiple different delimiters ");
        if (m_combineMultiple) {
            result.append("WILL ");
        } else {
            result.append("will NOT ");
        }
        result.append("be combined.");
        return result.toString();
    }

    /**
     * Checks the completeness and consistency of all settings and adds
     * informational messages, warnings, and errors, if something is suspicious.
     * 
     * @param status an object this methods adds its messages to.
     */

    protected void addStatusOfSettings(final SettingsStatus status) {

        // check the delimiter patterns defined
        for (int d = 0; d < m_delimPatterns.size(); d++) {
            try {
                checkDelimiterPattern(m_delimPatterns.get(d));
            } catch (IllegalArgumentException iae) {
                status.addError("Delimiter[" + d + "]: " + iae.getMessage());
            }
        }

        // check all quote patterns defined
        for (int q = 0; q < m_quotePatterns.size(); q++) {
            try {
                checkQuotePattern(m_quotePatterns.get(q));
            } catch (IllegalArgumentException iae) {
                status.addError("Quote[" + q + "]: " + iae.getMessage());
            }
        }

        // check for invalid comment patterns defined
        for (int c = 0; c < m_commentPatterns.size(); c++) {
            try {
                checkCommentPattern(m_commentPatterns.get(c));
            } catch (IllegalArgumentException iae) {
                status.addError("Comment[" + c + "]: " + iae.getMessage());
            }
        }

        // check for invalid whitespaces (i.e. more than one char long)
        for (String ws : m_whitespaces) {
            if (ws == null) {
                status.addError("Whitespace: Null-String specified");
            } else {
                if (ws.length() != 1) {
                    status.addError("Whitespace[" + ws + "]: Should be of "
                            + "length one.");
                }
            }
        }

    }

    /**
     * Method to check consistency and completeness of the current settings. It
     * will return a <code>SettingsStatus</code> object which contains info,
     * warning and error messages, if something is fishy with the settings.
     * 
     * @return a SettingsStatus object containing info, warning and error
     *         messages - or not if all settings are good.
     */
    public SettingsStatus getStatusOfSettings() {

        SettingsStatus status = new SettingsStatus();

        addStatusOfSettings(status);

        return status;
    }

    /**
     * takes a string that could contain "\t", or "\n", or "\\", and returns a
     * corresponding string with these patterns replaced by the characters '\t',
     * '\n', '\'.
     * 
     * @param str a string with escape sequences in
     * @return a string with all sequences translated. If there are no esc
     *         sequences in the specified string the exact same reference will
     *         be returned.
     */
    public static String unescapeString(final String str) {
        // Garbage in garbage out:
        if (str == null) {
            return null;
        }

        if (str.indexOf('\\') == -1) {
            return str;
        }
        StringBuffer result = new StringBuffer();
        String pattern = str;

        int idx;
        while ((idx = pattern.indexOf('\\')) >= 0) {
            if (idx > 0) {
                // copy everything up to the escape character
                result.append(pattern.substring(0, idx - 1));
            }
            if (idx == pattern.length() - 1) {
                // they had a backslash at the end
                result.append('\\');
                pattern = "";
            } else {
                char c = pattern.charAt(idx + 1);
                if (c == 't') {
                    result.append('\t');
                } else if (c == 'n') {
                    result.append('\n');
                } else {
                    result.append(c);
                }
                if (idx < pattern.length() - 2) {
                    pattern = pattern.substring(idx + 2);
                } else {
                    pattern = "";
                }
            }
        }
        return result.toString();

    }

    /**
     * @param str a string. Could be null.
     * @return a printable string with all control chars replaced.
     */
    public static String printableStr(final String str) {
        if (str == null) {
            return "<null>";
        }
        StringBuffer res = new StringBuffer(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c <= ' ') {
                // it's a ctrl char - translate it into something printable
                if (c == ' ') {
                    res.append("<space>");
                } else if (c == '\t') {
                    res.append("<tab>");
                } else if (c == '\n') {
                    res.append("<lf>");
                } else {
                    res.append("\\0x");
                    res.append(Integer.toHexString(c));
                }
            } else {
                res.append(c);
            }
        }
        return res.toString();
    }

} // FileTokenizerSettings
