<template>
  <div id="app">
    <tool-bar
        :searchEnable="searchEnable"
        @search="searchByText"
        @searchQuestion="searchQuestion"
        :has-next="statePtr + 1 < states.length"
        :has-prev="statePtr > 0"
        @next="statePtr++"
        @prev="statePtr--"
        @globalView="(x) => {overlay = x}"
        @closeGlobalView="() => {overlay = ''}"
        @climateView="getClimateView"
        @oceanCurrentView="getOceanCurrentView"
        @reinit="reinit"
        :global-view="globalView"
    ></tool-bar>
    <global-map
        :selectEnable="searchEnable"
        :marker-lat="marker.markerLat"
        :marker-lng="marker.markerLng"
        :marker-show="marker.markerShow"
        :geo-json-show="poly.show"
        :geo-json="poly.data"
        :overlay="overlay"
        @mapSelect="searchByLocation"
    ></global-map>
    <side-bar
        :state="curr"
        ref="sidebar"
        @searchByText="searchByText"
        @setMarker="setMarker"
        @setPoly="setPoly"
        @clearPoly="clearPoly"
        @setLocation="setLocation"
        @searchQuestion="searchQuestion"
        @selectQuestion="selectQuestion"
        @globalView="(x) => {overlay = x}"
        @singleClimateView="getSingleClimateView"
        @searchPlaceByClimate="searchByText"
    ></side-bar>
  </div>
</template>

<script>
import GlobalMap from "@/components/GlobalMap";
import ToolBar from "@/components/ToolBar";
import SideBar from "@/components/SideBar";
import config from "@/utils/config";
import axios from "axios";
import Wkt from 'wicket';

const defaultState = {
  state: "pending",
  searchResult: [],
  location: {},
  questionResult: [],
  question: {},
  climates: {
    endpoint: '',
    type: '',
    data: []
  },
  single_climate: {}
}

