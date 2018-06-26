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
 * ------------------------------------------------------------------------
 */
package org.knime.core.data.xml.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.knime.core.data.xml.util.XmlDomComparer.Diff;
import org.knime.core.data.xml.util.XmlDomComparer.Diff.Type;
import org.knime.core.data.xml.util.XmlDomComparerCustomizer.ChildrenCompareStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Test for the {@link XmlDomComparer}
 * 
 * @author Marcel Hanser
 * 
 */
public class XmlDomComparerTest {

	private static final Document X = document("<x/>");
	private static final Document X_b = document("<x b='b'/>");
	private static final Document X_A = document("<x><a/></x>");
	private static final Document X_A_B = document("<x><a/><b/></x>");
	private static final Document X_A_ab7_B = document("<x><a a='a' b='7'/><b/></x>");
	private static final Document X_A_abx_B = document("<x><a a='a' b='x'/><b/></x>");
	private static final Document X_B_A = document("<x><b/><a/></x>");

	private static final Document X_B_TEXT_A = document("<x><b/>LULU<a/></x>");
	private static final Document X_B_TEXT2_A = document("<x><b/>LALA<a/></x>");
	private static final Document X_B_A_TEXT = document("<x><b/><a/>LULU</x>");

	private static final Document X_BCcf_TEXT2_A = document("<x><b><c c='f'/></b>LALA<a/></x>");
	private static final Document X_TEXT2_A_BCca = document("<x>LULU<a/><b><c d='a'/></b></x>");
	private static final Document X_TEXT2_BCca_A = document("<x>LULU<b><c d='a'/></b><a/></x>");
	private static final Document X_BCca_A_TEXT2 = document("<x><b><c d='a'/></b><a/>LULU</x>");

	private static final Document NS_X_A = document("<x xmlns='https:test'><a/></x>");
	private static final Document NS_X_b = document("<x a:b='b' xmlns:a='http:test'/>");
	private static final Document NS_X = document("<x xmlns:a='http:test'/>");

	private static final Document NS_X_b1 = document("<x xmlns:a='http:test' a:b='0.1'/>");
	private static final Document NS_X_b100 = document("<x xmlns:a='http:test' a:b='0.100'/>");
	private static final Document NS_X_b10E00 = document("<x xmlns:a='http:test' a:b='1.00E-1'/>");
	private static final Document NS_X_b10e00 = document("<x xmlns:a='http:test' a:b='10.00e-2'/>");

	private static final Document NS_X_b2 = document("<x xmlns:a='http:test' a:b='2.1'/>");

	private static final XmlDomComparerCustomizer UNORDERED_CUSTOMIZER = new XmlDomComparerCustomizer(
			ChildrenCompareStrategy.UNORDERED) {

		@Override
		public boolean include(final Node name) {
			return true;
		}
	};

	private static final XmlDomComparerCustomizer IGNORES_B = new XmlDomComparerCustomizer(
			ChildrenCompareStrategy.ORDERED) {

		@Override
		public boolean include(final Node name) {
			return !"b".equals(name.getNodeName());
		}
	};

	@Test
	public void testHashCode() {
		assertEquals(XmlDomComparer.hashCode(X),
				XmlDomComparer.hashCode(document("<x/>")));

		assertEquals(XmlDomComparer.hashCode(X_b),
				XmlDomComparer.hashCode(document("<x b='b'/>")));

		assertEquals(XmlDomComparer.hashCode(X_A_B),
				XmlDomComparer.hashCode(document("<x><a/><b/></x>")));

		assertTrue(XmlDomComparer.hashCode(X) != XmlDomComparer.hashCode(X_b));

		assertTrue(XmlDomComparer.hashCode(X_b) != XmlDomComparer
				.hashCode(X_B_A));

		assertTrue(XmlDomComparer.hashCode(X_A_B) != XmlDomComparer
				.hashCode(X_B_A));

		assertEquals(XmlDomComparer.hashCode(X_A_B, UNORDERED_CUSTOMIZER),
				XmlDomComparer.hashCode(X_B_A, UNORDERED_CUSTOMIZER));

		assertTrue(XmlDomComparer.hashCode(X_A_B, UNORDERED_CUSTOMIZER) != XmlDomComparer
				.hashCode(X_B_TEXT_A, UNORDERED_CUSTOMIZER));

		assertEquals(XmlDomComparer.hashCode(X_B_A_TEXT, UNORDERED_CUSTOMIZER),
				XmlDomComparer.hashCode(X_B_TEXT_A, UNORDERED_CUSTOMIZER));
	}

	// this also tests that #compare does actually returns null or a diff
	@Test
	public void testEquals() {
		assertTrue(XmlDomComparer.equals(X, document("<x/>")));

		assertTrue(XmlDomComparer.equals(X_b, document("<x b='b'/>")));

		assertTrue(XmlDomComparer.equals(X_A_B, document("<x><a/><b/></x>")));

		assertFalse(XmlDomComparer.equals(X, X_b));

		assertFalse(XmlDomComparer.equals(X, X_A));

		assertFalse(XmlDomComparer.equals(X_b, X_B_A));

		assertFalse(XmlDomComparer.equals(X_A_B, X_B_A));

		assertTrue(XmlDomComparer.equals(X_A_B, X_B_A, UNORDERED_CUSTOMIZER));

		assertTrue(XmlDomComparer.equals(X_A, X_B_A, IGNORES_B));

		assertTrue(XmlDomComparer.equals(X_A, X_A_B, IGNORES_B));

		assertTrue(XmlDomComparer.equals(X, X_b, IGNORES_B));

		assertTrue(XmlDomComparer.equals(X_B_A_TEXT, X_B_TEXT_A,
				UNORDERED_CUSTOMIZER));

		assertFalse(XmlDomComparer.equals(X_B_TEXT2_A, X_B_TEXT_A));
	}

