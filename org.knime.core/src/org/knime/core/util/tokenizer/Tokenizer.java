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
package org.knime.core.util.tokenizer;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/**
 * This class reads tokens from a stream and returns them as strings. <br>
 * You can specify token delimiters, comments and quotes. <br>
 * The tokenizer can be configured to include delimiters or to return them as
 * separate tokens, to discard, include, or return comments, to allow line
 * continuations, and to combine consecutive delimiters.
 * <p>
 * It always returns tokens as strings.
 * <p>
 * It returns <code>null</code> if it read EOF before any other character.
 * (EOF is always a token delimiter.)
 * <p>
 * It will always ignore a '\r' if it immediately is followed by a '\n'.
 * <p>
 * You can set multiple delimiter patterns. <br>
 * You can specify multiple block comment begin/end pair patterns. <br>
 * You can specify multiple line comment begin patterns. <br>
 * And you can specify multiple quote begin/end pair patterns - and an escape
 * character with each pair. <br>
 * A pattern is a (multi or single character) string. <br>
 * The discard/return/include option can be specified for each delimiter and
 * comment pattern separately.
 * <p>
 * You can specify a line continuation character. This character immediately
 * followed by a newline and any space or tab character will be ignored then
 * inside a token or quoted string.
 * <p>
 * You can push back one (the last) token.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class Tokenizer {

    /* the source we read from */
    private final Reader m_source;

    /* the column delimiters we handle */
    private final Vector<Delimiter> m_delimPatterns;

    /* the quotes for strings */
    private final Vector<Quote> m_quotePatterns;

    /* the patterns marking comments */
    private final Vector<Comment> m_commentPatterns;

    /* Vector of white space strings (only one char strings are allowed) */
    private final Vector<String> m_whiteSpaces;

    /* flag indicating combination of different consecutive delimiters */
    private boolean m_combineMultipleDelimiters;

    /**
     * The maximum ASCII code for the first character of patterns (like 
     * delimiter, comment, and quote patterns.
     */
    public static final int MAX_CHAR = 0xFF;

    /*
     * an array for fast type lookup - different types are the following
     * constants
     */
    private int[] m_charType;

    /*
     * Each character can have multiple of the following types indicating that
     * at least one quote begin pattern starts with this char.
     */
    private static final int QUOTE = 1;

    /*
     * At least one token delimiter pattern starts with this char.
     */
    private static final int DELIM = 2;

    /*
     * At least one comment begin pattern starts with this char.
     */
    private static final int COMMENT = 4;

    /*
     * This char is the line continuation character.
     */
    private static final int LINECONT = 8;

    /*
     * If this bit is set, the character is a whitespace
     */
    private static final int WSCHAR = 16;

    /*
     * the buffer must be at least as long as the longest
     * (quote/delimiter/...)pattern
     */
    private static final int BUFFER_LENGTH = 31;

    /* our read buffer, realized as ring buffer with the following pointers */
    private int[] m_readBuffer;

    /* the current index in the read buffer */
    private int m_currIdx;

    /* end-of-buffer: the last index in the read (ring) buffer */
    private int m_eobIdx;

    /* we build the token in here */
    private StringBuffer m_newToken;

    /* the token returned by the last call to next() */
    private String m_lastToken;
    
    /* flag to remember which quotes we've seen with the last token */
    private Quote m_lastQuotes;

    /* flag to remember if the last token was pushed back */
    private boolean m_pushedBack;

    /* if set, this is going to be the next token */
    private String m_lastDelimiter;

    /* the current line number (not accurate if token got pushed back) */
    private int m_lineNo;
    
    /* the number of bytes read so far */
    private long m_readBytes;

    /* dont change settings after reading rom the tokenizer */
    private boolean m_settingsLocked;

    /* the carriage return character */
    private static final char CR = '\r';

    /* the line feed (new line) character (ends line comments) */
    private static final char LF = '\n';

    /** String containing only the LF char. Used internally for line comment */
    static final String LF_STR = new String(new char[] {LF});

    /* the end-of-file flag */
    private static final int EOF = -1;
    
    /* the flag which identifies if the last token is a delimiter or not */
    private boolean m_tokenWasDelimiter = false;

    /**
     * Creates a new tokenizer with the default behaviour.
     * 
     * @param source A reader the tokens are read from.
     * 
     * @see #resetToDefault() for what's the default behaviour.
     */
    public Tokenizer(final Reader source) {

        m_source = source;
        m_readBuffer = new int[BUFFER_LENGTH];
        m_currIdx = 0;
        m_eobIdx = 0;

        m_lineNo = 1;
        m_readBytes = 0;

        m_charType = new int[MAX_CHAR + 1];

        m_delimPatterns = new Vector<Delimiter>();
        m_quotePatterns = new Vector<Quote>();
        m_commentPatterns = new Vector<Comment>();
        m_whiteSpaces = new Vector<String>();

        m_combineMultipleDelimiters = false;

        m_newToken = new StringBuffer();
        m_lastToken = null;
        m_pushedBack = false;
        m_lastQuotes = null;
        m_lastDelimiter = null;
        m_settingsLocked = false;

        resetToDefault();
    }

    /**
     * Resets the tokenizer to its default behavior, which is:
     * <ul>
     * <li>No comments are supported,
     * <li>No quoted strings are supported,
     * <li>No line continuation is supported, and
     * <li>No token delimiter is set (only EOF - i.e. the entire file will be
     * returned as token if you don't change the settings!).
     * </ul>
     * It does not reset the line number, internal read buffers or the stream.
     */
    public void resetToDefault() {

        if (m_settingsLocked) {
            throw new IllegalStateException("Don't change tokenizer settings"
                    + " after reading from it.");
        }
        // let all characters be of type 'normal'.
        for (int i = 0; i <= MAX_CHAR; i++) {
            m_charType[i] = 0;
        }
        /* blow all delimiters, comments and quotes away */
        m_delimPatterns.clear();
        m_quotePatterns.clear();
        m_commentPatterns.clear();
        m_whiteSpaces.clear();

        m_combineMultipleDelimiters = false;
    }

    /**
     * @return Returns true if last token is a delimiter token, 
     * otherwise false.
     */
    public boolean lastTokenWasDelimiter() {
        return m_tokenWasDelimiter;
    }
    
    /**
     * Reads the next token from the stream and returns it as string. Or
     * <code>null</code> if no more token can be read.
     * 
     * @return The next token from the stream or null at the EOF.
     * @throws TokenizerException if something goes wrong during tokenizing.
     */
    public String nextToken() throws TokenizerException {

        m_settingsLocked = true;

        if (m_pushedBack) {
            // if the last token got pushed back just return it again.
            m_pushedBack = false;
            return m_lastToken;
        }
        if (m_lastDelimiter != null) {
            // if the last delmiter we read must be returned as token, we do so.
            String tmp = m_lastDelimiter;
            m_lastDelimiter = null;
            m_lastToken = tmp;
            m_tokenWasDelimiter = true;
            return tmp;
        }

        m_lastToken = null;
        m_newToken.setLength(0);
        m_lastQuotes = null;
        m_tokenWasDelimiter = false;
        
        int lastEndQuoteIdx = -1; // the idx of the end quote last seen or added
        int c = getNextChar();
        while (c != EOF) {
            int ctype = 0;
            if (ctype <= MAX_CHAR) {
                ctype = m_charType[c & MAX_CHAR];
            }
            if (ctype == 0) {
                // it's an ordinary character - just add it to the result
                m_newToken.append((char)c);
                c = getNextChar();
                continue;
            }
            // first check if it's a line continuation - and if not go on...
            if ((ctype & LINECONT) != 0) {
                int tmp = c;
                c = getNextChar();
                if (c == LF) {
                    c = getNextChar();
                    continue;
                } else {
                    putBackChar(c);
                    c = tmp;
                    // continue handling the character in c
                }
            }
            if ((ctype & COMMENT) != 0) {
                Comment commentPattern;
                // this character COULD start a comment. There is at least one
                // CommentBegin pattern that starts with this character.
                putBackChar(c);
                if ((commentPattern = isCommentPattern()) != null) {
                    // a comment started here.
                    if (commentPattern.returnAsSeparateToken()) {
                        if (m_newToken.length() == 0) {
                            m_newToken.append(readComment(commentPattern));
                        } // if there are already chars in the token, we just
                        // close it and read the comment next time around.
                        cutOffWhiteSpaces(m_newToken, lastEndQuoteIdx);
                        break; // done with that token.
                    } else if (commentPattern.includeInToken()) {
                        m_newToken.append(readComment(commentPattern));
                    } else {
                        // otherwise discard the comment.
                        readComment(commentPattern);
                    }
                    c = getNextChar();
                    continue;

                } else {
                    // the pattern didn't match any comment begin pattern:
                    c = getNextChar(); // we've pushed it back, remember?
                    assert (m_charType[c & MAX_CHAR] & COMMENT) != 0;
                    // continue handling that character...
                }
            }
            if ((ctype & DELIM) != 0) {
                Delimiter delimPattern;
                // it could start a token delimiter.
                putBackChar(c);
                if ((delimPattern = isDelimiterPattern()) != null) {
                    // readDelimiter will read as many delimiters as necessary
                    // will save them and/or return them to include into the
                    // current token.
                    String delim = readDelimiter(delimPattern);
                    // before we add the delimiter to the token, see if we
                    // need to strip off any whitespaces at the end.
                    cutOffWhiteSpaces(m_newToken, lastEndQuoteIdx);
                    m_newToken.append(delim);
                    // the token is complete after reading a delimiter.
                    break;
                } else {
                    // it is not a delimiter - go on examining the character
                    c = getNextChar(); // we've pushed it back before
                    assert (m_charType[c & MAX_CHAR] & DELIM) != 0;
                }
            }
            if ((ctype & QUOTE) != 0) {
                // this character COULD start a quoted string. There is at least
                // one QuoteBegin pattern that starts with this character.
                Quote quotePattern; // the begin/end pattern and esc char
                putBackChar(c);
                if ((quotePattern = isQuotePattern()) != null) {
                    // a quoted string actually started here.
                    try {
                        m_newToken.append(readQuotedString(quotePattern));
                    } catch (TokenizerException fte) {
                        // seems we are missing the closing quotes...
                        m_lastDelimiter = null;
                        m_lastQuotes = null;
                        throw fte;
                    }
                    lastEndQuoteIdx = m_newToken.length() - 1;
                    m_lastQuotes = quotePattern;
                    c = getNextChar();
                    continue;
                } else {
                    // not a quote - go on with that character in c
                    c = getNextChar(); // we've pushed it back before
                    assert (m_charType[c & MAX_CHAR] & QUOTE) != 0;
                }
            }
            if ((ctype & WSCHAR) != 0) {
                // at last: see if its a whitespace character. They will be
                // ignored here at the beginning of the token and where we
                // detect token delimiters, too.
                if (isWhiteSpace((char)c) && (m_newToken.length() == 0)) {
                    c = getNextChar();
                    continue;
                }
            }
            // the character had a special type but it actually was
            // not special: just add it to the token
            m_newToken.append((char)c);
            c = getNextChar();
        } // end of while (c != EOF)

        if (c == EOF) {
            try {
                m_source.close();
            } catch (IOException ioe) {
                // empty.
            }     
            // also strip off whitespaces if the last token ended through EOF
            cutOffWhiteSpaces(m_newToken, lastEndQuoteIdx);
        }
        if ((c == EOF) && (m_newToken.length() == 0)) {
            m_lastToken = null;
        } else {
            m_lastToken = m_newToken.toString();
        }
        return m_lastToken;
    } // nextToken()

    /*
     * Reads the next character either from the readBuffer or the stream. <p> A
     * CR character immediately followed by a LF character will be ignored and
     * only the LF will be returned (only if read from the stream!).
     * 
     * @return The next character. Or -1 if EOF was seen.
     */
    private int getNextChar() {
        try {
            // m_eobIdx("EndOfBufferIndex")points to the last char in the buffer
            // m_currIdx points to the last char read from the buffer
            if (m_eobIdx == m_currIdx) {
                // we need to read a new character from the stream
                if ((m_readBuffer[m_currIdx] = m_source.read()) == -1) {
                    // seen the EOF. Any further read will cause IOException.
                    m_source.close();
                } 
                m_readBytes++;
                if (m_readBuffer[m_currIdx] == CR) {
                    // read the next char to see if we need to swallow the CR
                    m_eobIdx = (m_eobIdx + 1) % BUFFER_LENGTH;
                    if ((m_readBuffer[m_eobIdx] = m_source.read()) == LF) {
                        m_currIdx = m_eobIdx;
                        // incr currIdx as well, which makes them equal again...
                    }
                    m_readBytes++;
                }
            } else {
                // take the next character from the buffer
                m_currIdx = (m_currIdx + 1) % BUFFER_LENGTH;
            }
            if (m_readBuffer[m_currIdx] == LF) {
                m_lineNo++;
            }
            return m_readBuffer[m_currIdx];
        } catch (IOException ioe) {
            return -1;
        }
    }

    /*
     * Stores the character in c in the read buffer. The next call to <code>
     * getNextChar() </code> will return it then. Can be called several times -
     * but be aware of a buffer overrun! (If a CR+LF is pushed back, the CR
     * won't be ignored, bytheway.) (This function is different from the public
     * <code> pushBack() </code> method in that it pushs back one character into
     * the internal read buffer, while the <code> pushBack() </code> function
     * pushes an entire token back into the stream.) @param c The character to
     * push back. You can push back a different character than the one you just
     * read - if you want. If you push back more LFs than you've read you end up
     * with negative line numbers.
     */
    private void putBackChar(final int c) {
        // we need the parameter c because the getNextChar function doesn't
        // incrementally and continously store the characters in the buffer.
        // If getNextChar is called several times, the characters get stored
        // at the same buffer index, overwriting each other.
        m_readBuffer[m_currIdx] = c;
        m_currIdx--;
        if (m_currIdx < 0) {
            m_currIdx += BUFFER_LENGTH;
        }
        if (c == LF) {
            m_lineNo--;
        }
        assert m_currIdx != m_eobIdx : "TokenizerPutBack: Buffer overrun!";
    }

    /*
     * Discards all characters (possibly) stored (pushed back) in the buffer and
     * causes the next character to be read from the stream.
     */
    private void clearReadBuffer() {
        m_currIdx = m_eobIdx;
    }
    
    /*
     * This function reads from the stream (or buffer) as long as it gets spaces
     * or tabs. It returns the number of chars read. @return The number of
     * characters swallowed.
     */
    private int readWhiteSpaces() {
        int count = 0;
        int c = getNextChar();
        while (isWhiteSpace((char)c)) {
            c = getNextChar();
            count++;
        } // we shouldn't have read this character... so: push it back.
        putBackChar(c);
        return count;
    }

    /**
     * @param c the character to test.
     * @return true if the specified character c is in the user defined Vector
     *         of whitespaces.
     */
    public boolean isWhiteSpace(final char c) {
        for (int w = 0; w < m_whiteSpaces.size(); w++) {
            if (m_whiteSpaces.get(w).charAt(0) == c) {
                return true;
            }
        }
        return false;
    }

    /**            
     * strips off whitespaces from the end of the string (not from the 
     * beginning!). It will not change anything in the string before or at the 
     * specified index (this is for leaving quoted parts untouched).
     * 
     * @param str the stringbuffer to modify
     * @param index the lowest index we may modify 
     */
    private void cutOffWhiteSpaces(final StringBuffer str, final int index) {
        
        if (str.length() == 0) {
            return;
        }
        int stopIdx = index;
        if (stopIdx < -1) {
            stopIdx = -1;
        }
        
        int cIdx;
        for (cIdx = str.length() - 1; cIdx > index; cIdx--) {
            if (!isWhiteSpace(str.charAt(cIdx))) {
                break;
            }
        }
        // cIdx points to the first char in the string not a WS
        if (cIdx < str.length() - 1) {
            // remove everything til the end
            str.delete(cIdx + 1, str.length());
        }
    }
    
    /*
     * Checks if the next characters in the stream are a "comment begin"
     * pattern, without modifying the stream. It reads character by character
     * and compares each Comment in the m_commentPatterns vector with the
     * pattern read so far. If it finds a matching pattern it returns the
     * corresponding Comment object. (That is the reason why no begin pattern
     * can be a prefix of any other begin pattern.) @return After its work is
     * done it pushes back all characters read so far. It returns <code> null
     * </code> if no matching pattern exists.
     */
    private Comment isCommentPattern() {
        String[] patterns = new String[m_commentPatterns.size()];
        for (int i = 0; i < patterns.length; i++) {
            patterns[i] = m_commentPatterns.get(i).getBegin();
        }

        int index = matchPattern(patterns);
        if (index >= 0) {
            return m_commentPatterns.get(index);
        } else {
            return null;
        }
    }

    /*
     * @see #isCommentPattern
     */
    private Delimiter isDelimiterPattern() {
        String[] patterns = new String[m_delimPatterns.size()];
        for (int i = 0; i < patterns.length; i++) {
            patterns[i] = m_delimPatterns.get(i).getDelimiter();
        }

        int index = matchPattern(patterns);
        if (index >= 0) {
            return m_delimPatterns.get(index);
        } else {
            return null;
        }

    }

    /*
     * @see #isCommentPattern
     */
    private Quote isQuotePattern() {
        String[] patterns = new String[m_quotePatterns.size()];
        for (int i = 0; i < patterns.length; i++) {
            patterns[i] = m_quotePatterns.get(i).getLeft();
        }

        int index = matchPattern(patterns);
        if (index >= 0) {
            return m_quotePatterns.get(index);
        } else {
            return null;
        }
    }

    /*
     * Given an array of Strings it trys to read from the stream until it can
     * match one of the patterns in the array. It returns the index of the match
     * in the array, or -1 if it didn't find a match. It pushes back all
     * characters it reads from the stream. @param patterns An array with
     * strings to match against. @return The index of the match, or -1 if the
     * next characters in the stream do not match any of the patterns in the
     * array. NOTE: for the sake of performance the function modifies the
     * contents of the array.
     */
    private int matchPattern(final String[] patterns) {
        int possibleMatches = patterns.length;
        int charPos;
        int nextChar;
        StringBuffer buffer = new StringBuffer();
        int result;
        /*
         * Here is what we do: We read one char after each other. With each new
         * character we loop through all patterns and compare it with the
         * character at that position in the pattern. If the chars dont match
         * there is no need to compare that string with any new characters - we
         * set the reference in the array to null. If all references are null
         * (which we count in 'possibleMatches'), we can stop. Also, if we
         * successfully compared the last character in a pattern we can safely
         * assume that we found a match and return the index of that pattern.
         */
        charPos = 0;
        result = -1;
        while ((possibleMatches > 0) && (result == -1)) {

            nextChar = getNextChar();
            if (nextChar == EOF) {
                break;
            }

            // store it, to write it back at the end.
            buffer.append((char)nextChar);

            for (int index = 0; index < patterns.length; index++) {
                if (patterns[index] != null) {
                    if (patterns[index].charAt(charPos) == nextChar) {
                        if (patterns[index].length() == (charPos + 1)) {
                            result = index;
                            break;
                        }
                    } else {
                        // this char in the pattern doesn't match. Remove it
                        // from the list of possible matches
                        patterns[index] = null;
                        possibleMatches--;
                    }
                }
            } // end of for

            charPos++;

        } // end of while

        // we are supposed to not read any characters from stream: push'em back.
        for (int i = buffer.length(); i > 0;) {
            putBackChar(buffer.charAt(--i));
        }

        return result;
    } // matchPattern(String[])

    /*
     * Reads the comment from the stream (or buffer) and returns it - including
     * the begin/end pattern - in a string. It assumes that the first thing in
     * the stream is the begin pattern! It will read in everything until it sees
     * the comment end pattern, or EOF. It will then return the characters it
     * read in a string. @param comment The object describing the comment
     * begin/end patterns. @return A string containing the comment. Incl. the
     * begin/end patterns. NOTE: The next characters in the stream MUST be the
     * comment begin pattern. Otherwise an assertion will go off. <br>
     */
    private String readComment(final Comment comment) {
        StringBuffer result = new StringBuffer();
        String endPattern = comment.getEnd();
        int nextChar;
        int patternLength;
        int endPatternIdx;
        int searchIdx;

        // the first characters MUST match the comment begin pattern!
        patternLength = comment.getBegin().length();
        for (int i = 0; i < patternLength; i++) {
            nextChar = getNextChar();
            if ((nextChar == EOF) 
                    || (nextChar != comment.getBegin().charAt(i))) {
                assert false : "Call only with a comment begin in the stream";
                return "";
            }
        }
        result.append(comment.getBegin());

        // end pattern idx always points to result.length()-endPattern.length()
        endPatternIdx = result.length() - endPattern.length();
        // the index where we start searching in the result for the endPattern.
        // This is to handle equal begin and end comment patterns.
        searchIdx = result.length();

        // now read on until we see the end pattern
        boolean endPatternRead = false;
        while (!endPatternRead) {
            nextChar = getNextChar();
            if (nextChar == EOF) {
                break;
            }
            result.append((char)nextChar);

            endPatternRead = (result.indexOf(endPattern, endPatternIdx) > -1);
            endPatternIdx++;
            if (searchIdx < endPatternIdx) {
                searchIdx = endPatternIdx;
            }
        }

        // Line comments have '\n' as end pattern which is not really part of
        // the comment and should stay in the stream
        if (endPatternRead && endPattern.equals(LF_STR)) {
            putBackChar(LF);
            // remove this LF (the last char) from the result
            result.delete(result.length() - LF_STR.length() - 1, result
                    .length() - 1);
        }

        /* Now, if the token so far is empty, we have read in only comment.
         * If the next character in the stream is a LF, we should consider it 
         * part of the comment, as is was added only for better readability. 
         * (Otherwise this LF causes an unexpected empty line most of the time.)
         */
        if (m_newToken.length() == 0) {
            int c = getNextChar();
            if (c != LF) {
                putBackChar(c);
            }
        }
        
        return result.toString();
    } // readComment(string)

    /*
     * Reads the delimiter specified in delim from the stream (or buffer).
     * Depending on the 'combineConsecutiveDelims' setting, it will also eat and
     * swallow all immediately following delimiters of the same kind. @param
     * delim An object describing the delimiter to read. @returns an empty
     * string if it only read delimiters to ignore (not to be returned as token)
     * or the first delimiter that must be returned in the sequence of
     * delimiters it read.
     */
    private String readDelimiter(final Delimiter delim) {
        int delimLength = delim.getDelimiter().length();
        int nextChar;

        if (!m_combineMultipleDelimiters) {

            for (int i = 0; i < delimLength; i++) {
                nextChar = getNextChar();
                if ((nextChar == EOF)
                        || (nextChar != delim.getDelimiter().charAt(i))) {
                    assert false : "Call only with a delimiter in the stream";
                    return "";
                }
            }

            // if we are not supposed to combine different delims - we still
            // need to check if we should combine delims of this kind
            if (delim.combineConsecutiveDelims()) {
                StringBuffer buffer = new StringBuffer();
                int index = 0;
                while ((nextChar = getNextChar()) != EOF) {

                    // store it in case it must go back...
                    buffer.append((char)nextChar);
                    if (nextChar != delim.getDelimiter().charAt(index)) {
                        break;
                    }
                    index = (index + 1) % delimLength;
                    if (index == 0) {
                        // we've read an entire delimiter - clear out the buffer
                        buffer.setLength(0);
                    }
                }

                for (int i = buffer.length(); i > 0;) {
                    // write back what turned out not to be a delimiter
                    putBackChar(buffer.charAt(--i));
                }
            }

            assert m_lastDelimiter == null;

            if (delim.returnAsToken()) {
                // store it to return it with the next call to 'nextToken()' 
                m_lastDelimiter = delim.getDelimiter();
                return "";
            } else {
                if (delim.includeInToken()) {
                    return delim.getDelimiter();
                } else {
                    return "";
                }
            }

        } else {
            // throw away this and all immediately following delimiters - of
            // any kind, unless we are supposed to return them as token.

            /*
             * this is how we do it: we eat up all delimiters - until we see one
             * for the second time that shouldn't be combined - or until we see
             * another delimiter that we should return as token.
             */

            // remember the ones seen, which should not be combined (of
            // same kind)
            Delimiter d = delim;
            Vector<Delimiter> uncombDelimsRead = new Vector<Delimiter>();
            Delimiter returnDel = null; // the one that must be returned

            while (d != null) {
                if (d.returnAsToken() || d.includeInToken()) {
                    if ((returnDel != null) && (!returnDel.equals(d))) {
                        // we already have one to return. Stop now.
                        break;
                    }
                    returnDel = d;
                }
                if (!d.combineConsecutiveDelims()) {
                    if (uncombDelimsRead.contains(d)) {
                        // we've seen this already and shouldn't combine
                        // multiple of this kind. Done.
                        break;
                    }
                    uncombDelimsRead.add(d);
                }
                // now we can swallow the delimiter
                for (int i = 0; i < d.getDelimiter().length(); i++) {
                    nextChar = getNextChar();
                }

                d = isDelimiterPattern();
            } // while (d != null)

            if (returnDel != null) {
                if (returnDel.includeInToken()) {
                    return returnDel.getDelimiter();
                } else {
                    assert returnDel.returnAsToken();
                        // we must store it for the next call to 'nextToken()'
                    m_lastDelimiter = returnDel.getDelimiter();
                    return "";
                }
            } else {
                return "";
            }
        }

    } // readDelimiter(Delimiter)

    /*
     * Reads the quote begin pattern from the stream, includes then any char it
     * gets in the result until is sees the end quote pattern (which is not
     * included in the result). The first characters in the stream MUST be the
     * quote begin pattern. An EOF ends a quoted string. A newline character
     * does not. @param quote An object defining the quote patterns to come.
     * @return The characters from the stream that were read between the quote
     * begin and quote end pattern.
     */
    private String readQuotedString(final Quote quote) 
            throws TokenizerException {
        StringBuilder result = new StringBuilder();
        int patternLength;
        String endPattern = quote.getRight();
        int nextChar = 0;
        char escChar = quote.getEscape();
        int endPatternIdx; // the idx the end pattern could start earliest.
        int searchIdx;
        // we need this, as the string could contain the
        // end pattern as escaped character.

        // first read the begin quote pattern - and discard it.
        patternLength = quote.getLeft().length();
        for (int i = 0; i < patternLength; i++) {
            nextChar = getNextChar();
            if ((nextChar == EOF) || (nextChar != quote.getLeft().charAt(i))) {
                assert false : "Call only with a quote begin in the stream";
                return "";
            }
        }
        
        // end pattern idx always points to result.length()-endPattern.length()
        endPatternIdx = -endPattern.length();
        // the index where we start searching in the result for the endPattern
        // sometimes we have to push this ahead.
        searchIdx = 0;

        // now read on until we see the end pattern
        while (result.indexOf(endPattern, searchIdx) == -1) {
            nextChar = getNextChar();
            if (nextChar == EOF) {
                break;
            }
            if ((nextChar <= MAX_CHAR) 
                && ((m_charType[nextChar] & LINECONT) != 0)) {
                // we support line continuations within quoted strings.
                int tmp = getNextChar();
                if (tmp == LF) {
                    readWhiteSpaces();
                    // but no line continuation in end patterns
                    searchIdx = result.length();
                    continue;
                } else {
                    putBackChar(tmp);
                }
            }
            if (nextChar == LF) {
                // read a LF within quotes: that is illegal!
                throw new TokenizerException("New line in quoted string"
                         + " (or closing quote missing).");
            }
            if ((nextChar == escChar) && quote.hasEscapeChar()) {
                nextChar = translateEscChar(nextChar);
                // the escaped char could be the end pattern. Start searching
                // for the endpattern at the end of the current result now.
                searchIdx = result.length() + 1;
            }
            result.append((char)nextChar);
            endPatternIdx++;
            if (searchIdx < endPatternIdx) {
                searchIdx = endPatternIdx;
            }
        }

        // add the quote patterns, if they should stay in the token
        if (quote.getDontRemoveFlag()) {
            result.insert(0, quote.getLeft());
        }

        if (!quote.getDontRemoveFlag() && (nextChar != EOF)) {
            // remove the end pattern from the token 
            assert result.indexOf(endPattern, endPatternIdx) > -1;
            return result.substring(0, result.length()
                    - quote.getRight().length());
        } else {
            return result.toString();
        }
    } // readQuotedString(Quote)

    /*
     * It assumes that the last character read from the stream is the escape
     * character. It will read the next character(s), translate them into the
     * intended character and return it. The following translations are
     * supported: <ul><li> n -> '\n' <li> t -> '\t' <li> any -> any (i.e. any
     * other character will translated into itself) <li> EOF -> escChar @param
     * escChar The character switching into the escape mode. @return The
     * character that is supposed to replace the escape sequence.
     */
    private int translateEscChar(final int escChar) {
        int nextChar = getNextChar();
        if (nextChar == EOF) {
            return escChar;
        } else if (nextChar == 't') {
            return '\t';
        } else if (nextChar == 'n') {
            return '\n';
        } else {
            return nextChar;
        }
    }

    /**
     * After a call to this function the token returned with the last call to
     * the <code>nextToken()</code> function will be returned once again with
     * the next call the the <code>nextToken()</code> function. Pushing back a
     * token does <b>not </b> decrease the line number accordingly.
     * 
     * @see #nextToken
     */
    public void pushBack() {
        m_pushedBack = true;
    }

    /**
     * Call this to distinguish between missing and empty tokens. If quote
     * patterns are set, e.g. '"' for beginning and ending quotes, and the
     * delimiter is set for example to a comma, the following line will return
     * five tokens and all of them will be returned as empty strings: "",,"",,""
     * The first, third, and fifth tokens are specified - but empty. The second
     * and fourth are not specified causing an empty token to be returned. With
     * this function you can figure out the difference.
     * 
     * @return <code>true</code> if the last token had quotes which were
     *         removed by the tokenizer.
     */
    public boolean lastTokenWasQuoted() {
        return (m_lastQuotes != null);
    }

    /**
     * Returns the left quote of the last token. Or, if the token contained
     * multiple quoted parts, the left quote of the last part that was quoted.
     * If there were no quotes in the last token, null will be returned. <br>
     * For example, if the tokenized stream contains ...,"foo"poo'loo',... with
     * comma separated tokens and single and double quotes - the last quote
     * begin pattern would be the single quote (').
     * 
     * @return the left quote pattern of the quotes in the last token. Or null
     *         if it wasn't quoted.
     */
    public String getLastQuoteBeginPattern() {
        if (m_lastQuotes == null) {
            return null;
        } else {
            return m_lastQuotes.getLeft();
        }
    }

    /**
     * Returns the right quote of the last token. Or, if the token contained
     * multiple quoted parts, the right quote of the last part that was quoted.
     * If there were no quotes in the last token, null will be returned. <br>
     * For example, if the tokenized stream contains ...,"foo"poo'loo',... with
     * comma separated tokens and single and double quotes - the last quote
     * begin pattern would be the single quote (').
     * 
     * @return the right quote pattern of the quotes in the last token. Or null
     *         if it wasn't quoted.
     */
    public String getLastQuoteEndPattern() {
        if (m_lastQuotes == null) {
            return null;
        } else {
            return m_lastQuotes.getRight();
        }
    }

    /**
     * @return The current line number in the stream.
     */
    public int getLineNumber() {
        return m_lineNo;
    }
    
    /**
     * Returns the number of bytes returned so far. Due to the buffering the
     * number of bytes read from the disk and the number of bytes returned by
     * this tokenizer can differ.
     * 
     * @return the number of bytes returned so far by this tokenizer
     */
    public long getReadBytes() {
        return m_readBytes;
    }

    /**
     * Closes the stream the tokenizer reads from. After the tokenizer read the
     * EOF from the stream it closes it automatically. If it's required to close
     * the stream before the end is read, you can call this method. A call to
     * <code>nextToken()</code> after a call to this token will return 
     * <code>null</code> (indicating the end of the file).
     */
    public void closeSourceStream() {
        // discard any characters pushed back. 
        clearReadBuffer();
        try {
            m_source.close();
        } catch (IOException ioe) {
            // okay, then don't close it.
        }
    }
    
    /**
     * Set new user settings in this tokenizer. The only way to configure this
     * tokenizer is to create an instance of the
     * <code>FileTokenizerSettings</code>, add all parameters there and pass
     * the settings object through this method.
     * 
     * @param ftSettings the settings object containing new settings.
     */
    public void setSettings(final TokenizerSettings ftSettings) {

        if (m_settingsLocked) {
            throw new IllegalStateException("Don't change tokenizer settings"
                    + " after reading from it.");
        }

        // blow the old settings away.
        // let all characters be of type 'normal'.
        for (int i = 0; i <= MAX_CHAR; i++) {
            m_charType[i] = 0;
        }
        /* blow all delimiters, comments and quotes away */
        m_delimPatterns.clear();
        m_quotePatterns.clear();
        m_commentPatterns.clear();
        m_whiteSpaces.clear();

        // Fill our own data structures for comment, quotes, delimiters and
        // line contin. char. Don't forget to set the character type
        // accordingly.
        for (Comment comment : ftSettings.getAllComments()) { 
            assert comment != null;
            m_commentPatterns.add(comment);
            char c = comment.getFirstCharOfBegin();
            m_charType[c] |= COMMENT;
        }
        for (Delimiter delim : ftSettings.getAllDelimiters()) { 
            assert delim != null;
            m_delimPatterns.add(delim);
            char c = delim.getFirstChar();
            m_charType[c] |= DELIM;
        }
        for (Quote quote : ftSettings.getAllQuotes()) {
            assert quote != null;
            m_quotePatterns.add(quote);
            char c = quote.getFirstCharOfLeft();
            m_charType[c] |= QUOTE;
        }
        for (String ws : ftSettings.getAllWhiteSpaces()) {
            assert ws != null;
            assert ws.length() == 1;
            m_whiteSpaces.add(ws);
            m_charType[ws.charAt(0)] |= WSCHAR;
        }
        // finally: the line continuation character.
        String lcc = ftSettings.getLineContinuationCharacter();
        if (lcc != null) {
            assert lcc.length() == 1;
            // set the new one
            m_charType[lcc.charAt(0)] |= LINECONT;
        }

        // not to forget the flag to combine multiple (different) delimiters
        m_combineMultipleDelimiters = ftSettings.getCombineMultipleDelimiters();

    }

    /**
     * @return an object containing the current settings of the tokenizer
     */
    public TokenizerSettings getSettings() {

        TokenizerSettings result = new TokenizerSettings();

        //add all currently set quote patterns
        result.setQuotes(m_quotePatterns);
        // add all comments
        result.setComments(m_commentPatterns);
        // add all delimiter patterns
        result.setDelimiters(m_delimPatterns);
        // add user defined whitespaces
        result.setWhiteSpaces(m_whiteSpaces);

        int lcc = getLineContChar(); // less than zero if not set
        if (lcc >= 0) {
            result.setLineContinuationCharacter((char)lcc);
        }

        result.setCombineMultipleDelimiters(m_combineMultipleDelimiters);

        return result;
    }

    // extracts the line continuation character from the charType array.
    // returns -1 if its not set.
    private int getLineContChar() {
        for (int i = 0; i < m_charType.length; i++) {
            if ((m_charType[i] & LINECONT) != 0) {
                return i;
            }
        }
        return -1;
    }

} // FileTokenizer
