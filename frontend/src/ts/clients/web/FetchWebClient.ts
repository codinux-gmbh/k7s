import type { WebClient } from "./WebClient"
import { WebRequest } from "./WebRequest"
import {LogService} from "../../service/LogService"
import {DI} from "../../service/DI"
import {HttpError} from "./HttpError"

export class FetchWebClient implements WebClient {

    constructor(private readonly baseUrl: String | null = null,
                private readonly log: LogService = DI.log,
                private readonly userAgent: string | null = null) {
    }


    async get<T>(request: string | WebRequest): Promise<T> {
        if (typeof request == "string") {
            return this.get(new WebRequest(request))
        } else {
            return this.fetch("GET", request)
        }
    }

    async post<T>(request: WebRequest): Promise<T> {
        return this.fetch("POST", request)
    }

    async delete<T>(request: string): Promise<T> {
        return this.fetch("DELETE", new WebRequest(request))
    }


    private async fetch<T>(method: string, request: WebRequest): Promise<T> {
        try {
            let body = request.body
            if (body && this.isNotBodyInit(body)) {
                body = JSON.stringify(body)
            }

            const response = await fetch((this.baseUrl ?? "") + request.url, {
                method: method,
                mode: "cors",
                body: body,
                headers: {
                    "Accept": request.accept ?? "*/*",
                    "User-Agent": this.userAgent ?? request.userAgent ?? ""
                },
                signal: request.abortController?.signal

            })

            return await this.mapResponse(method, request, response)
        } catch (e: any) {
            this.log.error(`Request to ${method} ${request.url} failed:`, e)
            // TODO: add context
            throw e
        }
    }

    private async mapResponse<T>(method: string, request: WebRequest, response: Response): Promise<T> {
        if (!response.ok) {
            await this.handleError(method, request, response)
        }

        const contentType = response.headers.get("content-type") || ""

        if (contentType.includes("text/") || contentType.includes("application/xml")
          || contentType.includes("application/yaml")) {
            const text = await response.text()
            return text as unknown as T
        } else {
            const json = await response.json()
            return json as T
        }
    }

    private async handleError(method: string, request: WebRequest, response: Response) {
        const status = response.status
        const errorText = await response.text()

        throw new HttpError(method, request.url, status, response.statusText, errorText)
    }

    private isNotBodyInit(value: any): boolean {
        return !(
            typeof value === 'string' ||
            value instanceof Blob ||
            value instanceof ArrayBuffer ||
            ArrayBuffer.isView(value) || // covers BufferSource: TypedArrays/DataView
            value instanceof FormData ||
            value instanceof URLSearchParams ||
            this.isReadableStream(value)
        )
    }

    private isReadableStream(obj: unknown): obj is ReadableStream<Uint8Array> {
        return typeof ReadableStream !== 'undefined' && obj instanceof ReadableStream
    }

}