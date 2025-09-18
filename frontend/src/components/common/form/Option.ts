export class Option {

  constructor(readonly value: string | undefined, readonly label: string = value ?? "") { }

}