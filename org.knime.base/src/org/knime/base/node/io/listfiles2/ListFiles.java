/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.listfiles2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ListFiles {
    /** (Static) output spec. */
    public static final DataTableSpec SPEC =
        new DataTableSpec(createDataColumnSpec());

    /** Filter criterion. */
    public enum Filter {
        /** No filter. */
        None,
        /** Filter on file extension. */
        Extensions,
        /** Regular expression filter. */
        RegExp,
        /** Plain wildcard filter. */
        Wildcards
    }

    /** RowId counter. */
    private long m_currentRowID;

    /** Output container. */
    private BufferedDataContainer m_dc;

    /** extensions in case of extension filter. */
    private String[] m_extensions;

    /** regular expression in case of an wildcard or
     * regular expression. */
    private Pattern m_regExpPattern;

    /** The search settings. */
    private final ListFilesSettings m_settings;

    /** Init object according to argument settings.
     * @param settings The node settings. */
    ListFiles(final ListFilesSettings settings) {
        m_settings = settings;
    }

    /** Searches file system according to settings.
     * @param exec Progress/cancelation
     * @return The table containing the search hits.
     * @throws Exception If anything goes wrong.
     */
    public BufferedDataTable search(
            final ExecutionContext exec) throws Exception {
        Collection<URL> roots = m_settings.getRootsFromLocationString();
        m_dc = exec.createDataContainer(SPEC);
        String extString = m_settings.getExtensionsString();
        Filter filter = m_settings.getFilter();
        switch (filter) {
        case None:
            break;
        case Extensions:
            // extensions had to be splitted
            m_extensions = extString.split(";");
            break;
        case RegExp:
            // no break;
        case Wildcards:
            String patternS;
            if (filter.equals(Filter.Wildcards)) {
                patternS = WildcardMatcher.wildcardToRegex(extString);
            } else {
                patternS = extString;
            }
            if (m_settings.isCaseSensitive()) {
                m_regExpPattern = Pattern.compile(patternS);
            } else {
                m_regExpPattern =
                    Pattern.compile(patternS, Pattern.CASE_INSENSITIVE);
            }
            break;
        default:
            throw new IllegalStateException("Unknown filter: " + filter);
            // transform wildcard to regExp.
        }
        m_currentRowID = 0;
        for (URL u : roots) {
            exec.setProgress("Scanning " + ("file".equalsIgnoreCase(u.getProtocol()) ? u.getPath() : u));
            addLocation(u, exec, true);
        }

        m_dc.close();
        return m_dc.getTable();
    }

    /**
     * Recursive method to add all Files of a given folder to the output table.
     *
     * @param loc folder to be analyzed
     * @throws CanceledExecutionException if user canceld.
     * @throws URISyntaxException
     * @throws IOException
     */
    private void addLocation(final URL root, final ExecutionContext exec, final boolean isRootDir)
            throws CanceledExecutionException, IOException, URISyntaxException {
        exec.checkCanceled();

        for (URL u : FileUtil.listFiles(root, u -> satisfiesFilter(u), m_settings.isRecursive())) {
            addLocationToContainer(u);
        }
    }

    /**
     * Checks if the given File name satisfies the selected filter requirements.
     *
     * @param name filename
     * @return True if satisfies the file else False
     */
    private boolean satisfiesFilter(final URL url) {
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            decodedPath = url.getPath();
        }
        switch (m_settings.getFilter()) {
        case None:
            return true;
        case Extensions:
            if (m_settings.isCaseSensitive()) {
                // check if one of the extensions matches
                for (String ext : m_extensions) {
                    if (decodedPath.endsWith(ext)) {
                        return true;
                    }
                }
            } else {
                // case insensitive check on toLowerCase
                String lowname = url.getPath().toLowerCase();
                for (String ext : m_extensions) {
                    if (lowname.endsWith(ext.toLowerCase())) {
                        return true;
                    }
                }
            }
            return false;
        case RegExp:
            // no break;
        case Wildcards:
            Matcher matcher = m_regExpPattern.matcher(FilenameUtils.getName(decodedPath));
            return matcher.matches();
        default:
            return false;
        }
    }

    /**
     * Adds a File to the table.
     *
     * @param file
     */
    private void addLocationToContainer(final URL url) throws UnsupportedEncodingException, URISyntaxException {
        DataCell[] row = new DataCell[2];
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            row[0] = new StringCell(Paths.get(url.toURI()).toString());
        } else {
            row[0] = new MissingCell("URL is remote and does not have a local location");
        }
        row[1] = new StringCell(url.toString());

        m_dc.addRowToTable(new DefaultRow(RowKey.createRowKey(m_currentRowID), row));
        m_currentRowID++;
    }

    private static DataColumnSpec[] createDataColumnSpec() {
        DataColumnSpec[] dcs = new DataColumnSpec[2];
        dcs[0] =
            new DataColumnSpecCreator("Location", StringCell.TYPE).createSpec();
        dcs[1] = new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec();
        return dcs;
    }


}