	@Test
	public void testCompare() {
		assertEquals("x", XmlDomComparer.compareNodes(X_A, X_A_B).getNode()
				.getNodeName());
		assertNull(XmlDomComparer.compareNodes(X_B_A, X_A, IGNORES_B));

	}

	@Test
	public void testCompareTypes() {
		assertEquals(Type.ELEMENT_CHILDREN_SIZE_MISSMATCH, XmlDomComparer
				.compareNodes(X_A, X_A_B).getType());

		assertEquals(Type.ELEMENT_MISSMATCH,
				XmlDomComparer.compareNodes(X_B_A, X_A_B).getType());

		assertEquals(Type.TEXT_MISSMATCH,
				XmlDomComparer.compareNodes(X_B_TEXT_A, X_B_TEXT2_A).getType());
	}

	@Test
	public void testCompareAttributes() {
		Diff compareNodes = XmlDomComparer.compareNodes(X_A_ab7_B, X_A_abx_B);
		assertEquals(Diff.Type.ATTRIBUTE_MISSMATCH, compareNodes.getType());
		assertEquals("b", compareNodes.getNode().getNodeName());
		assertEquals("7", compareNodes.getExpectedValue());
		assertEquals("x", compareNodes.getActualValue());
		assertEquals(3, compareNodes.getLevel());
		List<Node> reversePath = compareNodes.getReversePath();

		assertEquals("b", reversePath.get(0).getNodeName().toString());
		assertEquals("a", reversePath.get(1).getNodeName().toString());
		assertEquals("x", reversePath.get(2).getNodeName().toString());
	}

	@Test
	public void testDeeperDiffIsReturnedIfUnordered() {
		assertAttributeC(XmlDomComparer.compareNodes(X_BCcf_TEXT2_A,
				X_TEXT2_BCca_A, UNORDERED_CUSTOMIZER));
		assertAttributeC(XmlDomComparer.compareNodes(X_BCcf_TEXT2_A,
				X_BCca_A_TEXT2, UNORDERED_CUSTOMIZER));
		assertAttributeC(XmlDomComparer.compareNodes(X_BCcf_TEXT2_A,
				X_TEXT2_A_BCca, UNORDERED_CUSTOMIZER));
	}

	@Test
	public void testNamespacesAwareComparison() {
		assertFalse(XmlDomComparer.equals(NS_X_A, X_A));
		assertFalse(XmlDomComparer.equals(X_b, NS_X_b));
		// xmnls attributes does not influence equality
		assertTrue(XmlDomComparer.equals(NS_X, X));
	}

	@Test
	public void testNamespacesAwareHashCode() {

		assertTrue(XmlDomComparer.hashCode(NS_X_A) != XmlDomComparer
				.hashCode(X_A));
		assertTrue(XmlDomComparer.hashCode(X_b) != XmlDomComparer
				.hashCode(NS_X_b));
		// xmnls attributes does not influence equality
		assertEquals(XmlDomComparer.hashCode(NS_X), XmlDomComparer.hashCode(X));
	}

	@Test
	public void testNumericalConvertionInAttributes() {
		assertTrue(XmlDomComparer.equals(NS_X_b100, NS_X_b1));
		assertTrue(XmlDomComparer.equals(NS_X_b10e00, NS_X_b1));
		assertTrue(XmlDomComparer.equals(NS_X_b10E00, NS_X_b100));

		assertEquals(XmlDomComparer.hashCode(NS_X_b100),
				XmlDomComparer.hashCode(NS_X_b1));
		assertEquals(XmlDomComparer.hashCode(NS_X_b10e00),
				XmlDomComparer.hashCode(NS_X_b1));
		assertEquals(XmlDomComparer.hashCode(NS_X_b10E00),
				XmlDomComparer.hashCode(NS_X_b100));

		assertFalse(XmlDomComparer.equals(NS_X_b1, NS_X_b2));
	}

	@Test
	public void testElementVSNullComparison() {
		assertTrue(XmlDomComparer.equals(null, null));
		assertFalse(XmlDomComparer.equals(NS_X, null));
		assertFalse(XmlDomComparer.equals(null, NS_X));

		Diff compareNodes = XmlDomComparer.compareNodes(X, null);
		assertNotNull(compareNodes);
		assertEquals(Type.NODE_MISSING, compareNodes.getType());
		assertTrue(compareNodes.getNode() instanceof Document);

		compareNodes = XmlDomComparer.compareNodes(null, X);
		assertNotNull(compareNodes);
		assertEquals(Type.NODE_MISSING, compareNodes.getType());
		assertTrue(compareNodes.getNode() instanceof Document);
	}

	/**
	 * @param compareNodes
	 */
	private static void assertAttributeC(final Diff compareNodes) {
		assertNotNull(compareNodes);
		assertEquals(Diff.Type.ATTRIBUTE_MISSING, compareNodes.getType());
		assertEquals("c", compareNodes.getNode().getNodeName());
		assertEquals("c", compareNodes.getExpectedValue());
		assertEquals(null, compareNodes.getActualValue());
		assertEquals(4, compareNodes.getLevel());
		List<Node> reversePath = compareNodes.getReversePath();

		assertEquals("c", reversePath.get(0).getNodeName().toString());
		assertEquals("c", reversePath.get(1).getNodeName().toString());
		assertEquals("b", reversePath.get(2).getNodeName().toString());
		assertEquals("x", reversePath.get(3).getNodeName().toString());
	}

	private static Document document(final String string) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader(string)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
