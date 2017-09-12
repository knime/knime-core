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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.data.xml.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.knime.core.data.xml.util.XmlDomComparer.Diff.Type;
import org.knime.core.node.util.filter.InputFilter;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * Provides methods for comparing XML nodes.
 *
 * @author Alexander Fillbrunn
 * @author Marcel Hanser
 * @since 2.10
 *
 */
public final class XmlDomComparer {

    /**
     * Prime number for the hashcode computation.
     */
    private static final int PRIME = 17;

    // Regex for numbers used to determine whether an attribute contains a
    // number
    private static final String NUMBER_REGEX = "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$";

    private static final XmlDomComparerCustomizer DEFAULT_ALL_TRUE_ORDERED_FILTER = new XmlDomComparerCustomizer() {

        @Override
        public boolean include(final Node name) {
            return true;
        }
    };

    private XmlDomComparer() {
    }

    /**
     * Checks if two nodes are equal.
     *
     * @param node1 the first xml node
     * @param node2 the second xml node
     * @return A {@link Diff} comprising the difference between the both given nodes or <code>null</code> if they are
     *         equal
     */
    public static Diff compareNodes(final Node node1, final Node node2) {
        return compareNodes(node1, node2, null);
    }

    /**
     * Checks if two nodes are equal and uses a {@link XmlDomComparerCustomizer} to determine which nodes to check and
     * which to ignore and to determine if the ordering of child elements impacts the comparison.
     *
     * @param node1 the first xml node
     * @param node2 the second xml node
     * @param l the listener that determines whether a node is checked or not
     * @return A {@link Diff} comprising the difference between the both given nodes or <code>null</code> if they are
     *         equal or both <code>null</code>.
     */
    public static Diff compareNodes(final Node node1, final Node node2, final XmlDomComparerCustomizer l) {
        if (node1 == node2) {
            return null;
        }
        if (node1 == null) {
            return new Diff(node2, node2, Type.NODE_MISSING);
        }
        if (node2 == null) {
            return new Diff(node1, node1, Type.NODE_MISSING);
        }
        return areNodesEqual(node1, node2, l == null ? DEFAULT_ALL_TRUE_ORDERED_FILTER : l);
    }

    /**
     * <code>Equality</code> here is defined as the structural equality of the node. That includes the order of
     * occurring elements/text nodes, the equality of elements QName and (unordered) attributes. If both arguments are
     * <code>null</code> or referring the same object <code>true</code> is returned.
     *
     * @param arg0 first node
     * @param arg1 second node
     * @return <code>true</code> if the given Nodes are equal
     */
    public static boolean equals(final Node arg0, final Node arg1) {
        return equals(arg0, arg1, DEFAULT_ALL_TRUE_ORDERED_FILTER);
    }

    /**
     * <code>Equality</code> of Nodes is defined on the structural equality of the xml node. That includes the order of
     * occurring elements/text nodes, equality of elements QName and the matching of the attributes (unordered) in
     * elements. If both arguments are <code>null</code> or referring the same object <code>true</code> is returned.
     * With the help of the filter certain Attributes/Elements can be canceled during the equality check and the
     * comparison can be further customized. This is helpful to remove semantically not relevant meta data from Xml
     * structures, like the metadata element in SVG or to let the order of elements not influence the equality.
     *
     * @param arg0 first node
     * @param arg1 second node
     * @param filter determines the Nodes which are considered during the equality
     *
     * @return <code>true</code> if the given Nodes are equal
     *
     */
    public static boolean equals(final Node arg0, final Node arg1, final XmlDomComparerCustomizer filter) {
        return compareNodes(arg0, arg1, filter) == null;
    }

    /**
     * Computes a default hashCode from a given XML Node.
     *
     * @param arg0 the argument
     * @return hashcode based on given node
     */
    public static int hashCode(final Node arg0) {
        return hashCode(arg0, DEFAULT_ALL_TRUE_ORDERED_FILTER);
    }

