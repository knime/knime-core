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
 * Created on 28.05.2013 by thor
 */
package org.knime.core.node;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.xmlbeans.XmlObject;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.knime.core.internal.NodeDescriptionUtil;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.util.Version;
import org.w3c.dom.Element;

/**
 * This abstract class describes the meta information about a node. It is used by the {@link NodeFactory}. Usually the
 * meta information is read from the XML files which accompany every node factory. But also other means of providing
 * node descriptions are possible.
 *
 * Version 4.5 introduces getters for specific parts of the node description. These are assumed to return an empty
 * {@link Optional} (or an empty {@link List}) if the part is not present or not supported in the concrete node
 * description schema. Returned strings may contain HTML markup tags, these are assumed to come without namespace
 * prefixes or xmlns attributes.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @since 2.8
 */
public abstract class NodeDescription {

    /** Creating the DocumentBuilderFactory, hence a lazy initializer, see AP-8171. */
    private static final LazyInitializer<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY_INITIALIZER =
        new LazyInitializer<DocumentBuilderFactory>() {

            @Override
            protected DocumentBuilderFactory initialize() {
                return initializeDocumentBuilderFactory();
            }
        };

    private boolean m_deprecated;

    /**
     * Returns the path to the node's icon. The path should be either absolute (with the root being the root of the
     * classpath) or relative to the node factory's location.
     *
     * @return the path to the node icon, or <code>null</code> if no icon exists
     * @see NodeFactory#getIcon()
     */
    public abstract String getIconPath();

    /**
     * Returns a description for an input port.
     *
     * @param index the index of the input port, starting at 0. Does not include the single flow-variable port that
     *              is present on any node.
     * @return an input port description or <code>null</code> if no description exists
     * @see NodeFactory#getInportDescription(int)
     */
    public abstract String getInportDescription(final int index);

    /**
     * Returns a name for an input port.
     *
     * @param index the index of the input port, starting at 0. Does not include the single flow-variable port that
    *              is present on any node.
     * @return an input port name or <code>null</code> if not name is known
     * @see NodeFactory#getInportName(int)
     */
    public abstract String getInportName(final int index);

    /**
     * Returns the name of the interactive view if such a view exists. Otherwise <code>null</code> is returned.
     *
     * @return name of the interactive view or <code>null</code>
     * @see NodeFactory#getInteractiveViewName()
     */
    public abstract String getInteractiveViewName();

    /**
     * @return the description of the interactive view. May contain HTML markup.
     * @since 4.6
     */
    public Optional<String> getInteractiveViewDescription() {
        return Optional.empty();
    }

    /**
     * Returns the name of this node.
     *
     * @return the node's name or <code>null</code> if no name is known
     * @see NodeFactory#getNodeName()
     */
    public abstract String getNodeName();

    /**
     * @return the introduction text of the full node description. May contain HTML markup.
     * @since 4.6
     */
    public Optional<String> getIntro() {
        return Optional.empty();
    }

    /**
     * Should return empty list if no groups present.
     *
     * @return a list of dialog option groups
     * @since 4.6
     */
    public List<DialogOptionGroup> getDialogOptionGroups() {
        return Collections.emptyList();
    }

    /**
     * Should return empty list if no links present.
     *
     * @return a list of links
     * @since 4.6
     */
    public List<DescriptionLink> getLinks() {
        return Collections.emptyList();
    }

    /**
     * @return the short description of the node
     * @since 4.6
     */
    public Optional<String> getShortDescription() {
        return Optional.empty();
    }

    /**
     * @return the dynamic input port group descriptions.
     * @since 4.6
     */
    public List<DynamicPortGroupDescription> getDynamicInPortGroups() {
        return Collections.emptyList();
    }

    /**
     * @return the dynamic output port group descriptions.
     * @since 4.6
     */
    public List<DynamicPortGroupDescription> getDynamicOutPortGroups() {
        return Collections.emptyList();
    }

