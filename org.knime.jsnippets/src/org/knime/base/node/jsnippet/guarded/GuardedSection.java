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
 *   06.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.guarded;

import javax.swing.text.Position;

import org.knime.core.node.util.rsyntaxtextarea.guarded.GuardedDocument;

/**
 * A guarded, e.g. non editable section in a document.
 * <p>
 * This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 * @deprecated Use {@link org.knime.core.node.util.rsyntaxtextarea.guarded.GuardedSection} instead.
 */
@Deprecated
public final class GuardedSection extends org.knime.core.node.util.rsyntaxtextarea.guarded.GuardedSection {

    /* empty implementation for backwards compatibility */

    private GuardedSection(final Position start, final Position end, final GuardedDocument document,
        final boolean footer) {
        super(start, end, document, footer);
    }

    /**
     * Creates a guarded section in the document. Note that text can always be inserted after the guarded section. To
     * prevent this use the method addGuardedFootterSection(...).
     *
     * @param start the start of the guarded section
     * @param end the end point of the guarded section
     * @param document the document
     * @return the newly created guarded section
     * @deprecated
     */
    @Deprecated
    public static GuardedSection create(final Position start, final Position end, final GuardedDocument document) {
        return new GuardedSection(start, end, document, false);
    }

    /**
     * Creates a guarded section in the document. No text can be inserted right after this guarded section.
     *
     * @param start the start of the guarded section
     * @param end the end point of the guarded section
     * @param document the document
     * @return the newly created guarded section
     * @deprecated
     */
    @Deprecated
    public static GuardedSection createFooter(final Position start, final Position end,
        final GuardedDocument document) {
        return new GuardedSection(start, end, document, true);
    }

}
