package com.mapprjct.kotest.controller

import com.mapprjct.testKtorApp
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import org.testcontainers.containers.PostgreSQLContainer

class ProfileControllerTest : FunSpec() {
    init {
        val postgres = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)

        install(TestContainerSpecExtension(postgres))

        context(""){
            testKtorApp(postgres){

            }
        }
    }
}