    /**
     * Computes a default hashCode from a given XML Node. The customizer defines the Nodes which are considered during
     * the hashCode computation. If the customizer is <code>null</code> all nodes are considered.
     *
     * @param arg0 the argument
     * @param customizer the filter
     * @return hashcode based on given node and filter
     */
    public static int hashCode(final Node arg0, final XmlDomComparerCustomizer customizer) {

        int hash = 1;
        switch (arg0.getNodeType()) {
            case Node.DOCUMENT_NODE:
                Document expectedDoc = (Document)arg0;

                hash += hashCode(expectedDoc.getDocumentElement(), customizer);
                break;
            case Node.ELEMENT_NODE:
                Element expectedElement = (Element)arg0;

                hash += PRIME * getQualifiedName(expectedElement).hashCode();

                hash += hashAttributes(expectedElement.getAttributes(), customizer);

                NodeList expectedChildren = expectedElement.getChildNodes();
                int includedChildren = 0;
                for (int i = 0; i < expectedChildren.getLength(); i++) {
                    Node item = expectedChildren.item(i);
                    if (customizer.include(item)) {
                        includedChildren++;
                        switch (customizer.determineCompareStrategy(item)) {
                            case UNORDERED:
                                hash = hash + hashCode(item, customizer);
                                break;
                            case ORDERED:
                            default:
                                // take also the sequence of the elements into account
                                hash = hash * includedChildren * PRIME + hashCode(item, customizer);
                                break;
                        }
                    }
                }
                break;
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                hash += hash * PRIME + ((CharacterData)arg0).getData().trim().hashCode();
                break;
            default:
                break;
        }
        return hash;
    }

    /**
     * <code>true</code> if the given node is an element.
     *
     * @param node the node to check
     * @return <code>true</code> if the given node is an element
     * @throws NullPointerException if an argument is <code>null</code>
     */
    public static boolean isElement(final Node node) {
        return Node.ELEMENT_NODE == node.getNodeType();
    }

    /**
     * <code>true</code> if the given node is an attribute.
     *
     * @param node the node to check
     * @return <code>true</code> if the given node is an attribute
     * @throws NullPointerException if an argument is <code>null</code>
     */
    public static boolean isAttribute(final Node node) {
        return Node.ATTRIBUTE_NODE == node.getNodeType();
    }

    /**
     * <code>true</code> if the given node is an comment.
     *
     * @param node the node to check
     * @return <code>true</code> if the given node is an comment
     * @throws NullPointerException if an argument is <code>null</code>
     */
    public static boolean isCommment(final Node node) {
        return Node.COMMENT_NODE == node.getNodeType();
    }

    private static Diff areNodesEqual(final Node node1, final Node node2, final XmlDomComparerCustomizer l) {

        // Check for incompatible types
        if (node1.getNodeType() != node2.getNodeType()) {
            return new Diff(node1, node1.getNodeType(), node2.getNodeType(), Type.TYPE_MISSMATCH);
        }

        switch (node1.getNodeType()) {
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
            case Node.COMMENT_NODE:
                String textNode1 = ((CharacterData)node1).getData();
                String textNode2 = ((CharacterData)node2).getData();
                if (!Objects.equals(textNode1, textNode2)) {
                    return new Diff(node1, textNode1, textNode2, Type.TEXT_MISSMATCH);
                }
                return null;
            case Node.DOCUMENT_NODE:
                return areNodesEqual(((Document)node1).getDocumentElement(), ((Document)node2).getDocumentElement(), l);
            case Node.ELEMENT_NODE:
                // Different names means different nodes
                Element element1 = (Element)node1;
                Element element2 = (Element)node2;

                Diff namesComparison = checkNamesAndNS(element1, element2);

                if (namesComparison != null) {
                    return namesComparison;
                }

                Diff attributeComparison = areAttributesEqual(node1, node2, l);

                if (attributeComparison != null) {
                    return attributeComparison;
                }

                return areChildsEqual(node1, node2, l);
            case Node.PROCESSING_INSTRUCTION_NODE:
                String piNode1 =  ((ProcessingInstruction)node1).getData();
                String piNode2 =  ((ProcessingInstruction)node2).getData();
                if (!Objects.equals(piNode1, piNode2)) {
                    return new Diff(node1, piNode1, piNode2, Type.TEXT_MISSMATCH);
                }
                return null;
            default:
                throw new IllegalArgumentException("Nodes with type: " + node1.getNodeType() + " are not supported");
        }
    }

    private static Diff checkNamesAndNS(final Node node1, final Node node2) {
        String qName1 = getQualifiedName(node1);
        String qName2 = getQualifiedName(node2);

        return qName1.equals(qName2) ? null : new Diff(node1, qName1, qName2, Type.ELEMENT_MISSMATCH);
    }

    private static String getQualifiedName(final Node node1) {
        return node1.getNamespaceURI() == null ? node1.getNodeName() : node1.getNamespaceURI() + ":"
            + node1.getNodeName();
    }

