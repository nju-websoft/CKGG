<template>
  <div>
    <ul class="pl-0" v-if="result.length > 0">
      <b-card
          v-for="(x, i) in result"
          :key="x.key"
      >
        <b-card-title>
          <b-link href="#" @click="selectLocation(x)">{{x.title}} (#{{i + 1}})</b-link>
          <b-link
              v-if="x.hasOwnProperty('lat') && x.hasOwnProperty('lng') && x['lat'] !== undefined && x['lng'] !== undefined"
              style="float: right" variant="outline-success" @click="() => {$emit('setMarker', x.lat, x.lng);$emit('clearPoly');}">
            <b-icon-geo-alt></b-icon-geo-alt>
          </b-link>
        </b-card-title>
        <b-card-sub-title>
          <div>
            <span v-if="x.hasOwnProperty('b1l') && x['b1l'] !== undefined">{{x.b1l}}, </span>
            <span v-if="x.hasOwnProperty('bcl') && x['b1l'] !== undefined">{{x.bcl}}</span>
          </div>
          <div>URI: <b-link :href="x.key">{{x.key}}</b-link></div>
          <div v-if="x.hasOwnProperty('dist')">距离: {{x.dist}}&nbsp;km</div>
        </b-card-sub-title>
      </b-card>
    </ul>
    <h2 v-else>No results</h2>
  </div>
</template>

<script>
export default {
  name: "SearchView",
  props: ["result"],
  data () {
    return {
      hasObjLoading: false
    }
  },
  methods: {
    selectLocation (obj) {
      this.$emit("setLocation", obj.key)
    }
  }
}
</script>

<style scoped>

</style>