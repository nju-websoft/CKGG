<script>
import { Bar, mixins } from 'vue-chartjs'
const { reactiveProp } = mixins

const chartOptions = {
  title: {
    display: true,
    text: '气候图'
  },
  scales: {
    yAxes: [{
      id: 'precip',
      type: 'linear',
      scaleLabel: {
        display: true,
        labelString: '降水量 (mm)',
      },
      position: 'left',
      gridLines: {
        display: false
      }
    }, {
      id: 'temp',
      type: 'linear',
      scaleLabel: {
        display: true,
        labelString: '温度 (℃)',
      },
      position: 'right',
      gridLines: {
        display: false
      }
    }]
  }
}

export default {
  name: "PtChart",
  extends: Bar,
  mixins: [reactiveProp],
  props: [
    'options'
  ],
  mounted () {
    this.renderChart(this.chartData, chartOptions)
  }
}
</script>

<style scoped>

</style>