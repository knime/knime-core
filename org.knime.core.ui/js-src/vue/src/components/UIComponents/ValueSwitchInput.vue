<script>
import { defineComponent } from '@vue/composition-api';
import RadioInputBase from './RadioInputBase.vue';

const ValueSwitchInput = defineComponent({
    name: 'ValueSwitchInput',
    components: {
        RadioInputBase
    },
    inheritAttrs: false
});
export default ValueSwitchInput;
</script>

<template>
  <RadioInputBase
    v-bind="$attrs"
    type="valueSwitch"
  />
</template>
