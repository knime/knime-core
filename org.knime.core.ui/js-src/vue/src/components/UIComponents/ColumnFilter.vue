<script>
import { defineComponent } from '@vue/composition-api';
import TwinlistInput from './TwinlistInput.vue';

const defaultTwinlistSize = 7;
const defaultTwinlistLabelLeft = 'Excludes';
const defaultTwinlistLabelRight = 'Includes';

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
    }
});
export default ColumnFilter;
</script>

<template>
  <TwinlistInput
    v-bind="$attrs"
    :twinlist-size="twinlistSize"
    :twinlist-label-left="twinlistLabelLeft"
    :twinlist-label-right="twinlistLabelRight"
  />
</template>
