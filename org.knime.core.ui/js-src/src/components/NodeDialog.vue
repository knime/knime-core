<script>
import { mapState } from 'vuex';
import { JsonDataService, DialogService } from '@knime/ui-extension-service';
import { vanillaRenderers } from '@jsonforms/vue2-vanilla';
import { JsonForms } from '@jsonforms/vue2';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import Button from '~/webapps-common/ui/components/Button.vue';
import { createAjv } from '@jsonforms/core';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

export default {
    components: {
        JsonForms,
        Button
    },
    inject: ['getKnimeService'],
    data() {
        return {
            jsonDataService: null,
            settings: null,
            originalSettingsData: null,
            renderers: Object.freeze(renderers)
        };
    },
    computed: {
        // TODO: UIEXT-236 Move to dialog service
        ...mapState('pagebuilder/dialog', ['dirtyModelSettings'])
    },
    async mounted() {
        this.jsonDataService = new JsonDataService(this.getKnimeService());
        this.dialogService = new DialogService(this.getKnimeService());

        const settings = await this.jsonDataService.initialData();

        settings.schema.flowVariablesMap = await this.dialogService.getFlowVariableSettings();
        settings.schema.hasNodeView = this.dialogService.hasNodeView();
        this.settings = settings;

        this.originalSettingsData = JSON.stringify(this.settings?.data || {});
        this.jsonDataService.registerDataGetter(this.getData);
        this.$store.dispatch('pagebuilder/dialog/setApplySettings', { applySettings: this.applySettings });
        // TODO UIEXT-254: This is needed until we update to jsonforms 3.0.x as otherwise the different number types are
        // not supported
        this.ajv = createAjv({ unknownFormats: 'ignore' });
    },
    methods: {
        getData() {
            return this.settings.data;
        },
        onSettingsChanged(data) {
            if (data.data) {
                this.settings.data = data.data;
                // TODO: UIEXT-236 Move to dialog service
                if (this.originalSettingsData === JSON.stringify(this.settings.data)) {
                    this.$store.dispatch('pagebuilder/dialog/cleanSettings');
                } else {
                    this.$store.dispatch('pagebuilder/dialog/dirtySettings');
                }
                // TODO: UIEXT-236 Move to dialog service
                if (!this.dirtyModelSettings) {
                    this.jsonDataService.publishData(this.settings);
                }
            }
        },
        async applySettings() {
            this.originalSettingsData = JSON.stringify(this.settings.data);
            await this.jsonDataService.applyData();
        },
        async applySettingsCloseDialog() {
            await this.applySettings();
            this.closeDialog();
        },
        closeDialog() {
            window.closeCEFWindow();
        }
    }
};
</script>

<template>
  <div class="dialog">
    <div class="form">
      <JsonForms
        v-if="settings"
        :data="settings.data"
        :schema="settings.schema"
        :uischema="settings.ui_schema"
        :renderers="renderers"
        :ajv="ajv"
        @change="onSettingsChanged"
      />
    </div>
    <div class="controls">
      <Button
        with-border
        compact
        @click="closeDialog"
      >
        Cancel
      </Button>
      <Button
        primary
        compact
        @click.prevent="applySettingsCloseDialog"
      >
        Ok
      </Button>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.dialog {
  --controls-height: 49px;
  --description-button-size: 15px;

  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100vh;
  background-color: var(--knime-gray-ultra-light);
  border-left: 1px solid var(--knime-silver-sand);
  padding: 11px 0; /* Padding set at 11px to align with the commons "Messages" component */

  & .form {
    display: flex;
    flex-direction: column;
    height: calc(100vh - var(--controls-height));
    padding: 0 20px;
    overflow: hidden;
    overflow-y: auto;
  }

  & .controls {
    display: flex;
    justify-content: space-between;
    height: var(--controls-height);
    padding: 13px 20px 5px;
    background-color: var(--knime-gray-light-semi);
    border-top: 1px solid var(--knime-silver-sand);
  }
}
</style>
