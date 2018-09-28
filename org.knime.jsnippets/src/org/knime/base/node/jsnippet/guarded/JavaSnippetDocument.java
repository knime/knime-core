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
 *   05.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.guarded;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.knime.core.node.util.rsyntaxtextarea.guarded.GuardedSection;

/**
 * The document used in the jsnippet dialogs.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("serial")
public class JavaSnippetDocument extends GuardedDocument {
    /** The name of the guarded section for imports. */
    public static final String GUARDED_IMPORTS = "imports";
    /** The name of the guarded section for fields. */
    public static final String GUARDED_FIELDS = "fields";
    /** The name of the guarded section for the start of the body. */
    public static final String GUARDED_BODY_START = "bodyStart";
    /** The name of the guarded section for the end of the body. */
    public static final String GUARDED_BODY_END = "bodyEnd";

    /**
     * Create a new instance.
     *
     * @param methodSignature e.g. <code>public JavaRDD&ltRow> apply(JavaRDD&ltRow> rowRDD1) throws GenericKnimeSparkException</code>
     */
    public JavaSnippetDocument(final String methodSignature) {
        super(SyntaxConstants.SYNTAX_STYLE_NONE);
        try {
            addGuardedSection(GUARDED_IMPORTS, getLength());
            insertString(getLength(), " \n", null);

            addGuardedSection(GUARDED_FIELDS, getLength());
            insertString(getLength(), " \n", null);

            final org.knime.core.node.util.rsyntaxtextarea.guarded.GuardedSection bodyStart = addGuardedSection(GUARDED_BODY_START, getLength());
            bodyStart.setText("// expression start\n    " + methodSignature + " {\n");
            insertString(getLength(), " \n", null);

            final GuardedSection bodyEnd = addGuardedFooterSection(GUARDED_BODY_END, getLength());
            bodyEnd.setText("// expression end\n" + "    }\n" + "}");
        } catch (BadLocationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void insertString(final int offset, final String str,
            final AttributeSet a)
            throws BadLocationException {
        if (getBreakGuarded()) {
            super.insertString(offset, str, a);
        } else {
            // HACK when string is an import declaration as created by auto completion.
            if (getGuardedSection(GUARDED_IMPORTS).contains(offset) && str.startsWith("\nimport ")) {
                // change offset so that the import is inserted in the user imports.
                int min = getGuardedSection(GUARDED_IMPORTS).getEnd().getOffset() + 1;
                int pos = getGuardedSection(GUARDED_FIELDS).getStart().getOffset() - 1;
                try {
                    while (getText(pos, 1).equals("\n") && getText(pos - 1, 1).equals("\n") && pos > min) {
                        pos--;
                    }
                    super.insertString(pos, str, a);
                } catch (BadLocationException e) {
                    // do nothing, not critical
                }
            } else {
                super.insertString(offset, str, a);
            }
        }
    }

}
