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
 *   31.03.2011 (hofer): created
 */
package org.knime.core.data.json.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.json.JsonValue;

import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.internal.JacksonConversionsImpl;
import org.knime.core.data.xml.io.XMLCellReader;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

/**
 * A @link{JSONCellReader} to read a single cell from given
 *
 * @link{InputStream . <br/>
 *                   Based on {@link XMLCellReader}.
 *
 * @author Heiko Hofer
 * @author Gabor Bakos
 */
class JSONNodeCellReader implements JSONCellReader {
    private final InputSource m_in;

    private final ObjectReader m_builder;

    private boolean m_first = true;

    private JSONNodeCellReader(final InputSource is, final boolean allowComments) {
        m_in = is;
        //JsonProvider provider = JsonProvider.provider();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JSONValue.class.getClassLoader());
            ObjectMapper mapper = JacksonConversionsImpl.newMapper().registerModule(new JSR353Module());
            ObjectReader reader = mapper.reader();
            JsonFactory factory = reader.getFactory();
            factory = factory.configure(JsonParser.Feature.ALLOW_COMMENTS, allowComments);
            factory = factory.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, allowComments);
            reader = reader.with(factory);
            m_builder = reader;
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Create a new instance of a {@link JSONCellReader} to read a single cell from given {@link InputStream}.
     *
     * @param is the resource to read from
     */
    public JSONNodeCellReader(final InputStream is) {
        this(is, false);
    }

    public JSONNodeCellReader(final InputStream is, final boolean allowComments) {
        this(new InputSource(is), allowComments);
    }

    /**
     * Create a new instance of a {@link JSONCellReader} to read a single cell from given {@link Reader}.
     *
     * @param reader the resource to read from
     */
    public JSONNodeCellReader(final Reader reader) {
        this(reader, false);
    }

    public JSONNodeCellReader(final Reader reader, final boolean allowComments) {
        this(new InputSource(reader), allowComments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONValue readJSON() throws IOException {
        if (m_first) {
            m_first = false;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(JSONValue.class.getClassLoader());
                final JsonFactory jsonFactory = m_builder.getFactory();
                JsonValue json;
                Reader characterStream = m_in.getCharacterStream();
                //Class<?> cls = Activator.getJsonProviderClassLoader().loadClass("javax.json.JsonValue");
                if (characterStream != null) {
                    json = m_builder.readValue(jsonFactory.createParser(characterStream), JsonValue.class);
                } else {
                    json = m_builder.readValue(jsonFactory.createParser(m_in.getByteStream()), JsonValue.class);
                }
                return (JSONValue)JSONCellFactory.create(json);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (m_in.getByteStream() != null) {
            m_in.getByteStream().close();
        } else if (m_in.getCharacterStream() != null) {
            m_in.getCharacterStream().close();
        }
    }
}
