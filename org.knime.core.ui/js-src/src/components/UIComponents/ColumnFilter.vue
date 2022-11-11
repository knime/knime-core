<script>
import { defineComponent } from '@vue/composition-api';
import TwinlistInput from './TwinlistInput.vue';

const defaultTwinlistSize = 7;
const defaultTwinlistLeftLabel = 'Excludes';
const defaultTwinlistRightLabel = 'Includes';

const ColumnFilter = defineComponent({
    name: 'ColumnFilter',
    components: {
        TwinlistInput
    },
    inheritAttrs: false,
    props: {
        twinlistSize: {
            type: Number,
            required: false,
            default: defaultTwinlistSize
        },
        twinlistLeftLabel: {
            type: String,
            required: false,
            default: defaultTwinlistLeftLabel
        },
        twinlistRightLabel: {
            type: String,
            required: false,
            default: defaultTwinlistRightLabel
        }
    }
});
export default ColumnFilter;
</script>

<template>
  <TwinlistInput
    v-bind="$attrs"
    :twinlist-size="twinlistSize"
    :twinlist-left-label="twinlistLeftLabel"
    :twinlist-right-label="twinlistRightLabel"
  />
</template>
