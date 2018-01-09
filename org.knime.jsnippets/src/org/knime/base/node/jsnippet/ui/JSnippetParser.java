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
 * ------------------------------------------------------------------------
 *
 * History
 *   02.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.knime.base.node.jsnippet.util.JSnippet;
import org.knime.base.node.jsnippet.util.JavaSnippetCompiler;
import org.knime.core.node.NodeLogger;

/**
 *
 * <p>This class might change and is not meant as public API.
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class JSnippetParser extends AbstractParser {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            JSnippetParser.class);
    private JSnippet<?> m_snippet;

    /**
     * Create a new parser.
     * @param snippet the snippet
     */
    public JSnippetParser(final JSnippet<?> snippet) {
        m_snippet = snippet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult parse(final RSyntaxDocument doc, final String style) {
        assert m_snippet.getDocument() == doc;

        JavaSnippetCompiler compiler = new JavaSnippetCompiler(m_snippet);
        StringWriter log = new StringWriter();
        DiagnosticCollector<JavaFileObject> digsCollector =
            new DiagnosticCollector<>();
        CompilationTask compileTask = null;
        try {
            compileTask = compiler.getTask(log, digsCollector);
        } catch (IOException e) {
            LOGGER.error("Cannot create an compile task.", e);
            return new DefaultParseResult(this);
        }
        compileTask.call();
        DefaultParseResult parseResult = new DefaultParseResult(this);
        parseResult.setError(null);
        for (Diagnostic<? extends JavaFileObject> d
                : digsCollector.getDiagnostics()) {
            boolean isSnippet = m_snippet.isSnippetSource(d.getSource());
            if (isSnippet) {
                DefaultParserNotice notice = new DefaultParserNotice(this,
                        d.getMessage(Locale.US),
                        (int)d.getLineNumber(),
                        (int)d.getStartPosition(),
                        (int)(d.getEndPosition()
                                - d.getStartPosition() + 1));
                if (d.getKind().equals(Kind.ERROR)) {
                    notice.setLevel(ParserNotice.Level.ERROR);
//                    LOGGER.error(d.getMessage(Locale.US));
                } else if (d.getKind().equals(Kind.WARNING)) {
                    notice.setLevel(ParserNotice.Level.WARNING);
//                    LOGGER.warn(d.getMessage(Locale.US));
                } else {
                    notice.setLevel(ParserNotice.Level.INFO);
//                    LOGGER.debug(d.getMessage(Locale.US));
                }
                parseResult.addNotice(notice);
            }
        }
        return parseResult;
    }

}
