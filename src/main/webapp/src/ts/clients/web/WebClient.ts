import { WebRequest } from "./WebRequest"

export interface WebClient {

    get<T>(url: string): Promise<T>

    get<T>(request: WebRequest): Promise<T>

    post<T>(request: WebRequest): Promise<T>

    delete<T>(url: string): Promise<T>

}