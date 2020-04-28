#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
	// provide a list of upstream jobs which should trigger a rebuild of this job
	pipelineTriggers([upstream(
		'knime-shared/' + env.BRANCH_NAME.replaceAll('/', '%2F')
	)]),
	buildDiscarder(logRotator(numToKeepStr: '5')),
	disableConcurrentBuilds()
])

try {
    parallel (
        'Tycho Build': {
	        knimetools.defaultTychoBuild('org.knime.update.core')
        },
        'Testing: Linux': {
            node("ubuntu18.04 && workflow-tests") {
                checkout scm
                knimetools.runIntegratedWorkflowTests(profile: 'test')
            }
         },
        'Testing: Windows': {
            node('windows && p2-director') {
                checkout scm
                knimetools.runIntegratedWorkflowTests(profile: 'test')
            }
        },
        'Testing: MacOs': {
            node('macosx') {
                checkout scm
                knimetools.runIntegratedWorkflowTests(profile: 'test')
            }
        },
     )

    workflowTests.runTests(
        dependencies: [
            repositories: ['knime-core', 'knime-json', 'knime-python', 'knime-product']
        ],
        withAssertions: true
    )

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
