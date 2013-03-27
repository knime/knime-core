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
 *   05.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.guarded;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;

/**
 * A Document with guarded, non editable areas.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class GuardedDocument extends RSyntaxDocument {
    private Map<String, GuardedSection> m_guards;
    private boolean m_breakGuarded;


    /**
     * Constructs a plain text document.  A default root element is created,
     * and the tab size set to 5.
     *
     * @param syntaxStyle The syntax highlighting scheme to use.
     */
    public GuardedDocument(final String syntaxStyle) {
        super(syntaxStyle);
        m_guards = new LinkedHashMap<String, GuardedSection>();
    }

    /**
     * Returns true guarded areas can be edited.
     *
     * @return the break guarded property
     */
    public boolean getBreakGuarded() {
        return m_breakGuarded;
    }

    /**
     * Set property if guarded areas can be edited.
     *
     * @param breakGuarded the break guarded property to set
     */
    public void setBreakGuarded(final boolean breakGuarded) {
        this.m_breakGuarded = breakGuarded;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void insertString(final int offset, final String str,
            final AttributeSet a)
            throws BadLocationException {
        if (m_breakGuarded) {
            super.insertString(offset, str, a);
        } else {
            // Check if pos is within a guarded section
            for (GuardedSection gs : m_guards.values()) {
                if (gs.contains(offset)) {
                    throw new BadLocationException(
                            "Cannot insert text in guarded section.", offset);
                }
            }
            super.insertString(offset, str, a);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final int offset, final int len)
        throws BadLocationException {
        if (m_breakGuarded) {
            super.remove(offset, len);
        } else {
            // check if a guarded section intersects with [offset, offset+len]
            for (GuardedSection gs : m_guards.values()) {
                if (gs.intersects(offset, len)) {
                    throw new BadLocationException("Cannot remove text "
                            + "that intersects with a guarded section.",
                            offset);
                }
            }
            super.remove(offset, len);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void replace(final int offset, final int length, final String text,
            final AttributeSet attrs) throws BadLocationException {
        if (m_breakGuarded) {
            super.replace(offset, length, text, attrs);
        } else {
            // check if a guarded section intersects with [offset, offset+len]
            for (GuardedSection gs : m_guards.values()) {
                if (gs.intersects(offset, length)) {
                    throw new BadLocationException("Cannot replace text "
                            + "that intersects with a guarded section.",
                            offset);
                }
            }
            super.replace(offset, length, text, attrs);
        }
    }

    /**
     * Replaces the text between two subsequent guarded sections.
     *
     * @param guard1 the first guarded section
     * @param guard2 the second guarded section
     * @param s the string to replace with
     * @throws BadLocationException when the guarded sections do not exist,
     * when they are not subsequent guarded sections or when there is no
     * character between the guarded sections.
     */
    public void replaceBetween(final String guard1, final String guard2,
            final String s) throws BadLocationException {
        int start = getGuardedSection(guard1).getEnd().getOffset();
        int end = getGuardedSection(guard2).getStart().getOffset();
        if (end < start) {
            throw new BadLocationException("The offset of the first guarded"
                    + " section is greaten than the offset of the second"
                    + " guarded section.", start);
        }

        int offset = start + 1;
        int length = end - start - 2;

        replace(offset, length, s, null);
    }

    /**
     * Get the text between two subsequent guarded sections.
     *
     * @param guard1 the first guarded section
     * @param guard2 the second guarded section
     * @return the string between the given guarded sections
     * @throws BadLocationException when the guarded sections do not exist,
     * when they are no subsequent guarded sections.
     */
    public String getTextBetween(final String guard1, final String guard2)
    throws BadLocationException {
        int start = getGuardedSection(guard1).getEnd().getOffset();
        int end = getGuardedSection(guard2).getStart().getOffset();
        if (end < start) {
            throw new BadLocationException("The offset of the first guarded"
                    + " section is greaten than the offset of the second"
                    + " guarded section.", start);
        }

        int offset = start + 1;
        int length = end - start - 2;
        if (m_breakGuarded) {
            return getText(offset, length);
        } else {
            // check if a guarded section intersects with [offset, offset+len]
            for (GuardedSection gs : m_guards.values()) {
                if (gs.intersects(offset, length)) {
                    throw new BadLocationException("Cannot replace text "
                            + "that intersects with a guarded section.",
                            offset);
                }
            }
            return getText(offset, length);
        }
    }


    /**
     * Add a named guarded section to the document. Note that text can always
     * be inserted after the guarded section. To prevent this use the method
     * addGuardedFootterSection(...).
     *
     * @param name the name of the guarded section
     * @param offset the offset of the section (start point)
     * @return the newly created guarded section
     * @throws BadLocationException if offset is in a guarded section
     */
    public GuardedSection addGuardedSection(final String name, final int offset)
        throws BadLocationException {
        return doAddGuardedSection(name, offset, false);
    }

    /**
     * Add a named guarded section to the document. No text can be inserted
     * right after this guarded section.
     *
     * @param name the name of the guarded section
     * @param offset the offset of the section (start point)
     * @return the newly created guarded section
     * @throws BadLocationException if offset is in a guarded section
     */
    public GuardedSection addGuardedFooterSection(final String name,
            final int offset)
        throws BadLocationException {
        return doAddGuardedSection(name, offset, true);
    }

    /** Add a named guarded section to the document. */
    private GuardedSection doAddGuardedSection(final String name,
            final int offset, final boolean isFooter)
            throws BadLocationException {
        for (GuardedSection gs : m_guards.values()) {
            if (gs.getStart().getOffset() < offset
                    && gs.getEnd().getOffset() > offset) {
                throw new IllegalArgumentException(
                        "Guarded sections may not overlap.");
            }
        }
        GuardedSection gs = m_guards.get(name);
        if (gs != null) {
            throw new IllegalArgumentException(
                    "Guarded section with name \"" + name
                    + "\" does already exist.");
        }
        boolean orig = getBreakGuarded();
        setBreakGuarded(true);
        this.insertString(offset, " \n", null);
        setBreakGuarded(orig);

        GuardedSection guard = isFooter
            ? GuardedSection.createFooter(
                this.createPosition(offset),
                this.createPosition(offset + 1),
                this)
            : GuardedSection.create(
                this.createPosition(offset),
                this.createPosition(offset + 1),
                this);
        m_guards.put(name, guard);


        return guard;
    }


    /**
     * Retrieve guarded section by its name.
     *
     * @param name the name of the guarded section
     * @return the guarded section or null if a guarded section with the
     * given name does not exist
     */
    public GuardedSection getGuardedSection(final String name) {
        return m_guards.get(name);
    }


    /**
     * Get the list of guarded sections.
     *
     * @return the list of guarded sections.
     */
    public Set<String> getGuardedSections() {
        return m_guards.keySet();
     }



}
