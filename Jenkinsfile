#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
	// provide a list of upstream jobs which should trigger a rebuild of this job
	pipelineTriggers([upstream(
		'knime-shared/' + env.BRANCH_NAME.replaceAll('/', '%2F') + ',' +
		'knime-tp/' + env.BRANCH_NAME.replaceAll('/', '%2F')
	)]),
	buildDiscarder(logRotator(numToKeepStr: '5')),
	disableConcurrentBuilds()
])

try {
	knimetools.defaultTychoBuild('org.knime.update.core')

	/* workflowTests.runTests( */
	/* 	"org.knime.features.core.testing.feature.group", */
	/* 	false, */
	/* 	["knime-core", "knime-shared", "knime-tp"], */
	/* ) */

	stage('Sonarqube analysis') {
		env.lastStage = env.STAGE_NAME
		workflowTests.runSonar([])
	}
} catch (ex) {
	currentBuild.result = 'FAILED'
	throw ex
} finally {
	notifications.notifyBuild(currentBuild.result);
}
 /* vim: set ts=4: */
