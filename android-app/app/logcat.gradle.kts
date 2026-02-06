import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class LogcatTask : DefaultTask() {
    @TaskAction
    fun captureLogcat() {
        val adb = android.adbExecutable

        // Clear logs
        project.exec {
            commandLine(adb.absolutePath, "logcat", "-c")
        }

        println("Logs cleared. Open the app and try login...")
        println("Capturing logs in 5 seconds...")
        Thread.sleep(5000)

        // Capture logs
        val output = ByteArrayOutputStream()
        project.exec {
            commandLine(adb.absolutePath, "logcat", "-d")
            standardOutput = output
        }

        val logs = output.toString()
        val filteredLogs = logs.lines().filter {
            it.contains("LoginScreen") || it.contains("LoginViewModel")
        }

        println("\n========================================")
        println("CAPTURED LOGS:")
        println("========================================")
        filteredLogs.forEach { println(it) }
        println("========================================")
        println("Total: ${filteredLogs.size} lines")
    }
}

tasks.register<LogcatTask>("captureLogs")

