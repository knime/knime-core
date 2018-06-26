/*
 * ------------------------------------------------------------------------
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
 *   30.05.2013 (thor): created
 */
package org.knime.core.node;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.values.NamespaceManager;
import org.apache.xmlbeans.impl.values.XmlObjectBase;
import org.junit.Test;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.testfactories.v13.DTD_v13;
import org.knime.core.node.testfactories.v13.DTD_v13_deprecated;
import org.knime.core.node.testfactories.v13.DTD_v13_invalid;
import org.knime.core.node.testfactories.v27.DTD_v27;
import org.knime.core.node.testfactories.v27.DTD_v27_deprecated;
import org.knime.core.node.testfactories.v27.DTD_v27_invalid;
import org.knime.core.node.testfactories.v27.DTD_v27_noPublicId;
import org.knime.core.node.testfactories.v27.DTD_v27_unsupportedDoctype;
import org.knime.core.node.testfactories.v27.XSD_v27;
import org.knime.core.node.testfactories.v27.XSD_v27_deprecated;
import org.knime.core.node.testfactories.v27.XSD_v27_invalid;
import org.knime.core.node.testfactories.v28.XSD_v28;
import org.knime.core.node.testfactories.v28.XSD_v28_deprecated;
import org.knime.core.node.testfactories.v28.XSD_v28_invalid;
import org.knime.core.node.testfactories.v28.XSD_v28_noInteractiveView;
import org.knime.core.node.testfactories.v28.XSD_v28_unsupportedNamespace;

