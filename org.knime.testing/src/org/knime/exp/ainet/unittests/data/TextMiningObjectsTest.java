/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *    11.01.2007 (Tobias Koetter): created
 */

package org.knime.exp.ainet.unittests.data;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.exp.ainet.data.textmining.DocumentAuthorFactory;
import org.knime.exp.ainet.data.textmining.DocumentFactory;
import org.knime.exp.ainet.data.textmining.ProcessingInfoFactory;
import org.knime.exp.ainet.data.textmining.TermDocumentFrequenceSetFactory;
import org.knime.exp.ainet.data.textmining.TermFactory;
import org.knime.exp.textmining.data.Document;
import org.knime.exp.textmining.data.DocumentAuthor;
import org.knime.exp.textmining.data.DocumentCategory;
import org.knime.exp.textmining.data.DocumentSource;
import org.knime.exp.textmining.data.DocumentType;
import org.knime.exp.textmining.data.PartOfSpeechTag;
import org.knime.exp.textmining.data.ProcessingInfo;
import org.knime.exp.textmining.data.PublicationDate;
import org.knime.exp.textmining.data.Term;
import org.knime.exp.textmining.data.TermDocumentFrequencySet;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class TextMiningObjectsTest extends TestCase {
    
    private final Set<DocumentCategory> m_testCategories = 
        new HashSet<DocumentCategory>();

    private final Set<DocumentSource> m_testSources = 
        new HashSet<DocumentSource>();
    
    /**Constructor for class DocumentAuthorFactoryTest.
     * 
     */
    public TextMiningObjectsTest() {
        m_testCategories.add(new DocumentCategory("junitTestCategory"));
        m_testSources.add(new DocumentSource("junitTestSource"));
    }

    /**
     * Test method for Document creation
     * {@link org.knime.exp.ainet.data.textmining.DocumentFactory#create(
     * org.knime.exp.textmining.data.Document)}.
     */
    public final void testCreateDocument() {
        //test a document with a fulltext longer than 4000 character
        String file = "Fulltext > 4000 character TestFile";
        Set<DocumentAuthor> authors = 
            new HashSet<DocumentAuthor>(2);
        authors.add(
                DocumentAuthorFactory.create("junitLast1", "junitFirst1"));
        authors.add(
                DocumentAuthorFactory.create("junitLast2", "junitFirst2"));
        String fullText = "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über das Index Pull-Down-Menü kann ein bestimmtes Suchfeld fest eingestellt werden. Sie können direkt suchen oder sich mit dem Button Index aufblättern eine alphabetische Liste der Suchbegriffe anzeigen lassen. Dies ist dann sinnvoll, wenn Sie sich zum Beispiel bei der Schreibweise eines Namens oder Begriffes nicht sicher sind. Mit einem * hinter Ihrem Suchbegriff können Sie beliebige Wortendungen mitsuchen.\r\n" + 
                "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über das Index Pull-Down-Menü kann ein bestimmtes Suchfeld fest eingestellt werden. Sie können direkt suchen oder sich mit dem Button Index aufblättern eine alphabetische Liste der Suchbegriffe anzeigen lassen. Dies ist dann sinnvoll, wenn Sie sich zum Beispiel bei der Schreibweise eines Namens oder Begriffes nicht sicher sind. Mit einem * hinter Ihrem Suchbegriff können Sie beliebige Wortendungen mitsuchen.\r\n" + 
                "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über das Index Pull-Down-Menü kann ein bestimmtes Suchfeld fest eingestellt werden. Sie können direkt suchen oder sich mit dem Button Index aufblättern eine alphabetische Liste der Suchbegriffe anzeigen lassen. Dies ist dann sinnvoll, wenn Sie sich zum Beispiel bei der Schreibweise eines Namens oder Begriffes nicht sicher sind. Mit einem * hinter Ihrem Suchbegriff können Sie beliebige Wortendungen mitsuchen.\r\n" + 
                "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über das Index Pull-Down-Menü kann ein bestimmtes Suchfeld fest eingestellt werden. Sie können direkt suchen oder sich mit dem Button Index aufblättern eine alphabetische Liste der Suchbegriffe anzeigen lassen. Dies ist dann sinnvoll, wenn Sie sich zum Beispiel bei der Schreibweise eines Namens oder Begriffes nicht sicher sind. Mit einem * hinter Ihrem Suchbegriff können Sie beliebige Wortendungen mitsuchen.\r\n" + 
                "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über das Index Pull-Down-Menü kann ein bestimmtes Suchfeld fest eingestellt werden. Sie können direkt suchen oder sich mit dem Button Index aufblättern eine alphabetische Liste der Suchbegriffe anzeigen lassen. Dies ist dann sinnvoll, wenn Sie sich zum Beispiel bei der Schreibweise eines Namens oder Begriffes nicht sicher sind. Mit einem * hinter Ihrem Suchbegriff können Sie beliebige Wortendungen mitsuchen.\r\n" + 
                "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über das Index Pull-Down-Menü kann ein bestimmtes Suchfeld fest eingestellt werden. Sie können direkt suchen oder sich mit dem Button Index aufblättern eine alphabetische Liste der Suchbegriffe anzeigen lassen. Dies ist dann sinnvoll, wenn Sie sich zum Beispiel bei der Schreibweise eines Namens oder Begriffes nicht sicher sind. Mit einem * hinter Ihrem Suchbegriff können Sie beliebige Wortendungen mitsuchen.\r\n" + 
                "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über das Index Pull-Down-Menü kann ein bestimmtes Suchfeld fest eingestellt werden. Sie können direkt suchen oder sich mit dem Button Index aufblättern eine alphabetische Liste der Suchbegriffe anzeigen lassen. Dies ist dann sinnvoll, wenn Sie sich zum Beispiel bei der Schreibweise eines Namens oder Begriffes nicht sicher sind. Mit einem * hinter Ihrem Suchbegriff können Sie beliebige Wortendungen mitsuchen.\r\n" + 
                "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über das Index Pull-Down-Menü kann ein bestimmtes Suchfeld fest eingestellt werden. Sie können direkt suchen oder sich mit dem Button Index aufblättern eine alphabetische Liste der Suchbegriffe anzeigen lassen. Dies ist dann sinnvoll, wenn Sie sich zum Beispiel bei der Schreibweise eines Namens oder Begriffes nicht sicher sind. Mit einem * hinter Ihrem Suchbegriff können Sie beliebige Wortendungen mitsuchen.\r\n" + 
                "In der einfachen Suche können Sie in die Suchmaske ein oder mehrere beliebige Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über djetzt Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über djetzt Suchworte (z. Bsp. Nachname des Autors, Titelwort, Schlagwort) eingeben. Über djetztENDEENDEENDEENDE!!!";
        PublicationDate date = new PublicationDate();
        String title = "Junit document title";
        Document expectedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        Document retrievedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        assertEquals(expectedDoc, retrievedDoc);
        //test a document with no authors
        file = "noAuthorTestFile";
        authors.clear();
        fullText = 
            "In der einfachen Suche können Sie in die Suchmaske ein oder ";
        title = "Junit document title2";
        expectedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        retrievedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        assertEquals(expectedDoc, retrievedDoc);
        
        //test a document with none db objects
        file = "noDBObjectsTestFile";
        authors.clear();
        authors.add(
                new DocumentAuthor("junitLast1", "junitFirst1"));
        authors.add(
                new DocumentAuthor("junitLast3", "junitFirst3"));
        date = new PublicationDate(2007, 1);
         fullText = 
            "JUnit test with none db objects ";
        title = "Junit document title3 - None db objects";
        Document doc = new Document(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        expectedDoc = DocumentFactory.create(doc);
        retrievedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        assertEquals(expectedDoc, retrievedDoc);
    }

    /**
     * Test method for Document creation
     * {@link org.knime.exp.ainet.data.textmining.
     * TermDocumentFrequencySetFactory#create(
     * org.knime.exp.textmining.data.TermDocumentFrequencySet)}.
     */
    public final void testCreateTermDocumentFrequenceSet() {
        String file = "termDocumentFrequenceTestFile";
        Set<DocumentAuthor> authors = 
            new HashSet<DocumentAuthor>(2);
        authors.add(
                DocumentAuthorFactory.create("junitLast1", "junitFirst1"));
        authors.add(
                DocumentAuthorFactory.create("junitLast2", "junitFirst2"));
        PublicationDate date = new PublicationDate(2007);
         String fullText = 
            "In der einfachen Suche können Sie in die Suchmaske ein oder ";
        String title = "Junit document title2";
        Document doc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        Term term = TermFactory.create("junitTerm", PartOfSpeechTag.UNKNOWN);
        ProcessingInfo info = 
            ProcessingInfoFactory.create("junitTest", "junit test cases");
        TermDocumentFrequencySet expectedSet = 
            TermDocumentFrequenceSetFactory.create(term, doc, 123, info);
        TermDocumentFrequencySet retrievedSet = 
            TermDocumentFrequenceSetFactory.create(term, doc, 123, info);
        assertEquals(expectedSet, retrievedSet);
        //test it with none db document and terms
        file = "noDBObjectsTestFile";
        authors.clear();
        authors.add(
                new DocumentAuthor("junitLast1", "junitFirst1"));
        authors.add(
                new DocumentAuthor("junitLast3", "junitFirst3"));
        date = new PublicationDate(2007, 1);
         fullText = 
            "JUnit test with none db objects ";
        title = "Junit document title3 - None db objects";
        doc = new Document(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        term = new Term("junitTerm", PartOfSpeechTag.UNKNOWN);
        info = new ProcessingInfo("junitTest", "junit test cases");
        expectedSet = 
            TermDocumentFrequenceSetFactory.create(term, doc, 123, info);
        retrievedSet = 
            TermDocumentFrequenceSetFactory.create(term, doc, 123, info);
        assertEquals(expectedSet, retrievedSet);
    }
}
