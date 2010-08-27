/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   Aug 29, 2007 (wiswedel): created
 */
package org.knime.core.node;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.knime.core.util.FileReaderFileFilter;

/**
 * This class contains all available to-image-export options for node views. 
 * By default, only the PNG export option is available but customized exporters
 * can be added by calling the static function 
 * {@link #addExportType(org.knime.core.node.NodeViewExport.ExportType)}.
 * 
 * <p>This class is used in a static way, it is not meant to be instantiated.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class NodeViewExport {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(NodeViewExport.class);
    private static Map<String, ExportType> exportMap;
    
    private static File lastExportFile;
    
    private NodeViewExport() { }
    
    /** Used to initialized the export type map in a lazy way. */
    private static void createExportMapLazy() {
        if (exportMap == null) {
            exportMap = new LinkedHashMap<String, ExportType>();
            addExportType(new PNGExportType());
        }
    }
    
    /** Adds a singleton export type to the list of available exporters. If you
     * want to add your own image export type, you are advised to call this 
     * method in the start method of your knime node plugin. Make sure to call
     * it only once for each of the export types.
     * <p>This method refuses to add an export type twice (according to 
     * arguments <code>equals()</code> method).
     * @param newType The export type to add, all necessary information are
     * retrieved from the argument, the identifier name is uniquified, if 
     * necessary.
     * @throws NullPointerException If the argument is null.
     */
    public static void addExportType(final ExportType newType) {
        createExportMapLazy();
        String description = newType.getDescription();
        if (exportMap.containsValue(newType)) {
            LOGGER.warn("Refusing to add view export " + description 
                    + " twice (class " + newType.getClass() + ")");
            return;
        }
        if (exportMap.containsKey(description)) {
            int uniquifier = 1;
            do {
                description = newType.getDescription() + " #" + (uniquifier++);
            } while (exportMap.containsKey(description));
        }
        exportMap.put(description, newType);
    }
    
    /** Get a read only map containing pairs of export type identifier (as 
     * string) and the export type. This map can be used in a derived node view
     * (or any other view) to create a customized menu. You can also use the
     * {@link #createNewMenu(Container)} method for your convenience.
     * @return Such a read only map. It contains at least one entry (the 
     * png export).
     */
    public static Map<String, ExportType> getViewExportMap() {
        createExportMapLazy();
        return Collections.unmodifiableMap(exportMap);
    }
    
    /**
     * Convenience method that create a menu entry containing all available
     * export options. If the current export map (according to 
     * {@link #getViewExportMap()}) contains only one entry, the returned
     * menu is a single menu item. Otherwise it's a {@link JMenu} with the
     * export options as its children.
     * @param container The container to export when a item is selected.
     * @return Such a new(!) menu item.
     */
    public static JMenuItem createNewMenu(final Container container) {
        Map<String, ExportType> viewExportMap = getViewExportMap();
        JMenuItem[] list = new JMenuItem[viewExportMap.size()];
        int i = 0;
        for (Map.Entry<String, ExportType> e : viewExportMap.entrySet()) {
            final String name = e.getKey();
            final ExportType exType = e.getValue();
            JMenuItem item = new JMenuItem(name);
            item.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent action) {
                    onFileExport(exType, container);
                }
            });
            list[i] = item;
            i++;
        }
        if (list.length == 1) {
            list[0].setText("Export as PNG");
            list[0].setMnemonic('E');
            return list[0];
        } else {
            JMenu parent = new JMenu("Export as");
            parent.setMnemonic('E');
            for (JMenuItem item : list) {
                parent.add(item);
            }
            return parent;
        }
    }
    
    /** Method called by the actions of the menu items. */
    private static void onFileExport(final ExportType type, 
            final Container cont) {
        int width = cont.getWidth();
        int height = cont.getHeight();
        if (width <= 0 || height <= 0) {
            String msg = "View is too small to be exported.";
            JOptionPane.showConfirmDialog(cont, msg, "Warning", 
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);

            LOGGER.warn(msg);
            return;
        }
        JFileChooser chooser = new JFileChooser(lastExportFile);
        chooser.setFileFilter(new FileReaderFileFilter(type.getFileSuffix(),
                type.getDescription()));

        int returnVal = chooser.showSaveDialog(cont);
        File selectedFile;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            String suf = type.getFileSuffix();
            if (!suf.startsWith(".")) {
                suf = "." + suf;
            }
            if (!selectedFile.getName().endsWith(suf)) {
                selectedFile = new File(
                        selectedFile.getAbsolutePath().concat(suf));
            }
        } else {
            // do not save anything
            return;
        }
        try {
            type.export(selectedFile, cont, width, height);
        } catch (IOException e) {
            LOGGER.warn("View could not be exported due to io problems: ", e);
            JOptionPane.showConfirmDialog(cont,
                    "View could not be exported.", "Warning",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
            return;
        } catch (Exception e) {
            LOGGER.error("View could not be exported due to an exception: ", e);
            JOptionPane.showConfirmDialog(cont,
                    "View could not be exported.", "Warning",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
            return;
        }
        lastExportFile = selectedFile;
        JOptionPane.showConfirmDialog(cont, "View successfully exported.",
                "Info", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

    }
    
    /** Interface for export types. */
    public interface ExportType {
        /** A description for export type, this should be a one-liner such as
         * &quot;PNG - Portable Network Graphics&quot; or 
         * &quot;SVG - Scalable Vector Graphics&quot;. It's displayed as 
         * tooltip for the default menus and also in a file chooser dialog.
         * @return The description of the export type, not <code>null</code>.
         */
        public String getDescription();
        
        /** Suffix of the file being written. The suffix should not contain
         * a leading dot '.'. Use only, for instance &quot;png&quot; or
         * &quot;svg&quot; 
         * @return The file suffix, not <code>null</code>.
         */
        public String getFileSuffix();
        
        /** Called when the component is to be exported.
         * @param destination The destination file.
         * @param cont The component to draw.
         * @param width The width of the component.
         * @param height The height of the component.
         * @throws IOException If this fails for any reason.
         */
        public void export(File destination, final Component cont, 
                final int width, final int height) throws IOException;
    }
    
    /** Default implementation of an export type, 
     * which exports to a png file. */ 
    private static final class PNGExportType implements ExportType {

        /** {@inheritDoc} */
        public void export(final File destination, final Component cont, 
                final int width, final int height) 
            throws IOException {
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            // create graphics object to paint in
            Graphics2D graphics = image.createGraphics();
            cont.paint(graphics);
            destination.createNewFile();
            ImageIO.write(image, "png", destination);

        }
        /** {@inheritDoc} */
        public String getDescription() {
            return "PNG - Portable Network Graphics";
        }

        /** {@inheritDoc} */
        public String getFileSuffix() {
            return "png";
        }
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "PNG Exporter";
        }
        
        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
        
        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            return getClass().equals(obj.getClass());
        }
        
    }

}
