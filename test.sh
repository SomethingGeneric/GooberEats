#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${SCRIPT_DIR}"
BUILD_SCRIPT="${REPO_ROOT}/scripts/build-apk.sh"
OUTPUT_DIR="${REPO_ROOT}/dist"
APK_NAME="GooberEats-debug.apk"
APK_PATH="${OUTPUT_DIR}/${APK_NAME}"

AVD_NAME="${AVD_NAME:-GooberEatsTestAvd}"
SYSTEM_IMAGE="${SYSTEM_IMAGE:-system-images;android-34;google_apis;x86_64}"
BUILD_PLATFORM="${BUILD_PLATFORM:-android-36}"
BUILD_TOOLS="${BUILD_TOOLS:-36.0.0}"

USE_DOCKER_BUILD=false
SKIP_BUILD=false
EMULATOR_HEADLESS="${EMULATOR_HEADLESS:-0}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --docker-build)
      USE_DOCKER_BUILD=true
      shift
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    --system-image)
      SYSTEM_IMAGE="${2:?--system-image requires a value}"
      shift 2
      ;;
    --avd-name)
      AVD_NAME="${2:?--avd-name requires a value}"
      shift 2
      ;;
    --headless)
      EMULATOR_HEADLESS=1
      shift
      ;;
    *)
      echo "error: unknown argument '$1'" >&2
      echo "usage: $0 [--docker-build] [--skip-build] [--system-image <id>] [--avd-name <name>] [--headless]" >&2
      exit 1
      ;;
  esac
done

if [[ "${USE_DOCKER_BUILD}" == "true" && "${SKIP_BUILD}" == "true" ]]; then
  echo "error: --docker-build and --skip-build are mutually exclusive" >&2
  exit 1
fi

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "error: required command '$1' is not available on PATH" >&2
    exit 1
  fi
}

download_file() {
  local url="$1"
  local destination="$2"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "${url}" -o "${destination}"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "${url}" -O "${destination}"
  else
    echo "error: neither curl nor wget is available to download ${url}" >&2
    exit 1
  fi
}

ensure_cmdline_tools() {
  local tools_bin="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin"
  if [[ -x "${tools_bin}/sdkmanager" ]]; then
    return
  fi

  echo "[test.sh] Android SDK not found; downloading command-line tools..."
  ensure_command unzip

  local tmpdir
  tmpdir="$(mktemp -d)"
  local archive="${tmpdir}/commandlinetools.zip"
  local url="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

  download_file "${url}" "${archive}"
  unzip -q "${archive}" -d "${tmpdir}"

  mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"
  rm -rf "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
  mv "${tmpdir}/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest"

  rm -rf "${tmpdir}"
}

DEFAULT_SDK_DIR="${REPO_ROOT}/.android-sdk"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "${ANDROID_SDK_ROOT}" ]]; then
  ANDROID_SDK_ROOT="${DEFAULT_SDK_DIR}"
fi

mkdir -p "${ANDROID_SDK_ROOT}"

if [[ "${ANDROID_SDK_ROOT}" == "${DEFAULT_SDK_DIR}" ]]; then
  echo "[test.sh] Using Android SDK directory ${ANDROID_SDK_ROOT}"
fi

ensure_cmdline_tools

export ANDROID_SDK_ROOT
export ANDROID_HOME="${ANDROID_SDK_ROOT}"
export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/emulator:${PATH}"

ensure_command java
ensure_command sdkmanager
ensure_command avdmanager

run_sdkmanager() {
  local description="$1"
  shift
  local log_file
  log_file="$(mktemp)"
  echo "[test.sh] ${description}..."
  set +e
  yes | sdkmanager "$@" >"${log_file}" 2>&1
  local status=$?
  set -e
  if (( status != 0 )); then
    if grep -Eq "All SDK package licenses accepted" "${log_file}"; then
      status=0
    fi
  fi
  if (( status != 0 )); then
    if grep -Eq "Computing updates" "${log_file}" || grep -Eq "Unzipping" "${log_file}"; then
      status=0
    fi
  fi
  if (( status != 0 )); then
    echo "error: sdkmanager ${description} failed. Output:" >&2
    cat "${log_file}" >&2
    rm -f "${log_file}"
    exit 1
  fi
  rm -f "${log_file}"
}

run_sdkmanager "Accepting Android SDK licenses" --licenses
run_sdkmanager "Installing core SDK packages" --install \
  "platform-tools" \
  "platforms;${BUILD_PLATFORM}" \
  "build-tools;${BUILD_TOOLS}"
run_sdkmanager "Installing emulator packages" --install \
  "emulator" \
  "${SYSTEM_IMAGE}"

if [[ "${SKIP_BUILD}" != "true" ]]; then
  BUILD_ARGS=()
  if [[ "${USE_DOCKER_BUILD}" == "true" ]]; then
    BUILD_ARGS+=(--docker)
  fi
  "${BUILD_SCRIPT}" "${BUILD_ARGS[@]}"
