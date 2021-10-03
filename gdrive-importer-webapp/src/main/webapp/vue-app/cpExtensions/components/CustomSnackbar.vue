<template>
  <v-app>
    <v-snackbar
      dark
      right="right"
      top="top"
      :color="typeStyle"
      v-model="showWarning"
      :timeout="autoClose? 3000: -1">
      <v-container>
        <v-row no-gutters>
          <v-col class="text-center">
            <span>{{ $t(errorMessage) }}
              <v-btn
                v-if="link"
                :href="link"
                text
                target="_blank">
                Find your Drive
              </v-btn>
            </span>
          </v-col>
        </v-row>
      </v-container>
      <template v-slot:action="{ attrs }">
        <v-icon
          text
          v-bind="attrs"
          @click="showWarning = false">
          mdi-close
        </v-icon>
      </template>
    </v-snackbar>
  </v-app>
</template>

<script>
export default {
  props: {
    showWarning: {
      type: Boolean,
      default: () => {
        return false;
      }
    },
    errorMessage: {
      type: String,
      default: () => {
        return '';
      }
    },
    type: {
      type: String,
      default: () => {
        return 'error';
      }
    },
    link: {
      type: String,
      default: () => {
        return 'error';
      }
    },
    autoClose: {
      type: Boolean,
      default: () => {
        return true;
      }
    },
  },
  computed: {
    typeStyle() {
      return this.type && this.type === 'error' ? 'red darken-2' : this.type === 'warning'? 'orange darken-2' : 'blue darken-2';
    }
  },
  methods: {
    showSnack(show, message, type, autoClose, link) {
      this.errorMessage = message;
      this.type = type;
      this.link = link;
      this.autoClose = autoClose;
      this.showWarning = show;
    }
  },
  created() {
    this.$root.$on('show-validation-error', this.showSnack);
  },
  beforeDestroy() {
    this.$root.$off('show-validation-error', this.showSnack);
  }
};
</script>
