module.exports = {
    publicPath: process.env.NODE_ENV === 'production'
        ? 'http://ws.nju.edu.cn/CKGG/1.0/demo/'
        : '/'
}