package me.valer.ktlibminer

import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.config.TraceNode
import me.valer.ktlibminer.scenario.DefaultScenario
import me.valer.ktlibminer.scenario.InferenceOnlyScenario
import me.valer.ktlibminer.scenario.PrependScenario
import org.apache.commons.cli.*

fun main(args: Array<String>) {
    parseCommandLine(args)
}


fun parseCommandLine(args: Array<String>) {
    val options = Options()

    val helpOption = Option("h", "help", false, "help instructions")
    options.addOption(helpOption)

    val tokenOption = Option("t", "token", true, "GitHub API token")
    options.addOption(tokenOption)

    val saveDbOption = Option("h", "hold-db", false, "Allows you not to reset the accumulated data")
    options.addOption(saveDbOption)

    val inferOnlyOption = Option(
        "u",
        "use-trace",
        false,
        "inference with already collected traces (you can pass it param if you passed name)"
    )
    options.addOption(inferOnlyOption)

    val nameOption = Option("n", "name", true, "package name for analysis")
    options.addOption(nameOption)

    val allProjOption =
        Option("a", "all-projects", false, "download all found projects (only with jar assets by default))")
    options.addOption(allProjOption)

    val signatureOption =
        Option("s", "signature", false, "use the signature in the analysis (by default, the method name is used)")
    options.addOption(signatureOption)

    val jumpsOption =
        Option("j", "jumps-traversal", true, "max len of trace for analysis (default 200)")
    options.addOption(jumpsOption)

    val depthOption =
        Option("d", "depth-traversal", true, "max depth of trace for analysis or -1 for any depth (default 10)")
    options.addOption(depthOption)

    val traceCountOption =
        Option(
            "l",
            "limit-trace",
            true,
            "max quantity of all traces for analysis or -1 for any quantity (default 10000000)"
        )
    options.addOption(traceCountOption)

    val disMergeOption =
        Option("dm", "disable-merge", false, "disable merging of end states")
    options.addOption(disMergeOption)

    val kOption = Option("k", true, "minimum length of overlapping outgoing paths for a merge (default 1)")
    options.addOption(kOption)

    val goalOption = Option("g", "goal", true, "the number of projects to be found (default 100)")
    options.addOption(goalOption)

    val outputOption =
        Option("w", "workdir", true, "dir for output inference result and for cloning projects. required option!")
    options.addOption(outputOption)

    val inputOption = Option("p", "prepared", true, "dir with prepared projects for analysis")
    options.addOption(inputOption)

    val gradleVersionOption = Option("gv", "gradle-version", true, "gradle version for build projects")
    options.addOption(gradleVersionOption)

    val gradlePathOption = Option("gp", "gradle-path", true, "path to installed gradle for build projects")
    options.addOption(gradlePathOption)

    val mavenPathOption = Option("m", "maven", true, "path to installed maven for build projects")
    options.addOption(mavenPathOption)

    val parser: CommandLineParser = GnuParser()


    var formatter: HelpFormatter
    try {
        val line = parser.parse(options, args)
        if (line.hasOption("help") || !line.hasOption("w")) {
            formatter = HelpFormatter()
            formatter.printHelp("KtLibMiner", options)
            return
        }
        Configurations.workdir = line.getOptionValue("w")

        if (line.hasOption("k")) Configurations.kAlg = line.getOptionValue("k").toInt()
        if (line.hasOption("dm")) Configurations.unionEnd = false

        if (!line.hasOption("n") || line.hasOption("u")) {
            InferenceOnlyScenario().run()
            return
        }

        if (!line.hasOption("n")) throw ParseException("For run analysis set name arg")
        else Configurations.libName = line.getOptionValue("n")

        if (line.hasOption("h")) Configurations.saveDb = true

        if (line.hasOption("gv")) Configurations.gradleVersion = line.getOptionValue("gv")
        if (line.hasOption("gp")) Configurations.gradlePath = line.getOptionValue("gp")
        if (line.hasOption("m")) Configurations.mavenPath = line.getOptionValue("m")

        if (line.hasOption("s")) Configurations.traceNode = TraceNode.SIGNATURE
        if (line.hasOption("j")) Configurations.traversJumps = line.getOptionValue("j").toInt()
        if (line.hasOption("d")) Configurations.traversDepth = line.getOptionValue("d").toInt()
        if (line.hasOption("l")) Configurations.traceLimit = line.getOptionValue("l").toInt()

        if (line.hasOption("p")) {
            PrependScenario(line.getOptionValue("p")).run()
            return
        }

        if (!line.hasOption("t")) throw ParseException("For projects search set token arg")
        else Configurations.ghToken = line.getOptionValue("t")

        if (line.hasOption("a")) Configurations.allProj = true
        if (line.hasOption("g")) Configurations.goal = line.getOptionValue("g").toInt()
        DefaultScenario().run()
    } catch (e: ParseException) {
        System.err.println("Parsing failed.  Reason: " + e.message);
        formatter = HelpFormatter()
        formatter.printHelp("KtLibMiner", options);
    }
}




