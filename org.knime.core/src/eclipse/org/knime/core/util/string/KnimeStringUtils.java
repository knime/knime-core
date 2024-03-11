/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Oct 19, 2018 (moritz): created
 */
package org.knime.core.util.string;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.knime.core.node.NodeLogger;

/**
 * This is a utility class for string manipulation.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 * @since 3.7
 */
public class KnimeStringUtils {
    private static final String[] UMLAUTS = new String[]{"ä", "Ä", "ö", "Ö", "ü", "Ü", "ß", "\u1e9e"};

    private static final String[] REPLACEMENT_OMIT_E = new String[]{"a", "A", "o", "O", "u", "U", "ss", "Ss"};

    private static final String[] REPLACEMENT_E = new String[]{"ae", "Ae", "oe", "Oe", "ue", "Ue", "ss", "Ss"};

    private static NodeLogger LOGGER = NodeLogger.getLogger(KnimeStringUtils.class);

    private KnimeStringUtils() {
    }

    /**
     * Capitalizes all white space separated words in a string.
     *
     * @param str the string
     * @return the capitalized string
     */
    public static String capitalize(final String str) {
        return WordUtils.capitalizeFully(str);
    }

    /**
     * Capitalizes all delimiter separated words in a string.
     *
     * @param str the string
     * @param delim the delimiter
     * @return the capitalized string
     */
    public static String capitalize(final String str, final String delim) {
        return WordUtils.capitalizeFully(str, delim.toCharArray());
    }

    /**
     * Compares two strings lexicographically.
     *
     * @param str1 the first string
     * @param str2 the second string
     * @return the comparison result
     */
    public static int compare(final String str1, final String str2) {
        String s1 = null != str1 ? str1 : "";
        String s2 = null != str2 ? str2 : "";

        Collator collator = Collator.getInstance();
        return collator.compare(s1, s2);
    }

    /**
     * Count specific characters in the string.
     *
     * @param str the string
     * @param chars the characters to count
     * @return the count
     */
    public static int countChars(final String str, final String chars) {
        return countChars(str, chars, "");
    }

