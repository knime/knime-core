/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.data;

import java.io.IOException;

import org.knime.core.internal.SerializerMethodLoader.Serializer;

/**
 * Interface for classes that can read or write specific 
 * {@link org.knime.core.data.DataCell}
 * implementations. Using <code>DataCellSerializer</code> implementations are
 * normally considerably faster than ordinary Java serialization. Objects of
 * this class are returned by a static method in a 
 * {@link org.knime.core.data.DataCell} implementation. For further details see 
 * the {@link org.knime.core.data.DataCell} description and the <a
 * href="doc-files/newtypes.html#newtypes">manual</a> on how to define new
 * types.
 * 
 * @param <T> a {@link org.knime.core.data.DataValue} implementation being read 
 * or written
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataCellSerializer<T extends DataCell> extends Serializer<T> {

    /**
     * Saves the <code>cell</code> to the <code>output</code> stream.
     * 
     * @param cell the <code>DataCell</code> to save
     * @param output the place to write to
     * @throws IOException if writing fails
     */
    void serialize(final T cell, final DataCellDataOutput output) 
        throws IOException;

    /**
     * Loads a new instance of {@link org.knime.core.data.DataCell} of type 
     * <code>T</code> from the <code>input</code> stream, which represents a 
     * former serialized <code>DataCell</code> content.
     * 
     * @param input the source to load from, never <code>null</code>
     * @return a new {@link org.knime.core.data.DataValue} instance of type 
     *         <code>T</code> representing a former serialized 
     *         <code>DataCell</code>
     * @throws IOException if loading fails
     */
    T deserialize(final DataCellDataInput input) throws IOException;
 
}
