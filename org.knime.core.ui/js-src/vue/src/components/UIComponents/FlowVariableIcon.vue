<script>
import OnlyFlowVariable from '~/webapps-common/ui/assets/img/icons/only-flow-variables.svg?inline';
import ExposeFlowVariable from '~/webapps-common/ui/assets/img/icons/expose-flow-variables.svg?inline';
import BothFlowVariables from '~/webapps-common/ui/assets/img/icons/both-flow-variables.svg?inline';

const FlowVariableIcon = {
    name: 'FlowVariableIcon',
    components: {
        OnlyFlowVariable,
        ExposeFlowVariable,
        BothFlowVariables
    },
    props: {
        flowSettings: {
            default: null,
            type: Object
        }
    },
    computed: {
        isControlledByFlowVariable() {
            return this.flowSettings?.controllingFlowVariableName;
        },
        isExposedFlowVariable() {
            return this.flowSettings?.exposedFlowVariableName;
        }
    }
};
export default FlowVariableIcon;
</script>

<template>
  <div
    v-if="isControlledByFlowVariable && isExposedFlowVariable"
    title="Config is overwritten by flow variable and exposes a flow variable"
    class="flow-icon-tooltip"
  >
    <BothFlowVariables
      class="flow-icon"
    />
  </div>
  <div
    v-else-if="isControlledByFlowVariable"
    title="Config is overwritten by a flow variable"
    class="flow-icon-tooltip"
  >
    <OnlyFlowVariable class="flow-icon" />
  </div>
  <div
    v-else-if="isExposedFlowVariable"
    title="Config exposes a flow variable"
    class="flow-icon-tooltip"
  >
    <ExposeFlowVariable class="flow-icon" />
  </div>
</template>

<style lang="postcss" scoped>
.flow-icon-tooltip {
  display: inline-block;
  vertical-align: top;
}

.flow-icon {
  height: 15px;
  margin-left: 5px;
  position: relative;
  z-index: 1;
}
</style>
