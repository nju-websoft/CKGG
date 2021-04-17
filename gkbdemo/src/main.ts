import Vue from 'vue'
import { BootstrapVue, BootstrapVueIcons } from "bootstrap-vue";

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import 'leaflet/dist/leaflet.css';

Vue.use(BootstrapVue)
Vue.use(BootstrapVueIcons)

import App from './App.vue'

Vue.config.productionTip = false

new Vue({
  render: h => h(App),
}).$mount('#app')
