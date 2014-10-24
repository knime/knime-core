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
 *   16.12.2010 (hofer): created
 */
package org.knime.core.data.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.json.JsonValue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.NodeLogger;

/**
 * Cell factory that creates JSON cells. Depending on the size either normal {@link DataCell}s or {@link BlobDataCell}s
 * are created. Use this class to create {@link JSONValue} {@link DataCell}s. <br/>
 * Based on {@link XMLCellFactory}.
 *
 * @since 2.11
 *
 * @author Heiko Hofer
 * @author Gabor Bakos
 */
public final class JSONCellFactory {
    /**
     * Minimum size for blobs in bytes. That is, if a given string is at least as large as this value, it will be
     * represented by a blob cell
     */
    public static final int DEF_MIN_BLOB_SIZE_IN_BYTES = 8 * 1024;

    private static final int MIN_BLOB_SIZE_IN_BYTES;

    static {
        int size = DEF_MIN_BLOB_SIZE_IN_BYTES;
        String envVar = "org.knime.jsonminblobsize";
        String property = System.getProperty(envVar);
        if (property != null) {
            String s = property.trim();
            int multiplier = 1;
            if (s.endsWith("m") || s.endsWith("M")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024 * 1024;
            } else if (s.endsWith("k") || s.endsWith("K")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024;
            }
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new NumberFormatException("Size < 0" + newSize);
                }
                size = newSize * multiplier;
                NodeLogger.getLogger(JSONCellFactory.class).debug(
                    "Setting min blob size for JSON cells to " + size + " bytes");
            } catch (NumberFormatException e) {
                NodeLogger.getLogger(JSONCellFactory.class).warn(
                    "Unable to parse property " + envVar + ", using default", e);
            }
        }
        MIN_BLOB_SIZE_IN_BYTES = size;
    }

    /** Type for JSON cells. */
    public static final DataType TYPE = DataType.getType(JSONCell.class);

    /** Don't instantiate this class. */
    private JSONCellFactory() {
        //// private constructor prevents that an instance is created
    }

    /**
     * Factory method to create {@link DataCell} representing {@link JsonValue}. The returned cell is either of type
     * {@link JSONCell} (for small documents) or {@link JSONBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     *
     * @param json String representing the JSON
     * @param allowComments allow or not comments in {@code json}
     * @return DataCell representing the JSON
     * @throws IOException if an io error occurs while reading the JSON string
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final String json, final boolean allowComments) throws IOException {
        if (json == null) {
            throw new NullPointerException("JSON must not be null");
        }
        final Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(JSONCellFactory.class.getClassLoader());
        JSONCellContent content;
        try {
            content = new JSONCellContent(json, true, allowComments);
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }
        if (json.length() >= MIN_BLOB_SIZE_IN_BYTES) {
            return new JSONBlobCell(content);
        } else {
            return new JSONCell(content);
        }
    }

    /**
     * Factory method to create {@link DataCell} representing {@link JsonValue}. The returned cell is either of type
     * {@link JSONCell} (for small documents) or {@link JSONBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger). <br/>
     * It does not allow comments in {@code json}.
     *
     * @param json the returned data cell encapsulates this {@link JsonValue}.
     * @return DataCell representing the JSON
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final JsonValue json) {
        if (json == null) {
            throw new NullPointerException("JSON must not be null");
        }
        JSONCellContent content = new JSONCellContent(json);
        if (content.getStringValue().length() >= MIN_BLOB_SIZE_IN_BYTES) {
            return new JSONBlobCell(content);
        } else {
            return new JSONCell(content);
        }

    }

    /**
     * Factory method to create {@link DataCell} representing {@link JsonValue}. The returned cell is either of type
     * {@link JSONCell} (for small documents) or {@link JSONBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     *
     * @param is The stream containing the JSON
     * @param allowComments allow or not comments in {@code is}'s JSON
     * @return DataCell representing the JSON
     * @throws IOException if an io error occurs while reading the JSON string
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final InputStream is, final boolean allowComments) throws IOException {
        if (is == null) {
            throw new NullPointerException("InputStream must not be null");
        }
        JSONCellContent content = new JSONCellContent(is, allowComments);
        if (content.getStringValue().length() >= MIN_BLOB_SIZE_IN_BYTES) {
            return new JSONBlobCell(content);
        } else {
            return new JSONCell(content);
        }
    }

    /**
     * Factory method to create {@link DataCell} representing {@link JsonValue}. The returned cell is either of type
     * {@link JSONCell} (for small documents) or {@link JSONBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     *
     * @param reader The reader containing the JSON
     * @param allowComments allow or not comments in {@code reader}'s JSON
     * @return DataCell representing the JSON
     * @throws IOException if an io error occurs while reading the JSON string
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final Reader reader, final boolean allowComments) throws IOException {
        if (reader == null) {
            throw new NullPointerException("InputStream must not be null");
        }
        JSONCellContent content = new JSONCellContent(reader, allowComments);
        if (content.getStringValue().length() >= MIN_BLOB_SIZE_IN_BYTES) {
            return new JSONBlobCell(content);
        } else {
            return new JSONCell(content);
        }
    }

    /**
     * Factory method to create {@link DataCell} representing {@link JsonValue}. The returned cell is either of type
     * {@link JSONCell} (for small documents) or {@link JSONBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     *
     * @param json The cell represents this value.
     * @return DataCell representing the JSON
     * @throws NullPointerException if argument is null
     */
    public static DataCell create(final JSONValue json) {
        if (json == null) {
            throw new NullPointerException("JSONValue must not be null");
        }
        if (json instanceof DataCell) {
            return (DataCell)json;
        } else {
            JSONCellContent content = new JSONCellContent(json.getJsonValue());
            if (content.getStringValue().length() >= MIN_BLOB_SIZE_IN_BYTES) {
                return new JSONBlobCell(content);
            } else {
                return new JSONCell(content);
            }
        }
    }
}
