package tga.folder_sync.params

data class Parameters(
    val command: Command,
    val src: String,
    val dst: String,
    val sessionFolder: String?,
    val outDir: String
) {
    enum class Command{ `init` , sync }
    class WrongParametersException(msg: String, e: Exception? = null) : RuntimeException(msg, e)

    companion object{
        fun parse(vararg args: String): Parameters {
            val outDir: String = System.getProperty("outDir") ?: ""

            if (args.isEmpty()) return Parameters(Command.sync, "", "", null, outDir)

            val cmd = try{ Command.valueOf(args[0]) } catch (e: Exception) { throw(WrongParametersException("The <command> '${args[0]}' unrecognized", e)) }
            if (cmd == Command.sync) {
                val sessionFolder = if (args.size >= 2)  args[1] else null
                return Parameters(Command.sync, "", "", sessionFolder, outDir)
            } else {
                val src = if (args.size >= 2)  args[1] else throw( WrongParametersException("Parameter <source folder> is missed") )
                val dst = if (args.size >= 3)  args[2] else throw( WrongParametersException("Parameter <destination folder> is missed") )
                return Parameters(cmd, src, dst, null, outDir)
            }


        }
    }
}
