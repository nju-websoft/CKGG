<template>
  <div>
    <ul class="pl-0" v-if="result.length > 0">
      <b-card
          v-for="x in result"
          :key="x.id"
      >
        <b-card-title><b-link class="question-title" href="#" @click="selectQuestion(x.id)">{{x.title}}</b-link></b-card-title>
        <b-card-sub-title>ID: {{x.id}}</b-card-sub-title>
        <b-card-text>
          {{x.content}}
        </b-card-text>
      </b-card>
    </ul>
    <h2 v-else>无结果</h2>
  </div>
</template>

<script>
export default {
  name: "QuestionView",
  props: ['result'],
  methods: {
    selectQuestion(id) {
      this.$emit('selectQuestion', id)
    }
  }
}
</script>

<style scoped>
.question-title {
  max-height: 2em;
  text-overflow: ellipsis;
}
</style>