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
 *   Mar 16, 2012 (wiswedel): created
 */
package org.knime.core.data;

import java.util.Map;

/** Interface defining access on {@link AdapterCell}.
 *
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement Use {@link AdapterCell} instead.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.7
 */
public interface AdapterValue extends DataValue {
    /**
     * Returns whether this adapter value is an adapter for the given value class. By convention an adapter is also
     * adaptable to all value classes that it is compatible with (i.e. that the cell implementation implements).
     *
     * @param valueClass a value class
     * @return <code>true</code> if it is adaptable, <code>false</code> otherwise
     * @see DataType#isCompatible(Class)
     */
    public <V extends DataValue> boolean isAdaptable(final Class<V> valueClass);

    /**
     * Returns the value in this adapter for the given value class. The value is also returned if this adapter is
     * compatible with the given value class.
     *
     * @param valueClass a value class
     * @return a value object or <code>null</code> if an adapter error is stored instead of a value
     * @see #getAdapterError(Class)
     * @see DataType#isCompatible(Class)
     * @throws IllegalArgumentException if this value is not adaptable to the given value class
     */
    public <V extends DataValue> V getAdapter(final Class<V> valueClass);

    /**
     * Returns a missing value object if there is one for the given value class. If no error is available
     * <code>null</code> is returned.
     *
     * @param valueClass a value class
     * @return a missing value or <code>null</code>
     */
    public <V extends DataValue> MissingValue getAdapterError(final Class<V> valueClass);

    /**
     * Returns a read-only map of all adapters. The mapped cells are special instances with blob support. This
     * method is not meant to be used outside the KNIME core plugin.

     * @return a read-only adapter map
     * @noreference This method is not intended to be referenced by clients.
     */
    public Map<Class<? extends DataValue>, DataCell> getAdapterMap();
}
