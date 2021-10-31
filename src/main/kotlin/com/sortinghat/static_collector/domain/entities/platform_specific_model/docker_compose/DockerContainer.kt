package com.sortinghat.static_collector.domain.entities.platform_specific_model.docker_compose

import com.sortinghat.static_collector.domain.entities.base.Vertex

data class DockerContainer(val identifier: String) : Vertex() {
    var image: String = ""
    var build: String = ""
    var environment: HashMap<String, String> = hashMapOf()
    var dependsOn: MutableList<DockerContainer> = mutableListOf()
}