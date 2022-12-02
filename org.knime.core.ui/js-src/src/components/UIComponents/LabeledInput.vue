<script>
import Label from '~/webapps-common/ui/components/forms/Label.vue';
import ErrorMessage from './ErrorMessage.vue';
import FlowVariableIcon from './FlowVariableIcon.vue';
import ReexecutionIcon from '~/webapps-common/ui/assets/img/icons/reexecution.svg?inline';
import DescriptionPopover from './DescriptionPopover.vue';

const LabeledInput = {
    name: 'LabeledInput',
    components: {
        Label,
        ErrorMessage,
        FlowVariableIcon,
        ReexecutionIcon,
        DescriptionPopover
    },
    data() {
        return {
            hover: false
        };
    },
    props: {
        text: {
            default: '',
            type: String
        },
        description: {
            default: null,
            type: String
        },
        errors: {
            default: () => [],
            type: Array
        },
        showErrors: {
            default: true,
            type: Boolean
        },
        showReexecutionIcon: {
            default: false,
            type: Boolean
        },
        scope: {
            default: '',
            type: String
        },
        flowSettings: {
            default: null,
            type: Object
        }
    }
};
export default LabeledInput;
</script>

<template>
  <div
    class="labeled-input"
    @mouseover="hover = true"
    @mouseleave="hover = false"
  >
    <Label
      :text="text"
      :compact="true"
    >
      <ReexecutionIcon
        v-if="showReexecutionIcon"
        class="reexecution-icon"
      />
      <FlowVariableIcon
        :flow-settings="flowSettings"
      />
      <DescriptionPopover
        v-if="description"
        :html="description"
        :hover="hover"
        @close="hover = false"
      />
      <slot />
    </Label>
    <ErrorMessage
      v-if="showErrors"
      :error="errors"
    />
  </div>
</template>

<style lang="postcss" scoped>
.labeled-input {
  margin-bottom: 20px;
  position: relative;

  & > *:last-child {
    margin-top: 7px;
  }

  & span {
    font-weight: 300;
    font-size: 13px;
    color: var(--theme-color-error);
    display: inline-block;
    position: relative;
  }

  & >>> .label-text {
    display: inline-block;
    z-index: 1;
    max-width: calc(100% - var(--description-button-size) - 20px);
  }

  & .reexecution-icon {
    display: inline-block;
    vertical-align: top;
    height: 10px;
    margin: 3px 0 1px 5px;
  }
}
</style>
