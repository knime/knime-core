/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *     Red Hat, Inc - Extracted methods from WizardArchiveFileResourceImportPage1
 *******************************************************************************/

package org.knime.workbench.ui.wizards.imports;

import java.io.IOException;
import java.util.zip.ZipFile;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;

/**
 * @since 3.1
 */
public class ArchiveFileManipulations {

    private static ZipLeveledStructureProvider zipProviderCache;

    private static TarLeveledStructureProvider tarProviderCache;

    /**
     * Determine whether the file with the given filename is in .tar.gz or .tar
     * format.
     * 
     * @param fileName
     *            file to test
     * @return true if the file is in tar format
     */
    public static boolean isTarFile(String fileName) {
        if (fileName.length() == 0) {
            return false;
        }

        try {
            new TarFile(fileName);
        } catch (TarException tarException) {
            return false;
        } catch (IOException ioException) {
            return false;
        }

        return true;
    }

    /**
     * Determine whether the file with the given filename is in .zip or .jar
     * format.
     * 
     * @param fileName
     *            file to test
     * @return true if the file is in tar format
     */
    public static boolean isZipFile(String fileName) {
        if (fileName.length() == 0) {
            return false;
        }

        try {
            new ZipFile(fileName);
        } catch (IOException ioException) {
            return false;
        }

        return true;
    }

    /**
     * Clears the cached structure provider after first finalizing it properly.
     * 
     * @param shell
     *            The shell to display any possible Dialogs in
     */
    public static void clearProviderCache(Shell shell) {
        if (zipProviderCache != null) {
            closeZipFile(zipProviderCache.getZipFile(), shell);
            zipProviderCache = null;
        }
        tarProviderCache = null;
    }

    /**
     * @param targetZip
     * @param shell
     * @return the structure provider
     */
    public static ZipLeveledStructureProvider getZipStructureProvider(
            ZipFile targetZip, Shell shell) {
        if (zipProviderCache == null) {
            zipProviderCache = new ZipLeveledStructureProvider(targetZip);
        } else if (!zipProviderCache.getZipFile().getName().equals(
                targetZip.getName())) {
            clearProviderCache(shell);
            // ie.- new value, so finalize&remove old value
            zipProviderCache = new ZipLeveledStructureProvider(targetZip);
        } else if (!zipProviderCache.getZipFile().equals(targetZip)) {
            // duplicate handle to same zip
            // dispose old zip and use new one in case old one is closed
            zipProviderCache = new ZipLeveledStructureProvider(targetZip);
        }

        return zipProviderCache;
    }

    /**
     * Attempts to close the passed zip file, and answers a boolean indicating
     * success.
     * 
     * @param file
     *            The zip file to attempt to close
     * @param shell
     *            The shell to display error dialogs in
     * @return Returns true if the operation was successful
     */
    public static boolean closeZipFile(ZipFile file, Shell shell) {
        try {
            file.close();
        } catch (IOException e) {
            displayErrorDialog(DataTransferMessages.ZipImport_couldNotClose,
                    shell);
            return false;
        }

        return true;
    }

    /**
     * Returns a structure provider for the specified tar file.
     * 
     * @param targetTar
     *            The specified tar file
     * @param shell
     *            The shell to display dialogs in
     * @return the structure provider
     */
    public static TarLeveledStructureProvider getTarStructureProvider(
            TarFile targetTar, Shell shell) {
        if (tarProviderCache == null) {
            tarProviderCache = new TarLeveledStructureProvider(targetTar);
        } else if (!tarProviderCache.getTarFile().getName().equals(
                targetTar.getName())) {
            ArchiveFileManipulations.clearProviderCache(shell);
            // ie.- new value, so finalize&remove old value
            tarProviderCache = new TarLeveledStructureProvider(targetTar);
        }

        return tarProviderCache;
    }

    /**
     * Display an error dialog with the specified message.
     * 
     * @param message
     *            the error message
     */
    protected static void displayErrorDialog(String message, Shell shell) {
        MessageDialog.openError(shell, getErrorDialogTitle(), message);
    }

    /**
     * Get the title for an error dialog. Subclasses should override.
     */
    protected static String getErrorDialogTitle() {
        return IDEWorkbenchMessages.WizardExportPage_internalErrorTitle;
    }
}