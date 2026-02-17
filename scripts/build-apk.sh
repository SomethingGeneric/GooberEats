#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ANDROID_DIR="${REPO_ROOT}/GooberEats"
OUTPUT_DIR="${REPO_ROOT}/dist"
APK_NAME="GooberEats-debug.apk"
APK_SOURCE="${ANDROID_DIR}/app/build/outputs/apk/debug/app-debug.apk"
APK_TARGET="${OUTPUT_DIR}/${APK_NAME}"
DOCKERFILE="${REPO_ROOT}/Dockerfile.android"

USE_DOCKER=false
if [[ "${1:-}" == "--docker" || "${1:-}" == "-d" ]]; then
  USE_DOCKER=true
  shift
fi

if [[ "${USE_DOCKER}" == "true" ]]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "error: docker is not installed or not on PATH" >&2
    exit 1
  fi

  if [[ ! -f "${DOCKERFILE}" ]]; then
    echo "error: Dockerfile.android not found at ${DOCKERFILE}" >&2
    exit 1
  fi

  DOCKER_BUILD_ARGS_VALUE="${DOCKER_BUILD_ARGS-}"
  DOCKER_BUILD_ARGS_ARRAY=()
  if [[ -n "${DOCKER_BUILD_ARGS_VALUE}" ]]; then
    # shellcheck disable=SC2206
    DOCKER_BUILD_ARGS_ARRAY=( ${DOCKER_BUILD_ARGS_VALUE} )
  fi

  mkdir -p "${OUTPUT_DIR}"
  docker build \
    --file "${DOCKERFILE}" \
    "${DOCKER_BUILD_ARGS_ARRAY[@]}" \
    --output "type=local,dest=${OUTPUT_DIR}" \
    "${REPO_ROOT}"

  if [[ ! -f "${OUTPUT_DIR}/${APK_NAME}" ]]; then
    echo "error: docker build did not produce ${APK_NAME}" >&2
    exit 1
  fi

  echo "APK ready at ${OUTPUT_DIR}/${APK_NAME}"
  exit 0
fi

if [[ ! -x "${ANDROID_DIR}/gradlew" ]]; then
  echo "error: ${ANDROID_DIR}/gradlew is missing or not executable" >&2
  exit 1
fi

(
  cd "${ANDROID_DIR}"
  ./gradlew assembleDebug
)

if [[ ! -f "${APK_SOURCE}" ]]; then
  echo "error: expected APK not found at ${APK_SOURCE}" >&2
  exit 1
fi

mkdir -p "${OUTPUT_DIR}"
cp "${APK_SOURCE}" "${APK_TARGET}"
echo "APK ready at ${APK_TARGET}"
