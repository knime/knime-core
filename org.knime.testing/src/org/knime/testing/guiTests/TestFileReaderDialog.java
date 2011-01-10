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
 *   31.08.2006 (ritmeier): created
 */
package org.knime.testing.guiTests;

import java.awt.AWTException;
import java.io.File;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;

import junit.extensions.jfcunit.JFCTestHelper;
import junit.extensions.jfcunit.RobotTestHelper;
import junit.extensions.jfcunit.TestHelper;
import junit.extensions.jfcunit.finder.ComponentFinder;
import junit.extensions.jfcunit.finder.JLabelFinder;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.tableview.TableView;

/**
 * 
 * @author ritmeier, University of Konstanz
 */
public class TestFileReaderDialog extends GUITestCase {

    public FileReaderNodeFactory m_fileReaderFactory;

    public final String TEST_FILE = "testData/data.all";

    public File m_testFilePath;

    /**
     * 
     */
    public TestFileReaderDialog() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public TestFileReaderDialog(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() {
        if (false) {
            setHelper(new JFCTestHelper());
        } else {
            try {
                setHelper(new RobotTestHelper());
            } catch (AWTException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        // create and show the FilesReaderDialog
        m_fileReaderFactory = new FileReaderNodeFactory();
        NodeDialogPane dialog = m_fileReaderFactory.createNodeDialogPane();
        JFrame f = new JFrame();
        f.setContentPane(dialog.getPanel());
        f.pack();
        f.setVisible(true);

        // determin the location of the testfile
        try {
            m_testFilePath = new File(TestFileReaderDialog.class.getResource(
                    TEST_FILE).toURI());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tearDown() {
        TestHelper.cleanUp(this);

         try {
         Thread.sleep(2000);
         } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         }

    }
    


    /**
     * Tests the filereader dialog
     */
    public void testFileReaderDialog() {
        
        Thread t = new Thread() {
            @Override
            public void run() {
                JLabelFinder lablefinder = new JLabelFinder("");
                JLabel lable = (JLabel)lablefinder.find();
                System.out.println("got it" + lable.getText());
                assertEquals("valid URL:", lable.getText());
            }
        };
        t.start();

        // find the file name combobox
        JComboBox fileNameCombobox = null;
        try {
            fileNameCombobox = findComboBox(0);
        } catch (ClassCastException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // insert the filename into the combobox
        sendString(fileNameCombobox, m_testFilePath.getAbsolutePath());
        sendEnterKey(fileNameCombobox);

        // check if the previewTable is displayed and has data
        TableView dataTableView = findTableView();
        assertTrue("PreviewTable is empty", dataTableView.hasData());

        JButton browseButton = findButton("browse...");

        JCheckBox readRowHeaders = findCheckbox("read row headers");
        click(readRowHeaders);
        
        JCheckBox readColHeaders = findCheckbox("read column headers");
        click(readColHeaders);
        
        JCheckBox ignorespaces = findCheckbox("ignore spaces and tabs");
        click(ignorespaces);
        
        JCheckBox javastylecomments = findCheckbox("Java-style comments");
        click(javastylecomments);
        
        click(browseButton);

        ComponentFinder openFileDialogFinder = new ComponentFinder(
                JFileChooser.class);
        JFileChooser fileChooser = (JFileChooser)openFileDialogFinder.find();
        assertEquals(m_testFilePath.getParentFile(), fileChooser
                .getCurrentDirectory());

        JButton okButton = findButton("Open");
        click(okButton);

    }

}
