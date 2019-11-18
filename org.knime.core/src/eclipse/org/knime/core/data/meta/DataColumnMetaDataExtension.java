/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 25, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.meta;

import org.knime.core.data.DataValue;

/**
 * Bundles a {@link DataColumnMetaData} type with its {@link DataColumnMetaDataCreator} and
 * {@link DataColumnMetaDataSerializer}.<br/>
 * This class is registered at the MetaDataType extension point.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type of {@link DataColumnMetaData} this {@link DataColumnMetaDataExtension} is concerned with
 * @noreference This interface is not intended to be referenced by clients. Pending API.
 * @noextend This interface is not intended to be extended by clients. Pending API.
 * @noimplement This interface is not intended to be implemented by clients. Pending API.
 * @since 4.1
 */
public interface DataColumnMetaDataExtension<T extends DataColumnMetaData> extends DataColumnMetaDataFramework<T> {

    /**
     * Provides the class of {@link DataValue} that a {@link DataColumnMetaDataCreator} created by this object can
     * consume in order to create a {@link DataColumnMetaData} object.
     *
     * @return the type of {@link DataValue} the {@link DataColumnMetaData} associated with this factory is concerned
     *         with
     */
    Class<? extends DataValue> getDataValueClass();

    /**
     * Creates a fresh {@link DataColumnMetaData} that can consume cells implementing the {@link DataValue} class
     * returned by {@link DataColumnMetaDataExtension#getDataValueClass()}.
     *
     * @return a fresh {@link DataColumnMetaDataCreator} instance
     */
    DataColumnMetaDataCreator<T> create();

    /**
     * Creates a {@link DataColumnMetaDataSerializer} that can be used to serialize {@link DataColumnMetaData} objects
     * of the type associated with this class.
     *
     * @return creates a {@link DataColumnMetaDataSerializer}
     */
    DataColumnMetaDataSerializer<T> createSerializer();

}
