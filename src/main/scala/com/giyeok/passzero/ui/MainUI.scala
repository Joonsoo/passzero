package com.giyeok.passzero.ui

import java.io.File

object MainUI {
    def main(args: Array[String]): Unit = {
        val config: Config = Config(new StringRegistry {}, new File("./localInfo.p0"))

        new SWTUI(config).start()
    }
}