    /**
     * Returns a description for an output port.
     *
     * @param index the index of the output port, starting at 0. Does not include the single flow-variable port that
     *              is present on any node.
     * @return an output port description or <code>null</code> if not description exists
     * @see NodeFactory#getOutportDescription(int)
     */
    public abstract String getOutportDescription(final int index);

    /**
     * Returns a name for an output port.
     *
     * @param index the index of the output port, starting at 0. Does not include the single flow-variable port that
     *      *              is present on any node.
     * @return an output port name or <code>null</code> if no name is known
     * @see NodeFactory#getOutportName(int)
     */
    public abstract String getOutportName(final int index);

    /**
     * Returns the type of the node.
     *
     * @return the node's type
     * @see NodeFactory#getType()
     */
    public abstract NodeType getType();

    /**
     * Returns the number of views which are listed in the meta information.
     *
     * @return the number of views
     */
    public abstract int getViewCount();

    /**
     * Returns a description for a view.
     *
     * @param index the index of the view, starting at 0
     * @return a view description or <code>null</code> if no description exists
     * @see NodeFactory#getViewDescription(int)
     */
    public abstract String getViewDescription(final int index);

    /**
     * Returns the name for this node's view at the given index.
     *
     * @param index the view index, starting at 0
     * @return the view's name or <code>null</code> if no name is known
     */
    public abstract String getViewName(final int index);

    /**
     * The XML description can be used with the <code>NodeFactoryHTMLCreator</code> in order to get a converted HTML
     * description of it, which fits the overall KNIME HTML style.
     *
     * @return XML description of this node
     */
    public abstract Element getXMLDescription();

