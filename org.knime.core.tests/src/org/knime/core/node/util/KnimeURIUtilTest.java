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
 *   May 28, 2019 (hornm): created
 */
package org.knime.core.node.util;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.knime.core.util.KnimeURIUtil.guessEntityType;
import static org.knime.core.util.KnimeURIUtil.isHubURI;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.knime.core.util.KnimeURIUtil;
import org.knime.core.util.KnimeURIUtil.Type;

/**
 * Tests {@link KnimeURIUtil}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class KnimeURIUtilTest {

    @SuppressWarnings("javadoc")
    @Test
    public void testIsHubURI() throws URISyntaxException {
        URI knimeURI = new URI("https://hub.knime.com/johndoe/spaces/public/path/to/component");
        assertTrue("check for hub URI failed", isHubURI(knimeURI));

        knimeURI = new URI("https://hubdev.knime.com/johndoe/spaces/public/path/to/component");
        assertTrue("check for hub URI failed", isHubURI(knimeURI));

        knimeURI = new URI("https://dev.knime.com/johndoe/spaces/public/path/to/component");
        assertFalse("check for hub URI returned wrong result", isHubURI(knimeURI));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testGetEntityType() throws URISyntaxException {
        final String prefix = "https://hub.knime.com/janedoe/";

        // === URIs with known type ===

        // workflow, workflow groups, components specification user/space/
        URI knimeURI = new URI(prefix + "space/42/1.3/noFactory");
        assertThat("wrong enitity type", guessEntityType(knimeURI), is(Type.OBJECT));

        // extensions specification user/extension/extId and user/extension/extId/version
        knimeURI = new URI(prefix + "extensions/32492");
        assertThat("wrong enitity type", guessEntityType(knimeURI), is(Type.EXTENSION));

        knimeURI = new URI(prefix + "extensions/32492/2.3");
        assertThat("wrong enitity type", guessEntityType(knimeURI), is(Type.EXTENSION));

        // nodes specification user/extension/extId/version/nodeFactory
        knimeURI = new URI(prefix + "extensions/42/1.3/noFactory");
        assertThat("wrong enitity type", guessEntityType(knimeURI), is(Type.NODE));

        // === URIs with unkown type ===

        // only host adress
        knimeURI = new URI("https://hub.knime.com/");
        assertThat("wrong enitity type", guessEntityType(knimeURI), is(Type.UNKNOWN));

        // only user name (too short URI)
        knimeURI = new URI(prefix);
        assertThat("wrong enitity type", guessEntityType(knimeURI), is(Type.UNKNOWN));

        // typos in identifier space
        knimeURI = new URI(prefix + "spaces/42/1.3/noFactory");
        assertThat("wrong enitity type", guessEntityType(knimeURI), not(Type.OBJECT));

        // typos in extension identifier
        knimeURI = new URI(prefix + "extension/32492/2.3");
        assertThat("wrong enitity type", guessEntityType(knimeURI), not(Type.EXTENSION));

        // test too short extensions path
        knimeURI = new URI(prefix + "extensions/");
        assertThat("wrong enitity type", guessEntityType(knimeURI), not(Type.EXTENSION));
        assertThat("wrong enitity type", guessEntityType(knimeURI), not(Type.NODE));

        // test too long extensions path
        knimeURI = new URI(prefix + "extensions/42/1.3/noFactory/124020");
        assertThat("wrong enitity type", guessEntityType(knimeURI), not(Type.EXTENSION));

        // test too short nodes path
        knimeURI = new URI(prefix + "extension/42/1.3");
        assertThat("wrong enitity type", guessEntityType(knimeURI), not(Type.NODE));

        // test too long nodes path
        knimeURI = new URI(prefix + "extension/42/1.3/noFactory/");
        assertThat("wrong enitity type", guessEntityType(knimeURI), not(Type.NODE));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testEntityEndpointURIs() throws URISyntaxException {
        final String prefix = "https://hub.knime.com/janedoe/";
        final String apiPrefix = "https://api.hub.knime.com/";

        URI knimeURI = new URI(prefix + "extensions/32492/2.3");
        assertThat("wrong endpoint", KnimeURIUtil.getExtensionEndpointURI(knimeURI),
            is(new URI(apiPrefix + "extensions/32492")));

        knimeURI = new URI(prefix + "extensions/2348923/2.3/nodefac");
        assertThat("wrong endpoint", KnimeURIUtil.getNodeEndpointURI(knimeURI),
            is(new URI(apiPrefix + "nodes/nodefac")));

        final String suffix = "manamana/babdibidibi/manamana/babdibidi";
        knimeURI = new URI(prefix + "spaces/" + suffix);
        assertThat("wrong endpoint", KnimeURIUtil.getObjectEntityEndpointURI(knimeURI, false),
            is(new URI(apiPrefix + "knime/rest/v4/repository/Users/janedoe/" + suffix)));
        assertThat("wrong endpoint", KnimeURIUtil.getObjectEntityEndpointURI(knimeURI, true),
            is(new URI(apiPrefix + "knime/rest/v4/repository/Users/janedoe/" + suffix + ":data")));
    }

}
