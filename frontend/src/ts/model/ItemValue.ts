export class ItemValue {

  constructor(
    readonly name: string, readonly value?: string, readonly mobileValue?: string,
    readonly showOnMobile: boolean = true, readonly showOnDesktop: boolean = true,
    readonly useRemainingSpace: boolean = false,
  ) { }

}