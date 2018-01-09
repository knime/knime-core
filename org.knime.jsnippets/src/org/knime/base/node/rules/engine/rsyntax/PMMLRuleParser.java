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
 * Created on 2013.08.09. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.rsyntax;

import java.text.ParseException;

import org.fife.rsta.ac.LanguageSupport;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLTruePredicate;
import org.knime.base.node.rules.engine.BaseRuleParser.ParseState;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.rules.engine.RuleSupport;

/**
 * The parser for {@link RSyntaxTextArea}.
 *
 * @author Gabor Bakos
 * @since 2.9
 */
public class PMMLRuleParser extends AbstractRuleParser {

    /**
     * {@link LanguageSupport} class for PMML rules.
     *
     * @since 2.9
     */
    public static class PMMLRuleLanguageSupport extends AbstractRuleLanguageSupport<PMMLRuleParser> {
        /**
         * Default constructor.
         */
        public PMMLRuleLanguageSupport() {
            super(SYNTAX_STYLE_RULE_PMML, PMMLRuleLanguageSupport.class, PMMLRuleTokenMaker.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected PMMLRuleParser createParser() {
            return new PMMLRuleParser();
        }
    }

    /**
     * The tokenMaker for PMML rules.
     *
     * @since 2.9
     */
    public static class PMMLRuleTokenMaker extends WrappedTokenMaker {
        /**
         * Default constructor.
         */
        public PMMLRuleTokenMaker() {
            super(TokenMakerFactory.getDefaultInstance().getTokenMaker("text/java"), new PMMLRuleParser()
                .getOperators());
        }
    }

    /** Syntax style key for rules of PMML-based Rule editor. */
    public static final String SYNTAX_STYLE_RULE_PMML = "text/knime-PMML-4-1-rule";

    /**
     * Default constructor. (Warn on possible colrefs, do not allow missing outcome, or table references.)
     */
    public PMMLRuleParser() {
        this(true);
    }

    /**
     * Custom constructor for the parser.
     *
     * @param warnOnColRefsInStrings Warn if there is a possible column reference in {@link String}s.
     */
    public PMMLRuleParser(final boolean warnOnColRefsInStrings) {
        super(warnOnColRefsInStrings, RuleNodeSettings.PMMLRule);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isNotApplicable(final String style) {
        return !style.equals(SYNTAX_STYLE_RULE_PMML);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean parseAndWarn(final RSyntaxDocument doc, final DefaultParseResult res, final boolean wasCatchAllRule, final int line,
        final String lineText) throws ParseException {
        if (RuleSupport.isComment(lineText)) {
            return false;
        }
        ParseState state = new ParseState(lineText);
        org.knime.base.node.rules.engine.pmml.PMMLRuleParser mainParser = new org.knime.base.node.rules.engine.pmml.PMMLRuleParser(getSpec(), getFlowVariables());
        PMMLPredicate condition = mainParser.parseBooleanExpression(state);
        state.skipWS();
        state.consumeText("=>");
        mainParser.parseOutcomeOperand(state, null);
        if (wasCatchAllRule || (condition instanceof PMMLFalsePredicate)) {
            addWarningNotice(doc, res, wasCatchAllRule, line, lineText);
        }
        return condition instanceof PMMLTruePredicate;
    }
}
