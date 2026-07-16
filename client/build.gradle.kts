plugins {
	id("buildsrc.convention.kotlin-jvm")
	application
}

dependencies {
	implementation(project(":common"))
}

application {
	mainClass = "org.bread_experts_group.client.MainKt"
}