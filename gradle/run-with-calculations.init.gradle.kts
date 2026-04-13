import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

gradle.projectsEvaluated {
    val appProject = rootProject.findProject(":app") ?: return@projectsEvaluated

    appProject.tasks.register<JavaExec>("run-with-calculations") {
        group = "application"
        description = "Uruchamia pojedynczy eksperyment KNN i liczy precision/recall/F1/macroF1/accuracy"

        val sourceSets = appProject.extensions.getByType<SourceSetContainer>()
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("org.bir.knn.RunWithCalculationsRunner")

        val rawArgs = appProject.findProperty("appArgs")?.toString()?.trim().orEmpty()
        if (rawArgs.isNotBlank()) {
            args(rawArgs.split("\\s+".toRegex()))
        }
    }
}

