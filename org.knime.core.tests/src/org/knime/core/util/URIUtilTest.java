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
 *   Aug 2, 2019 (hornm): created
 */
package org.knime.core.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.junit.Test;

/**
 * Tests {@link URIUtil}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class URIUtilTest {

    /**
     * Test for https://knime-com.atlassian.net/browse/AP-12197.
     */
    @Test
    public void testAP12197() {
        String url = "https://hubdev.knime.com/hornm/space/Component3 sdguh4r & f -   ff äöü+ß=)(}{[]$§!";
        String urlEncoded = "https://hubdev.knime.com/hornm/space/Component3%20sdguh4r%20&%20f%20-%20%20%20ff%20%C3%A4%C3%B6%C3%BC+%C3%9F=)(%7D%7B%5B%5D$%C2%A7!";

        URI uriFromURL = URIUtil.createEncodedURI(url);
        URI uriFromEncodedURL = URIUtil.createEncodedURI(urlEncoded);
        assertThat("unexpected encoded url", uriFromURL.toString(), is(urlEncoded));
        assertThat("unexpected encoded url", uriFromEncodedURL.toString(), is(urlEncoded));
    }

}
