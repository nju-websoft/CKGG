<script>
export default {
  name: "NerShow",
  props: ['text', 'ner'],
  methods: {
    searchLoc(text) {
      console.log("search " + text)
      this.$emit("searchByText", text)
    }
  },
  render(h) {
    let elements = []
    let last = ''
    let nercur = 0
    for(let i = 0; i < this.text.length; i++) {
      if(nercur < this.ner.length && this.ner[nercur][0] === i) {
        if(last !== '') {
          elements.push(last)
          last = ''
        }
        i = this.ner[nercur][1] - 1
        let substr = this.text.substring(this.ner[nercur][0], this.ner[nercur][1])
        elements.push(h(
            'a', {
              attrs: {
                href: '#'
              },
              on: {
                click: () => this.searchLoc(substr)
              }
            }, substr
        ))
        nercur++
      } else {
        last += this.text.charAt(i)
      }
    }
    if(last !== '') {
      elements.push(last)
      last = ''
    }
    console.log(elements)
    return h(
        'span', elements
    )
  }
}
</script>

<style scoped>
</style>