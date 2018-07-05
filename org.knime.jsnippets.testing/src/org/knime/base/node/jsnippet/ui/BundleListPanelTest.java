package org.knime.base.node.jsnippet.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.event.MouseEvent;
import java.util.Arrays;

import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.testing.UiTest;
import org.osgi.framework.Bundle;

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
 *   17.10.2016 (Jonathan Hale): created
 */

/**
 * Test for {@link BundleListPanel}.
 *
 * @author Jonathan Hale
 */
public class BundleListPanelTest extends UiTest {

    private BundleListPanel panel = new BundleListPanel();

    /** Setup the panel */
    @Before
    public void before() {
        getTestFrame().add(panel);
        getTestFrame().setVisible(true);
    }

    /**
     * Test adding a bundle to the panel (manually and via dialog and simulated click).
     */
    @Test
    public void testBundleListPanel() {
        /* Test initialization */
        assertFalse(BundleListPanel.bundleNames.isEmpty());

        final String firstBundle = BundleListPanel.bundleNames.get(0);

        /* Test adding a bundle */
        assertFalse(panel.addBundle(null));
        assertEquals("Adding null bundle should not add anything", 0, panel.m_listModel.size());
        assertEquals("Tree view out of sync.", 0 + 2, panel.m_tree.getRowCount());

        assertTrue(panel.addBundle(firstBundle));
        assertEquals(1, panel.m_listModel.size());
        assertEquals("Tree view out of sync.", 1 + 2, panel.m_tree.getRowCount());
        final String bundleString = panel.m_listModel.getElementAt(0).serialize();
        final Bundle bundle = Platform.getBundle(panel.m_listModel.getElementAt(0).name);
        assertNotNull("Expected symbolic name of an existing bundle to have been added", bundle);

        assertTrue("Available bundles should not contain the added bundle",
            panel.m_bundleModel.getExcluded().contains(panel.m_listModel.getElementAt(0).toString()));

        assertFalse("Adding an already added bundle should not be permitted", panel.addBundle(bundleString));
        assertEquals("Adding a bundle a second time should not increase size of list", 1, panel.m_listModel.size());
        assertEquals("Tree view out of sync.", 1 + 2, panel.m_tree.getRowCount());

        assertTrue("Adding non-existent bundles should be allowed.", panel.addBundle("i.dont.exist 0.0.0"));

        /* Test clearing */
        panel.setBundles(new String[]{});
        assertEquals(0, panel.m_listModel.size());
        assertEquals("Tree view out of sync.", 0 + 2, panel.m_tree.getRowCount());
        assertFalse("Available bundles should contain removed bundles again",
            panel.m_bundleModel.getExcluded().contains(bundleString));

        /* Test initially setting bundles */
        panel.setBundles(new String[]{firstBundle});
        assertEquals(1, panel.m_listModel.size());
        assertEquals("Tree view out of sync.", 1 + 2, panel.m_tree.getRowCount());
        assertNotNull("Expected symbolic name of an existing bundle in the list",
            Platform.getBundle(panel.m_listModel.getElementAt(0).name));
        assertTrue("Available bundles should not contain added bundle",
            panel.m_bundleModel.getExcluded().contains(panel.m_listModel.getElementAt(0).toString()));

        /* Select first value */
        int indexToSelect = 0;
        final String addedBundleName = panel.m_listModel.getElementAt(0).name;
        for (; indexToSelect < panel.m_bundleList.getModel().getSize(); ++indexToSelect) {
            final String bundleName = panel.m_bundleList.getModel().getElementAt(indexToSelect).split(" ")[0];
            if (!addedBundleName.equals(bundleName)) {
                break;
            }
        }
        panel.m_bundleList.setSelectedIndex(indexToSelect);
        /* Simulate a double-click to close and add */
        panel.m_bundleList
            .dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 1, 0, 0, 0, 2, false, MouseEvent.BUTTON1));
        assertEquals("Double-Click should add a bundle", 2, panel.m_listModel.size());
        assertEquals("Tree view out of sync.", 2 + 2, panel.m_tree.getRowCount());

        /* Test getBundles() */
        final String[] bundleNames = panel.getBundles();
        // getBundles() is used to get the bundles for saving as settings. Hence their version should also be returned.
        assertArrayEquals("getBundles should return array of bundles with versions in list model",
            new String[]{panel.m_listModel.getElementAt(0).serialize(), panel.m_listModel.getElementAt(1).serialize()}, bundleNames);

        panel.setBundles(new String[]{bundleNames[0]});
        panel.addBundles(Arrays.asList(bundleNames[0], bundleNames[1], bundleNames[1], null));
        assertEquals("Duplicates and null should be skipped while adding bundles.", 2, panel.m_listModel.size());
        assertEquals("Tree view out of sync.", 2 + 2, panel.m_tree.getRowCount());
        assertTrue("Available bundles should not contain added bundles",
            panel.m_bundleModel.getExcluded().contains(panel.m_listModel.getElementAt(0).toString()));
        assertTrue("Available bundles should not contain added bundles",
            panel.m_bundleModel.getExcluded().contains(panel.m_listModel.getElementAt(1).toString()));

        /* Select everything and remove */
        panel.m_tree.setSelectionInterval(0, panel.m_tree.getRowCount());
        panel.removeSelectedBundles();
        assertEquals("Removing selected elements should remove them", 0, panel.m_listModel.size());
        assertFalse("Available bundles should contain removed bundles again",
            panel.m_bundleModel.getExcluded().contains(bundle.toString()));

        /* Test that removeSelectedBundles without selection doesn't error */
        panel.m_tree.clearSelection();
        panel.removeSelectedBundles();
    }
}
