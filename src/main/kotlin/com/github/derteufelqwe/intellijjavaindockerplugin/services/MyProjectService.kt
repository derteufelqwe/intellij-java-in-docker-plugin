package com.github.derteufelqwe.intellijjavaindockerplugin.services

import com.intellij.openapi.project.Project
import com.github.derteufelqwe.intellijjavaindockerplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
