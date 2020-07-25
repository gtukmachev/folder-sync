package tga.folder_sync.conf

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Conf {

    private lateinit var _config: Config

    val config: Config get() = _config

    fun init() {
        _config = ConfigFactory.load()
    }

}
