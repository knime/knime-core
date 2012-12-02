/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   24.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.ToolTipManager;

import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.rsyntaxarea.internal.RSyntaxAreaActivator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.knime.base.node.jsnippet.JavaSnippet;
import org.knime.base.node.jsnippet.JavaSnippetDocument;
import org.knime.base.node.jsnippet.guarded.GuardedDocument;
import org.knime.base.node.jsnippet.guarded.GuardedSection;
import org.knime.base.node.jsnippet.guarded.GuardedSectionsFoldParser;
import org.knime.core.node.NodeLogger;

/**
 * A text area for the java snippet expression.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class JSnippetTextArea extends RSyntaxTextArea {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(JSnippetTextArea.class);

    static {
        RSyntaxAreaActivator.ensureWorkaroundBug3692Applied();
    }

    /**
     * Create a new component.
     * @param snippet the snippet
     */
    public JSnippetTextArea(final JavaSnippet snippet) {
        // initial text != null causes a null pointer exception
        super(new JavaSnippetDocument(), null, 20, 60);

        setDocument(snippet.getDocument());
        addParser(snippet.getParser());

        try {
            applySyntaxColors();
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }


        boolean parserInstalled = FoldParserManager.get().getFoldParser(
                SYNTAX_STYLE_JAVA) instanceof GuardedSectionsFoldParser;
        if (!parserInstalled) {
            FoldParserManager.get().addFoldParserMapping(SYNTAX_STYLE_JAVA,
                    new GuardedSectionsFoldParser());
        }
        setCodeFoldingEnabled(true);
        setSyntaxEditingStyle(SYNTAX_STYLE_JAVA);

        ToolTipManager.sharedInstance().registerComponent(this);
        LanguageSupportFactory.get().register(this);
    }


    private void applySyntaxColors() throws IOException {
        Package pack = JSnippetTextArea.class.getPackage();
        String base = pack.getName().replace(".", "/") + "/";
        URL url = this.getClass().getClassLoader()
                .getResource(base + "eclipse.xml");
        InputStream in = url.openStream();
        Theme theme = Theme.load(in);
        theme.apply(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getForegroundForToken(final Token t) {
        if (isInGuardedSection(t.offset)) {
            return Color.gray;
        } else {
            return super.getForegroundForToken(t);
        }
    }

    /**
     * Returns true when offset is within a guarded section.
     *
     * @param offset the offset to test
     * @return true when offset is within a guarded section.
     */
    private boolean isInGuardedSection(final int offset) {
        GuardedDocument doc = (GuardedDocument)getDocument();

        for (String name : doc.getGuardedSections()) {
            GuardedSection gs = doc.getGuardedSection(name);
            if (gs.contains(offset)) {
                return true;
            }
        }
        return false;
    }
}
