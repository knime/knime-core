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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   06.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.guarded;

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;


/**
 * A guarded, e.g. non editable section in a document.
 *
 * @author Heiko Hofer
 */
public final class GuardedSection {

    private Position m_start;
    private Position m_end;
    private final GuardedDocument m_document;
    private int m_endOffset;

    /**
     * Creates new <code>GuardedSection</code>.
     *
     * @param start the start position of the range
     * @param end the end position of the range
     * @param document the document this section belongs to
     */
    private GuardedSection(final Position start, final Position end,
            final GuardedDocument document, final boolean footer) {
        this.m_start = start;
        this.m_end = end;
        this.m_document = document;
        this.m_endOffset = footer ? 1 : 0;
    }

    /**
     * Creates a guarded section in the document. Note that text can always
     * be inserted after the guarded section. To prevent this use the method
     * addGuardedFootterSection(...).
     *
     * @param start the start of the guarded section
     * @param end the end point of the guarded section
     * @param document the document
     * @return the newly created guarded section
     */
    public static GuardedSection create(final Position start,
            final Position end, final GuardedDocument document) {
        return new GuardedSection(start, end, document, false);
    }

    /**
     * Creates a guarded section in the document. No text can be inserted
     * right after this guarded section.
     *
     * @param start the start of the guarded section
     * @param end the end point of the guarded section
     * @param document the document
     * @return the newly created guarded section
     */
    public static GuardedSection createFooter(final Position start,
            final Position end, final GuardedDocument document) {
        return new GuardedSection(start, end, document, true);
    }

    /**
     * Get the start position.
     *
     * @return the start position
     */
    public Position getStart() {
        return m_start;
    }

    /**
     * Get the end position.
     *
     * @return the end position
     */
    public Position getEnd() {
        return m_end;
    }

    /**
     * Replaces the text of this guarded section.
     *
     * @param t new text to insert over existing text
     * @exception BadLocationException if the positions are out of the bounds
     * of the document
     */
    public void setText(final String t) throws BadLocationException {
        int p1 = m_start.getOffset();
        int p2 = m_end.getOffset();

        boolean orig = m_document.getBreakGuarded();
        m_document.setBreakGuarded(true);
        // Empty text is not allowed, this would break the positioning
        String text = null == t || t.isEmpty() ? " " : t;
        text = text.endsWith("\n")
                ? text.substring(0, text.length() - 1)
                : text;

        int docLen = m_document.getLength();
        m_document.insertString(p1 + 1, text, null);

        // compute length of inserted string
        int len = m_document.getLength() - docLen;
        m_document.remove(p1 + 1 + len, p2 - p1 - 1);
        m_document.remove(p1, 1);
        m_document.setBreakGuarded(orig);
    }

    /**
     * Get the text within the range.
     *
     * @return the text
     * @exception BadLocationException if the positions are out of the
     * bounds of the document
     */
    public String getText() throws BadLocationException {
        int p1 = m_start.getOffset();
        int p2 = m_end.getOffset();
        // for negative length when p1 > p2 => return ""
        return (p1 <= p2) ? m_document.getText(p1, p2 - p1 + 1) : "";
    }

    /**
     * Returns true when offset is in the guarded section.
     *
     * @param offset the offset to test
     * @return true when offset is in the guarded section
     */
    public boolean contains(final int offset) {
        return m_start.getOffset() <= offset
            && m_end.getOffset() + m_endOffset >= offset;
    }

    /**
     * Returns true when the guarded section intersects the given section.
     *
     * @param offset the start point of the section to test
     * @param length the length of the section to test
     * @return true when the guarded section intersects the given section
     */
    public boolean intersects(final int offset, final int length) {
        return m_end.getOffset() + m_endOffset >= offset
            && m_start.getOffset() <= offset + length;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + m_start.getOffset() + ", " + m_end.getOffset() + ")";
    }
}
