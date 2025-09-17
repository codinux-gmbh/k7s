export class WebRequest {
    constructor(
        readonly url: string,
        readonly body: any | null = null,

        readonly contentType: string | null = null,
        readonly accept: string | null = null,
        readonly headers: Map<string, string> = new Map(),
        readonly userAgent: string = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36",

        readonly abortController: AbortController | null = null,
    ) { }
}