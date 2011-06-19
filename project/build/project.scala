import sbt._ 

class Borachio(info: ProjectInfo) extends ParentProject(info) {
  
  val scalaToolsSnapshots = ScalaToolsSnapshots

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
  
  lazy val library = project("library", "Borachio Library", new LibraryProject(_))
  lazy val plugin = project("plugin", "Borachio Plugin", new PluginProject(_), library)
  lazy val examples = project("examples", "Borachio Examples", new ExamplesProject(_), library, plugin)
  
  class LibraryProject(info: ProjectInfo) extends DefaultProject(info) {

    val scalatest = "org.scalatest" %% "scalatest" % "1.4.1" % "optional"
    val junit = "junit" % "junit" % "3.8.2" % "optional"
    val specs2 = "org.specs2" %% "specs2" % "1.3" % "optional"
  }
  
  class PluginProject(info: ProjectInfo) extends DefaultProject(info)
  
  class ExamplesProject(info: ProjectInfo) extends DefaultProject(info) {

    val scalatest = "org.scalatest" %% "scalatest" % "1.4.1"
    val specs2 = "org.specs2" %% "specs2" % "1.3"

    def specs2Framework = new TestFramework("org.specs2.runner.SpecsFramework")
    override def testFrameworks = super.testFrameworks ++ Seq(specs2Framework)
    
    def managedSources = "src_managed"
    override def cleanAction = super.cleanAction dependsOn cleanTask(managedSources)
    
    def generateMocksSourceRoots = "src" / "generate_mocks" / "scala"
    def generateMocksSources = sources(generateMocksSourceRoots)
    
    def generateMockCompileOptions = 
      compileOptions(
        "-Xplugin:plugin/target/scala_"+ buildScalaVersion +"/borachio-plugin_"+ buildScalaVersion +"-"+ projectVersion.value +".jar",
        "-Xplugin-require:borachio",
        "-Ylog:generatemocks",
        "-P:borachio:generatemocks:examples/src_managed/mock/scala"
      ) ++ super.compileOptions
    
  	class GenerateMocksCompileConfig extends TestCompileConfig {
  		override def baseCompileOptions = generateMockCompileOptions
  		override def label = "generatemocks"
      override def sourceRoots = generateMocksSourceRoots
  		override def sources = generateMocksSources
  	}
  	def generateMocksCompileConfiguration = new GenerateMocksCompileConfig
  	lazy val generateMocksCompileConditional = new CompileConditional(generateMocksCompileConfiguration, buildCompiler)

    lazy val generateMocks = generateMocksAction
    def generateMocksAction = generateMocksTask describedAs "Generates sources for classes with the @mock annotation"
    def generateMocksTask = task {
      generateMocksCompileConditional.run
    }

    def mockSourceRoots = managedSources / "mock" / "scala"
    def mockSources = sources(mockSourceRoots)
    def mockCompilePath = outputPath / "mock-classes"
    def mockAnalysisPath = outputPath / "mock-analysis"
    
    class CompileMocksCompileConfig extends TestCompileConfig {
      override def label = "compilemocks"
      override def sourceRoots = mockSourceRoots
      override def sources = mockSources
      override def outputDirectory = mockCompilePath
      override def analysisPath = mockAnalysisPath
    }
    def compileMocksCompileConfiguration = new CompileMocksCompileConfig
    lazy val compileMocksCompileConditional = new CompileConditional(compileMocksCompileConfiguration, buildCompiler)
    
    lazy val compileMocks = compileMocksAction
    def compileMocksAction = compileMocksTask dependsOn generateMocksTask describedAs "Compile mocks"
    def compileMocksTask = task {
      compileMocksCompileConditional.run
    }
  }
}