<script>
import { defineComponent } from '@vue/composition-api';
import NumberInputBase from './NumberInputBase.vue';

const IntegerInput = defineComponent({
    name: 'IntegerInput',
    components: {
        NumberInputBase
    },
    inheritAttrs: false
});
export default IntegerInput;
</script>

<template>
  <NumberInputBase
    v-bind="$attrs"
    type="double"
  />
</template>
