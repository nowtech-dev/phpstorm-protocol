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

run_jscript_check() {
  if [[ -n "${WSL_DISTRO_NAME:-}" ]] || grep -qi microsoft /proc/version 2>/dev/null; then
    echo "Skip: cscript no WSL (use tests/check-jscript.cmd no cmd.exe nativo; lint Node já rodou)"
    return 0
  fi
  if ! command -v cscript.exe >/dev/null 2>&1 && ! command -v cscript >/dev/null 2>&1; then
    return 0
  fi
  local cscript_bin="cscript"
  command -v cscript.exe >/dev/null 2>&1 && cscript_bin="cscript.exe"
  local proto_dir="$ROOT/src/main/resources/protocol"
  local test_url='phpstorm://open?file=C:\Windows\win.ini&line=1'
  (cd "$proto_dir" && "$cscript_bin" //Nologo //E:JScript open-url.js "$test_url")
  local ec=$?
  if [[ "$ec" -ge 2 ]]; then
    echo "Erro: cscript falhou (syntax error no open-url.js)" >&2
    exit 1
  fi
}

cmd="${1:-}"
case "$cmd" in
  build)
    if command -v node >/dev/null 2>&1; then
      node "$ROOT/tests/jscript-es3-lint.mjs"
      node "$ROOT/tests/parse-protocol-url.mjs"
    fi
    run_jscript_check
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
