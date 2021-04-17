<template>
  <div>
    <h2>{{data.name}}</h2>
    <p>{{data.description}}</p>
    <b-btn block class="mx-auto d-block text-center" @click="searchPlaceByClimate" variant="outline-success"><b-icon-geo-alt></b-icon-geo-alt> 具有{{data.name}}的地点</b-btn>
    <b-btn block class="mx-auto d-block text-center" @click="searchQuestion" variant="outline-info"><b-icon-search></b-icon-search> 相关题目</b-btn>
  </div>
</template>

<script>
export default {
name: "SingleClimateView",
  props: ['data'],
  methods: {
    searchQuestion() {
      this.$emit('searchQuestion', this.data.name)
    },
    searchPlaceByClimate() {
      this.$emit('searchPlaceByClimate', this.data.name)
    }
  }
}
</script>

<style scoped>

</style>