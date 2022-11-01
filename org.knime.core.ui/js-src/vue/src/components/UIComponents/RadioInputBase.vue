<script>
import { defineComponent } from '@vue/composition-api';
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue2';
import { optionsMapper, getFlowVariablesMap, checkIsModelSetting } from '@/utils/nodeDialogUtils';
import RadioButtons from '~/webapps-common/ui/components/forms/RadioButtons.vue';
import ValueSwitch from '~/webapps-common/ui/components/forms/ValueSwitch.vue';
import LabeledInput from './LabeledInput.vue';

const RadioInputBase = defineComponent({
    name: 'RadioInputBase',
    components: {
        RadioButtons,
        ValueSwitch,
        LabeledInput
    },
    props: {
        ...rendererProps(),
        type: {
            type: String,
            required: true,
            default: 'radio'
        }
    },
    setup(props) {
        return useJsonFormsControl(props);
    },
    data() {
        return {
            options: null
        };
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
        },
        uiComponent() {
            switch (this.type) {
                case 'valueSwitch':
                    return ValueSwitch;
                case 'radio':
                    return RadioButtons;
                default:
                    return RadioButtons;
            }
        }
    },
    mounted() {
        this.options = this.control?.schema?.oneOf?.map(optionsMapper);
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
export default RadioInputBase;
</script>

<template>
  <LabeledInput
    v-if="control.visible"
    :text="control.label"
    :is-model-setting="isModelSetting"
    :scope="control.uischema.scope"
    :flow-settings="flowSettings"
    :description="control.description"
  >
    <component
      :is="uiComponent"
      v-if="options"
      :possible-values="options"
      :disabled="disabled"
      :value="control.data"
      @input="onChange"
    />
  </LabeledInput>
</template>

<style lang="postcss" scoped>
.labeled-input {
  margin-bottom: 10px;
}
</style>
