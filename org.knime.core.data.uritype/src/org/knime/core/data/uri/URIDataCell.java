/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Oct 4, 2011 (wiswedel): created
 */
package org.knime.core.data.uri;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class URIDataCell extends DataCell implements URIDataValue {

    /**
     * Serial id.
     */
    private static final long serialVersionUID = -1878057164912214296L;

    /**
     * Convenience access member for
     * <code>DataType.getType(URIDataValue.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(URIDataCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     *
     *
     * @return URIDataValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return URIDataValue.class;
    }

    /**
     * Serializer for this class.
     */
    public static final URIDataCellSerializer SERIALIZER =
            new URIDataCellSerializer();

    private final URIContent m_uriContent;

    /**
     * @param uriContent Content of this cell. Must not be null.
     */
    public URIDataCell(final URIContent uriContent) {
        if (uriContent == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_uriContent = uriContent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URIContent getURIContent() {
        return m_uriContent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getURIContent().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        URIContent oCnt = ((URIDataCell)dc).getURIContent();
        return oCnt.equals(getURIContent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getURIContent().hashCode();
    }

    private static final class URIDataCellSerializer implements
            DataCellSerializer<URIDataCell> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final URIDataCell cell,
                final DataCellDataOutput output) throws IOException {
            output.writeInt(0); // version
            URIContent cnt = cell.getURIContent();
            cnt.save(output);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public URIDataCell deserialize(final DataCellDataInput input)
                throws IOException {
            input.readInt(); // version
            URIContent cnt = URIContent.load(input);
            return new URIDataCell(cnt);
        }

    }

}
