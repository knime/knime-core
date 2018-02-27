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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   16.10.2014 (thor): created
 */
package org.knime.core.util;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.junit.Test;

/**
 * Testcases for {@link FileUtil}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class FileUtilTest {
    /**
     * Testcase for {@link FileUtil#toURL(String)} under Linux and MacOS.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testToUrlUnix() throws Exception {
        assumeThat(Platform.getOS(), anyOf(is(Platform.OS_LINUX), is(Platform.OS_MACOSX)));
        URL url = FileUtil.toURL("/etc/passwd");
        assertThat("Unexpected URL", url.toString(), is("file:/etc/passwd"));

        url = FileUtil.toURL("/tmp/with space");
        assertThat("Unexpected URL", url.toString(), is("file:/tmp/with%20space"));

        url = FileUtil.toURL("/tmp/with#hash");
        assertThat("Unexpected URL", url.toString(), is("file:/tmp/with%23hash"));

        url = FileUtil.toURL("/tmp/with%percent");
        assertThat("Unexpected URL", url.toString(), is("file:/tmp/with%25percent"));

        url = FileUtil.toURL("/tmp/with+plus");
        assertThat("Unexpected URL", url.toString(), is("file:/tmp/with+plus"));

        url = FileUtil.toURL("file:/tmp/with space");
        assertThat("Unexpected URL", url.toString(), is("file:/tmp/with%20space"));

        url = FileUtil.toURL("file:/tmp/with%20space");
        assertThat("Unexpected URL", url.toString(), is("file:/tmp/with%20space"));
    }

    /**
     * Testcase for {@link FileUtil#toURL(String)} under Windows.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testToUrlWindows() throws Exception {
        assumeThat(Platform.getOS(), is(Platform.OS_WIN32));
        URL url = FileUtil.toURL("C:/Windows/test.txt");
        assertThat("Unexpected URL", url.toString(), is("file:/C:/Windows/test.txt"));

        url = FileUtil.toURL("C:\\Windows\\test.txt");
        assertThat("Unexpected URL", url.toString(), is("file:/C:/Windows/test.txt"));

        url = FileUtil.toURL("C:/Windows/with#hash.txt");
        assertThat("Unexpected URL", url.toString(), is("file:/C:/Windows/with%23hash.txt"));

        url = FileUtil.toURL("C:/Windows/with%percent.txt");
        assertThat("Unexpected URL", url.toString(), is("file:/C:/Windows/with%25percent.txt"));

        url = FileUtil.toURL("C:/Windows/with+plus.txt");
        assertThat("Unexpected URL", url.toString(), is("file:/C:/Windows/with+plus.txt"));

        url = FileUtil.toURL("file:/C:/Windows/test.txt");
        assertThat("Unexpected URL", url.toString(), is("file:/C:/Windows/test.txt"));

        url = FileUtil.toURL("file:/C:/Windows/with space.txt");
        assertThat("Unexpected URL", url.toString(), is("file:/C:/Windows/with%20space.txt"));
    }

    /**
     * Testcase for {@link FileUtil#resolveToPath(URL)} under Linux and MacOS.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveToPathUnix() throws Exception {
        assumeThat(Platform.getOS(), anyOf(is(Platform.OS_LINUX), is(Platform.OS_MACOSX)));
        URL url = new URL("file:///etc/passwd");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("/etc/passwd"));

        url = new URL("file:///tmp/with%20space");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("/tmp/with space"));

        url = new URL("file:///tmp/with space");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("/tmp/with space"));

        url = new URL("file:///tmp/with+plus");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("/tmp/with+plus"));

        url = new URL("file:///tmp/with%23hash");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("/tmp/with#hash"));

        url = new URL("file:///tmp/with%25percent");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("/tmp/with%percent"));

        url = new URL("http://www.knime.com");
        assertThat("Unexpected path", FileUtil.resolveToPath(url), is(nullValue()));
    }

    /**
     * Testcase for {@link FileUtil#resolveToPath(URL)} under Windows.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveToPathWindows() throws Exception {
        assumeThat(Platform.getOS(), is(Platform.OS_WIN32));
        URL url = new URL("file://server/path/on%20server"); // UNC path
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("\\\\server\\path\\on server"));

        url = new URL("file:/C:/tmp/with%20space");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("C:\\tmp\\with space"));

        url = new URL("file:/C:/tmp/with space");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("C:\\tmp\\with space"));

        url = new URL("file:/C:/tmp/with+plus");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("C:\\tmp\\with+plus"));

        url = new URL("file:/C:/tmp/with%23hash");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("C:\\tmp\\with#hash"));

        url = new URL("file:/C:/tmp/with%25percent");
        assertThat("Unexpected path", FileUtil.resolveToPath(url).toString(), is("C:\\tmp\\with%percent"));

        url = new URL("http://www.knime.com");
        assertThat("Unexpected path", FileUtil.resolveToPath(url), is(nullValue()));
    }

    /**
     * Test for {@link FileUtil#looksLikeUNC(Path)} and {@link FileUtil#looksLikeUNC(URL)}.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testLooksLikeUNC() throws Exception {
        URL url = new URL("file://server/path/on%20server");
        assertThat("UNC URL '" + url + "' not recognized", FileUtil.looksLikeUNC(url),
            is(Platform.OS_WIN32.equals(Platform.getOS())));

        url = new URL("file:/server/path/on%20server");
        assertThat("Non-UNC URL '" + url + "' not recognized", FileUtil.looksLikeUNC(url), is(false));

        url = new URL("http://server/path/on%20server");
        assertThat("Non-UNC URL '" + url + "' not recognized", FileUtil.looksLikeUNC(url), is(false));

        Path path = Paths.get("\\\\server\\path");
        assertThat("UNC path '" + path + "' not recognized", FileUtil.looksLikeUNC(path),
            is(Platform.OS_WIN32.equals(Platform.getOS())));

        path = Paths.get("\\local\\path");
        assertThat("Non-UNC path '" + path + "' not recognized", FileUtil.looksLikeUNC(path), is(false));
    }

    /**
     * Test that assures that both the protocol \\ and HOSTNAME of a UNC path is included in the absolute path
     * of a file after calling {@link FileUtil#getFileFromURL(URL)}.
     *
     * UNC paths have the syntax \\<computer name>\<shared directory>\ and are used to access folders
     * and files on a windows network of computers.
     *
     * Fixes AP-8896 "Zip File node shows success after failing to write to remote location".
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testFileResolvedFromUncUrlContainsTheProtocolHostAndPathInItsAbsolutePath() throws Exception {
        assumeThat(Platform.getOS(), is(Platform.OS_WIN32)); // UNC paths only work under Windows
        File resolvedFile = FileUtil.getFileFromURL(new URL("file://HOST/path"));
        assertThat("Resolved file does not have a correct UNC path", resolvedFile.getAbsolutePath(), is("\\\\HOST\\path"));
    }
}
