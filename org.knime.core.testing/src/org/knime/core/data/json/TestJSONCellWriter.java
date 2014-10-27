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
 *   24. Oct. 2014 (Gabor): created
 */
package org.knime.core.data.json;

import static org.junit.Assert.assertEquals;
import static org.knime.core.data.json.TestJSONCell.norm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Tests the {@link JSONCellWriter} default implementation acquired through an OSGi service.
 *
 * @author Gabor Bakos
 */
public class TestJSONCellWriter {

    /**
     * Test method for
     * {@link org.knime.core.data.json.io.JSONCellWriterFactory#createJSONCellWriter(java.io.OutputStream)}.
     *
     * @throws IOException
     */
    @Test
    public void testCreateJSONCellWriter() throws IOException {
        String reference = "{}";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JSONCellWriter writer = org.knime.core.data.json.io.JSONCellWriterFactory.createJSONCellWriter(baos)) {
            writer.write((JSONValue)JSONCellFactory.create(reference, false));
        }
        String got = toString(baos);
        assertEquals(norm(reference), norm(got));
    }

    /**
     * Test method for {@link org.knime.core.data.json.io.JSONCellWriterFactory#create(java.io.OutputStream)}.
     *
     * @throws IOException
     */
    @Test
    public void testCreate() throws IOException {
        String reference = "{}";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JSONCellWriter writer = new org.knime.core.data.json.io.JSONCellWriterFactory().create(baos)) {
            writer.write((JSONValue)JSONCellFactory.create(reference, false));
        }
        String got = toString(baos);
        assertEquals(norm(reference), norm(got));
    }

    /**
     * Tests the {@link JSONCellWriterFactory} obtained through an OSGi factory.
     *
     * @throws IOException
     */
    public void testThroughOSGiService() throws IOException {
        String reference = "{}";
        BundleContext ctx = FrameworkUtil.getBundle(JSONValue.class).getBundleContext();
        ServiceReference<JSONCellWriterFactory> ref = ctx.getServiceReference(JSONCellWriterFactory.class);
        JSONCellWriterFactory factory = ctx.getService(ref);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JSONCellWriter writer = factory.create(baos)) {
            writer.write((JSONValue)JSONCellFactory.create(reference, false));
        }
        String got = toString(baos);
        assertEquals(norm(reference), norm(got));
    }

    /**
     * @param baos
     * @return
     * @throws IOException
     */
    private String toString(final ByteArrayOutputStream baos) throws IOException {
        baos.close();
        String got = new String(baos.toByteArray());
        return got;
    }
}
