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
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

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
            if (installedVersion == null) {
                return this.name + " (Not installed!)";
            } else {
                return this.name + " " + this.installedVersion.toString();
            }
        }

        /**
         * @return Symbolic name of the bundle
         */
        public String getName() {
            return this.name;
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

    /* Renderer for BundleListEntry and Bundle in the tree component */
    private class BundleListEntryRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
            final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
            Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
            if (userObject instanceof Bundle) {
                final Bundle b = (Bundle)userObject;
                userObject = String.format("%s %s", b.getSymbolicName(), b.getVersion());
            }
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
    final DefaultMutableTreeNode m_root = new DefaultMutableTreeNode("This node should be invisible.");

    final DefaultMutableTreeNode m_userBundlesRoot = new DefaultMutableTreeNode("Active Bundles");

    final DefaultMutableTreeNode m_customTypeRoot = new DefaultMutableTreeNode("Custom Type Bundles");

    final JTree m_tree = new JTree(m_root);

    /* Filterable list model containing all available bundles */
    final ArrayListModel<BundleListEntry> m_listModel = new ArrayListModel<>();

    /* Field for filtering the bundle list */
    final JTextField m_filterField = new JTextField();

    final JList<String> m_bundleList;

    final FilterableListModel m_bundleModel = new FilterableListModel(bundleNames);

    /* All available bundle names */
    final static List<String> bundleNames = new ArrayList<>();

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

        /* Only keep latest versions of bundles of which multiple versions are installed */
        String[] lastSplit = new String[]{null, null};
        final ArrayList<String> toRemove = new ArrayList<>();
        for (final String s : bundleNames) {
            final String[] split = s.split(" ");
            if(split[0].equals(lastSplit[0])) {
                if(Version.parseVersion(split[1]).compareTo(Version.parseVersion(lastSplit[1])) < 0) {
                    toRemove.add(s);
                } else {
                    toRemove.add(String.join(" ", lastSplit));
                }
            }
            lastSplit = split;
        }
        bundleNames.removeAll(toRemove);
    }

    /**
     * Constructor
     */
    public BundleListPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        initBundleNames();

        final BundleListEntryRenderer renderer = new BundleListEntryRenderer();
        m_tree.setCellRenderer(renderer);
        m_tree.setRootVisible(false);
        m_tree.addTreeWillExpandListener(this);

        m_root.add(m_userBundlesRoot);
        m_root.add(m_customTypeRoot);
        m_tree.expandPath(new TreePath(m_root));

        final JScrollPane scroll = new JScrollPane(m_tree);
        scroll.setMinimumSize(new Dimension(300, 300));
        scroll.setPreferredSize(new Dimension(300, 300));
        add(scroll);

        /* Remove button */
        final JButton removeBundleButton = new JButton("Remove Selected Bundles");
        removeBundleButton.addActionListener(e -> removeSelectedBundles());

        final JPanel southPane = new JPanel(new FlowLayout());
        southPane.add(removeBundleButton);
        add(southPane);

        /* Label for list of available bundles */
        final JPanel labelPane = new JPanel(new FlowLayout());
        labelPane.add(new JLabel("Add bundles from the current eclipse environment as dependencies."));
        add(labelPane);

        final JPanel availableBundlesPane = new JPanel(new BorderLayout());
        {
            final JPanel northPane = new JPanel(new GridLayout(2, 1));
            {
                /* Add bundles button */
                final JButton addBundlesButton = new JButton("Add Selected Bundles");
                addBundlesButton.addActionListener(e -> addSelectedBundles());
                northPane.add(addBundlesButton);

                m_filterField.requestFocusInWindow();
                northPane.add(m_filterField);
            }
            availableBundlesPane.add(northPane, BorderLayout.NORTH);

            m_bundleList = new JList<>(m_bundleModel);
            final JScrollPane scrollPane = new JScrollPane(m_bundleList);
            availableBundlesPane.add(scrollPane, BorderLayout.CENTER);

            m_tree.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(final KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                        removeSelectedBundles();
                    }
                }
            });
            m_tree.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                        removeSelectedBundles();
                    }
                }
            });
            m_bundleList.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                        final String bundleName = m_bundleList.getSelectedValue();
                        addBundle(bundleName);
                        updateFilterModel();
                        m_bundleList.clearSelection();
                    }
                }
            });
            m_bundleList.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(final KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        addSelectedBundles();
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

        add(availableBundlesPane);
    }

    private void addSelectedBundles() {
        addBundles(m_bundleList.getSelectedValuesList());
        updateFilterModel();
        m_bundleList.clearSelection();
    }

    /* Remove all bundles currently selected in list */
    void removeSelectedBundles() {
        final TreePath[] paths = m_tree.getSelectionPaths();
        if (paths == null) {
            /* No selection */
            return;
        }

        for (final TreePath p : paths) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode)p.getLastPathComponent();
            if (node.getParent() != m_userBundlesRoot) {
                /* User can only edit user bundles */
                continue;
            }
            ((DefaultTreeModel)m_tree.getModel()).removeNodeFromParent(node);

            final Object o = node.getUserObject();
            m_listModel.remove(o);
        }

        updateFilterModel();
    }

    /**
     * @return All added bundles.
     */
    public String[] getBundles() {
        return m_listModel.getAllElements().stream().map(BundleListEntry::toString).toArray(n -> new String[n]);
    }

    /**
     * @param bundles
     */
    public void setBundles(final String[] bundles) {
        removeAllChildren(m_tree, m_userBundlesRoot);
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
        if (e == null || m_listModel.contains(e)) {
            return false;
        }

        m_listModel.add(e);

        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(e);

        addDependenciesForNode(node, Platform.getBundle(e.name));
        ((DefaultTreeModel)m_tree.getModel()).insertNodeInto(node, m_userBundlesRoot,
            m_userBundlesRoot.getChildCount());

        ensureRootsExpanded();
        updateFilterModel();

        return true;
    }

    /**
     * Update m_bundleModel to hide the elements that were already added to the available bundles.
     */
    private void updateFilterModel() {
        m_bundleModel.setExcluded(m_listModel.getAllElements().stream().map(BundleListEntry::toString)
            .filter(s -> !s.endsWith(")")).toArray(n -> new String[n]));
    }

    /**
     * Add given bundle names to list. Duplicates and null names are skipped.
     *
     * @param list The bundle names to add
     */
    public void addBundles(final List<String> list) {
        /* Set allows us to prevent adding bundles duplicate in list */
        final LinkedHashSet<BundleListEntry> entries = new LinkedHashSet<>(list.size());
        for (final String b : list) {
            if (b == null) {
                continue;
            }

            final BundleListEntry e = makeBundleListEntry(b);
            if (m_listModel.contains(e)) {
                continue;
            }

            if (entries.add(e)) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(e);
                ((DefaultTreeModel)m_tree.getModel()).insertNodeInto(node, m_userBundlesRoot,
                    m_userBundlesRoot.getChildCount());
                addDependenciesForNode(node, Platform.getBundle(e.name));
            }
        }

        m_listModel.addAll(entries);

        ensureRootsExpanded();
        updateFilterModel();
    }

    /**
     * Expanding a path in JTree does not have an effect when the path is a leaf. As the root starts out as a leaf, we
     * can only expand it once it has children, hence with this function we can ensure the roots both are expanded once
     * some children have been added.
     */
    private void ensureRootsExpanded() {
        m_tree.expandPath(new TreePath(m_userBundlesRoot.getPath()));
        m_tree.expandPath(new TreePath(m_customTypeRoot.getPath()));
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
                    installedVersion.getMajor() != v.getMajor() || installedVersion.getMinor() < v.getMinor();
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

    /* Add direct bundle dependencies of bundle b to node. */
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

    @Override
    public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
        /* Add children of children of the expanded node, so that they will be rendered with the
         * appropriate icon, correctly indicating whether they have children.
         */
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
        Enumeration<DefaultMutableTreeNode> children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = children.nextElement();
            if (child != null && child.getUserObject() instanceof Bundle) {
                addDependenciesForNode(child, (Bundle)child.getUserObject());
            }
        }
    }

    @Override
    public void treeWillCollapse(final TreeExpansionEvent event) throws ExpandVetoException {
        final Object node = event.getPath().getLastPathComponent();
        if (node == m_userBundlesRoot || node == m_customTypeRoot) {
            /* Prevent collapsing "Active Bundles" and "Custom Type Bundles" */
            throw new ExpandVetoException(event);
        }
    }

    /**
     * @brief Set bundles used for custom types.
     * @param bundles Bundles available in the snippet via custom input or output type converters.
     *
     * Bundles set with this function will be displayed in the "Custom Types Bundles" section.
     */
    public void setCustomTypeBundles(final Collection<Bundle> bundles) {
        removeAllChildren(m_tree, m_customTypeRoot);
        for (Bundle b : bundles) {
            ((DefaultTreeModel)m_tree.getModel()).insertNodeInto(new DefaultMutableTreeNode(b), m_customTypeRoot, 0);
        }

        ensureRootsExpanded();
    }

    /* Helper method that allows removing all children properly in a way that notifies a JTree and causes a redraw. */
    private static void removeAllChildren(final JTree tree, final DefaultMutableTreeNode node) {
        @SuppressWarnings("unchecked")
        Enumeration<DefaultMutableTreeNode> children = node.children();
        final ArrayList<DefaultMutableTreeNode> toRemove = new ArrayList<>();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = children.nextElement();
            toRemove.add(child);
        }

        for (DefaultMutableTreeNode child : toRemove) {
            ((DefaultTreeModel)tree.getModel()).removeNodeFromParent(child);
        }
    }

}
