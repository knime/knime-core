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
 * Created on 5.10.2019 by Mark Ortmann
 */
package org.knime.core.node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.knime.core.internal.NodeDescriptionUtil;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.context.ports.ExtendablePortGroup;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.util.Pair;
import org.knime.core.util.Version;
import org.knime.node.v53.AbstractView;
import org.knime.node.v53.DynPort;
import org.knime.node.v53.InPort;
import org.knime.node.v53.Intro;
import org.knime.node.v53.Keywords;
import org.knime.node.v53.KnimeNode;
import org.knime.node.v53.KnimeNodeDocument;
import org.knime.node.v53.Option;
import org.knime.node.v53.OutPort;
import org.knime.node.v53.Port;
import org.knime.node.v53.Ports;
import org.knime.node.v53.Tab;
import org.knime.node.v53.View;
import org.knime.node.v53.Views;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link NodeDescription} for node descriptions introduced with 5.3. It uses XMLBeans to extract the
 * information from the XML file.<br>
 * If assertions are enabled (see {@link KNIMEConstants#ASSERTIONS_ENABLED} it also checks the contents of the XML for
 * against the XML schema and reports errors via the logger. This version of the node description supports nodes with
 * modifiable ports.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
public final class NodeDescription53Proxy extends NodeDescription {

    /** The dynamic port suffix. */
    private static final String DYNAMIC_PORT_SUFFIX = " (dynamic)";

    private static final XmlOptions OPTIONS = new XmlOptions();

    private static final NodeLogger logger = NodeLogger.getLogger(NodeDescription53Proxy.class);

    private static final String SCHEMA_VIOLATION_MSG =
        "Node description of '%s' does not conform to the Schema. Violations follow.";

    static {
        Map<String, String> namespaceMap = new HashMap<String, String>(1);
        namespaceMap.put("", KnimeNodeDocument.type.getContentModel().getName().getNamespaceURI());
        OPTIONS.setLoadSubstituteNamespaces(namespaceMap);
    }

    private final KnimeNodeDocument m_document;

    private final Version m_sinceVersion;

    /**
     * Creates a new proxy object using the given XML document. If assertions are enabled (see
     * {@link KNIMEConstants#ASSERTIONS_ENABLED} it also checks the contents of the XML for against the XML schema and
     * reports errors via the logger.
     *
     * @param doc the XML document of the node description XML file
     * @throws XmlException if something goes wrong while analyzing the XML structure
     */
    public NodeDescription53Proxy(final Document doc) throws XmlException {
        m_document = KnimeNodeDocument.Factory.parse(doc.getDocumentElement(), OPTIONS);
        setIsDeprecated(m_document.getKnimeNode().getDeprecated());
        validate();
        m_sinceVersion = null;
    }

    /**
     * Creates a new proxy object using the given XML document. If assertions are enabled (see
     * {@link KNIMEConstants#ASSERTIONS_ENABLED} it also checks the contents of the XML for against the XML schema and
     * reports errors via the logger.
     *
     * @param doc the XML document of the node description XML file
     * @param sinceVersion The version since which this node is available in the KNIME AP
     *
     * @throws XmlException if something goes wrong while analyzing the XML structure
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public NodeDescription53Proxy(final Document doc, final Version sinceVersion) throws XmlException {
        m_document = KnimeNodeDocument.Factory.parse(doc.getDocumentElement(), OPTIONS);
        setIsDeprecated(m_document.getKnimeNode().getDeprecated());
        validate();
        m_sinceVersion = sinceVersion;
    }

    /**
     * Creates a new proxy object using the given knime node document. If assertions are enabled (see
     * {@link KNIMEConstants#ASSERTIONS_ENABLED} it also checks the contents of the XML for against the XML schema and
     * reports errors via the logger.
     *
     * @param doc a knime node document
     */
    public NodeDescription53Proxy(final KnimeNodeDocument doc) {
        this(doc, KNIMEConstants.ASSERTIONS_ENABLED);
    }

    /**
     * Creates a new proxy object using the given knime node document. If assertions are enabled (see
     * {@link KNIMEConstants#ASSERTIONS_ENABLED} it also checks the contents of the XML for against the XML schema and
     * reports errors via the logger.
     *
     * @param doc a knime node document
     * @param validate flag indicating whether or not toe validate the document
     */
    private NodeDescription53Proxy(final KnimeNodeDocument doc, final boolean validate) {
        m_document = doc;
        setIsDeprecated(m_document.getKnimeNode().getDeprecated());
        if (validate) {
            validate();
        }
        m_sinceVersion = null;
    }

    /**
     * Validate against the XML Schema. If violations are found they are reported via the logger as coding problems.
     *
     * @return <code>true</code> if the document is valid, <code>false</code> otherwise
     */
    protected final boolean validate() {
        // this method has default visibility so that we can use it in testcases
        XmlOptions options = new XmlOptions(OPTIONS);
        List<XmlError> errors = new ArrayList<XmlError>();
        options.setErrorListener(errors);
        boolean valid = m_document.validate(options);
        if (!valid) {
            logger.coding(String.format(SCHEMA_VIOLATION_MSG, getNodeName()));
            for (XmlError err : errors) {
                logger.coding(err.toString());
            }
        }

        return validateInsertionIndices(valid);
    }

    private boolean validateInsertionIndices(boolean valid) {
        final Ports ports = m_document.getKnimeNode().getPorts();
        if (!validInsertionIndices(ports.getInPortList(), ports.getDynInPortList())) {
            if (valid) {
                logger.coding(String.format(SCHEMA_VIOLATION_MSG, m_document.getKnimeNode().getName()));
                valid = false;
            }
            logger.coding("The dynInPort insert-before attribute can at most the maximum inPort index plus one.");
        }

        if (!validInsertionIndices(ports.getOutPortList(), ports.getDynOutPortList())) {
            if (valid) {
                logger.coding(String.format(SCHEMA_VIOLATION_MSG, getNodeName()));
                valid = false;
            }
            logger.coding("The dynOutPort insert-before attribute can at most the maximum outPort index plus one.");
        }
        return valid;
    }

    private static boolean validInsertionIndices(final List<? extends Port> ports,
        final List<? extends DynPort> dynPorts) {
        return dynPorts.stream().mapToLong(p -> p.getInsertBefore().longValue()).max()
            .orElse(0) <= ports.stream().mapToLong(i -> i.getIndex().longValue()).max().orElse(-1) + 1;
    }

    @Override
    public String getIconPath() {
        return m_document.getKnimeNode().getIcon();
    }

    @Override
    public String getInportDescription(final int index) {
        if (m_document.getKnimeNode().getPorts() == null) {
            return null;
        }

        for (InPort inPort : m_document.getKnimeNode().getPorts().getInPortList()) {
            if (inPort.getIndex().intValue() == index) {
                return NodeDescriptionUtil.getPrettyXmlText(inPort);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DialogOptionGroup> getDialogOptionGroups() {
        return NodeDescriptionUtil.extractDialogOptionGroups(
                m_document.getKnimeNode().getFullDescription().getOptionList(),
                m_document.getKnimeNode().getFullDescription().getTabList(),
                Tab::getName, Tab::getDescription, Tab::getOptionList, Option::getName, Option::getOptional
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DescriptionLink> getLinks() {
        return NodeDescriptionUtil.extractDescriptionLinks(m_document.getKnimeNode().getFullDescription().getLinkList(),
            l -> l.getHref().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DynamicPortGroupDescription> getDynamicInPortGroups() {
        return NodeDescriptionUtil.extractDynamicPortGroupDescriptions(
            m_document.getKnimeNode().getPorts().getDynInPortList(), DynPort::getName, DynPort::getGroupIdentifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DynamicPortGroupDescription> getDynamicOutPortGroups() {
        return NodeDescriptionUtil.extractDynamicPortGroupDescriptions(
            m_document.getKnimeNode().getPorts().getDynOutPortList(), DynPort::getName, DynPort::getGroupIdentifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getInteractiveViewDescription() {
        AbstractView interactiveViewEl = m_document.getKnimeNode().getInteractiveView();
        return Optional.ofNullable(NodeDescriptionUtil.getPrettyXmlText(interactiveViewEl));
    }

    @Override
    public String getInportName(final int index) {
        if (m_document.getKnimeNode().getPorts() == null) {
            return null;
        }

        for (InPort inPort : m_document.getKnimeNode().getPorts().getInPortList()) {
            if (inPort.getIndex().intValue() == index) {
                return inPort.getName();
            }
        }
        return null;
    }

    @Override
    public String getInteractiveViewName() {
        if (m_document.getKnimeNode().getInteractiveView() != null) {
            return m_document.getKnimeNode().getInteractiveView().getName();
        } else {
            return null;
        }
    }

    @Override
    public String getNodeName() {
        String nodeName = m_document.getKnimeNode().getName();
        if (m_document.getKnimeNode().getDeprecated() && !nodeName.matches("^.+\\s+\\(?[dD]eprecated\\)?$")) {
            return nodeName + " (deprecated)";
        } else {
            return nodeName;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getIntro() {
        Intro introFrag = m_document.getKnimeNode().getFullDescription().getIntro();
        return Optional.ofNullable(NodeDescriptionUtil.getPrettyXmlText(introFrag));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getShortDescription() {
        return Optional
            .ofNullable(m_document.getKnimeNode().getShortDescription());
    }

    @Override
    public String getOutportDescription(final int index) {
        if (m_document.getKnimeNode().getPorts() == null) {
            return null;
        }

        for (OutPort outPort : m_document.getKnimeNode().getPorts().getOutPortList()) {
            if (outPort.getIndex().intValue() == index) {
                return NodeDescriptionUtil.getPrettyXmlText(outPort);
            }
        }
        return null;
    }

    @Override
    public String getOutportName(final int index) {
        if (m_document.getKnimeNode().getPorts() == null) {
            return null;
        }

        for (OutPort outPort : m_document.getKnimeNode().getPorts().getOutPortList()) {
            if (outPort.getIndex().intValue() == index) {
                return outPort.getName();
            }
        }
        return null;
    }

    @Override
    public NodeType getType() {
        try {
            return NodeType.valueOf(m_document.getKnimeNode().getType().toString());
        } catch (IllegalArgumentException ex) {
            logger.error("Unknown node type for " + m_document.getKnimeNode().getName() + ": "
                + m_document.getKnimeNode().getDomNode().getAttributes().getNamedItem("type").getNodeValue(), ex);
            return NodeType.Unknown;
        }
    }

    @Override
    public int getViewCount() {
        Views views = m_document.getKnimeNode().getViews();
        return (views == null) ? 0 : views.sizeOfViewArray();
    }

    @Override
    public String getViewDescription(final int index) {
        if (m_document.getKnimeNode().getViews() == null) {
            return null;
        }

        for (View view : m_document.getKnimeNode().getViews().getViewList()) {
            if (view.getIndex().intValue() == index) {
                return NodeDescriptionUtil.getPrettyXmlText(view);
            }
        }
        return null;
    }

    @Override
    public String getViewName(final int index) {
        if (m_document.getKnimeNode().getViews() == null) {
            return null;
        }

        for (View view : m_document.getKnimeNode().getViews().getViewList()) {
            if (view.getIndex().intValue() == index) {
                return view.getName();
            }
        }
        return null;
    }

    /**
     * @since 5.0
     */
    @Override
    public String[] getKeywords() {
        return Optional.ofNullable(m_document.getKnimeNode().getKeywords()) //
                .map(Keywords::getKeywordArray) //
                .orElse(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * @since 5.0
     */
    @Override
    public Optional<Version> getSinceVersion() {
        return Optional.ofNullable(m_sinceVersion);
    }

    @Override
    protected void setIsDeprecated(final boolean b) {
        super.setIsDeprecated(b);
        m_document.getKnimeNode().setDeprecated(b);
    }

    @Override
    public Element getXMLDescription() {
        return (Element)m_document.getKnimeNode().getDomNode();
    }

    @Override
    NodeDescription createUpdatedNodeDescription(final ModifiablePortsConfiguration portsConfiguration) {
        if (portsConfiguration != null) {
            // get extended port groups
            Map<String, ExtendablePortGroup> extendablePortGrps = portsConfiguration.getExtendablePorts();
            // if there is at least one modified port we have to update the node description
            if (!extendablePortGrps.isEmpty()) {
                final NodeDescription53Proxy modifiedDescription =
                    new NodeDescription53Proxy((KnimeNodeDocument)m_document.copy(), false);
                adaptPortsDescription(modifiedDescription.m_document.getKnimeNode(), extendablePortGrps);
                return modifiedDescription;
            }
        }
        return super.createUpdatedNodeDescription(portsConfiguration);
    }

    private static void adaptPortsDescription(final KnimeNode knimeNode,
        final Map<String, ExtendablePortGroup> extendablePortGrps) {
        final String nodeName = knimeNode.getName();
        final Ports ports = knimeNode.getPorts();
        // adapt input port descriptions
        adaptPortsDescription(nodeName, "input", extendablePortGrps, ports, ports.getDynInPortList(),
            Ports::getInPortList, Ports::addNewInPort, ExtendablePortGroup::definesInputPorts);
        // adapt output port descriptions
        adaptPortsDescription(nodeName, "output", extendablePortGrps, ports, ports.getDynOutPortList(),
            Ports::getOutPortList, Ports::addNewOutPort, ExtendablePortGroup::definesOutputPorts);
    }

    private static void adaptPortsDescription(final String nodeName, final String portLocation,
        final Map<String, ExtendablePortGroup> extendablePortGrps, final Ports ports,
        final List<? extends DynPort> portGrps, final Function<Ports, List<? extends Port>> getPorts,
        final Function<Ports, Port> createPort, final Predicate<ExtendablePortGroup> portRole) {

        // validate the stuff
        validateGroupIdentifiers(nodeName, portLocation, extendablePortGrps, portGrps, portRole);

        // collect all port groups that have a configured ports and count their occurence count
        final List<Pair<DynPort, Integer>> grpDescriptionToAdd = portGrps.stream()//
            .map(pG -> new Pair<DynPort, Integer>(pG,
                extendablePortGrps.get(pG.getGroupIdentifier()).getConfiguredPorts().length))//
            .filter(p -> p.getSecond() > 0)//
            .sorted(new Comparator<Pair<DynPort, Integer>>() {

                @Override
                public int compare(final Pair<DynPort, Integer> o1, final Pair<DynPort, Integer> o2) {
                    return o1.getFirst().getInsertBefore().compareTo(o2.getFirst().getInsertBefore());
                }
            }).collect(Collectors.toList());

        // Empty in cases where only the input or output ports have changed
        if (!grpDescriptionToAdd.isEmpty()) {
            appendPortDescriptions(ports, getPorts, createPort, grpDescriptionToAdd);
        }
    }

    private static void appendPortDescriptions(final Ports ports, final Function<Ports, List<? extends Port>> getPorts,
        final Function<Ports, Port> createPort, final List<Pair<DynPort, Integer>> grpDescriptionToAdd) {
        final List<? extends Port> portList = getPorts.apply(ports);
        long pos = 0;
        long offset = 0;

        final Map<Long, Long> m_idxOffsetsMap = new HashMap<>();
        m_idxOffsetsMap.put(0l, 0l);

        final Iterator<Pair<DynPort, Integer>> iterator = grpDescriptionToAdd.iterator();
        Pair<DynPort, Integer> curPortGrp = iterator.next();
        // update the indices of the existent port descriptions
        for (final Port port : portList) {
            // take care of the possibility of having several group with the same insertion index
            while (curPortGrp != null && curPortGrp.getFirst().getInsertBefore().longValue() == pos) {
                offset += curPortGrp.getSecond();
                if (iterator.hasNext()) {
                    curPortGrp = iterator.next();
                } else {
                    curPortGrp = null;
                }
            }
            ++pos;
            m_idxOffsetsMap.put(pos, offset + 1);
            port.setIndex(BigInteger.valueOf(offset));
            ++offset;
        }
        // no need to add the remaining curPortGrp elements as there can only be one and its insert-before index has to
        // be equal to the current pos
        curPortGrp = null;

        // create the new port descriptions (handles having several group entries with the same insertion index)
        for (final Pair<DynPort, Integer> dynPortPair : grpDescriptionToAdd) {
            final DynPort dynPort = dynPortPair.getFirst();
            pos = dynPort.getInsertBefore().longValue();
            offset = m_idxOffsetsMap.get(pos);
            final long size = dynPortPair.getSecond();
            for (int i = 0; i < size; i++) {
                final Port port = createPort.apply(ports);
                port.newCursor().setTextValue(dynPort.newCursor().getTextValue());
                port.addNewI().newCursor().setTextValue(DYNAMIC_PORT_SUFFIX);
                port.setIndex(BigInteger.valueOf(offset++));
                port.setName(dynPort.getName());
            }
            // in case we have several dyn ports with the same insertion index
            m_idxOffsetsMap.put(pos, offset);
        }
    }

    private static void validateGroupIdentifiers(final String nodeName, final String portLocation,
        final Map<String, ExtendablePortGroup> extendablePortGrps, final List<? extends DynPort> portGrps,
        final Predicate<ExtendablePortGroup> portLocationPredicate) {
        // get all extendable port group names for the given port location predicate
        final String[] extendablePortGrpNames = extendablePortGrps.entrySet().stream()
            .filter(e -> portLocationPredicate.test(e.getValue())).map(e -> e.getKey()).toArray(String[]::new);

        // each port group entry (defined via node description) requires one extendable port group (defined via factory)
        if (extendablePortGrpNames.length < portGrps.size()) {
            logger.coding(String.format(SCHEMA_VIOLATION_MSG, nodeName));
            logger
                .coding("Node description defines more (extendable) " + portLocation + " port groups than its factory");
            return;
        }

        // each extendable port group (defined via factory) requires one port group entry (defined via node description)
        if (extendablePortGrpNames.length > portGrps.size()) {
            logger.coding(String.format(SCHEMA_VIOLATION_MSG, nodeName));
            logger.coding(
                "node factory defines more (extendable) " + portLocation + " port groups than its node description");
            return;
        }

        // ensure same names
        for (final DynPort portGrp : portGrps) {
            // typically very small array and therefore not costly
            if (!Arrays.stream(extendablePortGrpNames).anyMatch(s -> s.equals(portGrp.getGroupIdentifier()))) {
                logger.coding(String.format(SCHEMA_VIOLATION_MSG, nodeName));
                logger.coding("node description and factory contain different (extendable) " + portLocation
                    + " port group identfier");
                return;
            }
        }

        // the order has to be the same
        int idx = 0;
        for (final DynPort portGrp : portGrps) {
            if (!portGrp.getGroupIdentifier().equals(extendablePortGrpNames[idx++])) {
                logger.coding(String.format(SCHEMA_VIOLATION_MSG, nodeName));
                logger.coding("The (extendable) " + portLocation
                    + " port group ordering differs between factory and node description.");
                return;
            }
        }
    }
}
