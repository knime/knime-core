/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 */
package org.knime.core.util;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Predicate;

/**
 * Service enabling access to files which potentially live on remote file systems. An example is a remote executor,
 * which not necessarily has access to all files on the KNIME server. However, using this service, the remote executor
 * can access the files as needed via KNIME Rest API.
 *
 * @author Christian Dietz, KNIME.com GmbH.
 * @since 3.4
 */
public interface IRemoteFileUtilsService {
    /**
     * Lists all direct children of the provided URL. The URL is assumed to be a directory on a remote server.
     *
     * @param root {@link URL} of a directory on the remote server
     * @param filter a filter for the URLs to be included in the result. The filter will only be called for files and
     *            not directories.
     * @param recursive <code>true</code> if files should be listed recursively, <code>false</code> if only direct
     *            children of the root folder should be returned
     *
     * @return direct children of parent as {@link List} of {@link URL}.
     * @throws IOException if an I/O error occurs while listing the children
     */
    List<URL> listRemoteFiles(URL root, Predicate<URL> filter, boolean recursive) throws IOException;

    /**
     * Check whether resource at given {@link URL} is a workflow group.
     *
     * @param url the {@link URL} of resource to check
     * @return <source>true</source>, if resource is a workflow group
     * @throws IOException if an I/O error occurs during communication
     *
     * @since 3.5
     */
    boolean isWorkflowGroup(URL url) throws IOException;

    /**
     * Deletes resource at given {@link URL}.
     *
     * @param url to delete.
     * @throws IOException if an I/O error occurs during communication
     *
     * @since 3.5
     */
    void delete(URL url) throws IOException;

    /**
     * Returns size of resource at {@link URL} in bytes.
     *
     * @param url to get size from
     * @return size of resource in bytes
     * @throws IOException if an I/O error occurs during communication
     *
     * @since 3.5
     */
    long getSize(URL url) throws IOException;

    /**
     * Creates directory in resource at {@link URL}
     *
     * @param url {@link URL} pointing on location where the directory is created
     * @throws IOException if an I/O error occurs during communication
     *
     * @since 3.5
     */
    void mkDir(URL url) throws IOException;

    /**
     * Get last modified date of {@link URL}.
     *
     * @param url {@link URL} pointing to resource in question
     * @return Time in UNIX-Time (seconds since 01.01.1970) or 0 if the file does not exist
     * @throws IOException if an I/O error occurs during communication
     *
     * @since 3.5
     */
    long lastModified(URL url) throws IOException;

    /**
     * Checks if a resource at a given {@link URL} exists.
     *
     * @param url {@link URL} pointing on location to check
     * @return <source>true</source> if resource exists
     * @throws IOException if an I/O error occurs during communication
     *
     * @since 3.5
     */
    boolean exists(URL url) throws IOException;
}
