/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */

package org.knime.core.data.convert.java;

import org.knime.core.data.DataValue;

/**
 * Interface for all factory classes which create {@link DataCellToJavaConverter DataCellToJavaConverters}.
 *
 * You may derive this class and register your derived class under the extension point
 * {@link DataCellToJavaConverterRegistry#EXTENSION_POINT_ID}. It is recommended that you extend
 * {@link SimpleDataCellToJavaConverterFactory} if possible.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 *
 * @param <S> type which the created {@link DataCellToJavaConverter}s can convert
 * @param <D> type which the created {@link DataCellToJavaConverter}s convert to
 * @since 3.2
 * @see org.knime.core.data.convert
 * @see DataCellToJavaConverter
 * @see DataCellToJavaConverterRegistry
 */
public interface DataCellToJavaConverterFactory<S extends DataValue, D> {

    /**
     * Create an instance of a {@link DataCellToJavaConverter} which is able to convert <code>S</code> into
     * <code>D</code>.
     *
     * @return a {@link DataCellToJavaConverter} which converts an instance of the type returned by
     *         {@link #getSourceType()} into an instance of the type returned by {@link #getDestinationType()}
     */
    public DataCellToJavaConverter<S, D> create();

    /**
     * Get the {@link Class} of the type (a subclass of {@link DataValue}, see type parameter {@code <S>}) which
     * converters created by this factory are able to convert.
     *
     * @return type which the created {@link DataCellToJavaConverter}s can convert
     */
    public Class<S> getSourceType();

    /**
     * Get the {@link Class} of the type which converters created by this factory are able to convert into.
     *
     * @return type which the created {@link DataCellToJavaConverter}s convert to
     */
    public Class<D> getDestinationType();

    /**
     * A human readable name for this converter factory to be displayed in user interfaces for example. Should contain
     * at least the simple name of the java type which is retrieved from the data value using this method.
     * <p>
     * <b> Examples: </b> "Double", "String", "JsonValue", "String (SDF)"
     * </p>
     *
     * @return the name of this converter factory
     */
    default String getName() {
        return getDestinationType().getSimpleName();
    }

    /**
     * Get the identifier for this factory. The identifier is a unique string used to unambiguously reference this
     * converter factory. Since this identifier is used for persistence, it is required that the identifier is the same
     * every runtime. If the identifier is not unique, the factory may not be loaded from the extension point.
     *
     * <p>
     * <b>Examples:</b>
     * </p>
     * <p>
     * "org.mypackage.MyConverterFactory&lt;MySourceDataValue,MyType>"
     * </p>
     *
     * @return a unique identifier for this factory
     */
    public String getIdentifier();
}
