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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.ref.SoftReference;

import javax.json.JsonValue;

import org.knime.core.data.json.io.JSONCellReader;
import org.knime.core.data.json.io.JSONCellReaderFactory;
import org.knime.core.data.json.io.JSONCellWriterFactory;
import org.knime.core.data.xml.XMLCellContent;
import org.knime.core.node.NodeLogger;

/**
 * This class encapsulates a {@link JsonValue}. It is the common content of a {@link JSONCell} and a
 * {@link JSONBlobCell} . <br/>
 * Based on {@link XMLCellContent}.
 *
 * This is not meant to be part of the API (only an implementation detail), that is the reason why it is
 * package-private.
 *
 * @since 2.11
 *
 * @author Heiko Hofer
 * @author Gabor Bakos
 */
public class JSONCellContent implements JSONValue {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JSONCellContent.class);

    private final String m_jsonString;

    private SoftReference<JsonValue> m_content;

    /**
     * Creates a {@link JsonValue} by parsing the passed string. It must contain a valid JSON. <br/>
     * This class is just an implementation detail, the common parts of {@link JSONCell} and {@link JSONBlobCell}.
     *
     * @param jsonString a JSON
     * @param checkJson if the JSON string should be parsed in order to check if it is a valid JSON
     * @throws IOException If any IO errors occur.
     */
    JSONCellContent(final String jsonString, final boolean checkJson) throws IOException {
        this(jsonString, checkJson, false);
    }

    JSONCellContent(final String jsonString, final boolean checkJson, final boolean allowComments) throws IOException {
        if (checkJson) {
            // check if JSON string is valid JSON
            JsonValue json = parse(jsonString, allowComments);
            // store the normalized string as cell content
            m_jsonString = serialize(json);
            m_content = new SoftReference<JsonValue>(json);
        } else {
            m_jsonString = jsonString;
            m_content = new SoftReference<JsonValue>(null);
        }
    }

    /**
     * Creates a {@link JsonValue} by parsing the contents of the passed {@link InputStream} (using the default
     * encoding, {@code UTF-8}). It must contain a valid JSON.
     *
     * @param is a JSON
     * @throws IOException If any IO errors occur.
     */
    JSONCellContent(final InputStream is) throws IOException {
        this(is, false);
    }

    JSONCellContent(final InputStream is, final boolean allowComments) throws IOException {
        JsonValue json = parse(is, allowComments);
        m_content = new SoftReference<JsonValue>(json);
        m_jsonString = serialize(json);
    }

    /**
     * Creates a {@link JsonValue} by parsing the contents of the passed {@link Reader}. It must contain a valid JSON.
     *
     * @param reader a JSON
     * @throws IOException If any IO errors occur.
     */
    JSONCellContent(final Reader reader) throws IOException {
        this(reader, false);
    }

    JSONCellContent(final Reader reader, final boolean allowComments) throws IOException {
        JsonValue json = parse(reader, allowComments);
        m_content = new SoftReference<JsonValue>(json);
        m_jsonString = serialize(json);
    }

    /**
     * Creates a new instance which encapsulates the passed {@link JsonValue}.
     *
     * @param jsonValue an {@link JsonValue}.
     */
    JSONCellContent(final JsonValue jsonValue) {
        m_content = new SoftReference<JsonValue>(jsonValue);
        String s = null;
        try {
            s = serialize(jsonValue);
        } catch (IOException ex) {
            // should not happen
            throw new AssertionError("Should not happen", ex);
        }
        m_jsonString = s;
    }

    /**
     * Return the {@link JsonValue}. The returned {@link JsonValue} must not be changed!
     *
     * @return The {@link JsonValue}. (Can be {@code null} when parsing failed, though that is considered an illegal
     *         state.)
     */
    @Override
    public final JsonValue getJsonValue() {
        JsonValue json = m_content.get();
        if (json == null) {
            try {
                json = parse(m_jsonString);
                m_content = new SoftReference<JsonValue>(json);
            } catch (Exception ex) {
                LOGGER.error("Error while parsing JSON in JSON Cell", ex);
            }
        }
        return json;
    }

    /**
     * Returns the JSON as a string.
     *
     * @return The JSON as a string.
     */
    String getStringValue() {
        return m_jsonString;
    }

    private static String serialize(final JsonValue json) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (JSONCellWriter writer = JSONCellWriterFactory.createJSONCellWriter(os)) {
            writer.write(new JSONValue() {
                @Override
                public JsonValue getJsonValue() {
                    return json;
                }
            });
        }
        return os.toString("UTF-8");
    }

    private static JsonValue parse(final String jsonString) throws IOException {
        return parse(jsonString, false);
    }

    private static JsonValue parse(final String jsonString, final boolean allowComments) throws IOException {
        try (JSONCellReader reader =
            JSONCellReaderFactory.createJSONCellReader(new StringReader(jsonString), allowComments)) {
            return reader.readJSON().getJsonValue();
        }
    }

    private static JsonValue parse(final InputStream is, final boolean allowComments) throws IOException {
        try (JSONCellReader reader = JSONCellReaderFactory.createJSONCellReader(is, allowComments)) {
            return reader.readJSON().getJsonValue();
        }
    }

    private static JsonValue parse(final Reader reader, final boolean allowComments) throws IOException {
        try (JSONCellReader jsonReader = JSONCellReaderFactory.createJSONCellReader(reader, allowComments)) {
            return jsonReader.readJSON().getJsonValue();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof JSONCellContent) {
            JSONCellContent that = (JSONCellContent)obj;
            return this.getStringValue().equals(that.getStringValue());
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getStringValue().hashCode();
    }
}
