export function progressHandle(event, file) {

    /*验证数据*/

    if (file.lastTime === 0 || file.lastTime === undefined) {

        file.lastTime = new Date().getTime()

        file.lastSize = event.loaded
        file.firstGetTime = true
        return
    }
    file.firstGetTime = false
    /*计算间隔*/

    let nowTime = new Date().getTime()
    let intervalTime = (nowTime - file.lastTime) / 1000 // 时间单位为毫秒，需转化为秒
    let intervalSize = event.loaded - file.lastSize
    /*重新赋值以便于下次计算*/

    file.lastTime = nowTime

    file.lastSize = event.loaded


    /*计算速度*/

    let speed = intervalSize / intervalTime

    let bSpeed = speed // 保存以b/s为单位的速度值，方便计算剩余时间

    let units = 'b/s' // 单位名称

    if (speed / 1024 > 1) {

        speed = speed / 1024

        units = 'k/s'

    }

    if (speed / 1024 > 1) {

        speed = speed / 1024

        units = 'M/s'

    }

    /*计算剩余时间*/

    let seconds = ((event.total - event.loaded) / bSpeed)
    let hour = Math.floor(seconds / 3600) >= 10 ? Math.floor(seconds / 3600) : '0' + Math.floor(seconds / 3600);
    seconds -= 3600 * hour;
    let min = Math.floor(seconds / 60) >= 10 ? Math.floor(seconds / 60) : '0' + Math.floor(seconds / 60);
    seconds -= 60 * min;
    let sec = Math.round(seconds) >= 10 ? Math.round(seconds) : '0' + Math.round(seconds);

    let time = hour + ":" + min + ":" + sec

    /*计算进度*/

    let progress = event.loaded / event.total * 100

    // console.log('当前进度：' + progress.toFixed(1)
    //     + '%    当前速度：' + speed.toFixed(1) + units
    //     + '   预计剩余时间：' + time)
    return [progress.toFixed(1) + "", speed.toFixed(1) + units + "", time + ""]
}


export function transform(size) {

    let units = 'B' // 单位名称

    if (size / 1024 > 1) {

        size = size / 1024

        units = 'KB'

    }

    if (size / 1024 > 1) {

        size = size / 1024

        units = 'MB'

    }

    if (size / 1024 > 1) {

        size = size / 1024

        units = 'GB'

    }

    if (size / 1024 > 1) {

        size = size / 1024

        units = 'TB'
    }
    size = Math.floor(size * 100) / 100
    return size + units
}