    /**
     * A list of keywords of the node, used to improve search by node name.
     *
     * @return The non-null list of keywords, often an empty array.
     * @since 5.0
     */
    public String[] getKeywords() {
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    /**
     * Returns the KNIME AP version in which this node was first available.
     * If this information is not available, returns an empty {@link Optional}.
     *
     * @return The KNIME AP version since which this node is available
     * @since 5.0
     */
    public Optional<Version> getSinceVersion() {
        return Optional.empty();
    }

    /**
     * Utility function which strips the <tt>&lt;xml-fragment&gt;</tt> start and end tags from the strings returned by
     * <tt>xmlText()</tt>.
     *
     * @param xmlObject an xmlObject whose contents should be serialized into a string
     * @return a string representation of the XML contents
     */
    protected static String stripXmlFragment(final XmlObject xmlObject) {
        String contents = xmlObject.xmlText();
        return NodeDescriptionUtil.stripXmlFragment(contents);
    }

    /**
     * Flags this node as deprecated node. Subclasses should override this method and modify the XML description
     * accordingly.
     *
     * @param b <code>true</code> if this node is node is deprecated, <code>false</code> otherwise
     * @since 3.0
     */
    protected void setIsDeprecated(final boolean b) {
        m_deprecated = b;
    }

    /**
     * Returns whether the node is deprecated.
     *
     * @return <code>true</code> if the node is deprecated, <code>false</code> otherwise
     * @since 3.0
     */
    public boolean isDeprecated() {
        return m_deprecated;
    }

    /**
     * Returns an updated instance of the node description. This is required if the node contains configurable ports.
     *
     * @param portsConfiguration the ports configuration can be {@code null}
     * @return an updated version of the node description
     *
     */
    NodeDescription createUpdatedNodeDescription(final ModifiablePortsConfiguration portsConfiguration) {
        return this;
    }

    /**
     * Get a singleton instance of a {@link DocumentBuilderFactory}, initialized lazily.
     *
     * @return That singleton instance.
     * @noreference This method is not intended to be referenced by clients.
     */
    public static DocumentBuilderFactory getDocumentBuilderFactory() {
        try {
            return DOCUMENT_BUILDER_FACTORY_INITIALIZER.get();
        } catch (ConcurrentException ex) {
            // will not be thrown as the 'initialize' method doesn't throw that exception
            NodeLogger.getLogger(NodeDescription.class).error(ex.getMessage(), ex);
            return initializeDocumentBuilderFactory();
        }
    }

    private static DocumentBuilderFactory initializeDocumentBuilderFactory() {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        return fac;
    }

    /**
     * Record to hold name and description of a node dialog option.
     *
     * @since 4.6
     */
    public static final class DialogOption {
        @NonNull
        private final String m_name;

        @NonNull
        private final String m_description;

        private final boolean m_optional;

        /**
         * @param name The name of the dialog option.
         * @param description The description of the dialog option.
         */
        public DialogOption(@NonNull final String name, @NonNull final String description, final boolean isOptional) {
            m_name = name;
            m_description = description;
            m_optional = isOptional;
        }

        /**
         * @return The name of the dialog option. Assumed to be a simple string.
         */
        public String getName() {
            return m_name;
        }

        /**
         * @return The description of the dialog option. May contain markup tags that resemble HTML formatting.
         */
        public String getDescription() {
            return m_description;
        }

        /**
         * @return Whether the dialog option is marked as optional
         */
        public boolean isOptional() {
            return m_optional;
        }

    }

    /**
     * Record to represent a dialog option group. Name and description may be null. In this case, this record is thought
     * to represent a set of ungrouped dialog options.
     *
     * @since 4.6
     */
    public static final class DialogOptionGroup {
        @Nullable
        private final String m_name;

        @Nullable
        private final String m_description;

        @NonNull
        private final List<DialogOption> m_options;

        /**
         * @param name the name of the option group
         * @param description an optional description of the option group. Can be given as null.
         * @param options a list of options belonging to the group being created.
         */
        public DialogOptionGroup(@Nullable final String name, @Nullable final String description,
            @NonNull final List<DialogOption> options) {
            m_name = name;
            m_description = description;
            m_options = options;
        }

        /**
         * @return the name of this option group if available, else an empty optional.
         */
        public Optional<String> getName() {
            return Optional.ofNullable(m_name);
        }

        /**
         * @return an Optional containing the description of this option group if available, else an empty optional.
         */
        public Optional<String> getDescription() {
            return Optional.ofNullable(m_description);
        }

        /**
         * @return the options belonging to this option group.
         */
        public List<DialogOption> getOptions() {
            return m_options;
        }

    }

    /**
     * Record to represent a link in the node description. Note that these are not links in freeform text but from an
     * explicit list of links defined in some description schemas.
     *
     * @since 4.6
     */
    public static final class DescriptionLink {
        @NonNull
        private final String m_target;

        @NonNull
        private final String m_text;

        /**
         * @param href the link target
         * @param text the link text
         */
        public DescriptionLink(@NonNull final String href, @NonNull final String text) {
            this.m_target = href;
            this.m_text = text;
        }

        /**
         * @return the link target. Assumed to resemble an URL.
         */
        public String getTarget() {
            return m_target;
        }

        /**
         * @return the link text.
         */
        public String getText() {
            return m_text;
        }

    }

    /**
     * Record to represent a dynamic port group. Does not hold information on possible port types since this has to be
     * inferred from the {@link Node}.
     *
     * @since 4.6
     */
    public static final class DynamicPortGroupDescription {

        @NonNull
        private final String m_groupName;

        @NonNull
        private final String m_groupIdentifier;

        @NonNull
        private final String m_groupDescription;

        /**
         * @param groupName The name of the dynamic port group
         * @param groupIdentifier The identifier of the dynamic port group
         * @param groupDescription The description of the dynamic port group
         */
        public DynamicPortGroupDescription(@NonNull final String groupName, @NonNull final String groupIdentifier,
            @NonNull final String groupDescription) {
            m_groupName = groupName;
            m_groupIdentifier = groupIdentifier;
            m_groupDescription = groupDescription;
        }

        /**
         * @return The name of the dynamic port group
         */
        public String getGroupName() {
            return m_groupName;
        }

        /**
         * @return The description of the dynamic port group
         */
        public String getGroupDescription() {
            return m_groupDescription;
        }

        /**
         * @return The identifier of the dynamic port group
         */
        public String getGroupIdentifier() {
            return m_groupIdentifier;
        }

    }
}
