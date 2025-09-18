export class Constants {

  static readonly isDev = import.meta.env.DEV


  static readonly width = document.documentElement.clientWidth || document.body.clientWidth

  static readonly isMobile = Constants.width < 1024

  static readonly isDesktop = !!!Constants.isMobile

}