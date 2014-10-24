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
 *   11 Sept. 2014 (Gabor): created
 */
package org.knime.core.data.json.internal;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.ServiceLoader;

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

/**
 *
 * @author Gabor Bakos
 */
public class KNIMEJsonProvider extends JsonProvider {
    static JsonProvider PROVIDER;

    public static void init(final ClassLoader classLoader) {
        PROVIDER = ServiceLoader.load(JsonProvider.class, classLoader).iterator().next();
    }

    /**
     *
     */
    public KNIMEJsonProvider() {
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return PROVIDER.createArrayBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonBuilderFactory createBuilderFactory(final Map<String, ?> arg0) {
        return PROVIDER.createBuilderFactory(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonGenerator createGenerator(final Writer arg0) {
        return PROVIDER.createGenerator(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonGenerator createGenerator(final OutputStream arg0) {
        return PROVIDER.createGenerator(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonGeneratorFactory createGeneratorFactory(final Map<String, ?> arg0) {
        return PROVIDER.createGeneratorFactory(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder createObjectBuilder() {
        return PROVIDER.createObjectBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonParser createParser(final Reader arg0) {
        return PROVIDER.createParser(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonParser createParser(final InputStream arg0) {
        return PROVIDER.createParser(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonParserFactory createParserFactory(final Map<String, ?> arg0) {
        return PROVIDER.createParserFactory(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonReader createReader(final Reader arg0) {
        return PROVIDER.createReader(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonReader createReader(final InputStream arg0) {
        return PROVIDER.createReader(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonReaderFactory createReaderFactory(final Map<String, ?> arg0) {
        return PROVIDER.createReaderFactory(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonWriter createWriter(final Writer arg0) {
        return PROVIDER.createWriter(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonWriter createWriter(final OutputStream arg0) {
        return PROVIDER.createWriter(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonWriterFactory createWriterFactory(final Map<String, ?> arg0) {
        return PROVIDER.createWriterFactory(arg0);
    }
}
