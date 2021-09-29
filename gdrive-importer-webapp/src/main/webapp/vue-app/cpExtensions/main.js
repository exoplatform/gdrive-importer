import './initComponents.js';
import {initExtensions} from './extensions.js';

const appId = 'copy-gdrive-application';

if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('test');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

Vue.use(Vuetify);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);
//getting language of the PLF
const lang = eXo && eXo.env.portal.language || 'en';
//should expose the locale ressources as REST API
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.gdriveimporter.gdrive_importer-${lang}.json`;

export function init() {
  initExtensions();
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    new Vue({
      template: `<cp-gdrive id="${appId}" />`,
      vuetify,
      i18n
    }).$mount(`#${appId}`);
  });
}
