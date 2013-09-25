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
 * Created on 2013.04.25. by Gabor
 */
package org.knime.base.node.util;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.ToolTipManager;

import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.rsyntaxarea.internal.RSyntaxAreaActivator;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextArea;
import org.knime.base.node.jsnippet.ui.JSnippetTextArea;
import org.knime.core.node.NodeLogger;

/**
 * A base class for {@link JSnippetTextArea}, but this can be used for non-Java editors too. This class loads the
 * default theme for the editor and applies workarounds to the known problems with {@link RSyntaxTextArea}.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
@SuppressWarnings("serial")
public class KnimeSyntaxTextArea extends RSyntaxTextArea {

    static {
        RSyntaxAreaActivator.ensureWorkaroundBug3692Applied();
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(KnimeSyntaxTextArea.class);

    /**
     * No-arg constructor for {@link KnimeSyntaxTextArea}.
     */
    public KnimeSyntaxTextArea() {
        super();
    }

    /**
     * Constructs {@link KnimeSyntaxTextArea}.
     *
     * @param doc The wrapped document.
     */
    public KnimeSyntaxTextArea(final RSyntaxDocument doc) {
        super(doc);
    }

    /**
     * Constructs {@link KnimeSyntaxTextArea}.
     *
     * @param text The initial text to show.
     */
    public KnimeSyntaxTextArea(final String text) {
        super(text);
    }

    /**
     * Constructs {@link KnimeSyntaxTextArea}.
     *
     * @param textMode The initial text mode. Either {@link RTextArea#INSERT_MODE} or {@link RTextArea#OVERWRITE_MODE}.
     * @see RSyntaxTextArea#RSyntaxTextArea(int)
     */
    public KnimeSyntaxTextArea(final int textMode) {
        super(textMode);
    }

    /**
     * Constructs {@link KnimeSyntaxTextArea}.
     *
     * @param rows Number of rows in preferred size.
     * @param columns Number of columns in preferred size.
     */
    public KnimeSyntaxTextArea(final int rows, final int columns) {
        super(rows, columns);
    }

    /**
     * Constructs {@link KnimeSyntaxTextArea}.
     *
     * @param text Initial text content.
     * @param rows Number of rows in preferred size.
     * @param columns Number of columns in preferred size.
     */
    public KnimeSyntaxTextArea(final String text, final int rows, final int columns) {
        super(text, rows, columns);
    }

    /**
     * Constructs {@link KnimeSyntaxTextArea}.
     *
     * @param document The wrapped document.
     * @param text Initial text content.
     * @param rows Number of rows in preferred size.
     * @param columns Number of columns in preferred size.
     */
    public KnimeSyntaxTextArea(final RSyntaxDocument document, final String text, final int rows, final int columns) {
        super(document, text, rows, columns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() {
        super.init();
        try {
            applySyntaxColors();
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }

        ToolTipManager.sharedInstance().registerComponent(this);
        LanguageSupportFactory.get().register(this);
    }

    /**
     * Loads the theme from the {@link JSnippetTextArea}'s class package's {@code eclipse.xml} file.
     *
     * @throws IOException Problem with loading.
     */
    protected void applySyntaxColors() throws IOException {
        InputStream in = JSnippetTextArea.class.getResourceAsStream("eclipse.xml");
        Theme theme = Theme.load(in);
        theme.apply(this);
    }
}