/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc - Was ZipFileStructureProvider, performed changes from 
 *     IImportStructureProvider to ILeveledImportStructureProvider
 *******************************************************************************/
package org.knime.workbench.ui.wizards.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;

/**
 * This class provides information regarding the context structure and content
 * of specified zip file entry objects.
 * 
 * @since 3.1
 */
public class ZipLeveledStructureProvider implements
        ILeveledImportStructureProvider {
    private ZipFile zipFile;

    private ZipEntry root = new ZipEntry("/");//$NON-NLS-1$

    private Map children;

    private Map directoryEntryCache = new HashMap();

    private int stripLevel;

    /**
     * Creates a <code>ZipFileStructureProvider</code>, which will operate on
     * the passed zip file.
     * 
     * @param sourceFile
     *            The source file to create the ZipLeveledStructureProvider
     *            around
     */
    public ZipLeveledStructureProvider(ZipFile sourceFile) {
        super();
        zipFile = sourceFile;
        stripLevel = 0;
    }

    /**
     * Creates a new container zip entry with the specified name, iff it has 
     * not already been created. If the parent of the given element does not
     * already exist it will be recursively created as well.
     * @param pathname The path representing the container
     * @return The element represented by this pathname (it may have already existed)
     */
    protected ZipEntry createContainer(IPath pathname) {
        ZipEntry existingEntry = (ZipEntry) directoryEntryCache.get(pathname);
        if (existingEntry != null) {
            return existingEntry;
        }

        ZipEntry parent;
        if (pathname.segmentCount() == 1) {
            parent = root;
        } else {
            parent = createContainer(pathname.removeLastSegments(1));
        }
        ZipEntry newEntry = new ZipEntry(pathname.toString());
        directoryEntryCache.put(pathname, newEntry);
        List childList = new ArrayList();
        children.put(newEntry, childList);

        List parentChildList = (List) children.get(parent);
        parentChildList.add(newEntry);
        return newEntry;
    }

    /**
     * Creates a new file zip entry with the specified name.
     */
    protected void createFile(ZipEntry entry) {
        IPath pathname = new Path(entry.getName());
        ZipEntry parent;
        if (pathname.segmentCount() == 1) {
            parent = root;
        } else {
            parent = (ZipEntry) directoryEntryCache.get(pathname
                    .removeLastSegments(1));
        }

        List childList = (List) children.get(parent);
        childList.add(entry);
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    public List getChildren(Object element) {
        if (children == null) {
            initialize();
        }

        return ((List) children.get(element));
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    public InputStream getContents(Object element) {
        try {
            return zipFile.getInputStream((ZipEntry) element);
        } catch (IOException e) {
            IDEWorkbenchPlugin.log(e.getLocalizedMessage(), e);
            return null;
        }
    }

    /*
     * Strip the leading directories from the path
     */
    private String stripPath(String path) {
        String pathOrig = new String(path);
        for (int i = 0; i < stripLevel; i++) {
            int firstSep = path.indexOf('/');
            // If the first character was a seperator we must strip to the next
            // seperator as well
            if (firstSep == 0) {
                path = path.substring(1);
                firstSep = path.indexOf('/');
            }
            // No seperator wasw present so we're in a higher directory right
            // now
            if (firstSep == -1) {
                return pathOrig;
            }
            path = path.substring(firstSep);
        }
        return path;
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    public String getFullPath(Object element) {
        return stripPath(((ZipEntry) element).getName());
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    public String getLabel(Object element) {
        if (element.equals(root)) {
            return ((ZipEntry) element).getName();
        }

        return stripPath(new Path(((ZipEntry) element).getName()).lastSegment());
    }

    /**
     * Returns the entry that this importer uses as the root sentinel.
     * 
     * @return java.util.zip.ZipEntry
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Returns the zip file that this provider provides structure for.
     * 
     * @return The zip file
     */
    public ZipFile getZipFile() {
        return zipFile;
    }


    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider#closeArchive()
     */
    public boolean closeArchive(){
        try {
            getZipFile().close();
        } catch (IOException e) {
            IDEWorkbenchPlugin.log(DataTransferMessages.ZipImport_couldNotClose
                    + getZipFile().getName(), e);
            return false;
        }
        return true;
    }
    
    /**
     * Initializes this object's children table based on the contents of the
     * specified source file.
     */
    protected void initialize() {
        children = new HashMap(1000);

        children.put(root, new ArrayList());
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            IPath path = new Path(entry.getName()).addTrailingSeparator();

            if (entry.isDirectory()) {
                createContainer(path);
            } else
            {
                // Ensure the container structure for all levels above this is initialized
                // Once we hit a higher-level container that's already added we need go no further
                int pathSegmentCount = path.segmentCount();
                if (pathSegmentCount > 1) {
                    createContainer(path.uptoSegment(pathSegmentCount - 1));
                }
                createFile(entry);
            }
        }
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    public boolean isFolder(Object element) {
        return ((ZipEntry) element).isDirectory();
    }

    public void setStrip(int level) {
        stripLevel = level;
    }

    public int getStrip() {
        return stripLevel;
    }
}
