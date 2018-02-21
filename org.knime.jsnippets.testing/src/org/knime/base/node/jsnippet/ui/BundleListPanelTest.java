package org.knime.base.node.jsnippet.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.event.MouseEvent;

import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.node.jsnippet.ui.BundleListPanel.AddBundleDialog;
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
    public void testAddBundle() {
        /* Test initialization */
        assertThat(BundleListPanel.bundleNames, not(empty()));

        final String firstBundle = BundleListPanel.bundleNames.get(0);

        /* Test adding a bundle */
        assertFalse(panel.addBundle(null));
        assertEquals("Adding null bundle should not add anything", 0, panel.m_listModel.size());

        assertTrue(panel.addBundle(firstBundle));
        assertEquals(1, panel.m_listModel.size());
        final Bundle bundle = Platform.getBundle(panel.m_listModel.getElementAt(0));
        assertNotNull("Expected symbolic name of an existing bundle to have been added", bundle);

        assertFalse(panel.addBundle("i.dont.exist"));

        /* Test clearing */
        panel.setBundles(new String[]{});
        assertEquals(0, panel.m_listModel.size());

        /* Test initially setting bundles */
        panel.setBundles(new String[]{firstBundle});
        assertEquals(1, panel.m_listModel.size());
        assertNotNull("Expected symbolic name of an existing bundle in the list",
            Platform.getBundle(panel.m_listModel.getElementAt(0)));

        /* Test that opening the dialog does not throw any exceptions */
        final AddBundleDialog dialog = panel.openAddBundleDialog();

        /* Select first value */
        dialog.m_bundleList.setSelectedIndex(0);
        /* Simulate a double-click to close and add */
        dialog.m_bundleList
            .dispatchEvent(new MouseEvent(dialog, MouseEvent.MOUSE_CLICKED, 1, MouseEvent.BUTTON1, 4, 4, 2, false));

        assertFalse("Double-Click should close AddBundleDialog", dialog.isVisible());
        assertEquals("Double-Click should add a bundle", 2, panel.m_listModel.size());

        /* Test getBundles() */
        assertArrayEquals("getBundles should return array of bundles in list model",
            new String[]{panel.m_listModel.getElementAt(0), panel.m_listModel.getElementAt(1)}, panel.getBundles());

        /* Select multiple and remove */
        panel.m_list.setSelectedIndices(new int[]{0, 1});
        panel.removeSelectedBundles();
        assertEquals("Removing selected elements should remove them", 0, panel.m_listModel.size());

        /* Test that removeSelectetedBundles without selection doesn't error */
        panel.m_list.clearSelection();
        panel.removeSelectedBundles();
    }
}
