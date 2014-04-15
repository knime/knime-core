o Reviewed the website http://www.dmg.org/v4-2/Changes.html to check backwards compatibility
o To support 4.2 the version of older PMML documents is changed to 4.2 when they are read
o In the MiningSchema the usageType "predicted" was deprecated and changed to "target" in PMML 4.2, so we changed all occurrences of org.dmg.pmml.FIELDUSAGETYPE.PREDICTED
  to org.dmg.pmml.FIELDUSAGETYPE.TARGET when writing PMML documents and check now for both values when reading documents.
o Created test cases for reading 4.1 and 4.2 documents
o Changed the build.xml to create schemas.jar for PMML 4.2
o Created a new xsdconfig file for PMML 4.2