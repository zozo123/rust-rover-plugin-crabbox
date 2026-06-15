# syntax=docker/dockerfile:1.7

FROM docker.io/library/rust:1-bookworm

ARG USERNAME=crabbox
ARG USER_UID=1000
ARG USER_GID=1000
ARG CARGO_NEXTEST_VERSION=0.9.104

ENV DEBIAN_FRONTEND=noninteractive \
    CARGO_HOME=/usr/local/cargo \
    RUSTUP_HOME=/usr/local/rustup \
    PATH=/usr/local/cargo/bin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
    CARGO_TERM_COLOR=always

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash \
        ca-certificates \
        clang \
        cmake \
        curl \
        git \
        jq \
        libclang-dev \
        libssl-dev \
        lld \
        mold \
        openssh-client \
        pkg-config \
        protobuf-compiler \
        python3 \
        python3-pip \
        rsync \
        sudo \
        tar \
        unzip \
        xz-utils \
        zstd \
    && rm -rf /var/lib/apt/lists/*

RUN rustup component add clippy rustfmt \
    && rustc --version \
    && cargo --version \
    && cargo clippy --version \
    && cargo fmt --version

RUN arch="$(dpkg --print-architecture)" \
    && case "$arch" in \
        amd64) nextest_arch="x86_64-unknown-linux-gnu" ;; \
        arm64) nextest_arch="aarch64-unknown-linux-gnu" ;; \
        *) echo "Unsupported architecture for cargo-nextest: $arch" >&2; exit 1 ;; \
    esac \
    && curl -fsSL "https://get.nexte.st/${CARGO_NEXTEST_VERSION}/${nextest_arch}.tar.gz" -o /tmp/cargo-nextest.tar.gz \
    && tar -xzf /tmp/cargo-nextest.tar.gz -C /usr/local/cargo/bin cargo-nextest \
    && rm /tmp/cargo-nextest.tar.gz \
    && cargo nextest --version

RUN groupadd --gid "${USER_GID}" "${USERNAME}" \
    && useradd --uid "${USER_UID}" --gid "${USER_GID}" --create-home --shell /bin/bash "${USERNAME}" \
    && mkdir -p /workspace/crabbox /workspace/cache \
    && chown -R "${USERNAME}:${USERNAME}" /workspace /usr/local/cargo /usr/local/rustup \
    && echo "${USERNAME} ALL=(ALL) NOPASSWD:ALL" > "/etc/sudoers.d/${USERNAME}" \
    && chmod 0440 "/etc/sudoers.d/${USERNAME}"

WORKDIR /workspace/crabbox
USER ${USERNAME}

RUN cargo --version \
    && cargo clippy --version \
    && cargo nextest --version

CMD ["bash"]
