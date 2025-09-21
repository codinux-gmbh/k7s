export class ContainerStatus {

  constructor(readonly name: string, readonly containerID: string | undefined,
              readonly image: string, readonly imageID: string, readonly restartCount: number,
              readonly started: boolean, readonly ready: boolean, readonly waiting: boolean,
              readonly running: boolean, readonly terminated: boolean) { }

}