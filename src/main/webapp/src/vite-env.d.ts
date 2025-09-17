/// <reference types="svelte" />
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_BACKEND_HOST: string | null
  readonly VITE_BACKEND_PORT: number | null
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
