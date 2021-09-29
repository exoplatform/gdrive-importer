<template>
  <v-app>
    <v-text-field
      :label="$t('UICloudLinkForm.label.titleTextBox')"
      v-model="folderOrFileId" />
    <span class="text-caption">
      <v-icon class="blue--text pt-2 pb-5">mdi-information</v-icon>
      {{ $t('UICloudLinkForm.msg.action.info') }}
    </span>
    <v-btn
      @click="authenticate"
      color="primary">
      {{ $t('UICloudLinkForm.action.Clone') }}
    </v-btn>
  </v-app>
</template>

<script>
export default {
  data: () => ({
    folderOrFileId: '',
  }),
  methods: {
    authenticate() {
      const groupId = `/spaces/${eXo.env.portal.spaceGroup}`;
      let targetId;
      if (this.folderOrFileId.indexOf('/d/') !== -1) {
        targetId = this.folderOrFileId.split('/d/')[1].split('/')[0];
      } else if (this.folderOrFileId.indexOf('/drive/folders/') !== -1) {
        targetId = this.folderOrFileId.split('/drive/folders/')[1].split('/')[0];
      }
      ClonedDrive.auth(targetId, groupId);
    },
  }
};
</script>
