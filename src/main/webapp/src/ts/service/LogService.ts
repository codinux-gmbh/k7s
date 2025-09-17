import { Constants } from "./Constants"

export class LogService {

    error(...data: any[]) {
        console.error(...data)
    }

    warn(...data: any[]) {
        console.warn(...data)
    }

    info(...data: any[]) {
        console.log(...data)
    }

    debug(...data: any[]) {
        if (Constants.isDev) {
            console.debug(...data)
        }
    }

}