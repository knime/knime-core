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
 *   19 Mar 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.util.urlresolve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link URLQueryParamUtil}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class URLQueryParamUtilTest {

    @SuppressWarnings("static-method")
    @Test
    void testAddQueryParam() throws URISyntaxException {
        assertEquals("https://example.com?key=value", replace("https://example.com", "key", "value").toString(),
            "Expected URI to contain key-value pair as query params.");
    }

    @SuppressWarnings("static-method")
    @Test
    void testReplaceQueryParam() throws URISyntaxException {
        final var uri = "https://example.com?key=value";
        final var uriWithNewValue = replace(uri, "key", "new-value");
        assertEquals("https://example.com?key=new-value", uriWithNewValue.toString(),
            "Expected URI to updated key-value pair as query params.");

        final var uriWithNewParam = replace(uri, "new-key", "new-value");
        assertEquals("https://example.com?key=value&new-key=new-value", uriWithNewParam.toString(),
            "Expected URI to contain new key-value pair as query params under new name.");
    }

    private static URI replace(final String uri, final String key, final String value) throws URISyntaxException {
        final var builder = new URIBuilder(uri);
        final var params = URLQueryParamUtil.replaceParameterValue(builder.getQueryParams(), key, value,
            NameValuePair::getName, NameValuePair::getValue, BasicNameValuePair::new);
        return builder.setParameters(params).build();
    }

    @SuppressWarnings("static-method")
    @Test
    void testException() {
        final var uriWithCommaSeparated = "https://example.com?key=value,value2";
        final var thrown = assertThrows(IllegalArgumentException.class,
            () -> replace(uriWithCommaSeparated, "key", "v"), "Expected method to throw IAE");
        assertTrue(thrown.getMessage().contains("Cannot handle multi-valued query parameters"),
            "Expected message to contain hint about multi-valued query parameters.");
    }
}
