/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2013
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 */
package org.knime.workbench.editor2.editparts;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 *
 * @author ohl, University of Konstanz
 */
public class NodeContainerProperties implements IPropertySource {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodeContainerProperties.class);

    private static final String PROP_INVALID_ID =
            "knime.workbench.settingsview.invalid";

    /* also used as a regExpr! */
    private static final String CONFIG_SEPARATOR = "/";

    private final WeakReference<NodeContainer> m_nodeRef;

    private Config m_settings;

    private final String m_prefix;

    /**
     * Constructor used to start property browsing.
     * @param node the underlying node.
     */
    public NodeContainerProperties(final NodeContainer node) {
        m_nodeRef = new WeakReference<NodeContainer>(node);
        NodeSettings nodeSettings = new NodeSettings("Props");
        WorkflowManager wfm = node.getParent();
        try {
            wfm.saveNodeSettings(node.getID(), nodeSettings);
            m_settings = nodeSettings.getConfig("model");
        } catch (InvalidSettingsException e) {
            m_settings = null;
        }
        m_prefix = "";
    }

    /**
     * Constructor used only for sub-configs.
     * @param node underlying node
     * @param subConfig the sub-config
     * @param prefix including the name of the passed config.
     */
    public NodeContainerProperties(final NodeContainer node,
            final Config subConfig, final String prefix) {
        m_nodeRef = new WeakReference<NodeContainer>(node);
        m_settings = subConfig;
        m_prefix = prefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getEditableValue() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        NodeContainer node = getNode();
        if (node == null) {
            return null;
        }
        if (!(node instanceof SingleNodeContainer)) {
            // only simple nodes have settings. Yet.
            return null;
        }
        if (m_settings != null) {
            return getDescriptors();
        } else {
            return new IPropertyDescriptor[]{new PropertyDescriptor(
                    PROP_INVALID_ID,
                    "Error(s) during computation of node's settings.")};
        }

    }

    /**
     * Could be null if the node is disposed of.
     *
     * @return could be null if the node is disposed of.
     */
    protected NodeContainer getNode() {
        return m_nodeRef.get();
    }

    /**
     * @return see {@link #getPropertyDescriptors()}
     */
    protected IPropertyDescriptor[] getDescriptors() {
        ArrayList<IPropertyDescriptor> descriptors =
                new ArrayList<IPropertyDescriptor>();

        // iterate through all settings in the config
        for (Enumeration<TreeNode> it = m_settings.children(); it
                .hasMoreElements();) {
            AbstractConfigEntry prop = (AbstractConfigEntry)it.nextElement();
            // the id should be globally unique
            String hierID =
                    m_prefix.isEmpty() ? prop.getKey() : m_prefix
                            + CONFIG_SEPARATOR + prop.getKey();

            if (prop instanceof Config) {
                // sub-config
                descriptors.add(new PropertyDescriptor(hierID, prop.getKey()));
            } else {
                // all settings are displayed as string
                String typeName = prop.getType().name().substring(1);
                // we don't have a label yet
                String label = prop.getKey() + " (" + typeName + ")";
                switch (prop.getType()) {
                // if cases are changed here, setPropertyValue must be adapted
                case xboolean:
                case xbyte:
                case xchar:
                case xdouble:
                case xfloat:
                case xint:
                case xlong:
                case xshort:
                case xstring:
                    // editable types
                    descriptors.add(new TextPropertyDescriptor(hierID, label));
                    break;
                default:
                    descriptors.add(new PropertyDescriptor(hierID, label));
                    break;
                }
            }
        }
        return descriptors.toArray(new IPropertyDescriptor[descriptors.size()]);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getPropertyValue(final Object id) {
        if (id instanceof String) {
            String hierID = (String)id;

            // cut off our prefix from the ID
            if (!hierID.startsWith(m_prefix)) {
                return "ERROR: Unexpected property id: " + hierID
                        + " (while in sub-config " + m_prefix + ")";
            }
            String ourID = hierID;
            if (!m_prefix.isEmpty()) {
                assert hierID.charAt(m_prefix.length()) == CONFIG_SEPARATOR
                        .charAt(0);
                // + 1 for removing the separator
                ourID = hierID.substring(m_prefix.length() + 1);
            }
            AbstractConfigEntry entry = m_settings.getEntry(ourID);
            if (entry instanceof Config) {
                return new NodeContainerProperties(getNode(), (Config)entry,
                        m_prefix.isEmpty() ? ourID : m_prefix
                                + CONFIG_SEPARATOR + ourID);
            } else {
                if (entry == null) {
                    return "ERROR: No value for key " + ourID;
                }
                return TokenizerSettings.printableStr(entry.toStringValue());
            }
        }
        return null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertySet(final Object id) {
        if (id instanceof String) {
            return m_settings.containsKey((String)id);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetPropertyValue(final Object id) {
        LOGGER.debug("Reset Property Value. id=" + id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPropertyValue(final Object id, final Object value) {
        if ((id instanceof String) && (value instanceof String)) {
            String strVal = (String)value;
            String strID = (String)id;
            if (strID.startsWith(m_prefix)) {
                String[] hierarchy = strID.split(CONFIG_SEPARATOR);
                String key = hierarchy[hierarchy.length - 1];
                // apply it to the node's settings:
                NodeContainer node = getNode();
                if (node == null) {
                    return;
                }
                WorkflowManager wfm = node.getParent();
                NodeSettings nodeSettings = new NodeSettings("Transfer");
                NodeSettings settings;
                try {
                    wfm.saveNodeSettings(node.getID(), nodeSettings);
                    // overwrite our config in the settings
                    settings = nodeSettings.getNodeSettings("model");
                    if (hierarchy.length > 1) {
                        for (int i = 0; i < hierarchy.length - 1; i++) {
                            settings = settings.getNodeSettings(hierarchy[i]);
                            if (settings == null) {
                                return;
                            }
                        }
                    }
                } catch (InvalidSettingsException e) {
                    // somehow node is not able to save its settings anymore
                    return;
                }
                AbstractConfigEntry entry = settings.getEntry(key);
                if (entry == null || entry instanceof Config) {
                    // settings are not complete or correct anymore
                    return;
                }
                switch (entry.getType()) {
                case xboolean:
                    settings.addBoolean(key, Boolean.parseBoolean(strVal));
                    break;
                case xbyte:
                    settings.addByte(key, Byte.parseByte(strVal));
                    break;
                case xchar:
                    String decoded = TokenizerSettings.unescapeString(strVal);
                    settings.addChar(key, decoded.charAt(0));
                    break;
                case xdouble:
                    settings.addDouble(key, Double.parseDouble(strVal));
                    break;
                case xfloat:
                    settings.addFloat(key, Float.parseFloat(strVal));
                    break;
                case xint:
                    settings.addInt(key, Integer.parseInt(strVal));
                    break;
                case xlong:
                    settings.addLong(key, Long.parseLong(strVal));
                    break;
                case xshort:
                    settings.addShort(key, Short.parseShort(strVal));
                    break;
                case xstring:
                    String dec = TokenizerSettings.unescapeString(strVal);
                    settings.addString(key, dec);
                    break;
                default:
                    // ignore the new value
                    return;
                }
                try {
                    wfm.loadNodeSettings(node.getID(), nodeSettings);
                } catch (Throwable t) {
                    LOGGER.error("Invalid Value (" + strVal + "): "
                            + t.getMessage(), t);
                    return;
                }

                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Sub-Config";
    }
}
