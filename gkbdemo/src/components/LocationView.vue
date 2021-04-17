<template>
  <div>
    <b-card>
      <b-card-title>{{info["name"]["value"]}}</b-card-title>
      <table class="table table-striped">
        <tbody>
        <tr v-for="(x, i) in filteredNames" :key="i">
          <th class="pr-4" width="33%">{{x["cn"]}}</th>
<!--          <th class="pr-4" width="33%">({{x["en"]}})</th>-->
          <td width="67%">
            <div v-if="x.multiple">
              <div v-for="(entry, i) in zipMulti(info[x.col]['value'], x.cola === null ? null : info[x.cola]['value'])" :key="i">
                <b-link v-if="x.col === 'oc'" href="#" @click="$emit('singleClimateView', entry.value, 'ocean_current')">{{x.parse(entry.value)}}</b-link>
                <b-link v-else-if="x.col === 'cl'" href="#" @click="$emit('singleClimateView', entry.value, 'climate')">{{x.parse(entry.value)}}</b-link>
                <b-link v-else-if="x.col === 'wikilink' || x.col === 'sameAs'" :href="entry.value" target="_blank">{{x.parse(entry.value)}}</b-link>
                <b-link v-else-if="x.col === 'all_coord'" href="#" @click="goto_coord(entry.value)"><b-btn variant="outline-primary" class="mb-2"><b-icon-geo-alt></b-icon-geo-alt>{{x.parse(entry.value)}}</b-btn></b-link>
                <b-link v-else-if="entry.link !== null" href="#" @click="$emit('setLocation', entry.link)">{{x.parse(entry.value)}}</b-link>
                <span v-else>{{x.parse(entry.value)}}</span>
              </div>
            </div>
            <div v-else>
              <b-link v-if="x.cola !== null" href="#" @click="$emit('setLocation', info[x.cola]['value'])">{{x.parse(info[x["col"]]["value"])}}</b-link>
              <b-btn variant="outline-secondary" size="sm" v-else-if="x.hide === true" href="#" @click="() => copy(info[x.col]['value'])"><b-icon-code></b-icon-code> Copy</b-btn>
              <span v-else>{{x.parse(info[x["col"]]["value"])}}</span>
<!--              <b-link v-if="x.col === 'alt'" style="float: right;" class="pr-2" @click="showAltMap">显示海拔高度图</b-link>-->
<!--              <b-link v-if="x.col === 'pop'" style="float: right;" class="pr-2" @click="showPopDensMap">显示人口密度图</b-link>-->
<!--              <b-link v-if="x.col === 'solar'" style="float: right;" class="pr-2" @click="showSolarMap">显示太阳辐射量图</b-link>-->
            </div>
          </td>
        </tr>
        </tbody>
      </table>
      <div class="pt-2">
        <pt-chart :chart-data="chartData"></pt-chart>
      </div>
      <div class="d-flex flex-row pt-2">
        <b-btn variant="outline-secondary" class="mx-auto d-block text-center" @click="setMarkerAndPoly"><b-icon-geo-alt></b-icon-geo-alt> 定位到地点</b-btn>
        <b-btn variant="outline-secondary" class="mx-auto d-block text-center" @click="searchQuestion"><b-icon-search></b-icon-search> 相关题目</b-btn>
        <b-btn variant="outline-secondary" class="mx-auto d-block text-center" :href="uri" target="_blank"><b-icon-link></b-icon-link> 知识库链接</b-btn>
<!--        <b-btn variant="outline-secondary" class="mx-auto d-block text-center" :href="longUri" target="_blank"><b-icon-code></b-icon-code> FCT链接</b-btn>-->
      </div>
    </b-card>
  </div>
</template>

<script>
import PtChart from "@/components/PtChart";
import Wkt from 'wicket';