fi

if [[ ! -f "${APK_PATH}" ]]; then
  echo "error: expected APK at ${APK_PATH}; build step failed or --skip-build used without existing artifact" >&2
  exit 1
fi

export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/emulator:${PATH}"

ensure_command emulator
ensure_command adb

AVD_EXISTS=false
if avdmanager list avd | grep -Fq "Name: ${AVD_NAME}"; then
  AVD_EXISTS=true
fi

if [[ "${AVD_EXISTS}" != "true" ]]; then
  echo "[test.sh] Creating AVD '${AVD_NAME}' using ${SYSTEM_IMAGE}"
  # shellcheck disable=SC2016
  if ! (echo "no" | avdmanager create avd -n "${AVD_NAME}" -k "${SYSTEM_IMAGE}" --device "pixel_5" >/dev/null); then
    echo "[test.sh] 'pixel_5' device not available; retrying with generic 'pixel' profile"
    # shellcheck disable=SC2016
    echo "no" | avdmanager create avd -n "${AVD_NAME}" -k "${SYSTEM_IMAGE}" --device "pixel" >/dev/null
  fi
fi

echo "[test.sh] Starting emulator '${AVD_NAME}'..."
EMULATOR_LOG="$(mktemp)"
EMULATOR_ARGS=(
  -avd "${AVD_NAME}"
  -no-snapshot
  -no-boot-anim
  -netfast
  -no-audio
)

if [[ "${EMULATOR_HEADLESS}" == "1" ]]; then
  EMULATOR_ARGS+=(-no-window)
fi

EMULATOR_BIN="$(command -v emulator)"
"${EMULATOR_BIN}" "${EMULATOR_ARGS[@]}" >"${EMULATOR_LOG}" 2>&1 &
EMULATOR_PID=$!

cleanup() {
  echo "[test.sh] Shutting down emulator..."
  if command -v adb >/dev/null 2>&1; then
    if [[ -n "${EMULATOR_SERIAL:-}" ]]; then
      adb -s "${EMULATOR_SERIAL}" emu kill >/dev/null 2>&1 || true
    else
      adb devices | awk '/emulator-/{print $1}' | while read -r emulator_id; do
        adb -s "${emulator_id}" emu kill >/dev/null 2>&1 || true
      done
    fi
  fi
  if ps -p "${EMULATOR_PID}" >/dev/null 2>&1; then
    kill "${EMULATOR_PID}" >/dev/null 2>&1 || true
    wait "${EMULATOR_PID}" >/dev/null 2>&1 || true
  fi
  if [[ "${KEEP_EMULATOR_LOG:-0}" != "1" ]]; then
    rm -f "${EMULATOR_LOG}"
  else
    echo "[test.sh] Emulator log preserved at ${EMULATOR_LOG}"
  fi
}
trap cleanup EXIT

adb wait-for-device

EMULATOR_SERIAL="$(adb devices | awk '/emulator-/{print $1; exit}')"
if [[ -z "${EMULATOR_SERIAL}" ]]; then
  KEEP_EMULATOR_LOG=1
  echo "error: emulator started but did not appear in 'adb devices'" >&2
  exit 1
fi

BOOT_TIMEOUT_SECONDS="${BOOT_TIMEOUT_SECONDS:-180}"
echo "[test.sh] Waiting for device boot (timeout ${BOOT_TIMEOUT_SECONDS}s)..."

SECONDS_WAITED=0
until adb -s "${EMULATOR_SERIAL}" shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; do
  sleep 2
  SECONDS_WAITED=$((SECONDS_WAITED + 2))
  if (( SECONDS_WAITED >= BOOT_TIMEOUT_SECONDS )); then
    KEEP_EMULATOR_LOG=1
    echo "error: emulator failed to boot within ${BOOT_TIMEOUT_SECONDS} seconds" >&2
    exit 1
  fi
done

adb -s "${EMULATOR_SERIAL}" shell input keyevent 82 >/dev/null 2>&1 || true

echo "[test.sh] Installing ${APK_NAME}..."
adb -s "${EMULATOR_SERIAL}" install -r "${APK_PATH}" >/dev/null || {
  KEEP_EMULATOR_LOG=1
  echo "error: failed to install APK; check emulator log at ${EMULATOR_LOG}" >&2
  exit 1
}

echo "[test.sh] Launching app..."
adb -s "${EMULATOR_SERIAL}" shell monkey -p cloud.goober.goobereats -c android.intent.category.LAUNCHER 1 >/dev/null || true

echo "[test.sh] App launched on emulator. Interact using 'adb logcat' or the emulator window."
echo "[test.sh] Press Ctrl+C when finished to stop the emulator."

wait "${EMULATOR_PID}"
