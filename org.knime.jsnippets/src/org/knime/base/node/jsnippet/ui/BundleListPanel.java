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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Panel for adding Bundles to the Java Snippet node for compilation.
 *
 * @author Jonathan Hale
 * @since 3.5
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class BundleListPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    final DefaultListModel<String> m_listModel = new DefaultListModel<>();

    final JList<String> m_list = new JList<>(m_listModel);

    final static ArrayList<String> bundleNames = new ArrayList<>();

    private static void initBundleNames() {
        if (!bundleNames.isEmpty()) {
            return;
        }

        for (final IBundleGroupProvider provider : Platform.getBundleGroupProviders()) {
            for (final IBundleGroup feature : provider.getBundleGroups()) {
                for (final Bundle bundle : feature.getBundles()) {
                    bundleNames.add(bundle.getSymbolicName() + " " + bundle.getVersion().toString());
                }
            }
        }

        Collections.sort(bundleNames);
    }

    private static final class BundleListModel extends AbstractListModel<String> {

        private static final long serialVersionUID = 1L;

        List<String> m_filtered = null;

        String m_filter = "";

        @Override
        public int getSize() {
            return m_filter.isEmpty() ? bundleNames.size() : m_filtered.size();
        }

        @Override
        public String getElementAt(final int index) {
            return m_filter.isEmpty() ? bundleNames.get(index) : m_filtered.get(index);
        }

        synchronized void setFilter(final String filter) {
            m_filter = filter;
            m_filtered = bundleNames.stream().filter(s -> s.contains(filter)).collect(Collectors.toList());
            this.fireContentsChanged(this, 0, bundleNames.size());
        }
    }

    /**
     * Constructor
     */
    public BundleListPanel() {
        super(new BorderLayout());

        initBundleNames();

        add(new JScrollPane(m_list), BorderLayout.CENTER);

        final JPanel northPane = new JPanel(new FlowLayout());
        northPane.add(new JLabel("Add bundles from the current eclipse environment as dependencies."));
        add(northPane, BorderLayout.NORTH);

        final JButton addBundleButton = new JButton("Add Bundle");
        addBundleButton.addActionListener(e -> addBundle(m_listModel));

        final JButton removeBundleButton = new JButton("Remove");
        removeBundleButton.addActionListener(e -> removeBundle());

        final JPanel southPane = new JPanel(new FlowLayout());
        southPane.add(addBundleButton);
        southPane.add(removeBundleButton);
        add(southPane, BorderLayout.SOUTH);
    }

    private void addBundle(final DefaultListModel<String> listToAddTo) {
        final int width = 280;
        final int height = 360;

        final JFrame frame = new JFrame("Add Bundle");

        final Point p = getLocationOnScreen();
        final Dimension d = getSize();
        frame.setLocation(p.x + (d.width - width) / 2, p.y + (d.height - height) / 2);

        final JPanel pane = new JPanel(new BorderLayout());

        final JPanel northPane = new JPanel(new GridLayout(2, 1));
        northPane.add(new JLabel("Double-click bundle to add it together with its dependencies."));
        final JTextField filterField = new JTextField();
        northPane.add(filterField);
        pane.add(northPane, BorderLayout.NORTH);

        final BundleListModel bundleModel = new BundleListModel();
        final JList<String> bundleList = new JList<>(bundleModel);

        final JScrollPane scrollPane = new JScrollPane(bundleList);
        scrollPane.setPreferredSize(new Dimension(width, height));
        pane.add(scrollPane, BorderLayout.CENTER);

        bundleList.addMouseListener(new MouseListener() {

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
                    final String bundleName = bundleList.getSelectedValue();
                    final Bundle firstBundle = Platform.getBundle(bundleName.split(" ")[0]);

                    final Set<String> bundleNameSet = new HashSet<String>();

                    final LinkedBlockingQueue<Bundle> pending = new LinkedBlockingQueue<Bundle>();
                    pending.add(firstBundle);

                    Bundle bundle = null;
                    while ((bundle = pending.poll()) != null) {
                        if (!bundleNameSet.add(bundle.getSymbolicName() + " " + bundle.getVersion())) {
                            continue;
                        }

                        final BundleWiring wiring = bundle.adapt(BundleWiring.class);
                        final List<BundleWire> requiredWires =
                            wiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE);

                        for (final BundleWire w : requiredWires) {
                            pending.add(w.getProviderWiring().getBundle());
                        }
                    }

                    for (final String bn : bundleNameSet) {
                        listToAddTo.addElement(bn);
                    }

                    frame.setVisible(false);
                    frame.dispose();
                }
            }
        });

        filterField.getDocument().addDocumentListener(new DocumentListener() {

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
                    final Document d = e.getDocument();
                    bundleModel.setFilter(d.getText(0, d.getLength()));
                } catch (BadLocationException e1) {
                    // Will never happen
                }
            }
        });

        frame.add(pane);
        frame.pack();
        frame.setVisible(true);
    }

    private void removeBundle() {
        final int sel = m_list.getSelectedIndex();
        if (sel != -1) {
            m_listModel.remove(sel);
        }
    }

    /**
     * @return All added bundles.
     */
    public String[] getBundles() {
        final String[] bundles = new String[m_listModel.getSize()];

        for (int i = 0; i < m_listModel.getSize(); ++i) {
            bundles[i] = m_listModel.get(i);
        }

        return bundles;
    }

    /**
     * @param bundles
     */
    public void setBundles(final String[] bundles) {
        m_listModel.clear();

        for (final String b : bundles) {
            m_listModel.addElement(b);
        }
    }

}
