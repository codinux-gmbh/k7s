export class HttpError extends Error {

  constructor(readonly method: string, readonly url: string,
              readonly status: number, readonly statusText: string, readonly body: string) {
    super(`${method} ${url} failed with ${status} ${statusText}: ${body}`)
    this.name = "HttpError"
  }

}