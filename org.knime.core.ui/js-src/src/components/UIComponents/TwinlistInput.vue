<script>
import { defineComponent } from '@vue/composition-api';
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue2';
import { optionsMapper, getFlowVariablesMap, checkIsModelSetting } from '@/utils/nodeDialogUtils';
import Twinlist from '~/webapps-common/ui/components/forms/Twinlist.vue';
import LabeledInput from './LabeledInput.vue';

const defaultTwinlistSize = 7;
const defaultTwinlistLabelLeft = 'Excluded Values';
const defaultTwinlistLabelRight = 'Included Values';

const TwinlistInput = defineComponent({
    name: 'TwinListInput',
    components: {
        Twinlist,
        LabeledInput
    },
    props: {
        ...rendererProps(),
        twinlistSize: {
            type: Number,
            required: false,
            default: defaultTwinlistSize
        },
        twinlistLabelLeft: {
            type: String,
            required: false,
            default: defaultTwinlistLabelLeft
        },
        twinlistLabelRight: {
            type: String,
            required: false,
            default: defaultTwinlistLabelRight
        }
    },
    setup(props) {
        return useJsonFormsControl(props);
    },
    data() {
        return {
            possibleValues: null
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
        }
    },
    created() {
        this.possibleValues = this.control.schema.anyOf.map(optionsMapper);
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
export default TwinlistInput;
</script>

<template>
  <LabeledInput
    :text="control.label"
    :is-model-setting="isModelSetting"
    :scope="control.uischema.scope"
    :flow-settings="flowSettings"
    :description="control.description"
  >
    <Twinlist
      v-if="possibleValues"
      :disabled="disabled"
      :value="control.data"
      :possible-values="possibleValues"
      :size="twinlistSize"
      :label-left="twinlistLabelLeft"
      :label-right="twinlistLabelRight"
      @input="onChange"
    />
  </LabeledInput>
</template>

<style lang="postcss" scoped>
.twinlist >>> .lists >>> .multiselect-list-box >>> [role="listbox"] {
  font-size: 13px;
}

.twinlist >>> .header >>> .title {
  font-size: 13px;
  font-weight: 500;
  color: var(--knime-dove-gray);
}
</style>
