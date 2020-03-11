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
             runIntegratedWorkflowTests('ubuntu18.04 && workflow-tests')
         },
        'Testing: Windows': {
            runIntegratedWorkflowTests('windows && p2-director')
        },
        'Testing: MacOs': {
            runIntegratedWorkflowTests('macosx')
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

def runIntegratedWorkflowTests(String image){
    node(image){ 
        stage('Integrated Workflow Tests:'){
            env.lastStage = env.STAGE_NAME
            checkout scm
            withMavenJarsignerCredentials(options: [artifactsPublisher(disabled: true)]) {
                withCredentials([usernamePassword(credentialsId: 'ARTIFACTORY_CREDENTIALS', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_LOGIN')]) {
                    sh '''
                        export TEMP="${WORKSPACE}/tmp"
                        rm -rf "${TEMP}"; mkdir "${TEMP}"
                        
                        XVFB=$(which Xvfb) || true
                        if [[ -x "$XVFB" ]]; then
                            Xvfb :$$ -pixdepths 24 -screen 0 1280x1024x24 +extension RANDR &
                            XVFB_PID=$!
                            export DISPLAY=:$$
                        fi

                        mvn -Dmaven.test.failure.ignore=true -Dknime.p2.repo=${P2_REPO} clean verify -P test
                        rm -rf "${TEMP}"
                        if [[ -n "$XVFB_PID" ]]; then
                            kill $XVFB_PID
                        fi
                    '''
                }
            }
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
        }
    }
}
 /* vim: set ts=4: */
