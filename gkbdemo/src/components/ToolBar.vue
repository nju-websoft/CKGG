<template>
  <b-navbar>
    <b-navbar-brand href="#" @click="$emit('reinit')">CKGG</b-navbar-brand>
    <b-navbar-nav>
      <b-nav-form @submit.prevent="search">
        <b-form-input size="md" class="mr-md-2" placeholder="Search" v-model="keyword" :disabled="!searchEnable"></b-form-input>
        <b-button size="md" class="my-2 mx-2" variant="info" type="submit" :disabled="!searchEnable || keyword === ''"><b-icon-search></b-icon-search> 地点</b-button>
        <b-button size="md" class="my-2 mx-2" variant="primary" @click.prevent="searchProblem" :disabled="!searchEnable || keyword === ''"><b-icon-search></b-icon-search> 题目</b-button>
      </b-nav-form>
    </b-navbar-nav>
    <b-navbar-nav>
      <b-nav-item-dropdown text="全局视图">
        <b-dropdown-item @click="showClimate">气候类型分布</b-dropdown-item>
        <b-dropdown-item @click="showOceanCurrent">洋流分布</b-dropdown-item>
        <b-dropdown-divider></b-dropdown-divider>
        <b-dropdown-item @click="$emit('reinit')">欢迎界面</b-dropdown-item>
      </b-nav-item-dropdown>
    </b-navbar-nav>
    <b-navbar-nav>
      <b-nav-item v-if="globalView" @click="$emit('closeGlobalView')"><b-btn variant="outline-danger">关闭全局视图</b-btn></b-nav-item>
    </b-navbar-nav>
    <b-navbar-nav class="ml-2">
      <b-nav-item :disabled="!hasPrev" @click="$emit('prev')">
        <b-icon-arrow-left-circle-fill font-scale="1.8"></b-icon-arrow-left-circle-fill>
      </b-nav-item>
      <b-nav-item :disabled="!hasNext" @click="$emit('next')">
        <b-icon-arrow-right-circle-fill font-scale="1.8"></b-icon-arrow-right-circle-fill>
      </b-nav-item>
    </b-navbar-nav>
  </b-navbar>
</template>

<script>
export default {
  name: "ToolBar",
  props: [
      "searchEnable",
      "hasNext",
      "hasPrev",
      "globalView"
  ],
  data() {
    return {
      keyword: ""
    }
  },
  methods: {
    search () {
      this.$emit("search", this.keyword)
    },
    searchProblem () {
      this.$emit("searchQuestion", this.keyword)
    },
    showClimate() {
      this.$emit('climateView')
    },
    showOceanCurrent() {
      this.$emit('oceanCurrentView')
    }
  }
}
</script>

<style scoped>

</style>