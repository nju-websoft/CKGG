<template>
  <div>
  <b-button id="expand" v-b-toggle.sidebar size="sm" style="height: 60%; font-weight: bolder">&lt;</b-button>
  <b-sidebar id="sidebar" v-model="visible" right shadow="sm" width="33vw">
    <div v-if="state.state === 'pending'">
      <b-spinner class="mx-auto d-block"></b-spinner>
      <h3 style="text-align: center">Loading (takes &lt;30s)...</h3>
    </div>
    <div class="px-3 py-2" v-else-if="state.state === 'location'">
      <location-view
          :uri="state.location.uri"
          :info="state.location.info"
          v-on="$listeners"
      ></location-view>
    </div>
    <div class="px-3 py-2" v-else-if="state.state === 'search'">
      <search-view
          :result="state.searchResult"
          v-on="$listeners"
      ></search-view>
    </div>
    <div class="px-3 py-2" v-else-if="state.state === 'search_question'">
      <question-view
          :result="state.questionResult"
          v-on="$listeners"
      ></question-view>
    </div>
    <div class="px-3 py-2" v-else-if="state.state === 'single_question'">
      <single-question-view
          :data="state.question"
          v-on="$listeners"
      ></single-question-view>
    </div>
    <div class="px-3 py-2" v-else-if="state.state === 'climate_view'">
      <climate-view
          :data="state.climates"
          v-on="$listeners"
      ></climate-view>
    </div>
    <div class="px-3 py-2" v-else-if="state.state === 'single_climate_view'">
      <single-climate-view
          :data="state.single_climate"
          v-on="$listeners"
      ></single-climate-view>
    </div>
    <div class="px-3 py-2" v-else>
<!--      <h2>CKGG: A Chinese Knowledge Graph for High-School Geography Education and Beyond</h2>-->
<!--      <img src="@/assets/figure1.png" width="100%">-->
      <h3>城市</h3>
      <ul>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/414710')">北京</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/1291271')">旧金山</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/1393135')">伦敦</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/2287406')">伊斯坦布尔</b-link></li>
      </ul>
      <h3>国家和行政区</h3>
      <ul>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/2834351')">中国</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/2983160')">德国</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/2944777')">尼日利亚</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/390400')">古吉拉特邦</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/336585')">江苏省</b-link></li>
      </ul>
      <h3>大洲和大洋</h3>
      <ul>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/1201744')">亚洲</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/1198540')">欧洲</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/2067788')">印度洋</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/1446532')">大西洋</b-link></li>
      </ul>
      <h3>其他</h3>
      <ul>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/870986')">塞浦路斯岛</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/7431525')">华北平原</b-link></li>
        <li><b-link @click="$emit('setLocation', 'http://w3id.org/ckgg/1.0/instances/location/720313')">珠穆朗玛峰</b-link></li>
      </ul>
      <h3>例题</h3>
      <ul>
        <li><b-link @click="$emit('selectQuestion', 12518)">下图示意某地区某月等温线分布...</b-link></li>
        <li><b-link @click="$emit('selectQuestion', 41292)">下图为江苏省第一个国家级新区...</b-link></li>
      </ul>
      <h3>版权信息</h3>
      <ul>
        <li>数据源
          <a href="http://geonames.org/">GeoNames</a>,
          <a href="http://wikidata.org/">Wikidata</a>,
          <a href="http://naturalearthdata.com/">Natural Earth</a>,
          <a href="http://dbpedia.org/">DBpedia</a>,
          <a href="http://globalsolaratlas.info/">Global Solar Atlas</a>,
          <a href="http://wikipedia.org/">Wikipedia</a>,
          <a href="http://berkeleyearth.org/">Berkeley Earth</a>,
          <a href="http://disc.gsfc.nasa.gov/">GES DISC</a>,
          <a href="http://www.stats.gov.cn/">National Bureau of Statistics of China</a>
        </li>
        <li>地图数据源 &copy; <a href="http://osm.org/copyright">OpenStreetMap</a>
<!--          <a href="https://www.mapbox.com/">MapBox</a>,-->
<!--          <a href="https://sedac.ciesin.columbia.edu/data/collection/gpw-v4/sets/browse">SEDAC</a>-->
        </li>
      </ul>
    </div>
  </b-sidebar>
  </div>
</template>

<script>
import LocationView from "@/components/LocationView";
import SearchView from "@/components/SearchView";
import QuestionView from "@/components/QuestionView";
import SingleQuestionView from "@/components/SingleQuestionView";
import ClimateView from "@/components/ClimateView";
import SingleClimateView from "@/components/SingleClimateView";
export default {
  name: "SideBar",
  components: {SingleClimateView, ClimateView, SingleQuestionView, QuestionView, SearchView, LocationView},
  props: [
      "state"
  ],
  data() {
    return {
      visible: true
    }
  }
}
</script>

<style scoped>
#expand {
  position: absolute;
  right: 0;
  top: 0;
  transform: translateY(calc(50vh - 50%));
  z-index: 999;
}
</style>