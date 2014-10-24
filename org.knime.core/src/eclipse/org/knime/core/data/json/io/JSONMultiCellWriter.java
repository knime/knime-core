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
 *   08.03.2011 (hofer): created
 */
package org.knime.core.data.json.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.json.JsonValue;

import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellWriter;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.internal.JacksonConversionsImpl;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

/**
 * An @link{JSONCellWriter} to write {@link JSONCell}s that can optionally be enclosed in a root element.
 *
 * @author Heiko Hofer
 */
class JSONMultiCellWriter implements JSONCellWriter {
    private ObjectMapper m_writer;

    private final OutputStream m_os;

    /**
     * Create writer to write JSON cells.
     *
     * @param os the JSON cells are written to this resource.
     * @throws IOException when header could not be written.
     */
    JSONMultiCellWriter(final OutputStream os) throws IOException {
        initWriter();
        m_os = os;
    }

    /**
     * Initialize the stream writer object.
     */
    private void initWriter() {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JSONValue.class.getClassLoader());
            ObjectMapper mapper = JacksonConversionsImpl.newMapper().configure(Feature.AUTO_CLOSE_TARGET, false)
                    .configure(Feature.AUTO_CLOSE_JSON_CONTENT, false);
            m_writer = mapper.registerModule(new JSR353Module());
            m_writer.disable(SerializationFeature.CLOSE_CLOSEABLE);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final JSONValue cell) throws IOException {
        JsonValue json = cell.getJsonValue();
        m_writer.writeValue(m_os, json);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // close stream since m_writer.close() does no necessarily do it
        m_os.close();
    }

}
