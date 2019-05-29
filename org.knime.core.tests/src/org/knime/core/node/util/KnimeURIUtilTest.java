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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.knime.core.util.KnimeURIUtil.getBaseURI;
import static org.knime.core.util.KnimeURIUtil.getDownloadURI;
import static org.knime.core.util.KnimeURIUtil.getEntityType;
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
    public void testGetEntityType() throws URISyntaxException {
        URI knimeURI = new URI("http://knime.com/path/to/component?knimeEntityType=component");
        assertThat("wrong entity type", getEntityType(knimeURI), is(Type.COMPONENT));

        knimeURI = new URI("http://knime.com/path/to/component?knimeEntityType=component&foo=bar");
        assertThat("wrong entity type", getEntityType(knimeURI), is(Type.COMPONENT));

        knimeURI = new URI("http://knime.com/path/to/component?knimeEntityType&foo=bar");
        assertThat("wrong entity type", getEntityType(knimeURI), is(Type.UNKNOWN));

        knimeURI = new URI("http://knime.com/path/to/component?foo=bar");
        assertThat("wrong entity type", getEntityType(knimeURI), is(Type.UNKNOWN));

        knimeURI = new URI("http://knime.com/path/to/component?&foo=bar&knimeEntityType=component");
        assertThat("wrong entity type", getEntityType(knimeURI), is(Type.COMPONENT));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testGetBaseURI() throws URISyntaxException {
        URI knimeURI = new URI("http://knime.com/path/to/component?foo=bar&knimeEntityType=component");
        assertThat("problem extracting base uri", getBaseURI(knimeURI),
            is(new URI("http://knime.com/path/to/component?foo=bar")));

        knimeURI = new URI("http://knime.com/path/to/component?knimeEntityType=component&key=value");
        assertThat("problem extracting base uri", getBaseURI(knimeURI),
            is(new URI("http://knime.com/path/to/component?key=value")));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testGetDownloadURI() throws URISyntaxException {
        //check mapping for 'user-owned' entities
        URI knimeURI = new URI("https://hub.knime.com/johndoe/space/path/to/component");
        URI downloadURI = getDownloadURI(knimeURI);
        assertThat("wrong download URI", downloadURI,
            is(new URI("https://api.hub.knime.com/knime/rest/v4/repository/Users/johndoe/path/to/component:data")));

        knimeURI = new URI("https://hubdev.knime.com/johndoe/space/path/to/component");
        downloadURI = getDownloadURI(knimeURI);
        assertThat("wrong download URI", downloadURI,
            is(new URI("https://api.hubdev.knime.com/knime/rest/v4/repository/Users/johndoe/path/to/component:data")));

        //check mapping for 'knime-owned' entities (i.e. example workflows/components)
        knimeURI = new URI("https://hub.knime.com/knime/space/Examples/path/to/component");
        downloadURI = getDownloadURI(knimeURI);
        assertThat("wrong download URI", downloadURI,
            is(new URI("https://api.hub.knime.com/knime/rest/v4/repository/Users/knime/Examples/path/to/component:data")));

        //non-hub uri
        URI someRandomURI = new URI("http://knime.com/foo/bar");
        assertThat("wrong download URI", someRandomURI, is(new URI("http://knime.com/foo/bar")));

        //edge cases
        knimeURI = new URI("https://hub.knime.com/space/space/path/to/component");
        downloadURI = getDownloadURI(knimeURI);
        assertThat("wrong download URI", downloadURI,
            is(new URI("https://api.hub.knime.com/knime/rest/v4/repository/Users/space/path/to/component:data")));

        knimeURI = new URI("https://hub.knime.com/user/space/space/to/component");
        downloadURI = getDownloadURI(knimeURI);
        assertThat("wrong download URI", downloadURI,
            is(new URI("https://api.hub.knime.com/knime/rest/v4/repository/Users/user/space/to/component:data")));
    }

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

}
