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
