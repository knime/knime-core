#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
	pipelineTriggers([upstream(
		'knime-shared/' + env.BRANCH_NAME.replaceAll('/', '%2F')
	)]),
	parameters(workflowTests.getConfigurationsAsParameters()),
	buildDiscarder(logRotator(numToKeepStr: '5')),
	disableConcurrentBuilds()
])

try {
    parallel (
        'Tycho Build': {
	        knimetools.defaultTychoBuild('org.knime.update.core')
        },
        'Integrated Workflowtests': {
                workflowTests.runIntegratedWorkflowTests(profile: 'test')
         },
     )

    workflowTests.runTests(
        dependencies: [
            repositories: ['knime-json', 'knime-python', 'knime-filehandling',
                'knime-datageneration', 'knime-jep', 'knime-js-base', 'knime-cloud', 'knime-database', 'knime-kerberos',
				'knime-textprocessing', 'knime-dl4j', 'knime-virtual', 'knime-r', 'knime-streaming', 'knime-cluster']
        ]
    )

	stage('Sonarqube analysis') {
		env.lastStage = env.STAGE_NAME
		workflowTests.runSonar()
	}
} catch (ex) {
	currentBuild.result = 'FAILURE'
	throw ex
} finally {
	notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
