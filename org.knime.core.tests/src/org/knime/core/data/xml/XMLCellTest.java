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
 *   23.10.2015 (thor): created
 */
package org.knime.core.data.xml;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Testcases for {@link XMLCell}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class XMLCellTest {
    /**
     * Checks if equals and hashcode are compatible.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testEqualsAndHashcodeNotEqual() throws Exception {
        Document d1 = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Document d2 = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element root1 = d1.createElement("root");
        d1.appendChild(root1);

        Element root2 = d2.createElement("root");
        d2.appendChild(root2);

        root1.setAttributeNS("http://ns", "ns:ok", "value");
        root1.setAttribute("ns:bad", "value");

        root2.setAttributeNS("http://ns", "ns:ok", "value");
        root2.setAttributeNS("http://ns", "ns:bad", "value");

        XMLCellContent c1 = new XMLCellContent(d1);
        XMLCellContent c2 = new XMLCellContent(d2);

        boolean eq = c1.equals(c2);
        boolean hc = c1.hashCode() == c2.hashCode();
        assertThat("equals and hashcode are not compatible", eq && hc || !eq , is(true));
    }


    /**
     * Checks if equals and hashcode are compatible.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testEqualsAndHashcodeEqual() throws Exception {
        Document d1 = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Document d2 = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element root1 = d1.createElement("root");
        d1.appendChild(root1);

        Element root2 = d2.createElement("root");
        d2.appendChild(root2);

        root1.setAttributeNS("http://ns", "ns:ok", "value");

        root2.setAttributeNS("http://ns", "ns:ok", "value");

        XMLCellContent c1 = new XMLCellContent(d1);
        XMLCellContent c2 = new XMLCellContent(d2);

        boolean eq = c1.equals(c2);
        boolean hc = c1.hashCode() == c2.hashCode();
        assertThat("equals and hashcode are not compatible", eq && hc || !eq , is(true));
    }

}