/**
 * Testcases for the new node description implementation of 2.8.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class NodeDescriptionTest {
    /**
     * Tests DTD based pre-2.0 node descriptions.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testVersion13DTD() throws Exception {
        testCommonFields(DTD_v13.class, NodeDescription13Proxy.class, "http://knime.org/node/v1.3", false);
        testDeprecatedNodeName(DTD_v13_deprecated.class);
        testInvalidDescription(DTD_v13_invalid.class, NodeDescription13Proxy.class);
    }

    /**
     * Tests DTD based pre-2.8 node descriptions.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testVersion27DTD() throws Exception {
        testCommonFields(DTD_v27.class, NodeDescription27Proxy.class, "http://knime.org/node2012", false);
        testCommonFields(DTD_v27_noPublicId.class, NodeDescription27Proxy.class, "http://knime.org/node2012", false);
        testDeprecatedNodeName(DTD_v27_deprecated.class);
        testInvalidDescription(DTD_v27_invalid.class, NodeDescription27Proxy.class);
    }

    /**
     * Tests XSD based pre-2.8 node descriptions.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testVersion27XSD() throws Exception {
        testCommonFields(XSD_v27.class, NodeDescription27Proxy.class, "http://knime.org/node2012", false);
        testDeprecatedNodeName(XSD_v27_deprecated.class);
        testInvalidDescription(XSD_v27_invalid.class, NodeDescription27Proxy.class);
    }

    /**
     * Tests XSD based 2.8 node descriptions.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testVersion28XSD() throws Exception {
        testCommonFields(XSD_v28.class, NodeDescription28Proxy.class, "http://knime.org/node/v2.8", true);
        testCommonFields(XSD_v28_noInteractiveView.class, NodeDescription28Proxy.class, "http://knime.org/node/v2.8",
                         false);
        testDeprecatedNodeName(XSD_v28_deprecated.class);
        testInvalidDescription(XSD_v28_invalid.class, NodeDescription28Proxy.class);
    }

    /**
     * Checks if unsupported doctypes are detected.
     *
     * @throws Exception if something goes wrong
     */
    @Test(expected = XmlException.class)
    public void testUnsupportedDoctype() throws Exception {
        NodeDescriptionParser parser = new NodeDescriptionParser();
        parser.parseDescription(DTD_v27_unsupportedDoctype.class);
    }

    /**
     * Checks if unsupported namespaces are detected.
     *
     * @throws Exception if something goes wrong
     */
    @Test(expected = XmlException.class)
    public void testUnsupportedNamespace() throws Exception {
        NodeDescriptionParser parser = new NodeDescriptionParser();
        parser.parseDescription(XSD_v28_unsupportedNamespace.class);
    }


    /**
     * Checks if test <tt>strpXmlFragment</tt> methods works as expected.
     */
    @Test
    public void testStripXmlFragment() {
        assertThat(NodeDescription.stripXmlFragment(createTextObjectStub("<frag>Text</frag>")), is("Text"));
        assertThat(NodeDescription.stripXmlFragment(createTextObjectStub("<frag att='Bla' >Text</frag>")), is("Text"));
        assertThat(NodeDescription.stripXmlFragment(createTextObjectStub("<fragText</frag>")), is(""));
        assertThat(NodeDescription.stripXmlFragment(createTextObjectStub("<fragText</frag")), is(""));
        assertThat(NodeDescription.stripXmlFragment(createTextObjectStub("frag>Text/frag>")), is(""));
    }

    @SuppressWarnings("rawtypes")
    private void testInvalidDescription(final Class<? extends NodeFactory> factoryClass,
                                        final Class<? extends NodeDescription> expectedProxyClass) throws Exception {
        NodeDescriptionParser parser = new NodeDescriptionParser();
        NodeDescription description = parser.parseDescription(factoryClass);
        assertThat("Invalid node description not recognized", (Boolean)expectedProxyClass.getDeclaredMethod("validate")
                .invoke(description), is(Boolean.FALSE));
    }

    @SuppressWarnings("rawtypes")
    private void testDeprecatedNodeName(final Class<? extends NodeFactory> factoryClass) throws Exception {
        NodeDescriptionParser parser = new NodeDescriptionParser();
        NodeDescription description = parser.parseDescription(factoryClass);
        assertThat("Wrong node name extracted from deprecated node", description.getNodeName(),
                   is("My own name (deprecated)"));

    }

    @SuppressWarnings("rawtypes")
    private void testCommonFields(final Class<? extends NodeFactory> factoryClass,
                                  final Class<? extends NodeDescription> expectedProxyClass,
                                  final String expectedNamespace, final boolean hasInteractiveView) throws Exception {
        NodeDescriptionParser parser = new NodeDescriptionParser();
        NodeDescription description = parser.parseDescription(factoryClass);

        assertThat("Unexpected proxy class", description, instanceOf(expectedProxyClass));
        assertThat("Valid node description recognized as invalid",
                   (Boolean)expectedProxyClass.getDeclaredMethod("validate").invoke(description), is(Boolean.TRUE));

        assertThat("Wrong icon path extracted", description.getIconPath(), is("icon.png"));
        // port descriptions
        assertThat("Wrong inport description extracted at data port 0", description.getInportDescription(0),
                   is("First input table description"));
        assertThat("Wrong inport description extracted at data port 1", description.getInportDescription(1),
                   is("Second input table description"));
        assertThat("Wrong inport description extracted at data port 2", description.getInportDescription(2),
                   is("Third input table description"));

        assertThat("Wrong inport description extracted at model port 0", description.getInportDescription(3),
                   is("First input model description"));
        assertThat("Wrong inport description extracted at model port 1", description.getInportDescription(4),
                   is("Second input model description"));
        assertThat("Wrong inport description extracted at model port 2", description.getInportDescription(5),
                   is("Third input model description"));
        assertThat("Wrong description for non-existing input port", description.getInportDescription(7),
                   is(nullValue()));

        assertThat("Wrong outport description extracted at port 0", description.getOutportDescription(0),
                   is("First output table description"));
        assertThat("Wrong outport description extracted at port 1", description.getOutportDescription(1),
                   is("Second output table description"));
        assertThat("Wrong outport description extracted at port 2", description.getOutportDescription(2),
                   is("Third output table description"));

        assertThat("Wrong outport description extracted at model port 0", description.getOutportDescription(3),
                   is("First output model description"));
        assertThat("Wrong outport description extracted at model port 1", description.getOutportDescription(4),
                   is("Second output model description"));
        assertThat("Wrong outport description extracted at model port 2", description.getOutportDescription(5),
                   is("Third output model description"));
        assertThat("Wrong description for non-existing output port", description.getOutportDescription(7),
                   is(nullValue()));

        // port names
        assertThat("Wrong inport name extracted at data port 0", description.getInportName(0),
                   is("First input table name"));
        assertThat("Wrong inport name extracted at data port 1", description.getInportName(1),
                   is("Second input table name"));
        assertThat("Wrong inport name extracted at data port 2", description.getInportName(2),
                   is("Third input table name"));

        assertThat("Wrong inport name extracted at model port 0", description.getInportName(3),
                   is("First input model name"));
        assertThat("Wrong inport name extracted at model port 1", description.getInportName(4),
                   is("Second input model name"));
        assertThat("Wrong inport name extracted at model port 2", description.getInportName(5),
                   is("Third input model name"));
        assertThat("Wrong name for non-existing input port", description.getInportName(7), is(nullValue()));

        assertThat("Wrong outport description extracted at port 0", description.getOutportName(0),
                   is("First output table name"));
        assertThat("Wrong outport description extracted at port 1", description.getOutportName(1),
                   is("Second output table name"));
        assertThat("Wrong outport description extracted at port 2", description.getOutportName(2),
                   is("Third output table name"));

        assertThat("Wrong outport description extracted at model port 0", description.getOutportName(3),
                   is("First output model name"));
        assertThat("Wrong outport description extracted at model port 1", description.getOutportName(4),
                   is("Second output model name"));
        assertThat("Wrong outport description extracted at model port 2", description.getOutportName(5),
                   is("Third output model name"));
        assertThat("Wrong description for non-existing output port", description.getOutportName(7), is(nullValue()));

        // interactive view
        if (hasInteractiveView) {
            assertThat("Wrong name for interactive view", description.getInteractiveViewName(),
                       is("My interactive view name"));
        } else {
            assertThat("Wrong name for non-existing interactive view", description.getInteractiveViewName(),
                       is(nullValue()));
        }

        // node name
        assertThat("Wrong node name extracted", description.getNodeName(), is("My own name"));

        // not type
        assertThat("Wrong node type", description.getType(), is(NodeType.Other));

        // view count
        assertThat("Wrong node type", description.getViewCount(), is(3));

        // views
        assertThat("Wrong name for view 0", description.getViewName(0), is("First view name"));
        assertThat("Wrong name for view 1", description.getViewName(1), is("Second view name"));
        assertThat("Wrong name for view 2", description.getViewName(2), is("Third view name"));
        assertThat("Wrong name for non-existing view", description.getViewName(3), is(nullValue()));

        assertThat("Wrong description for view 0", description.getViewDescription(0), is("First view description"));
        assertThat("Wrong description for view 1", description.getViewDescription(1), is("Second view description"));
        assertThat("Wrong description for view 2", description.getViewDescription(2), is("Third view description"));
        assertThat("Wrong description for non-existing view", description.getViewDescription(3), is(nullValue()));

        assertThat("Wrong namespace", description.getXMLDescription().getNamespaceURI(), is(expectedNamespace));
    }

    @SuppressWarnings("serial")
    private static XmlObject createTextObjectStub(final String text) {
        return new XmlObjectBase() {
            @Override
            protected int value_hash_code() {
                return 0;
            }

            @Override
            protected void set_text(final String s) {
            }

            @Override
            protected void set_nil() {
            }

            @Override
            public SchemaType schemaType() {
                return null;
            }

            @Override
            protected boolean equal_to(final XmlObject xmlobj) {
                return false;
            }

            @Override
            protected String compute_text(final NamespaceManager nsm) {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String xmlText() {
                return text;
            }
        };
    }
}
