#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

usage() {
  cat <<'EOF'
Uso: ./builder <comando>

Comandos:
  build    Compila o plugin (./gradlew buildPlugin)
  clean    Limpa artefatos de build
  help     Mostra esta ajuda
EOF
}

cmd="${1:-}"
case "$cmd" in
  build)
    if command -v node >/dev/null 2>&1; then
      node "$ROOT/tests/jscript-es3-lint.mjs"
      node "$ROOT/tests/parse-protocol-url.mjs"
    fi
    if command -v cmd.exe >/dev/null 2>&1; then
      cmd.exe /c "$ROOT/tests/check-jscript.cmd" || exit 1
    fi
    ./gradlew buildPlugin --no-daemon
    zip_path="$(ls -t "$ROOT/build/distributions/"*.zip 2>/dev/null | head -1 || true)"
    if [[ -n "${zip_path:-}" && -f "$zip_path" ]]; then
      echo ""
      echo "Plugin gerado: $zip_path"
    fi
    ;;
  clean)
    ./gradlew clean --no-daemon
    ;;
  help | -h | --help)
    usage
    ;;
  "")
    echo "Erro: informe um comando." >&2
    echo "" >&2
    usage >&2
    exit 1
    ;;
  *)
    echo "Erro: comando desconhecido: $cmd" >&2
    echo "" >&2
    usage >&2
    exit 1
    ;;
esac