const displayNames = [
  // {"cn": "所有位置(debug)", "en": "", "col": "all_coord", "cola": null, "hide": false, "multiple": true, parse: (x) => x},
  {"cn": "名称", "en": "Longitude", "col": "allname", "cola": null, "hide": false, "multiple": true, parse: (x) => x},
  {"cn": "类型", "en": "Type", "col": "alltype", "cola": null, "hide": false, "multiple": true, parse: (x) => x},
  {"cn": "经度", "en": "Longitude", "col": "lon", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "纬度", "en": "Latitude", "col": "lat", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "Wikipedia 链接", "en": "", "col": "wikilink", "cola": null, "hide": false, "multiple": true, parse: (x) => x},
  {"cn": "相同实体", "en": "", "col": "sameAs", "cola": null, "hide": false, "multiple": true, parse: (x) => x},
  {"cn": "GeoNames 类型", "en": "", "col": "gncls", "cola": null, "hide": false, "multiple": true, parse: (x) => x},
  {"cn": "重要性分数", "en": "", "col": "rankscore", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  // {"cn": "面积(㎡)", "en": "Area", "col": "area", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "距海距离(m)", "en": "Distance To Ocean", "col": "toocean", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "范围(wkt)", "en": "Polygon", "col": "poly", "cola": null, "hide": true, "multiple": false, parse: (x) => x},
  {"cn": "海拔高度(m)", "en": "Altitude", "col": "alt", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "人口总量", "en": "Population", "col": "pop", "cola": null, "hide": false, "multiple": false, parse: (x) => parseInt(x)},
  // {"cn": "人口密度", "en": "Population", "col": "popdens", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  // {"cn": "城市人口数量", "en": "Population", "col": "urbanpop", "cola": null, "hide": false, "multiple": false, parse: (x) => parseInt(x)},
  {"cn": "行政区代码", "en": "Admin Code", "col": "code", "cola": null, "hide": false, "multiple": true, parse: (x) => x},
  // {"cn": "货币", "en": "Currency", "col": "cur", "cola": null, "hide": false, "multiple": true, parse: (x) => x},
  {"cn": "所属国家或地区", "en": "Country", "col": "bcl", "cola": "bc", "hide": false, "multiple": false, parse: (x) => x},
  {"cn": "所属一级行政区", "en": "Admin 1", "col": "b1l", "cola": "b1", "hide": false, "multiple": false, parse: (x) => x},
  {"cn": "所属二级行政区", "en": "Admin 2", "col": "b2l", "cola": "b2", "hide": false, "multiple": false, parse: (x) => x},
  {"cn": "所属三级行政区", "en": "Admin 3", "col": "b3l", "cola": "b3", "hide": false, "multiple": false, parse: (x) => x},
  {"cn": "所属四级行政区", "en": "Admin 4", "col": "b4l", "cola": "b4", "hide": false, "multiple": false, parse: (x) => x},
  {"cn": "属于", "en": "Belongs to", "col": "allupperl", "cola": "allupper", "hide": false, "multiple": true, parse: (x) => x},
  {"cn": "时区", "en": "Time Zone", "col": "tzv", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "年太阳辐射量(kWh/㎡)", "en": "Annual Solar Radiation", "col": "solar", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "受洋流影响", "en": "Influenced by ocean current", "col": "oc", "cola": null, hide: false, "multiple": true, parse: (x) => x},
  {"cn": "气候类型", "en": "Climate type", "col": "cl", "cola": null, hide: false, "multiple": true, parse: (x) => x},
  {"cn": "人口性别比", "en": "", "col": "P755", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "单位产值能耗", "en": "", "col": "P175", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "粮食产量", "en": "", "col": "P128", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "植被覆盖率", "en": "", "col": "P101", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "失业率", "en": "", "col": "P158", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "死亡率", "en": "", "col": "P750", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "出生率", "en": "", "col": "P749", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "地区生产总值", "en": "", "col": "P118", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "自然增长率", "en": "", "col": "P751", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)},
  {"cn": "人均生产总值", "en": "", "col": "P117", "cola": null, "hide": false, "multiple": false, parse: (x) => parseFloat(x)}
]

