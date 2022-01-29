package com.erickrodrigues.staticcollector.domain.dockercompose

data class DockerContainer(
    var build: String? = null,
    var image: String? = null,
) {
    var restart: String? = null
    var container_name: String? = null
    var env_file: List<String>? = null
    var environment: Map<String, String>? = null
    var ports: List<String>? = null
    var networks: Map<String, Map<String, String>>? = null
    var depends_on: List<String>? = null
    var volumes: List<String>? = null
    var links: List<String>? = null
    var entrypoint: String? = null
    var command: String? = null
}