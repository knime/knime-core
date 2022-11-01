<script>
import { defineComponent } from '@vue/composition-api';
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue2';
import { checkIsModelSetting, getFlowVariablesMap } from '@/utils/nodeDialogUtils';
import NumberInput from '~/webapps-common/ui/components/forms/NumberInput.vue';
import LabeledInput from './LabeledInput.vue';

const NumberInputBase = defineComponent({
    name: 'NumberInputBase',
    components: {
        NumberInput,
        LabeledInput
    },
    props: {
        ...rendererProps(),
        type: {
            type: String,
            required: false,
            default: 'double'
        }
    },
    setup(props) {
        return useJsonFormsControl(props);
    },
    computed: {
        isModelSetting() {
            return checkIsModelSetting(this.control);
        },
        flowSettings() {
            return getFlowVariablesMap(this.control);
        },
        disabled() {
            return !this.control.enabled || this.flowSettings?.controllingFlowVariableAvailable;
        }
    },
    methods: {
        onChange(event) {
            this.handleChange(this.control.path, event);
            if (this.isModelSetting) {
                this.$store.dispatch('pagebuilder/dialog/dirtySettings', true);
            }
        }
    }
});
export default NumberInputBase;
</script>

<template>
  <LabeledInput
    v-if="control.visible"
    :text="control.label"
    :description="control.description"
    :errors="[control.errors]"
    :is-model-setting="isModelSetting"
    :scope="control.uischema.scope"
    :flow-settings="flowSettings"
  >
    <NumberInput
      class="number-input"
      :disabled="!control.enabled"
      :value="control.data"
      :type="type"
      :min="control.schema.minimum"
      :max="control.schema.maximum"
      @input="onChange"
    />
  </LabeledInput>
</template>

<style lang="postcss" scoped>
  .number-input {
    height: 40px;

    & >>> input[type="number"] {
      height: 38px;
    }
  }
</style>
