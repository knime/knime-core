/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
    public GUITestCase(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    public void sendEnterKey(Component comp) {
        getHelper().sendKeyAction(
                new KeyEventData(this, comp, KeyEvent.VK_ENTER));
    }

    public void sendString(Component comp, String text) {
        getHelper().sendString(new StringEventData(this, comp, text));
    }

    public JComboBox findComboBox(int pos) {
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

    public void click(Component readColHeaders) {
        getHelper()
                .enterClickAndLeave(new MouseEventData(this, readColHeaders));
    }

    public JCheckBox findCheckbox(String text) {
        if(text == null) {
            text = "";
        }
        ComponentFinder checkBoxFinder = new ComponentFinder(JCheckBox.class);
        List<JCheckBox> checkboxList = checkBoxFinder.findAll();
        for (JCheckBox currentCheckBox : checkboxList) {
            if(text.equalsIgnoreCase(currentCheckBox.getText())) {
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