export default {
  name: "LocationView",
  components: {PtChart},
  props: ["info", "uri"],
  data() {
    return {
      precip: Array(12).fill(NaN),
      temp: Array(12).fill(NaN),
      displayNames
    }
  },
  computed: {
    filteredNames () {
      // eslint-disable-next-line no-prototype-builtins
      return displayNames.filter(x => this.info.hasOwnProperty(x.col) && this.info[x.col].value !== '')
    },
    chartData() {
      return {
        labels: [...Array(12).keys()].map(x => x + 1),
        datasets: [
          {
            type: 'bar',
            label: '降水量',
            data: this.precip,
            yAxisID: 'precip',
            backgroundColor: "rgba(2, 117, 216, 0.5)"
          },
          {
            type: 'line',
            label: '气温',
            data: this.temp,
            yAxisID: 'temp',
            fill: false,
            borderColor: "rgba(217, 83, 79, 0.5)"
          }
        ]
      }
    },
    longUri() {
      return 'http://simba:8892/describe/?url=' + encodeURI(this.uri)
    }
  },
  watch: {
    info: {
      immediate: true,
      handler() {
        this.getMonthlyPrecip()
        this.getMonthlyTemp()
      }
    }
  },
  methods: {
    setMarkerAndPoly() {
      // eslint-disable-next-line no-prototype-builtins
      if(this.info.hasOwnProperty("lat") && this.info.hasOwnProperty("lon")) {
        this.$emit('setMarker', this.info["lat"]["value"], this.info["lon"]["value"])
        // eslint-disable-next-line no-prototype-builtins
        if(this.info.hasOwnProperty("poly")) {
          this.$emit('setPoly', this.info["poly"]["value"])
        } else {
          this.$emit('clearPoly')
        }
      }
    },
    goto_coord(coord) {
      let wkt = new Wkt.Wkt()
      console.log(wkt.read(coord))
      this.$emit('setMarker', wkt.components[0].y, wkt.components[0].x)
    },
    searchQuestion() {
      console.log(this.info["keyword"]["value"])
      this.$emit('searchQuestion', this.info["keyword"]["value"])
    },
    copy(s) {
      navigator.clipboard.writeText(s).then(() => this.$bvToast.toast(`Copied ${s.length} characters`))
    },
    zipMulti(names, links) {
      let namesarr = names.split("\t")
      let ret = []
      if(links !== null) {
        let linksarr = links.split("\t")
        for(let i = 0; i < namesarr.length; i++) {
          ret.push({
            "value": namesarr[i],
            "link": linksarr[i]
          })
        }
      } else {
        for(let i = 0; i < namesarr.length; i++) {
          ret.push({
            "value": namesarr[i],
            "link": null
          })
        }
      }
      return ret
    },
    getMonthlyPrecip() {
      let monthArr = this.info['pm']['value'].split('\t')
      let valueArr = this.info['pv']['value'].split('\t')
      let ret = Array(12).fill(NaN)
      for(let i = 0; i < monthArr.length; i++) {
        let month = monthArr[i]
        if(month >= 1 && month <= 12) {
          ret[parseInt(month) - 1] = parseFloat(valueArr[i]) * 1000
        }
      }
      this.precip = ret
    },
    getMonthlyTemp() {
      let monthArr = this.info['tm']['value'].split('\t')
      let valueArr = this.info['tv']['value'].split('\t')
      let ret = Array(12).fill(NaN)
      for(let i = 0; i < monthArr.length; i++) {
        let month = monthArr[i]
        if(month >= 1 && month <= 12) {
          ret[parseInt(month) - 1] = parseFloat(valueArr[i])
        }
      }
      this.temp = ret
    }
  },
  mounted() {
    this.setMarkerAndPoly()
  }
}
</script>

<style scoped>

</style>