package com.liftrr.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LiftrrApplication

fun main(args: Array<String>) {
	runApplication<LiftrrApplication>(*args)
}
