<template>
  <l-map
      :zoom="zoom"
      :center="center"
      style="height: 800px; width: 100%"
      @click="clickMap"
  >
    <l-tile-layer
        :url="url"
        :attribution="attribution"
    />
    <l-tile-layer
        v-if="overlay !== ''"
        :opacity="0.7"
        :url="overlay"
    />
    <l-marker :visible="markerShow"
        :lat-lng="markerPosition"
        :icon="icon"
    >
    </l-marker>
    <l-geo-json :visible="geoJsonShow" :geojson="geoJson"></l-geo-json>
  </l-map>
</template>

<script>
import { latLng, icon, Icon } from "leaflet";
import iconImage from 'leaflet/dist/images/marker-icon.png';
import iconImage2x from 'leaflet/dist/images/marker-icon-2x.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

Icon.Default.prototype.options.iconUrl = iconImage

import {
  LMap,
  LTileLayer,
  LMarker,
  LGeoJson
} from "vue2-leaflet";

export default {
  name: "GlobalMap",
  props: [
      "selectEnable",
      "markerLat",
      "markerLng",
      "markerShow",
      "geoJsonShow",
      "geoJson",
      "overlay"
  ],
  components: {
    LMap,
    LTileLayer,
    LGeoJson,
    LMarker
  },
  watch: {
    markerLat() {
      this.center = [this.markerLat, this.markerLng]
    },
    markerLng() {
      this.center = [this.markerLat, this.markerLng]
    }
  },
  computed: {
    markerPosition () {
      return latLng(this.markerLat, this.markerLng)
    }
  },
  data() {
    return {
      zoom: 2,
      center: [0.0, 0.0],
      icon: icon({
        iconUrl: iconImage,
        iconRetinaUrl: iconImage2x,
        shadowUrl:     iconShadow,
        iconSize:    [25, 41],
        iconAnchor:  [12, 41],
        popupAnchor: [1, -34],
        tooltipAnchor: [16, -28],
        shadowSize:  [41, 41]
      }),
      marker: {
        show: false
      },
      circle: {
        center: latLng(47.41322, -1.0482),
        radius: 4500
      },
      rectangle: {
        bounds: [[47.341456, -1.397133], [47.303901, -1.243813]],
        style: { color: "red", weight: 5 }
      },
      polygon: {
        latlngs: [
          [47.2263299, -1.6222],
          [47.21024000000001, -1.6270065],
          [47.1969447, -1.6136169],
          [47.18527929999999, -1.6143036],
          [47.1794457, -1.6098404],
          [47.1775788, -1.5985107],
          [47.1676598, -1.5753365],
          [47.1593731, -1.5521622],
          [47.1593731, -1.5319061],
          [47.1722111, -1.5143967],
          [47.1960115, -1.4841843],
          [47.2095404, -1.4848709],
          [47.2291277, -1.4683914],
          [47.2533687, -1.5116501],
          [47.2577961, -1.5531921],
          [47.26828069, -1.5621185],
          [47.2657179, -1.589241],
          [47.2589612, -1.6204834],
          [47.237287, -1.6266632],
          [47.2263299, -1.6222]
        ],
        color: "#ff00ff"
      },
      polyline: {
        latlngs: [
          [47.334852, -1.509485],
          [47.342596, -1.328731],
          [47.241487, -1.190568],
          [47.234787, -1.358337]
        ],
        color: "green"
      },
      url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      attribution:
          '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    };
  },
  methods: {
    setCenter(lat, lng) {
      this.center = [lat, lng]
    },
    clickMap(e) {
      if(!this.selectEnable) return;
      let latlng = e.latlng.wrap()
      let lat = latlng.lat
      let lng = latlng.lng
      this.setCenter(lat, lng)
      this.$emit("mapSelect", lat, lng)
    },
    clickBtn() {
      this.rectangle.style.weight++;
      this.rectangle.style.color =
          this.rectangle.style.weight % 2 === 0 ? "blue" : "green";
    }
  }
};
</script>