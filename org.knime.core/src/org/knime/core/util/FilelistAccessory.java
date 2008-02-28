/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   29.08.2006 (ohl): created
 */

package org.knime.core.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * A file list used to display the directory content in the file chooser (when
 * the chooser is set to DIRECTORIES_ONLY). It overcomes the drawback of the
 * JFileChooser, that doesn't show the content of the directory. Users find
 * it disturbing to select a directory they don't know the content of. This
 * accessory displays all files contained in the currently selected dir.
 * 
 * @author ohl, University of Konstanz
 */
public class FilelistAccessory extends JPanel 
        implements PropertyChangeListener {

    private final JList m_fileList;
 
    private final JFileChooser m_fc;
    
    /* a file filter accepting only files (no directories) */
    private FileFilter m_dirFilter = new FileFilter() {
        public boolean accept(final File f) {
            return !f.isDirectory();
        }
    };

    /**
     * The constructor. The new instance must be set as accessory with the
     * specified file chooser. It will register itself with the file chooser (to
     * get updated when user selection changes).
     * 
     * @param fc the file chooser this component will be set as accessory to.
     */
    public FilelistAccessory(final JFileChooser fc) {

        m_fc = fc;
        
        m_fileList = new JList();
        m_fileList.setCellRenderer(new FileListRenderer(m_fc));
        // as we can't disable selection, we set it to single selection
        m_fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_fileList.setSelectionModel(new NoSelectionListSelectionModel());

        Box labelBox = Box.createHorizontalBox();
        labelBox.add(Box.createHorizontalStrut(3));
        labelBox.add(new JLabel("Files in the selected directory:"));
        labelBox.add(Box.createHorizontalGlue());
        
        setLayout(new BorderLayout());
        add(labelBox, BorderLayout.NORTH);
        add(new JScrollPane(m_fileList), BorderLayout.CENTER);

        // register with the file chooser to get notified when selection changes
        m_fc.addPropertyChangeListener(this);

        // update the file list.
        this.propertyChange(new PropertyChangeEvent(this,
                JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, null, m_fc
                        .getCurrentDirectory()));
    }

    /**
     * {@inheritDoc}
     */
    public void propertyChange(final PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        // if the dir changed, or the selection changed
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)
                || JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {

            File dir = m_fc.getSelectedFile();
            if (dir == null) {
                dir = m_fc.getCurrentDirectory();
            }
            
            File[] fileList = null;
            if ((dir != null) && (dir.isDirectory())) {
                fileList = dir.listFiles(m_dirFilter);
            }
            if (fileList == null) {
                fileList = new File[0];
            }
            
            final File[] finalListForThread = fileList; 
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    m_fileList.setListData(finalListForThread);
                }
            });
            
        }
    }
    
    /**
     * Renderer that checks if the value being renderer is of type
     * <code>File</code> and if so it will render the name of the file
     * together with the file's icon, which it retrieves from the passed
     * FileChooser. If the value is not a file, the passed value's toString()
     * method is used for rendering.
     * 
     * @author ohl, University of Konstanz
     */
    public class FileListRenderer extends DefaultListCellRenderer {

        private final JFileChooser m_fchooser;

        /**
         * Creates a new instance of a renderer for a JList containing files.
         * 
         * @param fc the FileChooser (to retrieve the icons from). Could be
         *            null, in which case no icons will be displayed.
         */
        public FileListRenderer(final JFileChooser fc) {
            m_fchooser = fc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            // The super method will reset the icon if we call this method
            // last. So we let super do its job first and then we take care
            // that everything is properly set.
            Component c =
                    super.getListCellRendererComponent(list, value, index,
                            isSelected, cellHasFocus);
            assert (c == this);
            if (value instanceof File) {
                setText(((File)value).getName());
                if (m_fchooser != null) {
                    setIcon(m_fchooser.getIcon((File)value));
                }
            }
            return this;
        }
    }
}
