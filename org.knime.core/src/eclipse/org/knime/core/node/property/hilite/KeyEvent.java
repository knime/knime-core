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
 * --------------------------------------------------------------------- *
 *
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.RowKey;


/**
 * Event object that is fired when registered listener need to update its
 * properties. An event keeps an unmodifiable set of row keys as
 * {@link RowKey}.
 *
 * @see HiLiteHandler
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class KeyEvent extends EventObject {
    private static final long serialVersionUID = -5555018973664128867L;

    /** Internal unmodifiable set of row IDs. */
    private final Set<RowKey> m_keys;

    /**
     * Creates an empty key event with the given source.
     * @param src the source of this key event
     */
    @SuppressWarnings("unchecked")
    public KeyEvent(final Object src) {
        this(src, Collections.EMPTY_SET);
    }

    /**
     * Creates a new event with the underlying source and one data cell.
     *
     * @param src the object on which the event initially occurred
     * @param ids an array of  <code>RowKey</code> elements for which this
     *         event is created.
     * @throws NullPointerException if the key array is null
     * @throws IllegalArgumentException if key array contains null elements
     *
     * @see java.util.EventObject#EventObject(Object)
     */
    public KeyEvent(final Object src, final RowKey... ids) {
        this(src, new LinkedHashSet<RowKey>(Arrays.asList(ids)));
    }

    /**
     * Creates a new event with the underlying source and a set of row keys.
     *
     * @param src the object on which the event initially occurred
     * @param ids a set of <code>RowKey</code> row IDs for which the
     *         event is created.
     * @throws NullPointerException if the key set is null
     * @throws IllegalArgumentException if key array contains null elements
     *
     * @see java.util.EventObject#EventObject(Object)
     */
    public KeyEvent(final Object src, final Set<RowKey> ids) {
        super(src);
        if (ids.contains(null)) {
            throw new IllegalArgumentException(
                    "KeyEvent must not contains null elements.");
        }
        m_keys = Collections.unmodifiableSet(ids);
    }

    /**
     * Returns the set of <code>RowKey</code> row keys on which the event
     * initially occurred.
     *
     * @return a set of row IDs
     */
    public Set<RowKey> keys() {
        return m_keys;
    }

    /**
     * @return true, if the key event does not contain any keys
     */
    public boolean isEmpty() {
        return m_keys.isEmpty();
    }

}
