import CopyGdrive from './components/CopyGdrive.vue';

const components = {
  'cp-gdrive': CopyGdrive
};

for (const key in components) {
  Vue.component(key, components[key]);
}