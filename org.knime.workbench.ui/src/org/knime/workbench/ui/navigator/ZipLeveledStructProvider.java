package org.knime.workbench.ui.navigator;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;

public class ZipLeveledStructProvider extends ZipLeveledStructureProvider {

    /**
     * @param sourceFile
     */
    public ZipLeveledStructProvider(final ZipFile sourceFile) {
        super(sourceFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabel(final Object element) {
        IPath elePath = new Path(((ZipEntry)element).getName());
        // strip off one level
        elePath = elePath.removeFirstSegments(getStrip());
        String result = elePath.lastSegment();
        if (result == null) {
            return "/";
        } else {
            return result;
        }
    }

}
