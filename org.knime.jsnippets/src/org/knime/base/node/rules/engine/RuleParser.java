/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * Created on 2013.04.16. by Gabor
 */
package org.knime.base.node.rules.engine;

import java.net.URL;
import java.text.ParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.text.Element;
import javax.swing.text.Segment;

import org.fife.rsta.ac.AbstractLanguageSupport;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ExtendedHyperlinkListener;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Wraps a normal parser for the rules and provides the {@link Parser} interface.
 *
 * @author Gabor
 * @since 2.8
 */
public class RuleParser implements Parser {
    /** Syntax style key for rules. */
    static final String SYNTAX_STYLE_RULE = "text/knime-rule";

    //    /** Syntax style key for rules without mandatory outcome */
    //    static final String SYNTAX_STYLE_RULE_NO_OUTCOME = "text/knime-rule-no-outcome";

    /**
     * Language support class for the rule language.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    public static class RuleLanguageSupport extends AbstractLanguageSupport {
        /**
         * Constructs the {@link RuleLanguageSupport}.
         */
        RuleLanguageSupport() {
            LanguageSupportFactory.get().addLanguageSupport(SYNTAX_STYLE_RULE, RuleLanguageSupport.class.getName());
            ((AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance())
                    .putMapping(RuleParser.SYNTAX_STYLE_RULE, KnimeTokenMaker.class.getName(),
                                RuleLanguageSupport.class.getClassLoader());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void install(final RSyntaxTextArea textArea) {
            RuleParser parser = new RuleParser();
            textArea.addParser(parser);
            textArea.putClientProperty(PROPERTY_LANGUAGE_PARSER, parser);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void uninstall(final RSyntaxTextArea textArea) {
            RuleParser parser = findParser(textArea);
            if (parser != null) {
                textArea.removeParser(parser);
            }
        }

        /**
         * @param textArea
         * @return
         */
        private RuleParser findParser(final RSyntaxTextArea textArea) {
            Object parser = textArea.getClientProperty(PROPERTY_LANGUAGE_PARSER);
            if (parser instanceof RuleParser) {
                return (RuleParser)parser;
            }
            return null;
        }
    }

    /**
     * Wraps a {@link TokenMaker} and makes the {@link Operators} {@link Operators#toString()} a keyword.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    public static class KnimeTokenMaker implements TokenMaker {
        private final TokenMaker m_wrapped;

        private final Set<String> m_operators;

        /**
         * Constructs a {@link Rule} token maker based on the Java {@link TokenMaker}.
         */
        public KnimeTokenMaker() {
            this(TokenMakerFactory.getDefaultInstance().getTokenMaker("text/java"));
        }

        private KnimeTokenMaker(final TokenMaker wrapped) {
            m_wrapped = wrapped;
            m_operators = new HashSet<String>(new RuleParser().m_operators);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addNullToken() {
            m_wrapped.addNullToken();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addToken(final char[] array, final int start, final int end, final int tokenType,
                             final int startOffset) {
            m_wrapped.addToken(array, start, end, tokenType, startOffset);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean getCurlyBracesDenoteCodeBlocks() {
            return m_wrapped.getCurlyBracesDenoteCodeBlocks();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getLastTokenTypeOnLine(final Segment text, final int initialTokenType) {
            return m_wrapped.getLastTokenTypeOnLine(text, initialTokenType);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] getLineCommentStartAndEnd() {
            return m_wrapped.getLineCommentStartAndEnd();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Action getInsertBreakAction() {
            return m_wrapped.getInsertBreakAction();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean getMarkOccurrencesOfTokenType(final int type) {
            return m_wrapped.getMarkOccurrencesOfTokenType(type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean getShouldIndentNextLineAfter(final Token token) {
            return m_wrapped.getShouldIndentNextLineAfter(token);
        }

        /**
         * {@inheritDoc}
         *
         * Makes the operator names {@link TokenTypes#RESERVED_WORD}.
         */
        @Override
        public Token getTokenList(final Segment text, final int initialTokenType, final int startOffset) {
            Token tokenList = m_wrapped.getTokenList(text, initialTokenType, startOffset);
            Token token = tokenList;
            while (token != null && token.isPaintable()) {
                if (m_operators.contains(token.getLexeme())) {
                    token.type = TokenTypes.RESERVED_WORD;
                }
                token = token.getNextToken();
            }
            return tokenList;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isMarkupLanguage() {
            return m_wrapped.isMarkupLanguage();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isWhitespaceVisible() {
            return m_wrapped.isWhitespaceVisible();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setWhitespaceVisible(final boolean visible) {
            m_wrapped.setWhitespaceVisible(visible);
        }

    }

    private DataTableSpec m_dataTableSpec;

    private Map<String, FlowVariable> m_flowVariables;

    private Set<String> m_operators = new HashSet<String>();

    private boolean m_isEnabled;

    private final boolean m_warnOnColRefsInStrings;

    private final boolean m_allowNoOutcome;

    private final boolean m_allowTableReferences;

    /**
     * Constructs a {@link RuleParser}. It requires outcomes in the rules.
     *
     * @param warnOnColRefsInStrings In outcome strings signal a warning if it starts with and ends with {@code $}
     *            signs.
     * @see #RuleParser(boolean, boolean)
     */
    public RuleParser(final boolean warnOnColRefsInStrings) {
        this(warnOnColRefsInStrings, false, true);

    }

    /**
     * Constructs the {@link RuleParser} to allow or disallow no outcomes and to warn or not on {@code $} characters in
     * the outcomes.
     *
     * @param warnOnColRefsInStrings In outcome strings signal a warning if it starts with and ends with {@code $}
     *            signs.
     * @param allowNoOutcome Whether or not allow omitting the outcome.
     */
    public RuleParser(final boolean warnOnColRefsInStrings, final boolean allowNoOutcome) {
        this(warnOnColRefsInStrings, allowNoOutcome, true);
    }

    /**
     * Constructs the {@link RuleParser} to allow or disallow no outcomes and to warn or not on {@code $} characters in
     * the outcomes.
     *
     * @param warnOnColRefsInStrings In outcome strings signal a warning if it starts with and ends with {@code $}
     *            signs.
     * @param allowNoOutcome Whether or not allow omitting the outcome.
     * @param allowTableReferences Enable to parse {@code $$ROWINDEX$$}, {@code $$ROWID$$} or {@code $$ROWCOUNT$$}.
     */
    public RuleParser(final boolean warnOnColRefsInStrings, final boolean allowNoOutcome,
                      final boolean allowTableReferences) {
        if (allowNoOutcome && !allowTableReferences) {
            throw new UnsupportedOperationException(
                    "Not supported combination of parameters: allow no outcome and not allowing table references.");
        }
        this.m_warnOnColRefsInStrings = warnOnColRefsInStrings;
        this.m_allowNoOutcome = allowNoOutcome;
        this.m_allowTableReferences = allowTableReferences;
        m_isEnabled = true;
        for (Rule.Operators op : Rule.Operators.values()) {
            m_operators.add(op.toString());
        }
    }

    /**
     * Constructs the {@link RuleParser} with warnings for {@code $} characters in {@link String}s and disallowing the
     * missing outcome option.
     *
     * @see #RuleParser(boolean, boolean)
     */
    public RuleParser() {
        this(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtendedHyperlinkListener getHyperlinkListener() {
        // Not required to be implemented.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getImageBase() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return m_isEnabled;
    }

    /**
     * @param enabled New value for enabled.
     * @see #isEnabled()
     */
    public void setEnabled(final boolean enabled) {
        m_isEnabled = enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult parse(final RSyntaxDocument doc, final String style) {
        if (!RuleParser.SYNTAX_STYLE_RULE.equals(style) || !m_isEnabled) {
            return new DefaultParseResult(this);
        }
        DefaultParseResult res = new DefaultParseResult(this);
        Element rootElement = doc.getDefaultRootElement();
        //SimpleRuleParser ruleParser = new SimpleRuleParser(m_dataTableSpec, m_flowVariables);
        final RuleFactory factory =
                m_allowNoOutcome ? RuleFactory.getFilterInstance() : m_allowTableReferences ? RuleFactory.getInstance()
                        : RuleFactory.getVariableInstance();
        for (int line = 0; line < rootElement.getElementCount(); ++line) {
            String lastString = null;
            StringBuilder sb = new StringBuilder();
            //Go through the tokens
            Token token = doc.getTokenListForLine(line);
            while (token != null && token.isPaintable()) {
                String lexeme = token.getLexeme();
                //If it is an operator, make it a reserved word.
                if (m_operators.contains(lexeme)) {
                    token.type = TokenTypes.RESERVED_WORD;
                    token.setLanguageIndex(0);
                }
                sb.append(lexeme);
                if (lexeme.length() > 0 && lexeme.charAt(0) == '"' && lexeme.charAt(lexeme.length() - 1) == '"') {
                    lastString = lexeme;
                }
                token = token.getNextToken();
            }
            try {
                String lineText = sb.toString();
                //Check for potential error in outcomes.
                if (lineText.trim().endsWith("\"") && m_warnOnColRefsInStrings) {
                    String lastContent =
                            lastString != null && lastString.length() > 2 ? lastString
                                    .substring(1, lastString.length() - 1) : lastString;
                    if (lastContent != null && lastContent.endsWith("$") && lastContent.startsWith("$")) { //TODO better check
                        DefaultParserNotice notice =
                                new DefaultParserNotice(this,
                                        "You are referring to a column or flow variable, although no String interpolation is implemented, you might want to "
                                                + "remove the quotes around the reference.", line,
                                        doc.getTokenListForLine(line).offset + lineText.lastIndexOf(lastContent),
                                        lastContent.length());
                        notice.setLevel(ParserNotice.WARNING);
                        res.addNotice(notice);
                    }
                }
                if (!lineText.isEmpty()) {
                    factory.parse(lineText, m_dataTableSpec, m_flowVariables);
                }
            } catch (ParseException e) {
                DefaultParserNotice notice;
                if (e.getErrorOffset() >= sb.length()) {
                    notice = new DefaultParserNotice(this, e.getMessage(), line);
                } else {
                    notice =
                            new DefaultParserNotice(this, e.getMessage(), line, doc.getTokenListForLine(line).offset
                                    + e.getErrorOffset(), sb.length() - e.getErrorOffset());
                }
                res.addNotice(notice);
            }
        }
        return res;
    }

    /**
     * @param dataTableSpec the dataTableSpec to set
     */
    public void setDataTableSpec(final DataTableSpec dataTableSpec) {
        this.m_dataTableSpec = dataTableSpec;
    }

    /**
     * @param flowVariables the flow variables to set
     */
    public void setFlowVariables(final Map<String, FlowVariable> flowVariables) {
        this.m_flowVariables = new LinkedHashMap<String, FlowVariable>(flowVariables);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_dataTableSpec == null) ? 0 : m_dataTableSpec.hashCode());
        result = prime * result + (m_isEnabled ? 1231 : 1237);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RuleParser other = (RuleParser)obj;
        if (m_dataTableSpec == null) {
            if (other.m_dataTableSpec != null) {
                return false;
            }
        } else if (!m_dataTableSpec.equals(other.m_dataTableSpec)) {
            return false;
        }
        if (m_isEnabled != other.m_isEnabled) {
            return false;
        }
        if (m_warnOnColRefsInStrings != other.m_warnOnColRefsInStrings) {
            return false;
        }
        if (m_allowNoOutcome != other.m_allowNoOutcome) {
            return false;
        }
        return true;
    }
}