    private static Diff areChildsEqual(final Node node1, final Node node2, final XmlDomComparerCustomizer l) {
        NodeList children1 = node1.getChildNodes();
        NodeList children2 = node2.getChildNodes();

        // Lists holding all nodes for which are intended to be checked for
        // equality
        List<Node> c1 = filterChilds(children1, l);
        List<Node> c2 = filterChilds(children2, l);

        // If different number of children, elements are not equal
        if (c1.size() != c2.size()) {
            return new Diff(node1, c1.size(), c2.size(), Type.ELEMENT_CHILDREN_SIZE_MISSMATCH);
        }

        switch (l.determineCompareStrategy(node1)) {
            case UNORDERED:
                // Recursively compare nodes with each other if we found an equal
                // element we break that
                //
                ListIterator<Node> childIt1 = c1.listIterator();

                while (childIt1.hasNext()) {
                    Node childNode1 = childIt1.next();
                    boolean success = false;
                    Diff deepestDif = null;

                    ListIterator<Node> childIt2 = c2.listIterator();
                    while (childIt2.hasNext()) {

                        Node childNode2 = childIt2.next();

                        Diff diff = areNodesEqual(childNode1, childNode2, l);

                        if (diff == null) {
                            // remove it as we found a match
                            childIt2.remove();
                            success = true;
                            break;
                        } else {
                            if (deepestDif == null || diff.getLevel() > deepestDif.getLevel()) {
                                deepestDif = diff;
                            }
                        }
                    }

                    if (!success) {
                        return deepestDif;
                    }
                }

                break;

            case ORDERED:
            default:
                for (int i = 0; i < c1.size(); i++) {
                    Node childNode1 = c1.get(i);
                    Node childNode2 = c2.get(i);

                    Diff res = areNodesEqual(childNode1, childNode2, l);

                    if (res != null) {
                        return res;
                    }
                }
                break;
        }

        return null;
    }

