<script>
import { optionsMapper } from '@/utils/nodeDialogUtils';
import DropdownInput from './DropdownInput.vue';

export default {
    name: 'ColumnSelect',
    components: {
        DropdownInput
    },
    inheritAttrs: false,
    methods: {
        optionsGenerator(control) {
            return [
                ...control.uischema.options.showNoneColumn ? [{ id: '<none>', text: 'None' }] : [],
                ...control.uischema.options.showRowKeys ? [{ id: '<row-keys>', text: 'RowIDs' }] : [],
                ...control.schema.oneOf.map(optionsMapper)
            ];
        }
    }
};
</script>

<template>
  <DropdownInput
    v-bind="$attrs"
    :options-generator="optionsGenerator"
  />
</template>
