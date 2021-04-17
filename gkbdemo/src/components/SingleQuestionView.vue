<template>
  <div>
    <h2><ner-show v-on="$listeners" :text="data['background']" :ner="data['background_ner']"></ner-show></h2>
    <ul class="pl-0" v-for="(x, i) in data.multiplechoice" :key="i">
      <b-card>
        <h4><ner-show v-on="$listeners" :text="x['question']" :ner="x['question_ner']"></ner-show></h4>
        <p class="choice">A: <ner-show v-on="$listeners" :text="x['choice_A']" :ner="x['choice_A_ner']"></ner-show></p>
        <p class="choice">B: <ner-show v-on="$listeners" :text="x['choice_B']" :ner="x['choice_B_ner']"></ner-show></p>
        <p class="choice">C: <ner-show v-on="$listeners" :text="x['choice_C']" :ner="x['choice_C_ner']"></ner-show></p>
        <p class="choice">D: <ner-show v-on="$listeners" :text="x['choice_D']" :ner="x['choice_D_ner']"></ner-show></p>
        <h5 class="choice">答案: {{x['answer']}}</h5>
        <b-button block variant="info" v-b-toggle="'explains_' + i">显示/隐藏解析</b-button>
        <b-collapse :id="'explains_' + i">
          <h5 class="choice mt-3">解析: <ner-show v-on="$listeners" :text="x['explains']" :ner="x['explains_ner']"></ner-show></h5>
        </b-collapse>
      </b-card>
    </ul>
  </div>
</template>

<script>
import NerShow from "@/components/NerShow";
export default {
  name: "SingleQuestionView",
  components: {NerShow},
  props: ['data'],
}
</script>
