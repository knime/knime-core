<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--one rule, to transform the input root (/) -->
  <!-- test for PMML -->
  <xsl:template match="/">
    <Validation>
      <!-- PMML test -->
      <xsl:if test="count(PMML)=1">
        <!--<TEST name="PMML" status="pass"/>-->
      </xsl:if>

      <xsl:if test="count(PMML)=0">
        <TEST name="PMML" status="fail">
          <FAILURE msgid="1">Missing PMML element</FAILURE>
        </TEST>
      </xsl:if>
      <xsl:apply-templates select="PMML"/>


      <!--
 -->

    </Validation>
  </xsl:template>

  <xsl:template match="PMML">
    <!-- test for DataDictionary -->
    <xsl:choose>
      <xsl:when test="DataDictionary">
        <!--<TEST name="DataDictionary" status="pass"/>-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="DataDictionary" status="fail">
          <FAILURE msgid="3">Missing DataDictionary element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>



    <!-- test for supported models and content -->
    <xsl:choose>
      <xsl:when test="TreeModel">
        <!--<TEST name="Model" status="pass" details="TreeModel"/>-->
        <xsl:apply-templates select="TreeModel"/>
      </xsl:when>
      <xsl:when test="NeuralNetwork">
        <!--<TEST name="Model" status="pass" details="NeuralNetwork"/>-->
        <xsl:apply-templates select="NeuralNetwork"/>
      </xsl:when>
      <xsl:when test="ClusteringModel">
        <!--<TEST name="Model" status="pass" details="ClusteringModel"/>-->
        <xsl:apply-templates select="ClusteringModel"/>
      </xsl:when>
      <xsl:when test="RegressionModel">
        <!--<TEST name="Model" status="pass" details="RegessionModel"/>-->
        <xsl:apply-templates select="RegressionModel"/>
      </xsl:when>
      <xsl:when test="GeneralRegressionModel">
        <!--<TEST name="Model" status="pass" details="GeneralRegressionModel"/>-->
        <xsl:apply-templates select="GeneralRegressionModel"/>
      </xsl:when>
      <xsl:when test="MiningModel">
        <!--<TEST name="Model" status="pass" details="MiningModel"/>-->
        <xsl:apply-templates select="MiningModel"/>
      </xsl:when>
      <xsl:when test="SupportVectorMachineModel">
        <!--<TEST name="Model" status="pass" details="SupportVectorMachineModel"/>-->
        <xsl:apply-templates select="SupportVectorMachineModel"/>
      </xsl:when>
      <xsl:when test="RuleSetModel">
        <!--<TEST name="Model" status="pass" details="RuleSetModel"/>-->
        <xsl:apply-templates select="RuleSetModel"/>
      </xsl:when>
      <xsl:when test="AssociationModel">
        <!--<TEST name="Model" status="pass" details="AssociationModel"/>-->
        <xsl:apply-templates select="AssociationModel"/>
      </xsl:when>
      <xsl:when test="NaiveBayesModel">
        <!--<TEST name="Model" status="pass" details="NaiveBayesModel"/>-->
        <xsl:apply-templates select="NaiveBayesModel"/>
      </xsl:when>
      <xsl:when test="TextModel">
        <!--<TEST name="Model" status="pass" details="TextModel"/>-->
        <xsl:apply-templates select="TextModel"/>
      </xsl:when>
      <xsl:when test="SequenceModel">
        <!--<TEST name="Model" status="pass" details="SequenceModel"/>-->
        <xsl:apply-templates select="SequenceModel"/>
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Model" status="fail">
          <FAILURE msgid="4">Unsupported Model</FAILURE>
        </TEST>
      </xsl:otherwise>

    </xsl:choose>

  </xsl:template>

  <xsl:template match="AssociationModel"/>
  <xsl:template match="NaivesBayesModel"/>
  <xsl:template match="TextModel"/>
  <xsl:template match="SequenceModel"/>


  <xsl:template match="NeuralNetwork">
    <!-- test for MiningSchema -->
    <xsl:choose>
      <xsl:when test="MiningSchema">
        <!--<TEST name="MiningSchema" status="pass"/>-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="MiningSchema" status="fail">
          <FAILURE msgid="5">Missing MiningSchema element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!--test for exactly 1 neuralinputs-->
    <xsl:choose>
      <xsl:when test="count(NeuralInputs)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="NeuralInputs" status="fail">
          <FAILURE msgid="14">There must be exactly 1 NeuralInputs Element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="count(NeuralLayer)=0">
      <TEST name="NeuralLayer" status="fail">
        <FAILURE msgid="15">Missing NeuralLayer element</FAILURE>
      </TEST>
    </xsl:if>
    <xsl:choose>
      <!--test for NeuralOutputs less than equal to 1-->
      <xsl:when test="count(NeuralOutputs)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(NeuralOutputs)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="NeuralOutputs" status="fail">
          <FAILURE msgid="16">More than 1 NeuralOutputs Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <!--check for functionName attribute-->
      <xsl:when test="@functionName">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@functionName" status="fail">
          <FAILURE msgid="19">Missing functionName attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <!--check for activationFunction attribute-->
      <xsl:when test="count(@activationFunction)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@activationFunction" status="fail">
          <FAILURE msgid="19">Missing activationFunction attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="NeuralLayer">
      <xsl:apply-templates select="NeuralLayer"/>
    </xsl:if>
    <xsl:if test="NeuralOutputs">
      <xsl:apply-templates select="NeuralOutputs"/>
    </xsl:if>
    <xsl:if test="NeuralOutput">
      <xsl:apply-templates select="NeuralOutput"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="NeuralLayer">
    <xsl:if test="count(Neuron)=0">
      <TEST name="Neuron" status="fail">
        <FAILURE msgid="17">Missing Neuron in NeuralLayer</FAILURE>
      </TEST>
    </xsl:if>
  </xsl:template>

  <xsl:template match="NeuralOutputs">
    <xsl:if test="count(NeuralOutput)=0">
      <TEST name="NeuralOutput" status="fail">
        <FAILURE msgid="18">Missing NeuralOutput in NeuralOutputs</FAILURE>
      </TEST>
    </xsl:if>
  </xsl:template>

  <xsl:template match="NeuralOutput">
    <xsl:if test="@outputNeuron">
      <TEST name="outputNeuron" status="fail">
        <FAILURE msgid="20">Missing outputNeuron attribute in NeuralOutput</FAILURE>
      </TEST>
    </xsl:if>
  </xsl:template>

  <xsl:template match="TreeModel">
    <!-- test for MiningSchema -->
    <xsl:choose>
      <xsl:when test="MiningSchema">
        <!--<TEST name="MiningSchema" status="pass"/>-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="MiningSchema" status="fail">
          <FAILURE msgid="5">Missing MiningSchema element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Outputs-->
    <xsl:choose>
      <xsl:when test="count(Output)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Output)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Output" status="fail">
          <FAILURE msgid="16">More than 1 Output Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelStats-->
    <xsl:choose>
      <xsl:when test="count(ModelStats)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelStats)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelStats" status="fail">
          <FAILURE msgid="16">More than 1 ModelStats Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 LocalTransformations-->
    <xsl:choose>
      <xsl:when test="count(LocalTransformations)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(LocalTransformations)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="LocalTransformations" status="fail">
          <FAILURE msgid="16">More than 1 LocalTransformations Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelVerification-->
    <xsl:choose>
      <xsl:when test="count(ModelVerification)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelVerification)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelVerification" status="fail">
          <FAILURE msgid="16">More than 1 ModelVerification Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Targets-->
    <xsl:choose>
      <xsl:when test="count(Targets)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Targets)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Targets" status="fail">
          <FAILURE msgid="16">More than 1 Targets Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- exactly 1 Node-->
    <xsl:choose>
      <xsl:when test="count(Node)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Node" status="fail">
          <FAILURE msgid="16">Missing Node Element.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute functionname-->
    <xsl:choose>
      <xsl:when test="count(@functionName)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@functionName" status="fail">
          <FAILURE msgid="19">Missing functionName attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="Node">
      <xsl:apply-templates select="Node"/>
    </xsl:if>
    <xsl:if test="SimplePredicate">
      <xsl:apply-templates select="SimplePredicate"/>
    </xsl:if>
    <xsl:if test="CompoundPredicate">
      <xsl:apply-templates select="CompoundPredicate"/>
    </xsl:if>
    <xsl:if test="SimpleSetPredicate">
      <xsl:apply-templates select="SimpleSetPredicate"/>
    </xsl:if>
    <xsl:if test="ScoreDistribution">
      <xsl:apply-templates select="ScoreDistribution"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="Node">
    <!-- required attribute score-->
    <xsl:choose>
      <xsl:when test="count(@score)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@score" status="fail">
          <FAILURE msgid="16">Missing score attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="SimplePredicate">
    <!-- required attribute field-->
    <xsl:choose>
      <xsl:when test="count(@field)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@field" status="fail">
          <FAILURE msgid="16">Missing field attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute operator-->
    <xsl:choose>
      <xsl:when test="count(@operator)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@operator" status="fail">
          <FAILURE msgid="16">Missing operator attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="CompoundPredicate">
    <!-- required attribute booleanoperator-->
    <xsl:choose>
      <xsl:when test="count(@booleanOperator)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@booleanOpeartor" status="fail">
          <FAILURE msgid="16">Missing booleanOperator attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template match="SimpleSetPredicate">
    <!-- required attribute field-->
    <xsl:choose>
      <xsl:when test="count(@field)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@field" status="fail">
          <FAILURE msgid="16">Missing field attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute operator-->
    <xsl:choose>
      <xsl:when test="count(@booleanOperator)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@booleanOperator" status="fail">
          <FAILURE msgid="16">Missing booleanOperator attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="ScoreDistribution">
    <!-- required attribute value-->
    <xsl:choose>
      <xsl:when test="count(@value)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@value" status="fail">
          <FAILURE msgid="16">Missing value attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute recordCount-->
    <xsl:choose>
      <xsl:when test="count(@recordCount)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@recordCount" status="fail">
          <FAILURE msgid="16">Missing recordCount attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="ClusteringModel">
    <!-- test for MiningSchema -->
    <xsl:choose>
      <xsl:when test="MiningSchema">
        <!--<TEST name="MiningSchema" status="pass"/>-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="MiningSchema" status="fail">
          <FAILURE msgid="5">Missing MiningSchema element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- test for at least 1 cluster -->
    <xsl:if test="count(Cluster)=0">
      <TEST name="Cluster" status="fail">
        <FAILURE msgid="11">Missing Cluster Element</FAILURE>
      </TEST>
    </xsl:if>
    <!-- exactly 1 comparisonMeasure-->
    <xsl:choose>
      <xsl:when test="count(ComparisonMeasure)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ComparisonMeasure" status="fail">
          <FAILURE msgid="14">There must be exactly 1 ComparisonMeasure Element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Outputs-->
    <xsl:choose>
      <xsl:when test="count(Output)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Output)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Output" status="fail">
          <FAILURE msgid="16">More than 1 Output Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelStats-->
    <xsl:choose>
      <xsl:when test="count(ModelStats)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelStats)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelStats" status="fail">
          <FAILURE msgid="16">More than 1 ModelStats Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 LocalTransformations-->
    <xsl:choose>
      <xsl:when test="count(LocalTransformations)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(LocalTransformations)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="LocalTransformations" status="fail">
          <FAILURE msgid="16">More than 1 LocalTransformations Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 CenterFiels-->
    <xsl:choose>
      <xsl:when test="count(CenterFields)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(CenterFields)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="CenterFields" status="fail">
          <FAILURE msgid="16">More than 1 CenterFields Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 MissingValueWeights-->
    <xsl:choose>
      <xsl:when test="count(MissingValueWeights)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(MissingValueWeights)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="MissingValueWeights" status="fail">
          <FAILURE msgid="16">More than 1 MissingValueWeights Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelVerification-->
    <xsl:choose>
      <xsl:when test="count(ModelVerification)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelVerification)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelVerification" status="fail">
          <FAILURE msgid="16">More than 1 ModelVerification Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute functionname-->
    <xsl:choose>
      <xsl:when test="count(@functionName)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@functionName" status="fail">
          <FAILURE msgid="19">Missing functionName attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute modelClass-->
    <xsl:choose>
      <xsl:when test="count(@modelClass)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@modelClass" status="fail">
          <FAILURE msgid="19">Missing modelClass attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute numberofclusters-->
    <xsl:choose>
      <xsl:when test="count(@numberOfClusters)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@numberOfClusters" status="fail">
          <FAILURE msgid="19">Missing numberOfClusters attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="RegressionModel">
    <!-- test for MiningSchema -->
    <xsl:choose>
      <xsl:when test="MiningSchema">
        <!--<TEST name="MiningSchema" status="pass"/>-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="MiningSchema" status="fail">
          <FAILURE msgid="5">Missing MiningSchema element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Outputs-->
    <xsl:choose>
      <xsl:when test="count(Output)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Output)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Output" status="fail">
          <FAILURE msgid="16">More than 1 Output Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelStats-->
    <xsl:choose>
      <xsl:when test="count(ModelStats)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelStats)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelStats" status="fail">
          <FAILURE msgid="16">More than 1 ModelStats Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 LocalTransformations-->
    <xsl:choose>
      <xsl:when test="count(LocalTransformations)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(LocalTransformations)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="LocalTransformations" status="fail">
          <FAILURE msgid="16">More than 1 LocalTransformations Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelVerification-->
    <xsl:choose>
      <xsl:when test="count(ModelVerification)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelVerification)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelVerification" status="fail">
          <FAILURE msgid="16">More than 1 ModelVerification Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Targets-->
    <xsl:choose>
      <xsl:when test="count(Targets)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Targets)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Targets" status="fail">
          <FAILURE msgid="16">More than 1 Targets Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at least 1 RegressionTable-->
    <xsl:choose>
      <xsl:when test="count(RegressionTable)=0">
        <TEST name="RegressionTable" status="fail">
          <FAILURE msgid="16">Missing RegressionTable Element found.</FAILURE>
        </TEST>
      </xsl:when>
      <xsl:otherwise>
        <!-- do nothing-->
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute functionname-->
    <xsl:choose>
      <xsl:when test="count(@functionName)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@functionName" status="fail">
          <FAILURE msgid="19">Missing functionName attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="NumericPredictor">
      <xsl:apply-templates select="NumericPredictor"/>
    </xsl:if>
    <xsl:if test="CategoricalPredictor">
      <xsl:apply-templates select="CategoricalPredictor"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="NumericPredictor">
    <xsl:choose>
      <xsl:when test="count(@name)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@name" status="fail">
          <FAILURE msgid="19">Missing name attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="count(@coefficient)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@coefficient" status="fail">
          <FAILURE msgid="19">Missing coefficient attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="CategoricalPredictor">
    <xsl:choose>
      <xsl:when test="count(@name)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@name" status="fail">
          <FAILURE msgid="19">Missing name attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="count(@coefficient)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@coefficient" status="fail">
          <FAILURE msgid="19">Missing coefficient attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="count(@value)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@value" status="fail">
          <FAILURE msgid="19">Missing value attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template match="GeneralRegressionModel">
    <!-- test of CovariateList and/or FactorList (GeneralRegressionModel only) -->
    <xsl:choose>
      <xsl:when test="CovariateList">
        <!--<TEST name="Covariate-FactorList" status="pass" details="CovariateList"/>-->
      </xsl:when>
      <xsl:when test="FactorList">
        <!--<TEST name="Covariate-FactorList" status="pass" details="FactorList"/>-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Covariate-FactorList" status="fail">
          <FAILURE msgid="6">GeneralRegression missing at least one of CovariateList or FactorList</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!--test of exactly one ParameterList-->
    <xsl:choose>
      <xsl:when test="count(ParameterList)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ParameterList" status="fail">
          <FAILURE msgid="12">There should be exactly 1 ParameterList element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!--test of exactly one ParamMatrix-->
    <xsl:choose>
      <xsl:when test="count(ParamMatrix)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ParamMatrix" status="fail">
          <FAILURE msgid="12">There should be exactly 1 ParamMatrix element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="ParameterList">
      <xsl:apply-templates select="ParameterList"/>
    </xsl:if>
    <xsl:if test="ParamMatrix">
      <xsl:apply-templates select="ParamMatrix"/>
    </xsl:if>
    <xsl:if test="PPMatrix">
      <xsl:apply-templates select="PPMatrix"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="ParameterList">
    <!--test for at least one parameter-->
    <xsl:if test="count(Parameter)=0">
      <TEST name="Parameter" status="fail">
        <FAILURE msgid="13">Missing Parameter Element</FAILURE>
      </TEST>
    </xsl:if>
  </xsl:template>

  <xsl:template match="ParamMatrix">
    <!--test for at least one PCell-->
    <xsl:if test="count(PCell)=0">
      <TEST name="PCell" status="fail">
        <FAILURE msgid="30">Missing PCell Element</FAILURE>
      </TEST>
    </xsl:if>
  </xsl:template>

  <xsl:template match="PPMatrix">
    <!--test for at least one PPCell-->
    <xsl:if test="count(PPCell)=0">
      <TEST name="PPCell" status="fail">
        <FAILURE msgid="40">Missing PPCell Element</FAILURE>
      </TEST>
    </xsl:if>
  </xsl:template>

  <xsl:template match="MiningModel">
    <!-- test for MiningSchema -->
    <xsl:choose>
      <xsl:when test="MiningSchema">
        <!--<TEST name="MiningSchema" status="pass"/> -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="MiningSchema" status="fail">
          <FAILURE msgid="5">Missing MiningSchema element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="SupportVectorMachineModel">
    <!-- test for MiningSchema -->
    <xsl:choose>
      <xsl:when test="MiningSchema">
        <!--<TEST name="MiningSchema" status="pass"/> -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="MiningSchema" status="fail">
          <FAILURE msgid="5">Missing MiningSchema element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Outputs-->
    <xsl:choose>
      <xsl:when test="count(Output)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Output)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Output" status="fail">
          <FAILURE msgid="16">More than 1 Output Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelStats-->
    <xsl:choose>
      <xsl:when test="count(ModelStats)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelStats)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelStats" status="fail">
          <FAILURE msgid="16">More than 1 ModelStats Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 LocalTransformations-->
    <xsl:choose>
      <xsl:when test="count(LocalTransformations)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(LocalTransformations)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="LocalTransformations" status="fail">
          <FAILURE msgid="16">More than 1 LocalTransformations Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelVerification-->
    <xsl:choose>
      <xsl:when test="count(ModelVerification)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelVerification)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelVerification" status="fail">
          <FAILURE msgid="16">More than 1 ModelVerification Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Targets-->
    <xsl:choose>
      <xsl:when test="count(Targets)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Targets)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Targets" status="fail">
          <FAILURE msgid="16">More than 1 Targets Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- exactly 1 VectorDictionary-->
    <xsl:choose>
      <xsl:when test="count(VectorDictionary)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="VectorDictionary" status="fail">
          <FAILURE msgid="16">Missing VectorDictionary Element.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at least 1 SVM-->
    <xsl:choose>
      <xsl:when test="count(SupportVectorMachine)=0">
        <TEST name="SupportVectorMachine" status="fail">
          <FAILURE msgid="16">Missing SupportVectorMachine Element.</FAILURE>
        </TEST>
      </xsl:when>
      <xsl:otherwise>
        <!-- do nothing-->
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute functionname-->
    <xsl:choose>
      <xsl:when test="count(@functionName)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@functionName" status="fail">
          <FAILURE msgid="19">Missing functionName attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="VectorDictionary">
      <xsl:apply-templates select="VectorDictionary"/>
    </xsl:if>
    <xsl:if test="VectorFields">
      <xsl:apply-templates select="VectorFields"/>
    </xsl:if>
    <xsl:if test="SupportVectorMachine">
      <xsl:apply-templates select="SupportVectorMachine"/>
    </xsl:if>
    <xsl:if test="SupportVectors">
      <xsl:apply-templates select="SupportVectors"/>
    </xsl:if>
    <xsl:if test="SupportVector">
      <xsl:apply-templates select="SupportVector"/>
    </xsl:if>
    <xsl:if test="Coefficients">
      <xsl:apply-templates select="Coefficients"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="VectorDictionary">
    <!-- exactly 1 vectorfields-->
    <xsl:choose>
      <xsl:when test="count(VectorFields)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="VectorFields" status="fail">
          <FAILURE msgid="19">Missing VectorFields element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="VectorFields">
    <!-- at least 1 field-->
    <xsl:choose>
      <xsl:when test="count(FieldRef)=0">
        <TEST name="FieldRef" status="fail">
          <FAILURE msgid="19">Missing FieldRef element</FAILURE>
        </TEST>
      </xsl:when>
      <xsl:otherwise>
        <!--do nothing -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="SupportVectorMachine">
    <!-- at most 1 SupportVectors-->
    <xsl:choose>
      <xsl:when test="count(SupportVectors)=0">
        <!-- do nothing-->
      </xsl:when>
      <xsl:when test="count(SupportVectors)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="SupportVectors" status="fail">
          <FAILURE msgid="19">Missing SupportVectors element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- exactly 1 Coefficients-->
    <xsl:choose>
      <xsl:when test="count(Coefficients)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Coefficients" status="fail">
          <FAILURE msgid="19">Missing Coefficients element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="SupportVectors">
    <!-- at least 1 SupportVector-->
    <xsl:choose>
      <xsl:when test="count(SupportVector)=0">
        <TEST name="SupportVector" status="fail">
          <FAILURE msgid="19">Missing SupportVector element</FAILURE>
        </TEST>
      </xsl:when>
      <xsl:otherwise>
        <!--do nothing -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="SupportVector">
    <!-- required attribute vectorID-->
    <xsl:choose>
      <xsl:when test="count(@vectorID)=0">
        <TEST name="@vectorID" status="fail">
          <FAILURE msgid="19">Missing vectorID attribute</FAILURE>
        </TEST>
      </xsl:when>
      <xsl:otherwise>
        <!--do nothing -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="Coefficients">
    <!-- at least 1 coefficient-->
    <xsl:choose>
      <xsl:when test="count(Coefficient)=0">
        <TEST name="Coefficient" status="fail">
          <FAILURE msgid="19">Missing Coefficient element</FAILURE>
        </TEST>
      </xsl:when>
      <xsl:otherwise>
        <!--do nothing -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="RuleSetModel">
    <!-- test for MiningSchema -->
    <xsl:choose>
      <xsl:when test="MiningSchema">
        <!--<TEST name="MiningSchema" status="pass"/> -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="MiningSchema" status="fail">
          <FAILURE msgid="5">Missing MiningSchema element</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Outputs-->
    <xsl:choose>
      <xsl:when test="count(Output)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Output)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Output" status="fail">
          <FAILURE msgid="16">More than 1 Output Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelStats-->
    <xsl:choose>
      <xsl:when test="count(ModelStats)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelStats)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelStats" status="fail">
          <FAILURE msgid="16">More than 1 ModelStats Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 LocalTransformations-->
    <xsl:choose>
      <xsl:when test="count(LocalTransformations)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(LocalTransformations)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="LocalTransformations" status="fail">
          <FAILURE msgid="16">More than 1 LocalTransformations Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 ModelVerification-->
    <xsl:choose>
      <xsl:when test="count(ModelVerification)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(ModelVerification)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="ModelVerification" status="fail">
          <FAILURE msgid="16">More than 1 ModelVerification Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- at most 1 Targets-->
    <xsl:choose>
      <xsl:when test="count(Targets)=1">
        <!--do nothing-->
      </xsl:when>
      <xsl:when test="count(Targets)=0">
        <!--do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="Targets" status="fail">
          <FAILURE msgid="16">More than 1 Targets Element found.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- exactly 1 RuleSet-->
    <xsl:choose>
      <xsl:when test="count(RuleSet)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="RuleSet" status="fail">
          <FAILURE msgid="16">Exactly one RuleSet Element allowed.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <!-- required attribute functionname-->
    <xsl:choose>
      <xsl:when test="count(@functionName)=1">
        <!--do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@functionName" status="fail">
          <FAILURE msgid="19">Missing functionName attribute</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="RuleSet">
      <xsl:apply-templates select="RuleSet"/>
    </xsl:if>
    <xsl:if test="RuleSelectionMethod">
      <xsl:apply-templates select="RuleSelectionMethod"/>
    </xsl:if>
    <xsl:if test="SimpleRule">
      <xsl:apply-templates select="SimpleRule"/>
    </xsl:if>
    <xsl:if test="CompoundRule">
      <xsl:apply-templates select="CompoundRule"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="RuleSet">
    <!-- at least 1 RuleSelectionMethod-->
    <xsl:choose>
      <xsl:when test="count(RuleSelectionMethod)=0">
        <TEST name="RuleSelectionMethod" status="fail">
          <FAILURE msgid="16">Missing RuleSelectionMethod Element found.</FAILURE>
        </TEST>
      </xsl:when>
      <xsl:otherwise>
        <!-- do nothing-->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="RuleSelectionMethod">
    <!--required attribute criterion-->
    <xsl:choose>
      <xsl:when test="count(@criterion)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@criterion" status="fail">
          <FAILURE msgid="16">Missing criterion attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="SimpleRule">
    <!--required attribute score-->
    <xsl:choose>
      <xsl:when test="count(@score)=1">
        <!-- do nothing-->
      </xsl:when>
      <xsl:otherwise>
        <TEST name="@score" status="fail">
          <FAILURE msgid="16">Missing score attribute.</FAILURE>
        </TEST>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="CompoundRule">
    <!--at least 1 rule-->
    <xsl:choose>
      <xsl:when test="count(Rule)=0">
        <TEST name="Rule" status="fail">
          <FAILURE msgid="16">Missing Rule Element.</FAILURE>
        </TEST>
      </xsl:when>
      <xsl:otherwise>
        <!-- do nothing-->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>