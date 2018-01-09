/*
 * ------------------------------------------------------------------------
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
 * Created on 2013.04.16. by Gabor
 */
package org.knime.base.node.rules.engine.rsyntax;

import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.text.Element;
import javax.swing.text.Segment;

import org.fife.rsta.ac.AbstractLanguageSupport;
import org.fife.rsta.ac.LanguageSupport;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.OccurrenceMarker;
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
import org.knime.base.node.rules.engine.Rule;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.RuleFactory;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.rules.engine.rsyntax.RuleParser.RuleLanguageSupport;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Wraps a normal parser for the rules and provides the {@link Parser} interface.
 *
 * @author Gabor Bakos
 * @since 2.9
 */
public class AbstractRuleParser implements Parser {
    /** Syntax style key for rules of Rule Engine. */
    public static final String SYNTAX_STYLE_RULE = "text/knime-rule";

    /**
     * An abstract {@link RuleLanguageSupport} class, which registers the class to the {@link LanguageSupportFactory} in
     * the constructor.
     *
     * @param <ParserType> The type of {@link Parser}.
     * @since 2.9
     */
    protected static abstract class AbstractRuleLanguageSupport<ParserType extends Parser> extends
        AbstractLanguageSupport {
        private final Class<? extends AbstractRuleLanguageSupport<?>> m_langSupportClass;

        private final Class<? extends TokenMaker> m_tokenMakerClass;

        /**
         * Constructs the {@link RuleParser.RuleLanguageSupport}.
         *
         * @param styleKey The style key.
         * @param langSupportClass The class of {@link LanguageSupport} class.
         * @param tokenMakerClass The {@link TokenMaker} class.
         */
        public AbstractRuleLanguageSupport(final String styleKey,
            final Class<? extends AbstractRuleLanguageSupport<?>> langSupportClass,
            final Class<? extends TokenMaker> tokenMakerClass) {
            super();
            this.m_langSupportClass = langSupportClass;
            this.m_tokenMakerClass = tokenMakerClass;
            LanguageSupportFactory.get().addLanguageSupport(styleKey, langSupportClass.getName());
            ((AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance()).putMapping(styleKey,
                m_tokenMakerClass.getName(), m_langSupportClass.getClassLoader());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void install(final RSyntaxTextArea textArea) {
            ParserType parser = createParser();
            textArea.addParser(parser);
            textArea.putClientProperty(PROPERTY_LANGUAGE_PARSER, parser);
        }

        /**
         * @return Creates the parser of type parameter.
         */
        protected abstract ParserType createParser();

        /**
         * {@inheritDoc}
         */
        @Override
        public void uninstall(final RSyntaxTextArea textArea) {
            AbstractRuleParser parser = findParser(textArea, AbstractRuleParser.class);
            if (parser != null) {
                textArea.removeParser(parser);
            }
        }

        /**
         * @param textArea An {@link RSyntaxTextArea}.
         * @param cls The class of the {@link Parser}.
         * @return The {@link Parser} belonging to the {@code textArea}, or {@code null}.
         */
        protected static <T extends Parser> T findParser(final RSyntaxTextArea textArea, final Class<? extends T> cls) {
            Object parser = textArea.getClientProperty(PROPERTY_LANGUAGE_PARSER);
            if (cls.isInstance(parser)) {
                return cls.cast(parser);
            }
            return null;
        }
    }

    /**
     * Wraps a {@link TokenMaker} and allows to special case certain methods.
     */
    public static class WrappedTokenMaker implements TokenMaker {
        private final TokenMaker m_wrapped;

        private final Set<String> m_operators;

        /**
         * Constructs {@link WrappedTokenMaker} to highlight the oparator arguments.
         *
         * @param wrapped A {@link TokenMaker}.
         * @param operators The operator names to convert to reserved words.
         */
        protected WrappedTokenMaker(final TokenMaker wrapped, final Collection<String> operators) {
            m_wrapped = wrapped;
            m_operators = new HashSet<String>(operators);
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
        public boolean getCurlyBracesDenoteCodeBlocks(final int languageIndex) {
            return m_wrapped.getCurlyBracesDenoteCodeBlocks(languageIndex);
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
        public String[] getLineCommentStartAndEnd(final int languageIndex) {
            return m_wrapped.getLineCommentStartAndEnd(languageIndex);
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
            boolean perlStyleMode = false, escape = false;
            while (token != null && token.isPaintable()) {
                String lexeme = token.getLexeme();
                if (lexeme.equals("/")) {
                    perlStyleMode = (!escape && !perlStyleMode) || escape;
                }
                escape = lexeme.equals("\\") && perlStyleMode;
                switch (token.getType()) {
                    case TokenTypes.RESERVED_WORD:
                    case TokenTypes.RESERVED_WORD_2:
                    case TokenTypes.LITERAL_BOOLEAN:
                    case TokenTypes.LITERAL_CHAR:
                    case TokenTypes.COMMENT_DOCUMENTATION:
                    case TokenTypes.COMMENT_MULTILINE:
                        token.setType(TokenTypes.IDENTIFIER);
                        break;
                    case TokenTypes.ERROR_STRING_DOUBLE:
                    case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
                            if (token.getLexeme().endsWith("\"")) {
                                token.setType(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE);
                            }
                        break;
                    default:
                        //Do nothing.
                        break;
                }
                if (m_operators.contains(token.getLexeme())) {
                    token.setType(TokenTypes.RESERVED_WORD);
                }
                if (perlStyleMode || lexeme.equals("/")) {
                    token.setType(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE);
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
        public OccurrenceMarker getOccurrenceMarker() {
            return m_wrapped.getOccurrenceMarker();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getClosestStandardTokenTypeForInternalType(final int type) {
            return m_wrapped.getClosestStandardTokenTypeForInternalType(type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isIdentifierChar(final int languageIndex, final char ch) {
            return m_wrapped.isIdentifierChar(languageIndex, ch);
        }

    }

    private DataTableSpec m_dataTableSpec;

    private Map<String, FlowVariable> m_flowVariables;

    private final Set<String> m_operators = new HashSet<String>();

    private boolean m_isEnabled;

    private boolean m_warnOnColRefsInStrings;

    private boolean m_allowNoOutcome;

    private boolean m_allowTableReferences;

    private final RuleNodeSettings m_nodeType;

    /**
     * @param warnOnColRefsInStrings Warn on suspicious references in {@link String}s.
     * @param nodeType The {@link RuleNodeSettings}.
     */
    public AbstractRuleParser(final boolean warnOnColRefsInStrings, final RuleNodeSettings nodeType) {
        this.m_warnOnColRefsInStrings = warnOnColRefsInStrings;
        this.m_nodeType = nodeType;
        this.m_allowNoOutcome = false;
        this.m_allowTableReferences = nodeType.allowTableProperties();
        for (Operators op : nodeType.supportedOperators()) {
            m_operators.add(op.toString());
        }
        m_isEnabled = true;
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
        if (isNotApplicable(style)) {
            return new DefaultParseResult(this);
        }
        DefaultParseResult res = new DefaultParseResult(this);
        Element rootElement = doc.getDefaultRootElement();
        boolean wasCatchAllRule = false;
        for (int line = 0; line < rootElement.getElementCount(); ++line) {
            String lastString = null;
            StringBuilder sb = new StringBuilder();
            //Go through the tokens
            Token token = doc.getTokenListForLine(line);
            while (token != null && token.isPaintable()) {
                String lexeme = token.getLexeme();
                //If it is an operator, make it a reserved word.
                if (getOperators().contains(lexeme)) {
                    token.setType(TokenTypes.RESERVED_WORD);
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
                    //ENH better check
                    if (lastContent != null && lastContent.endsWith("$") && lastContent.charAt(0) == '$') {
                        DefaultParserNotice notice =
                            new DefaultParserNotice(this,
                                "You might be referring to a column or flow variable, although no String "
                                    + "interpolation is implemented, you might want to "
                                    + "remove the quotes around the reference.", line,
                                doc.getTokenListForLine(line).getOffset() + lineText.lastIndexOf(lastContent),
                                lastContent.length());
                        notice.setLevel(ParserNotice.Level.WARNING);
                        res.addNotice(notice);
                    }
                }
                if (!lineText.isEmpty()) {
                    wasCatchAllRule |= parseAndWarn(doc, res, wasCatchAllRule, line, lineText);
                }
            } catch (ParseException e) {
                DefaultParserNotice notice;
                if (e.getErrorOffset() >= sb.length()) {
                    notice = new DefaultParserNotice(this, e.getMessage(), line);
                } else {
                    notice =
                        new DefaultParserNotice(this, e.getMessage(), line, doc.getTokenListForLine(line).getOffset()
                            + e.getErrorOffset(), sb.length() - e.getErrorOffset());
                }
                res.addNotice(notice);
            }
        }
        return res;
    }

    /**
     * Parses the line and warns about possible problems.
     *
     * @param doc The document.
     * @param res The {@link ParseResult}.
     * @param wasCatchAllRule Whether there was a catchAll rule before.
     * @param line The line index starting from {@code 0}.
     * @param lineText The rule text.
     * @return The rule's condition will always be true.
     * @throws ParseException Problem during parsing.
     */
    protected boolean parseAndWarn(final RSyntaxDocument doc, final DefaultParseResult res, final boolean wasCatchAllRule, final int line,
        final String lineText) throws ParseException {
        Rule rule = RuleFactory.getInstance(m_nodeType).parse(lineText, m_dataTableSpec, m_flowVariables);
        if (rule.getCondition().isEnabled() && (wasCatchAllRule || rule.getCondition().isConstantFalse())) {
            addWarningNotice(doc, res, wasCatchAllRule, line, lineText);
        }
        return rule.getCondition().isCatchAll();
    }

    /**
     * Adds a warning about unreachability or FALSE conditions.
     *
     * @param doc The document.
     * @param res The {@link ParseResult}.
     * @param wasCatchAllRule Whether there was a catchAll rule before.
     * @param line The line index starting from {@code 0}.
     * @param lineText The rule text.
     */
    protected void addWarningNotice(final RSyntaxDocument doc, final DefaultParseResult res,
        final boolean wasCatchAllRule, final int line, final String lineText) {
        DefaultParserNotice notice =
            new DefaultParserNotice(this, wasCatchAllRule
                ? "There was a rule that might always match, this rule will probably never be used."
                : "This rule might never match.", line, doc.getTokenListForLine(line).getOffset(),
                lineText.length());
        notice.setLevel(ParserNotice.Level.WARNING);
        res.addNotice(notice);
    }

    /**
     * Checks whether this parser is applicable to the input or not.
     *
     * @param style The style key of the grammar to use. (Default implementation checks for
     *            {@link AbstractRuleParser#SYNTAX_STYLE_RULE}.)
     * @return If <em>not</em> applicable, return {@code true}, if applicable, return {@code false}.
     */
    protected boolean isNotApplicable(final String style) {
        return !AbstractRuleParser.SYNTAX_STYLE_RULE.equals(style) || !m_isEnabled
            || (m_dataTableSpec == null && m_allowTableReferences);
    }

    /**
     * @return the m_operators
     */
    public Collection<String> getOperators() {
        return m_operators;
    }

    /**
     * @param allowNoOutcome the allowNoOutcome to set
     */
    public void setAllowNoOutcome(final boolean allowNoOutcome) {
        this.m_allowNoOutcome = allowNoOutcome;
    }

    /**
     * @param allowTableReferences the allowTableReferences to set
     */
    public void setAllowTableReferences(final boolean allowTableReferences) {
        this.m_allowTableReferences = allowTableReferences;
    }

    /**
     * @param warnOnColRefsInStrings the warnOnColRefsInStrings to set
     */
    public void setWarnOnColRefsInStrings(final boolean warnOnColRefsInStrings) {
        this.m_warnOnColRefsInStrings = warnOnColRefsInStrings;
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
        AbstractRuleParser other = (AbstractRuleParser)obj;
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

    /**
     * @return the dataTableSpec
     */
    protected DataTableSpec getSpec() {
        return m_dataTableSpec;
    }

    /**
     * @return the flowVariables
     */
    protected Map<String, FlowVariable> getFlowVariables() {
        return Collections.unmodifiableMap(m_flowVariables);
    }
}
