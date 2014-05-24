/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   Sep 1, 2006 (ritmeier): created
 */
package org.knime.testing.guiTests;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import junit.extensions.jfcunit.JFCTestCase;
import junit.extensions.jfcunit.eventdata.KeyEventData;
import junit.extensions.jfcunit.eventdata.MouseEventData;
import junit.extensions.jfcunit.eventdata.StringEventData;
import junit.extensions.jfcunit.finder.ComponentFinder;

import org.knime.core.node.tableview.TableView;

/**
 *
 * @author ritmeier, University of Konstanz
 */
public class GUITestCase extends JFCTestCase {

    /**
     *
     */
    public GUITestCase() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public GUITestCase(final String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    public void sendEnterKey(final Component comp) {
        getHelper().sendKeyAction(
                new KeyEventData(this, comp, KeyEvent.VK_ENTER));
    }

    public void sendString(final Component comp, final String text) {
        getHelper().sendString(new StringEventData(this, comp, text));
    }

    public JComboBox findComboBox(final int pos) {
        ComponentFinder comboBoxFinder = new ComponentFinder(JComboBox.class);
        JComboBox comboBox = (JComboBox)comboBoxFinder.find(pos);
        assertNotNull("Could not find Combobox", comboBox);
        return comboBox;
    }

    public TableView findTableView() {
        ComponentFinder tableViewFinder = new ComponentFinder(TableView.class);
        TableView dataTableView = (TableView)tableViewFinder.find();
        assertNotNull("Could not find TableView", dataTableView);
        return dataTableView;
    }

    public void click(final Component readColHeaders) {
        getHelper()
                .enterClickAndLeave(new MouseEventData(this, readColHeaders));
    }

    public JCheckBox findCheckbox(String text) {
        if (text == null) {
            text = "";
        }
        ComponentFinder checkBoxFinder = new ComponentFinder(JCheckBox.class);
        List<JCheckBox> checkboxList = checkBoxFinder.findAll();
        for (JCheckBox currentCheckBox : checkboxList) {
            if (text.equalsIgnoreCase(currentCheckBox.getText())) {
                return currentCheckBox;
            }
        }
        fail("Could not find CheckBox with text: " + text);
        return null;
    }

    public JButton findButton(String text) {
        if (text == null) {
            text = "";
        }
        JButton button = null;
        ComponentFinder buttonFinder = new ComponentFinder(JButton.class);
        List<JButton> buttonList = buttonFinder.findAll();
        for (JButton currentButton : buttonList) {
            if (text.equalsIgnoreCase(currentButton.getText())) {
                button = currentButton;
            }
        }
        assertNotNull("Could not find button: " + text, button);
        return button;
    }

}
