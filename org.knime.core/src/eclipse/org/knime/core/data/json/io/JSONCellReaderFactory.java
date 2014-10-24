/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   09.03.2011 (hofer): created
 */
package org.knime.core.data.json.io;

import java.io.InputStream;
import java.io.Reader;

import org.knime.core.data.xml.io.XMLCellReaderFactory;

/**
 * Factory class for {@link JSONCellReader}. <br/>
 * Based on {@link XMLCellReaderFactory}.
 *
 * @since 2.11
 *
 * @author Gabor Bakos
 * @author Heiko Hofer
 */
public class JSONCellReaderFactory {
    /**
     * Creates a {@link JSONCellReader} to read a single cell from given
     * 
     * @link{InputStream . <br/>
     *                   It does not allow comments within JSON content.
     *
     * @param is the JSON document
     * @return {@link JSONCellReader} to read a single cell from given {@link InputStream} using the default
     *         {@code UTF-8} encoding.
     */
    public static JSONCellReader createJSONCellReader(final InputStream is) {
        return new JSONNodeCellReader(is);
    }

    /**
     * Creates a {@link JSONCellReader} to read a single cell from given
     * 
     * @link{InputStream .
     *
     * @param is the JSON document
     * @param allowComments allow or not comments in the document
     * @return {@link JSONCellReader} to read a single cell from given {@link InputStream} using the default
     *         {@code UTF-8} encoding.
     */
    public static JSONCellReader createJSONCellReader(final InputStream is, final boolean allowComments) {
        return new JSONNodeCellReader(is, allowComments);
    }

    /**
     * Creates a {@link JSONCellReader} to read a single cell from given {@link Reader}. <br/>
     * It does not allow comments within the JSON documents.
     *
     * @param reader a reader for the JSON document
     * @return @link{JSONCellReader} to read a single cell from given
     * @link{InputStream .
     */
    public static JSONCellReader createJSONCellReader(final Reader reader) {
        return new JSONNodeCellReader(reader);
    }

    /**
     * Creates a {@link JSONCellReader} to read a single cell from given {@link Reader}
     *
     * @param reader a reader for the JSON document
     * @param allowComments allow or not comments in the document
     * @return @link{JSONCellReader} to read a single cell from given
     * @link{InputStream .
     */
    public static JSONCellReader createJSONCellReader(final Reader reader, final boolean allowComments) {
        return new JSONNodeCellReader(reader, allowComments);
    }
}