    private static Diff areAttributesEqual(final Node node1, final Node node2, final XmlDomComparerCustomizer l) {
        // Check attributes
        NamedNodeMap attributes1 = node1.getAttributes();
        NamedNodeMap attributes2 = node2.getAttributes();

        if (attributes1 == null ^ attributes2 == null) {
            // this should actually not happen since we are just calling this on
            // elements ?!
            // can only happen if the types miss matches
            return new Diff(node1, node1.getNodeType(), Type.TYPE_MISSMATCH);
        } else if (attributes1 != null && attributes2 != null) {

            Map<String, String> attr1 = createAttributesMap(attributes1, l);
            Map<String, String> attr2 = createAttributesMap(attributes2, l);

            if (attr1.size() != attr2.size()) {
                return new Diff(node1, attr1.size(), attr2.size(), Type.ATTRIBUTE_SIZE_MISSMATCH);
            } else {
                for (Map.Entry<String, String> k : attr1.entrySet()) {
                    String val1 = k.getValue();
                    String val2 = attr2.get(k.getKey());

                    if (val2 == null) {
                        return new Diff(node1.getAttributes().getNamedItem(k.getKey()), k.getKey(),
                            Type.ATTRIBUTE_MISSING);
                    }
                    if (val1.equals(val2)) {
                        continue;
                    } else {
                        // We have to take care of cases where an attribute can
                        // be 1.0 in one
                        // document and 1 in the other.
                        // There could also be cases such as 1.000 and 1.0, so
                        // string comparison is not that easy.
                        // BigDecimal is used in order to avoid floating point
                        // errors
                        BigDecimal bd1 = parseNumber(val1);
                        BigDecimal bd2 = parseNumber(val2);

                        if (bd1 != null && bd2 != null) {
                            int maxScale = Math.max(bd1.scale(), bd2.scale());
                            bd1 = bd1.setScale(maxScale);
                            bd2 = bd2.setScale(maxScale);
                            if (!bd1.equals(bd2)) {
                                return new Diff(node1.getAttributes().getNamedItem(k.getKey()), bd1, bd2,
                                    Type.ATTRIBUTE_MISSMATCH);
                            }
                        } else {
                            return new Diff(node1.getAttributes().getNamedItem(k.getKey()), val1, val2,
                                Type.ATTRIBUTE_MISSMATCH);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param val1
     * @return
     */
    private static BigDecimal parseNumber(final String val1) {
        try {
            if (val1.matches(NUMBER_REGEX)) {
                return new BigDecimal(val1.replace('e', 'E'));
            }
        } catch (NumberFormatException e) {
            // Thats ok as the caller will return a new CompareResult
        }
        return null;
    }

    private static Map<String, String> createAttributesMap(final NamedNodeMap expectedAttrs,
        final InputFilter<Node> filter) {
        Map<String, String> toReturn = new HashMap<String, String>();

        for (int i = 0; i < expectedAttrs.getLength(); i++) {
            Attr expectedAttr = (Attr)expectedAttrs.item(i);
            if (!expectedAttr.getName().startsWith("xmlns") && filter.include(expectedAttr)) {
                toReturn.put(getQualifiedName(expectedAttr), expectedAttr.getValue());
            }
        }
        return toReturn;
    }

    private static List<Node> filterChilds(final NodeList children1, final XmlDomComparerCustomizer l) {
        List<Node> toReturn = new ArrayList<Node>(children1.getLength());
        for (int i = 0; i < children1.getLength(); i++) {
            Node n = children1.item(i);
            if (l.include(n)) {
                toReturn.add(n);
            }
        }
        return toReturn;
    }

    private static int hashAttributes(final NamedNodeMap attributes, final XmlDomComparerCustomizer filter) {
        int hash = 1;
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr)attributes.item(i);
            if (!attribute.getName().startsWith("xmlns") && filter.include(attribute)) {
                hash = hash + getQualifiedName(attribute).hashCode();

                BigDecimal bd1 = parseNumber(attribute.getValue());

                hash = hash + (bd1 == null ? attribute.getValue().hashCode() : bd1.intValue());
            }
        }
        return hash * PRIME;
    }

    /**
     * Comprises a difference in two DOM {@link Document}s. Clients should use the {@link #getReversePath()} function to
     * get a path from the node which differs to the root as {@link Node#getParentNode()} does not always returns a
     * parent, e.g. if the node is an attribute.
     *
     * @author Marcel Hanser
     */
    public static final class Diff {

        /**
         * The type of the difference.
         *
         * @author Marcel Hanser
         */
        public enum Type {
            /**
             * The type of the nodes differ. Expected and actual value contains the differing types.
             */
            TYPE_MISSMATCH,
            /**
             * An element is missing. Expected values contains the expected element name.
             */
            ELEMENT_MISSING,
            /**
             * An attribute is missing. Expected value contains the expected attribute.
             */
            ATTRIBUTE_MISSING,
            /**
             * There are more attributes of an element as on the other elements. Expected and actual values contain the
             * size of the attribute maps.
             */
            ATTRIBUTE_SIZE_MISSMATCH,
            /**
             * A value of an attributes differs. Expected and actual values contains the differing values.
             */
            ATTRIBUTE_MISSMATCH,
            /**
             * The name of an element mismatches. Expected and actual values contain the expected element name.
             */
            ELEMENT_MISSMATCH,
            /**
             * The amount of children of the elements differs. Expected and actual values contain the amount of
             * children.
             */
            ELEMENT_CHILDREN_SIZE_MISSMATCH,
            /**
             * A text differs. Expected and actual values contain the text content.
             */
            TEXT_MISSMATCH,
            /**
             * The other given node is <code>null</code>. Expected value is the string representation of the node which
             * is not null.
             */
            NODE_MISSING;
        }

        private final Node m_node;

        private final String m_expectedValue;

        private final String m_actualValue;

        private final Type m_type;

        private List<Node> m_reversePath;

        private Diff(final Node node, final Object expectedValue, final Type type) {
            this(node, expectedValue, null, type);
        }

        private Diff(final Node node, final Object expectedValue, final Object actualValue, final Type type) {
            m_node = node;
            this.m_expectedValue = expectedValue.toString();
            this.m_actualValue = actualValue == null ? null : actualValue.toString();
            this.m_type = type;
        }

        /**
         * @return the node
         */
        public Node getNode() {
            return m_node;
        }

        /**
         * @return the level
         */
        public int getLevel() {
            return getReversePath().size();
        }

        /**
         * @return the reverse path as a immutable list
         */
        public List<Node> getReversePath() {
            if (m_reversePath == null) {
                ArrayList<Node> toReturn = new ArrayList<Node>();
                Node current;
                switch (m_node.getNodeType()) {
                    case Node.ATTRIBUTE_NODE:
                        current = ((Attr)m_node).getOwnerElement();
                        toReturn.add(m_node);
                        break;
                    default:
                        current = m_node;
                }
                while (current.getParentNode() != null) {
                    toReturn.add(current);
                    current = current.getParentNode();
                }
                m_reversePath = Collections.unmodifiableList(toReturn);
            }
            return m_reversePath;
        }

        /**
         * Depending on {@link #getType()} this might return <code>null</code>.
         *
         * @return the expectedValue
         */
        public String getExpectedValue() {
            return m_expectedValue;
        }

        /**
         * Depending on {@link #getType()} this might return <code>null</code>.
         *
         * @return the actualValue
         */
        public String getActualValue() {
            return m_actualValue;
        }

        /**
         * @return the type
         */
        public Type getType() {
            return m_type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            // why on earth do we store the reverse path???
            List<Node> temp = new ArrayList<>();
            for (int i = getReversePath().size() - 1; i >= 0; i--) {
                temp.add(getReversePath().get(i));
            }

            return getType() + " at /"
                + temp.stream().map(n -> ((n instanceof Attr) ? "@" : "") + n.getNodeName())
                    .collect(Collectors.joining("/"))
                + ": expected '" + getExpectedValue() + "', actual '" + getActualValue() + "'";
        }
    }
}
