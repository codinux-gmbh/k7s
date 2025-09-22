export class UuidService {

    private static cryptoWithUuid: CryptoWithUuid | null = this.determineIfCryptoSupportsGeneratingUuids()

    static createId(): string {
        // Crypto.randomUUID() is only available in secure contexts (https and localhost) and on newer browsers
        if (this.cryptoWithUuid) {
            return this.cryptoWithUuid.randomUUID()
        }

        return this.uuidv4()
    }

    // copied from https://stackoverflow.com/a/2117523 . Not a real UUID but absolutely satisfactory for our needs
    private static uuidv4(): string {
        return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, c =>
            (+c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> +c / 4).toString(16)
        );
    }

    private static determineIfCryptoSupportsGeneratingUuids(): CryptoWithUuid | null {
        if (typeof window !== "undefined") {
            const crypto = window.crypto
            if (crypto && "randomUUID" in crypto && typeof (crypto as any).randomUUID == "function") {
                return crypto as CryptoWithUuid
            }
        }

        return null
    }

}


// @ts-ignore
interface CryptoWithUuid extends Crypto {
    randomUUID() : string
}
