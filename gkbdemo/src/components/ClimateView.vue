<template>
  <div>
    <h2 class="mb-3">{{ data.type }}</h2>
    <ul v-for="x in data.data" :key="x.name">
      <li><b-link @click="$emit('singleClimateView', x.name, data.endpoint)">{{x.name}}</b-link></li>
    </ul>
  </div>
</template>

<script>
export default {
  name: "ClimateView",
  props: ["data"]
}
</script>

<style scoped>

</style>