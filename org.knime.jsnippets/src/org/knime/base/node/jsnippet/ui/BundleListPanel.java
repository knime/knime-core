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
 *   13.09.2017 (Jonathan Hale): created
 */
package org.knime.base.node.jsnippet.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.eclipse.core.runtime.Platform;
import org.knime.core.node.util.filter.ArrayListModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Panel for adding Bundles to the Java Snippet node for compilation.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.5
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class BundleListPanel extends JPanel implements TreeWillExpandListener {

    private static final long serialVersionUID = 1L;

    /** Entry in the Bundle List */
    static class BundleListEntry {
        /** Name of the bundle */
        final String name;

        final Version installedVersion;

        final Version savedVersion;

        /**
         * Constructor
         *
         * @param name
         * @param installedVersion
         * @param savedVersion Version the bundle was saved with, in case it significantly differs from the installed
         *            version.
         */
        public BundleListEntry(final String name, final Version installedVersion, final Version savedVersion) {
            this.name = name;
            this.installedVersion = installedVersion;
            this.savedVersion = savedVersion;
        }

        @Override
        public String toString() {
            return this.name + " " + this.installedVersion.toString();
        }

        /**
         * Whether this bundle is installed in the current eclipse runtime
         *
         * @return whether this bundle is installed.
         */
        public boolean exists() {
            return installedVersion != null;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof String) {
                return this.name.equals(obj);
            } else if (obj instanceof BundleListEntry) {
                return this.name.equals(((BundleListEntry)obj).name);
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }
    }

    class BundleListEntryRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
            final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
            final Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
            if (!(userObject instanceof BundleListEntry)) {
                /* Bundle dependency */
                final Component c =
                    super.getTreeCellRendererComponent(tree, userObject, sel, expanded, leaf, row, hasFocus);
                c.setForeground(Color.GRAY);
                return c;
            }

            final BundleListEntry e = (BundleListEntry)userObject;
            final Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (!e.exists()) {
                /* Bundle not found */
                c.setForeground(Color.RED);
                if (c instanceof JLabel) {
                    ((JLabel)c).setToolTipText("Bundle is not installed.");
                }
            } else if (e.savedVersion != null) {
                /* Bundle version changed */
                c.setForeground(Color.ORANGE);
                if (c instanceof JLabel) {
                    ((JLabel)c).setToolTipText(String.format(
                        "Installed version (%s) is different than the version when this workflow was saved (%s).",
                        e.installedVersion, e.savedVersion));
                }
            }

            return c;
        }
    }

    /* Bundle Tree display */
    final DefaultMutableTreeNode m_rootNode = new DefaultMutableTreeNode("Active Bundles");
    final DefaultMutableTreeNode m_customTypeRoot = new DefaultMutableTreeNode("Custom Type Bundles");

    final JTree m_tree = new JTree(m_rootNode);
    final JTree m_customTypeTree = new JTree(m_customTypeRoot);

    /* Filterable list model containing all available bundles */
    final ArrayListModel<BundleListEntry> m_listModel = new ArrayListModel<>();

    /* Field for filtering the bundle list */
    final JTextField m_filterField = new JTextField();

    final JList<String> m_bundleList;

    final FilterableListModel m_bundleModel = new FilterableListModel(bundleNames);

    /* All available bundle names */
    final static ArrayList<String> bundleNames = new ArrayList<>();

    /* Find all available bundles */
    private static void initBundleNames() {
        if (!bundleNames.isEmpty()) {
            return;
        }

        final BundleContext ctx = FrameworkUtil.getBundle(BundleListPanel.class).getBundleContext();
        for (final Bundle bundle : ctx.getBundles()) {
            bundleNames.add(bundle.getSymbolicName() + " " + bundle.getVersion().toString());
        }

        Collections.sort(bundleNames);
    }

    /* List model which filters bundleNames according to a search string */
    final class FilterableListModel extends AbstractListModel<String> {

        private static final long serialVersionUID = 1L;

        private final List<String> m_unfiltered;

        private List<String> m_filtered;

        private List<String> m_excluded = Collections.emptyList();

        /* Filter string, only ever null to force refiltering */
        private String m_filter = "";

        /**
         * Constructor
         *
         * @param unfiltered Unfiltered list of elements
         */
        public FilterableListModel(final List<String> unfiltered) {
            m_filtered = m_unfiltered = unfiltered;
        }

        @Override
        public int getSize() {
            return m_filtered.size();
        }

        @Override
        public String getElementAt(final int index) {
            return m_filtered.get(index);
        }

        /**
         * Set the string with which to filter the elements of this list.
         *
         * @param filter Filter string
         */
        public synchronized void setFilter(final String filter) {
            if (m_filter != null && m_filter.equals(filter)) {
                return;
            }
            if (m_filter != null && filter.startsWith(m_filter)) {
                // the most common use case will be a list gradually refined by user typing more characters
                m_filtered = m_filtered.stream().filter(s -> s.contains(filter)).collect(Collectors.toList());
            } else if (filter.isEmpty()) {
                m_filtered = m_unfiltered;
            } else {
                m_filtered = m_unfiltered.stream().filter(s -> s.contains(filter)).collect(Collectors.toList());
            }
            m_filter = filter;
            m_filtered.removeAll(m_excluded);
            this.fireContentsChanged(this, 0, bundleNames.size());
        }

        /**
         * Set list of excluded elements. Will cause the list to be refiltered.
         *
         * @param list List of elements to exclude.
         */
        public synchronized void setExcluded(final List<String> list) {
            m_excluded = list;
            final String filter = m_filter;
            m_filter = null;
            setFilter(filter);
        }
    }

    /**
     * Constructor
     */
    public BundleListPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        initBundleNames();

        final JPanel treesPane = new JPanel();
        treesPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        final BundleListEntryRenderer renderer = new BundleListEntryRenderer();
        m_tree.setCellRenderer(renderer);
        m_tree.addTreeWillExpandListener(this);
        m_tree.expandPath(new TreePath(m_rootNode));
        treesPane.add(m_tree, gbc);

        m_customTypeTree.setCellRenderer(renderer);
        m_customTypeTree.addTreeWillExpandListener(this);
        m_customTypeTree.expandPath(new TreePath(m_customTypeRoot));
        gbc.weighty = 1.0;
        treesPane.add(m_customTypeTree, gbc);

        final JScrollPane scroll = new JScrollPane(treesPane);
        scroll.setMinimumSize(new Dimension(300, 300));
        scroll.setPreferredSize(new Dimension(300, 300));
        add(scroll);

        final JPanel pane = new JPanel(new BorderLayout());
        {
            final JPanel northPane = new JPanel(new GridLayout(2, 1));
            northPane.add(new JLabel("Double-click bundle to add it together with its dependencies."));

            m_filterField.requestFocusInWindow();
            northPane.add(m_filterField);
            pane.add(northPane, BorderLayout.NORTH);

            m_bundleList = new JList<>(m_bundleModel);
            final JScrollPane scrollPane = new JScrollPane(m_bundleList);
            pane.add(scrollPane, BorderLayout.CENTER);

            m_bundleList.addMouseListener(new MouseListener() {

                @Override
                public void mouseReleased(final MouseEvent e) {
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                }

                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Double click closes the dialog
                        final String bundleName = m_bundleList.getSelectedValue();
                        addBundle(bundleName);
                        m_bundleModel.setExcluded(m_listModel.getAllElements().stream().map(Object::toString).collect(Collectors.toList()));
                        m_bundleList.clearSelection();
                    }
                }
            });
            m_bundleList.addKeyListener(new KeyListener() {

                @Override
                public void keyTyped(final KeyEvent e) {
                }

                @Override
                public void keyReleased(final KeyEvent e) {
                }

                @Override
                public void keyPressed(final KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        addBundles(m_bundleList.getSelectedValuesList());
                        m_bundleModel.setExcluded(m_listModel.getAllElements().stream().map(Object::toString).collect(Collectors.toList()));
                        m_bundleList.clearSelection();
                    }
                }
            });

            m_filterField.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    update(e);
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    update(e);
                }

                @Override
                public void changedUpdate(final DocumentEvent e) {
                    update(e);
                }

                void update(final DocumentEvent e) {
                    try {
                        final Document doc = e.getDocument();
                        m_bundleModel.setFilter(doc.getText(0, doc.getLength()));
                    } catch (BadLocationException e1) {
                        // Will never happen
                    }
                }
            });
        }

        /* Remove button */
        final JButton removeBundleButton = new JButton("Remove");
        removeBundleButton.addActionListener(e -> removeSelectedBundles());

        final JPanel southPane = new JPanel(new FlowLayout());
        southPane.add(removeBundleButton);
        add(southPane);

        /* Label for list of available bundles */
        final JPanel northPane = new JPanel(new FlowLayout());
        northPane.add(new JLabel("Add bundles from the current eclipse environment as dependencies."));
        add(northPane);

        add(pane);
    }

    /* Remove all bundles currently selected in list */
    void removeSelectedBundles() {
        for (final TreePath p : m_tree.getSelectionPaths()) {
            if (p.getPathCount() != 2) {
                /* Either root or a dependency */
                continue;
            }
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode)p.getLastPathComponent();
            ((DefaultTreeModel)m_tree.getModel()).removeNodeFromParent(node);

            final Object o = node.getUserObject();
            m_listModel.remove(o);
        }
    }

    /**
     * @return All added bundles.
     */
    public String[] getBundles() {
        final String[] bundles = new String[m_listModel.getSize()];

        for (int i = 0; i < m_listModel.getSize(); ++i) {
            bundles[i] = m_listModel.get(i).toString();
        }

        return bundles;
    }

    /**
     * @param bundles
     */
    public void setBundles(final String[] bundles) {
        m_rootNode.removeAllChildren();
        m_listModel.clear();

        addBundles(Arrays.asList(bundles));
    }

    /**
     * Add a bundle to the bundle list.
     *
     * @param bundleName Name of the bundle to add.
     * @return true if adding was successful, false if bundle with given name was not found.
     */
    public boolean addBundle(final String bundleName) {
        final BundleListEntry e = makeBundleListEntry(bundleName);
        if (e == null || m_listModel.contains(e.name)) {
            return false;
        }

        m_listModel.add(e);

        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(e);

        addDependenciesForNode(node, Platform.getBundle(e.name));
        ((DefaultTreeModel)m_tree.getModel()).insertNodeInto(node, m_rootNode, m_rootNode.getChildCount());

        return true;
    }

    /**
     * Add given bundle names to list. Duplicates and null names are skipped.
     *
     * @param list The bundle names to add
     */
    public void addBundles(final List<String> list) {
        final LinkedHashSet<BundleListEntry> entries = new LinkedHashSet<>(list.size());
        for (final String b : list) {
            if (b == null) {
                continue;
            }

            final BundleListEntry e = makeBundleListEntry(b);
            if (m_listModel.contains(e.name)) {
                continue;
            }

            entries.add(e);
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(e);
            ((DefaultTreeModel)m_tree.getModel()).insertNodeInto(node, m_rootNode, m_rootNode.getChildCount());
            addDependenciesForNode(node, Platform.getBundle(e.name));
        }

        m_listModel.addAll(entries);
    }

    /* Create a BundleListEntry from bundleName */
    private static BundleListEntry makeBundleListEntry(final String bundleName) {
        if (bundleName == null) {
            return null;
        }

        final String[] split = bundleName.split(" ");
        final String nameWithoutVersion = split[0];
        final Bundle firstBundle = Platform.getBundle(nameWithoutVersion);

        if (firstBundle != null) {
            final Version installedVersion = firstBundle.getVersion();
            Version savedVersion = null;
            if (split.length > 1) {
                final Version v = Version.parseVersion(split[1]);

                // check whether the versions differ up to minor version.
                final boolean versionsDiffer =
                    installedVersion.getMajor() != v.getMajor() || installedVersion.getMinor() != v.getMinor();
                if (versionsDiffer) {
                    savedVersion = v;
                }
            }

            return new BundleListEntry(firstBundle.getSymbolicName(), installedVersion, savedVersion);
        } else {
            return new BundleListEntry(nameWithoutVersion, null, null);
        }
    }

    /**
     * @return Model of main List
     */
    public ListModel<BundleListEntry> getListModel() {
        return m_listModel;
    }

    private void addDependenciesForNode(final DefaultMutableTreeNode node, final Bundle b) {
        if (b == null) {
            return;
        }

        for (BundleWire dep : b.adapt(BundleWiring.class).getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE)) {
            final Bundle depBundle = dep.getProviderWiring().getBundle();
            final DefaultMutableTreeNode child = new DefaultMutableTreeNode(depBundle);
            node.add(child);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
        Enumeration<DefaultMutableTreeNode> children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = children.nextElement();
            if (child != null && child.getUserObject() instanceof Bundle) {
                addDependenciesForNode(child, (Bundle)child.getUserObject());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void treeWillCollapse(final TreeExpansionEvent event) throws ExpandVetoException {
    }

    /** @brief Bundles used for custom types. */
    public void setCustomTypeBundles(final Collection<Bundle> bundles) {
        for(Bundle b : bundles) {
            ((DefaultTreeModel)m_customTypeTree.getModel()).insertNodeInto(new DefaultMutableTreeNode(b), m_customTypeRoot, 0);
        }
    }

}
