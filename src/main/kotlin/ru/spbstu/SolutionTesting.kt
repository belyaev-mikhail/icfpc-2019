package ru.spbstu

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import ru.spbstu.util.log
import java.io.File

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

object Tester {
    lateinit var browser: ChromeDriver
    fun prepare() {
        val chromeOptions = ChromeOptions()
        chromeOptions.setBinary("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe")
        System.setProperty("webdriver.chrome.driver", "D:\\!9Semester\\icfpc-2019\\chromedriver.exe");
        browser = ChromeDriver(chromeOptions)
        browser.get("https://icfpcontest2019.github.io/solution_checker/")
    }

    fun run(solution: String, description: String, booster: String?): ValidationResult {
        val taskSubmissionField = browser.findElementById("submit_task")
        taskSubmissionField.sendKeys(description)
        val solSubmissionField = browser.findElementById("submit_solution")
        solSubmissionField.sendKeys(solution)
        val boostSubmissionField = browser.findElementById("submit_boosters")
        boostSubmissionField.clear()
        if (booster != null) boostSubmissionField.sendKeys(booster)
        val submitButton = browser.findElementById("execute_solution")
        submitButton.click()
        val result = browser.findElementById("output")
        while (true) {
            val text = result.text
            if (text.startsWith("Cannot check")) {
                log.error("Error in files")
                break
            } else if (text.startsWith("Success")) {
                return ValidationResult.Success
            } else if (text.startsWith("Failed")) {
                return ValidationResult.Error(text)
            }
        }
        throw Exception("Validation not finished")
    }


    fun close() {
        browser.close()
    }

    fun validate(description: String, solutionsDir: String) {
        val descFolder = File(description)
        val solFolder = File(solutionsDir)
        val descriptions = descFolder.walkTopDown().filter { it.isFile }.filter { it.extension == "desc" }
        val solutionSources = solFolder.walkTopDown().filter { it.isDirectory }
        val solutions = solutionSources.flatMap {
            solFolder.walkTopDown()
                    .filter { it.isFile }
                    .filter { it.extension == "sol" }
                    .map { "${it.nameWithoutExtension}.desc" to it }
        }.groupBy { it.first }
                .mapValues { (_, files) -> files.toSet().toList() }

        for (desc in descriptions) {
            val sol = solutions[desc.name]
            if (sol == null || sol.isEmpty()) {
                log.warn("${desc.name}: No solution for file")
                continue
            }
            for (item in sol) {
                val result = run(item.second.absolutePath, desc.absolutePath, null)
                log.debug("${item.second.parentFile.name}/${item.second.name}: $result")
                if (result is ValidationResult.Error) {
                    log.warn("${item.second.parentFile.name}/${item.second.name}: ${result.message}")
                    item.second.renameTo(File("${item.second.absolutePath}.invalid"))
                }
            }

        }
    }
}

fun main(args: Array<String>) {
    Tester.prepare()
    Tester.validate("D:\\!9Semester\\icfpc-2019\\docs\\tasks", "D:\\!9Semester\\icfpc-2019\\fuck")
    Tester.close()
}