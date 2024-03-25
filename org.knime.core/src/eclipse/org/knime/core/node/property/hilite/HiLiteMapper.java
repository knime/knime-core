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
 * -------------------------------------------------------------------
 *
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.RowKey;

/**
 * This mapper has to be implemented by all classes that are interested in
 * mapping hilite events between {@link RowKey}s.
 *
 * @see HiLiteTranslator
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public interface HiLiteMapper {

    /**
     * Returns a set of <code>RowKey</code> elements which are associated
     * by the specified <b>key</b> or <code>null</code> if no mapping
     * is available.
     *
     * @param key the key to get the mapping for
     * @return a set of mapped <code>RowKey</code> elements
     */
    Set<RowKey> getKeys(RowKey key);

    /**
     * Returns an unmodifiable set of key (source) for hiliting.
     * @return A set of keys to hilite.
     */
    Set<RowKey> keySet();

    /**
     * Used to propagate events downstream.
     *
     * @param hilitInputKeys to compute hilit output keys for
     * @return the union of the row key sets that the input keys are mapped to
     */
    default Set<RowKey> applyUnion(final Iterable<RowKey> hilitInputKeys) {
        Set<RowKey> fireSet = new LinkedHashSet<>();
        for (RowKey key : hilitInputKeys) {
            Set<RowKey> s = getKeys(key);
            if (s != null && !s.isEmpty()) {
                fireSet.addAll(s);
            }
        }
        return fireSet;
    }

    /**
     * Used to propagate events upstream.
     *
     * @param hilitOutputKeys to compute hilit input keys for
     * @return all input keys whose associated output keys are all hilit
     */
    default Set<RowKey> inverseCovered(final Collection<RowKey> hilitOutputKeys) {
        final var covered = new LinkedHashSet<RowKey>();
        for (RowKey key : keySet()) {
            final var requiredKeys = getKeys(key);
            if (hilitOutputKeys.containsAll(requiredKeys)) {
                covered.add(key);
            }
        }
        return covered;
    }

}
