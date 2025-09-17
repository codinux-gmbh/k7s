export class ResourceParameter {

  constructor(readonly group: string | undefined = undefined,
              readonly kind: string,
              readonly context?: string, readonly namespace?: string) { }

}