export default {
  name: 'App',
  data() {
    return {
      // 10km spatial range
      spatialRange: 10,
      statePtr: 0,
      states: [
          Object.assign({}, defaultState, {state: "init"})
      ],
      marker: {
        markerLat: 0,
        markerLng: 150,
        markerShow: false
      },
      poly: {
        data: null,
        show: false
      },
      overlay: '',
    }
  },
  computed: {
    searchEnable() {
      return this.curr.state !== "pending"
    },
    curr() {
      return this.states[this.statePtr]
    },
    globalView() {
      return this.overlay !== ''
    }
  },
  watch: {
    statePtr() {
      this.$refs['sidebar'].visible = true;
    }
  },
  components: {
    SideBar,
    ToolBar,
    GlobalMap
  },
  methods: {
    newState() {
      this.states.splice(this.statePtr + 1)
      this.states.push(Object.assign({}, defaultState))
      this.statePtr++;
    },
    setMarker(lat, lng) {
      this.marker = Object.assign({}, this.marker, {markerLat: lat, markerLng: lng, markerShow: true})
    },
    setPoly(poly) {
      let wkt = new Wkt.Wkt();
      wkt.read(poly);
      this.poly = Object.assign({}, this.poly, {
        data: wkt.toJson(),
        show: true
      })
    },
    clearPoly() {
      this.poly = Object.assign({}, this.poly, {
        data: null,
        show: false
      })
    },
    setLocation(uri) {
      this.newState()
      axios.get(config.searchEndpoint + '/describe', {params: {uri}}).then(x => {
        console.log(x)
        this.curr.location = {uri, info: x.data.results.bindings[0]}
      }).catch(x => {
        this.$bvToast.toast(x.toString())
      }).finally(() => {
        this.curr.state = "location"
      })
    },
    searchByLocation(lat, lng) {
      this.newState()
      this.setMarker(lat, lng)
      axios.get(config.searchEndpoint + '/spatialQuery', {
        params: {
          lat,
          lng,
          dist: this.spatialRange
        }
      }).then(x => {
        this.curr.searchResult = x.data.results.bindings.map(
            y => {
              return {
                key: y["entity"]["value"],
                title: y["name"]["value"],
                dist: y["dist"]["value"],
                // eslint-disable-next-line no-prototype-builtins
                bcl: y.hasOwnProperty("bcl") ? y["bcl"]["value"] : undefined,
                // eslint-disable-next-line no-prototype-builtins
                b1l: y.hasOwnProperty("b1l") ? y["b1l"]["value"] : undefined,
                // eslint-disable-next-line no-prototype-builtins
                lat: y.hasOwnProperty("lat") ? y["lat"]["value"] : undefined,
                // eslint-disable-next-line no-prototype-builtins
                lng: y.hasOwnProperty("lng") ? y["lng"]["value"] : undefined,
              }
            }
        )
      }).catch(x => {
        this.$bvToast.toast(x.toString())
      }).finally(() => {
        this.curr.state = "search"
      })
    },
    searchByText(keyword) {
      this.newState()
      axios.get(config.searchEndpoint + '/keywordQuery', {
        params: {keyword}
      }).then(x => {
        this.curr.searchResult = x.data.results.bindings.map(
            y => {
              return {
                key: y["entity"]["value"],
                title: y["name"]["value"],
                // eslint-disable-next-line no-prototype-builtins
                bcl: y.hasOwnProperty("bcl") ? y["bcl"]["value"] : undefined,
                // eslint-disable-next-line no-prototype-builtins
                b1l: y.hasOwnProperty("b1l") ? y["b1l"]["value"] : undefined,
                // eslint-disable-next-line no-prototype-builtins
                lat: y.hasOwnProperty("lat") ? y["lat"]["value"] : undefined,
                // eslint-disable-next-line no-prototype-builtins
                lng: y.hasOwnProperty("lng") ? y["lng"]["value"] : undefined,
              }
            }
        )
      }).catch(x => {
        this.$bvToast.toast(x.toString())
      }).finally(() => {
        this.curr.state = "search"
      })
    },
    searchQuestion(keyword) {
      this.newState()
      axios.get(config.searchEndpoint + '/search_problem', {params: {'q': keyword}}).then(x => {
        this.curr.questionResult = x.data
      }).catch(x => {
        this.$bvToast.toast(x.toString())
      }).finally(() => {
        this.curr.state = "search_question"
      })
    },
    selectQuestion(id) {
      this.newState()
      axios.get(config.searchEndpoint + '/problem/' + id).then(x => {
        this.curr.question = x.data
      }).catch(x => {
        this.$bvToast.toast(x.toString())
      }).finally(() => {
        this.curr.state = "single_question"
      })
    },
    getClimateView() {
      this.newState()
      axios.get(config.searchEndpoint + '/getAllClimates').then(x => {
        this.curr.climates = Object.assign({}, this.curr.climates, {
          endpoint: 'climate',
          type: "气候类型视图",
          data: x.data.results.bindings.map(y => ({name: y.name.value}))
        })
      }).catch(x => {
        this.$bvToast.toast(x.toString())
      }).finally(() => {
        this.curr.state = "climate_view"
      })
    },
    getOceanCurrentView() {
      this.newState()
      axios.get(config.searchEndpoint + '/getAllOceanCurrents').then(x => {
        this.curr.climates = Object.assign({}, this.curr.climates, {
          endpoint: 'ocean_current',
          type: "洋流视图",
          data: x.data.results.bindings.map(y => ({name: y.name.value}))
        })
      }).catch(x => {
        this.$bvToast.toast(x.toString())
      }).finally(() => {
        this.curr.state = "climate_view"
      })
    },
    // eslint-disable-next-line no-unused-vars
    getSingleClimateView(name, endpoint) {
      this.newState()
      axios.get(config.searchEndpoint + '/auxPolygonQuery', {params: {text: name}}).then(x => {
        if(x.data.results.bindings.length !== 1) {
          throw new Error('Internal Error, climate size not match')
        }
        this.setPoly(x.data.results.bindings[0].poly.value)
        this.marker.markerShow = false
        this.curr.single_climate = {
          'name': x.data.results.bindings[0].name.value,
          'description': x.data.results.bindings[0].description.value
        }
      }).catch(x => {
        this.$bvToast.toast(x.toString())
      }).finally(() => {
        this.curr.state = "single_climate_view"
      })
    },
    reinit() {
      this.newState()
      this.curr.state = "init"
    }
  }
}
</script>