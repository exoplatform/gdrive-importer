import CopyGdrive from './components/CopyGdrive.vue';
import CustomSnackbar from './components/CustomSnackbar.vue';
const components = {
  'cp-gdrive': CopyGdrive,
  'custom-snackbar': CustomSnackbar
};

for (const key in components) {
  Vue.component(key, components[key]);
}