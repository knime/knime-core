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
 * History
 *   20.08.2015 (thor): created
 */
package org.knime.core.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;

import org.knime.core.node.ExecutionContext;

/**
 * Interface for a factory that can create data cells from certain input formats. The factory is free to create any
 * compatible cell, e.g. normal or blob cell. Such factories are mainly used by reader node in order to create arbitrary
 * cells based on the user-supplied node configuration. A new factory is created every time a node requests one via the
 * {@link DataType#getCellFactory(ExecutionContext)}.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients. Instead implement the special interfaces
 *              such as {@link FromSimpleString} or {@link FromInputStream}.
 */
public interface DataCellFactory {
    /**
     * Factory that creates cells from strings. <b>This interface is not meant for direct implementation. Implement
     * {@link FromSimpleString} or {@link FromComplexString} instead.</b>
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     * @noimplement This interface is not intended to be implemented by clients, implement {@link FromSimpleString}
     *              and/or {@link FromComplexString} instead
     */
    public interface FromString extends DataCellFactory {
        /**
         * Creates a new data cell from a string.
         *
         * @param input a string, never <code>null</code>
         * @return a new data cell
         * @throws IllegalArgumentException if the string cannot be converted into a data cell
         */
        DataCell createCell(String input);
    }

    /**
     * Factory that creates cells from simple, usually single-line, strings.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     */
    public interface FromSimpleString extends FromString {
    }

    /**
     * Factory that creates cells from complex, usually multi-line, string representations.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     */
    public interface FromComplexString extends FromString {
    }

    /**
     * Factory that creates cells from an binary input stream. When reading character data consider using
     * {@link FromReader} instead or at least assume UTF-8 encoding.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     */
    public interface FromInputStream extends DataCellFactory {
        /**
         * Creates a new data cell from an input stream.
         *
         * @param input an input stream, never <code>null</code>
         * @return a new data cell
         * @throws IllegalArgumentException if the stream contents cannot be converted into a data cell
         * @throws IOException if an I/O error occurs
         */
        DataCell createCell(InputStream input) throws IOException;
    }

    /**
     * Factory that creates cells from an character reader.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     */
    public interface FromReader extends DataCellFactory {
        /**
         * Creates a new data cell from a reader.
         *
         * @param input a reader, never <code>null</code>
         * @return a new data cell
         * @throws ParseException if the reader contents cannot be converted into a data cell
         * @throws IOException if an I/O error occurs
         */
        DataCell createCell(Reader input) throws ParseException, IOException;
    }

    /**
     * Returns the data type of the cells that this factory will create.
     *
     * @return a data type, never <code>null</code>
     */
    DataType getDataType();

    /**
     * This method is called once by {@link DataTypeRegistry} when a factory instance is created. The default
     * implementation does nothing but implementors can override it a make use of the passed execution context. Note
     * that this method may not be called or called with a <code>null</code> argument. In this case it's allowed to fail
     * when an attempt to create a cell is made and an execution context is required.
     *
     * @param execContext the current node's execution context, may be <code>null</code>
     * @noreference This method is not intended to be referenced by clients.
     */
    default void init(final ExecutionContext execContext) {
        // no nothing
    }
}