    /**
     * Count specific characters in the string.
     *
     * @param str the string
     * @param chars the characters to count
     * @param modifiers modifiers like ignore case
     * @return the count
     */
    public static int countChars(final String str, final String chars, final String modifiers) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        String c = (null != chars) ? chars : "";
        String opt = (null != modifiers) ? modifiers.toLowerCase(Locale.ENGLISH) : "";
        boolean ignoreCase = StringUtils.contains(opt, 'i');
        boolean matchOpposite = StringUtils.contains(opt, 'v');
        int sum = 0;
        for (int i = 0; i < c.length(); i++) {
            String s = c.substring(i, i + 1);
            if (ignoreCase) {
                // search for lower case and upper case
                String lower = s.toLowerCase();
                sum += StringUtils.countMatches(str, lower);
                String upper = s.toUpperCase();
                if (!lower.equals(upper)) {
                    sum += StringUtils.countMatches(str, upper);
                }
            } else {
                sum += StringUtils.countMatches(str, s);
            }
        }
        return matchOpposite ? str.length() - sum : sum;
    }

    /**
     * Count substrings in the string.
     *
     * @param str the string
     * @param toCount the characters to count
     * @return the count
     */
    public static int count(final String str, final String toCount) {
        return count(str, toCount, "");
    }

    /**
     * Count substrings in the string.
     *
     * @param str the string
     * @param toCount the characters to count
     * @param modifiers modifiers like ignore case
     * @return the count
     */
    public static int count(final String str, final String toCount, final String modifiers) {
        if (str == null || str.isEmpty() || toCount == null || toCount.isEmpty()) {
            return 0;
        }

        int count = 0;
        int index = 0;
        index = indexOf(str, toCount, index, modifiers);
        while (-1 != index) {
            count++;
            index += toCount.length();
            index = indexOf(str, toCount, index, modifiers);
        }
        return count;
    }

    /**
     * Gives the first index of toSearch in the string or -1 if toSearch is not found.
     *
     * @param str the string
     * @param searchChars the characters to search
     * @return the index of the first occurrence of toSearch in s
     */
    public static int indexOfChars(final CharSequence str, final String searchChars) {
        return indexOfChars(str, searchChars, 0);
    }

    /**
     * Gives the occurrence of a character in searchChars in the string or -1 if the characters in searchChars are not
     * found in the string.
     *
     * @param str the string
     * @param searchChars the characters to search
     * @param options string with binary options
     * @return the index of the first occurrence of toSearch in s
     */
    public static int indexOfChars(final CharSequence str, final String searchChars, final String options) {
        String opt = null != options ? options : "";
        boolean backward = StringUtils.contains(opt.toLowerCase(Locale.ENGLISH), 'b');
        int start = backward ? str.length() : 0;
        return indexOfChars(str, searchChars, start, options);
    }

    /**
     * Gives the first index of toSearch in the string or -1 if toSearch is not found.
     *
     * @param str the string
     * @param searchChars the characters to search
     * @param start characters in s before this will be ignored
     * @return the index of the first occurrence of toSearch in s
     */
    public static int indexOfChars(final CharSequence str, final String searchChars, final int start) {
        return indexOfChars(str, searchChars, start, "");
    }

    /**
     * Gives the occurrence of a character in searchChars in the string or -1 if the characters in searchChars are not
     * found in the string.
     *
     * @param str the string
     * @param searchChars the character sequence to search
     * @param start characters in s before this will be ignored
     * @param modifiers string with binary options
     * @return the index of the first occurrence of toSearch in s
     */
    public static int indexOfChars(final CharSequence str, final String searchChars, final int start,
        final String modifiers) {
        if (StringUtils.isEmpty(str) || StringUtils.isEmpty(searchChars)) {
            return StringUtils.INDEX_NOT_FOUND;
        }
        String opt = (null != modifiers) ? modifiers.toLowerCase(Locale.ENGLISH) : "";
        return indexOfAny(str, start, opt, searchChars.toCharArray());
    }

    /**
     * Search for characters in searchChars.
     */
    private static int indexOfAny(final CharSequence cs, final int start, final String modifiers,
        final char... searchChars) {
        boolean backward = StringUtils.contains(modifiers, 'b');

        // forward search
        if (!backward) {
            return indexOfAnyForward(cs, start, modifiers, searchChars);
        } else { // search right to left (backward)
            return indexOfAnyBackward(cs, start, modifiers, searchChars);
        }
    }

    /**
     * Forward search (left to right).
     */
    private static int indexOfAnyForward(final CharSequence cs, final int start, final String modifiers,
        final char... searchChars) {
        boolean ignoreCase = StringUtils.contains(modifiers, 'i');
        boolean matchOpposite = StringUtils.contains(modifiers, 'v');
        int offset = Math.max(start, 0);

        for (int i = offset; i < cs.length(); i++) {
            int c = Character.codePointAt(cs, i);
            if (doesMatch(c, ignoreCase, matchOpposite, searchChars)) {
                return i;
            }
        }
        return StringUtils.INDEX_NOT_FOUND;
    }

    /**
     * Backward search (right to left).
     */
    private static int indexOfAnyBackward(final CharSequence cs, final int start, final String modifiers,
        final char... searchChars) {
        boolean ignoreCase = StringUtils.contains(modifiers, 'i');
        boolean matchOpposite = StringUtils.contains(modifiers, 'v');
        int offset = Math.max(start, 0);
        offset = Math.min(cs.length() - 1, offset);

        for (int i = offset; i >= 0; i--) {
            int c = Character.codePointAt(cs, i);
            if (doesMatch(c, ignoreCase, matchOpposite, searchChars)) {
                return i;
            }

        }
        return StringUtils.INDEX_NOT_FOUND;
    }

    /**
     * If matchOpposite is false the method gives true when c is in the searchChars. If matchOpposite is true the method
     * gives false when c is in the searchChars.
     */
    private static boolean doesMatch(final int c, final boolean ignoreCase, final boolean matchOpposite,
        final char... searchChars) {
        boolean match = false;
        for (int j = 0; j < searchChars.length; j++) {
            int cRef = Character.codePointAt(searchChars, j);
            if (!ignoreCase) { // match case
                if (c == cRef) {
                    match = true;
                }
            } else { // ignore case
                if (Character.toUpperCase(c) == Character.toUpperCase(cRef)) {
                    match = true;
                }
            }
            // c matches cReff && this should considered to be a match
            if (match && !matchOpposite) {
                return true;
            }
        }
        // c is not in searchChars && this case should considered as a
        // match
        if (!match && matchOpposite) {
            return true;
        }
        return false;
    }

    /**
     * Gives the first index of toSearch in the string or -1 if toSearch is not found.
     *
     * @param str the string
     * @param needle the character sequence to search
     * @param options string with binary options
     * @return the index of the first occurrence of needle in s
     */
    public static int indexOf(final CharSequence str, final CharSequence needle, final String options) {
        String opt = null != options ? options : "";
        boolean backward = StringUtils.contains(opt.toLowerCase(Locale.ENGLISH), 'b');
        int start = backward ? str.length() : 0;
        return indexOf(str, needle, start, opt);
    }

    /**
     * Gives the first index of toSearch in the string or -1 if toSearch is not found.
     *
     * @param str the string
     * @param needle the character sequence to search
     * @param start characters in s before this will be ignored
     * @param modifiers string with binary options
     * @return the index of the first occurrence of needle in s
     */
    public static int indexOf(final CharSequence str, final CharSequence needle, final int start,
        final String modifiers) {
        String opt = (null != modifiers) ? modifiers.toLowerCase(Locale.ENGLISH) : "";
        boolean ignoreCase = StringUtils.contains(opt, 'i');
        boolean backward = StringUtils.contains(opt, 'b');
        boolean words = StringUtils.contains(opt, 'w');

        if (ignoreCase) {
            if (backward) {
                // ignore case, backward
                if (words) {
                    int index = StringUtils.lastIndexOfIgnoreCase(str, needle, start);
                    while (!hasWordBoundaries(str, needle, index)) {
                        index = StringUtils.lastIndexOfIgnoreCase(str, needle, index - 1);
                    }
                    return index;
                } else {
                    return StringUtils.lastIndexOfIgnoreCase(str, needle, start);
                }
            } else {
                // ignore case, forward
                if (words) {
                    int index = StringUtils.indexOfIgnoreCase(str, needle, start);
                    while (!hasWordBoundaries(str, needle, index)) {
                        index = StringUtils.indexOfIgnoreCase(str, needle, index + 1);
                    }
                    return index;
                } else {
                    return StringUtils.indexOfIgnoreCase(str, needle, start);
                }
            }
        } else {
            if (backward) {
                // match case, backward
                if (words) {
                    int index = StringUtils.lastIndexOf(str, needle, start);
                    while (!hasWordBoundaries(str, needle, index)) {
                        index = StringUtils.lastIndexOf(str, needle, index - 1);
                    }
                    return index;
                } else {
                    return StringUtils.lastIndexOf(str, needle, start);
                }
            } else {
                // match case, forward
                if (words) {
                    int index = StringUtils.indexOf(str, needle, start);
                    while (!hasWordBoundaries(str, needle, index)) {
                        index = StringUtils.indexOf(str, needle, index + 1);
                    }
                    return index;
                } else {
                    return StringUtils.indexOf(str, needle, start);
                }
            }
        }
    }

    /**
     * Checks if a string contains another string.
     *
     * @param str    The haystack string to search in
     * @param search The needle string to search for
     * @return True iff <code>search</code> appears as a substring in <code>str</code>
     * @since 4.3
     */
    public static boolean contains(String str, String search) {
        return KnimeStringUtils.contains(str, search, "");
    }

    /**
     * Checks if a string contains another string, optionally with word boundaries or ignoring case.
     *
     * @param str       The haystack string to search in
     * @param search    The needle string to search for
     * @param modifiers String of single characters describing search options.
     * @return True iff <code>search</code> appears as a substring in <code>str</code> with respect to
     * the search options given in <code>modifiers</code>
     * @since 4.3
     */
    public static boolean contains(final String str, final String search, final String modifiers) {
        // Technically, the `b` (backwards) modifier is supported here as well,
        // but it does not affect the result.
        return KnimeStringUtils.indexOf(str, search, modifiers) >= 0;
    }

    /**
     * Returns true if needle has word boundaries in str at position index.
     */
    private static boolean hasWordBoundaries(final CharSequence str, final CharSequence needle, final int index) {
        if (-1 == index) {
            return true;
        }
        return isWordBoundary(str, index - 1) && isWordBoundary(str, index + needle.length());
    }

    /**
     * Returns true if the character at index is a white space character or if the index is out of bounds (index < 0 or
     * index > str.length - 1). Gives false in all other cases.
     */
    private static boolean isWordBoundary(final CharSequence str, final int index) {
        if (index < 0 || index > str.length() - 1) {
            return true;
        } else {
            return Character.isWhitespace(str.charAt(index));
        }
    }

    /**
     * Concatenates strings.
     *
     * @param str the strings to concatenate
     * @return the concatenated strings with separator
     */
    public static String join(final String... str) {
        return joinSep(null, str);
    }

    /**
     * Concatenates strings using the given separator.
     *
     * @param sep the separator to use
     * @param str the strings to concatenate
     * @return the concatenated strings with separator
     */
    public static String joinSep(final String sep, final String... str) {
        if (null == str) {
            return null;
        }
        if (str.length > 0) {
            // test for solely null elements
            boolean allNull = true;
            for (int i = 0; i < str.length; i++) {
                if (str[i] != null) {
                    allNull = false;
                    break;
                }
            }
            if (allNull) {
                return null;
            }
        }
        return StringUtils.join(str, sep);
    }

    /**
     * @param str input string (must not be null)
     * @return md5 checksum as string (never null)
     */
    public static String md5Checksum(final String str) {
        try {
            // NOFLUID no specific use-case associated with this instance
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(str.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            String hashtext = bigInt.toString(16);
            // Now we need to zero pad it if we actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("No md5 checksum algorithm.", e);
            return str;
        }
    }

    /**
     * @param str input string (must not be null)
     * @param regex which is matched against str (must not be null)
     * @return String True/False (never null)
     */
    public static String regexMatcher(final String str, final String regex) {
        if (str.matches(regex)) {
            return "True";
        } else {
            return "False";
        }
    }

    /**
     * @param str input string (must not be null)
     * @param regex regex pattern (must not be null)
     * @param replaceStr replacement string (must not be null)
     * @return string with replacements (never null)
     */
    public static String regexReplace(final String str, final String regex, final String replaceStr) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        return m.replaceAll(replaceStr);
    }

    /**
     * Remove all occurrences of given characters from a string.
     *
     * @param str the string
     * @param chars the characters that will be removed
     * @return the processed string
     */
    public static String removeChars(final String str, final String chars) {
        if (null == str) {
            return null;
        }
        if (null == chars || chars.isEmpty()) {
            return str;
        }
        return str.replaceAll("[" + Pattern.quote(chars.toString()) + "]+", "");
    }

    /**
     * @param str input string (must not be null)
     * @return string without diacritics (never null)
     */
    public static String removeDiacritic(final String str) {
        if (str == null) {
            return null;
        }
        // normalize the input string and remove all letters which are part of the diacritic.
        return Normalizer.normalize(str, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
    }

    /**
     * Remove duplicated spaces in a string.
     *
     * @param str the string
     * @return the string without duplicated spaces
     */
    public static String removeDuplicates(final String str) {
        if (null == str) {
            return null;
        }
        return str.replaceAll("[ ]+", " ");
    }

    /**
     * Remove all space characters from a string.
     *
     * @param str the string
     * @return the string without space characters
     */
    public static String removeChars(final String str) {
        if (null == str) {
            return null;
        }
        return str.replaceAll("[ ]+", "");
    }

    /**
     * Replaces all occurrences of a String within another String..
     *
     * @param str the string
     * @param searchChars the search chars
     * @param replaceChars the replacements
     * @return the index of the first occurrence of needle in s
     */
    public static String replaceChars(final String str, final String searchChars, final String replaceChars) {
        return replaceChars(str, searchChars, replaceChars, "");
    }

    /**
     * Replaces all occurrences of a String within another String..
     *
     * @param str the string
     * @param searchChars the search chars
     * @param replaceChars the replacements
     * @param modifiers string with binary options
     * @return the index of the first occurrence of needle in s
     */
    public static String replaceChars(final String str, final String searchChars, final String replaceChars,
        final String modifiers) {
        if (null == str || str.isEmpty() || null == searchChars || searchChars.isEmpty() || replaceChars == null) {
            return str;
        }
        String opt = (null != modifiers) ? modifiers.toLowerCase(Locale.ENGLISH) : "";
        boolean ignoreCase = StringUtils.contains(opt, 'i');
        // create new modifiers string with allowed options
        String mdfrs = "";
        mdfrs = ignoreCase ? opt + "i" : opt;

        int start = 0;
        int end = indexOfChars(str, searchChars, mdfrs);
        if (end == StringUtils.INDEX_NOT_FOUND) {
            return str;
        }
        StringBuilder buf = new StringBuilder();
        while (end != StringUtils.INDEX_NOT_FOUND) {
            buf.append(str.substring(start, end));
            int codePoint = str.codePointAt(end);

            int index = indexOfChar(searchChars, codePoint, ignoreCase);
            if (index < replaceChars.length()) {
                buf.appendCodePoint(replaceChars.codePointAt(index));
            }
            start = end + 1;
            end = indexOfChars(str, searchChars, start, mdfrs);
        }
        buf.append(str.substring(start));
        return buf.toString();
    }

    private static int indexOfChar(final String searchChars, final int codePoint, final boolean ignoreCase) {
        if (!ignoreCase) {
            return searchChars.indexOf(codePoint);
        }
        if (Character.isLowerCase(codePoint)) {
            // search for lower case, first
            int index = searchChars.indexOf(Character.toLowerCase(codePoint));
            return index >= 0 ? index : searchChars.indexOf(Character.toUpperCase(codePoint));
        } else {
            // search for upper case, first
            int index = searchChars.indexOf(Character.toUpperCase(codePoint));
            return index >= 0 ? index : searchChars.indexOf(Character.toLowerCase(codePoint));

        }
    }

    /**
     * Replaces all occurrences of a String within another String..
     *
     * @param str the string
     * @param search the substring to search for
     * @param replace every occurrence of search will be replaced by this
     * @return the index of the first occurrence of needle in s
     */
    public static String replace(final String str, final String search, final String replace) {
        return replace(str, search, replace, "");
    }

    /**
     * Replaces all occurrences of a String within another String..
     *
     * @param str the string
     * @param search the substring to search for
     * @param replace every occurrence of search will be replaced by this
     * @param modifiers string with binary options
     * @return the index of the first occurrence of needle in s
     */
    public static String replace(final String str, final String search, final String replace, final String modifiers) {
        if (null == str || str.isEmpty() || null == search || search.isEmpty() || replace == null) {
            return str;
        }
        String opt = (null != modifiers) ? modifiers.toLowerCase(Locale.ENGLISH) : "";
        boolean ignoreCase = StringUtils.contains(opt, 'i');
        boolean words = StringUtils.contains(opt, 'w');
        // create new modifiers string with allowed options
        String mdfrs = "";
        mdfrs = ignoreCase ? opt + "i" : opt;
        mdfrs = words ? opt + "w" : opt;

        int start = 0;
        int end = indexOf(str, search, mdfrs);
        if (end == StringUtils.INDEX_NOT_FOUND) {
            return str;
        }
        StringBuilder buf = new StringBuilder();
        while (end != StringUtils.INDEX_NOT_FOUND) {
            buf.append(str.substring(start, end)).append(replace);
            start = end + search.length();
            end = indexOf(str, search, start, mdfrs);
        }
        buf.append(str.substring(start));
        return buf.toString();
    }

    /**
     * @param str where all umlauts should be replaced (must be not null)
     * @param omitE option if 'e' should be omitted. (Ex. 'ä' --> 'ae' or 'a') (must be not null)
     * @return string without umlauts (never null)
     */
    public static String replaceUmlauts(final String str, final boolean omitE) {
        String result;
        if (omitE) {
            result = StringUtils.replaceEach(str, UMLAUTS, REPLACEMENT_OMIT_E);
        } else {
            result = StringUtils.replaceEach(str, UMLAUTS, REPLACEMENT_E);
        }
        return result;
    }

    /**
     * Get the reverse of the string.
     *
     * @param str the string
     * @return the reverse of the string
     */
    public static String reverse(final String str) {
        if (null == str || str.isEmpty()) {
            return str;
        }
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * Strips any whitespace characters from the end of strings.
     *
     * @param str the strings to strip
     * @return the list of stripped strings
     */
    public static String[] stripEnd(final String... str) {
        if (str == null || (str.length) == 0) {
            return str;
        }
        String[] newArr = new String[str.length];
        for (int i = 0; i < str.length; i++) {
            newArr[i] = StringUtils.stripEnd(str[i], null);
        }
        return newArr;
    }

    /**
     * Strips any whitespace characters from the start and the end of strings.
     *
     * @param str the strings to strip
     * @return the list of stripped strings
     */
    public static String[] strip(final String... str) {
        if (str == null || (str.length) == 0) {
            return str;
        }
        String[] newArr = new String[str.length];
        for (int i = 0; i < str.length; i++) {
            newArr[i] = StringUtils.strip(str[i], null);
        }
        return newArr;
    }

    /**
     * Strips any whitespace characters from the start of strings.
     *
     * @param str the strings to strip
     * @return the list of stripped strings
     */
    public static String[] stripStart(final String... str) {
        if (str == null || (str.length) == 0) {
            return str;
        }
        String[] newArr = new String[str.length];
        for (int i = 0; i < str.length; i++) {
            newArr[i] = StringUtils.stripStart(str[i], null);
        }
        return newArr;
    }

    /**
     * Converts null inputs to empty strings.
     *
     * @param str the values to convert
     * @return the converted strings
     */
    public static String[] toEmpty(final String... str) {
        if (str == null || (str.length) == 0) {
            return str;
        }
        String[] newArr = new String[str.length];
        for (int i = 0; i < str.length; i++) {
            newArr[i] = null != str[i] ? str[i] : "";
        }
        return newArr;
    }

    /**
     * Converts empty strings to null.
     *
     * @param str the values to convert
     * @return the converted strings
     */
    public static String[] toNull(final String... str) {
        if (str == null || (str.length) == 0) {
            return str;
        }
        String[] newArr = new String[str.length];
        for (int i = 0; i < str.length; i++) {
            newArr[i] = null != str[i] && str[i].isEmpty() ? null : str[i];
        }
        return newArr;
    }


    /**
     * Replaces percent encoded character triplets back to their referenced characters. For instance:
     * "%20" -> " "
     * "%2F" -> "/"
     *
     * Uses the java standard library implementation {@link URLDecoder#decode(String, String)}.
     * Silently ignores exceptions caused by passing an illegal charsetName.
     *
     * @param str the string to decode
     * @param charsetName the character set to use.
     * @return the decoded string or the original string if the charset is not supported.
     * @since 4.2
     */
    public static String urlDecode(final String str, final String charsetName) {
        try {
            return URLDecoder.decode(str, charsetName);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage());
            return str;
        }
    }

    /**
     * Replaces characters that are not allowed or reserved in an URL by applying percent encoding. This replaces the
     * octet (8 bits) by a character triplet starting with a percent sign. For instance: " " -> "%20" "/" -> "%2F" "?"
     * -> "%3F"
     *
     * Uses the java standard library implementation: {@link URLEncoder#encode(String, String)}. set.
     * Silently ignores exceptions caused by passing an illegal charsetName.
     * See https://en.wikipedia.org/wiki/Percent-encoding and the URI RFC on details when to apply percent encoding.
     * https://tools.ietf.org/html/rfc3986#section-2.1
     *
     * @param str string to apply percent encoding to
     * @param charsetName the character set to use
     * @return the escaped string or the original string if the charset is not supported.
     * @since 4.2
     */
    public static String urlEncode(final String str, final String charsetName) {
        try {
            return URLEncoder.encode(str, charsetName);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage());
            return str;
        }
    }

}
