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
 * ---------------------------------------------------------------------
 *
 * Created on Sep 12, 2012 by wiswedel
 */
package org.knime.core.data.blob;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.Icon;

import org.apache.commons.io.IOUtils;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.convert.DataValueAccessMethod;
import org.knime.core.node.util.SharedIcons;

/** Implemented by cell elements that are binary objects (BLOB).
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.7
 */
public interface BinaryObjectDataValue extends DataValue {
    /** Meta information to type. */
    UtilityFactory UTILITY = new BinaryObjectUtilityFactory();

    /** Length in bytes of the binary object. It's only a hint on how many bytes are returned by the
     * {@link #openInputStream()} method.
     * @return The number of bytes in the stream.
     */
    long length();

    /** Opens a new input stream on the byte content.
     * @return A new input stream on the byte content, not null.
     * @throws IOException If that fails for whatever I/O problems.
     */
    @DataValueAccessMethod(name = "InputStream")
    InputStream openInputStream() throws IOException;

    /** Implementations of the meta information of this value class. */
    final class BinaryObjectUtilityFactory extends ExtensibleUtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON = SharedIcons.TYPE_BLOB.get();

        /**
         * Constructor.
         */
        protected BinaryObjectUtilityFactory() {
            super(BinaryObjectDataValue.class);
        }

        /** {@inheritDoc} */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /** {@inheritDoc} */
        @Override
        protected DataValueComparator getComparator() {
            return new DataValueComparator() {

                @Override
                protected int compareDataValues(final DataValue v1, final DataValue v2) {
                    BinaryObjectDataValue b1 = (BinaryObjectDataValue)v1;
                    BinaryObjectDataValue b2 = (BinaryObjectDataValue)v2;
                    long b1Length = b1.length();
                    long b2Length = b2.length();
                    return b1Length < b2Length ? -1 : (b1Length == b2Length ? 0 : 1);
                }
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "Binary Object";
        }

        @Override
        protected String[] getLegacyNames() {
            return new String[]{"Binary object"};
        }
    }

    /**
     * Returns whether the two data values have the same content.
     *
     * @param v1 the first data value
     * @param v2 the second data value
     * @return <code>true</code> if both values are equal, <code>false</code> otherwise
     * @throws IOException if an I/O error occurs while opening the binary input streams
     * @since 3.0
     */
    static boolean equalContent(final BinaryObjectDataValue v1, final BinaryObjectDataValue v2) throws IOException {
        try (InputStream is1 = v1.openInputStream(); InputStream is2 = v2.openInputStream()) {
            return IOUtils.contentEquals(is1, is2);
        }
    }
}
